package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.coupon.CouponRepository;
import com.smart.chat.couple.domain.coupon.WishCoupon;
import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

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

    /** 发一张券固定花 10 分。 */
    static final int COUPON_COST = 10;
    static final String COUPON_SPEND_PREFIX = "发出愿望券：";

    private final CoupleSpaceRepository spaceRepository;
    private final CouponRepository couponRepository;
    private final PointLedgerRepository ledgerRepository;
    private final CoupleEventPublisher push;

    public CoupleCeremonyService(CoupleSpaceRepository spaceRepository, CouponRepository couponRepository,
                                 PointLedgerRepository ledgerRepository, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.couponRepository = couponRepository;
        this.ledgerRepository = ledgerRepository;
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
                couponsOf(space, WishCoupon.STATUS_OPEN),
                couponsOf(space, WishCoupon.STATUS_USED),
                balance(space, me), COUPON_COST);
    }

    // ========== 写 ==========

    /** 发一张愿望券（券面自拟，先扣发券人的积分）。 */
    public OverviewVO issueCoupon(String me, String title) {
        CoupleSpace space = requireSpace(me);
        // 券面规则、字数上限、余额闸门都在 WishCoupon 里；这里只管取余额和落库
        WishCoupon wish = rule(() -> WishCoupon.grant(space.id(), title, me, balance(space, me)));
        couponRepository.issue(wish, "");
        ledgerRepository.append(PointEntry.spend(space.id(), me, COUPON_SPEND_PREFIX + wish.title(), COUPON_COST));
        push.pushCoupleEvent("ceremony-coupon", me, space.partnerOf(me), "TA 给你发了一张愿望券：" + wish.title());
        return overview(me);
    }

    /** 核销一张愿望券（OPEN→USED，谁收到券谁说了算）。 */
    public OverviewVO useCoupon(String me, String id) {
        CoupleSpace space = requireSpace(me);
        // 别的空间的券号在这里等同于不存在——这条归属闸门原先写在 Service 里现拼，现在归端口
        WishCoupon coupon = couponRepository.findByIdIn(id, space.id())
                .orElseThrow(() -> new BusinessException(404, "这张愿望券不存在"));
        guard(() -> coupon.useBy(me, System.currentTimeMillis()));
        couponRepository.save(coupon);
        push.pushCoupleEvent("ceremony-coupon-used", me, space.partnerOf(me), "愿望券被兑现了：" + coupon.title());
        return overview(me);
    }

    // ========== 聚合 ==========

    /** 本人积分余额 = 累计 EARN − 累计 SPEND。 */
    private int balance(CoupleSpace space, String me) {
        return ledgerRepository.findBySpace(space.id()).stream()
                .filter(e -> me.equals(e.fromUser()))
                .mapToInt(e -> e.earned() ? e.points() : -e.points())
                .sum();
    }

    private List<CouponVO> couponsOf(CoupleSpace space, String status) {
        List<CouponVO> list = new ArrayList<>();
        for (WishCoupon coupon : couponRepository.findBySpace(space.id())) {
            if (status.equals(coupon.status())) {
                list.add(new CouponVO(coupon.id(), coupon.title(), coupon.status(), coupon.ref(),
                        coupon.grantedBy(), coupon.usedBy(), coupon.issuedAt()));
            }
        }
        list.sort(Comparator.comparing(CouponVO::created, Comparator.reverseOrder()));
        return list;
    }

    // ========== 通用 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
