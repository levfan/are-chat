package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 深聊系列（F66/F67/F68/F69）：真心话抽签、匿名树洞、心灵感应、情话储蓄罐。
 * 情绪价值设计：给不敢问的问题一个安全通道、给同频一个可测量的证据、
 * 给情话一个「会在某个晚上抵达」的利息机制。
 */
@Service
public class CoupleTalkService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleTruthMapper truthMapper;
    private final CoupleWhisperMapper whisperMapper;
    private final CoupleTelepathyMapper telepathyMapper;
    private final CoupleLoveBankMapper loveBankMapper;
    private final ImPushService push;

    public CoupleTalkService(CoupleSpaceMapper spaceMapper, CoupleTruthMapper truthMapper,
                             CoupleWhisperMapper whisperMapper, CoupleTelepathyMapper telepathyMapper,
                             CoupleLoveBankMapper loveBankMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.truthMapper = truthMapper;
        this.whisperMapper = whisperMapper;
        this.telepathyMapper = telepathyMapper;
        this.loveBankMapper = loveBankMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** 今天的真心话：题目 + 双方回答（谁没答是 null）。 */
    public record TruthTodayVO(String day, String question, String myAnswer, String partnerAnswer) {
    }

    public record TruthHistoryVO(String day, String question, String myAnswer, String partnerAnswer) {
    }

    public record WhisperVO(String id, String question, boolean anonymous, String askerLabel, String answer,
                            Long answeredAt, boolean mine, Long created) {
    }

    public record TelepathyRoundVO(String id, int round, String question, String[] options,
                                   String answerA, String answerB, boolean settled, boolean matched, boolean mineStarted) {
    }

    public record TelepathyBoardVO(TelepathyRoundVO current, List<TelepathyRoundVO> history,
                                   int roundsLeftToday, long matchedCount, long totalSettled) {
    }

    public record LoveBankVO(String id, String content, boolean delivered, Long deliveredAt, Long created) {
    }

    public record LoveBankBoardVO(long inJar, long deliveredCount, List<LoveBankVO> mine) {
    }

    // ========== F66 真心话 ==========

    /** 今天的真心话：题目按「空间+日期」稳定（同一天双方同题）。 */
    public TruthTodayVO truthToday(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String day = LocalDate.now().toString();
        String question = CoupleTalkBank.pickTruth(space.getId(), day);
        String mine = truthMapper.findByDay(space.getId(), day, question).stream()
                .filter(t -> t.getAnswerer().equals(me)).findFirst()
                .map(t -> t.getAnswer()).orElse(null);
        String theirs = truthMapper.findByDay(space.getId(), day, question).stream()
                .filter(t -> t.getAnswerer().equals(partner)).findFirst()
                .map(t -> t.getAnswer()).orElse(null);
        return new TruthTodayVO(day, question, mine, theirs);
    }

    /** 回答今天的真心话（可修改）；答完推送对方来看。 */
    public TruthTodayVO answerTruth(String me, String answer) {
        if (answer == null || answer.isBlank() || answer.length() > CoupleTruth.ANSWER_MAX) {
            throw new BusinessException(400, "真心话要写 " + CoupleTruth.ANSWER_MAX + " 字以内的真心哦");
        }
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String day = LocalDate.now().toString();
        String question = CoupleTalkBank.pickTruth(space.getId(), day);
        CoupleTruth existing = truthMapper.findByDay(space.getId(), day, question).stream()
                .filter(t -> t.getAnswerer().equals(me)).findFirst().orElse(null);
        if (existing == null) {
            truthMapper.insert(CoupleTruth.of(space.getId(), day, question, me, answer.trim()));
        } else {
            existing.setAnswer(answer.trim());
            truthMapper.updateById(existing);
        }
        push.pushCoupleEvent("truth-answered", me, partner,
                "💬 TA 交出了今天的真心话：「" + question + "」快去看看 TA 说了什么");
        return truthToday(me);
    }

    /** 双方都答过的真心话存档（新→旧）。 */
    public List<TruthHistoryVO> truthHistory(String me, Integer days) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        int limit = days == null || days <= 0 ? 30 : Math.min(days, 90);
        Map<String, List<CoupleTruth>> byDay = truthMapper.findBySpace(space.getId()).stream()
                .collect(Collectors.groupingBy(CoupleTruth::getDay, java.util.LinkedHashMap::new, Collectors.toList()));
        String today = LocalDate.now().toString();
        return byDay.entrySet().stream()
                .filter(e -> e.getKey().compareTo(today) <= 0)
                .filter(e -> e.getValue().stream().anyMatch(t -> t.getAnswerer().equals(me)))
                .filter(e -> e.getValue().stream().anyMatch(t -> t.getAnswerer().equals(partner)))
                .limit(limit)
                .map(e -> {
                    String question = e.getValue().get(0).getQuestion();
                    String mine = e.getValue().stream().filter(t -> t.getAnswerer().equals(me)).findFirst()
                            .map(t -> t.getAnswer()).orElse(null);
                    String theirs = e.getValue().stream().filter(t -> t.getAnswerer().equals(partner)).findFirst()
                            .map(t -> t.getAnswer()).orElse(null);
                    return new TruthHistoryVO(e.getKey(), question, mine, theirs);
                })
                .toList();
    }

    // ========== F67 匿名树洞 ==========

    public List<WhisperVO> whispers(String me) {
        CoupleSpace space = requireSpace(me);
        return whisperMapper.findBySpace(space.getId()).stream()
                .map(w -> toWhisperVO(w, me))
                .toList();
    }

    /** 往树洞里投一个问题：可选匿名（TA 回答后揭晓是谁）；在途最多一个。 */
    public List<WhisperVO> askWhisper(String me, String question, boolean anonymous) {
        if (question == null || question.isBlank() || question.length() > CoupleWhisper.QUESTION_MAX) {
            throw new BusinessException(400, "问题要写 " + CoupleWhisper.QUESTION_MAX + " 字以内哦");
        }
        CoupleSpace space = requireSpace(me);
        if (whisperMapper.findPendingByUser(space.getId(), me) != null) {
            throw new BusinessException(400, "你有一个问题还在树洞里，等 TA 回答再投下一个吧 🕳️");
        }
        CoupleWhisper whisper = CoupleWhisper.of(space.getId(), me, question.trim(), anonymous);
        whisperMapper.insert(whisper);
        String asker = anonymous ? "匿名小可爱" : me;
        push.pushCoupleEvent("whisper-asked", me, space.partnerOf(me),
                "🕳️ 树洞里有一条来自「" + asker + "」的提问，去回答吧");
        return whispers(me);
    }

    /** 回答树洞提问：不能自问自答；回答后自动揭晓匿名提问人。 */
    public List<WhisperVO> answerWhisper(String me, String id, String answer) {
        if (answer == null || answer.isBlank() || answer.length() > CoupleWhisper.ANSWER_MAX) {
            throw new BusinessException(400, "回答要写 " + CoupleWhisper.ANSWER_MAX + " 字以内哦");
        }
        CoupleSpace space = requireSpace(me);
        CoupleWhisper whisper = whisperMapper.selectById(id);
        if (whisper == null || !whisper.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这条树洞提问哦");
        }
        if (whisper.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己的问题自己回答，树洞会害羞的");
        }
        if (whisper.getAnswer() == null) {
            whisper.setAnswer(answer.trim());
            whisper.setAnsweredAt(System.currentTimeMillis());
            whisperMapper.updateById(whisper);
            push.pushCoupleEvent("whisper-answered", me, whisper.getFromUser(),
                    "👁️ TA 回答了你的树洞提问（顺便揭晓了你是谁 👀）：快去看看");
        }
        return whispers(me);
    }

    private WhisperVO toWhisperVO(CoupleWhisper w, String viewer) {
        boolean mine = w.getFromUser().equals(viewer);
        // 匿名且未回答：对提问人以外隐藏是谁问的
        String asker = mine || !w.isAnonymous() || w.getAnswer() != null ? w.getFromUser() : "匿名小可爱";
        return new WhisperVO(w.getId(), w.getQuestion(), w.isAnonymous(), asker, w.getAnswer(),
                w.getAnsweredAt(), mine, w.getCreated());
    }

    // ========== F68 心灵感应 ==========

    public TelepathyBoardVO telepathyBoard(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        List<CoupleTelepathy> rounds = telepathyMapper.findBySpace(space.getId());
        long usedToday = rounds.stream().filter(r -> r.getDay().equals(today)).count();
        TelepathyRoundVO current = rounds.stream()
                .filter(r -> r.getDay().equals(today) && !r.bothAnswered())
                .findFirst().map(r -> toRoundVO(r, space, me)).orElse(null);
        List<TelepathyRoundVO> history = rounds.stream()
                .filter(CoupleTelepathy::bothAnswered)
                .limit(30)
                .map(r -> toRoundVO(r, space, me))
                .toList();
        long settled = history.size();
        long matched = history.stream().filter(TelepathyRoundVO::matched).count();
        int left = (int) Math.max(0, CoupleTelepathy.ROUND_MAX - usedToday);
        return new TelepathyBoardVO(current, history, left, matched, settled);
    }

    /** 发起一轮心灵感应：从题库随机出题（双方从选项里选，不许商量）；每天最多 3 轮。 */
    public TelepathyBoardVO startTelepathy(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        long usedToday = telepathyMapper.countByDay(space.getId(), today);
        if (usedToday >= CoupleTelepathy.ROUND_MAX) {
            throw new BusinessException(400, "今天 3 轮心灵感应都玩完啦，明天再来测同频 🧠");
        }
        boolean pendingExists = !telepathyMapper.findBySpace(space.getId()).stream()
                .filter(r -> r.getDay().equals(today) && !r.bothAnswered())
                .toList().isEmpty();
        if (pendingExists) {
            throw new BusinessException(400, "有一轮还在等 TA 作答，先等这轮揭晓");
        }
        Object[] picked = CoupleTalkBank.randomTelepathy();
        CoupleTelepathy round = CoupleTelepathy.of(space.getId(), (String) picked[0]);
        round.setRound((int) usedToday + 1);
        telepathyMapper.insert(round);
        push.pushCoupleEvent("telepathy-started", me, partner,
                "🧠 TA 发起了一轮「心灵感应」：" + picked[0] + " 快去作答，不许商量！");
        return telepathyBoard(me);
    }

    /** 作答：按 space.userA/userB 归档答案；双方都答完自动结算并揭晓是否同频。 */
    public TelepathyBoardVO answerTelepathy(String me, String answer) {
        if (answer == null || answer.isBlank() || answer.length() > CoupleTelepathy.ANSWER_MAX) {
            throw new BusinessException(400, "从选项里选一个哦");
        }
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        CoupleTelepathy round = telepathyMapper.findBySpace(space.getId()).stream()
                .filter(r -> r.getDay().equals(today) && !r.bothAnswered())
                .findFirst().orElse(null);
        if (round == null) {
            throw new BusinessException(404, "今天没有进行中的心灵感应，先发起一轮吧");
        }
        if (space.getUserA().equals(me)) {
            round.setAnswerA(answer.trim());
        } else {
            round.setAnswerB(answer.trim());
        }
        telepathyMapper.updateById(round);
        if (round.bothAnswered()) {
            String detail = round.matched()
                    ? "🧠✨ 心灵感应成功！你们都选了「" + round.getAnswerA() + "」——这就是同频吗！"
                    : "🧠 这轮感应失败：一个选「" + round.getAnswerA() + "」，一个选「" + round.getAnswerB()
                    + "」——但看到了彼此的脑回路，也不亏";
            push.pushCoupleEventBoth(round.matched() ? "telepathy-matched" : "telepathy-diff",
                    me, space.getUserA(), space.getUserB(), detail);
        } else {
            push.pushCoupleEvent("telepathy-answered", me, partner, "🧠 TA 已经交卷，就等你了！");
        }
        return telepathyBoard(me);
    }

    private TelepathyRoundVO toRoundVO(CoupleTelepathy round, CoupleSpace space, String viewer) {
        boolean mineStarted = round.getAnswerA() != null ^ round.getAnswerB() != null;
        String[] options = CoupleTalkBank.telepathyOptions(round.getQuestion());
        return new TelepathyRoundVO(round.getId(), round.getRound(), round.getQuestion(),
                options, round.getAnswerA(), round.getAnswerB(), round.bothAnswered(), round.matched(), mineStarted);
    }

    // ========== F69 情话储蓄罐 ==========

    public LoveBankBoardVO loveBank(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleLoveBank> mine = loveBankMapper.findByUser(space.getId(), me);
        long inJar = mine.stream().filter(b -> !b.isDelivered()).count();
        long delivered = mine.size() - inJar;
        List<LoveBankVO> vos = mine.stream()
                .map(b -> new LoveBankVO(b.getId(), b.getContent(), b.isDelivered(), b.getDeliveredAt(), b.getCreated()))
                .toList();
        return new LoveBankBoardVO(inJar, delivered, vos);
    }

    /** 存一句情话进罐子：TA 只会知道「你存了一句」，具体内容等利息日揭晓。 */
    public LoveBankBoardVO depositLove(String me, String content) {
        if (content == null || content.isBlank() || content.length() > CoupleLoveBank.CONTENT_MAX) {
            throw new BusinessException(400, "情话要写 " + CoupleLoveBank.CONTENT_MAX + " 字以内哦");
        }
        CoupleSpace space = requireSpace(me);
        loveBankMapper.insert(CoupleLoveBank.of(space.getId(), me, content.trim()));
        push.pushCoupleEvent("love-bank-deposit", me, space.partnerOf(me),
                "🏦 TA 悄悄往「情话储蓄罐」里存了一句话，会在某个晚上作为利息送达…");
        return loveBank(me);
    }

    /** 每晚 21:00 的利息：每人随机取一句未投递的情话送给 TA（有存货才推）。 */
    public void deliverInterest() {
        for (CoupleSpace space : spaceMapper.findAllActive()) {
            for (String user : List.of(space.getUserA(), space.getUserB())) {
                List<CoupleLoveBank> undelivered = loveBankMapper.findUndelivered(space.getId()).stream()
                        .filter(b -> b.getFromUser().equals(user))
                        .toList();
                if (undelivered.isEmpty()) {
                    continue;
                }
                CoupleLoveBank pick = undelivered.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(undelivered.size()));
                pick.setDelivered(true);
                pick.setDeliveredAt(System.currentTimeMillis());
                loveBankMapper.updateById(pick);
                push.pushCoupleEvent("love-bank-interest", user, space.partnerOf(user),
                        "💸 情话储蓄罐的今日利息：TA 曾悄悄存过一句——「" + pick.getContent() + "」");
            }
        }
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
