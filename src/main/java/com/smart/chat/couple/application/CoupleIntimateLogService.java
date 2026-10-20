package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.intimatelog.IntimateLog;
import com.smart.chat.couple.domain.intimatelog.IntimateLogRepository;
import com.smart.chat.couple.domain.intimatelog.IntimateLogs;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 亲密记录：双方都能记一笔、删一笔；本月与累计次数由 {@link IntimateLogs} 读时算。
 * <p>
 * 刻意不推 WS 事件：这是两个人的私密账本，不该因为对方点了一下就弹提醒。
 * 看板只回最近 {@value #RECENT_LIMIT} 条，统计仍按全量算。
 */
@Service
public class CoupleIntimateLogService {

    private static final int RECENT_LIMIT = 30;

    public record IntimateLogVO(String id, String day, String kind, String note, String recorderUser, Long created) {
    }

    public record IntimateBoardVO(List<IntimateLogVO> records, String lastDay, String lastKind, int monthCount,
                                  int monthEdgeCount, int monthSexCount, int totalCount) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final IntimateLogRepository logRepository;

    public CoupleIntimateLogService(CoupleSpaceRepository spaceRepository, IntimateLogRepository logRepository) {
        this.spaceRepository = spaceRepository;
        this.logRepository = logRepository;
    }

    /** 看板：最近 30 条 + 最近一次 + 本月分类型次数 + 累计次数。 */
    public IntimateBoardVO board(String me) {
        CoupleSpace space = requireSpace(me);
        List<IntimateLog> logs = logRepository.findBySpace(space.id());
        IntimateLogs stats = IntimateLogs.of(logs, LocalDate.now());
        List<IntimateLogVO> rows = logs.stream()
                .limit(RECENT_LIMIT)
                .map(log -> new IntimateLogVO(log.id(), log.day(), log.kind(), log.note(), log.recorderUser(),
                        log.created()))
                .toList();
        IntimateLog last = stats.last();
        return new IntimateBoardVO(rows, last == null ? null : last.day(), last == null ? null : last.kind(),
                stats.monthCount(), stats.monthEdgeCount(), stats.monthSexCount(), stats.totalCount());
    }

    /** 记一笔：日期不能是将来，类型只收 EDGE/SEX，备注可空。 */
    public IntimateBoardVO add(String me, String day, String kind, String note) {
        CoupleSpace space = requireSpace(me);
        IntimateLog log = rule(() -> IntimateLog.record(space.id(), day, kind, note, me, LocalDate.now()));
        logRepository.save(log);
        return board(me);
    }

    /** 删一笔：双方都能删，记录本身不进回收站。 */
    public IntimateBoardVO remove(String me, String id) {
        CoupleSpace space = requireSpace(me);
        IntimateLog log = (id == null ? Optional.<IntimateLog>empty() : logRepository.findById(id))
                .filter(row -> space.id().equals(row.spaceId()))
                .orElseThrow(() -> new BusinessException(404, "这条亲密记录不在你们的空间里"));
        logRepository.deleteById(log.id());
        return board(me);
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
