package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleCeremonyCouponPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleCeremonyCouponMapper;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerPO;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.couple.domain.coupon.WishCoupon;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
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
import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;
@Service
public class CoupleCeremonyService {

    static final int COUPON_TITLE_MAX = 80;
    /** 发一张券固定花 10 分。 */
    static final int COUPON_COST = 10;
    static final String COUPON_SPEND_PREFIX = "发出愿望券：";

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCeremonyCouponMapper couponMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final CoupleEventPublisher push;

    public CoupleCeremonyService(CoupleSpaceMapper spaceMapper, CoupleCeremonyCouponMapper couponMapper,
                                 CouplePointLedgerMapper ledgerMapper, CoupleEventPublisher push) {
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
        CoupleSpacePO space = requireSpace(me);
        return new OverviewVO(LocalDate.now().toString(),
                couponsOf(space, CoupleCeremonyCouponPO.STATUS_OPEN),
                couponsOf(space, CoupleCeremonyCouponPO.STATUS_USED),
                balance(space, me), COUPON_COST);
    }

    // ========== 写 ==========

    /** 发一张愿望券（券面自拟，先扣发券人的积分）。 */
    public OverviewVO issueCoupon(String me, String title) {
        CoupleSpacePO space = requireSpace(me);
        // 券面规则、字数上限、余额闸门都在 WishCoupon 里；这里只管取余额和落库
        WishCoupon wish = rule(() -> WishCoupon.grant(title, me, balance(space, me)));
        String text = wish.title();
        couponMapper.insert(CoupleCeremonyCouponPO.of(space.getId(), text, me, ""));
        ledgerMapper.insert(CouplePointLedgerPO.of(space.getId(), me, CouplePointLedgerPO.TYPE_SPEND,
                COUPON_SPEND_PREFIX + text, COUPON_COST));
        push.pushCoupleEvent("ceremony-coupon", me, space.partnerOf(me), "TA 给你发了一张愿望券：" + text);
        return overview(me);
    }

    /** 核销一张愿望券（OPEN→USED，谁收到券谁说了算）。 */
    public OverviewVO useCoupon(String me, String id) {
        CoupleSpacePO space = requireSpace(me);
        CoupleCeremonyCouponPO coupon = couponMapper.selectById(id);
        if (coupon == null || !coupon.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这张愿望券不存在");
        }
        WishCoupon wish = WishCoupon.restore(coupon.getId(), coupon.getTitle(), coupon.getIssuer(),
                coupon.getStatus(), coupon.getUsedBy(), coupon.getUsedAt());
        guard(() -> wish.useBy(me, System.currentTimeMillis()));
        coupon.setStatus(wish.status());
        coupon.setUsedBy(wish.usedBy());
        coupon.setUsedAt(wish.usedAt());
        couponMapper.updateById(coupon);
        push.pushCoupleEvent("ceremony-coupon-used", me, space.partnerOf(me), "愿望券被兑现了：" + coupon.getTitle());
        return overview(me);
    }

    // ========== 聚合 ==========

    /** 本人积分余额 = 累计 EARN − 累计 SPEND。 */
    private int balance(CoupleSpacePO space, String me) {
        return ledgerMapper.findBySpace(space.getId()).stream()
                .filter(l -> me.equals(l.getFromUser()))
                .mapToInt(l -> CouplePointLedgerPO.TYPE_EARN.equals(l.getType())
                        ? (l.getPoints() == null ? 0 : l.getPoints())
                        : -(l.getPoints() == null ? 0 : l.getPoints()))
                .sum();
    }

    private List<CouponVO> couponsOf(CoupleSpacePO space, String status) {
        List<CouponVO> list = new ArrayList<>();
        for (CoupleCeremonyCouponPO coupon : couponMapper.findBySpace(space.getId())) {
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

    private CoupleSpacePO requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
