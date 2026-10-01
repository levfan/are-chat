package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 趣味游戏（F130-F139，批次九）：一百问（答一题解锁一题）、出题考TA、心动概率、
 * 塔罗、世界情话课、周末盲选、情话Battle、恋爱天气、抽象画（骰子为纯前端）。
 * 情绪价值设计：把「了解彼此」做成闯关（一百问）、把「较劲」做成甜的（Battle/考TA）、
 * 把「日常」做成抽签（天气/塔罗/盲选），让相处自带游戏性。
 */
@Service
public class CouplePlayService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleSurveyAnswerMapper surveyMapper;
    private final CoupleQuizDuelMapper quizMapper;
    private final CoupleLoveWordMapper loveWordMapper;
    private final CoupleBlindPickMapper blindMapper;
    private final CoupleSweetBattleMapper battleMapper;
    private final CoupleSweetLineMapper lineMapper;
    private final CoupleArtGalleryMapper artMapper;
    private final ImPushService push;

    public CouplePlayService(CoupleSpaceMapper spaceMapper, CoupleSurveyAnswerMapper surveyMapper,
                             CoupleQuizDuelMapper quizMapper, CoupleLoveWordMapper loveWordMapper,
                             CoupleBlindPickMapper blindMapper, CoupleSweetBattleMapper battleMapper,
                             CoupleSweetLineMapper lineMapper, CoupleArtGalleryMapper artMapper,
                             ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.surveyMapper = surveyMapper;
        this.quizMapper = quizMapper;
        this.loveWordMapper = loveWordMapper;
        this.blindMapper = blindMapper;
        this.battleMapper = battleMapper;
        this.lineMapper = lineMapper;
        this.artMapper = artMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record SurveyAnswerVO(int qNo, String answer) {
    }

    public record SurveyVO(int total, int myCount, int partnerCount, List<String> questions,
                           List<SurveyAnswerVO> my, List<SurveyAnswerVO> partnerUnlocked) {
    }

    public record QuizVO(String id, String fromUser, boolean mine, String question,
                         String answerText, String status, String verdict, Long created) {
    }

    public record LessonVO(String language, String word, String meaning, List<CoupleLoveWord> collected) {
    }

    public record BlindVO(String week, List<String> mine, boolean partnerSubmitted,
                          String planMine, String planPartner, boolean settled, List<CoupleBlindPick> history) {
    }

    public record BattleLineVO(String id, String fromUser, boolean mine, String content) {
    }

    public record BattleVO(String id, String day, String status, boolean voted,
                           String winner, List<BattleLineVO> lines, List<CoupleSweetBattle> history) {
    }

    public record HeartbeatVO(int score, String line) {
    }

    public record WeatherVO(String name, String emoji, String tip) {
    }

    public record TarotVO(String name, String emoji, String message) {
    }

    // ========== F130 一百问 ==========

    public SurveyVO survey(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleSurveyAnswer> mine = surveyMapper.findByUser(space.getId(), me);
        List<CoupleSurveyAnswer> partner = surveyMapper.findByUser(space.getId(), space.partnerOf(me));
        // 答一题解锁一题：只有我也答了的题，才展示 TA 的答案
        List<Integer> myQNos = mine.stream().map(CoupleSurveyAnswer::getQNo).toList();
        List<SurveyAnswerVO> partnerUnlocked = partner.stream()
                .filter(a -> myQNos.contains(a.getQNo()))
                .map(a -> new SurveyAnswerVO(a.getQNo(), a.getAnswer()))
                .toList();
        return new SurveyVO(CoupleSurveyAnswer.Q_MAX, mine.size(), partner.size(),
                CouplePlayBank.surveyQuestions(),
                mine.stream().map(a -> new SurveyAnswerVO(a.getQNo(), a.getAnswer())).toList(),
                partnerUnlocked);
    }

    /** 答一题（可改答案），答完解锁 TA 同题答案。 */
    public SurveyVO answerSurvey(String me, int qNo, String answer) {
        CoupleSpace space = requireSpace(me);
        if (qNo < 1 || qNo > CoupleSurveyAnswer.Q_MAX) {
            throw new BusinessException(400, "题号要在 1-100 之间哦");
        }
        String text = trimLimit(answer, CoupleSurveyAnswer.ANSWER_MAX,
                "答案写 " + CoupleSurveyAnswer.ANSWER_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "先写下你的答案吧 ✍️");
        }
        CoupleSurveyAnswer row = surveyMapper.find(space.getId(), me, qNo);
        if (row == null) {
            surveyMapper.insert(CoupleSurveyAnswer.of(space.getId(), me, qNo, text));
        } else {
            row.setAnswer(text);
            row.setUpdatedAt(System.currentTimeMillis());
            surveyMapper.updateById(row);
        }
        push.pushCoupleEvent("survey-answered", me, space.partnerOf(me),
                "📝 TA 答了一百问的第 " + qNo + " 题——你也答这题，就能解锁 TA 的答案。");
        return survey(me);
    }

    // ========== F131 出题考TA ==========

    public List<QuizVO> quizzes(String me) {
        CoupleSpace space = requireSpace(me);
        return quizMapper.findBySpace(space.getId()).stream()
                .map(q -> {
                    boolean mine = q.getFromUser().equals(me);
                    // 对方的作答只有出题人（判分方）可见，作答后出题人判分
                    String answer = q.getAnswerText() == null ? null
                            : (mine || CoupleQuizDuel.STATUS_JUDGED.equals(q.getStatus()) ? q.getAnswerText() : null);
                    return new QuizVO(q.getId(), q.getFromUser(), mine, q.getQuestion(),
                            answer, q.getStatus(), q.getVerdict(), q.getCreated());
                })
                .toList();
    }

    /** 出一道题考 TA。 */
    public List<QuizVO> makeQuiz(String me, String question) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(question, CoupleQuizDuel.QUESTION_MAX, "题目写 " + CoupleQuizDuel.QUESTION_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "写下你要考的题目吧 📝");
        }
        quizMapper.insert(CoupleQuizDuel.of(space.getId(), me, text));
        push.pushCoupleEvent("quiz-made", me, space.partnerOf(me),
                "🎯 TA 出了一道题考你：「" + text + "」——看看你有多懂 TA。");
        return quizzes(me);
    }

    /** 对方作答（作答内容只有出题人可见，等 TA 判分）。 */
    public List<QuizVO> answerQuiz(String me, String id, String answer) {
        CoupleSpace space = requireSpace(me);
        CoupleQuizDuel row = requireQuiz(space.getId(), id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己出的题要 TA 来答哦");
        }
        if (!CoupleQuizDuel.STATUS_OPEN.equals(row.getStatus())) {
            throw new BusinessException(400, "这道题已经答过啦");
        }
        String text = trimLimit(answer, CoupleQuizDuel.ANSWER_MAX, "答案写 " + CoupleQuizDuel.ANSWER_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "先写下你的答案吧 ✍️");
        }
        row.setAnswerText(text);
        row.setStatus(CoupleQuizDuel.STATUS_ANSWERED);
        quizMapper.updateById(row);
        push.pushCoupleEvent("quiz-answered", me, space.partnerOf(me),
                "✍️ TA 交卷了！快去判分：「" + row.getQuestion() + "」");
        return quizzes(me);
    }

    /** 出题人判分（只有出题人能判）。 */
    public List<QuizVO> judgeQuiz(String me, String id, String verdict) {
        CoupleSpace space = requireSpace(me);
        CoupleQuizDuel row = requireQuiz(space.getId(), id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(403, "这题是 TA 出的，判分权在 TA 手里");
        }
        if (!CoupleQuizDuel.STATUS_ANSWERED.equals(row.getStatus())) {
            throw new BusinessException(400, "TA 还没作答，先等等");
        }
        if (!CoupleQuizDuel.VERDICT_RIGHT.equals(verdict) && !CoupleQuizDuel.VERDICT_WRONG.equals(verdict)) {
            throw new BusinessException(400, "判分只能是 RIGHT 或 WRONG 哦");
        }
        row.setVerdict(verdict);
        row.setStatus(CoupleQuizDuel.STATUS_JUDGED);
        quizMapper.updateById(row);
        push.pushCoupleEventBoth("quiz-judged", me, space.getUserA(), space.getUserB(),
                CoupleQuizDuel.VERDICT_RIGHT.equals(verdict)
                        ? "🎉 判分出炉：答对啦！TA 果然很懂你。"
                        : "😅 判分出炉：没答对……回去再抄一百遍 TA 的喜好。");
        return quizzes(me);
    }

    // ========== F132 心动概率（无表） ==========

    /** 今日心动概率：按 space+day 稳定。 */
    public HeartbeatVO heartbeat(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        int score = Math.floorMod(CoupleRitualBank.stableHash(space.getId() + "|heart|" + today), 101);
        return new HeartbeatVO(score, CouplePlayBank.heartbeatLine(score));
    }

    // ========== F133 塔罗（无表） ==========

    /** 今日塔罗：按 space+day 稳定抽牌。 */
    public TarotVO tarot(String me) {
        CoupleSpace space = requireSpace(me);
        CouplePlayBank.TarotCard card = CouplePlayBank.pickTarot(space.getId(), LocalDate.now().toString());
        return new TarotVO(card.name(), card.emoji(), card.message());
    }

    // ========== F134 世界情话课 ==========

    /** 今日一课 + 收藏夹。 */
    public LessonVO loveLesson(String me) {
        CoupleSpace space = requireSpace(me);
        CouplePlayBank.LoveLesson lesson = CouplePlayBank.pickLesson(space.getId(), LocalDate.now().toString());
        return new LessonVO(lesson.language(), lesson.word(), lesson.meaning(),
                loveWordMapper.findBySpace(space.getId()));
    }

    /** 收藏一句情话。 */
    public LessonVO collectLoveWord(String me, String word, String meaning) {
        CoupleSpace space = requireSpace(me);
        String w = trimLimit(word, CoupleLoveWord.WORD_MAX, "情话写 " + CoupleLoveWord.WORD_MAX + " 字以内哦");
        if (w == null) {
            throw new BusinessException(400, "写下要收藏的情话吧 💘");
        }
        loveWordMapper.insert(CoupleLoveWord.of(space.getId(), me, w,
                trimLimit(meaning, CoupleLoveWord.MEANING_MAX, null)));
        push.pushCoupleEvent("love-word-kept", me, space.partnerOf(me),
                "💘 TA 收藏了一句情话：「" + w + "」——下一课，说给你听。");
        return loveLesson(me);
    }

    // ========== F135 周末盲选 ==========

    public BlindVO blindPick(String me) {
        CoupleSpace space = requireSpace(me);
        String week = mondayOf(LocalDate.now()).toString();
        List<CoupleBlindPick> rows = blindMapper.findByWeek(space.getId(), week);
        CoupleBlindPick mine = rows.stream().filter(r -> r.getFromUser().equals(me)).findFirst().orElse(null);
        CoupleBlindPick partner = rows.stream().filter(r -> !r.getFromUser().equals(me)).findFirst().orElse(null);
        String planMine = null;
        String planPartner = null;
        boolean settled = mine != null && partner != null;
        if (settled) {
            List<String> myPicks = List.of(mine.getPicks().split(","));
            List<String> partnerPicks = List.of(partner.getPicks().split(","));
            long hash = CoupleRitualBank.stableHash(space.getId() + "|blind|" + week);
            planMine = myPicks.get(Math.floorMod(hash, myPicks.size()));
            planPartner = partnerPicks.get(Math.floorMod(hash >> 16, partnerPicks.size()));
        }
        return new BlindVO(week,
                mine == null ? List.of() : List.of(mine.getPicks().split(",")),
                partner != null, planMine, planPartner, settled, blindMapper.findBySpace(space.getId()));
    }

    /** 提交本周的 3 个周末愿望（可改）。 */
    public BlindVO submitBlindPick(String me, List<String> picks) {
        CoupleSpace space = requireSpace(me);
        List<String> cleaned = new ArrayList<>();
        if (picks != null) {
            picks.stream().filter(p -> p != null && !p.isBlank()).limit(CoupleBlindPick.PICK_COUNT)
                    .forEach(p -> cleaned.add(p.trim()));
        }
        if (cleaned.size() < 2) {
            throw new BusinessException(400, "至少写 2 个周末愿望（最多 3 个）🎁");
        }
        String week = mondayOf(LocalDate.now()).toString();
        String joined = String.join(",", cleaned);
        CoupleBlindPick row = blindMapper.findByWeek(space.getId(), week).stream()
                .filter(r -> r.getFromUser().equals(me)).findFirst().orElse(null);
        if (row == null) {
            blindMapper.insert(CoupleBlindPick.of(space.getId(), me, week, joined));
        } else {
            row.setPicks(joined);
            blindMapper.updateById(row);
        }
        BlindVO vo = blindPick(me);
        if (vo.settled()) {
            push.pushCoupleEventBoth("blind-settled", me, space.getUserA(), space.getUserB(),
                    "🎁 周末盲选开奖！本周计划：「" + vo.planMine() + "」+「" + vo.planPartner() + "」");
        } else {
            push.pushCoupleEvent("blind-submitted", me, space.partnerOf(me),
                    "🎁 TA 把本周的周末愿望投进盲盒了，就等你的三张。");
        }
        return vo;
    }

    // ========== F136 情话Battle ==========

    public BattleVO battle(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CoupleSweetBattle row = battleMapper.findByDay(space.getId(), today);
        if (row == null) {
            return new BattleVO(null, today, CoupleSweetBattle.STATUS_OPEN, false, null, List.of(),
                    battleMapper.findBySpace(space.getId()));
        }
        List<BattleLineVO> lines = lineMapper.findByBattle(row.getId()).stream()
                .map(l -> new BattleLineVO(l.getId(), l.getFromUser(), l.getFromUser().equals(me), l.getContent()))
                .toList();
        boolean voted = space.getUserA().equals(me) ? row.getVoteA() != null : row.getVoteB() != null;
        return new BattleVO(row.getId(), today, row.getStatus(), voted, row.getWinner(), lines,
                battleMapper.findBySpace(space.getId()));
    }

    /** 参加今日情话 Battle（每人一句，可改）。 */
    public BattleVO joinBattle(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CoupleSweetBattle row = battleMapper.findByDay(space.getId(), today);
        if (row == null) {
            row = CoupleSweetBattle.of(space.getId(), today);
            battleMapper.insert(row);
        }
        if (CoupleSweetBattle.STATUS_DONE.equals(row.getStatus())) {
            throw new BusinessException(400, "今天的 Battle 已经结算啦，明天再来 ⏰");
        }
        String text = trimLimit(content, CoupleSweetLine.CONTENT_MAX, "情话写 " + CoupleSweetLine.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "写一句你的参赛情话吧 💘");
        }
        CoupleSweetLine mine = lineMapper.findByBattle(row.getId()).stream()
                .filter(l -> l.getFromUser().equals(me)).findFirst().orElse(null);
        if (mine == null) {
            lineMapper.insert(CoupleSweetLine.of(space.getId(), row.getId(), me, text));
        } else {
            mine.setContent(text);
            lineMapper.updateById(mine);
        }
        boolean full = lineMapper.findByBattle(row.getId()).size() >= 2;
        if (full && CoupleSweetBattle.STATUS_OPEN.equals(row.getStatus())) {
            row.setStatus(CoupleSweetBattle.STATUS_FULL);
            battleMapper.updateById(row);
            push.pushCoupleEventBoth("battle-full", me, space.getUserA(), space.getUserB(),
                    "💘 今天的情话 Battle 双方到齐！互相投出你心中最心动的一句。");
        } else if (!full) {
            push.pushCoupleEvent("battle-joined", me, space.partnerOf(me),
                    "💘 TA 上场了：「" + text + "」——今天谁更会撩？等你应战。");
        }
        return battle(me);
    }

    /** 投票（场上两句任选其一：投自己也算一种自信），双方投完即结算。 */
    public BattleVO voteBattle(String me, String toUser) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CoupleSweetBattle row = battleMapper.findByDay(space.getId(), today);
        if (row == null || !CoupleSweetBattle.STATUS_FULL.equals(row.getStatus())) {
            throw new BusinessException(400, "今天的 Battle 还没到投票环节哦");
        }
        if (!me.equals(toUser) && !space.partnerOf(me).equals(toUser)) {
            throw new BusinessException(400, "只能投场上两句话中的一句哦");
        }
        boolean targetOnStage = lineMapper.findByBattle(row.getId()).stream()
                .anyMatch(l -> l.getFromUser().equals(toUser));
        if (!targetOnStage) {
            throw new BusinessException(400, "TA 的句子还没上场，先等等再投");
        }
        if (space.getUserA().equals(me)) {
            row.setVoteA(toUser);
        } else {
            row.setVoteB(toUser);
        }
        if (row.getVoteA() != null && row.getVoteB() != null) {
            row.setStatus(CoupleSweetBattle.STATUS_DONE);
            row.setWinner(row.getVoteA().equals(row.getVoteB()) ? row.getVoteA() : null);
            battleMapper.updateById(row);
            String result = row.getWinner() == null
                    ? "💘 今天的情话 Battle 平局——双方句子都甜到犯规，双倍甜！"
                    : "🏆 今天的情话 Battle 赢家是 " + row.getWinner() + "！输的人请喝奶茶。";
            push.pushCoupleEventBoth("battle-done", me, space.getUserA(), space.getUserB(), result);
        } else {
            battleMapper.updateById(row);
            push.pushCoupleEvent("battle-voted", me, space.partnerOf(me), "🗳️ TA 投出了心中最心动的一句，就差你一票。");
        }
        return battle(me);
    }

    // ========== F137 恋爱天气（无表） ==========

    /** 今日恋爱天气预报：按 space+day 稳定。 */
    public WeatherVO weather(String me) {
        CoupleSpace space = requireSpace(me);
        CouplePlayBank.LoveWeather w = CouplePlayBank.pickWeather(space.getId(), LocalDate.now().toString());
        return new WeatherVO(w.name(), w.emoji(), w.tip());
    }

    // ========== F138 抽象画 ==========

    public List<CoupleArtGallery> arts(String me) {
        return artMapper.findBySpace(requireSpace(me).getId());
    }

    /** 送一幅抽象画进画廊（seed 由前端随机生成，画作由前端按种子绘制）。 */
    public List<CoupleArtGallery> createArt(String me, String title, int seed) {
        CoupleSpace space = requireSpace(me);
        String t = trimLimit(title, CoupleArtGallery.TITLE_MAX, "标题写 " + CoupleArtGallery.TITLE_MAX + " 字以内哦");
        if (t == null) {
            throw new BusinessException(400, "给这幅画起个标题吧 🎨");
        }
        artMapper.insert(CoupleArtGallery.of(space.getId(), me, t, Math.floorMod(seed, 100000)));
        push.pushCoupleEvent("art-added", me, space.partnerOf(me),
                "🎨 TA 送了一幅抽象画《" + t + "》进画廊——我看不懂，但我很喜欢。");
        return arts(me);
    }

    // ========== 内部工具 ==========

    private LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private String trimLimit(String text, int max, String message) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        if (message != null && t.length() > max) {
            throw new BusinessException(400, message);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private CoupleQuizDuel requireQuiz(String spaceId, String id) {
        CoupleQuizDuel row = quizMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这道题哦");
        }
        return row;
    }
}
