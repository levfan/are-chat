package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.question.DailyQuestion;
import com.smart.chat.couple.infrastructure.content.CoupleQuestionBank;
import com.smart.chat.couple.domain.question.QuestionAnswerRepository;
import com.smart.chat.couple.domain.question.QuestionAnswer;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 每日一问：每天给这个空间推一道题，两人各答一次，<b>双方都答过之后才互看</b>。
 * <p>
 * 选题由 {@link CoupleQuestionBank} 按「空间 + 日期」稳定哈希决定，题号与题干都落库存快照；
 * 可见性规则在 {@link DailyQuestion} 里，本 Service 只负责取数、落库与推送。
 */
@Service
public class CoupleQuestionService {

    /** 答题历史回看的上限天数 */
    private static final int HISTORY_MAX = 90;

    // ========== VO ==========

    public record AnswerVO(String username, String answer, Long createdAt, Long updatedAt) {
    }

    public record TodayVO(String day, int index, String question, AnswerVO mine, String partnerAnswer,
                          boolean answeredByMe, boolean answeredByPartner, boolean bothAnswered,
                          int answerMax) {
    }

    public record HistoryVO(String day, String question, String myAnswer, String partnerAnswer,
                            boolean bothAnswered) {
    }

    public record HistoryListVO(List<HistoryVO> items, int answeredDays, int bothAnsweredDays) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final QuestionAnswerRepository answerRepository;
    private final CoupleStreakService streakService;
    private final CoupleEventPublisher push;

    public CoupleQuestionService(CoupleSpaceRepository spaceRepository, QuestionAnswerRepository answerRepository,
                                 CoupleStreakService streakService, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.answerRepository = answerRepository;
        this.streakService = streakService;
        this.push = push;
    }

    // ========== 读 ==========

    /** 今日一问：题目 + 我的回答 + （双方都答完才有的）TA 的回答。 */
    public TodayVO today(String me) {
        CoupleSpace space = requireSpace(me);
        return todayOf(space, me);
    }

    TodayVO todayOf(CoupleSpace space, String me) {
        String day = LocalDate.now().toString();
        int index = CoupleQuestionBank.indexOf(space.id(), day);
        String question = CoupleQuestionBank.textAt(index);
        QuestionAnswer mine = answerRepository.find(space.id(), day, me).orElse(null);
        QuestionAnswer theirs = answerRepository.find(space.id(), day, space.partnerOf(me)).orElse(null);
        DailyQuestion view = DailyQuestion.of(day, index, question,
                mine == null ? null : mine.answerText(), theirs == null ? null : theirs.answerText());
        return new TodayVO(day, index, view.question(),
                mine == null ? null : new AnswerVO(mine.username(), mine.answerText(), mine.created(),
                        mine.updatedAt()),
                view.partnerAnswerText(), view.answeredByMe(), view.answeredByPartner(), view.bothAnswered(),
                DailyQuestion.ANSWER_MAX);
    }

    /** 回看最近 N 天（1-90，默认 14）：没答的一侧留空，答完才互看。 */
    public HistoryListVO history(String me, Integer days) {
        CoupleSpace space = requireSpace(me);
        int limit = days == null || days <= 0 ? 14 : Math.min(days, HISTORY_MAX);
        String startDay = LocalDate.now().minusDays(limit - 1L).toString();
        List<QuestionAnswer> rows = answerRepository.findBySpaceFrom(space.id(), startDay);
        List<String> dayKeys = new ArrayList<>();
        for (QuestionAnswer row : rows) {
            if (!dayKeys.contains(row.day())) {
                dayKeys.add(row.day());
            }
        }
        List<HistoryVO> items = new ArrayList<>();
        int bothCount = 0;
        int mineCount = 0;
        for (String day : dayKeys) {
            QuestionAnswer mine = findOf(rows, day, me);
            QuestionAnswer theirs = findOf(rows, day, space.partnerOf(me));
            String question = mine != null ? mine.question() : (theirs == null ? "" : theirs.question());
            DailyQuestion view = DailyQuestion.of(day, 0, question,
                    mine == null ? null : mine.answerText(), theirs == null ? null : theirs.answerText());
            if (view.answeredByMe()) {
                mineCount++;
            }
            if (view.bothAnswered()) {
                bothCount++;
            }
            items.add(new HistoryVO(day, view.question(), view.myAnswerText(), view.partnerAnswerText(),
                    view.bothAnswered()));
        }
        return new HistoryListVO(items, mineCount, bothCount);
    }

    // ========== 写 ==========

    /** 回答（或改写今天的答案）：闸门在 DailyQuestion，返回整份今日视图。 */
    public TodayVO answer(String me, String text) {
        CoupleSpace space = requireSpace(me);
        String answer = rule(() -> DailyQuestion.requireAnswerText(text));
        String day = LocalDate.now().toString();
        int index = CoupleQuestionBank.indexOf(space.id(), day);
        String question = CoupleQuestionBank.textAt(index);
        Optional<QuestionAnswer> existing = answerRepository.find(space.id(), day, me);
        if (existing.isPresent()) {
            QuestionAnswer row = existing.get();
            row.rewrite(answer);
            answerRepository.save(row);
        } else {
            answerRepository.save(QuestionAnswer.answer(space.id(), day, index, question, me, answer));
        }
        String partner = space.partnerOf(me);
        boolean partnerAnswered = answerRepository.find(space.id(), day, partner).isPresent();
        push.pushCoupleEvent("question-answered", me, partner,
                partnerAnswered ? "你们今天的每日一问都答完啦 💬 可以互看"
                        : "TA 答了今天的每日一问 💬 你也答一个就能互相看到");
        // 答完就是打卡：连续互动打卡的 AUTO 行只有这一个触发点
        if (partnerAnswered) {
            streakService.confirmBothAnswered(space, me);
        }
        return todayOf(space, me);
    }

    // ========== 定时任务 ==========

    /**
     * 每天 09:00 由 {@code CoupleQuestionJob} 调用：把这个空间当天的题推给双方。
     * 两个人都答完了就不打扰，没答完的那一方会收到提醒。
     */
    public void remindDailyQuestion() {
        String day = LocalDate.now().toString();
        for (CoupleSpace space : spaceRepository.findAllActive()) {
            if (answerRepository.findBySpaceAndDay(space.id(), day).size() >= 2) {
                continue;
            }
            String question = CoupleQuestionBank.textAt(CoupleQuestionBank.indexOf(space.id(), day));
            push.pushCoupleEventBoth("question-daily", space.userA(), space.userA(), space.userB(),
                    "今天的每日一问：" + question + " 💬 两个人都答完才能互看");
        }
    }

    // ========== 供其它服务只读复用 ==========

    /** 这个空间答过的总条数（百日回顾用）。 */
    public long answeredCount(String spaceId) {
        return answerRepository.findBySpace(spaceId).size();
    }

    /** 双方都答完的天数：心动值的供数之一，口径与「打卡一天」完全同源（一人一行才算答完）。 */
    public long bothAnsweredDays(String spaceId) {
        Set<String> both = new HashSet<>();
        Map<String, Set<String>> who = new HashMap<>();
        for (QuestionAnswer row : answerRepository.findBySpace(spaceId)) {
            if (row.answerText() != null && !row.answerText().isBlank()) {
                who.computeIfAbsent(row.day(), k -> new HashSet<>()).add(row.username());
            }
        }
        for (Map.Entry<String, Set<String>> entry : who.entrySet()) {
            if (entry.getValue().size() >= 2) {
                both.add(entry.getKey());
            }
        }
        return both.size();
    }

    /** 最近若干条问答（百日回顾的时间轴原料）。 */
    public List<QuestionAnswer> recentAnswers(String spaceId) {
        return answerRepository.findBySpace(spaceId);
    }

    private QuestionAnswer findOf(List<QuestionAnswer> rows, String day, String username) {
        for (QuestionAnswer row : rows) {
            if (row.day().equals(day) && row.username().equals(username)) {
                return row;
            }
        }
        return null;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
