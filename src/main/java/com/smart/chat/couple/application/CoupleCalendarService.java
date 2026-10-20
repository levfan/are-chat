package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.RuleViolation;
import com.smart.chat.couple.domain.intimatelog.IntimateLog;
import com.smart.chat.couple.domain.intimatelog.IntimateLogRepository;
import com.smart.chat.couple.domain.period.PeriodCycles;
import com.smart.chat.couple.domain.period.PeriodRecord;
import com.smart.chat.couple.domain.period.PeriodRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 亲密日历的读模型：把「这个月哪几天在经期里、哪几天记了边缘/性生活」一次算齐给日历铺格子。
 * <p>
 * 经期与亲密记录各有一套自己的看板，但日历同时要看两者——让前端去拼就得再打一次
 * {@code /period/board}，同一页出现两份经期真相。所以这里跨两个聚合出一份只读结果，
 * 覆盖日由 {@link PeriodRecord#covers} 判（还没记结束日的只到今天），周期推算照 {@link PeriodCycles} 的口径。
 * 和两张记录卡一样：不推 WS 事件，也不参与心动值。
 */
@Service
public class CoupleCalendarService {

    /** 日历上的一条亲密标记：够铺图标和展开当天详情用，删除仍回记录卡。 */
    public record CalendarIntimateVO(String day, String kind, String note, String recorderUser) {
    }

    public record CoupleCalendarVO(String month, List<String> periodDays, List<String> periodStartDays,
                                   List<String> periodEndDays, String predictedNextDay,
                                   List<CalendarIntimateVO> intimate) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final PeriodRepository periodRepository;
    private final IntimateLogRepository logRepository;

    public CoupleCalendarService(CoupleSpaceRepository spaceRepository, PeriodRepository periodRepository,
                                 IntimateLogRepository logRepository) {
        this.spaceRepository = spaceRepository;
        this.periodRepository = periodRepository;
        this.logRepository = logRepository;
    }

    /** 整月日历：month 必须 yyyy-MM；未来的月份正常返回（经期覆盖日为空，只可能带预测日）。 */
    public CoupleCalendarVO month(String me, String month) {
        CoupleSpace space = requireSpace(me);
        YearMonth viewed = rule(() -> requireMonth(month));
        LocalDate today = LocalDate.now();
        List<PeriodRecord> records = periodRepository.findBySpace(space.id());
        PeriodCycles cycles = PeriodCycles.of(records, today);

        List<String> periodDays = new ArrayList<>();
        List<String> periodStartDays = new ArrayList<>();
        List<String> periodEndDays = new ArrayList<>();
        for (LocalDate day = viewed.atDay(1); !day.isAfter(viewed.atEndOfMonth()); day = day.plusDays(1)) {
            String key = day.toString();
            boolean covered = false;
            boolean start = false;
            boolean end = false;
            for (PeriodRecord record : records) {
                covered |= record.covers(key, today.toString());
                start |= record.startsOn(key);
                end |= record.endsOn(key);
            }
            if (covered) {
                periodDays.add(key);
            }
            if (start) {
                periodStartDays.add(key);
            }
            if (end) {
                periodEndDays.add(key);
            }
        }

        List<CalendarIntimateVO> rows = logRepository
                .findInRange(space.id(), viewed.atDay(1), viewed.plusMonths(1).atDay(1)).stream()
                .map(CoupleCalendarService::toRow)
                .toList();
        return new CoupleCalendarVO(viewed.toString(), List.copyOf(periodDays), List.copyOf(periodStartDays),
                List.copyOf(periodEndDays),
                cycles.predictedNextDay() == null ? null : cycles.predictedNextDay().toString(), rows);
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private static YearMonth requireMonth(String text) {
        if (text == null || text.isBlank()) {
            throw new RuleViolation("要看哪个月呀？给我 yyyy-MM 这样的");
        }
        try {
            return YearMonth.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new RuleViolation("月份得是 yyyy-MM 这样的");
        }
    }

    private static CalendarIntimateVO toRow(IntimateLog log) {
        return new CalendarIntimateVO(log.day(), log.kind(), log.note(), log.recorderUser());
    }
}
