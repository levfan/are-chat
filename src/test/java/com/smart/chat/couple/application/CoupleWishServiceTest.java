package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.infrastructure.persistence.CoupleWishMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleWishPO;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.sharedkernel.web.BusinessException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 愿望清单单测，核心是那条「偷偷」：<b>标记已准备一个字节都不许推给对方</b>，
 * 并且许愿人视角拿到的 VO 里状态、时刻都不能露。这条一旦破功，功能就从「偷偷准备」变成「剧透」。
 */
@ExtendWith(MockitoExtension.class)
class CoupleWishServiceTest {

    private static final String SPACE = "s1";

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private CoupleWishMapper wishMapper;
    @Mock
    private CoupleEventPublisher push;

    @InjectMocks
    private CoupleWishService service;

    private final class Bag {
        private final List<CoupleWishPO> rows = new ArrayList<>();
        private boolean duplicateTitle;

        Bag stub() {
            lenient().when(wishMapper.findBySpace(SPACE)).thenAnswer(inv -> List.copyOf(rows));
            lenient().when(wishMapper.insert(any(CoupleWishPO.class))).thenAnswer(inv -> {
                rows.add(inv.getArgument(0));
                return 1;
            });
            lenient().when(wishMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                    .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
            lenient().when(wishMapper.updateById(any(CoupleWishPO.class))).thenReturn(1);
            lenient().when(wishMapper.deleteById(anyString())).thenAnswer(inv -> {
                rows.removeIf(r -> r.getId().equals(inv.getArgument(0)));
                return 1;
            });
            lenient().when(wishMapper.existsSameTitle(eq(SPACE), anyString(), anyString()))
                    .thenAnswer(inv -> duplicateTitle);
            lenient().when(wishMapper.countFulfilled(SPACE))
                    .thenAnswer(inv -> rows.stream()
                            .filter(r -> CoupleWishPO.STATUS_FULFILLED.equals(r.getStatus())).count());
            return this;
        }

        CoupleWishPO add(String owner, String creator, String title, String status) {
            CoupleWishPO row = CoupleWishPO.of(SPACE, owner, creator, title, null);
            row.setId("w" + (rows.size() + 1));
            row.setStatus(status);
            rows.add(row);
            return row;
        }
    }

    private void stubSpace() {
        CoupleSpace space = CoupleSpace.restore(SPACE, "alice", "bob", CoupleSpace.STATUS_ACTIVE,
                System.currentTimeMillis(), null, null, null, null, null, null, null);
        lenient().when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.of(space));
    }

    @Test
    void markingPreparedPushesNothingAtAll() {
        stubSpace();
        Bag bag = new Bag().stub();
        CoupleWishPO row = bag.add("alice", "alice", "想要一副耳机", CoupleWishPO.STATUS_OPEN);

        service.prepare("bob", row.getId());

        verifyNoInteractions(push);
        assertThat(row.getStatus()).isEqualTo(CoupleWishPO.STATUS_PREPARED);
        assertThat(row.getPreparedBy()).isEqualTo("bob");
    }

    @Test
    void ownerSeesOpenStatusAndNoPreparedTimestamp() {
        stubSpace();
        Bag bag = new Bag().stub();
        CoupleWishPO row = bag.add("alice", "alice", "想一起去海边", CoupleWishPO.STATUS_OPEN);
        service.prepare("bob", row.getId());

        CoupleWishService.WishBoardVO mine = service.board("alice");
        CoupleWishService.WishBoardVO theirs = service.board("bob");

        // 许愿人这边：还在 open 组，状态是 OPEN，连标记时刻都是 null
        assertThat(mine.open()).extracting(CoupleWishService.WishVO::id).contains(row.getId());
        assertThat(mine.prepared()).isEmpty();
        CoupleWishService.WishVO asOwner = mine.open().stream()
                .filter(w -> w.id().equals(row.getId())).findFirst().orElseThrow();
        assertThat(asOwner.status()).isEqualTo("OPEN");
        assertThat(asOwner.preparedFlag()).isFalse();
        assertThat(asOwner.preparedAt()).isNull();
        assertThat(asOwner.mineFlag()).isTrue();
        assertThat(asOwner.preparableFlag()).isFalse();

        // 标记人这边：单独成组，看得见真状态与时刻，但不能再点准备
        assertThat(theirs.prepared()).hasSize(1);
        CoupleWishService.WishVO asMarker = theirs.prepared().get(0);
        assertThat(asMarker.status()).isEqualTo("PREPARED");
        assertThat(asMarker.preparedFlag()).isTrue();
        assertThat(asMarker.preparedAt()).isNotNull();
        assertThat(asMarker.preparableFlag()).isFalse();
        assertThat(asMarker.canFulfillFlag()).isFalse();
    }

    @Test
    void ownerCannotTouchPreparationWithoutLeakingItsState() {
        stubSpace();
        Bag bag = new Bag().stub();
        CoupleWishPO row = bag.add("alice", "alice", "想要机械键盘", CoupleWishPO.STATUS_OPEN);

        assertThatThrownBy(() -> service.prepare("alice", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("给 TA 留的");
        service.prepare("bob", row.getId());
        // 已标记之后再让许愿人点一次：话术必须与「没标记过」时一模一样，
        // 不能出现「已经标过了」这种把惊喜说出去的字（本地真后端探针实测到的缺陷）
        assertThatThrownBy(() -> service.prepare("alice", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessage("这条愿望是你自己许的，「已准备」那一格是给 TA 留的");
        assertThatThrownBy(() -> service.unprepare("alice", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessage("这条愿望是你自己许的，「已准备」那一格是给 TA 留的");
        service.unprepare("bob", row.getId());
        assertThat(row.getStatus()).isEqualTo(CoupleWishPO.STATUS_OPEN);
    }

    @Test
    void fulfillingIsTheOwnersCallAndMakesItPublic() {
        stubSpace();
        Bag bag = new Bag().stub();
        CoupleWishPO row = bag.add("alice", "alice", "想要那本书", CoupleWishPO.STATUS_OPEN);
        service.prepare("bob", row.getId());

        assertThatThrownBy(() -> service.fulfill("bob", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有许愿的人");

        CoupleWishService.WishBoardVO board = service.fulfill("alice", row.getId());
        assertThat(board.fulfilled()).extracting(CoupleWishService.WishVO::id).contains(row.getId());
        verify(push).pushCoupleEvent(eq("wish-fulfilled"), eq("alice"), eq("bob"), anyString());
        // 实现之后谁都不能再改状态
        assertThatThrownBy(() -> service.prepare("bob", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经实现");
    }

    @Test
    void addingForThePartnerAnnouncesButAddingForMyselfDoesNot() {
        stubSpace();
        Bag bag = new Bag().stub();

        service.add("bob", "陪我去一次图书馆", null, "alice");
        verify(push).pushCoupleEvent(eq("wish-added"), eq("bob"), eq("alice"), anyString());

        // 给自己许愿不推送：一共就该只有上面那一次
        service.add("alice", "想要一台相机", null, null);
        verify(push, times(1)).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
        assertThat(bag.rows).hasSize(2);
    }

    @Test
    void addGatesRejectAndWriteNothing() {
        stubSpace();
        Bag bag = new Bag().stub();

        assertThatThrownBy(() -> service.add("alice", "   ", null, null))
                .isInstanceOf(BusinessException.class).hasMessage("想要什么总得写一句呀");
        assertThatThrownBy(() -> service.add("alice", "一".repeat(81), null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("80");
        assertThatThrownBy(() -> service.add("alice", "想要的东西", null, "someone-else"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只能许给自己");
        duplicate(bag);
        assertThatThrownBy(() -> service.add("alice", "想要一副耳机", null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经在清单上了");

        assertThat(bag.rows).isEmpty();
        verify(wishMapper, never()).insert(any(CoupleWishPO.class));
    }

    @Test
    void openListIsCappedAtThirty() {
        stubSpace();
        Bag bag = new Bag().stub();
        for (int i = 0; i < 30; i++) {
            bag.add("alice", "alice", "愿望 " + i, CoupleWishPO.STATUS_OPEN);
        }
        assertThatThrownBy(() -> service.add("alice", "再多一条", null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多同时挂 30 条");
    }

    @Test
    void removeAndEditNoteBelongToTheRecorder() {
        stubSpace();
        Bag bag = new Bag().stub();
        CoupleWishPO row = bag.add("alice", "bob", "帮 TA 实现的盲盒", CoupleWishPO.STATUS_OPEN);

        assertThatThrownBy(() -> service.remove("alice", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有记这条愿望的人");
        assertThatThrownBy(() -> service.updateNote("alice", row.getId(), "改一下"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有记这条愿望的人能改");

        service.updateNote("bob", row.getId(), "改成蓝色的");
        assertThat(row.getNote()).isEqualTo("改成蓝色的");
        service.remove("bob", row.getId());
        assertThat(bag.rows).isEmpty();
    }

    @Test
    void wishFromAnotherSpaceIsNotFound() {
        stubSpace();
        Bag bag = new Bag().stub();
        CoupleWishPO elsewhere = CoupleWishPO.of("other-space", "carol", "carol", "别人的愿望", null);
        elsewhere.setId("w9");
        bag.rows.add(elsewhere);

        assertThatThrownBy(() -> service.prepare("bob", "w9"))
                .isInstanceOf(BusinessException.class).hasMessage("这条愿望不在你们的清单里");
        assertThatThrownBy(() -> service.fulfill("alice", "nope"))
                .isInstanceOf(BusinessException.class).hasMessage("这条愿望不在你们的清单里");
    }

    @Test
    void noSpaceThrowsTheStandardFourOhFour() {
        when(spaceRepository.findActiveByMember("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("solo"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
    }

    private void duplicate(Bag bag) {
        bag.duplicateTitle = true;
    }
}
