package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 倾听与发声（F260-F269，批次二十二）：想被听时段、替我说、误会倒带、卡壳一问、
 * 换位信、早想说队列、三行打卡、语气翻译官、休战旗、称呼日。
 * 情绪价值设计：把「你没在听我说话」这类指控，改写成「我们约一个 10 分钟的耳朵」——
 * 沟通不是讲理，是被听见。
 */
@Service
public class CoupleListenService {

    static final int SLOT_TOPIC_MAX = 140;
    static final int PROXY_MAX = 200;
    static final int MIS_MAX = 200;
    static final int LETTER_MAX = 500;
    static final int THREE_LINE_MAX = 80;
    static final int THREE_STREAK_BADGE = 21;
    static final int HOLD_GAP_DAYS = 7;
    static final int TRUCE_DEFAULT_MIN = 30;
    static final int TRUCE_MIN_MIN = 10;
    static final int TRUCE_MAX_MIN = 120;
    static final long TRUCE_RESUME_WINDOW_MS = 24 * 60 * 60 * 1000L;
    static final int MIS_LOOKBACK_DAYS = 30;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleListenSlotMapper slotMapper;
    private final CoupleProxyWordMapper proxyMapper;
    private final CoupleMisrewindMapper misMapper;
    private final CoupleStuckQMapper stuckMapper;
    private final CoupleSwapLetterMapper letterMapper;
    private final CoupleHoldWordMapper holdMapper;
    private final CoupleThreeLineMapper threeMapper;
    private final CoupleToneNoteMapper toneMapper;
    private final CoupleTruceMapper truceMapper;
    private final CoupleNameDayMapper nameMapper;
    private final ImPushService push;

    public CoupleListenService(CoupleSpaceMapper spaceMapper, CoupleListenSlotMapper slotMapper,
                               CoupleProxyWordMapper proxyMapper, CoupleMisrewindMapper misMapper,
                               CoupleStuckQMapper stuckMapper, CoupleSwapLetterMapper letterMapper,
                               CoupleHoldWordMapper holdMapper, CoupleThreeLineMapper threeMapper,
                               CoupleToneNoteMapper toneMapper, CoupleTruceMapper truceMapper,
                               CoupleNameDayMapper nameMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.slotMapper = slotMapper;
        this.proxyMapper = proxyMapper;
        this.misMapper = misMapper;
        this.stuckMapper = stuckMapper;
        this.letterMapper = letterMapper;
        this.holdMapper = holdMapper;
        this.threeMapper = threeMapper;
        this.toneMapper = toneMapper;
        this.truceMapper = truceMapper;
        this.nameMapper = nameMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record SlotVO(String id, String day, String topic, String status, boolean mine,
                         boolean confirmed, Integer rateMine, Integer ratePartner, String note) {
    }

    public record ProxyVO(String id, String content, String fromUser, boolean mine, String status,
                          String finalText, String adoptedBy) {
    }

    public record MisVO(String day, String topic, String mineThought, String mineGuess,
                        String partnerThought, String partnerGuess, boolean both) {
    }

    public record StuckVO(String id, String question, String answer, boolean mine, boolean answered) {
    }

    public record LetterVO(String id, String day, String openDay, String status, boolean mine,
                           boolean due, String content) {
    }

    public record HoldVO(String id, String content, String fromUser, boolean mine, String status,
                         String openDay, Long sentAt) {
    }

    public record ThreeVO(String morning, String thanks, String praise, Integer streakMine,
                          Integer streakPartner, Integer badgeDays) {
    }

    public record ToneVO(String fromUser, String tone, String label, String line, boolean mine) {
    }

    public record TruceVO(String id, String raiser, Long untilAt, boolean expired, boolean mine,
                          Integer decideA, Integer decideB, boolean ended) {
    }

    public record NameDayVO(String day, String name, boolean usedMine, boolean usedPartner, boolean done) {
    }

    public record TodayVO(String day, String week, SlotVO slot, List<SlotVO> recentSlots,
                          ProxyVO proxyDraft, List<ProxyVO> adopted, List<MisVO> misrewinds,
                          List<StuckVO> stuck, List<LetterVO> letters, List<HoldVO> holds,
                          String nextHoldDay, ThreeVO three, List<ToneVO> tones, TruceVO truce,
                          NameDayVO nameDay) {
    }

    // ========== 读：今日倾听台（含惰性结算：早想说放行 / 休战到期） ==========

    /** 今日倾听与发声总览。 */
    public TodayVO today(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String week = now.with(DayOfWeek.MONDAY).toString();
        settleDue(space, now);
        return buildToday(space, me, now, day, week);
    }

    // ========== F260 想被听时段 ==========

    /** 申请 10 分钟「只听我说」时段。 */
    public TodayVO requestSlot(String me, String topic) {
        CoupleSpace space = requireSpace(me);
        String t = trim(topic, "想聊的主题要写一句");
        if (t.length() > SLOT_TOPIC_MAX) {
            throw new BusinessException(400, "主题最多 140 字，剩下的见面说");
        }
        if (!slotMapper.findCurrent(space.getId()).isEmpty()) {
            throw new BusinessException(400, "已有一个时段在途，先聊完这一个");
        }
        slotMapper.insert(CoupleListenSlot.of(space.getId(), LocalDate.now().toString(), me, t));
        push.pushCoupleEvent("slot-request", me, space.partnerOf(me),
                "TA 申请一个「只听我说」时段：" + t + " 🎧 确认一下？");
        return today(me);
    }

    /** 倾听人确认开麦。 */
    public TodayVO confirmSlot(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleListenSlot row = requireSlot(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "说的人不能自己确认，耳朵是 TA 的");
        }
        if (!CoupleListenSlot.STATUS_OPEN.equals(row.getStatus())) {
            return today(me);
        }
        row.setStatus(CoupleListenSlot.STATUS_CONFIRMED);
        row.setConfirmedAt(System.currentTimeMillis());
        slotMapper.updateById(row);
        push.pushCoupleEvent("slot-confirmed", me, row.getFromUser(), "TA 已就位，麦交给你，说吧 🎤");
        return today(me);
    }

    /** 聊完收场（任意一方点）。 */
    public TodayVO doneSlot(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleListenSlot row = requireSlot(space, id);
        if (!CoupleListenSlot.STATUS_CONFIRMED.equals(row.getStatus())) {
            throw new BusinessException(400, "TA 还没确认开麦，先等等");
        }
        row.setStatus(CoupleListenSlot.STATUS_DONE);
        row.setDoneAt(System.currentTimeMillis());
        slotMapper.updateById(row);
        push.pushCoupleEventBoth("slot-done", me, space.getUserA(), space.getUserB(),
                "这 10 分钟聊完了，互相打个「被听见分」吧 📝");
        return today(me);
    }

    /** 互评被听感（说的人给表达分，听的人给被听分，双评齐推 both）。 */
    public TodayVO rateSlot(String me, String id, Integer score, String note) {
        CoupleSpace space = requireSpace(me);
        CoupleListenSlot row = requireSlot(space, id);
        if (!CoupleListenSlot.STATUS_DONE.equals(row.getStatus())) {
            throw new BusinessException(400, "聊完才能打分");
        }
        boolean speaker = row.getFromUser().equals(me);
        int s = clampScore(score);
        String line = trim(note, "");
        if (speaker) {
            if (row.getRateMine() != null) {
                return today(me);
            }
            row.setRateMine(s);
            row.setNote(line.isEmpty() ? CoupleListenBank.rateLine(s) : line);
        } else {
            if (row.getRatePartner() != null) {
                return today(me);
            }
            row.setRatePartner(s);
        }
        slotMapper.updateById(row);
        if (row.getRateMine() != null && row.getRatePartner() != null) {
            push.pushCoupleEventBoth("slot-rated", me, space.getUserA(), space.getUserB(),
                    "本场被听见互评：" + row.getNote() + "（" + row.getRateMine() + "/" + row.getRatePartner() + "）");
        } else {
            push.pushCoupleEvent("slot-rate", me, space.partnerOf(me), "TA 给这次倾听打了分 🎧");
        }
        return today(me);
    }

    // ========== F261 替我说 ==========

    /** 以 TA 口吻写一句你不好意思说的话（一人一份在途草稿，可覆盖）。 */
    public TodayVO proxy(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String t = trim(content, "替 TA 说的话要写出来");
        if (t.length() > PROXY_MAX) {
            throw new BusinessException(400, "最多 200 字，留白也是台词");
        }
        CoupleProxyWord draft = proxyMapper.findDraft(space.getId(), me);
        if (draft != null) {
            draft.setContent(t);
            draft.setUpdatedAt(System.currentTimeMillis());
            proxyMapper.updateById(draft);
        } else {
            proxyMapper.insert(CoupleProxyWord.of(space.getId(), t, me));
        }
        push.pushCoupleEvent("proxy-word", me, space.partnerOf(me),
                "TA 用你的口吻写了一句心里话，看看要不要照念 💌");
        return today(me);
    }

    /** 被代笔的人定稿（可改写后定稿；不能定稿自己写的）。 */
    public TodayVO adoptProxy(String me, String id, String finalText) {
        CoupleSpace space = requireSpace(me);
        CoupleProxyWord row = requireProxy(space, id);
        if (CoupleProxyWord.STATUS_ADOPTED.equals(row.getStatus())) {
            return today(me);
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己代笔的词，不能自己定稿");
        }
        String t = trim(finalText, row.getContent());
        if (t.length() > PROXY_MAX) {
            throw new BusinessException(400, "定稿最多 200 字");
        }
        row.setStatus(CoupleProxyWord.STATUS_ADOPTED);
        row.setFinalText(t);
        row.setAdoptedBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        proxyMapper.updateById(row);
        push.pushCoupleEventBoth("proxy-adopted", me, space.getUserA(), space.getUserB(),
                "这句话被定稿了：「" + t + "」");
        return today(me);
    }

    // ========== F262 误会倒带卡 ==========

    /** 就一次争执写下「我当时以为/我猜你其实想」，双份齐并排回放。 */
    public TodayVO misrewind(String me, String topic, String mine, String theirs) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        String tp = trim(topic, "争执主题要写一句");
        if (tp.length() > 60) {
            throw new BusinessException(400, "主题最多 60 字");
        }
        String m = trim(mine, "");
        String g = trim(theirs, "");
        if (m.isEmpty() && g.isEmpty()) {
            throw new BusinessException(400, "两边至少写一边，倒带才有画面对");
        }
        if (m.length() > MIS_MAX || g.length() > MIS_MAX) {
            throw new BusinessException(400, "每条最多 200 字");
        }
        CoupleMisrewind exist = misMapper.find(space.getId(), day, tp, me);
        if (exist != null) {
            exist.setMine(m);
            exist.setTheirs(g);
            misMapper.updateById(exist);
            return today(me);
        }
        misMapper.insert(CoupleMisrewind.of(space.getId(), day, tp, me, m, g));
        String partner = space.partnerOf(me);
        if (misMapper.find(space.getId(), day, tp, partner) != null) {
            push.pushCoupleEventBoth("misrewind-done", me, space.getUserA(), space.getUserB(),
                    "「" + tp + "」的误会倒带双份齐了，并排看看 📼");
        } else {
            push.pushCoupleEvent("misrewind", me, partner, "TA 倒带了一次「" + tp + "」，等你的那份");
        }
        return today(me);
    }

    // ========== F263 本周答不上来的问题 ==========

    /** 本周抛出一个难住我的问题。 */
    public TodayVO stuck(String me, String question) {
        CoupleSpace space = requireSpace(me);
        String week = LocalDate.now().with(DayOfWeek.MONDAY).toString();
        String q = trim(question, "问题要写出来");
        if (q.length() > SLOT_TOPIC_MAX) {
            throw new BusinessException(400, "问题最多 140 字");
        }
        if (stuckMapper.find(space.getId(), week, me) != null) {
            throw new BusinessException(400, "本周你已经问过一题了，下周再来");
        }
        stuckMapper.insert(CoupleStuckQ.of(space.getId(), week, me, q));
        push.pushCoupleEvent("stuck-question", me, space.partnerOf(me),
                "TA 这周被一个问题难住了，等你作答 🤔");
        return today(me);
    }

    /** 答对方的题；双答齐推 both。 */
    public TodayVO answerStuck(String me, String id, String answer) {
        CoupleSpace space = requireSpace(me);
        CoupleStuckQ row = stuckMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这道题不存在");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己的题不能自己答，那是答案不是倾听");
        }
        String a = trim(answer, "答案总要写一点");
        if (a.length() > MIS_MAX) {
            throw new BusinessException(400, "答案最多 200 字");
        }
        if (row.isAnswered()) {
            return today(me);
        }
        row.setAnswer(a);
        row.setAnsweredAt(System.currentTimeMillis());
        stuckMapper.updateById(row);
        String partner = space.partnerOf(me);
        CoupleStuckQ mineQ = stuckMapper.find(space.getId(), row.getWeek(), me);
        if (mineQ != null && mineQ.isAnswered()) {
            push.pushCoupleEventBoth("stuck-both", me, space.getUserA(), space.getUserB(),
                    "本周两题都答完了，默契 +1 📮");
        } else {
            push.pushCoupleEvent("stuck-answered", me, partner, "TA 答了你的题，去看看 🤔");
        }
        return today(me);
    }

    // ========== F264 换位信 ==========

    /** 以对方的口吻写信（一封信封存到开放日，到期由对方拆）。 */
    public TodayVO letter(String me, String content, String openDay) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String c = trim(content, "信总要写几句");
        if (c.length() > LETTER_MAX) {
            throw new BusinessException(400, "信最多 500 字，情不至字不在多");
        }
        String od = openDay == null || openDay.isBlank() ? now.plusDays(HOLD_GAP_DAYS).toString() : openDay.trim();
        try {
            LocalDate.parse(od);
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "开放日格式应为 yyyy-MM-dd");
        }
        if (od.compareTo(day) <= 0) {
            throw new BusinessException(400, "开放日要在写信日之后，憋一憋更真诚");
        }
        if (letterMapper.find(space.getId(), day, me) != null) {
            throw new BusinessException(400, "今天的换位信已经写过一封了");
        }
        letterMapper.insert(CoupleSwapLetter.of(space.getId(), day, me, c, od));
        String partner = space.partnerOf(me);
        if (letterMapper.find(space.getId(), day, partner) != null) {
            push.pushCoupleEventBoth("swap-letter-pair", me, space.getUserA(), space.getUserB(),
                    "两封换位信都封存了，" + od + " 互相拆 📮");
        } else {
            push.pushCoupleEvent("swap-letter", me, partner, "TA 用你的口吻写了一封信，" + od + " 拆给你");
        }
        return today(me);
    }

    /** 拆 TA 写给我的换位信（到日才可拆；拆自己写的 400）。 */
    public TodayVO openLetter(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSwapLetter row = requireLetter(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "你写的是底稿，要拆的是 TA 给你的那封");
        }
        if (CoupleSwapLetter.STATUS_OPENED.equals(row.getStatus())) {
            return today(me);
        }
        if (!row.isOpenable(LocalDate.now().toString())) {
            throw new BusinessException(400, "还没到开放日（" + row.getOpenDay() + "），再等等");
        }
        row.setStatus(CoupleSwapLetter.STATUS_OPENED);
        row.setOpenedAt(System.currentTimeMillis());
        letterMapper.updateById(row);
        push.pushCoupleEvent("swap-letter-opened", me, row.getFromUser(),
                "TA 拆了你写的换位信 📬 想知道 TA 眼里的自己吗？");
        return today(me);
    }

    // ========== F265 早想说队列 ==========

    /** 封存一句早就想说的话，按队列每 7 天放行一句。 */
    public TodayVO hold(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String c = trim(content, "想说的话要写出来");
        if (c.length() > PROXY_MAX) {
            throw new BusinessException(400, "最多 200 字");
        }
        LocalDate now = LocalDate.now();
        List<CoupleHoldWord> all = holdMapper.findBySpace(space.getId());
        LocalDate tail = null;
        for (CoupleHoldWord w : all) {
            if (CoupleHoldWord.STATUS_HELD.equals(w.getStatus())) {
                LocalDate d = LocalDate.parse(w.getOpenDay());
                if (tail == null || d.isAfter(tail)) {
                    tail = d;
                }
            }
        }
        LocalDate open = tail == null ? now.plusDays(HOLD_GAP_DAYS) : tail.plusDays(HOLD_GAP_DAYS);
        holdMapper.insert(CoupleHoldWord.of(space.getId(), c, me, open.toString()));
        return today(me);
    }

    // ========== F266 三行打卡 ==========

    /** 今日三行（印象/感谢/夸奖），连续 21 天解锁纪念。 */
    public TodayVO three(String me, String morning, String thanks, String praise) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        String m = trim(morning, "");
        String t = trim(thanks, "");
        String p = trim(praise, "");
        if (m.isEmpty() && t.isEmpty() && p.isEmpty()) {
            throw new BusinessException(400, "三行里至少写一行，空白不算打卡");
        }
        for (String s : new String[]{m, t, p}) {
            if (s.length() > THREE_LINE_MAX) {
                throw new BusinessException(400, "每行最多 80 字");
            }
        }
        CoupleThreeLine exist = threeMapper.find(space.getId(), day, me);
        int streakBefore = streak(space.getId(), me, day);
        if (exist != null) {
            exist.setMorning(m);
            exist.setThanks(t);
            exist.setPraise(p);
            exist.setUpdatedAt(System.currentTimeMillis());
            threeMapper.updateById(exist);
            return today(me);
        }
        threeMapper.insert(CoupleThreeLine.of(space.getId(), day, me, m, t, p));
        int streak = streak(space.getId(), me, day);
        if (streakBefore < THREE_STREAK_BADGE && streak >= THREE_STREAK_BADGE) {
            push.pushCoupleEventBoth("three-line-21", me, space.getUserA(), space.getUserB(),
                    CoupleListenBank.THREE_LINE_21);
        }
        return today(me);
    }

    // ========== F267 语气翻译官 ==========

    /** 自报今日语气，对方看到翻译条（当日可改，改后不再重推）。 */
    public TodayVO tone(String me, String toneKey, String note) {
        CoupleSpace space = requireSpace(me);
        String tk = toneKey == null ? "" : toneKey.trim().toUpperCase();
        if (!CoupleListenBank.TONE_LABEL.containsKey(tk)) {
            throw new BusinessException(400, "语气只有四种：累了/忙/低落/其实没事");
        }
        String day = LocalDate.now().toString();
        String n = trim(note, "");
        if (n.length() > 60) {
            throw new BusinessException(400, "补一句最多 60 字");
        }
        CoupleToneNote exist = toneMapper.find(space.getId(), day, me);
        if (exist != null) {
            exist.setTone(tk);
            exist.setNote(n);
            toneMapper.updateById(exist);
            return today(me);
        }
        toneMapper.insert(CoupleToneNote.of(space.getId(), day, me, tk, n));
        push.pushCoupleEvent("tone-marked", me, space.partnerOf(me),
                "TA 给今天报了语气：「" + CoupleListenBank.TONE_LABEL.get(tk) + "」，你那边有翻译条 🈯");
        return today(me);
    }

    // ========== F268 休战旗 ==========

    /** 举旗休战（默认 30 分钟，10-120 分钟），在途仅一面。 */
    public TodayVO truce(String me, Integer minutes) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        settleTruce(space, now);
        if (!truceMapper.findCurrent(space.getId()).isEmpty()) {
            throw new BusinessException(400, "休战旗已经举着了，给彼此一点时间");
        }
        int min = minutes == null ? TRUCE_DEFAULT_MIN : Math.max(TRUCE_MIN_MIN, Math.min(TRUCE_MAX_MIN, minutes));
        truceMapper.insert(CoupleTruce.of(space.getId(), me,
                System.currentTimeMillis() + min * 60_000L));
        push.pushCoupleEventBoth("truce-raised", me, space.getUserA(), space.getUserB(),
                "「" + me + "」举了休战旗 ⏸ 冻结 " + min + " 分钟，到点再决定继续还是算了");
        return today(me);
    }

    /** 到点后双方各表态：继续（各退一步再聊）/ 算了（翻篇）。 */
    public TodayVO decideTruce(String me, boolean goOn) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        List<CoupleTruce> current = truceMapper.findCurrent(space.getId());
        if (current.isEmpty()) {
            throw new BusinessException(400, "现在没有休战旗");
        }
        CoupleTruce row = current.get(0);
        if (!row.isExpired(System.currentTimeMillis())) {
            throw new BusinessException(400, "还没到解冻时刻，再冷静一下");
        }
        boolean userA = space.getUserA().equals(me);
        if (userA ? row.getDecideA() != null : row.getDecideB() != null) {
            return today(me);
        }
        if (userA) {
            row.setDecideA(goOn ? 1 : 0);
        } else {
            row.setDecideB(goOn ? 1 : 0);
        }
        truceMapper.updateById(row);
        if (row.getDecideA() == null || row.getDecideB() == null) {
            push.pushCoupleEvent("truce-decide", me, space.partnerOf(me),
                    "TA 对这次休战表了态，等你一起收场");
            return today(me);
        }
        row.setStatus(CoupleTruce.STATUS_ENDED);
        row.setEndedAt(System.currentTimeMillis());
        truceMapper.updateById(row);
        boolean bothOn = Integer.valueOf(1).equals(row.getDecideA()) && Integer.valueOf(1).equals(row.getDecideB());
        String card = CoupleListenBank.truceEndCard(
                CoupleRitualBank.stableHash(space.getId() + "|truce|" + row.getId()));
        push.pushCoupleEventBoth(bothOn ? "truce-resumed" : "truce-off", me, space.getUserA(), space.getUserB(),
                (bothOn ? "双方决定把这页聊完：" : "这页翻篇了：") + card);
        return today(me);
    }

    // ========== F269 称呼日 ==========

    /** 今日限定称呼，双方各「用过一次」完成当日。 */
    public TodayVO useName(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        CoupleNameDay row = nameMapper.find(space.getId(), day);
        if (row == null) {
            String name = CoupleListenBank.nameOfDay(
                    CoupleRitualBank.stableHash(space.getId() + "|nameday|" + day));
            row = CoupleNameDay.of(space.getId(), day, name);
            nameMapper.insert(row);
        }
        boolean userA = space.getUserA().equals(me);
        int used = userA ? row.getUsedA() : row.getUsedB();
        if (used == 1) {
            return today(me);
        }
        if (userA) {
            row.setUsedA(1);
        } else {
            row.setUsedB(1);
        }
        nameMapper.updateById(row);
        boolean both = Integer.valueOf(1).equals(row.getUsedA()) && Integer.valueOf(1).equals(row.getUsedB());
        if (both) {
            push.pushCoupleEventBoth("nameday-hit", me, space.getUserA(), space.getUserB(),
                    "今日称呼「" + row.getNameText() + "」双方都喊过了，仪式达成 🏷️");
        } else {
            push.pushCoupleEvent("nameday-use", me, space.partnerOf(me),
                    "TA 今天已经用「" + row.getNameText() + "」喊过你了，该你回喊");
        }
        return today(me);
    }

    // ========== 内部 ==========

    /** 惰性结算：早想说到期放行（每天最多一句）+ 休战到期自动待表态。 */
    private void settleDue(CoupleSpace space, LocalDate now) {
        String day = now.toString();
        List<CoupleHoldWord> due = holdMapper.findDue(space.getId(), day);
        if (!due.isEmpty()) {
            CoupleHoldWord head = due.get(0);
            boolean alreadyToday = head.getSentAt() != null
                    && java.time.Instant.ofEpochMilli(head.getSentAt()).atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate().equals(now);
            if (!alreadyToday) {
                head.setStatus(CoupleHoldWord.STATUS_SENT);
                head.setSentAt(System.currentTimeMillis());
                holdMapper.updateById(head);
                push.pushCoupleEvent("hold-sent", head.getFromUser(), space.partnerOf(head.getFromUser()),
                        CoupleListenBank.holdDeliverLine());
            }
        }
        settleTruce(space, now);
    }

    /** 过窗口的休战旗自动收旗（超过解冻时刻 24h 仍未双方表态则 ENDED）。 */
    private void settleTruce(CoupleSpace space, LocalDate now) {
        long ms = System.currentTimeMillis();
        for (CoupleTruce t : truceMapper.findCurrent(space.getId())) {
            if (t.isExpired(ms) && (t.getDecideA() == null || t.getDecideB() == null)
                    && t.getUntilAt() != null && ms - t.getUntilAt() > TRUCE_RESUME_WINDOW_MS) {
                t.setStatus(CoupleTruce.STATUS_ENDED);
                t.setEndedAt(ms);
                truceMapper.updateById(t);
                push.pushCoupleEventBoth("truce-off", t.getRaiser(), space.getUserA(), space.getUserB(),
                        "休战旗过期自动收旗，这页默认翻篇 🍃");
            }
        }
    }

    private TodayVO buildToday(CoupleSpace space, String me, LocalDate now, String day, String week) {
        String partner = space.partnerOf(me);

        List<CoupleListenSlot> current = slotMapper.findCurrent(space.getId());
        SlotVO slot = current.isEmpty() ? null : toSlotVO(current.get(0), me);
        List<SlotVO> recent = new ArrayList<>();
        for (CoupleListenSlot s : slotMapper.findCurrent(space.getId())) {
            recent.add(toSlotVO(s, me));
        }

        CoupleProxyWord draft = proxyMapper.findDraft(space.getId(), me);
        List<ProxyVO> adopted = new ArrayList<>();
        for (CoupleProxyWord p : proxyMapper.findByStatus(space.getId(), CoupleProxyWord.STATUS_ADOPTED)) {
            adopted.add(toProxyVO(p, me));
        }

        Map<String, CoupleMisrewind[]> byTopic = new LinkedHashMap<>();
        for (CoupleMisrewind m : misMapper.findRecent(space.getId(), now.minusDays(MIS_LOOKBACK_DAYS).toString())) {
            CoupleMisrewind[] pair = byTopic.computeIfAbsent(m.getDay() + "|" + m.getTopic(),
                    k -> new CoupleMisrewind[2]);
            if (m.getFromUser().equals(me)) {
                pair[0] = m;
            } else {
                pair[1] = m;
            }
        }
        List<MisVO> mis = new ArrayList<>();
        for (Map.Entry<String, CoupleMisrewind[]> e : byTopic.entrySet()) {
            CoupleMisrewind mineR = e.getValue()[0];
            CoupleMisrewind other = e.getValue()[1];
            if (mineR == null && other == null) {
                continue;
            }
            mis.add(new MisVO(e.getKey().split("\\|")[0], e.getKey().split("\\|", 2)[1],
                    mineR == null ? "" : mineR.getMine(), mineR == null ? "" : mineR.getTheirs(),
                    other == null ? "" : other.getMine(), other == null ? "" : other.getTheirs(),
                    mineR != null && other != null));
        }

        List<StuckVO> stuck = new ArrayList<>();
        for (CoupleStuckQ q : stuckMapper.findByWeek(space.getId(), week)) {
            stuck.add(new StuckVO(q.getId(), q.getQuestion(), q.getAnswer(), q.getFromUser().equals(me),
                    q.isAnswered()));
        }

        List<LetterVO> letters = new ArrayList<>();
        for (CoupleSwapLetter l : letterMapper.findBySpace(space.getId())) {
            boolean own = l.getFromUser().equals(me);
            boolean openable = l.isOpenable(day);
            if (own) {
                if (letters.size() < 6) {
                    letters.add(new LetterVO(l.getId(), l.getDay(), l.getOpenDay(), l.getStatus(), true, false,
                            l.getContent()));
                }
            } else if (letters.size() < 12) {
                String content = CoupleSwapLetter.STATUS_OPENED.equals(l.getStatus()) ? l.getContent()
                        : (openable ? "" : "封存中，" + l.getOpenDay() + " 可见");
                letters.add(new LetterVO(l.getId(), l.getDay(), l.getOpenDay(), l.getStatus(), false, openable, content));
            }
        }

        List<HoldVO> holds = new ArrayList<>();
        String nextHoldDay = null;
        for (CoupleHoldWord w : holdMapper.findBySpace(space.getId())) {
            if (holds.size() < 8) {
                holds.add(new HoldVO(w.getId(), w.getContent(), w.getFromUser(), w.getFromUser().equals(me),
                        w.getStatus(), w.getOpenDay(), w.getSentAt()));
            }
        }
        CoupleHoldWord next = holdMapper.findNextHeld(space.getId(), day);
        if (next != null) {
            nextHoldDay = next.getOpenDay();
        }

        CoupleThreeLine myThree = threeMapper.find(space.getId(), day, me);
        int streakMine = streak(space.getId(), me, day);
        int streakPartner = streak(space.getId(), partner, day);
        ThreeVO three = new ThreeVO(myThree == null ? "" : myThree.getMorning(),
                myThree == null ? "" : myThree.getThanks(), myThree == null ? "" : myThree.getPraise(),
                streakMine, streakPartner, THREE_STREAK_BADGE);

        List<ToneVO> tones = new ArrayList<>();
        for (CoupleToneNote t : toneMapper.findByDay(space.getId(), day)) {
            tones.add(new ToneVO(t.getFromUser(), t.getTone(), CoupleListenBank.TONE_LABEL.get(t.getTone()),
                    t.getFromUser().equals(me) ? "" : CoupleListenBank.TONE_LINE.get(t.getTone()),
                    t.getFromUser().equals(me)));
        }

        List<CoupleTruce> on = truceMapper.findCurrent(space.getId());
        TruceVO truce = null;
        if (!on.isEmpty()) {
            CoupleTruce t = on.get(0);
            truce = new TruceVO(t.getId(), t.getRaiser(), t.getUntilAt(),
                    t.isExpired(System.currentTimeMillis()), t.getRaiser().equals(me),
                    t.getDecideA(), t.getDecideB(), false);
        }

        CoupleNameDay name = nameMapper.find(space.getId(), day);
        boolean userA = space.getUserA().equals(me);
        NameDayVO nameDay = name == null ? null : new NameDayVO(name.getDay(), name.getNameText(),
                Integer.valueOf(1).equals(userA ? name.getUsedA() : name.getUsedB()),
                Integer.valueOf(1).equals(userA ? name.getUsedB() : name.getUsedA()),
                Integer.valueOf(1).equals(name.getUsedA()) && Integer.valueOf(1).equals(name.getUsedB()));

        return new TodayVO(day, week, slot, recent, draft == null ? null : toProxyVO(draft, me), adopted,
                mis, stuck, letters, holds, nextHoldDay, three, tones, truce, nameDay);
    }

    /** 连续天数：从今天（或昨天仍连着）往回数。 */
    private int streak(String spaceId, String user, String day) {
        List<CoupleThreeLine> rows = threeMapper.findByUser(spaceId, user);
        if (rows.isEmpty()) {
            return 0;
        }
        java.util.Set<String> days = new java.util.HashSet<>();
        for (CoupleThreeLine r : rows) {
            days.add(r.getDay());
        }
        LocalDate cursor = LocalDate.parse(day);
        if (!days.contains(cursor.toString())) {
            cursor = cursor.minusDays(1);
            if (!days.contains(cursor.toString())) {
                return 0;
            }
        }
        int n = 0;
        while (days.contains(cursor.minusDays(n).toString())) {
            n++;
            if (n > 3650) {
                break;
            }
        }
        return n;
    }

    private SlotVO toSlotVO(CoupleListenSlot s, String me) {
        return new SlotVO(s.getId(), s.getDay(), s.getTopic(), s.getStatus(), s.getFromUser().equals(me),
                CoupleListenSlot.STATUS_CONFIRMED.equals(s.getStatus()) || CoupleListenSlot.STATUS_DONE.equals(s.getStatus()),
                s.getRateMine(), s.getRatePartner(), s.getNote());
    }

    private ProxyVO toProxyVO(CoupleProxyWord p, String me) {
        return new ProxyVO(p.getId(), p.getContent(), p.getFromUser(), p.getFromUser().equals(me),
                p.getStatus(), p.getFinalText(), p.getAdoptedBy());
    }

    private int clampScore(Integer score) {
        if (score == null) {
            throw new BusinessException(400, "打个 1-5 分");
        }
        return Math.max(1, Math.min(5, score));
    }

    private CoupleListenSlot requireSlot(CoupleSpace space, String id) {
        CoupleListenSlot row = id == null ? null : slotMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这个时段不存在");
        }
        return row;
    }

    private CoupleProxyWord requireProxy(CoupleSpace space, String id) {
        CoupleProxyWord row = id == null ? null : proxyMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这句代笔不存在");
        }
        return row;
    }

    private CoupleSwapLetter requireLetter(CoupleSpace space, String id) {
        CoupleSwapLetter row = id == null ? null : letterMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这封信不存在");
        }
        return row;
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
