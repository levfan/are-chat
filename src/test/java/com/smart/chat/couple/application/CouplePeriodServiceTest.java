package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.period.PeriodCycles;
import com.smart.chat.couple.domain.period.PeriodRecord;
import com.smart.chat.couple.domain.period.PeriodRepository;
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
class CouplePeriodServiceTest {

    private static final String SPACE = "s1";
    private static final LocalDate TODAY = LocalDate.now();

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private PeriodRepository periodRepository;

    @InjectMocks
    private CouplePeriodService service;

    /** 假仓储：排序口径与适配器一致（开始日倒序），PO 与 Mapper 不进用例。 */
    private final class Bag {
        private final List<PeriodRecord> rows = new ArrayList<>();

        Bag stub() {
            lenient().when(periodRepository.findBySpace(SPACE)).thenAnswer(inv -> {
                rows.sort(Comparator.comparing(PeriodRecord::startDay).reversed());
                return List.copyOf(rows);
            });
            lenient().when(periodRepository.findById(any())).thenAnswer(inv -> rows.stream()
                    .filter(row -> row.id().equals(inv.getArgument(0))).findFirst());
            lenient().when(periodRepository.existsByStart(org.mockito.ArgumentMatchers.eq(SPACE), anyString()))
                    .thenAnswer(inv -> rows.stream()
                            .anyMatch(row -> row.startDay().equals(inv.getArgument(1))));
            lenient().doAnswer(inv -> {
                PeriodRecord saved = inv.getArgument(0);
                rows.removeIf(row -> row.id().equals(saved.id()));
                rows.add(saved);
                return null;
            }).when(periodRepository).save(any(PeriodRecord.class));
            lenient().doAnswer(inv -> {
                rows.removeIf(row -> row.id().equals(inv.getArgument(0)));
                return null;
            }).when(periodRepository).deleteById(anyString());
            return this;
        }
    }

    private void stubSpace() {
        CoupleSpace space = CoupleSpace.restore(SPACE, "alice", "bob", CoupleSpace.STATUS_ACTIVE,
                System.currentTimeMillis(), null, null);
        lenient().when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.of(space));
    }

    @Test
    void addStoresTheDayAndLeavesItOpen() {
        stubSpace();
        Bag bag = new Bag().stub();

        service.add("alice", TODAY.minusDays(3).toString());

        assertThat(bag.rows).hasSize(1);
        assertThat(bag.rows.get(0).startDay()).isEqualTo(TODAY.minusDays(3).toString());
        assertThat(bag.rows.get(0).endDay()).isNull();
        assertThat(bag.rows.get(0).recorderUser()).isEqualTo("alice");
    }

    /** 周期与预测的算法在 PeriodCycles 里（那里逐条测过），这里只确认两行都落库、没互相覆盖。 */
    @Test
    void twoRecordsGiveARealAverageAndPrediction() {
        stubSpace();
        new Bag().stub();
        service.add("alice", TODAY.minusDays(56).toString());
        service.add("bob", TODAY.minusDays(28).toString());

        PeriodCycles cycles = PeriodCycles.of(periodRepository.findBySpace(SPACE), TODAY);
        assertThat(cycles.sampleCount()).isEqualTo(1);
        assertThat(cycles.averageCycleDays()).isEqualTo(28);
        assertThat(cycles.predictedNextDay()).isEqualTo(TODAY);
    }

    @Test
    void duplicateAndFutureDaysAreRejectedWithoutWriting() {
        stubSpace();
        Bag bag = new Bag().stub();
        service.add("alice", TODAY.minusDays(1).toString());

        assertThatThrownBy(() -> service.add("bob", TODAY.minusDays(1).toString()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经记过");
        assertThatThrownBy(() -> service.add("alice", TODAY.plusDays(1).toString()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没到的日子");
        assertThat(bag.rows).hasSize(1);
        verify(periodRepository).save(any(PeriodRecord.class));
    }

    @Test
    void endIsRecordedOnceAndOnly() {
        stubSpace();
        Bag bag = new Bag().stub();
        service.add("alice", TODAY.minusDays(4).toString());
        String id = bag.rows.get(0).id();

        service.end("bob", id);
        assertThat(bag.rows.get(0).endDay()).isEqualTo(TODAY.toString());

        assertThatThrownBy(() -> service.end("alice", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经记过结束日");
    }

    @Test
    void removeDeletesTheRowAndUnknownIdsAreNotFound() {
        stubSpace();
        Bag bag = new Bag().stub();
        service.add("alice", TODAY.minusDays(2).toString());
        String id = bag.rows.get(0).id();

        service.remove("bob", id);
        assertThat(bag.rows).isEmpty();

        assertThatThrownBy(() -> service.end("alice", id))
                .isInstanceOf(BusinessException.class).hasMessage("这条经期记录不在你们的空间里");
        PeriodRecord elsewhere = PeriodRecord.restore("p9", "other-space", TODAY.toString(), null, "carol", 0L);
        bag.rows.add(elsewhere);
        assertThatThrownBy(() -> service.remove("alice", "p9"))
                .isInstanceOf(BusinessException.class).hasMessage("这条经期记录不在你们的空间里");
    }

    /** 三个写口都没有看板可回了，闸门与 404 得逐个确认还在。 */
    @Test
    void noSpaceMeansStandardNotFoundAndNoWrites() {
        when(spaceRepository.findActiveByMember("solo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add("solo", TODAY.toString()))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
        assertThatThrownBy(() -> service.end("solo", "p1"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
        assertThatThrownBy(() -> service.remove("solo", "p1"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
        verify(periodRepository, never()).save(any(PeriodRecord.class));
    }

    @Test
    void invalidDateIsTranslatedToFourHundred() {
        stubSpace();
        Bag bag = new Bag().stub();
        assertThatThrownBy(() -> service.add("alice", "2026/10/01"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy-MM-dd");
        assertThat(bag.rows).isEmpty();
    }
}
