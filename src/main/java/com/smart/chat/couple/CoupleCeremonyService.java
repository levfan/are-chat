package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 小日子·仪式感（F230-F239，批次十九）：建国纪念日、节日老黄历、过法任务卡、庆祝打卡、
 * 爱情保险柜、续约仪式、愿望券本、小日子史册、年度加冕、当日体感。
 * 情绪价值设计：把「我们的日子」从日历里挑出来郑重对待——写过法、打勾、交保费、签字续约，
 * 每一个小动作都在说：这一天因为有你才值得庆祝。
 */
@Service
public class CoupleCeremonyService {

    static final int NAME_MAX = 60;
    static final int TEXT_MAX = 140;
    static final int COUPON_TITLE_MAX = 80;
    /** 积分口径：发一张愿望券花 10 分，是裁剪后积分唯一的花分出口。 */
    static final int COUPON_COST = 10;
    static final String COUPON_SPEND_PREFIX = "发出愿望券：";
    static final int RITUAL_MAX = 3;
    static final int RENEW_EVERY_DAYS = 100;
    static final int[] POLICY_MILESTONES = {3, 6, 12};
    static final int ALMANAC_MAX = 15;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCeremonyFoundedMapper foundedMapper;
    private final CoupleCeremonyRitualMapper ritualMapper;
    private final CoupleCeremonyMarkMapper markMapper;
    private final CoupleCeremonyPolicyMapper policyMapper;
    private final CoupleCeremonyRenewMapper renewMapper;
    private final CoupleCeremonyCouponMapper couponMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final CoupleCountdownMapper countdownMapper;
    private final ImPushService push;

    public CoupleCeremonyService(CoupleSpaceMapper spaceMapper, CoupleCeremonyFoundedMapper foundedMapper,
                                 CoupleCeremonyRitualMapper ritualMapper, CoupleCeremonyMarkMapper markMapper,
                                 CoupleCeremonyPolicyMapper policyMapper, CoupleCeremonyRenewMapper renewMapper,
                                CoupleCeremonyCouponMapper couponMapper, CouplePointLedgerMapper ledgerMapper,
                                 CoupleAnniversaryMapper anniversaryMapper, CoupleCountdownMapper countdownMapper,
                                 ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.foundedMapper = foundedMapper;
        this.ritualMapper = ritualMapper;
        this.markMapper = markMapper;
        this.policyMapper = policyMapper;
        this.renewMapper = renewMapper;
        this.couponMapper = couponMapper;
        this.ledgerMapper = ledgerMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.countdownMapper = countdownMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record RitualVO(String id, String foundedId, String content, boolean markedToday) {
    }

    public record FoundedVO(String id, String name, String startDay, boolean repeatYear,
                            String nextDay, Long daysLeft, Integer edition, List<RitualVO> rituals) {
    }

    public record PolicyVO(String month, String mine, String partner, int paidMonths,
                           List<Integer> paidMilestones, Integer monthsToNext) {
    }

    public record RenewLineVO(String anchorDay, String fromUser, boolean mine, String line) {
    }

    public record RenewVO(String anchorDay, boolean dueToday, boolean mineSigned, boolean partnerSigned,
                          long daysToNext, List<RenewLineVO> scroll) {
    }

    public record CouponVO(String id, String title, String status, String ref, String issuer,
                           String usedBy, Long created) {
    }

    public record CrownItemVO(String name, int marks) {
    }

    public record OverviewVO(String day, List<FoundedVO> founded,
                             List<String> nudges, PolicyVO policy, RenewVO renew,
                             List<CouponVO> couponsOpen, List<CouponVO> couponsUsed) {
    }

    // ========== 读：仪式总览 ==========

    /** 今日仪式总览（小日子/黄历/催办/保险柜/续约/券本/体感/加冕一次拉齐）。 */
    public OverviewVO overview(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        LocalDate today = LocalDate.parse(day);
        List<CoupleCeremonyRitual> allRituals = ritualMapper.findBySpace(space.getId());
        List<CoupleCeremonyMark> allMarks = markMapper.findBySpace(space.getId());
        List<FoundedVO> founded = foundedList(space, allRituals, allMarks, today);
        return new OverviewVO(day, founded, nudges(space, founded, allRituals, allMarks, today),
                policyState(space, me, day), renewState(space, me, today),
                couponsOf(space, CoupleCeremonyCoupon.STATUS_OPEN), couponsOf(space, CoupleCeremonyCoupon.STATUS_USED));
    }

    // ========== F230 建国纪念日 ==========

    /** 自定义「我们的小日子」：起名+定起始日+是否每年重复。 */
    public OverviewVO addFounded(String me, String name, String startDay, Boolean repeatYear) {
        CoupleSpace space = requireSpace(me);
        String title = requireText(name, NAME_MAX, "给小日子起个名字吧");
        String start = normDay(startDay);
        foundedMapper.insert(CoupleCeremonyFounded.of(space.getId(), title, start,
                repeatYear == null || repeatYear));
        push.pushCoupleEvent("ceremony-founded", me, space.partnerOf(me), "TA 新建了一个小日子「" + title + "」");
        return overview(me);
    }

    /** 删除小日子（连同过法卡与打卡）。 */
    public OverviewVO removeFounded(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleCeremonyFounded founded = requireFounded(space, id);
        List<String> ritualIds = ritualMapper.findByFounded(space.getId(), founded.getId()).stream()
                .map(CoupleCeremonyRitual::getId).toList();
        markMapper.deleteByRitualIds(ritualIds);
        ritualIds.forEach(ritualMapper::deleteById);
        foundedMapper.deleteById(founded.getId());
        return overview(me);
    }

    // ========== F232 过法任务卡 ==========

    /** 给小日子写一条庆祝方式（每个小日子最多 3 条）。 */
    public OverviewVO addRitual(String me, String foundedId, String content) {
        CoupleSpace space = requireSpace(me);
        CoupleCeremonyFounded founded = requireFounded(space, foundedId);
        String text = requireText(content, TEXT_MAX, "庆祝方式写点具体的动作吧");
        if (ritualMapper.findByFounded(space.getId(), founded.getId()).size() >= RITUAL_MAX) {
            throw new BusinessException(400, "每个小日子最多 " + RITUAL_MAX + " 条过法，贪多嚼不烂");
        }
        ritualMapper.insert(CoupleCeremonyRitual.of(space.getId(), founded.getId(), text));
        push.pushCoupleEvent("ceremony-ritual", me, space.partnerOf(me), "「" + founded.getName() + "」多了一条过法：" + text);
        return overview(me);
    }

    /** 划掉一条过法卡（连同它的打卡记录）。 */
    public OverviewVO removeRitual(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleCeremonyRitual ritual = requireRitual(space, id);
        markMapper.deleteByRitualIds(List.of(ritual.getId()));
        ritualMapper.deleteById(ritual.getId());
        return overview(me);
    }

    // ========== F233 庆祝打卡 ==========

    /** 给过法卡打勾（当日幂等）；这个小日子当条过法全部打满推双方默契。 */
    public OverviewVO mark(String me, String ritualId) {
        CoupleSpace space = requireSpace(me);
        CoupleCeremonyRitual ritual = requireRitual(space, ritualId);
        String day = LocalDate.now().toString();
        if (markMapper.find(ritual.getId(), day) == null) {
            markMapper.insert(CoupleCeremonyMark.of(space.getId(), ritual.getId(), day));
            CoupleCeremonyFounded founded = foundedMapper.selectById(ritual.getFoundedId());
            List<CoupleCeremonyRitual> siblings = ritualMapper.findByFounded(space.getId(), ritual.getFoundedId());
            long done = siblings.stream().filter(r -> markMapper.find(r.getId(), day) != null).count();
            if (founded != null && done == siblings.size()) {
                push.pushCoupleEventBoth("ceremony-all-done", me, space.getUserA(), space.getUserB(),
                        "「" + founded.getName() + "」的过法全部打勾了✓");
            } else if (founded != null) {
                push.pushCoupleEvent("ceremony-mark", me, space.partnerOf(me),
                        "TA 勾掉了「" + founded.getName() + "」的一条过法：" + ritual.getContent());
            }
        }
        return overview(me);
    }

    // ========== F234 爱情保险柜 ==========

    /** 交本月保费：夸 TA 一句（一人一月一句，可改写）；双方交齐=当月生效，满 3/6/12 月自动 payout 愿望券。 */
    public OverviewVO payPolicy(String me, String quote) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(quote, TEXT_MAX, "保费是夸 TA 的一句话，别空着");
        String month = YearMonth.now().toString();
        CoupleCeremonyPolicy existing = policyMapper.find(space.getId(), month, me);
        if (existing == null) {
            policyMapper.insert(CoupleCeremonyPolicy.of(space.getId(), month, me, text));
        } else {
            existing.setQuote(text);
            policyMapper.updateById(existing);
        }
        payoutCheck(space, me);
        return overview(me);
    }

    /** 满 3/6/12 个交齐月且未 payout 过 → 发愿望券（幂等按 ref）。 */
    private void payoutCheck(CoupleSpace space, String me) {
        int paid = paidMonths(space).size();
        for (int milestone : POLICY_MILESTONES) {
            String ref = "policy-" + milestone;
            if (paid >= milestone && !couponMapper.existsRef(space.getId(), ref)) {
                String title = CoupleCeremonyBank.payoutTitle(milestone);
                couponMapper.insert(CoupleCeremonyCoupon.of(space.getId(), title, me, ref));
                push.pushCoupleEventBoth("ceremony-payout", me, space.getUserA(), space.getUserB(),
                        "保险柜满 " + milestone + " 个月，payout 一张愿望券！");
            }
        }
    }

    // ========== F235 续约仪式 ==========

    /** 续约日（每满 100 天/周年）签一句「我还是选你」；双方都签推默契。 */
    public OverviewVO renew(String me, String line) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(line, TEXT_MAX, "续约的话写一句真心话");
        LocalDate today = LocalDate.now();
        if (!renewDue(space, today)) {
            throw new BusinessException(400, "今天还不是续约日，下次续约在 "
                    + ChronoUnit.DAYS.between(today, nextRenewAnchor(space, today)) + " 天后");
        }
        String anchor = today.toString();
        String partner = space.partnerOf(me);
        boolean partnerSigned = renewMapper.find(space.getId(), anchor, partner) != null;
        CoupleCeremonyRenew existing = renewMapper.find(space.getId(), anchor, me);
        if (existing == null) {
            renewMapper.insert(CoupleCeremonyRenew.of(space.getId(), anchor, me, text));
            if (partnerSigned) {
                push.pushCoupleEventBoth("ceremony-renew", me, space.getUserA(), space.getUserB(),
                        "续约日：你们又互相选了一次✓");
            } else {
                push.pushCoupleEvent("ceremony-renew-sign", me, partner, "TA 在续约长卷上签了一句「我还是选你」");
            }
        } else {
            existing.setLine(text);
            renewMapper.updateById(existing);
        }
        return overview(me);
    }

    // ========== F236 愿望券本 ==========

    /** 发一张愿望券（券面自拟）。 */
    public OverviewVO issueCoupon(String me, String title) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(title, COUPON_TITLE_MAX, "券面写点什么愿望吧");
        // 系统裁剪后积分唯一的花分出口：发券必须先有余额，否则「愿望」就成了空头支票
        int balance = balance(space, me);
        if (balance < COUPON_COST) {
            throw new BusinessException(400, "发一张愿望券要 " + COUPON_COST + " 分，你只有 " + balance
                    + " 分——先去好事簿记一笔 TA 为你做过的事吧");
        }
        couponMapper.insert(CoupleCeremonyCoupon.of(space.getId(), text, me, ""));
        ledgerMapper.insert(CouplePointLedger.of(space.getId(), me, CouplePointLedger.TYPE_SPEND,
                COUPON_SPEND_PREFIX + text, COUPON_COST));
        push.pushCoupleEvent("ceremony-coupon", me, space.partnerOf(me), "TA 给你发了一张愿望券：" + text);
        return overview(me);
    }

    /** 本人积分余额 = 累计 EARN − 累计 SPEND。 */
    private int balance(CoupleSpace space, String me) {
        return ledgerMapper.findBySpace(space.getId()).stream()
                .filter(l -> me.equals(l.getFromUser()))
                .mapToInt(l -> CouplePointLedger.TYPE_EARN.equals(l.getType())
                        ? (l.getPoints() == null ? 0 : l.getPoints())
                        : -(l.getPoints() == null ? 0 : l.getPoints()))
                .sum();
    }

    /** 核销一张愿望券（OPEN→USED，一人一次说了算）。 */
    public OverviewVO useCoupon(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleCeremonyCoupon coupon = couponMapper.selectById(id);
        if (coupon == null || !coupon.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这张愿望券不存在");
        }
        if (!coupon.isOpen()) {
            throw new BusinessException(400, "这张券已经核销过了");
        }
        coupon.setStatus(CoupleCeremonyCoupon.STATUS_USED);
        coupon.setUsedBy(me);
        coupon.setUsedAt(System.currentTimeMillis());
        couponMapper.updateById(coupon);
        push.pushCoupleEvent("ceremony-coupon-used", me, space.partnerOf(me), "愿望券被兑现了：" + coupon.getTitle());
        return overview(me);
    }

    // ========== F239 当日体感 ==========

    // ========== F237 小日子史册 ==========

    // ========== 聚合装配 ==========

    private List<FoundedVO> foundedList(CoupleSpace space, List<CoupleCeremonyRitual> allRituals,
                                        List<CoupleCeremonyMark> allMarks, LocalDate today) {
        String todayStr = today.toString();
        Set<String> markedToday = new HashSet<>();
        for (CoupleCeremonyMark mark : allMarks) {
            if (todayStr.equals(mark.getDay())) {
                markedToday.add(mark.getRitualId());
            }
        }
        List<FoundedVO> list = new ArrayList<>();
        for (CoupleCeremonyFounded founded : foundedMapper.findBySpace(space.getId())) {
            LocalDate next = nextOccurrence(founded, today);
            List<RitualVO> rituals = allRituals.stream()
                    .filter(r -> r.getFoundedId().equals(founded.getId()))
                    .map(r -> new RitualVO(r.getId(), r.getFoundedId(), r.getContent(), markedToday.contains(r.getId())))
                    .toList();
            list.add(new FoundedVO(founded.getId(), founded.getName(), founded.getStartDay(), founded.repeats(),
                    next == null ? null : next.toString(),
                    next == null ? null : ChronoUnit.DAYS.between(today, next),
                    next == null ? null : next.getYear() - LocalDate.parse(founded.getStartDay()).getYear() + 1,
                    rituals));
        }
        list.sort(Comparator.comparing(v -> v.daysLeft() == null ? Long.MAX_VALUE : v.daysLeft()));
        return list;
    }

    /** 小日子下一次发生日（一次性且已过为空；每年重复滚到今天或明年）。 */
    private LocalDate nextOccurrence(CoupleCeremonyFounded founded, LocalDate today) {
        LocalDate start = LocalDate.parse(founded.getStartDay());
        if (!founded.repeats()) {
            return start.isBefore(today) ? null : start;
        }
        LocalDate candidate = start.withYear(today.getYear());
        return candidate.isBefore(today) ? candidate.plusYears(1) : candidate;
    }

    /** F233 补催：最近 3 天内到过却没过齐的小日子，各催一句。 */
    private List<String> nudges(CoupleSpace space, List<FoundedVO> founded, List<CoupleCeremonyRitual> allRituals,
                                List<CoupleCeremonyMark> allMarks, LocalDate today) {
        List<String> list = new ArrayList<>();
        Set<String> markedKeys = new HashSet<>();
        for (CoupleCeremonyMark mark : allMarks) {
            markedKeys.add(mark.getRitualId() + "|" + mark.getDay());
        }
        for (FoundedVO vo : founded) {
            List<CoupleCeremonyRitual> rituals = allRituals.stream()
                    .filter(r -> r.getFoundedId().equals(vo.id())).toList();
            if (rituals.isEmpty()) {
                continue;
            }
            for (int back = 1; back <= 3; back++) {
                LocalDate past = today.minusDays(back);
                if (!isOccurrence(vo, past)) {
                    continue;
                }
                boolean allDone = rituals.stream().allMatch(r -> markedKeys.contains(r.getId() + "|" + past));
                if (!allDone) {
                    list.add(CoupleCeremonyBank.nudge(space.getId(), vo.name()));
                }
                break;
            }
        }
        return list;
    }

    private boolean isOccurrence(FoundedVO vo, LocalDate date) {
        LocalDate start = LocalDate.parse(vo.startDay());
        if (vo.repeatYear()) {
            return date.getMonthValue() == start.getMonthValue() && date.getDayOfMonth() == start.getDayOfMonth();
        }
        return start.isEqual(date);
    }

    private PolicyVO policyState(CoupleSpace space, String me, String day) {
        String month = YearMonth.parse(day.substring(0, 7)).toString();
        String partner = space.partnerOf(me);
        CoupleCeremonyPolicy mineRow = policyMapper.find(space.getId(), month, me);
        CoupleCeremonyPolicy partnerRow = policyMapper.find(space.getId(), month, partner);
        Set<String> paid = paidMonths(space);
        List<Integer> paidMilestones = new ArrayList<>();
        Integer monthsToNext = null;
        for (int milestone : POLICY_MILESTONES) {
            if (couponMapper.existsRef(space.getId(), "policy-" + milestone)) {
                paidMilestones.add(milestone);
            } else if (monthsToNext == null) {
                monthsToNext = Math.max(0, milestone - paid.size());
            }
        }
        return new PolicyVO(month, mineRow == null ? null : mineRow.getQuote(),
                partnerRow == null ? null : partnerRow.getQuote(), paid.size(), paidMilestones, monthsToNext);
    }

    /** 双方都交过句子的自然月数。 */
    private Set<String> paidMonths(CoupleSpace space) {
        Map<String, Set<String>> byMonth = new HashMap<>();
        for (CoupleCeremonyPolicy row : policyMapper.findBySpace(space.getId())) {
            byMonth.computeIfAbsent(row.getMonth(), k -> new HashSet<>()).add(row.getFromUser());
        }
        Set<String> paid = new HashSet<>();
        byMonth.forEach((month, users) -> {
            if (users.contains(space.getUserA()) && users.contains(space.getUserB())) {
                paid.add(month);
            }
        });
        return paid;
    }

    private RenewVO renewState(CoupleSpace space, String me, LocalDate today) {
        boolean due = renewDue(space, today);
        LocalDate anchor = due ? today : nextRenewAnchor(space, today);
        String anchorDay = anchor.toString();
        String partner = space.partnerOf(me);
        List<RenewLineVO> scroll = new ArrayList<>();
        for (CoupleCeremonyRenew row : renewMapper.findBySpace(space.getId())) {
            scroll.add(new RenewLineVO(row.getAnchorDay(), row.getFromUser(),
                    row.getFromUser().equals(me), row.getLine()));
        }
        scroll.sort(Comparator.comparing(RenewLineVO::anchorDay).reversed());
        return new RenewVO(anchorDay, due,
                renewMapper.find(space.getId(), anchorDay, me) != null,
                renewMapper.find(space.getId(), anchorDay, partner) != null,
                ChronoUnit.DAYS.between(today, anchor), scroll);
    }

    /** 续约日判定：距在一起锚点满 100 天整数倍，或恰逢周年当天。 */
    private boolean renewDue(CoupleSpace space, LocalDate today) {
        LocalDate base = anchorBase(space);
        long days = ChronoUnit.DAYS.between(base, today);
        if (days <= 0) {
            return false;
        }
        return days % RENEW_EVERY_DAYS == 0
                || (today.getMonthValue() == base.getMonthValue() && today.getDayOfMonth() == base.getDayOfMonth());
    }

    /** 下一个续约日（100 天档与周年档取先到）。 */
    private LocalDate nextRenewAnchor(CoupleSpace space, LocalDate today) {
        LocalDate base = anchorBase(space);
        long days = ChronoUnit.DAYS.between(base, today);
        LocalDate byHundred = base.plusDays((Math.floorDiv(Math.max(days, 0), RENEW_EVERY_DAYS) + 1) * RENEW_EVERY_DAYS);
        LocalDate anniv = base.withYear(today.getYear());
        if (anniv.isBefore(today) || anniv.isEqual(today)) {
            anniv = anniv.plusYears(1);
        }
        return anniv.isBefore(byHundred) ? anniv : byHundred;
    }

    private LocalDate anchorBase(CoupleSpace space) {
        String anniv = space.getAnniversary();
        if (anniv != null && !anniv.isBlank()) {
            try {
                return LocalDate.parse(anniv);
            } catch (DateTimeParseException e) {
                // 坏数据回退按空间创建日
            }
        }
        long ms = space.getCreated() == null ? System.currentTimeMillis() : space.getCreated();
        return java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private List<CouponVO> couponsOf(CoupleSpace space, String status) {
        List<CouponVO> list = new ArrayList<>();
        for (CoupleCeremonyCoupon coupon : couponMapper.findBySpace(space.getId())) {
            if (status.equals(coupon.getStatus())) {
                list.add(new CouponVO(coupon.getId(), coupon.getTitle(), coupon.getStatus(), coupon.getRef(),
                        coupon.getIssuer(), coupon.getUsedBy(), coupon.getCreated()));
            }
        }
        list.sort(Comparator.comparing(CouponVO::created, Comparator.reverseOrder()));
        return list;
    }

    

    // ========== 通用 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private CoupleCeremonyFounded requireFounded(CoupleSpace space, String id) {
        CoupleCeremonyFounded founded = foundedMapper.selectById(id);
        if (founded == null || !founded.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这个小日子不存在");
        }
        return founded;
    }

    private CoupleCeremonyRitual requireRitual(CoupleSpace space, String id) {
        CoupleCeremonyRitual ritual = ritualMapper.selectById(id);
        if (ritual == null || !ritual.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这张过法卡不存在");
        }
        return ritual;
    }

    private String requireText(String value, int max, String emptyMsg) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, emptyMsg);
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BusinessException(400, "最多 " + max + " 个字，心意不在字数");
        }
        return trimmed;
    }

    private String normDay(String day) {
        try {
            return LocalDate.parse(day).toString();
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "日期格式应为 yyyy-MM-dd");
        }
    }
}
