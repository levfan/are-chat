package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.wish.Wish;
import com.smart.chat.couple.domain.wish.WishRepository;

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
    private WishRepository wishRepository;
    @Mock
    private CoupleEventPublisher push;

    @InjectMocks
    private CoupleWishService service;

    /** 假清单：只在 WishRepository 这一层成立，PO 与 Mapper 不进用例。 */
    private final class Bag {
        private final List<Wish> rows = new ArrayList<>();
        private boolean duplicateTitle;

        Bag stub() {
            lenient().when(wishRepository.findBySpace(SPACE)).thenAnswer(inv -> List.copyOf(rows));
            lenient().doAnswer(inv -> {
                Wish saved = inv.getArgument(0);
                if (saved.id() == null) {
                    // 新愿望：id 与记录时刻由写这一层落定，和适配器一致
                    rows.add(Wish.restore("w" + (rows.size() + 1), saved.spaceId(), saved.ownerUser(),
                            saved.creatorUser(), saved.title(), saved.note(), saved.status(), saved.preparedBy(),
                            saved.preparedAt(), saved.fulfilledAt(), System.currentTimeMillis()));
                } else {
                    rows.replaceAll(w -> w.id().equals(saved.id()) ? saved : w);
                }
                return null;
            }).when(wishRepository).save(any(Wish.class));
            lenient().when(wishRepository.findById(any())).thenAnswer(inv -> rows.stream()
                    .filter(r -> r.id().equals(inv.getArgument(0))).findFirst());
            lenient().doAnswer(inv -> {
                rows.removeIf(r -> r.id().equals(inv.getArgument(0)));
                return null;
            }).when(wishRepository).deleteById(anyString());
            lenient().when(wishRepository.sameTitleExists(eq(SPACE), anyString(), anyString()))
                    .thenAnswer(inv -> duplicateTitle);
            lenient().when(wishRepository.countFulfilled(SPACE))
                    .thenAnswer(inv -> rows.stream()
                            .filter(r -> Wish.STATUS_FULFILLED.equals(r.status())).count());
            return this;
        }

        Wish add(String owner, String creator, String title, String status) {
            Wish wish = Wish.restore("w" + (rows.size() + 1), SPACE, owner, creator, title, null, status,
                    null, null, null, System.currentTimeMillis());
            rows.add(wish);
            return wish;
        }
    }

    private void stubSpace() {
        CoupleSpace space = CoupleSpace.restore(SPACE, "alice", "bob", CoupleSpace.STATUS_ACTIVE,
                System.currentTimeMillis(), null, null, null, null, null, null);
        lenient().when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.of(space));
    }

    @Test
    void markingPreparedPushesNothingAtAll() {
        stubSpace();
        Bag bag = new Bag().stub();
        Wish row = bag.add("alice", "alice", "想要一副耳机", Wish.STATUS_OPEN);

        service.prepare("bob", row.id());

        verifyNoInteractions(push);
        assertThat(row.status()).isEqualTo(Wish.STATUS_PREPARED);
        assertThat(row.preparedBy()).isEqualTo("bob");
    }

    @Test
    void ownerSeesOpenStatusAndNoPreparedTimestamp() {
        stubSpace();
        Bag bag = new Bag().stub();
        Wish row = bag.add("alice", "alice", "想一起去海边", Wish.STATUS_OPEN);
        service.prepare("bob", row.id());

        CoupleWishService.WishBoardVO mine = service.board("alice");
        CoupleWishService.WishBoardVO theirs = service.board("bob");

        // 许愿人这边：还在 open 组，状态是 OPEN，连标记时刻都是 null
        assertThat(mine.open()).extracting(CoupleWishService.WishVO::id).contains(row.id());
        assertThat(mine.prepared()).isEmpty();
        CoupleWishService.WishVO asOwner = mine.open().stream()
                .filter(w -> w.id().equals(row.id())).findFirst().orElseThrow();
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
        Wish row = bag.add("alice", "alice", "想要机械键盘", Wish.STATUS_OPEN);

        assertThatThrownBy(() -> service.prepare("alice", row.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("给 TA 留的");
        service.prepare("bob", row.id());
        // 已标记之后再让许愿人点一次：话术必须与「没标记过」时一模一样，
        // 不能出现「已经标过了」这种把惊喜说出去的字（本地真后端探针实测到的缺陷）
        assertThatThrownBy(() -> service.prepare("alice", row.id()))
                .isInstanceOf(BusinessException.class).hasMessage("这条愿望是你自己许的，「已准备」那一格是给 TA 留的");
        assertThatThrownBy(() -> service.unprepare("alice", row.id()))
                .isInstanceOf(BusinessException.class).hasMessage("这条愿望是你自己许的，「已准备」那一格是给 TA 留的");
        service.unprepare("bob", row.id());
        assertThat(row.status()).isEqualTo(Wish.STATUS_OPEN);
    }

    @Test
    void fulfillingIsTheOwnersCallAndMakesItPublic() {
        stubSpace();
        Bag bag = new Bag().stub();
        Wish row = bag.add("alice", "alice", "想要那本书", Wish.STATUS_OPEN);
        service.prepare("bob", row.id());

        assertThatThrownBy(() -> service.fulfill("bob", row.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有许愿的人");

        CoupleWishService.WishBoardVO board = service.fulfill("alice", row.id());
        assertThat(board.fulfilled()).extracting(CoupleWishService.WishVO::id).contains(row.id());
        verify(push).pushCoupleEvent(eq("wish-fulfilled"), eq("alice"), eq("bob"), anyString());
        // 实现之后谁都不能再改状态
        assertThatThrownBy(() -> service.prepare("bob", row.id()))
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
        verify(wishRepository, never()).save(any(Wish.class));
    }

    @Test
    void openListIsCappedAtThirty() {
        stubSpace();
        Bag bag = new Bag().stub();
        for (int i = 0; i < 30; i++) {
            bag.add("alice", "alice", "愿望 " + i, Wish.STATUS_OPEN);
        }
        assertThatThrownBy(() -> service.add("alice", "再多一条", null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多同时挂 30 条");
    }

    @Test
    void removeAndEditNoteBelongToTheRecorder() {
        stubSpace();
        Bag bag = new Bag().stub();
        Wish row = bag.add("alice", "bob", "帮 TA 实现的盲盒", Wish.STATUS_OPEN);

        assertThatThrownBy(() -> service.remove("alice", row.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有记这条愿望的人");
        assertThatThrownBy(() -> service.updateNote("alice", row.id(), "改一下"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有记这条愿望的人能改");

        service.updateNote("bob", row.id(), "改成蓝色的");
        assertThat(row.note()).isEqualTo("改成蓝色的");
        service.remove("bob", row.id());
        assertThat(bag.rows).isEmpty();
    }

    @Test
    void wishFromAnotherSpaceIsNotFound() {
        stubSpace();
        Bag bag = new Bag().stub();
        Wish elsewhere = Wish.restore("w9", "other-space", "carol", "carol", "别人的愿望", null,
                Wish.STATUS_OPEN, null, null, null, System.currentTimeMillis());
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
