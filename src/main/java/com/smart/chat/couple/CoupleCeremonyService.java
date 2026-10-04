package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.List;

/**
 * 愿望券本（保留卡 `couple-cere-coupon`，原 F236）：花积分给 TA 发一张「任意愿望」券，
 * TA 想用的时候点核销——积分唯一的花法，也是「我攒的分都花在你身上」这句情绪的落点。
 *
 * 系统裁剪：我们的小日子、过法任务卡、庆祝打卡、爱情保险柜、续约仪式、当日体感、
 * 小日子史册、年度加冕、节日黄历全部下线；券的产出也从「保险柜满几个月掉券」
 * 改成纯积分购买（见 docs/couple-trim-ranking.md 第七节）。
 */
@Service
public class CoupleCeremonyService {

    static final int COUPON_TITLE_MAX = 80;
    /** 发一张券固定花 10 分。 */
    static final int COUPON_COST = 10;
    static final String COUPON_SPEND_PREFIX = "发出愿望券：";

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCeremonyCouponMapper couponMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final ImPushService push;

    public CoupleCeremonyService(CoupleSpaceMapper spaceMapper, CoupleCeremonyCouponMapper couponMapper,
                                 CouplePointLedgerMapper ledgerMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.couponMapper = couponMapper;
        this.ledgerMapper = ledgerMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record CouponVO(String id, String title, String status, String ref, String issuer,
                           String usedBy, Long created) {
    }

    /** 券本总览：在途券 + 已核销券 + 本人余额（发券要花分，余额必须看得见）。 */
    public record OverviewVO(String day, List<CouponVO> couponsOpen, List<CouponVO> couponsUsed,
                             int myBalance, int couponCost) {
    }

    // ========== 读 ==========

    /** 券本总览（GET /overview）。 */
    public OverviewVO overview(String me) {
        CoupleSpace space = requireSpace(me);
        return new OverviewVO(LocalDate.now().toString(),
                couponsOf(space, CoupleCeremonyCoupon.STATUS_OPEN),
                couponsOf(space, CoupleCeremonyCoupon.STATUS_USED),
                balance(space, me), COUPON_COST);
    }

    // ========== 写 ==========

    /** 发一张愿望券（券面自拟，先扣发券人的积分）。 */
    public OverviewVO issueCoupon(String me, String title) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(title, COUPON_TITLE_MAX, "券面写点什么愿望吧");
        // 积分唯一的花分出口：发券必须先有余额，否则「愿望」就成了空头支票
        int balance = balance(space, me);
        if (balance < COUPON_COST) {
            throw new BusinessException(400, "发一张愿望券要 " + COUPON_COST + " 分，你只有 " + balance
                    + " 分——先去好事簿记一笔 TA 为你做过的事吧");
        }
        couponMapper.insert(CoupleCeremonyCoupon.of(space.getId(), text, me, ""));
        ledgerMapper.insert(CouplePointLedger.of(space.getId(), me, CouplePointLedger.TYPE_SPEND,
                COUPON_SPEND_PREFIX + text, COUPON_COST));
        push.pushCoupleEvent("ceremony-coupon", me, space.partnerOf(me), "TA 给你发了一张愿望券：" + text);
        return overview(me);
    }

    /** 核销一张愿望券（OPEN→USED，谁收到券谁说了算）。 */
    public OverviewVO useCoupon(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleCeremonyCoupon coupon = couponMapper.selectById(id);
        if (coupon == null || !coupon.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这张愿望券不存在");
        }
        if (!coupon.isOpen()) {
            throw new BusinessException(400, "这张券已经核销过了");
        }
        coupon.setStatus(CoupleCeremonyCoupon.STATUS_USED);
        coupon.setUsedBy(me);
        coupon.setUsedAt(System.currentTimeMillis());
        couponMapper.updateById(coupon);
        push.pushCoupleEvent("ceremony-coupon-used", me, space.partnerOf(me), "愿望券被兑现了：" + coupon.getTitle());
        return overview(me);
    }

    // ========== 聚合 ==========

    /** 本人积分余额 = 累计 EARN − 累计 SPEND。 */
    private int balance(CoupleSpace space, String me) {
        return ledgerMapper.findBySpace(space.getId()).stream()
                .filter(l -> me.equals(l.getFromUser()))
                .mapToInt(l -> CouplePointLedger.TYPE_EARN.equals(l.getType())
                        ? (l.getPoints() == null ? 0 : l.getPoints())
                        : -(l.getPoints() == null ? 0 : l.getPoints()))
                .sum();
    }

    private List<CouponVO> couponsOf(CoupleSpace space, String status) {
        List<CouponVO> list = new ArrayList<>();
        for (CoupleCeremonyCoupon coupon : couponMapper.findBySpace(space.getId())) {
            if (status.equals(coupon.getStatus())) {
                list.add(new CouponVO(coupon.getId(), coupon.getTitle(), coupon.getStatus(), coupon.getRef(),
                        coupon.getIssuer(), coupon.getUsedBy(), coupon.getCreated()));
            }
        }
        list.sort(Comparator.comparing(CouponVO::created, Comparator.reverseOrder()));
        return list;
    }

    // ========== 通用 ==========

    private String requireText(String value, int max, String emptyMsg) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, emptyMsg);
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BusinessException(400, "最多 " + max + " 个字，心意不在字数");
        }
        return trimmed;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
