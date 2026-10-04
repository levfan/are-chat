package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.coupon.CouponRepository;
import com.smart.chat.couple.domain.coupon.WishCoupon;
import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 愿望券本（保留卡 `couple-cere-coupon`）单测：积分唯一的花分出口。
 * 锁的是「券与台账 SPEND 行都真落库、余额不够时一行都不许写」，不是返回值回显。
 * mock 的是仓储端口（CouponRepository / PointLedgerRepository），PO 与 Mapper 不在这一层出现。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCeremonyServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private PointLedgerRepository ledgerRepository;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCeremonyService service;

    private CoupleSpace space() {
        return CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, System.currentTimeMillis(),
                null, null, null, null, null, null, null);
    }

    private void stubSpace(String me) {
        lenient().when(spaceRepository.findActiveByMember(me)).thenReturn(Optional.of(space()));
    }

    private static WishCoupon coupon(String id, String title, String issuer, String status) {
        return WishCoupon.restore(id, "s1", title, issuer, "", 1L, status, null, null);
    }

    /** 假券本 + 假台账：只在端口这一层成立，落库口径由适配器自己的用例锁。 */
    private final class Bag {
        private final List<WishCoupon> coupons = new ArrayList<>();
        private final List<PointEntry> ledger = new ArrayList<>();

        void stub() {
            lenient().when(ledgerRepository.findBySpace("s1")).thenAnswer(inv -> List.copyOf(ledger));
            lenient().when(couponRepository.findBySpace("s1")).thenAnswer(inv -> List.copyOf(coupons));
            lenient().doAnswer(inv -> {
                WishCoupon issued = inv.getArgument(0);
                coupons.add(coupon("c" + (coupons.size() + 1), issued.title(), issued.grantedBy(),
                        WishCoupon.STATUS_OPEN));
                return null;
            }).when(couponRepository).issue(any(WishCoupon.class), anyString());
            lenient().doAnswer(inv -> {
                ledger.add(inv.getArgument(0));
                return null;
            }).when(ledgerRepository).append(any(PointEntry.class));
            lenient().when(couponRepository.findByIdIn(any(), eq("s1"))).thenAnswer(inv -> coupons.stream()
                    .filter(c -> c.id().equals(inv.getArgument(0))).findFirst());
            lenient().doAnswer(inv -> {
                WishCoupon used = inv.getArgument(0);
                coupons.replaceAll(c -> c.id().equals(used.id()) ? used : c);
                return null;
            }).when(couponRepository).save(any(WishCoupon.class));
        }

        void earn(String user, int points) {
            ledger.add(PointEntry.earn("s1", user, "好事簿：接我下班", points));
        }
    }

    @Test
    void issuingACouponCostsPointsAndBlocksWhenBroke() {
        stubSpace("alice");
        Bag bag = new Bag();
        bag.stub();

        // 一分没有就发不出券，且一行都不许落库
        assertThatThrownBy(() -> service.issueCoupon("alice", "陪我去海边"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有 0 分");
        assertThat(bag.coupons).isEmpty();
        verify(couponRepository, never()).issue(any(WishCoupon.class), anyString());

        // 好事簿攒来的 12 分够发一张 10 分的券，券必须真落到台账的 SPEND 行
        bag.earn("alice", 12);
        service.issueCoupon("alice", "陪我去海边");
        assertThat(bag.coupons).hasSize(1);
        assertThat(bag.ledger).filteredOn(e -> !e.earned()).hasSize(1);
        ArgumentCaptor<PointEntry> spend = ArgumentCaptor.forClass(PointEntry.class);
        verify(ledgerRepository).append(spend.capture());
        assertThat(spend.getValue().fromUser()).isEqualTo("alice");
        assertThat(spend.getValue().points()).isEqualTo(CoupleCeremonyService.COUPON_COST);
        assertThat(spend.getValue().item()).isEqualTo("发出愿望券：陪我去海边");
        assertThat(spend.getValue().earned()).isFalse();

        // 余额只剩 2 分，第二张发不出去
        assertThatThrownBy(() -> service.issueCoupon("alice", "再一张"))
                .isInstanceOf(BusinessException.class);
        assertThat(bag.coupons).hasSize(1);
    }

    @Test
    void issueCouponValidatesTitle() {
        stubSpace("alice");
        Bag bag = new Bag();
        bag.stub();
        // 发券现在要先有积分余额，这里给够一张券的钱再验标题
        bag.earn("alice", 50);

        assertThatThrownBy(() -> service.issueCoupon("alice", "  "))
                .isInstanceOf(BusinessException.class).hasMessage("券面写点什么愿望吧");
        service.issueCoupon("alice", "一次说走就走的骑行");
        verify(couponRepository).issue(any(WishCoupon.class), eq(""));
    }

    @Test
    void useCouponGuardsExistenceAndStatus() {
        stubSpace("alice");
        Bag bag = new Bag();
        bag.stub();

        assertThatThrownBy(() -> service.useCoupon("alice", "nope"))
                .isInstanceOf(BusinessException.class).hasMessage("这张愿望券不存在");

        bag.coupons.add(coupon("c1", "已核销券", "bob", WishCoupon.STATUS_USED));
        assertThatThrownBy(() -> service.useCoupon("alice", "c1"))
                .isInstanceOf(BusinessException.class).hasMessage("这张券已经核销过了");

        bag.coupons.add(coupon("c2", "看一次海", "bob", WishCoupon.STATUS_OPEN));
        service.useCoupon("alice", "c2");
        WishCoupon used = bag.coupons.stream().filter(c -> "c2".equals(c.id())).findFirst().orElseThrow();
        assertThat(used.status()).isEqualTo(WishCoupon.STATUS_USED);
        assertThat(used.usedBy()).isEqualTo("alice");
        verify(couponRepository).save(any(WishCoupon.class));
        verify(push).pushCoupleEvent(eq("ceremony-coupon-used"), eq("alice"), eq("bob"), any());
    }

    @Test
    void overviewExposesBalanceSoTheUserKnowsIfAnotherCardIsAffordable() {
        stubSpace("alice");
        Bag bag = new Bag();
        bag.stub();
        bag.ledger.add(PointEntry.earn("s1", "alice", "好事簿：接我下班", 12));
        bag.ledger.add(PointEntry.earn("s1", "alice", "家务轮盘干完：倒垃圾", 3));
        bag.ledger.add(PointEntry.spend("s1", "alice", "发出愿望券：看一次海", 10));
        // 对方的流水不能算进我的余额
        bag.ledger.add(PointEntry.earn("s1", "bob", "好事簿：帮我吹头", 2));

        CoupleCeremonyService.OverviewVO vo = service.overview("alice");
        assertThat(vo.myBalance()).isEqualTo(5);
        assertThat(vo.couponCost()).isEqualTo(CoupleCeremonyService.COUPON_COST);
    }

    @Test
    void noSpaceThrows404() {
        when(spaceRepository.findActiveByMember("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.overview("solo"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
    }
}
