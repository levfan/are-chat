package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.quest.QuestOvertime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 加班预报仓储适配器：验证「只回写聚合纳管的列」这条纪律，两个方向都要照住。
 * 改期不能把对方留的灯清掉；留灯不能把预报的小时/说明冲掉；created / 归属 / id 一律不碰。
 * 这类 bug 靠聚合整行重建最容易发生，而编译和业务用例都照不出来。
 */
@ExtendWith(MockitoExtension.class)
class QuestOvertimeRepositoryAdapterTest {

    private static final String DAY = "2026-10-05";

    @Mock
    private CoupleQuestOvertimeMapper overtimeMapper;

    @InjectMocks
    private QuestOvertimeRepositoryAdapter adapter;

    private CoupleQuestOvertimePO stored() {
        CoupleQuestOvertimePO po = new CoupleQuestOvertimePO();
        po.setId("q1");
        po.setSpaceId("s1");
        po.setDay(DAY);
        po.setFromUser("alice");
        po.setUntilHour(22);
        po.setNote("加班");
        return po;
    }

    @Test
    void newForecastIsInsertedWithLampLeftEmpty() {
        when(overtimeMapper.selectById(any())).thenReturn(null);
        QuestOvertime forecast = QuestOvertime.forecast("s1", DAY, "alice", 26, "赶年结");

        adapter.save(forecast);

        ArgumentCaptor<CoupleQuestOvertimePO> captor = ArgumentCaptor.forClass(CoupleQuestOvertimePO.class);
        verify(overtimeMapper).insert(captor.capture());
        CoupleQuestOvertimePO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(forecast.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getFromUser()).isEqualTo("alice");
        assertThat(po.getUntilHour()).isEqualTo(23);
        assertThat(po.getNote()).isEqualTo("赶年结");
        assertThat(po.getLamp()).isEmpty();
        assertThat(po.getLampBy()).isNull();
        assertThat(po.getCreated()).isEqualTo(po.getUpdatedAt());
        verify(overtimeMapper, never()).updateById(any(CoupleQuestOvertimePO.class));
    }

    @Test
    void reforecastKeepsExistingLamp() {
        CoupleQuestOvertimePO existing = stored();
        existing.setLamp("灯给你留着");
        existing.setLampBy("bob");
        existing.setCreated(111L);
        existing.setUpdatedAt(111L);
        when(overtimeMapper.selectById("q1")).thenReturn(existing);

        QuestOvertime base = QuestOvertime.restore("q1", "s1", DAY, "alice", 22, "加班", "灯给你留着", "bob", 111L, 111L);
        adapter.save(base.reforecast(26, "改到更晚"));

        ArgumentCaptor<CoupleQuestOvertimePO> captor = ArgumentCaptor.forClass(CoupleQuestOvertimePO.class);
        verify(overtimeMapper).updateById(captor.capture());
        CoupleQuestOvertimePO written = captor.getValue();
        assertThat(written.getUntilHour()).isEqualTo(23);
        assertThat(written.getNote()).isEqualTo("改到更晚");
        assertThat(written.getLamp()).as("改期不清对方留的灯").isEqualTo("灯给你留着");
        assertThat(written.getLampBy()).isEqualTo("bob");
        assertThat(written.getUpdatedAt()).isNotEqualTo(111L);
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(overtimeMapper, never()).insert(any(CoupleQuestOvertimePO.class));
    }

    @Test
    void leaveLampKeepsExistingForecast() {
        CoupleQuestOvertimePO existing = stored();
        existing.setLamp("");
        existing.setLampBy(null);
        existing.setCreated(111L);
        existing.setUpdatedAt(111L);
        when(overtimeMapper.selectById("q1")).thenReturn(existing);

        QuestOvertime base = QuestOvertime.restore("q1", "s1", DAY, "alice", 22, "加班", "", null, 111L, 111L);
        adapter.save(base.leaveLampBy("bob", "到家灯给你留着"));

        ArgumentCaptor<CoupleQuestOvertimePO> captor = ArgumentCaptor.forClass(CoupleQuestOvertimePO.class);
        verify(overtimeMapper).updateById(captor.capture());
        CoupleQuestOvertimePO written = captor.getValue();
        assertThat(written.getLamp()).isEqualTo("到家灯给你留着");
        assertThat(written.getLampBy()).isEqualTo("bob");
        assertThat(written.getUntilHour()).as("留灯不冲掉预报的小时/说明").isEqualTo(22);
        assertThat(written.getNote()).isEqualTo("加班");
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(overtimeMapper, never()).insert(any(CoupleQuestOvertimePO.class));
    }

    @Test
    void queriesMapRowsBackIntoOvertimes() {
        CoupleQuestOvertimePO row = stored();
        row.setLamp("灯");
        row.setLampBy("bob");
        row.setCreated(200L);
        when(overtimeMapper.findByDay("s1", DAY)).thenReturn(List.of(row));
        when(overtimeMapper.find("s1", DAY, "alice")).thenReturn(row);
        when(overtimeMapper.selectById("q1")).thenReturn(row);

        assertThat(adapter.listByDay("s1", DAY)).extracting(QuestOvertime::fromUser).containsExactly("alice");
        Optional<QuestOvertime> byUser = adapter.findBySpaceAndUserAndDay("s1", "alice", DAY);
        assertThat(byUser).isPresent();
        assertThat(byUser.orElseThrow().lampBy()).isEqualTo("bob");
        assertThat(adapter.findByIdInSpace("s2", "q1")).isEmpty();
    }
}
