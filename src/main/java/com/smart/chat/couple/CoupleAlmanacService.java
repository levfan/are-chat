package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 夫妻老黄历（F250-F259，批次二十一）：节气跟风、节气过法、择吉日、农历生日换算、
 * 节日家档、节气手账、生肖年运、长假倒数、反仪式感日、一年日子小结。
 * 情绪价值设计：把「跟着时令过日子」变成两个人的共同仪式——跟风不丢人，一起过才浪漫；
 * 而每年三天的「什么都不做」，是给仪式感的留白。
 */
@Service
public class CoupleAlmanacService {

    static final int NOTE_MAX = 140;
    static final int PLAN_MAX = CoupleFestivalPlan.PLAN_MAX;
    static final int LUCKY_UPCOMING = 5;
    static final int WISH_WINDOW_DAYS = 60;
    static final int LUNAR_LOOKAHEAD_YEARS = 10;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleTermCheckMapper checkMapper;
    private final CoupleTermRitualMapper ritualMapper;
    private final CoupleLuckyDayMapper luckyMapper;
    private final CoupleFestivalPlanMapper festivalMapper;
    private final CoupleTermNoteMapper noteMapper;
    private final CoupleHolidayWishMapper wishMapper;
    private final CoupleNormalDayMapper normalMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final UserProfileMapper profileMapper;
    private final ImPushService push;

    public CoupleAlmanacService(CoupleSpaceMapper spaceMapper, CoupleTermCheckMapper checkMapper,
                                CoupleTermRitualMapper ritualMapper, CoupleLuckyDayMapper luckyMapper,
                                CoupleFestivalPlanMapper festivalMapper, CoupleTermNoteMapper noteMapper,
                                CoupleHolidayWishMapper wishMapper, CoupleNormalDayMapper normalMapper,
                                CoupleAnniversaryMapper anniversaryMapper, UserProfileMapper profileMapper,
                                ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.checkMapper = checkMapper;
        this.ritualMapper = ritualMapper;
        this.luckyMapper = luckyMapper;
        this.festivalMapper = festivalMapper;
        this.noteMapper = noteMapper;
        this.wishMapper = wishMapper;
        this.normalMapper = normalMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.profileMapper = profileMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record CheckUserVO(String fromUser, String note, boolean mine) {
    }

    public record TermTodayVO(String term, String nextTerm, Integer nextDays, List<CheckUserVO> todayChecks) {
    }

    public record RitualVO(String id, String term, String content, boolean mine, String lastDoneYear) {
    }

    public record LuckyVO(String id, String day, String matter, String comment, boolean mine, boolean confirmed) {
    }

    public record FestivalVO(String key, String label, String day, String mine, String partner) {
    }

    public record NoteVO(String term, String mine, String partner) {
    }

    public record HolidayVO(String key, String name, String day, Integer daysLeft,
                            String wish, String wishedBy, String appendedBy) {
    }

    public record NormalVO(boolean today, List<String> days) {
    }

    public record LunarVO(String id, String title, String lunarMd, List<String> nextSolars) {
    }

    public record TodayVO(String day, String year, TermTodayVO term, List<RitualVO> rituals, List<LuckyVO> lucky,
                          List<FestivalVO> festivals, List<NoteVO> notes, HolidayVO holiday, NormalVO normal,
                          List<LunarVO> lunar) {
    }

    public record YearVO(String year, Integer checksDone, Integer notesDone, Integer ritualsTotal,
                         Integer ritualsDone, Integer luckyCount, Integer festivalPlans,
                         Integer normalDays, List<String> scroll) {
    }

    public record ZodiacVO(String zodiacMine, String zodiacPartner, String fortune) {
    }

    // ========== 读：今日老黄历总览 ==========

    /** 今日岁时总览（节气/过法/吉日/家档/手账/长假/放空/农历换算一次拉齐）。 */
    public TodayVO today(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String year = String.valueOf(now.getYear());
        String partner = space.partnerOf(me);

        String term = CoupleTermBank.termOfToday(now);
        String nextTerm = CoupleTermBank.nextTerm(now);
        Integer nextDays = null;
        if (nextTerm != null) {
            LocalDate nd = CoupleTermBank.termOn(nextTerm, now.getYear());
            if (nd != null && !nd.isBefore(now)) {
                nextDays = (int) ChronoUnit.DAYS.between(now, nd);
            } else {
                LocalDate nd2 = CoupleTermBank.termOn(nextTerm, now.getYear() + 1);
                nextDays = nd2 == null ? null : (int) ChronoUnit.DAYS.between(now, nd2);
            }
        }
        List<CheckUserVO> checks = new ArrayList<>();
        if (term != null) {
            CoupleTermCheck mine = checkMapper.find(space.getId(), term, year, me);
            CoupleTermCheck other = checkMapper.find(space.getId(), term, year, partner);
            if (mine != null) {
                checks.add(new CheckUserVO(me, mine.getNote(), true));
            }
            if (other != null) {
                checks.add(new CheckUserVO(partner, other.getNote(), false));
            }
        }
        TermTodayVO termVo = new TermTodayVO(term, nextTerm, nextDays, checks);

        List<RitualVO> rituals = new ArrayList<>();
        for (CoupleTermRitual r : ritualMapper.findBySpace(space.getId())) {
            if (term != null && term.equals(r.getTerm())) {
                rituals.add(toRitualVO(r, me));
            }
        }

        List<LuckyVO> lucky = new ArrayList<>();
        for (CoupleLuckyDay l : luckyMapper.findUpcoming(space.getId(), day)) {
            if (lucky.size() < LUCKY_UPCOMING) {
                lucky.add(toLuckyVO(l, me));
            }
        }

        Map<String, Map<String, String>> plansByFestival = new HashMap<>();
        for (CoupleFestivalPlan p : festivalMapper.findByYear(space.getId(), year)) {
            plansByFestival.computeIfAbsent(p.getFestival(), k -> new HashMap<>()).put(p.getFromUser(), p.getPlan());
        }
        List<FestivalVO> festivals = new ArrayList<>();
        for (String key : CoupleTermBank.FESTIVALS) {
            String fDay = festivalDay(key, now, space);
            if (fDay == null) {
                continue;
            }
            Map<String, String> got = plansByFestival.getOrDefault(key, Map.of());
            festivals.add(new FestivalVO(key, CoupleTermBank.FESTIVAL_LABEL.get(key), fDay,
                    got.getOrDefault(me, ""), got.getOrDefault(partner, "")));
        }

        Map<String, Map<String, String>> notesByTerm = new HashMap<>();
        for (CoupleTermNote n : noteMapper.findByYear(space.getId(), year)) {
            notesByTerm.computeIfAbsent(n.getTerm(), k -> new HashMap<>()).put(n.getFromUser(), n.getText());
        }
        List<NoteVO> notes = new ArrayList<>();
        for (String t : orderedTermsOfYear(now)) {
            Map<String, String> got = notesByTerm.getOrDefault(t, Map.of());
            String mineText = got.getOrDefault(me, "");
            String partnerText = got.getOrDefault(partner, "");
            if (!mineText.isEmpty() || !partnerText.isEmpty() || t.equals(term)) {
                notes.add(new NoteVO(t, mineText, partnerText));
            }
        }

        HolidayVO holidayVo = null;
        String[] nh = CoupleTermBank.nextHoliday(now, WISH_WINDOW_DAYS);
        if (nh != null) {
            CoupleHolidayWish w = wishMapper.find(space.getId(), nh[0]);
            holidayVo = new HolidayVO(nh[0], nh[1], nh[2], Integer.parseInt(nh[3]),
                    w == null ? "" : w.getWish(), w == null ? "" : w.getWishedBy(), w == null ? "" : w.getAppendedBy());
        }

        List<String> normalDays = new ArrayList<>();
        boolean normalToday = false;
        for (CoupleNormalDay d : normalMapper.findByYear(space.getId(), year)) {
            normalDays.add(d.getDay());
            if (day.equals(d.getDay())) {
                normalToday = true;
            }
        }

        List<LunarVO> lunar = new ArrayList<>();
        for (CoupleAnniversary a : anniversaryMapper.findBySpace(space.getId())) {
            if (!CoupleAnniversary.CALENDAR_LUNAR.equals(a.getCalendarType())
                    || a.getLunarMd() == null || a.getLunarMd().length() != 4) {
                continue;
            }
            int lm = Integer.parseInt(a.getLunarMd().substring(0, 2));
            int ld = Integer.parseInt(a.getLunarMd().substring(2));
            List<String> solars = new ArrayList<>();
            for (int y = now.getYear(); y <= now.getYear() + LUNAR_LOOKAHEAD_YEARS && solars.size() < 5; y++) {
                LocalDate s = CoupleTermBank.lunarToSolar(y, lm, ld, false);
                if (s != null && !s.isBefore(now)) {
                    solars.add(s.toString());
                }
            }
            lunar.add(new LunarVO(a.getId(), a.getTitle(), a.getLunarMd(), solars));
        }

        return new TodayVO(day, year, termVo, rituals, lucky, festivals, notes, holidayVo,
                new NormalVO(normalToday, normalDays), lunar);
    }

    // ========== F250 节气跟风 ==========

    /** 节气当日一键跟风；放空日挡打卡；双跟风推 both。 */
    public TodayVO termCheck(String me, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String term = CoupleTermBank.termOfToday(now);
        if (term == null) {
            throw new BusinessException(400, "今天不是节气日，跟风要趁当天 🔕");
        }
        guardNormalDay(space, now);
        String year = String.valueOf(now.getYear());
        if (checkMapper.find(space.getId(), term, year, me) != null) {
            throw new BusinessException(400, "这个节气你已经跟过风啦，等下一个");
        }
        String text = note == null ? "" : note.trim();
        if (text.length() > NOTE_MAX) {
            throw new BusinessException(400, "晒的一句话最多 140 字，留点意犹未尽");
        }
        checkMapper.insert(CoupleTermCheck.of(space.getId(), term, year, me, text));
        String partner = space.partnerOf(me);
        if (checkMapper.find(space.getId(), term, year, partner) != null) {
            push.pushCoupleEventBoth("term-check-both", me, space.getUserA(), space.getUserB(),
                    pickLine(space.getId() + "|checkline|" + term + year) + "｜「" + term + "」两人都在场");
        } else {
            push.pushCoupleEvent("term-check", me, partner, "TA 在「" + term + "」跟了风，快去凑一对 🌿");
        }
        return today(me);
    }

    // ========== F251 节气过法 ==========

    /** 给节气写过法（每节气 ≤2 条），推 TA。 */
    public TodayVO addRitual(String me, String term, String content) {
        CoupleSpace space = requireSpace(me);
        if (term == null || !CoupleTermBank.TERMS.contains(term)) {
            throw new BusinessException(400, "节气名不对，二十四个里挑一个吧");
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty() || text.length() > CoupleTermRitual.CONTENT_MAX) {
            throw new BusinessException(400, "过法要写，最多 80 字");
        }
        if (ritualMapper.findByTerm(space.getId(), term).size() >= CoupleTermRitual.RITUAL_MAX) {
            throw new BusinessException(400, "每个节气最多记 2 条过法，贪多嚼不烂");
        }
        if (ritualMapper.findByTerm(space.getId(), term).stream()
                .anyMatch(r -> r.getContent().equals(text))) {
            throw new BusinessException(400, "这条过法已经记过了");
        }
        ritualMapper.insert(CoupleTermRitual.of(space.getId(), term, text, me));
        push.pushCoupleEvent("term-ritual", me, space.partnerOf(me),
                "TA 给「" + term + "」定了过法：" + text + " 🧂");
        return today(me);
    }

    /** 划掉过法（谁写的谁划）。 */
    public TodayVO removeRitual(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleTermRitual row = requireRitual(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "这条过法是 TA 写的，让 TA 自己划");
        }
        ritualMapper.deleteById(row.getId());
        return today(me);
    }

    /** 节气当日打卡过法；全部打满推 both。 */
    public TodayVO markRitual(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleTermRitual row = requireRitual(space, id);
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        if (row.doneThisYear(year)) {
            return today(me);
        }
        LocalDate td = CoupleTermBank.termOn(row.getTerm(), now.getYear());
        if (td == null || Math.abs(ChronoUnit.DAYS.between(td, now)) > 1) {
            throw new BusinessException(400, "「" + row.getTerm() + "」的过法要在那天打，到时候再来");
        }
        guardNormalDay(space, now);
        row.setLastDoneYear(year);
        ritualMapper.updateById(row);
        String partner = space.partnerOf(me);
        List<CoupleTermRitual> siblings = ritualMapper.findByTerm(space.getId(), row.getTerm());
        boolean all = siblings.stream().allMatch(r -> r.doneThisYear(year));
        if (all) {
            push.pushCoupleEventBoth("term-ritual-all", me, space.getUserA(), space.getUserB(),
                    "「" + row.getTerm() + "」的过法全部打勾，这个节气被我们认真过掉了 ✅");
        } else {
            push.pushCoupleEvent("term-ritual-done", me, partner,
                    "TA 把「" + row.getTerm() + "」的过法打勾了：" + row.getContent());
        }
        return today(me);
    }

    // ========== F252 择吉日 ==========

    /** 为大事择吉日（Bank 出点评），推 TA 来双盖章。 */
    public TodayVO lucky(String me, String day, String matter) {
        CoupleSpace space = requireSpace(me);
        String d = normalizeDay(day);
        LocalDate today = LocalDate.now();
        if (d.compareTo(today.toString()) < 0) {
            throw new BusinessException(400, "吉日要往后挑，过去的日子已经是我们的了");
        }
        String text = matter == null ? "" : matter.trim();
        if (text.isEmpty() || text.length() > CoupleLuckyDay.MATTER_MAX) {
            throw new BusinessException(400, "大事要写清楚，最多 60 字");
        }
        if (luckyMapper.find(space.getId(), d, text) != null) {
            throw new BusinessException(400, "这天这事已经择过吉日了");
        }
        String comment = CoupleTermBank.luckyComment(CoupleRitualBank.stableHash(space.getId() + "|" + d + "|" + text), text);
        luckyMapper.insert(CoupleLuckyDay.of(space.getId(), d, text, me, comment));
        push.pushCoupleEvent("term-lucky", me, space.partnerOf(me),
                "TA 为「" + text + "」择了吉日 " + d + "，等你盖章 🧧");
        return today(me);
    }

    /** 对方给吉日双盖章。 */
    public TodayVO confirmLucky(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleLuckyDay row = requireLucky(space, id);
        if (row.confirmedFlag()) {
            return today(me);
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己择的日，要等 TA 来盖章");
        }
        row.setConfirmed(1);
        row.setConfirmedBy(me);
        luckyMapper.updateById(row);
        push.pushCoupleEventBoth("term-lucky-confirmed", me, space.getUserA(), space.getUserB(),
                "吉日 " + row.getDay() + " 双盖章：「" + row.getMatter() + "」，到时谁都不许放鸽子 🤝");
        return today(me);
    }

    // ========== F254 节日家档 ==========

    /** 写某节日今年的过法（可改写；双案互见推 both）。 */
    public TodayVO festival(String me, String festival, String year, String plan) {
        CoupleSpace space = requireSpace(me);
        if (festival == null || !CoupleTermBank.FESTIVALS.contains(festival)) {
            throw new BusinessException(400, "这八个节日里挑一个：元旦/除夕/情人节/520/七夕/中秋/国庆/周年月");
        }
        String y = normalizeYear(year);
        String text = plan == null ? "" : plan.trim();
        if (text.isEmpty() || text.length() > PLAN_MAX) {
            throw new BusinessException(400, "过法要写，最多 200 字");
        }
        CoupleFestivalPlan exist = festivalMapper.find(space.getId(), festival, y, me);
        if (exist != null) {
            exist.setPlan(text);
            exist.setUpdatedAt(System.currentTimeMillis());
            festivalMapper.updateById(exist);
            return today(me);
        }
        festivalMapper.insert(CoupleFestivalPlan.of(space.getId(), festival, y, me, text));
        String partner = space.partnerOf(me);
        if (festivalMapper.find(space.getId(), festival, y, partner) != null) {
            push.pushCoupleEventBoth("term-festival-both", me, space.getUserA(), space.getUserB(),
                    CoupleTermBank.FESTIVAL_LABEL.get(festival) + " 两边方案都到位，到日拆盲盒 🎁");
        } else {
            push.pushCoupleEvent("term-festival", me, partner,
                    "TA 把 " + CoupleTermBank.FESTIVAL_LABEL.get(festival) + " 怎么过写好了，该你交卷");
        }
        return today(me);
    }

    // ========== F255 节气手账 ==========

    /** 给某节气记一件小事（本人可改写）。 */
    public TodayVO note(String me, String term, String year, String text) {
        CoupleSpace space = requireSpace(me);
        if (term == null || !CoupleTermBank.TERMS.contains(term)) {
            throw new BusinessException(400, "节气名不对，二十四个里挑一个吧");
        }
        String y = normalizeYear(year);
        String content = text == null ? "" : text.trim();
        if (content.isEmpty() || content.length() > NOTE_MAX) {
            throw new BusinessException(400, "手账要写满一句，最多 140 字");
        }
        CoupleTermNote exist = noteMapper.find(space.getId(), term, y, me);
        if (exist != null) {
            exist.setText(content);
            exist.setUpdatedAt(System.currentTimeMillis());
            noteMapper.updateById(exist);
            return today(me);
        }
        noteMapper.insert(CoupleTermNote.of(space.getId(), term, y, me, content));
        push.pushCoupleEvent("term-note", me, space.partnerOf(me),
                "TA 在「" + term + "」的手账写了一笔，来看看 📔");
        return today(me);
    }

    // ========== F257 长假愿望 ==========

    /** 给最近的长假写愿望：首写或补写，补写推 both。 */
    public TodayVO holidayWish(String me, String wish) {
        CoupleSpace space = requireSpace(me);
        String text = wish == null ? "" : wish.trim();
        if (text.isEmpty() || text.length() > CoupleHolidayWish.WISH_MAX) {
            throw new BusinessException(400, "愿望要写，最多 200 字");
        }
        String[] nh = CoupleTermBank.nextHoliday(LocalDate.now(), WISH_WINDOW_DAYS);
        if (nh == null) {
            throw new BusinessException(400, "最近 " + WISH_WINDOW_DAYS + " 天没有法定长假，先把日子过稳");
        }
        CoupleHolidayWish row = wishMapper.find(space.getId(), nh[0]);
        if (row == null) {
            wishMapper.insert(CoupleHolidayWish.of(space.getId(), nh[0], nh[2], me, text));
            push.pushCoupleEvent("term-wish", me, space.partnerOf(me),
                    "TA 先写下了 " + nh[1] + " 怎么过，等你补一句 ✍️");
            return today(me);
        }
        if (!row.hasAppend() && !me.equals(row.getWishedBy())) {
            row.setWish(row.getWish() + "\n——\n" + text);
            row.setAppendedBy(me);
            row.setUpdatedAt(System.currentTimeMillis());
            wishMapper.updateById(row);
            push.pushCoupleEventBoth("term-wish-append", me, space.getUserA(), space.getUserB(),
                    nh[1] + "（" + row.getDay() + "）的过法凑齐了两段，收进行程 🧳");
            return today(me);
        }
        if (me.equals(row.getWishedBy())) {
            String kept = row.hasAppend() ? text + "\n——\n" + row.getWish().split("\n——\n", 2)[1] : text;
            row.setWish(kept);
        } else {
            String head = row.getWish().split("\n——\n", 2)[0];
            row.setWish(head + "\n——\n" + text);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        wishMapper.updateById(row);
        return today(me);
    }

    // ========== F258 反仪式感日 ==========

    /** 提报放空日（每年最多 3 天），当日挡打卡。 */
    public TodayVO normalDay(String me, String day) {
        CoupleSpace space = requireSpace(me);
        String d = normalizeDay(day);
        LocalDate today = LocalDate.now();
        if (d.compareTo(today.toString()) < 0) {
            throw new BusinessException(400, "放空日要往未来放，过去的就让它空过去");
        }
        String year = d.substring(0, 4);
        if (normalMapper.findByYear(space.getId(), year).size() >= CoupleNormalDay.YEAR_MAX) {
            throw new BusinessException(400, "一年最多 3 天「什么都不做」，已经够贪懒了");
        }
        if (normalMapper.find(space.getId(), year, d) != null) {
            throw new BusinessException(400, "这天已经是我们的放空日了");
        }
        normalMapper.insert(CoupleNormalDay.of(space.getId(), year, d, me));
        push.pushCoupleEvent("term-normal", me, space.partnerOf(me),
                "TA 把 " + d + " 定成我们的放空日，那天谁也不许搞仪式感 🛋️");
        return today(me);
    }

    // ========== F256/F259 读：生肖年运 / 一年日子小结 ==========

    /** 生肖年运：双方生肖 + 本年双人年运一句（无表）。 */
    public ZodiacVO zodiac(String me) {
        CoupleSpace space = requireSpace(me);
        int year = LocalDate.now().getYear();
        String zm = zodiacOf(me);
        String zp = zodiacOf(space.partnerOf(me));
        long seed = CoupleRitualBank.stableHash(space.getId() + "|zodiac|" + year + "|" + zm + zp);
        return new ZodiacVO(zm, zp, CoupleTermBank.zodiacFortune(seed + year * 31L));
    }

    /** 一年日子小结：跟风/手账/过法/吉日/家档/放空 计数 + 节气长卷（无表）。 */
    public YearVO yearly(String me, String year) {
        CoupleSpace space = requireSpace(me);
        String y = normalizeYear(year);
        int iy = Integer.parseInt(y);
        String partner = space.partnerOf(me);
        Map<String, CoupleTermCheck> checkTerms = new HashMap<>();
        for (CoupleTermCheck c : checkMapper.findByYear(space.getId(), y)) {
            checkTerms.put(c.getTerm() + "|" + c.getFromUser(), c);
        }
        List<CoupleTermNote> notes = noteMapper.findByYear(space.getId(), y);
        Map<String, String> noteBy = new HashMap<>();
        for (CoupleTermNote n : notes) {
            noteBy.put(n.getTerm() + "|" + n.getFromUser(), n.getText());
        }
        List<CoupleTermRitual> allRituals = ritualMapper.findBySpace(space.getId());
        int ritualsDone = (int) allRituals.stream().filter(r -> y.equals(r.getLastDoneYear())).count();
        int luckyCount = (int) luckyMapper.findBySpace(space.getId()).stream()
                .filter(l -> l.getDay().startsWith(y)).count();
        int normalCount = normalMapper.findByYear(space.getId(), y).size();
        List<String> scroll = new ArrayList<>();
        for (String t : CoupleTermBank.TERMS) {
            LocalDate td = CoupleTermBank.termOn(t, iy);
            if (td == null || (iy == LocalDate.now().getYear() && td.isAfter(LocalDate.now()))) {
                continue;
            }
            boolean cM = checkTerms.containsKey(t + "|" + me);
            boolean cP = checkTerms.containsKey(t + "|" + partner);
            String nM = noteBy.getOrDefault(t + "|" + me, "");
            String nP = noteBy.getOrDefault(t + "|" + partner, "");
            if (!cM && !cP && nM.isEmpty() && nP.isEmpty()) {
                continue;
            }
            StringBuilder line = new StringBuilder("「" + t + "」");
            if (cM && cP) {
                line.append("跟风双人组 ✅ ");
            } else if (cM || cP) {
                line.append("跟风差 TA 一步 ⏳ ");
            }
            if (!nM.isEmpty()) {
                line.append("我：").append(nM).append(' ');
            }
            if (!nP.isEmpty()) {
                line.append("TA：").append(nP);
            }
            scroll.add(line.toString().strip());
        }
        return new YearVO(y, checkTerms.size(), notes.size(), allRituals.size(), ritualsDone,
                luckyCount, festivalMapper.findByYear(space.getId(), y).size(), normalCount, scroll);
    }

    // ========== 内部 ==========

    private void guardNormalDay(CoupleSpace space, LocalDate now) {
        if (normalMapper.find(space.getId(), String.valueOf(now.getYear()), now.toString()) != null) {
            throw new BusinessException(400,
                    CoupleTermBank.normalDayCard(CoupleRitualBank.stableHash(space.getId() + "|normal|" + now)));
        }
    }

    /** 节日当年公历日；ANNIVM 取空间纪念日的今年月份首日，无纪念日则不展示。 */
    private String festivalDay(String key, LocalDate now, CoupleSpace space) {
        if ("ANNIVM".equals(key)) {
            LocalDate anniv = parseAnnivDate(space);
            if (anniv == null) {
                return null;
            }
            return LocalDate.of(now.getYear(), anniv.getMonthValue(), 1).toString();
        }
        LocalDate d = CoupleTermBank.festivalOn(key, now.getYear());
        return d == null ? null : d.toString();
    }

    private LocalDate parseAnnivDate(CoupleSpace space) {
        String a = space.getAnniversary();
        if (a == null || a.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(a.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 今年已过（含今天）的节气，按时间顺序。 */
    private List<String> orderedTermsOfYear(LocalDate now) {
        List<String> passed = new ArrayList<>();
        for (String t : CoupleTermBank.TERMS) {
            LocalDate d = CoupleTermBank.termOn(t, now.getYear());
            if (d != null && !d.isAfter(now)) {
                passed.add(t);
            }
        }
        return passed;
    }

    private String zodiacOf(String user) {
        UserProfile p = profileMapper.selectById(user);
        String b = p == null ? null : p.getBirthday();
        if (b == null || b.length() < 4 || !b.substring(0, 4).matches("\\d{4}")) {
            return "未填生日";
        }
        return CoupleTermBank.zodiac(Integer.parseInt(b.substring(0, 4)));
    }

    private String pickLine(String seed) {
        return CoupleTermBank.CHECK_LINES.get(Math.floorMod(CoupleRitualBank.stableHash(seed),
                CoupleTermBank.CHECK_LINES.size()));
    }

    private RitualVO toRitualVO(CoupleTermRitual r, String me) {
        return new RitualVO(r.getId(), r.getTerm(), r.getContent(), r.getFromUser().equals(me),
                r.getLastDoneYear() == null ? "" : r.getLastDoneYear());
    }

    private LuckyVO toLuckyVO(CoupleLuckyDay l, String me) {
        return new LuckyVO(l.getId(), l.getDay(), l.getMatter(), l.getComment(),
                l.getFromUser().equals(me), l.confirmedFlag());
    }

    private CoupleTermRitual requireRitual(CoupleSpace space, String id) {
        CoupleTermRitual row = id == null ? null : ritualMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这条过法不存在");
        }
        return row;
    }

    private CoupleLuckyDay requireLucky(CoupleSpace space, String id) {
        CoupleLuckyDay row = id == null ? null : luckyMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这个吉日不存在");
        }
        return row;
    }

    private String normalizeDay(String day) {
        String d = day == null ? "" : day.trim();
        try {
            LocalDate.parse(d);
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "日期格式应为 yyyy-MM-dd");
        }
        return d;
    }

    private String normalizeYear(String year) {
        String y = year == null || year.isBlank() ? String.valueOf(LocalDate.now().getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份格式应为 yyyy");
        }
        return y;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
