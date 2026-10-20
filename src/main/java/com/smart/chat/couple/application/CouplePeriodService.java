package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.period.PeriodCycles;
import com.smart.chat.couple.domain.period.PeriodRecord;
import com.smart.chat.couple.domain.period.PeriodRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 经期记录：双方都能记开始日、补结束日、删错记的一条。
 * <p>
 * 平均周期与下次日期全部由 {@link PeriodCycles} 读时算，表里没有缓存列，也不再有独立的看板端点——
 * 展示统一走 {@link CoupleCalendarService} 的月历读模型，所以这里的三个写口只负责落库与闸门，
 * 返回 void，由前端重拉当月。刻意不推 WS 事件：这是两个人的私密记录，不该因为对方点了一下就弹提醒。
 */
@Service
public class CouplePeriodService {

    private final CoupleSpaceRepository spaceRepository;
    private final PeriodRepository periodRepository;

    public CouplePeriodService(CoupleSpaceRepository spaceRepository, PeriodRepository periodRepository) {
        this.spaceRepository = spaceRepository;
        this.periodRepository = periodRepository;
    }

    /** 记一个开始日：同一天只能有一条，未来的日子不收。 */
    public void add(String me, String startDay) {
        CoupleSpace space = requireSpace(me);
        PeriodRecord record = rule(() -> PeriodRecord.start(space.id(), startDay, me, LocalDate.now()));
        if (periodRepository.existsByStart(space.id(), record.startDay())) {
            throw new BusinessException(400, "这一天已经记过啦，看看下面的记录 🩸");
        }
        periodRepository.save(record);
    }

    /** 补结束日：只能记今天，且这条记录还没结束过。 */
    public void end(String me, String id) {
        CoupleSpace space = requireSpace(me);
        PeriodRecord record = requireRecord(space, id);
        guard(() -> record.markEnded(LocalDate.now()));
        periodRepository.save(record);
    }

    /** 删一条错记：双方都能删，记录本身不进回收站。 */
    public void remove(String me, String id) {
        CoupleSpace space = requireSpace(me);
        PeriodRecord record = requireRecord(space, id);
        periodRepository.deleteById(record.id());
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
