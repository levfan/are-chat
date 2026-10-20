package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.intimatelog.IntimateLog;
import com.smart.chat.couple.domain.intimatelog.IntimateLogRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoupleIntimateLogServiceTest {

    private static final String SPACE = "s1";
    private static final LocalDate TODAY = LocalDate.now();

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private IntimateLogRepository logRepository;

    @InjectMocks
    private CoupleIntimateLogService service;

    /** 假仓储：排序口径与适配器一致（日期倒序、同日记录时刻倒序）。 */
    private final class Bag {
        private final List<IntimateLog> rows = new ArrayList<>();

        Bag stub() {
            lenient().when(logRepository.findBySpace(SPACE)).thenAnswer(inv -> {
                rows.sort(Comparator.comparing(IntimateLog::day).reversed()
                        .thenComparing(Comparator.comparing(IntimateLog::created).reversed()));
                return List.copyOf(rows);
            });
            lenient().when(logRepository.findById(any())).thenAnswer(inv -> rows.stream()
                    .filter(row -> row.id().equals(inv.getArgument(0))).findFirst());
            lenient().doAnswer(inv -> {
                rows.add(inv.getArgument(0));
                return null;
            }).when(logRepository).save(any(IntimateLog.class));
            lenient().doAnswer(inv -> {
                rows.removeIf(row -> row.id().equals(inv.getArgument(0)));
                return null;
            }).when(logRepository).deleteById(anyString());
            return this;
        }
    }

    private void stubSpace() {
        CoupleSpace space = CoupleSpace.restore(SPACE, "alice", "bob", CoupleSpace.STATUS_ACTIVE,
                System.currentTimeMillis(), null, null);
        lenient().when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.of(space));
    }

    /** 写口不再返回看板（展示统一走月历读模型），所以这里直接看落库的那一行长什么样。 */
    @Test
    void addStoresTheRecordAsTyped() {
        stubSpace();
        Bag bag = new Bag().stub();

        service.add("alice", TODAY.minusDays(2).toString(), "SEX", "  很好  ");

        assertThat(bag.rows).hasSize(1);
        assertThat(bag.rows.get(0).day()).isEqualTo(TODAY.minusDays(2).toString());
        assertThat(bag.rows.get(0).kind()).isEqualTo(IntimateLog.KIND_SEX);
        assertThat(bag.rows.get(0).note()).isEqualTo("很好");
        assertThat(bag.rows.get(0).recorderUser()).isEqualTo("alice");
    }

    @Test
    void theSameDayCanHoldMoreThanOneRecord() {
        stubSpace();
        Bag bag = new Bag().stub();
        service.add("alice", TODAY.toString(), "EDGE", null);
        service.add("bob", TODAY.toString(), "SEX", null);

        assertThat(bag.rows).hasSize(2);
        assertThat(bag.rows).extracting(IntimateLog::kind)
                .containsExactlyInAnyOrder(IntimateLog.KIND_EDGE, IntimateLog.KIND_SEX);
    }

    @Test
    void removeDeletesTheRowAndUnknownIdsAreNotFound() {
        stubSpace();
        Bag bag = new Bag().stub();
        service.add("alice", TODAY.toString(), "SEX", null);
        String id = bag.rows.get(0).id();

        service.remove("bob", id);
        assertThat(bag.rows).isEmpty();

        IntimateLog elsewhere = IntimateLog.restore("i9", "other-space", TODAY.toString(), "EDGE", null, "carol", 0L);
        bag.rows.add(elsewhere);
        assertThatThrownBy(() -> service.remove("alice", "i9"))
                .isInstanceOf(BusinessException.class).hasMessage("这条亲密记录不在你们的空间里");
    }

    @Test
    void noSpaceMeansStandardNotFoundAndNoWrites() {
        when(spaceRepository.findActiveByMember("solo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add("solo", TODAY.toString(), "SEX", null))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
        assertThatThrownBy(() -> service.remove("solo", "i1"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
        verify(logRepository, never()).save(any(IntimateLog.class));
    }

    @Test
    void invalidKindIsTranslatedToFourHundred() {
        stubSpace();
        Bag bag = new Bag().stub();
        assertThatThrownBy(() -> service.add("alice", TODAY.toString(), "KISS", null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("选「边缘」或「生活」");
        assertThat(bag.rows).isEmpty();
    }

    @Test
    void futureDayAndOverlongNoteAreBothRejected() {
        stubSpace();
        Bag bag = new Bag().stub();
        assertThatThrownBy(() -> service.add("alice", TODAY.plusDays(1).toString(), "SEX", null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没到的日子");
        assertThatThrownBy(() -> service.add("alice", TODAY.toString(), "SEX", "啊".repeat(61)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("备注最多 60 个字");
        assertThat(bag.rows).isEmpty();
    }
}
