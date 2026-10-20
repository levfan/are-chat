package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.intimatelog.IntimateLog;
import com.smart.chat.couple.domain.intimatelog.IntimateLogRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 亲密记录：双方都能记一笔、删一笔。
 * <p>
 * 展示统一走 {@link CoupleCalendarService} 的月历读模型（看板端点已随卡片合并下线），
 * 所以这里只负责落库与闸门，返回 void，由前端重拉当月。
 * 刻意不推 WS 事件：这是两个人的私密账本，不该因为对方点了一下就弹提醒。
 */
@Service
public class CoupleIntimateLogService {

    private final CoupleSpaceRepository spaceRepository;
    private final IntimateLogRepository logRepository;

    public CoupleIntimateLogService(CoupleSpaceRepository spaceRepository, IntimateLogRepository logRepository) {
        this.spaceRepository = spaceRepository;
        this.logRepository = logRepository;
    }

    /** 记一笔：日期不能是将来，类型只收 EDGE/SEX，备注可空。 */
    public void add(String me, String day, String kind, String note) {
        CoupleSpace space = requireSpace(me);
        IntimateLog log = rule(() -> IntimateLog.record(space.id(), day, kind, note, me, LocalDate.now()));
        logRepository.save(log);
    }

    /** 删一笔：双方都能删，记录本身不进回收站。 */
    public void remove(String me, String id) {
        CoupleSpace space = requireSpace(me);
        IntimateLog log = (id == null ? Optional.<IntimateLog>empty() : logRepository.findById(id))
                .filter(row -> space.id().equals(row.spaceId()))
                .orElseThrow(() -> new BusinessException(404, "这条亲密记录不在你们的空间里"));
        logRepository.deleteById(log.id());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
