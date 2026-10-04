package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.question.DailyQuestion;
import com.smart.chat.couple.infrastructure.content.CoupleQuestionBank;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestionAnswerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestionAnswerPO;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final CoupleQuestionAnswerMapper answerMapper;
    private final CoupleEventPublisher push;

    public CoupleQuestionService(CoupleSpaceRepository spaceRepository, CoupleQuestionAnswerMapper answerMapper,
                                 CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.answerMapper = answerMapper;
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
        CoupleQuestionAnswerPO mine = answerMapper.find(space.id(), day, me).orElse(null);
        CoupleQuestionAnswerPO theirs = answerMapper.find(space.id(), day, space.partnerOf(me)).orElse(null);
        DailyQuestion view = DailyQuestion.of(day, index, question,
                mine == null ? null : mine.getAnswer(), theirs == null ? null : theirs.getAnswer());
        return new TodayVO(day, index, view.question(),
                mine == null ? null : new AnswerVO(mine.getUsername(), mine.getAnswer(), mine.getCreated(),
                        mine.getUpdatedAt()),
                view.partnerAnswerText(), view.answeredByMe(), view.answeredByPartner(), view.bothAnswered(),
                DailyQuestion.ANSWER_MAX);
    }

    /** 回看最近 N 天（1-90，默认 14）：没答的一侧留空，答完才互看。 */
    public HistoryListVO history(String me, Integer days) {
        CoupleSpace space = requireSpace(me);
        int limit = days == null || days <= 0 ? 14 : Math.min(days, HISTORY_MAX);
        String startDay = LocalDate.now().minusDays(limit - 1L).toString();
        List<CoupleQuestionAnswerPO> rows = answerMapper.findBySpaceFrom(space.id(), startDay);
        List<String> dayKeys = new ArrayList<>();
        for (CoupleQuestionAnswerPO row : rows) {
            if (!dayKeys.contains(row.getDay())) {
                dayKeys.add(row.getDay());
            }
        }
        List<HistoryVO> items = new ArrayList<>();
        int bothCount = 0;
        int mineCount = 0;
        for (String day : dayKeys) {
            CoupleQuestionAnswerPO mine = findOf(rows, day, me);
            CoupleQuestionAnswerPO theirs = findOf(rows, day, space.partnerOf(me));
            String question = mine != null ? mine.getQuestion() : (theirs == null ? "" : theirs.getQuestion());
            DailyQuestion view = DailyQuestion.of(day, 0, question,
                    mine == null ? null : mine.getAnswer(), theirs == null ? null : theirs.getAnswer());
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
        Optional<CoupleQuestionAnswerPO> existing = answerMapper.find(space.id(), day, me);
        if (existing.isPresent()) {
            CoupleQuestionAnswerPO row = existing.get();
            row.setAnswer(answer);
            row.setUpdatedAt(System.currentTimeMillis());
            answerMapper.updateById(row);
        } else {
            answerMapper.insert(CoupleQuestionAnswerPO.of(space.id(), day, index, question, me, answer));
        }
        String partner = space.partnerOf(me);
        boolean partnerAnswered = answerMapper.find(space.id(), day, partner).isPresent();
        push.pushCoupleEvent("question-answered", me, partner,
                partnerAnswered ? "你们今天的每日一问都答完啦 💬 可以互看"
                        : "TA 答了今天的每日一问 💬 你也答一个就能互相看到");
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
            if (answerMapper.findBySpaceAndDay(space.id(), day).size() >= 2) {
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
        return answerMapper.findBySpace(spaceId).size();
    }

    /** 最近若干条问答（百日回顾的时间轴原料）。 */
    public List<CoupleQuestionAnswerPO> recentAnswers(String spaceId) {
        return answerMapper.findBySpace(spaceId);
    }

    private CoupleQuestionAnswerPO findOf(List<CoupleQuestionAnswerPO> rows, String day, String username) {
        for (CoupleQuestionAnswerPO row : rows) {
            if (row.getDay().equals(day) && row.getUsername().equals(username)) {
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
