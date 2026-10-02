package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 回音壁（F350-F359，批次三十一）：好事簿、能量补给、鼓励语罐、被爱日历、感谢慢递、
 * 高光重放、夸夸回执、电量预报、写给低落的自己、回音壁年报。
 * 情绪价值设计：把「TA 爱我」的证据一条条存下来，低落的时候取出来充电——
 * 好事簿是证据库，补给是充电口，慢递是延迟到达的谢谢，电量预报让对方知道今晚该怎么对你。
 */
@Service
public class CoupleEchoService {

    static final int DEED_PAGE = 30;
    static final int REFILL_DEEDS = 3;
    static final int RECENT_ARRIVED = 10;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleEchoDeedMapper deedMapper;
    private final CoupleEchoJuiceMapper juiceMapper;
    private final CoupleEchoRefillLogMapper refillLogMapper;
    private final CoupleEchoSlowMapper slowMapper;
    private final CoupleEchoHighlightMapper highlightMapper;
    private final CoupleEchoReceiptMapper receiptMapper;
    private final CoupleEchoBatteryMapper batteryMapper;
    private final CoupleEchoSelfLetterMapper selfLetterMapper;
    private final CouplePraiseMapper praiseMapper;
    private final ImPushService push;

    public CoupleEchoService(CoupleSpaceMapper spaceMapper, CoupleEchoDeedMapper deedMapper,
                             CoupleEchoJuiceMapper juiceMapper, CoupleEchoRefillLogMapper refillLogMapper,
                             CoupleEchoSlowMapper slowMapper, CoupleEchoHighlightMapper highlightMapper,
                             CoupleEchoReceiptMapper receiptMapper, CoupleEchoBatteryMapper batteryMapper,
                             CoupleEchoSelfLetterMapper selfLetterMapper, CouplePraiseMapper praiseMapper,
                             ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.deedMapper = deedMapper;
        this.juiceMapper = juiceMapper;
        this.refillLogMapper = refillLogMapper;
        this.slowMapper = slowMapper;
        this.highlightMapper = highlightMapper;
        this.receiptMapper = receiptMapper;
        this.batteryMapper = batteryMapper;
        this.selfLetterMapper = selfLetterMapper;
        this.praiseMapper = praiseMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** F350 好事簿条目（mine=我是记录人；partnerDeeds 里 mine=false 表示 TA 记录的「我为 TA 做的事」）。 */
    public record DeedVO(String id, String fromUser, boolean mine, String content, String day,
                         boolean starred, long created) {
    }

    /** F352 鼓励语条目（idx=罐子槽位 1-5）。 */
    public record JuiceVO(String id, String fromUser, boolean mine, int idx, String content, long created) {
    }

    /** F354 慢递条目（openDay=送达日；delivered=0 表示还在路上）。 */
    public record SlowVO(String id, String fromUser, boolean mine, String toUser, String content,
                         String openDay, boolean delivered, long created) {
    }

    /** F355 三行高光卡。 */
    public record HighlightVO(String id, String fromUser, boolean mine, String moment, String did,
                              String feel, long created) {
    }

    /** F356 夸夸回执（quoteContent/quoteFrom 来自 couple_praise 跨模块只读，条目消失时给空串）。 */
    public record ReceiptVO(String id, String quoteId, String quoteFrom, String quoteContent, long created) {
    }

    /** F357 电量格（hint=对方 ≤2 格时的「今晚轻轻的」提示行，只挂对方那格）。 */
    public record BatteryVO(String fromUser, boolean mine, int level, String want, String hint) {
    }

    /** F358 给自己的信（vault 里只出现在途 SEALED 那封；/self/read 返回刚开读的 READ 那封）。 */
    public record SelfLetterVO(String id, String content, String status, long created) {
    }

    /** F351 能量补给：vault 里是今日态（列表为空）；POST /refill 返回拆开的补给包。 */
    public record RefillVO(boolean mineToday, boolean partnerToday, List<DeedVO> deeds, List<JuiceVO> juices,
                           List<HighlightVO> highlights, String selfLetter, String line) {
    }

    /** F353 被爱日历的一天（deeds=当天双方记录数，starred=当天被加星数，refilled=当天有人领过补给）。 */
    public record CalendarDayVO(String day, int deeds, int starred, boolean refilled) {
    }

    /** F359 年报聚合（数字全部来自真实表）。 */
    public record YearlyVO(int year, int deeds, int starred, int refills, int slowArrived, int receipts,
                           String summary) {
    }

    /** 回音壁总览：写接口全部原样返回这份聚合，前端整体替换。 */
    public record EchoVO(String day, List<DeedVO> deeds, List<DeedVO> partnerDeeds, List<JuiceVO> juices,
                         RefillVO refill, List<SlowVO> slowInFlight, List<SlowVO> slowArrived,
                         List<HighlightVO> highlights, List<ReceiptVO> receipts, List<BatteryVO> battery,
                         SelfLetterVO selfLetter, YearlyVO yearly) {
    }

    // ========== 读 ==========

    /** 回音壁总览（含慢递到日惰性结算）。 */
    public EchoVO vault(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        settleSlow(space, now);
        return build(space, me, now, null, null);
    }

    // ========== F350 好事簿 ==========

    /** F350 记一件「TA 为我做的事」（同日同人同内容重复 400；新增推双方）。 */
    public EchoVO addDeed(String me, String content, String day) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String text = trim(content, "好事总得写一句");
        if (text.length() > CoupleEchoDeed.CONTENT_MAX) {
            throw new BusinessException(400, "一件好事最多 " + CoupleEchoDeed.CONTENT_MAX + " 字");
        }
        String d = day == null || day.isBlank() ? now.toString() : day.trim();
        if (!d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BusinessException(400, "日期写成 yyyy-MM-dd");
        }
        if (deedMapper.findByDayContent(space.getId(), me, d, text) != null) {
            throw new BusinessException(400, "这条已经记过了");
        }
        deedMapper.insert(CoupleEchoDeed.of(space.getId(), me, text, d));
        push.pushCoupleEventBoth("echo-deed-added", me, space.getUserA(), space.getUserB(),
                CoupleEchoBank.deedAddedLine(text));
        return build(space, me, now, null, null);
    }

    /** F350 记录人本人给证据点「这条救过我」（幂等；TA 的记录只能 TA 自己点）。 */
    public EchoVO starDeed(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleEchoDeed row = id == null || id.isBlank() ? null : deedMapper.selectById(id.trim());
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(400, "这条不在好事簿里");
        }
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "只有记下这条的人能加星");
        }
        if (row.starredFlag()) {
            return build(space, me, now, null, null);
        }
        row.setStarred(1);
        row.setUpdatedAt(System.currentTimeMillis());
        deedMapper.updateById(row);
        push.pushCoupleEventBoth("echo-deed-starred", me, space.getUserA(), space.getUserB(),
                CoupleEchoBank.deedStarredLine(row.getContent()));
        return build(space, me, now, null, null);
    }

    // ========== F352 鼓励语罐 ==========

    /** F352 往自己罐里塞一张鼓励语（≤5 条，第 6 条 400；槽位复用删掉的空格）。 */
    public EchoVO addJuice(String me, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String text = trim(content, "鼓励语总得写一句");
        if (text.length() > CoupleEchoJuice.CONTENT_MAX) {
            throw new BusinessException(400, "一张纸条最多 " + CoupleEchoJuice.CONTENT_MAX + " 字");
        }
        List<CoupleEchoJuice> jar = new ArrayList<>(juiceMapper.findByUser(space.getId(), me));
        if (jar.size() >= CoupleEchoJuice.CAP) {
            throw new BusinessException(400, "罐子装不下了");
        }
        jar.sort(Comparator.comparingInt(j -> j.getIdx() == null ? 0 : j.getIdx()));
        int slot = 1;
        for (CoupleEchoJuice j : jar) {
            if (j.getIdx() != null && j.getIdx() == slot) {
                slot++;
            }
        }
        juiceMapper.insert(CoupleEchoJuice.of(space.getId(), me, slot, text));
        return build(space, me, now, null, null);
    }

    /** F352 删掉自己罐里的一张（只能删本人的）。 */
    public EchoVO removeJuice(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleEchoJuice row = id == null || id.isBlank() ? null : juiceMapper.selectById(id.trim());
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(400, "这张纸条不在罐子里");
        }
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "只能清自己罐子里的纸条");
        }
        juiceMapper.deleteById(row.getId());
        return build(space, me, now, null, null);
    }

    // ========== F351 能量补给 ==========

    /** F351 领今天的能量补给（每人每天一次；顺带开读自己的在途信；推双方）。 */
    public EchoVO refill(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        if (refillLogMapper.findByDayUser(space.getId(), me, now.toString()) != null) {
            throw new BusinessException(400, "今天已经充过电了");
        }
        refillLogMapper.insert(CoupleEchoRefillLog.of(space.getId(), me, now.toString()));
        RefillVO pack = composeRefill(space, me, now);
        push.pushCoupleEventBoth("echo-refilled", me, space.getUserA(), space.getUserB(),
                CoupleEchoBank.refillPushLine(me));
        return build(space, me, now, pack, null);
    }

    /** 拆补给包：我的证据随机 ≤3 条 + 双方 juice/highlight 各 1 条 + 在途信顺带开读。 */
    private RefillVO composeRefill(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        String partner = space.partnerOf(me);
        long seed = CoupleRitualBank.stableHash(space.getId() + "|refill|" + day);
        List<CoupleEchoDeed> myDeeds = new ArrayList<>(deedMapper.findByUser(space.getId(), me));
        Collections.shuffle(myDeeds, new Random(seed));
        List<DeedVO> deeds = myDeeds.stream().limit(REFILL_DEEDS).map(d -> toDeed(d, me)).toList();

        List<JuiceVO> juices = new ArrayList<>();
        for (String u : List.of(me, partner)) {
            CoupleEchoJuice pick = pickByHash(juiceMapper.findByUser(space.getId(), u),
                    space.getId() + "|juice|" + day + "|" + u);
            if (pick != null) {
                juices.add(toJuice(pick, me));
            }
        }
        List<HighlightVO> highlights = new ArrayList<>();
        for (String u : List.of(me, partner)) {
            CoupleEchoHighlight pick = pickByHash(highlightMapper.findByUser(space.getId(), u),
                    space.getId() + "|hl|" + day + "|" + u);
            if (pick != null) {
                highlights.add(toHighlight(pick, me));
            }
        }
        String selfLetter = "";
        CoupleEchoSelfLetter sealed = selfLetterMapper.findSealedByUser(space.getId(), me);
        if (sealed != null) {
            sealed.setStatus(CoupleEchoSelfLetter.STATUS_READ);
            sealed.setUpdatedAt(System.currentTimeMillis());
            selfLetterMapper.updateById(sealed);
            selfLetter = sealed.getContent();
        }
        return new RefillVO(true, refillLogMapper.findByDayUser(space.getId(), partner, day) != null,
                deeds, juices, highlights, selfLetter, CoupleEchoBank.refillLine(seed));
    }

    // ========== F354 感谢慢递 ==========

    /** F354 寄一封感谢慢递（≤100 字，在途每人 ≤3 封；7 天后送达）。 */
    public EchoVO writeSlow(String me, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String text = trim(content, "想谢的话总要写一句");
        if (text.length() > CoupleEchoSlow.CONTENT_MAX) {
            throw new BusinessException(400, "慢递最多 " + CoupleEchoSlow.CONTENT_MAX + " 字");
        }
        long inFlight = slowMapper.findByUser(space.getId(), me).stream()
                .filter(s -> !s.deliveredFlag()).count();
        if (inFlight >= CoupleEchoSlow.IN_FLIGHT_MAX) {
            throw new BusinessException(400, "路上还有 " + CoupleEchoSlow.IN_FLIGHT_MAX + " 封");
        }
        slowMapper.insert(CoupleEchoSlow.of(space.getId(), me, space.partnerOf(me), text,
                now.plusDays(CoupleEchoSlow.DELIVER_AFTER_DAYS).toString()));
        return build(space, me, now, null, null);
    }

    /** F354 读时惰性结算：open_day<=今天且未送达的置 1 并推双方（不建定时任务）。 */
    private void settleSlow(CoupleSpace space, LocalDate now) {
        for (CoupleEchoSlow row : slowMapper.findDue(space.getId(), now.toString())) {
            row.setDelivered(1);
            row.setUpdatedAt(System.currentTimeMillis());
            slowMapper.updateById(row);
            push.pushCoupleEventBoth("echo-thanks-arrived", row.getFromUser(),
                    space.getUserA(), space.getUserB(),
                    CoupleEchoBank.thanksArrivedLine(row.getFromUser(), row.getContent()));
        }
    }

    // ========== F355 高光重放 ==========

    /** F355 收藏一条三行高光（moment/did/feel 各有上限，每人 ≤12 条）。 */
    public EchoVO addHighlight(String me, String moment, String did, String feel) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String m = trim(moment, "高光发生在什么时候");
        if (m.length() > CoupleEchoHighlight.MOMENT_MAX) {
            throw new BusinessException(400, "「什么时候」最多 " + CoupleEchoHighlight.MOMENT_MAX + " 字");
        }
        String d = trim(did, "TA 做了什么");
        if (d.length() > CoupleEchoHighlight.DID_MAX) {
            throw new BusinessException(400, "「做了什么」最多 " + CoupleEchoHighlight.DID_MAX + " 字");
        }
        String f = trim(feel, "当时什么感觉");
        if (f.length() > CoupleEchoHighlight.FEEL_MAX) {
            throw new BusinessException(400, "「什么感觉」最多 " + CoupleEchoHighlight.FEEL_MAX + " 字");
        }
        if (highlightMapper.findByUser(space.getId(), me).size() >= CoupleEchoHighlight.CAP) {
            throw new BusinessException(400, "精选夹满了");
        }
        highlightMapper.insert(CoupleEchoHighlight.of(space.getId(), me, m, d, f));
        return build(space, me, now, null, null);
    }

    /** F355 删掉自己精选夹里的一条。 */
    public EchoVO removeHighlight(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleEchoHighlight row = id == null || id.isBlank() ? null : highlightMapper.selectById(id.trim());
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(400, "这条不在精选夹里");
        }
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "只能整理自己的精选夹");
        }
        highlightMapper.deleteById(row.getId());
        return build(space, me, now, null, null);
    }

    // ========== F356 夸夸回执 ==========

    /** F356 给夸夸墙某句点「收到」（uk 幂等，重复点不重复记；非本空间 400；推给夸的人）。 */
    public EchoVO receipt(String me, String quoteId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CouplePraise praise = quoteId == null || quoteId.isBlank() ? null
                : praiseMapper.selectById(quoteId.trim());
        if (praise == null || !space.getId().equals(praise.getSpaceId())) {
            throw new BusinessException(400, "这句话不在你们的夸夸墙上");
        }
        if (receiptMapper.findByQuoteUser(space.getId(), praise.getId(), me) == null) {
            receiptMapper.insert(CoupleEchoReceipt.of(space.getId(), praise.getId(), me));
            push.pushCoupleEvent("echo-receipt-given", me, praise.getFromUser(),
                    CoupleEchoBank.receiptLine(praise.getContent()));
        }
        return build(space, me, now, null, null);
    }

    // ========== F357 电量预报 ==========

    /** F357 报今天的电量（1-5 钳制，null 按 3 格；本人当天可改写）。 */
    public EchoVO battery(String me, Integer level, String want) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        int v = level == null ? CoupleEchoBattery.LEVEL_DEFAULT
                : Math.max(CoupleEchoBattery.LEVEL_MIN, Math.min(CoupleEchoBattery.LEVEL_MAX, level));
        String w = want == null ? "" : want.trim();
        if (w.length() > CoupleEchoBattery.WANT_MAX) {
            throw new BusinessException(400, "「想被怎样对待」最多 " + CoupleEchoBattery.WANT_MAX + " 字");
        }
        CoupleEchoBattery row = batteryMapper.findByDayUser(space.getId(), now.toString(), me);
        if (row == null) {
            batteryMapper.insert(CoupleEchoBattery.of(space.getId(), now.toString(), me, v, w));
        } else {
            row.setLevel(v);
            row.setWant(w);
            row.setUpdatedAt(System.currentTimeMillis());
            batteryMapper.updateById(row);
        }
        return build(space, me, now, null, null);
    }

    // ========== F358 写给低落的自己 ==========

    /** F358 写一封给低落的自己（≤300 字；已有在途信 400「还有一封在等你」）。 */
    public EchoVO writeSelf(String me, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String text = trim(content, "哪怕一句也行，写给低落的自己");
        if (text.length() > CoupleEchoSelfLetter.CONTENT_MAX) {
            throw new BusinessException(400, "这封信最多 " + CoupleEchoSelfLetter.CONTENT_MAX + " 字");
        }
        if (selfLetterMapper.findSealedByUser(space.getId(), me) != null) {
            throw new BusinessException(400, "还有一封在等你");
        }
        selfLetterMapper.insert(CoupleEchoSelfLetter.of(space.getId(), me, text));
        return build(space, me, now, null, null);
    }

    /** F358 本人开读在途信置 READ（写给自己的信，不推送给对方）。 */
    public EchoVO readSelf(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleEchoSelfLetter row = selfLetterMapper.findSealedByUser(space.getId(), me);
        if (row == null) {
            throw new BusinessException(400, "现在没有在途的信");
        }
        row.setStatus(CoupleEchoSelfLetter.STATUS_READ);
        row.setUpdatedAt(System.currentTimeMillis());
        selfLetterMapper.updateById(row);
        return build(space, me, now, null, toSelf(row));
    }

    // ========== F353 被爱日历 ==========

    /** F353 被爱日历：按年聚合双方 deed/star 与领取日志，只返回有动静的日子。 */
    public List<CalendarDayVO> calendar(String me, String year) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        settleSlow(space, now);
        String y = normalizeYear(year, now);
        Map<String, int[]> deeds = new TreeMap<>();
        for (CoupleEchoDeed d : deedMapper.findBySpace(space.getId())) {
            if (d.getDay() != null && d.getDay().startsWith(y)) {
                int[] c = deeds.computeIfAbsent(d.getDay(), k -> new int[2]);
                c[0]++;
                if (d.starredFlag()) {
                    c[1]++;
                }
            }
        }
        TreeSet<String> refilled = new TreeSet<>();
        for (CoupleEchoRefillLog r : refillLogMapper.findBySpace(space.getId())) {
            if (r.getDay() != null && r.getDay().startsWith(y)) {
                refilled.add(r.getDay());
            }
        }
        TreeSet<String> days = new TreeSet<>();
        days.addAll(deeds.keySet());
        days.addAll(refilled);
        List<CalendarDayVO> out = new ArrayList<>();
        for (String day : days) {
            int[] c = deeds.getOrDefault(day, new int[2]);
            out.add(new CalendarDayVO(day, c[0], c[1], refilled.contains(day)));
        }
        return out;
    }

    // ========== F359 年报 ==========

    /** F359 回音壁年报：deed/star/领取/慢递送达/回执五项计数 + Bank 文案 summary。 */
    public YearlyVO yearReport(String me, String year) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        settleSlow(space, now);
        return yearly(space, normalizeYear(year, now));
    }

    private YearlyVO yearly(CoupleSpace space, String y) {
        int deeds = 0;
        int starred = 0;
        for (CoupleEchoDeed d : deedMapper.findBySpace(space.getId())) {
            if (d.getDay() != null && d.getDay().startsWith(y)) {
                deeds++;
                if (d.starredFlag()) {
                    starred++;
                }
            }
        }
        int refills = (int) refillLogMapper.findBySpace(space.getId()).stream()
                .filter(r -> r.getDay() != null && r.getDay().startsWith(y)).count();
        int arrived = (int) slowMapper.findBySpace(space.getId()).stream()
                .filter(s -> s.deliveredFlag() && s.getOpenDay() != null && s.getOpenDay().startsWith(y)).count();
        int receipts = (int) receiptMapper.findBySpace(space.getId()).stream()
                .filter(r -> r.getCreated() != null && yearOf(r.getCreated()) == Integer.parseInt(y)).count();
        long seed = CoupleRitualBank.stableHash(space.getId() + "|echo-year|" + y);
        return new YearlyVO(Integer.parseInt(y), deeds, starred, refills, arrived, receipts,
                CoupleEchoBank.yearSummary(y, deeds, starred, refills, arrived, receipts, seed));
    }

    // ========== 聚合 ==========

    private EchoVO build(CoupleSpace space, String me, LocalDate now, RefillVO refillOverride,
                         SelfLetterVO selfOverride) {
        String day = now.toString();
        String partner = space.partnerOf(me);

        List<DeedVO> deeds = deedMapper.findByUser(space.getId(), me).stream()
                .limit(DEED_PAGE).map(d -> toDeed(d, me)).toList();
        List<DeedVO> partnerDeeds = deedMapper.findByUser(space.getId(), partner).stream()
                .limit(DEED_PAGE).map(d -> toDeed(d, me)).toList();
        List<JuiceVO> juices = juiceMapper.findBySpace(space.getId()).stream()
                .map(j -> toJuice(j, me)).toList();
        RefillVO refill = refillOverride != null ? refillOverride : new RefillVO(
                refillLogMapper.findByDayUser(space.getId(), me, day) != null,
                refillLogMapper.findByDayUser(space.getId(), partner, day) != null,
                List.of(), List.of(), List.of(), "", "");

        List<SlowVO> inFlight = new ArrayList<>();
        List<SlowVO> arrived = new ArrayList<>();
        for (CoupleEchoSlow s : slowMapper.findBySpace(space.getId())) {
            if (s.deliveredFlag()) {
                arrived.add(toSlow(s, me));
            } else {
                inFlight.add(toSlow(s, me));
            }
        }
        inFlight.sort(Comparator.comparingLong(SlowVO::created));
        arrived.sort(Comparator.comparingLong(SlowVO::created).reversed());
        if (arrived.size() > RECENT_ARRIVED) {
            arrived = new ArrayList<>(arrived.subList(0, RECENT_ARRIVED));
        }

        List<HighlightVO> highlights = highlightMapper.findBySpace(space.getId()).stream()
                .map(h -> toHighlight(h, me)).toList();
        List<ReceiptVO> receipts = receiptMapper.findByUser(space.getId(), me).stream()
                .map(this::toReceipt).toList();
        List<BatteryVO> battery = batteryMapper.findByDay(space.getId(), day).stream()
                .map(b -> toBattery(b, me, space, day)).toList();

        SelfLetterVO selfLetter = selfOverride;
        if (selfLetter == null) {
            CoupleEchoSelfLetter sealed = selfLetterMapper.findSealedByUser(space.getId(), me);
            selfLetter = sealed == null ? null : toSelf(sealed);
        }
        return new EchoVO(day, deeds, partnerDeeds, juices, refill, inFlight, arrived, highlights,
                receipts, battery, selfLetter, yearly(space, String.valueOf(now.getYear())));
    }

    // ========== 小件 ==========

    private DeedVO toDeed(CoupleEchoDeed d, String me) {
        return new DeedVO(d.getId(), d.getFromUser(), d.getFromUser().equals(me),
                nz(d.getContent()), nz(d.getDay()), d.starredFlag(), d.getCreated() == null ? 0 : d.getCreated());
    }

    private JuiceVO toJuice(CoupleEchoJuice j, String me) {
        return new JuiceVO(j.getId(), j.getFromUser(), j.getFromUser().equals(me),
                j.getIdx() == null ? 0 : j.getIdx(), nz(j.getContent()),
                j.getCreated() == null ? 0 : j.getCreated());
    }

    private SlowVO toSlow(CoupleEchoSlow s, String me) {
        return new SlowVO(s.getId(), s.getFromUser(), s.getFromUser().equals(me), nz(s.getToUser()),
                nz(s.getContent()), nz(s.getOpenDay()), s.deliveredFlag(),
                s.getCreated() == null ? 0 : s.getCreated());
    }

    private HighlightVO toHighlight(CoupleEchoHighlight h, String me) {
        return new HighlightVO(h.getId(), h.getFromUser(), h.getFromUser().equals(me), nz(h.getMoment()),
                nz(h.getDid()), nz(h.getFeel()), h.getCreated() == null ? 0 : h.getCreated());
    }

    private ReceiptVO toReceipt(CoupleEchoReceipt r) {
        CouplePraise praise = praiseMapper.selectById(r.getQuoteId());
        return new ReceiptVO(r.getId(), nz(r.getQuoteId()),
                praise == null ? "" : nz(praise.getFromUser()),
                praise == null ? "" : nz(praise.getContent()),
                r.getCreated() == null ? 0 : r.getCreated());
    }

    private BatteryVO toBattery(CoupleEchoBattery b, String me, CoupleSpace space, String day) {
        boolean mine = b.getFromUser().equals(me);
        String hint = "";
        if (!mine && b.getLevel() != null && b.getLevel() <= CoupleEchoBattery.LOW_LEVEL) {
            hint = CoupleEchoBank.lowBatteryLine(CoupleRitualBank.stableHash(space.getId() + "|battery|" + day));
        }
        return new BatteryVO(b.getFromUser(), mine, b.getLevel() == null ? 0 : b.getLevel(),
                nz(b.getWant()), hint);
    }

    /**
     * F358 未拆读（SEALED）时不下发正文：规格写的是「只在本人点开补给时可读」，
     * 聚合接口提前把信抄给前端就等于没锁（前端不渲染也照样能在网络面板里看到）。
     */
    private SelfLetterVO toSelf(CoupleEchoSelfLetter s) {
        boolean sealed = CoupleEchoSelfLetter.STATUS_SEALED.equals(s.getStatus());
        return new SelfLetterVO(s.getId(), sealed ? "" : nz(s.getContent()), nz(s.getStatus()),
                s.getCreated() == null ? 0 : s.getCreated());
    }

    private <T> T pickByHash(List<T> rows, String seedKey) {
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(Math.floorMod(CoupleRitualBank.stableHash(seedKey), rows.size()));
    }

    private int yearOf(Long ts) {
        return ts == null ? 0 : LocalDate.ofInstant(java.time.Instant.ofEpochMilli(ts),
                java.time.ZoneId.systemDefault()).getYear();
    }

    private String normalizeYear(String year, LocalDate now) {
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份写成 yyyy");
        }
        return y;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
