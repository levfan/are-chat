package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 回忆资产·聚合系（F80/F81/F82/F85/F86）：恋爱编年史、考古卡、恋爱问答机、
 * 周年报告、生日回顾。全部基于现有记录聚合，不落新表。
 * 情绪价值设计：时间不是流走的，是攒下来的——编年史让每一年都有名字；
 * 考古卡让你在普通的一天里挖出普通又珍贵的一天。
 */
@Service
public class CoupleChronicleService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleFirstMapper firstMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final CoupleCapsuleMapper capsuleMapper;
    private final CouplePromiseMapper promiseMapper;
    private final CoupleTravelWishMapper travelMapper;
    private final CoupleTruthMapper truthMapper;
    private final CouplePassbookMapper passbookMapper;
    private final CoupleMoodMapper moodMapper;
    private final CoupleQuoteMapper quoteMapper;
    private final UserProfileMapper profileMapper;

    public CoupleChronicleService(CoupleSpaceMapper spaceMapper, CoupleFirstMapper firstMapper,
                                  CoupleAnniversaryMapper anniversaryMapper, CoupleCapsuleMapper capsuleMapper,
                                  CouplePromiseMapper promiseMapper, CoupleTravelWishMapper travelMapper,
                                  CoupleTruthMapper truthMapper, CouplePassbookMapper passbookMapper,
                                  CoupleMoodMapper moodMapper, CoupleQuoteMapper quoteMapper,
                                  UserProfileMapper profileMapper) {
        this.spaceMapper = spaceMapper;
        this.firstMapper = firstMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.capsuleMapper = capsuleMapper;
        this.promiseMapper = promiseMapper;
        this.travelMapper = travelMapper;
        this.truthMapper = truthMapper;
        this.passbookMapper = passbookMapper;
        this.moodMapper = moodMapper;
        this.quoteMapper = quoteMapper;
        this.profileMapper = profileMapper;
    }

    // ========== VO ==========

    /** 编年史事件（day = yyyy-MM-dd）。 */
    public record ChronicleEvent(String day, String type, String title, String detail, String icon) {
    }

    public record ChronicleYearVO(String year, List<ChronicleEvent> events) {
    }

    /** 考古卡：从旧记录里挖出的一天。 */
    public record ArchaeologyCardVO(String kind, String day, long daysAgo, String title, String content) {
    }

    /** 问答机单题（answerIndex 供前端判分，娱乐向）。 */
    public record QuizQuestionVO(String key, String question, List<String> options, int answerIndex) {
    }

    public record ReportItem(String key, String label, String emoji, long value, String unit) {
    }

    /** 周年报告：最近一个周年以来的我们。 */
    public record AnniversaryReportVO(String anniversaryDay, int nthYear, String sinceDay, List<ReportItem> items, String summary) {
    }

    /** 生日回顾：TA 生日那天，历史上都发生过什么。 */
    public record BirthdayLookVO(String partner, String partnerLabel, String birthday, List<ChronicleEvent> events) {
    }

    // ========== F80 恋爱编年史 ==========

    /** 把第一次 / 纪念日 / 胶囊 / 约定兑现 / 旅行打卡 / 真心话存档按年编成史册。 */
    public List<ChronicleYearVO> chronicle(String me) {
        CoupleSpace space = requireSpace(me);
        List<ChronicleEvent> events = new ArrayList<>(collectEvents(space));
        events.sort((a, b) -> b.day().compareTo(a.day()));
        Map<String, List<ChronicleEvent>> byYear = new java.util.LinkedHashMap<>();
        for (ChronicleEvent event : events) {
            byYear.computeIfAbsent(event.day().substring(0, 4), k -> new ArrayList<>()).add(event);
        }
        return byYear.entrySet().stream()
                .map(e -> new ChronicleYearVO(e.getKey(), e.getValue()))
                .toList();
    }

    private List<ChronicleEvent> collectEvents(CoupleSpace space) {
        List<ChronicleEvent> events = new ArrayList<>();
        for (CoupleFirst first : firstMapper.findBySpace(space.getId())) {
            if (first.getFirstDay() != null && !first.getFirstDay().isBlank()) {
                events.add(new ChronicleEvent(first.getFirstDay(), "first", first.getTitle(),
                        first.getNote() == null ? "" : first.getNote(), "🧾"));
            }
        }
        for (CoupleAnniversary row : anniversaryMapper.findBySpace(space.getId())) {
            events.add(new ChronicleEvent(row.getEventDate(), "anniversary", row.getTitle(), "值得记住的日子", "📅"));
        }
        for (CoupleCapsule capsule : capsuleMapper.findBySpace(space.getId())) {
            String day = capsule.getCreated() == null ? capsule.getOpenDay()
                    : toDay(capsule.getCreated());
            events.add(new ChronicleEvent(day, "capsule", "封存了一枚时光胶囊",
                    "约定 " + capsule.getOpenDay() + " 开启", "⏳"));
        }
        for (CouplePromise promise : promiseMapper.findBySpace(space.getId())) {
            if (CouplePromise.STATUS_DONE.equals(promise.getStatus()) && promise.getDoneAt() != null) {
                events.add(new ChronicleEvent(toDay(promise.getDoneAt()), "promise", "兑现了一个约定",
                        promise.getContent(), "🤝"));
            }
        }
        for (CoupleTravelWish wish : travelMapper.findBySpace(space.getId())) {
            if (wish.isVisited() && wish.getVisitedAt() != null) {
                events.add(new ChronicleEvent(toDay(wish.getVisitedAt()), "travel", "一起去过 " + wish.getPlace(),
                        wish.getVisitedNote() == null ? "" : wish.getVisitedNote(), "🧳"));
            }
        }
        for (CoupleTruth truth : truthMapper.findBySpace(space.getId())) {
            events.add(new ChronicleEvent(truth.getDay(), "truth", "真心话：" + shortText(truth.getAnswer(), 40),
                    truth.getQuestion(), "💬"));
        }
        return events;
    }

    // ========== F81 考古卡 ==========

    /** 随机挖一张旧卡片：真心话 / 存折 / 语录 / 心情，越旧越惊喜。 */
    public ArchaeologyCardVO archaeology(String me) {
        CoupleSpace space = requireSpace(me);
        List<ArchaeologyCardVO> pool = new ArrayList<>();
        for (CoupleTruth t : truthMapper.findBySpace(space.getId())) {
            pool.add(new ArchaeologyCardVO("truth", t.getDay(), daysAgo(t.getDay()),
                    "那天 TA 的真心话", t.getAnswer()));
        }
        for (CouplePassbook p : passbookMapper.findBySpace(space.getId())) {
            pool.add(new ArchaeologyCardVO("passbook", p.getDay(), daysAgo(p.getDay()),
                    "那天 TA 往恋爱存折里存了", p.getContent()));
        }
        for (CoupleQuote q : quoteMapper.findBySpace(space.getId())) {
            pool.add(new ArchaeologyCardVO("quote", toDay(q.getCreated()), daysAgo(toDay(q.getCreated())),
                    "册子里收藏的一句话", q.getContent()));
        }
        for (CoupleMood m : moodMapper.findBySpace(space.getId())) {
            if (m.getNote() != null && !m.getNote().isBlank()) {
                pool.add(new ArchaeologyCardVO("mood", m.getMoodDay(), daysAgo(m.getMoodDay()),
                        "那天 TA 的心情随笔", m.getNote()));
            }
        }
        List<ArchaeologyCardVO> old = pool.stream()
                .filter(c -> c.daysAgo() >= 30)
                .toList();
        List<ArchaeologyCardVO> usable = old.isEmpty() ? pool : old;
        if (usable.isEmpty()) {
            throw new BusinessException(404, "考古层还是空的——多记录一些日子，以后挖起来才惊喜");
        }
        return usable.get(ThreadLocalRandom.current().nextInt(usable.size()));
    }

    // ========== F82 恋爱问答机 ==========

    /** 基于真实数据出 2-3 道选择题（正确答案随题返回，前端判分，娱乐向）。 */
    public List<QuizQuestionVO> quiz(String me) {
        CoupleSpace space = requireSpace(me);
        List<QuizQuestionVO> questions = new ArrayList<>();
        List<CoupleFirst> firsts = firstMapper.findBySpace(space.getId()).stream()
                .filter(f -> f.getFirstDay() != null && !f.getFirstDay().isBlank())
                .toList();
        if (!firsts.isEmpty()) {
            CoupleFirst pick = firsts.get(ThreadLocalRandom.current().nextInt(firsts.size()));
            List<String> pool = firsts.stream().map(CoupleFirst::getFirstDay).distinct().toList();
            questions.add(dateQuiz("first:" + pick.getId(), "我们的「" + pick.getTitle() + "」发生在哪一天？",
                    pick.getFirstDay(), pool));
        }
        String annivDay = space.getAnniversary() == null || space.getAnniversary().isBlank()
                ? toDay(space.getCreated()) : space.getAnniversary();
        questions.add(dateQuiz("anniversary", "我们是在哪一天在一起的？", annivDay, List.of(annivDay)));

        long days = daysTogether(space);
        List<String> dayOptions = buildNumberOptions(days, 30, 400);
        int dayAnswer = dayOptions.indexOf(String.valueOf(days));
        questions.add(new QuizQuestionVO("days", "到今天为止，我们已经在一起多少天了？", dayOptions, dayAnswer));
        return questions;
    }

    /** 日期选择题：正确答案 + 干扰项（真实池优先，不足用日期偏移补齐），打乱后定位答案。 */
    private QuizQuestionVO dateQuiz(String key, String question, String correct, List<String> pool) {
        List<String> options = new ArrayList<>();
        options.add(correct);
        for (String candidate : pool) {
            if (options.size() >= 4) {
                break;
            }
            if (!options.contains(candidate)) {
                options.add(candidate);
            }
        }
        LocalDate base = LocalDate.parse(correct);
        while (options.size() < 4) {
            String candidate = base.plusDays(ThreadLocalRandom.current().nextLong(30, 400)).toString();
            if (!options.contains(candidate)) {
                options.add(candidate);
            }
        }
        java.util.Collections.shuffle(options, ThreadLocalRandom.current());
        return new QuizQuestionVO(key, question, options, options.indexOf(correct));
    }

    /** 数字选择题：正确值 + 3 个不重复干扰值。 */
    private List<String> buildNumberOptions(long correct, long minDelta, long maxDelta) {
        List<String> options = new ArrayList<>();
        options.add(String.valueOf(correct));
        while (options.size() < 4) {
            long delta = ThreadLocalRandom.current().nextLong(minDelta, maxDelta);
            long candidate = ThreadLocalRandom.current().nextBoolean() ? correct + delta : Math.max(1, correct - delta);
            String text = String.valueOf(candidate);
            if (!options.contains(text)) {
                options.add(text);
            }
        }
        java.util.Collections.shuffle(options, ThreadLocalRandom.current());
        return options;
    }

    // ========== F85 周年报告 ==========

    /** 最近一个周年以来的「这一年我们」。 */
    public AnniversaryReportVO anniversaryReport(String me) {
        CoupleSpace space = requireSpace(me);
        String annivDay = space.getAnniversary() == null || space.getAnniversary().isBlank()
                ? toDay(space.getCreated()) : space.getAnniversary();
        LocalDate anniv = LocalDate.parse(annivDay);
        LocalDate today = LocalDate.now();
        LocalDate since = anniv.withYear(today.getYear());
        if (since.isAfter(today)) {
            since = since.minusYears(1);
        }
        int nthYear = (int) ChronoUnit.YEARS.between(anniv, today) + 1;
        LocalDate sinceFinal = since;
        long promisesDone = countInDayRange(promiseDoneDays(space), sinceFinal, today);
        long moods = moodMapper.findBySpace(space.getId()).stream()
                .filter(m -> inRange(m.getMoodDay(), sinceFinal, today)).count();
        long truths = truthMapper.findBySpace(space.getId()).stream()
                .filter(t -> inRange(t.getDay(), sinceFinal, today)).count();
        long firsts = firstMapper.findBySpace(space.getId()).stream()
                .filter(f -> f.getFirstDay() != null && inRange(f.getFirstDay(), sinceFinal, today)).count();
        long travels = travelMapper.findBySpace(space.getId()).stream()
                .filter(w -> w.isVisited() && w.getVisitedAt() != null
                        && inRange(toDay(w.getVisitedAt()), sinceFinal, today)).count();
        long deposits = passbookMapper.findBySpace(space.getId()).stream()
                .filter(p -> inRange(p.getDay(), sinceFinal, today)).count();
        List<ReportItem> items = List.of(
                new ReportItem("promises", "兑现的约定", "🤝", promisesDone, "个"),
                new ReportItem("moods", "记录的心情", "🌦️", moods, "天"),
                new ReportItem("truths", "交出的真心话", "💬", truths, "次"),
                new ReportItem("firsts", "解锁的第一次", "🧾", firsts, "个"),
                new ReportItem("travels", "一起走过的地方", "🧳", travels, "个"),
                new ReportItem("deposits", "恋爱存折存款", "💰", deposits, "笔"));
        String summary = "从 " + since + " 到今天，是你们在一起的第 " + nthYear + " 年——"
                + (promisesDone + firsts + travels == 0
                ? "这一年平澹但踏实，下一年多创造一点值得写进史册的事吧"
                : "这一年你们又攒下了 " + (promisesDone + firsts + travels) + " 件值得写进史册的事");
        return new AnniversaryReportVO(annivDay, nthYear, since.toString(), items, summary);
    }

    private List<String> promiseDoneDays(CoupleSpace space) {
        return promiseMapper.findBySpace(space.getId()).stream()
                .filter(p -> CouplePromise.STATUS_DONE.equals(p.getStatus()) && p.getDoneAt() != null)
                .map(p -> toDay(p.getDoneAt()))
                .toList();
    }

    private long countInDayRange(List<String> days, LocalDate since, LocalDate today) {
        return days.stream().filter(d -> inRange(d, since, today)).count();
    }

    // ========== F86 生日回顾 ==========

    /** TA 生日（MM-dd）那天，历史上发生过的事。 */
    public BirthdayLookVO birthdayLook(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        UserProfile profile = profileMapper.selectById(partner);
        String birthday = profile == null ? null : profile.getBirthday();
        if (birthday == null || birthday.isBlank()) {
            throw new BusinessException(404, "TA 还没有填写生日，先让 TA 去资料卡补上吧 🎂");
        }
        String mmdd = birthday.length() >= 10 ? birthday.substring(5) : birthday;
        List<ChronicleEvent> events = new ArrayList<>();
        for (CoupleFirst first : firstMapper.findBySpace(space.getId())) {
            if (first.getFirstDay() != null && first.getFirstDay().endsWith(mmdd)) {
                events.add(new ChronicleEvent(first.getFirstDay(), "first", first.getTitle(),
                        first.getNote() == null ? "" : first.getNote(), "🧾"));
            }
        }
        for (CoupleTruth t : truthMapper.findBySpace(space.getId())) {
            if (t.getDay().endsWith(mmdd)) {
                events.add(new ChronicleEvent(t.getDay(), "truth", "真心话：" + shortText(t.getAnswer(), 40),
                        t.getQuestion(), "💬"));
            }
        }
        for (CouplePassbook p : passbookMapper.findBySpace(space.getId())) {
            if (p.getDay().endsWith(mmdd)) {
                events.add(new ChronicleEvent(p.getDay(), "passbook", "存折里的一笔", p.getContent(), "💰"));
            }
        }
        for (CoupleMood m : moodMapper.findBySpace(space.getId())) {
            if (m.getMoodDay().endsWith(mmdd)) {
                events.add(new ChronicleEvent(m.getMoodDay(), "mood",
                        "那天 TA 的心情：" + CoupleMood.emojiOf(m.getMood()),
                        m.getNote() == null ? "" : m.getNote(), "🌦️"));
            }
        }
        events.sort((a, b) -> b.day().compareTo(a.day()));
        String partnerLabel = partner.equals(space.getUserA()) ? space.getUserA() : partner;
        return new BirthdayLookVO(partner, partnerLabel, birthday, events);
    }

    // ========== 内部工具 ==========

    private boolean inRange(String day, LocalDate since, LocalDate today) {
        if (day == null || day.length() < 10) {
            return false;
        }
        return day.compareTo(since.toString()) >= 0 && day.compareTo(today.toString()) <= 0;
    }

    private long daysAgo(String day) {
        try {
            return Math.max(0, ChronoUnit.DAYS.between(LocalDate.parse(day), LocalDate.now()));
        } catch (Exception e) {
            return 0;
        }
    }

    private long daysTogether(CoupleSpace space) {
        String annivDay = space.getAnniversary() == null || space.getAnniversary().isBlank()
                ? toDay(space.getCreated()) : space.getAnniversary();
        return ChronoUnit.DAYS.between(LocalDate.parse(annivDay), LocalDate.now()) + 1;
    }

    private String toDay(long epochMilli) {
        return java.time.Instant.ofEpochMilli(epochMilli).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
    }

    private String shortText(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
