package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.period.PeriodCycles;
import com.smart.chat.couple.domain.period.PeriodRecord;
import com.smart.chat.couple.domain.period.PeriodRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 经期记录：双方都能记开始日、补结束日、删错记的一条。
 * <p>
 * 看板里的平均周期与下次日期全部由 {@link PeriodCycles} 读时算，表里没有缓存列；
 * 这里只负责取数、归属闸门与把领域对象翻成 VO。刻意不推 WS 事件：这是两个人的私密记录，
 * 不该因为对方点了一下就弹提醒。
 */
@Service
public class CouplePeriodService {

    public record PeriodRecordVO(String id, String startDay, String endDay, String recorderUser, Long created) {
    }

    public record PeriodBoardVO(List<PeriodRecordVO> records, int averageCycleDays, String predictedNextDay,
                                Integer daysUntilNext, int cycleSampleCount) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final PeriodRepository periodRepository;

    public CouplePeriodService(CoupleSpaceRepository spaceRepository, PeriodRepository periodRepository) {
        this.spaceRepository = spaceRepository;
        this.periodRepository = periodRepository;
    }

    /** 看板：最近一次在最前，附平均周期与下次预计（没有记录时预测字段为 null）。 */
    public PeriodBoardVO board(String me) {
        CoupleSpace space = requireSpace(me);
        List<PeriodRecord> records = periodRepository.findBySpace(space.id());
        PeriodCycles cycles = PeriodCycles.of(records, LocalDate.now());
        List<PeriodRecordVO> rows = records.stream()
                .map(record -> new PeriodRecordVO(record.id(), record.startDay(), record.endDay(),
                        record.recorderUser(), record.created()))
                .toList();
        return new PeriodBoardVO(rows, cycles.averageCycleDays(),
                cycles.predictedNextDay() == null ? null : cycles.predictedNextDay().toString(),
                cycles.daysUntilNext(), cycles.sampleCount());
    }

    /** 记一个开始日：同一天只能有一条，未来的日子不收。 */
    public PeriodBoardVO add(String me, String startDay) {
        CoupleSpace space = requireSpace(me);
        PeriodRecord record = rule(() -> PeriodRecord.start(space.id(), startDay, me, LocalDate.now()));
        if (periodRepository.existsByStart(space.id(), record.startDay())) {
            throw new BusinessException(400, "这一天已经记过啦，看看下面的记录 🩸");
        }
        periodRepository.save(record);
        return board(me);
    }

    /** 补结束日：只能记今天，且这条记录还没结束过。 */
    public PeriodBoardVO end(String me, String id) {
        CoupleSpace space = requireSpace(me);
        PeriodRecord record = requireRecord(space, id);
        guard(() -> record.markEnded(LocalDate.now()));
        periodRepository.save(record);
        return board(me);
    }

    /** 删一条错记：双方都能删，记录本身不进回收站。 */
    public PeriodBoardVO remove(String me, String id) {
        CoupleSpace space = requireSpace(me);
        PeriodRecord record = requireRecord(space, id);
        periodRepository.deleteById(record.id());
        return board(me);
    }

    private PeriodRecord requireRecord(CoupleSpace space, String id) {
        return (id == null ? Optional.<PeriodRecord>empty() : periodRepository.findById(id))
                .filter(record -> space.id().equals(record.spaceId()))
                .orElseThrow(() -> new BusinessException(404, "这条经期记录不在你们的空间里"));
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
