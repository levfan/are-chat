package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会说情话·沟通增强（F100-F109，批次六）：恋爱翻译器、冷静角、情绪接力棒、
 * 你比划我猜、故事接龙、专属词典小考、情话合成器、道歉三部曲、情绪词汇足迹、晚安电台。
 * 情绪价值设计：让话变软（翻译/冷静角/道歉三部曲）、让情绪被接住（接力棒）、
 * 让两个人玩起来（比划猜/接龙/小考）、让每天有一句甜的（合成器/晚安电台）。
 */
@Service
public class CoupleCommService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleGuessRoundMapper guessMapper;
    private final ImPushService push;

    public CoupleCommService(CoupleSpaceMapper spaceMapper, CoupleGuessRoundMapper guessMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.guessMapper = guessMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record GuessVO(String id, String day, String fromUser, String word, String clue, String guess,
                          int attempts, String status, Long settledAt, Long created) {
    }

    // ========== F100 恋爱翻译器（静态） ==========

    // ========== F101 冷静角 ==========

    // ========== F102 情绪接力棒 ==========


    // ========== F103 你比划我猜 ==========

    public List<GuessVO> guessRounds(String me) {
        return guessMapper.findBySpace(requireSpace(me).getId()).stream()
                .map(r -> toGuessVO(r, me))
                .toList();
    }

    /** 比划猜 VO：词只给比划人看，结算后双方可见。 */
    private GuessVO toGuessVO(CoupleGuessRound r, String me) {
        boolean visible = r.getFromUser().equals(me)
                || CoupleGuessRound.STATUS_HIT.equals(r.getStatus())
                || CoupleGuessRound.STATUS_MISSED.equals(r.getStatus());
        return new GuessVO(r.getId(), r.getDay(), r.getFromUser(), visible ? r.getWord() : null,
                r.getClue(), r.getGuess(), r.getAttempts() == null ? 0 : r.getAttempts(),
                r.getStatus(), r.getSettledAt(), r.getCreated());
    }

    /** 开一轮：系统抽词，只有比划人能看到词。 */
    public List<GuessVO> startGuess(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        if (guessMapper.countByDay(space.getId(), day) >= CoupleGuessRound.DAILY_ROUNDS) {
            throw new BusinessException(400, "今天 " + CoupleGuessRound.DAILY_ROUNDS + " 轮额度用完啦，明天再来玩～");
        }
        CoupleGuessRound row = CoupleGuessRound.of(space.getId(), day, me,
                CoupleChatBank.pickGuessWord(space.getId(), day,
                        (int) (guessMapper.countByDay(space.getId(), day) + 1)));
        guessMapper.insert(row);
        push.pushCoupleEvent("guess-started", me, space.partnerOf(me),
                "🙈 TA 开了一轮「你比划我猜」，等你来猜！");
        return guessRounds(me);
    }

    /** 比划人出提示（不能包含原词）。 */
    public List<GuessVO> clueGuess(String me, String id, String clue) {
        CoupleSpace space = requireSpace(me);
        CoupleGuessRound row = requireGuess(space.getId(), id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(403, "只有比划的人才能出提示哦");
        }
        if (!CoupleGuessRound.STATUS_DRAWN.equals(row.getStatus())) {
            return guessRounds(me);
        }
        String c = clue == null || clue.isBlank() ? null : clue.trim();
        if (c == null) {
            throw new BusinessException(400, "提示不能为空，也不能直接说答案哦 🙊");
        }
        if (c.contains(row.getWord())) {
            throw new BusinessException(400, "提示里不能包含「" + row.getWord() + "」本身哦 🙈");
        }
        if (c.length() > CoupleGuessRound.CLUE_MAX) {
            throw new BusinessException(400, "提示写 " + CoupleGuessRound.CLUE_MAX + " 字以内啦");
        }
        row.setClue(c);
        row.setStatus(CoupleGuessRound.STATUS_CLUED);
        guessMapper.updateById(row);
        push.pushCoupleEvent("guess-clued", me, space.partnerOf(me),
                "🙉 提示来啦：「" + c + "」，猜猜是什么词？");
        return guessRounds(me);
    }

    /** 猜词人猜词：中则 HIT，错满 3 次则 MISSED。 */
    public List<GuessVO> doGuess(String me, String id, String word) {
        CoupleSpace space = requireSpace(me);
        CoupleGuessRound row = requireGuess(space.getId(), id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(403, "比划的人不能自己猜哦");
        }
        if (!CoupleGuessRound.STATUS_CLUED.equals(row.getStatus())) {
            return guessRounds(me);
        }
        String w = word == null ? "" : word.trim();
        row.setGuess(w);
        row.setAttempts(row.getAttempts() + 1);
        boolean hit = !w.isBlank() && w.equals(row.getWord());
        if (hit) {
            row.setStatus(CoupleGuessRound.STATUS_HIT);
            row.setSettledAt(System.currentTimeMillis());
        } else if (row.getAttempts() >= CoupleGuessRound.MAX_ATTEMPTS) {
            row.setStatus(CoupleGuessRound.STATUS_MISSED);
            row.setSettledAt(System.currentTimeMillis());
        }
        guessMapper.updateById(row);
        if (hit) {
            push.pushCoupleEventBoth("guess-hit", me, space.getUserA(), space.getUserB(),
                    "🎉 猜中啦！答案是「" + row.getWord() + "」，默契值 +1");
        } else if (CoupleGuessRound.STATUS_MISSED.equals(row.getStatus())) {
            push.pushCoupleEventBoth("guess-missed", me, space.getUserA(), space.getUserB(),
                    "🙃 3 次没猜中，答案是「" + row.getWord() + "」。比划的人检讨，猜的人重来！");

        } else {
            push.pushCoupleEvent("guess-wrong", me, row.getFromUser(),
                    "❌ 猜了「" + w + "」不对哦，还剩 " + (CoupleGuessRound.MAX_ATTEMPTS - row.getAttempts()) + " 次机会");
        }
        return guessRounds(me);
    }

    // ========== F104 故事接龙 ==========

    // ========== F105 专属词典小考（聚合恋爱词典，无状态） ==========

    // ========== F106 情话合成器（静态） ==========

    // ========== F107 道歉三部曲 ==========

    // ========== F108 情绪词汇足迹 ==========

    // ========== F109 晚安电台（聚合歌单 + 静态文案） ==========

    // ========== 内部工具 ==========

    private CoupleGuessRound requireGuess(String spaceId, String id) {
        CoupleGuessRound row = guessMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这轮对局哦");
        }
        return row;
    }

    /** 文本校验：非空 + 长度上限（message 非空时超限抛错）。 */
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

    private String trimEmoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            return null;
        }
        String e = emoji.trim();
        return e.length() <= 10 ? e : null;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
