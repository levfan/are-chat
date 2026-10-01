package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 我们百科（F280-F289，批次二十四）：词条共建、默契综艺、喜好 TOP10 互猜、外号考据、
 * 友情测验、去过的地方、第一眼对视双盲、习惯图鉴、口味变迁、人格双报。
 * 情绪价值设计：在一起久了，两个人就自然长出了一部只有彼此看得懂的百科——
 * 把它考据下来、考一考、猜一猜，是「我记得你，也想重新认识你」。
 */
@Service
public class CoupleCodexService {

    static final int QUIZ_SIZE = 5;
    static final int TOP_SIZE = 10;
    static final int TOP_TEXT_MAX = 600;
    static final int EXAM_Q_MAX = 140;
    static final int EXAM_A_MAX = 60;
    static final int EXAM_RETRY_DAYS = 7;
    static final int TYPE_QUESTIONS = 8;
    static final int MOMENT_MAX = 200;
    static final int DEFINITION_MAX = 200;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCodexEntryMapper entryMapper;
    private final CoupleQuizShowMapper quizMapper;
    private final CoupleTopListMapper topListMapper;
    private final CoupleTopGuessMapper topGuessMapper;
    private final CouplePetnameStoryMapper storyMapper;
    private final CoupleExamMapper examMapper;
    private final CouplePlaceMapper placeMapper;
    private final CoupleFirstLookMapper firstMapper;
    private final CoupleHabitMapMapper habitMapper;
    private final CoupleTasteShiftMapper tasteMapper;
    private final CoupleTypeReportMapper typeMapper;
    private final ImPushService push;

    public CoupleCodexService(CoupleSpaceMapper spaceMapper, CoupleCodexEntryMapper entryMapper,
                              CoupleQuizShowMapper quizMapper, CoupleTopListMapper topListMapper,
                              CoupleTopGuessMapper topGuessMapper, CouplePetnameStoryMapper storyMapper,
                              CoupleExamMapper examMapper, CouplePlaceMapper placeMapper,
                              CoupleFirstLookMapper firstMapper, CoupleHabitMapMapper habitMapper,
                              CoupleTasteShiftMapper tasteMapper, CoupleTypeReportMapper typeMapper,
                              ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.entryMapper = entryMapper;
        this.quizMapper = quizMapper;
        this.topListMapper = topListMapper;
        this.topGuessMapper = topGuessMapper;
        this.storyMapper = storyMapper;
        this.examMapper = examMapper;
        this.placeMapper = placeMapper;
        this.firstMapper = firstMapper;
        this.habitMapper = habitMapper;
        this.tasteMapper = tasteMapper;
        this.typeMapper = typeMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record EntryVO(String id, String term, String definition, String origin, String usageNote,
                          boolean mine, String updatedBy) {
    }

    public record QuizVO(String day, List<String> terms, String myAnswer, String partnerAnswer,
                         boolean bothIn, Integer match, String comment) {
    }

    public record TopBoardVO(String category, String label, List<String> mine, List<String> partner,
                             List<String> myGuess, boolean revealed, List<String> rematch) {
    }

    public record StoryVO(String id, String nickname, String givenBy, String occasion, String story,
                          String firstUsedDay, boolean mine) {
    }

    public record ExamVO(String id, String question, String quizzedUser, boolean mine, boolean toMe,
                         String verdict, String lastTryDay) {
    }

    public record PlaceVO(String id, String name, String year, String happened, int rating, boolean mine) {
    }

    public record FirstLookVO(String mine, String partner, boolean revealed, boolean waiting) {
    }

    public record HabitVO(String id, String habit, String tag, String observerUser, boolean mine,
                          String verdict) {
    }

    public record TasteVO(String id, String thing, String beforeText, String nowText,
                          String shiftedDay, boolean mine) {
    }

    public record TypeVO(String year, String myKey, String partnerKey, String diffLine, String answers) {
    }

    public record OverviewVO(String day, List<EntryVO> entries, QuizVO todayQuiz, List<QuizVO> history,
                             List<TopBoardVO> tops, List<StoryVO> stories, List<ExamVO> exams,
                             List<PlaceVO> places, FirstLookVO firstLook, List<HabitVO> habits,
                             List<TasteVO> tastes, TypeVO type, int entryCount) {
    }

    // ========== 读：百科总览 ==========

    /** 我们百科总览（十个板块一次拉齐）。 */
    public OverviewVO overview(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String partner = space.partnerOf(me);
        String day = now.toString();

        List<EntryVO> entries = new ArrayList<>();
        for (CoupleCodexEntry e : entryMapper.findBySpace(space.getId())) {
            entries.add(new EntryVO(e.getId(), e.getTerm(), e.getDefinition(), e.getOrigin(),
                    e.getUsageNote(), e.getFromUser().equals(me), nullToEmpty(e.getUpdatedBy())));
        }

        QuizVO todayQuiz = null;
        List<QuizVO> history = new ArrayList<>();
        CoupleQuizShow t = quizMapper.findDay(space.getId(), day);
        if (t != null) {
            todayQuiz = toQuizVO(t, me, space);
        }
        for (CoupleQuizShow q : quizMapper.findBySpace(space.getId())) {
            if (history.size() < 6 && !q.getDay().equals(day)) {
                history.add(toQuizVO(q, me, space));
            }
        }

        List<CoupleTopList> lists = topListMapper.findBySpace(space.getId());
        List<CoupleTopGuess> guesses = topGuessMapper.findBySpace(space.getId());
        List<TopBoardVO> tops = new ArrayList<>();
        for (String cat : CoupleCodexBank.TOP_CATEGORIES) {
            List<String> mineList = itemsOf(lists, cat, me);
            List<String> partnerList = itemsOf(lists, cat, partner);
            List<String> myGuess = guessOf(guesses, cat, me);
            boolean revealed = !partnerList.isEmpty() && !myGuess.isEmpty();
            List<String> rematch = new ArrayList<>();
            if (revealed) {
                Set<String> guessSet = new LinkedHashSet<>(myGuess);
                for (String item : partnerList) {
                    if (!guessSet.contains(item)) {
                        rematch.add(CoupleCodexBank.rematchLine(cat, item));
                    }
                }
            }
            tops.add(new TopBoardVO(cat, CoupleCodexBank.TOP_LABELS.getOrDefault(cat, cat),
                    mineList, partnerList, myGuess, revealed, rematch));
        }

        List<StoryVO> stories = new ArrayList<>();
        for (CouplePetnameStory s : storyMapper.findBySpace(space.getId())) {
            stories.add(new StoryVO(s.getId(), s.getNickname(), nullToEmpty(s.getGivenBy()),
                    nullToEmpty(s.getOccasion()), nullToEmpty(s.getStory()), nullToEmpty(s.getFirstUsedDay()),
                    s.getFromUser().equals(me)));
        }

        List<ExamVO> exams = new ArrayList<>();
        for (CoupleExam x : examMapper.findBySpace(space.getId())) {
            exams.add(new ExamVO(x.getId(), x.getQuestion(), x.getQuizzedUser(),
                    x.getFromUser().equals(me), x.getQuizzedUser().equals(me),
                    nullToEmpty(x.getVerdict()), nullToEmpty(x.getLastTryDay())));
        }

        List<PlaceVO> places = new ArrayList<>();
        for (CouplePlace p : placeMapper.findBySpace(space.getId())) {
            places.add(new PlaceVO(p.getId(), p.getName(), nullToEmpty(p.getYear()),
                    nullToEmpty(p.getHappened()), p.getRating() == null ? 5 : p.getRating(),
                    p.getFromUser().equals(me)));
        }

        CoupleFirstLook mineF = firstMapper.findUser(space.getId(), me);
        CoupleFirstLook otherF = firstMapper.findUser(space.getId(), partner);
        boolean revealedF = (mineF != null && mineF.isRevealed()) || (otherF != null && otherF.isRevealed());
        boolean canSee = revealedF || (mineF != null && otherF != null
                && mineF.getTries() >= CoupleFirstLook.TRIES_MAX && otherF.getTries() >= CoupleFirstLook.TRIES_MAX);
        FirstLookVO firstLook = new FirstLookVO(
                mineF == null ? "" : mineF.getMoment(),
                canSee && otherF != null ? otherF.getMoment() : "",
                revealedF,
                mineF != null && otherF != null && !revealedF && !canSee);

        List<HabitVO> habits = new ArrayList<>();
        for (CoupleHabitMap h : habitMapper.findBySpace(space.getId())) {
            habits.add(new HabitVO(h.getId(), h.getHabit(), nullToEmpty(h.getTag()), h.getObserverUser(),
                    h.getObserverUser().equals(me), nullToEmpty(h.getVerdict())));
        }

        List<TasteVO> tastes = new ArrayList<>();
        for (CoupleTasteShift s : tasteMapper.findBySpace(space.getId())) {
            tastes.add(new TasteVO(s.getId(), s.getThing(), nullToEmpty(s.getBeforeText()),
                    nullToEmpty(s.getNowText()), nullToEmpty(s.getShiftedDay()), s.getFromUser().equals(me)));
        }

        String year = String.valueOf(now.getYear());
        CoupleTypeReport mineR = typeMapper.find(space.getId(), year, me);
        CoupleTypeReport otherR = typeMapper.find(space.getId(), year, partner);
        TypeVO type = null;
        if (mineR != null || otherR != null) {
            String diff = mineR != null && otherR != null
                    ? CoupleCodexBank.typeDiffLine(CoupleRitualBank.stableHash(space.getId() + "|type|" + year),
                    sameAxes(mineR.getTypeKey(), otherR.getTypeKey()))
                    : "";
            type = new TypeVO(year, mineR == null ? "" : mineR.getTypeKey(),
                    otherR == null ? "" : otherR.getTypeKey(), diff,
                    mineR == null ? "" : mineR.getAnswers());
        }

        return new OverviewVO(day, entries, todayQuiz, history, tops, stories, exams, places,
                firstLook, habits, tastes, type, entries.size());
    }

    // ========== F280 词条共建 ==========

    /** 新建/修订词条（双人共编，改后推 TA）。 */
    public OverviewVO saveEntry(String me, String term, String definition, String origin, String usageNote) {
        CoupleSpace space = requireSpace(me);
        String t = trim(term, "词条名要写");
        if (t.length() > CoupleCodexEntry.TERM_MAX) {
            throw new BusinessException(400, "词条名最多 40 字");
        }
        String def = trim(definition, "释义要写一句");
        if (def.length() > DEFINITION_MAX) {
            throw new BusinessException(400, "释义最多 200 字");
        }
        CoupleCodexEntry exist = entryMapper.findTerm(space.getId(), t);
        if (exist != null) {
            exist.setDefinition(def);
            exist.setOrigin(origin == null ? "" : origin.trim());
            exist.setUsageNote(usageNote == null ? "" : usageNote.trim());
            exist.setUpdatedBy(me);
            exist.setUpdatedAt(System.currentTimeMillis());
            entryMapper.updateById(exist);
        } else {
            entryMapper.insert(CoupleCodexEntry.of(space.getId(), t, def, origin, usageNote, me));
        }
        push.pushCoupleEvent("codex-entry", me, space.partnerOf(me),
                "百科词条「" + t + "」" + (exist == null ? "新建" : "被 TA 修订") + "了 📚");
        return overview(me);
    }

    /** 删词条（首建人可删）。 */
    public OverviewVO removeEntry(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleCodexEntry row = requireEntry(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "这个词条是 TA 首建的，让 TA 自己撤");
        }
        entryMapper.deleteById(row.getId());
        return overview(me);
    }

    // ========== F281 默契综艺 ==========

    /** 开一期默契考（≥5 词条，一天一期）。 */
    public OverviewVO startQuiz(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        if (quizMapper.findDay(space.getId(), day) != null) {
            throw new BusinessException(400, "今天已经考过一期了，明天再来");
        }
        List<CoupleCodexEntry> entries = entryMapper.findBySpace(space.getId());
        if (entries.size() < QUIZ_SIZE) {
            throw new BusinessException(400, "词条还不到 " + QUIZ_SIZE + " 条，先去百科里攒词");
        }
        List<String> terms = pickTerms(space.getId(), day, entries);
        quizMapper.insert(CoupleQuizShow.of(space.getId(), day, String.join(",", terms)));
        push.pushCoupleEvent("codex-quiz-open", me, space.partnerOf(me),
                "TA 开了今天的默契综艺，五道填空题等你作答 🎪");
        return overview(me);
    }

    /** 交本期五答；双交齐算默契率推 both。 */
    public OverviewVO answerQuiz(String me, String answers) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleQuizShow q = quizMapper.findDay(space.getId(), day);
        if (q == null) {
            throw new BusinessException(400, "今天还没有开场，等 TA 开一期");
        }
        List<String> ans = splitItems(answers, QUIZ_SIZE, 40);
        boolean userA = space.getUserA().equals(me);
        if (hasAnswered(q, userA)) {
            throw new BusinessException(400, "这一期你已经交卷了，等 TA 对答案");
        }
        if (userA) {
            q.setAnswerA(String.join(",", ans));
        } else {
            q.setAnswerB(String.join(",", ans));
        }
        q.setUpdatedAt(System.currentTimeMillis());
        quizMapper.updateById(q);
        if (!nullToEmpty(q.getAnswerA()).isEmpty() && !nullToEmpty(q.getAnswerB()).isEmpty()) {
            int match = matchCount(q.getAnswerA(), q.getAnswerB());
            push.pushCoupleEventBoth("codex-quiz-done", me, space.getUserA(), space.getUserB(),
                    CoupleCodexBank.quizComment(CoupleRitualBank.stableHash(space.getId() + "|quiz|" + day), match));
        } else {
            push.pushCoupleEvent("codex-quiz-answer", me, space.partnerOf(me), "TA 交卷了，去对答案 🎪");
        }
        return overview(me);
    }

    // ========== F282 喜好 TOP10 ==========

    /** 更新本人的类目榜单。 */
    public OverviewVO topList(String me, String category, String items) {
        CoupleSpace space = requireSpace(me);
        requireCategory(category);
        List<String> list = splitItems(items, TOP_SIZE, 60);
        CoupleTopList exist = topListMapper.find(space.getId(), category, me);
        if (exist != null) {
            exist.setItems(String.join(",", list));
            exist.setUpdatedAt(System.currentTimeMillis());
            topListMapper.updateById(exist);
        } else {
            topListMapper.insert(CoupleTopList.of(space.getId(), category, me, String.join(",", list)));
            push.pushCoupleEvent("codex-top-list", me, space.partnerOf(me),
                    "TA 更新了「" + CoupleCodexBank.TOP_LABELS.getOrDefault(category, category) + "」，快来猜 🎯");
        }
        return overview(me);
    }

    /** 猜对方的类目榜单（可改；对方榜单已存在即揭榜）。 */
    public OverviewVO topGuess(String me, String category, String items) {
        CoupleSpace space = requireSpace(me);
        requireCategory(category);
        String partner = space.partnerOf(me);
        List<String> list = splitItems(items, TOP_SIZE, 60);
        CoupleTopGuess exist = topGuessMapper.find(space.getId(), category, me);
        if (exist != null) {
            exist.setItems(String.join(",", list));
            exist.setUpdatedAt(System.currentTimeMillis());
            topGuessMapper.updateById(exist);
            return overview(me);
        }
        topGuessMapper.insert(CoupleTopGuess.of(space.getId(), category, partner, me, String.join(",", list)));
        push.pushCoupleEvent("codex-top-guess", me, partner,
                "TA 对你的「" + CoupleCodexBank.TOP_LABELS.getOrDefault(category, category) + "」下注了 🎯");
        return overview(me);
    }

    // ========== F283 外号考据 ==========

    /** 收录/修订外号诞生故事。 */
    public OverviewVO story(String me, String nickname, String givenBy, String occasion,
                            String story, String firstUsedDay) {
        CoupleSpace space = requireSpace(me);
        String n = trim(nickname, "外号要写");
        if (n.length() > 40) {
            throw new BusinessException(400, "外号最多 40 字");
        }
        String st = trim(story, "考据故事要写");
        if (st.length() > 300) {
            throw new BusinessException(400, "故事最多 300 字");
        }
        CouplePetnameStory exist = storyMapper.findNick(space.getId(), n);
        if (exist != null) {
            exist.setGivenBy(nullToEmpty(givenBy));
            exist.setOccasion(occasion == null ? "" : occasion.trim());
            exist.setStory(st);
            exist.setFirstUsedDay(firstUsedDay == null ? "" : firstUsedDay.trim());
            exist.setUpdatedAt(System.currentTimeMillis());
            storyMapper.updateById(exist);
            return overview(me);
        }
        storyMapper.insert(CouplePetnameStory.of(space.getId(), n, givenBy, occasion, st, firstUsedDay, me));
        push.pushCoupleEvent("codex-story", me, space.partnerOf(me),
                "TA 给外号「" + n + "」立了小传 📜");
        return overview(me);
    }

    // ========== F284 友情测验 ==========

    /** 给对方出一道「你记得吗」。 */
    public OverviewVO examAsk(String me, String question, String answer) {
        CoupleSpace space = requireSpace(me);
        String q = trim(question, "题面要写");
        if (q.length() > EXAM_Q_MAX) {
            throw new BusinessException(400, "题面最多 140 字");
        }
        String a = trim(answer, "标准答案要写");
        if (a.length() > EXAM_A_MAX) {
            throw new BusinessException(400, "答案最多 60 字");
        }
        if (examMapper.findQuestion(space.getId(), q) != null) {
            throw new BusinessException(400, "这题已经出过了");
        }
        examMapper.insert(CoupleExam.of(space.getId(), q, a, space.partnerOf(me), me));
        push.pushCoupleEvent("codex-exam", me, space.partnerOf(me), "TA 给你出了一道「你记得吗」✍️");
        return overview(me);
    }

    /** 被考人作答；答错 7 天后可补考。 */
    public OverviewVO examTry(String me, String id, String answer) {
        CoupleSpace space = requireSpace(me);
        CoupleExam x = requireExam(space, id);
        if (!x.getQuizzedUser().equals(me)) {
            throw new BusinessException(400, "这题不是考你的");
        }
        if (x.isDone()) {
            throw new BusinessException(400, "这题已经答对了，光荣记档");
        }
        LocalDate now = LocalDate.now();
        String today = now.toString();
        if ("WRONG".equals(x.getVerdict()) && !x.getLastTryDay().isEmpty()
                && now.isBefore(LocalDate.parse(x.getLastTryDay()).plusDays(EXAM_RETRY_DAYS))) {
            throw new BusinessException(400, "答错缓 " + EXAM_RETRY_DAYS + " 天，"
                    + LocalDate.parse(x.getLastTryDay()).plusDays(EXAM_RETRY_DAYS) + " 后可补考");
        }
        String a = trim(answer, "总得写个答案");
        boolean right = a.equalsIgnoreCase(x.getAnswer().trim());
        x.setVerdict(right ? "RIGHT" : "WRONG");
        x.setLastTryDay(today);
        examMapper.updateById(x);
        if (right) {
            push.pushCoupleEvent("codex-exam-hit", me, x.getFromUser(), "TA 答对了「" + x.getQuestion() + "」✅");
        } else {
            push.pushCoupleEvent("codex-exam-miss", me, x.getFromUser(),
                    "TA 这题答错了：「" + a + "」😝 记得给 TA 补课");
        }
        return overview(me);
    }

    // ========== F285 去过的地方 ==========

    /** 登记/修订足迹（同名覆盖）。 */
    public OverviewVO place(String me, String name, String year, String happened, Integer rating) {
        CoupleSpace space = requireSpace(me);
        String n = trim(name, "地名要写");
        if (n.length() > 60) {
            throw new BusinessException(400, "地名最多 60 字");
        }
        String y = year == null || year.isBlank() ? "" : year.trim();
        if (!y.isEmpty() && !y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份格式应为 yyyy");
        }
        int r = rating == null ? 5 : Math.max(1, Math.min(5, rating));
        CouplePlace exist = placeMapper.findName(space.getId(), n);
        if (exist != null) {
            exist.setYear(y);
            exist.setHappened(happened == null ? "" : happened.trim());
            exist.setRating(r);
            exist.setUpdatedAt(System.currentTimeMillis());
            placeMapper.updateById(exist);
            return overview(me);
        }
        placeMapper.insert(CouplePlace.of(space.getId(), n, y, happened, r, me));
        push.pushCoupleEvent("codex-place", me, space.partnerOf(me), "足迹地图上新了一处：「" + n + "」📍");
        return overview(me);
    }

    // ========== F286 第一眼对视 ==========

    /** 盲提交「你注意到我是哪一刻」；一致或各满 3 次互见。 */
    public OverviewVO firstLook(String me, String moment) {
        CoupleSpace space = requireSpace(me);
        String m = trim(moment, "那一刻要写");
        if (m.length() > MOMENT_MAX) {
            throw new BusinessException(400, "最多 200 字，留一点给心跳");
        }
        String partner = space.partnerOf(me);
        CoupleFirstLook mine = firstMapper.findUser(space.getId(), me);
        if (mine != null && mine.isRevealed()) {
            throw new BusinessException(400, "已经互见过啦，这一刻尘埃落定");
        }
        boolean isNew = mine == null;
        if (isNew) {
            mine = CoupleFirstLook.of(space.getId(), me, m);
            firstMapper.insert(mine);
        } else {
            if (mine.getTries() >= CoupleFirstLook.TRIES_MAX) {
                throw new BusinessException(400, "你已经交满 " + CoupleFirstLook.TRIES_MAX + " 次了，等 TA 的答卷");
            }
            mine.setMoment(m);
            mine.setTries(mine.getTries() + 1);
            mine.setUpdatedAt(System.currentTimeMillis());
            firstMapper.updateById(mine);
        }
        CoupleFirstLook other = firstMapper.findUser(space.getId(), partner);
        if (other != null && !mine.isRevealed()) {
            if (other.getMoment().equals(mine.getMoment())) {
                revealBoth(mine, other);
                push.pushCoupleEventBoth("codex-firstlook-match", me, space.getUserA(), space.getUserB(),
                        CoupleCodexBank.firstLookLine(CoupleRitualBank.stableHash(space.getId() + "|fl|same"), true));
            } else if (mine.getTries() >= CoupleFirstLook.TRIES_MAX && other.getTries() >= CoupleFirstLook.TRIES_MAX) {
                revealBoth(mine, other);
                push.pushCoupleEventBoth("codex-firstlook-force", me, space.getUserA(), space.getUserB(),
                        CoupleCodexBank.firstLookLine(CoupleRitualBank.stableHash(space.getId() + "|fl|force"), false));
            } else if (isNew) {
                push.pushCoupleEvent("codex-firstlook-new", me, partner, "TA 交上了第一眼答卷，去交你的那一份 🙈");
            }
        }
        return overview(me);
    }

    // ========== F287 习惯图鉴 ==========

    /** 记录 TA 的一个小习惯。 */
    public OverviewVO habitAdd(String me, String habit, String tag) {
        CoupleSpace space = requireSpace(me);
        String h = trim(habit, "习惯要写");
        if (h.length() > 60) {
            throw new BusinessException(400, "习惯最多 60 字");
        }
        String partner = space.partnerOf(me);
        if (habitMapper.findHabit(space.getId(), h, me) != null) {
            throw new BusinessException(400, "这条习惯已经图鉴在案");
        }
        habitMapper.insert(CoupleHabitMap.of(space.getId(), h, tag, me, partner));
        push.pushCoupleEvent("codex-habit", me, partner, "TA 把你的小习惯记进了图鉴：「" + h + "」，去标「确实/冤枉」🔍");
        return overview(me);
    }

    /** 被观察的人判案：确实/冤枉。 */
    public OverviewVO habitVerdict(String me, String id, String verdict) {
        CoupleSpace space = requireSpace(me);
        CoupleHabitMap row = id == null ? null : habitMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这条图鉴不存在");
        }
        if (!row.getTargetUser().equals(me)) {
            throw new BusinessException(400, "这是关于你的习惯，得你亲自判");
        }
        if (!row.getVerdict().isEmpty()) {
            return overview(me);
        }
        if (!CoupleHabitMap.VERDICT_REAL.equals(verdict) && !CoupleHabitMap.VERDICT_WRONG.equals(verdict)) {
            throw new BusinessException(400, "只能判「确实」或「冤枉」");
        }
        row.setVerdict(verdict);
        habitMapper.updateById(row);
        push.pushCoupleEvent("codex-habit-verdict", me, row.getObserverUser(),
                "TA 对你的观察判了案：" + (CoupleHabitMap.VERDICT_REAL.equals(verdict) ? "确实 ✅" : "冤枉 ❌"));
        return overview(me);
    }

    // ========== F288 口味变迁 ==========

    /** 记一笔「以前不爱现在爱」（同人同对象可改）。 */
    public OverviewVO taste(String me, String thing, String beforeText, String nowText, String shiftedDay) {
        CoupleSpace space = requireSpace(me);
        String t = trim(thing, "口味对象要写");
        if (t.length() > 60) {
            throw new BusinessException(400, "对象最多 60 字");
        }
        String sd = shiftedDay == null || shiftedDay.isBlank() ? LocalDate.now().toString() : shiftedDay.trim();
        try {
            LocalDate.parse(sd);
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "转折日格式应为 yyyy-MM-dd");
        }
        CoupleTasteShift exist = tasteMapper.findThing(space.getId(), t, me);
        if (exist != null) {
            exist.setBeforeText(beforeText == null ? "" : beforeText.trim());
            exist.setNowText(nowText == null ? "" : nowText.trim());
            exist.setShiftedDay(sd);
            exist.setUpdatedAt(System.currentTimeMillis());
            tasteMapper.updateById(exist);
            return overview(me);
        }
        tasteMapper.insert(CoupleTasteShift.of(space.getId(), t, beforeText, nowText, sd, me));
        push.pushCoupleEvent("codex-taste", me, space.partnerOf(me), "TA 的口味又进化了：「" + t + "」😋");
        return overview(me);
    }

    // ========== F289 人格双报 ==========

    /** 提交八题四维速测（一年一报，重测覆盖当年）。 */
    public OverviewVO typeReport(String me, String answers) {
        CoupleSpace space = requireSpace(me);
        List<String> ans = splitItems(answers, TYPE_QUESTIONS, 2);
        if (ans.size() != TYPE_QUESTIONS || !ans.stream().allMatch(a -> a.equals("1") || a.equals("2"))) {
            throw new BusinessException(400, "八题都要作答，每题选 1 或 2");
        }
        String year = String.valueOf(LocalDate.now().getYear());
        String key = typeKey(ans);
        CoupleTypeReport exist = typeMapper.find(space.getId(), year, me);
        if (exist != null) {
            exist.setAnswers(String.join(",", ans));
            exist.setTypeKey(key);
            exist.setUpdatedAt(System.currentTimeMillis());
            typeMapper.updateById(exist);
        } else {
            typeMapper.insert(CoupleTypeReport.of(space.getId(), year, me, String.join(",", ans), key));
        }
        String partner = space.partnerOf(me);
        CoupleTypeReport other = typeMapper.find(space.getId(), year, partner);
        if (other != null && (exist == null || !other.getTypeKey().isEmpty())) {
            push.pushCoupleEventBoth("codex-type-both", me, space.getUserA(), space.getUserB(),
                    "双人年报到齐：TA " + key + " × TA " + other.getTypeKey() + "，"
                            + CoupleCodexBank.typeDiffLine(
                            CoupleRitualBank.stableHash(space.getId() + "|type|" + year), sameAxes(key, other.getTypeKey())));
        }
        return overview(me);
    }

    // ========== 内部 ==========

    /** 四维各两题多数票定档，平票取第一题。 */
    private String typeKey(List<String> ans) {
        StringBuilder key = new StringBuilder();
        for (int axis = 0; axis < 4; axis++) {
            boolean firstLeft = ans.get(axis).equals("1");
            boolean secondLeft = ans.get(axis + 4).equals("1");
            char pick = firstLeft == secondLeft
                    ? (firstLeft ? CoupleCodexBank.TYPE_AXES[axis][0] : CoupleCodexBank.TYPE_AXES[axis][1])
                    : (firstLeft ? CoupleCodexBank.TYPE_AXES[axis][0] : CoupleCodexBank.TYPE_AXES[axis][1]);
            key.append(pick);
        }
        return key.toString();
    }

    private int sameAxes(String a, String b) {
        int n = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                n++;
            }
        }
        return n;
    }

    private void revealBoth(CoupleFirstLook a, CoupleFirstLook b) {
        a.setRevealed(1);
        b.setRevealed(1);
        firstMapper.updateById(a);
        firstMapper.updateById(b);
    }

    private boolean hasAnswered(CoupleQuizShow q, boolean userA) {
        return !nullToEmpty(userA ? q.getAnswerA() : q.getAnswerB()).isEmpty();
    }

    private QuizVO toQuizVO(CoupleQuizShow q, String me, CoupleSpace space) {
        boolean userA = space.getUserA().equals(me);
        String mineA = nullToEmpty(userA ? q.getAnswerA() : q.getAnswerB());
        String otherA = nullToEmpty(userA ? q.getAnswerB() : q.getAnswerA());
        boolean both = !mineA.isEmpty() && !otherA.isEmpty();
        Integer match = both ? matchCount(q.getAnswerA(), q.getAnswerB()) : null;
        return new QuizVO(q.getDay(), Arrays.asList(q.getTerms().split(",")), mineA, otherA, both, match,
                both ? CoupleCodexBank.quizComment(
                        CoupleRitualBank.stableHash(space.getId() + "|quiz|" + q.getDay()), match) : "");
    }

    private int matchCount(String a, String b) {
        String[] as = a.split(",", -1);
        String[] bs = b.split(",", -1);
        int n = 0;
        for (int i = 0; i < Math.min(as.length, bs.length); i++) {
            if (!as[i].isBlank() && as[i].trim().equalsIgnoreCase(bs[i].trim())) {
                n++;
            }
        }
        return n;
    }

    private List<String> pickTerms(String spaceId, String day, List<CoupleCodexEntry> entries) {
        List<CoupleCodexEntry> pool = new ArrayList<>(entries);
        java.util.Collections.shuffle(pool, new java.util.Random(
                CoupleRitualBank.stableHash(spaceId + "|quiz|" + day)));
        List<String> terms = new ArrayList<>();
        for (int i = 0; i < QUIZ_SIZE && i < pool.size(); i++) {
            terms.add(pool.get(i).getTerm());
        }
        return terms;
    }

    private List<String> itemsOf(List<CoupleTopList> lists, String category, String user) {
        for (CoupleTopList l : lists) {
            if (l.getCategory().equals(category) && l.getOwnerUser().equals(user)) {
                return splitBlank(l.getItems());
            }
        }
        return List.of();
    }

    private List<String> guessOf(List<CoupleTopGuess> guesses, String category, String user) {
        for (CoupleTopGuess g : guesses) {
            if (g.getCategory().equals(category) && g.getGuesserUser().equals(user)) {
                return splitBlank(g.getItems());
            }
        }
        return List.of();
    }

    private void requireCategory(String category) {
        if (category == null || !CoupleCodexBank.TOP_CATEGORIES.contains(category)) {
            throw new BusinessException(400, "类目不存在，从八个榜单类目里挑一个");
        }
    }

    private CoupleCodexEntry requireEntry(CoupleSpace space, String id) {
        CoupleCodexEntry row = id == null ? null : entryMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这个词条不存在");
        }
        return row;
    }

    private CoupleExam requireExam(CoupleSpace space, String id) {
        CoupleExam row = id == null ? null : examMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这道题不存在");
        }
        return row;
    }

    private List<String> splitItems(String raw, int max, int lenEach) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            throw new BusinessException(400, "内容不能为空");
        }
        for (String s : raw.split("[,，、]")) {
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > lenEach) {
                throw new BusinessException(400, "每条最多 " + lenEach + " 字");
            }
            out.add(t);
        }
        if (out.isEmpty()) {
            throw new BusinessException(400, "至少写一项");
        }
        if (out.size() > max) {
            throw new BusinessException(400, "最多 " + max + " 项");
        }
        return out;
    }

    private List<String> splitBlank(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty() && !failMessage.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
