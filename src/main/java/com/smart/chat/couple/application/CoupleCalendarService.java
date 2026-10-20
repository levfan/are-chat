package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.RuleViolation;
import com.smart.chat.couple.domain.intimatelog.IntimateLog;
import com.smart.chat.couple.domain.intimatelog.IntimateLogRepository;
import com.smart.chat.couple.domain.intimatelog.IntimateLogs;
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
 * 亲密日历的读模型：日历卡是经期与亲密记录的唯一入口，所以「这个月铺什么格子、
 * 某一天能删哪条、周期算得怎么样」全在这里一次给齐。
 * <p>
 * 覆盖日由 {@link PeriodRecord#covers} 判（还没记结束日的只到今天），周期推算照 {@link PeriodCycles} 的口径；
 * 前端只按下发的天列表与记录 id 渲染，不自己数区间。写口（经期与亲密的 add/end/remove）
 * 成功后返回的也是这一份，界面拿它整体替换，不用再发第二次请求。
 * 和两张记录卡原来一样：不推 WS 事件，也不参与心动值。
 */
@Service
public class CoupleCalendarService {

    /** 日历上的一条亲密标记：带 id 才删得掉，备注与记录人给「点一天看详情」用。 */
    public record CalendarIntimateVO(String id, String day, String kind, String note, String recorderUser) {
    }

    /** 与本月经期区间有关的一次记录：endDay 为 null 就是还没结束，日历据此给「今天结束了」。 */
    public record CalendarPeriodVO(String id, String startDay, String endDay) {
    }

    public record CoupleCalendarVO(String month, List<String> periodDays, List<String> periodStartDays,
                                   List<String> periodEndDays, List<CalendarPeriodVO> periods,
                                   String predictedNextDay, int averageCycleDays, int cycleSampleCount,
                                   Integer daysUntilNext, int intimateTotal,
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
        List<CalendarPeriodVO> periods = new ArrayList<>();
        String first = viewed.atDay(1).toString();
        String last = viewed.atEndOfMonth().toString();
        String todayText = today.toString();
        for (PeriodRecord record : records) {
            // 与本月的三种相交：压住月初、压住月末、开始日就在这个月里
            if (record.covers(first, todayText) || record.covers(last, todayText)
                    || (record.startDay().compareTo(first) >= 0 && record.startDay().compareTo(last) <= 0)) {
                periods.add(new CalendarPeriodVO(record.id(), record.startDay(), record.endDay()));
            }
        }
        for (LocalDate day = viewed.atDay(1); !day.isAfter(viewed.atEndOfMonth()); day = day.plusDays(1)) {
            String key = day.toString();
            boolean covered = false;
            boolean start = false;
            boolean end = false;
            for (PeriodRecord record : records) {
                covered |= record.covers(key, todayText);
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
        // 累计次数走领域值对象（它按全量算，不受本月列表影响），其余统计都来自本月列表
        int intimateTotal = IntimateLogs.of(logRepository.findBySpace(space.id()), today).totalCount();
        return new CoupleCalendarVO(viewed.toString(), List.copyOf(periodDays), List.copyOf(periodStartDays),
                List.copyOf(periodEndDays), List.copyOf(periods),
                cycles.predictedNextDay() == null ? null : cycles.predictedNextDay().toString(),
                cycles.averageCycleDays(), cycles.sampleCount(), cycles.daysUntilNext(),
                intimateTotal, rows);
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
        return new CalendarIntimateVO(log.id(), log.day(), log.kind(), log.note(), log.recorderUser());
    }
}
