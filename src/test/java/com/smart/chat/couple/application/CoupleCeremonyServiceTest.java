package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleCeremonyCoupon;
import com.smart.chat.couple.infrastructure.persistence.CoupleCeremonyCouponMapper;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedger;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpace;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 愿望券本（保留卡 `couple-cere-coupon`）单测：积分唯一的花分出口。
 * 锁的是「券行与台账 SPEND 行都真落库、余额不够时一行都不许写」，不是返回值回显。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCeremonyServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleCeremonyCouponMapper couponMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCeremonyService service;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setCreated(System.currentTimeMillis());
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    private final class Bag {
        private final List<CoupleCeremonyCoupon> coupons = new ArrayList<>();
        private final List<CouplePointLedger> ledger = new ArrayList<>();

        void stub() {
            lenient().when(ledgerMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(ledger));
            lenient().when(couponMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(coupons));
            lenient().when(couponMapper.insert(any(CoupleCeremonyCoupon.class))).thenAnswer(inv -> {
                coupons.add(inv.getArgument(0));
                return 1;
            });
            lenient().when(couponMapper.updateById(any(CoupleCeremonyCoupon.class))).thenReturn(1);
            lenient().when(ledgerMapper.insert(any(CouplePointLedger.class))).thenAnswer(inv -> {
                ledger.add(inv.getArgument(0));
                return 1;
            });
            lenient().when(couponMapper.selectById(any())).thenAnswer(inv -> coupons.stream()
                    .filter(c -> c.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
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
        verify(couponMapper, never()).insert(any(CoupleCeremonyCoupon.class));

        // 好事簿攒来的 12 分够发一张 10 分的券，券必须真落到台账的 SPEND 行
        bag.ledger.add(CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "好事簿：接我下班", 12));
        service.issueCoupon("alice", "陪我去海边");
        assertThat(bag.coupons).hasSize(1);
        assertThat(bag.ledger).filteredOn(l -> CouplePointLedger.TYPE_SPEND.equals(l.getType())).hasSize(1);
        CouplePointLedger spend = bag.ledger.stream()
                .filter(l -> CouplePointLedger.TYPE_SPEND.equals(l.getType())).findFirst().orElseThrow();
        assertThat(spend.getFromUser()).isEqualTo("alice");
        assertThat(spend.getPoints()).isEqualTo(CoupleCeremonyService.COUPON_COST);
        assertThat(spend.getItem()).isEqualTo("发出愿望券：陪我去海边");

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
        bag.ledger.add(CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "好事簿：接我下班", 50));

        assertThatThrownBy(() -> service.issueCoupon("alice", "  "))
                .isInstanceOf(BusinessException.class).hasMessage("券面写点什么愿望吧");
        service.issueCoupon("alice", "一次说走就走的骑行");
        verify(couponMapper).insert(any(CoupleCeremonyCoupon.class));
    }

    @Test
    void useCouponGuardsExistenceAndStatus() {
        stubSpace("alice");
        Bag bag = new Bag();
        bag.stub();

        assertThatThrownBy(() -> service.useCoupon("alice", "nope"))
                .isInstanceOf(BusinessException.class).hasMessage("这张愿望券不存在");

        CoupleCeremonyCoupon used = CoupleCeremonyCoupon.of("s1", "已核销券", "bob", "");
        used.setId("c1");
        used.setStatus(CoupleCeremonyCoupon.STATUS_USED);
        bag.coupons.add(used);
        assertThatThrownBy(() -> service.useCoupon("alice", "c1"))
                .isInstanceOf(BusinessException.class).hasMessage("这张券已经核销过了");

        CoupleCeremonyCoupon open = CoupleCeremonyCoupon.of("s1", "看一次海", "bob", "");
        open.setId("c2");
        bag.coupons.add(open);
        service.useCoupon("alice", "c2");
        assertThat(open.getStatus()).isEqualTo(CoupleCeremonyCoupon.STATUS_USED);
        assertThat(open.getUsedBy()).isEqualTo("alice");
        verify(couponMapper).updateById(open);
        verify(push).pushCoupleEvent(eq("ceremony-coupon-used"), eq("alice"), eq("bob"), any());
    }

    @Test
    void overviewExposesBalanceSoTheUserKnowsIfAnotherCardIsAffordable() {
        stubSpace("alice");
        Bag bag = new Bag();
        bag.stub();
        bag.ledger.add(CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "好事簿：接我下班", 12));
        bag.ledger.add(CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "家务轮盘干完：倒垃圾", 3));
        bag.ledger.add(CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_SPEND, "发出愿望券：看一次海", 10));
        // 对方的流水不能算进我的余额
        bag.ledger.add(CouplePointLedger.of("s1", "bob", CouplePointLedger.TYPE_EARN, "好事簿：帮我吹头", 2));

        CoupleCeremonyService.OverviewVO vo = service.overview("alice");
        assertThat(vo.myBalance()).isEqualTo(5);
        assertThat(vo.couponCost()).isEqualTo(CoupleCeremonyService.COUPON_COST);
    }

    @Test
    void noSpaceThrows404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.overview("solo"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
    }
}
