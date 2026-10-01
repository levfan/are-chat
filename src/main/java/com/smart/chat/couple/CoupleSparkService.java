package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 默契亲密系（F170-F179，批次十三）：爱语测评与对照、心动闪光、「如果」问答、
 * 动作暗语本、同频共振、默契仪表盘、心动日历、同频排行榜、默契周报。
 * 情绪价值设计：亲密不是玄学，是可以练习的——知道对方的爱语、存住心动的瞬间、
 * 用 10 秒按键对一次拍子、在日历上给今天盖一个心动邮戳。
 */
@Service
public class CoupleSparkService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleLoveLangMapper loveLangMapper;
    private final CoupleHeartFlashMapper flashMapper;
    private final CoupleWhatIfMapper whatIfMapper;
    private final CoupleSecretSignalMapper signalMapper;
    private final CoupleSyncTapMapper tapMapper;
    private final CoupleHeartDayMapper heartDayMapper;
    private final ImPushService push;

    public CoupleSparkService(CoupleSpaceMapper spaceMapper, CoupleLoveLangMapper loveLangMapper,
                              CoupleHeartFlashMapper flashMapper, CoupleWhatIfMapper whatIfMapper,
                              CoupleSecretSignalMapper signalMapper, CoupleSyncTapMapper tapMapper,
                              CoupleHeartDayMapper heartDayMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.loveLangMapper = loveLangMapper;
        this.flashMapper = flashMapper;
        this.whatIfMapper = whatIfMapper;
        this.signalMapper = signalMapper;
        this.tapMapper = tapMapper;
        this.heartDayMapper = heartDayMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record LoveLangVO(String fromUser, boolean mine, List<Integer> scores, String primaryLang, Long updatedAt) {
    }

    public record LoveLangPairVO(CoupleLoveLang mine, CoupleLoveLang partner,
                                 String myLang, String partnerLang,
                                 String myTip, String partnerTip) {
    }

    public record FlashVO(String id, String fromUser, boolean mine, String moment, Long created) {
    }

    public record WhatIfVO(String day, String question, CoupleWhatIf mine, CoupleWhatIf partner,
                           boolean bothAnswered, String firstStar) {
    }

    public record SignalVO(String id, String fromUser, boolean mine, String signal, String meaning, Long created) {
    }

    /** 单次按键返回：配对成功时 diffMs 非空。 */
    public record TapResultVO(Long diffMs, boolean hit, Long bestMs, Integer attempts, Integer hits) {
    }

    public record HeartDayVO(String id, String day, String fromUser, boolean mine, Integer level, Long updatedAt) {
    }

    public record SyncRankVO(String day, Long bestMs, Integer attempts, Integer hits) {
    }

    public record SparkDashboardVO(Integer score, String label, Long bestMs, Integer whatIfBothDays,
                                   Integer heartDays, Integer signals) {
    }

    public record SparkWeeklyVO(Integer whatIfBoth, Integer heartMarks, Integer syncAttempts, String summary) {
    }

    // ========== F170 爱语测评 ==========

    public List<CoupleSparkBank.QuizQuestion> quiz() {
        return CoupleSparkBank.quiz();
    }

    /** 提交答卷（12 题 A/B），算分并落库（可重测覆盖）。 */
    public LoveLangVO submitLoveLang(String me, List<String> answers) {
        CoupleSpace space = requireSpace(me);
        List<CoupleSparkBank.QuizQuestion> quiz = CoupleSparkBank.quiz();
        if (answers == null || answers.size() != quiz.size()) {
            throw new BusinessException(400, "要答完 " + quiz.size() + " 道题才算数哦");
        }
        CoupleLoveLang row = CoupleLoveLang.of(space.getId(), me, CoupleLoveLang.LANG_WORDS);
        for (int i = 0; i < answers.size(); i++) {
            String a = answers.get(i);
            CoupleSparkBank.QuizQuestion q = quiz.get(i);
            String lang;
            if ("A".equalsIgnoreCase(a)) {
                lang = q.optionA().lang();
            } else if ("B".equalsIgnoreCase(a)) {
                lang = q.optionB().lang();
            } else {
                throw new BusinessException(400, "第 " + (i + 1) + " 题的答案要是 A 或 B");
            }
            row.addScore(lang, 1);
        }
        String primary = CoupleSparkBank.LANGS.stream()
                .max((a, b) -> Integer.compare(row.scoreOf(b.code()), row.scoreOf(a.code())))
                .map(CoupleSparkBank.LangMeta::code)
                .orElse(CoupleLoveLang.LANG_WORDS);
        row.setPrimaryLang(primary);
        CoupleLoveLang existing = loveLangMapper.find(space.getId(), me);
        if (existing != null) {
            row.setId(existing.getId());
            row.setCreated(existing.getCreated());
            loveLangMapper.updateById(row);
        } else {
            loveLangMapper.insert(row);
        }
        push.pushCoupleEvent("love-lang-done", me, space.partnerOf(me),
                "💗 TA 的爱语说明书出炉了：主爱语是「" + CoupleSparkBank.langOf(primary).name() + "」——快去对照你的。");
        return toLangVO(row, me);
    }

    public LoveLangVO myLoveLang(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleLoveLang row = loveLangMapper.find(space.getId(), me);
        if (row == null) {
            throw new BusinessException(404, "还没做过爱语测评，先答一遍 12 道题吧");
        }
        return toLangVO(row, me);
    }

    private LoveLangVO toLangVO(CoupleLoveLang row, String me) {
        List<Integer> scores = CoupleSparkBank.LANGS.stream().map(l -> row.scoreOf(l.code())).toList();
        return new LoveLangVO(row.getFromUser(), row.getFromUser().equals(me), scores, row.getPrimaryLang(), row.getUpdatedAt());
    }

    // ========== F171 爱语对照卡（聚合，无新表） ==========

    public LoveLangPairVO loveLangPair(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleLoveLang mine = loveLangMapper.find(space.getId(), me);
        CoupleLoveLang partner = loveLangMapper.find(space.getId(), space.partnerOf(me));
        if (mine == null || partner == null) {
            throw new BusinessException(404, "要两个人都完成测评才能出对照卡哦（还差一人）");
        }
        CoupleSparkBank.LangMeta myMeta = CoupleSparkBank.langOf(mine.getPrimaryLang());
        CoupleSparkBank.LangMeta partnerMeta = CoupleSparkBank.langOf(partner.getPrimaryLang());
        return new LoveLangPairVO(mine, partner, myMeta.name(), partnerMeta.name(), myMeta.tip(), partnerMeta.tip());
    }

    // ========== F172 心动闪光 ==========

    public List<FlashVO> flashes(String me) {
        CoupleSpace space = requireSpace(me);
        return flashMapper.findBySpace(space.getId()).stream()
                .map(f -> new FlashVO(f.getId(), f.getFromUser(), f.getFromUser().equals(me), f.getMoment(), f.getCreated()))
                .toList();
    }

    /** 速记一次心动。 */
    public List<FlashVO> addFlash(String me, String moment) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(moment, CoupleHeartFlash.MOMENT_MAX, "心动一刻写 " + CoupleHeartFlash.MOMENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "把突然软下来的那一瞬间写下来 ⚡");
        }
        flashMapper.insert(CoupleHeartFlash.of(space.getId(), me, text));
        push.pushCoupleEvent("heart-flash", me, space.partnerOf(me),
                "⚡ TA 记下了一个和你有关的心动瞬间，去猜猜是哪一刻。");
        return flashes(me);
    }

    // ========== F173 「如果」问答 ==========

    public WhatIfVO whatIf(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleWhatIf mine = whatIfMapper.find(space.getId(), day, me);
        CoupleWhatIf partner = whatIfMapper.find(space.getId(), day, space.partnerOf(me));
        boolean both = mine != null && partner != null;
        String star = null;
        if (both) {
            // 先答者为今日默契之星
            star = mine.getCreated() <= partner.getCreated() ? me : space.partnerOf(me);
        }
        return new WhatIfVO(day, CoupleSparkBank.pickWhatIf(space.getId(), day), mine, both ? partner : null, both, star);
    }

    /** 回答今日「如果」（可改）。 */
    public WhatIfVO answerWhatIf(String me, String answer) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(answer, CoupleWhatIf.ANSWER_MAX, "答案写 " + CoupleWhatIf.ANSWER_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "大胆想，聊点不着边际的才有趣 🌌");
        }
        String day = LocalDate.now().toString();
        CoupleWhatIf row = whatIfMapper.find(space.getId(), day, me);
        if (row == null) {
            whatIfMapper.insert(CoupleWhatIf.of(space.getId(), day, me, text));
        } else {
            row.setAnswer(text);
            row.setUpdatedAt(System.currentTimeMillis());
            whatIfMapper.updateById(row);
        }
        if (whatIfMapper.find(space.getId(), day, space.partnerOf(me)) != null) {
            push.pushCoupleEventBoth("whatif-both", me, space.getUserA(), space.getUserB(),
                    "🌌 今日「如果」双方都作答了，看看你们会不会不约而同。");
        } else {
            push.pushCoupleEvent("whatif-answered", me, space.partnerOf(me),
                    "🌌 TA 已经答了今日「如果」，先答的人是默契之星 ⭐");
        }
        return whatIf(me);
    }

    // ========== F174 动作暗语本 ==========

    public List<SignalVO> signals(String me) {
        CoupleSpace space = requireSpace(me);
        return signalMapper.findBySpace(space.getId()).stream()
                .map(s -> new SignalVO(s.getId(), s.getFromUser(), s.getFromUser().equals(me), s.getSignal(), s.getMeaning(), s.getCreated()))
                .toList();
    }

    /** 约定一个动作暗语。 */
    public List<SignalVO> addSignal(String me, String signal, String meaning) {
        CoupleSpace space = requireSpace(me);
        String s = trimLimit(signal, CoupleSecretSignal.SIGNAL_MAX, "动作写 " + CoupleSecretSignal.SIGNAL_MAX + " 字以内哦");
        String m = trimLimit(meaning, CoupleSecretSignal.MEANING_MAX, "含义写 " + CoupleSecretSignal.MEANING_MAX + " 字以内哦");
        if (s == null || m == null) {
            throw new BusinessException(400, "动作和它代表的那句话都要写 🤝");
        }
        signalMapper.insert(CoupleSecretSignal.of(space.getId(), me, s, m));
        push.pushCoupleEvent("signal-added", me, space.partnerOf(me),
                "🤝 TA 和你约定了一个新暗语，人前不能说的话又多了一种说法。");
        return signals(me);
    }

    // ========== F175 同频共振 ==========

    /** 按键：10 秒窗口内双方先后按下即配对，差值越小越默契。 */
    public TapResultVO tap(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        long now = System.currentTimeMillis();
        CoupleSyncTap row = tapMapper.find(space.getId(), day);
        if (row == null) {
            row = CoupleSyncTap.of(space.getId(), day);
            tapMapper.insert(row);
        }
        Long diff = null;
        boolean hit = false;
        if (row.getLastTapUser() != null && !row.getLastTapUser().equals(me)
                && row.getLastTapAt() != null && now - row.getLastTapAt() <= CoupleSyncTap.WINDOW_MS) {
            diff = Math.abs(now - row.getLastTapAt());
            hit = diff <= CoupleSyncTap.HIT_MS;
            row.setAttempts(row.getAttempts() + 1);
            if (row.getBestMs() == null || diff < row.getBestMs()) {
                row.setBestMs(diff);
            }
            if (hit) {
                row.setHits(row.getHits() + 1);
                push.pushCoupleEventBoth("sync-tap-hit", me, space.getUserA(), space.getUserB(),
                        "🎧 同频成功！你们这次只差 " + diff + " 毫秒——这就是默契的样子。");
            }
            row.setLastTapUser(null);
            row.setLastTapAt(null);
        } else {
            row.setLastTapUser(me);
            row.setLastTapAt(now);
        }
        row.setUpdatedAt(now);
        tapMapper.updateById(row);
        return new TapResultVO(diff, hit, row.getBestMs(), row.getAttempts(), row.getHits());
    }

    public TapResultVO todayTap(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleSyncTap row = tapMapper.find(space.getId(), LocalDate.now().toString());
        return new TapResultVO(null, false, row == null ? null : row.getBestMs(),
                row == null ? 0 : row.getAttempts(), row == null ? 0 : row.getHits());
    }

    // ========== F177 心动日历 ==========

    public List<HeartDayVO> heartDays(String me) {
        CoupleSpace space = requireSpace(me);
        return heartDayMapper.findBySpace(space.getId()).stream()
                .map(h -> new HeartDayVO(h.getId(), h.getDay(), h.getFromUser(), h.getFromUser().equals(me), h.getLevel(), h.getUpdatedAt()))
                .toList();
    }

    /** 给今天盖心动邮戳（1 平淡 / 2 甜甜 / 3 心动爆棚，可改）。 */
    public List<HeartDayVO> markHeartDay(String me, Integer level) {
        CoupleSpace space = requireSpace(me);
        int lv = level == null ? 1 : Math.max(CoupleHeartDay.LEVEL_MIN, Math.min(CoupleHeartDay.LEVEL_MAX, level));
        String day = LocalDate.now().toString();
        CoupleHeartDay row = heartDayMapper.find(space.getId(), day, me);
        if (row == null) {
            heartDayMapper.insert(CoupleHeartDay.of(space.getId(), day, me, lv));
        } else {
            row.setLevel(lv);
            row.setUpdatedAt(System.currentTimeMillis());
            heartDayMapper.updateById(row);
        }
        return heartDays(me);
    }

    // ========== F176 默契仪表盘（聚合，无新表） ==========

    public SparkDashboardVO dashboard(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleSyncTap> taps = tapMapper.findBySpace(space.getId());
        Long bestMs = taps.stream().map(CoupleSyncTap::getBestMs).filter(java.util.Objects::nonNull)
                .min(Long::compareTo).orElse(null);

        // 双答天数：what_if 按 day 分组统计出现两人的天数
        List<CoupleWhatIf> answers = whatIfMapper.findBySpace(space.getId());
        long bothDays = answers.stream().map(CoupleWhatIf::getDay).distinct().count();

        List<CoupleHeartDay> hearts = heartDayMapper.findBySpace(space.getId());
        List<CoupleSecretSignal> signals = signalMapper.findBySpace(space.getId());

        int score = 0;
        if (bestMs != null) {
            score += bestMs <= 100 ? 40 : bestMs <= 250 ? 30 : bestMs <= 500 ? 20 : 10;
        }
        score += Math.min(30, (int) bothDays * 3);
        score += Math.min(20, hearts.size() * 2);
        score += Math.min(10, signals.size() * 2);
        String label = score >= 80 ? "心有灵犀" : score >= 60 ? "渐入佳境" : score >= 30 ? "培养中" : "刚起步";
        return new SparkDashboardVO(Math.min(100, score), label, bestMs, (int) bothDays, hearts.size(), signals.size());
    }

    // ========== F178 同频排行榜（聚合，无新表） ==========

    public List<SyncRankVO> syncRank(String me) {
        CoupleSpace space = requireSpace(me);
        return tapMapper.findBySpace(space.getId()).stream()
                .filter(t -> t.getBestMs() != null)
                .sorted((a, b) -> Long.compare(a.getBestMs(), b.getBestMs()))
                .limit(10)
                .map(t -> new SyncRankVO(t.getDay(), t.getBestMs(), t.getAttempts(), t.getHits()))
                .toList();
    }

    // ========== F179 默契周报（聚合，无新表） ==========

    public SparkWeeklyVO weekly(String me) {
        CoupleSpace space = requireSpace(me);
        String monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();

        int whatIfBoth = 0;
        List<CoupleWhatIf> answers = whatIfMapper.findBySpace(space.getId());
        for (String day : answers.stream().map(CoupleWhatIf::getDay).distinct().toList()) {
            if (day.compareTo(monday) >= 0
                    && answers.stream().anyMatch(a -> a.getDay().equals(day))
                    && answers.stream().filter(a -> a.getDay().equals(day)).count() >= 1
                    && answers.stream().filter(a -> a.getDay().equals(day)).map(CoupleWhatIf::getFromUser).distinct().count() >= 2) {
                whatIfBoth++;
            }
        }
        int heartMarks = (int) heartDayMapper.findBySpace(space.getId()).stream()
                .filter(h -> h.getDay().compareTo(monday) >= 0).count();
        int syncAttempts = tapMapper.findBySpace(space.getId()).stream()
                .filter(t -> t.getDay().compareTo(monday) >= 0)
                .mapToInt(CoupleSyncTap::getAttempts).sum();

        StringBuilder summary = new StringBuilder("本周你们");
        if (whatIfBoth > 0) {
            summary.append("一起答了 ").append(whatIfBoth).append(" 天「如果」，");
        }
        if (syncAttempts > 0) {
            summary.append("同频按键尝试 ").append(syncAttempts).append(" 次，");
        }
        if (heartMarks > 0) {
            summary.append("盖了 ").append(heartMarks).append(" 个心动邮戳，");
        }
        if (summary.length() == 4) {
            summary.append("的默契还没开张，从一道「如果」开始吧 🌌");
        } else {
            summary.setLength(summary.length() - 1);
            summary.append("。默契就是这样一点点攒出来的。");
        }
        return new SparkWeeklyVO(whatIfBoth, heartMarks, syncAttempts, summary.toString());
    }

    // ========== 内部工具 ==========

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
}
