package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.quest.QuestOvertime;
import com.smart.chat.couple.domain.quest.QuestOvertimeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link QuestOvertimeRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 查询复用 {@link CoupleQuestOvertimeMapper} 已有的 default 方法，取数口径与改造前逐字相同；
 * 归属闸门（「那条预报是不是这个空间的」）在这里用 {@code selectById} + 空间过滤复现原 {@code requireOvertime}。
 * <p>
 * {@link #save} 沿用 {@code CoupleSpaceRepositoryAdapter} 的「只回写聚合持有的列」纪律：新预报整行插入
 * （lamp 空串、lampBy null，与 {@link CoupleQuestOvertimePO#of} 一致）；改写只动 until_hour / note / lamp /
 * lamp_by / updated_at，id / space_id / day / from_user / created 一律不碰。
 */
@Component
public class QuestOvertimeRepositoryAdapter implements QuestOvertimeRepository {

    private final CoupleQuestOvertimeMapper overtimeMapper;

    public QuestOvertimeRepositoryAdapter(CoupleQuestOvertimeMapper overtimeMapper) {
        this.overtimeMapper = overtimeMapper;
    }

    @Override
    public Optional<QuestOvertime> findBySpaceAndUserAndDay(String spaceId, String user, String day) {
        return Optional.ofNullable(overtimeMapper.find(spaceId, day, user))
                .map(QuestOvertimeRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<QuestOvertime> findByIdInSpace(String spaceId, String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        CoupleQuestOvertimePO row = overtimeMapper.selectById(id);
        if (row == null || !spaceId.equals(row.getSpaceId())) {
            return Optional.empty();
        }
        return Optional.of(toDomain(row));
    }

    @Override
    public List<QuestOvertime> listByDay(String spaceId, String day) {
        return overtimeMapper.findByDay(spaceId, day).stream().map(QuestOvertimeRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(QuestOvertime overtime) {
        CoupleQuestOvertimePO existing = overtimeMapper.selectById(overtime.id());
        if (existing == null) {
            overtimeMapper.insert(toPo(overtime));
            return;
        }
        applyOwnedFields(existing, overtime);
        overtimeMapper.updateById(existing);
    }

    /** 聚合负责维护的列：预报内容 + 灯卡 + 更新时间（改期与留灯各自只真的改动其中一部分，其余沿用载入值）。 */
    @Override
    public List<QuestOvertime> listBySpace(String spaceId) {
        return overtimeMapper.findBySpace(spaceId).stream().map(QuestOvertimeRepositoryAdapter::toDomain).toList();
    }

    private static void applyOwnedFields(CoupleQuestOvertimePO po, QuestOvertime overtime) {
        po.setUntilHour(overtime.untilHour());
        po.setNote(overtime.note());
        po.setLamp(overtime.lamp());
        po.setLampBy(overtime.lampBy());
        po.setUpdatedAt(overtime.updatedAt());
    }

    private static CoupleQuestOvertimePO toPo(QuestOvertime overtime) {
        CoupleQuestOvertimePO po = new CoupleQuestOvertimePO();
        po.setId(overtime.id());
        po.setSpaceId(overtime.spaceId());
        po.setDay(overtime.day());
        po.setFromUser(overtime.fromUser());
        po.setUntilHour(overtime.untilHour());
        po.setNote(overtime.note());
        po.setLamp(overtime.lamp());
        po.setLampBy(overtime.lampBy());
        po.setCreated(overtime.created());
        po.setUpdatedAt(overtime.updatedAt());
        return po;
    }

    private static QuestOvertime toDomain(CoupleQuestOvertimePO po) {
        return QuestOvertime.restore(po.getId(), po.getSpaceId(), po.getDay(), po.getFromUser(), po.getUntilHour(),
                po.getNote(), po.getLamp(), po.getLampBy(), po.getCreated(), po.getUpdatedAt());
    }
}
