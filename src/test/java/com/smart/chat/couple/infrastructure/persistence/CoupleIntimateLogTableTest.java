package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.intimatelog.IntimateLog;
import com.smart.chat.couple.domain.intimatelog.IntimateLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V55 亲密记录表的真库测试（H2 MODE=MySQL + Flyway 全量脚本）：
 * 建表、同一天多条、排序与删除这几件事只有打到真表才暴露。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoupleIntimateLogTableTest {

    private static final String SPACE = "intimate_test_space";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Autowired
    private CoupleIntimateLogMapper logMapper;
    @Autowired
    private IntimateLogRepository logRepository;

    @AfterEach
    void cleanUp() {
        logMapper.findBySpace(SPACE).forEach(row -> logMapper.deleteById(row.getId()));
    }

    @Test
    void rowsRoundTripAndSortNewestFirst() {
        IntimateLog older = IntimateLog.record(SPACE, "2026-09-22", "EDGE", "旧的一条", "alice", TODAY);
        IntimateLog newer = IntimateLog.record(SPACE, "2026-10-20", "SEX", null, "bob", TODAY);
        logRepository.save(older);
        logRepository.save(newer);

        List<IntimateLog> rows = logRepository.findBySpace(SPACE);
        assertThat(rows).extracting(IntimateLog::day).containsExactly("2026-10-20", "2026-09-22");
        assertThat(rows.get(0).kind()).isEqualTo(IntimateLog.KIND_SEX);
        assertThat(rows.get(1).note()).isEqualTo("旧的一条");
        assertThat(rows.get(1).recorderUser()).isEqualTo("alice");
    }

    @Test
    void theSameDayCanHoldMoreThanOneRow() {
        logRepository.save(IntimateLog.record(SPACE, "2026-10-20", "EDGE", null, "alice", TODAY));
        logRepository.save(IntimateLog.record(SPACE, "2026-10-20", "SEX", null, "bob", TODAY));

        assertThat(logRepository.findBySpace(SPACE)).hasSize(2);
    }

    /** 日历按月取数：区间是左含右不含，同一天按记录时刻正序——真表上的边界只有打库才验得出来。 */
    @Test
    void rangeQueryKeepsOneMonthAndSortsAscending() {
        logRepository.save(IntimateLog.restore("rng_last", SPACE, "2026-08-31", "SEX", null, "alice", 1L));
        logRepository.save(IntimateLog.restore("rng_next", SPACE, "2026-10-01", "SEX", null, "bob", 2L));
        logRepository.save(IntimateLog.restore("rng_late", SPACE, "2026-09-05", "EDGE", "后记的", "bob", 300L));
        logRepository.save(IntimateLog.restore("rng_early", SPACE, "2026-09-05", "SEX", null, "alice", 100L));

        List<IntimateLog> september = logRepository.findInRange(SPACE, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1));

        assertThat(september).extracting(IntimateLog::id).containsExactly("rng_early", "rng_late");
        assertThat(september).allSatisfy(row -> assertThat(row.day()).startsWith("2026-09"));
    }

    @Test
    void deletingOneRowLeavesTheRestUntouched() {
        IntimateLog kept = IntimateLog.record(SPACE, "2026-10-19", "EDGE", null, "alice", TODAY);
        IntimateLog removed = IntimateLog.record(SPACE, "2026-10-18", "SEX", null, "bob", TODAY);
        logRepository.save(kept);
        logRepository.save(removed);

        logRepository.deleteById(removed.id());

        assertThat(logRepository.findBySpace(SPACE)).extracting(IntimateLog::id).containsExactly(kept.id());
        assertThat(logRepository.findById(removed.id())).isEmpty();
    }
}
