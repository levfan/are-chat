package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.period.PeriodRecord;
import com.smart.chat.couple.domain.period.PeriodRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V54 经期记录表的真库测试（H2 MODE=MySQL + Flyway 全量脚本）：
 * 建表、唯一键（一天一次）、结束日的往返更新这三件事只有打到真表才暴露。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouplePeriodTableTest {

    private static final String SPACE = "period_test_space";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Autowired
    private CouplePeriodMapper periodMapper;
    @Autowired
    private PeriodRepository periodRepository;

    @AfterEach
    void cleanUp() {
        periodMapper.findBySpace(SPACE).forEach(row -> periodMapper.deleteById(row.getId()));
    }

    @Test
    void rowsRoundTripAndSortNewestFirst() {
        PeriodRecord older = PeriodRecord.start(SPACE, "2026-09-22", "alice", TODAY);
        PeriodRecord newer = PeriodRecord.start(SPACE, "2026-10-20", "bob", TODAY);
        periodRepository.save(newer);
        periodRepository.save(older);

        List<PeriodRecord> rows = periodRepository.findBySpace(SPACE);
        assertThat(rows).extracting(PeriodRecord::startDay).containsExactly("2026-10-20", "2026-09-22");
        assertThat(rows.get(0).recorderUser()).isEqualTo("bob");
        assertThat(periodRepository.existsByStart(SPACE, "2026-09-22")).isTrue();
        assertThat(periodRepository.existsByStart(SPACE, "2026-09-23")).isFalse();
    }

    @Test
    void uniqueKeyBlocksTheSameStartDayTwice() {
        periodRepository.save(PeriodRecord.start(SPACE, "2026-10-20", "alice", TODAY));

        PeriodRecord duplicate = PeriodRecord.start(SPACE, "2026-10-20", "bob", TODAY);
        assertThatThrownBy(() -> periodRepository.save(duplicate)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void endingARecordOnlyRewritesTheEndDay() {
        PeriodRecord record = PeriodRecord.start(SPACE, "2026-10-18", "alice", TODAY);
        periodRepository.save(record);

        record.markEnded(TODAY);
        periodRepository.save(record);

        PeriodRecord reloaded = periodRepository.findById(record.id()).orElseThrow();
        assertThat(reloaded.startDay()).isEqualTo("2026-10-18");
        assertThat(reloaded.endDay()).isEqualTo("2026-10-20");
        assertThat(reloaded.recorderUser()).isEqualTo("alice");
    }
}
