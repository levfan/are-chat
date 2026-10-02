package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 两家与朋友（F330-F339，批次二十九）：拜访攻略、送礼互助池、朋友视角问卷、官宣日、
 * 文案代写、进城接待方案、亲戚称呼册、社会信用、群聊记者、代 TA 赔礼。
 * 情绪价值设计：见家长、送礼、朋友怎么看我们，是最容易一个人慌的事——
 * 这里把「慌」拆成可以提前准备的任务卡，把「丢人」变成有人一起兜底的流程。
 */
@Service
public class CoupleWorldService {

    static final int PREP_MAX = 60;
    static final int PREP_LIMIT = 8;
    static final int REPORT_MAX = 200;
    static final int GIFT_PERSON_MAX = 30;
    static final int GIFT_IDEA_MAX = 80;
    static final int GIFT_FIELD_MAX = 60;
    static final int VIEW_ANSWER_MAX = 200;
    static final int DECLARE_MAX = 200;
    static final int CAPTION_MAX = 140;
    static final int CITY_MAX = 30;
    static final int ITINERARY_ITEM_MAX = 60;
    static final int ITINERARY_LIMIT = 8;
    static final int PACK_TOTAL_MAX = 296;
    static final int TERM_MAX = 20;
    static final int QNA_MAX = 140;
    static final int VOW_MAX = 140;
    static final int BREAK_NOTE_MAX = 80;
    static final int GROUP_MAX = 200;
    static final int APOLOGY_TO_MAX = 30;
    static final int APOLOGY_REASON_MAX = 200;
    static final int APOLOGY_DRAFT_MAX = 300;
    static final int REVIEW_NOTE_MAX = 140;
    static final int LIST_VISIT_MAX = 30;
    static final int LIST_GIFT_MAX = 40;
    static final int LIST_DECLARE_MAX = 24;
    static final int LIST_CAPTION_MAX = 30;
    static final int LIST_APOLOGY_MAX = 20;
    static final int LIST_VOW_MAX = 30;
    static final int VOW_KEPT_POINTS = 10;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleWorldVisitMapper visitMapper;
    private final CoupleWorldGiftMapper giftMapper;
    private final CoupleWorldFriendViewMapper viewMapper;
    private final CoupleWorldDeclareMapper declareMapper;
    private final CoupleWorldCaptionMapper captionMapper;
    private final CoupleWorldCityPlanMapper cityMapper;
    private final CoupleWorldRelativesQMapper relativeMapper;
    private final CoupleWorldVowMapper vowMapper;
    private final CoupleWorldGroupReportMapper groupMapper;
    private final CoupleWorldApologyMapper apologyMapper;
    private final CoupleCodexEntryMapper codexMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final ImPushService push;

    public CoupleWorldService(CoupleSpaceMapper spaceMapper, CoupleWorldVisitMapper visitMapper,
                              CoupleWorldGiftMapper giftMapper, CoupleWorldFriendViewMapper viewMapper,
                              CoupleWorldDeclareMapper declareMapper, CoupleWorldCaptionMapper captionMapper,
                              CoupleWorldCityPlanMapper cityMapper, CoupleWorldRelativesQMapper relativeMapper,
                              CoupleWorldVowMapper vowMapper, CoupleWorldGroupReportMapper groupMapper,
                              CoupleWorldApologyMapper apologyMapper, CoupleCodexEntryMapper codexMapper,
                              CouplePointLedgerMapper ledgerMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.visitMapper = visitMapper;
        this.giftMapper = giftMapper;
        this.viewMapper = viewMapper;
        this.declareMapper = declareMapper;
        this.captionMapper = captionMapper;
        this.cityMapper = cityMapper;
        this.relativeMapper = relativeMapper;
        this.vowMapper = vowMapper;
        this.groupMapper = groupMapper;
        this.apologyMapper = apologyMapper;
        this.codexMapper = codexMapper;
        this.ledgerMapper = ledgerMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record PrepVO(int seq, String text, String kind) {
    }

    public record VisitVO(String id, String day, boolean mine, String hostSide, String hostLabel,
                          List<PrepVO> preps, boolean confirmed, String report, String status,
                          boolean canConfirm) {
    }

    public record GiftVO(String id, String person, String idea, String budget, String avoid,
                         boolean mine, String takerUser, String status, boolean canTake) {
    }

    public record ViewVO(int slot, String question, String askedTo, String answer, String byUser, boolean filled) {
    }

    public record DeclareVO(String month, String text, boolean mine) {
    }

    public record CaptionVO(String id, String day, boolean mine, int slot, String text, boolean won) {
    }

    public record CityVO(String id, String city, String arriveDay, boolean mine, List<String> itinerary,
                         String transport, List<String> packList, long daysLeft) {
    }

    public record RelativeVO(String id, String term, String question, String answer, boolean mine,
                             int wrongCount, String lastWrongDay, boolean canTry) {
    }

    public record VowVO(String id, String content, boolean mine, String dueDay, boolean witnessed,
                        String status, String brokenNote, long daysLeft, boolean canWitness, boolean canBreak) {
    }

    public record GroupVO(String day, String myLine, String partnerLine, boolean iLaughed,
                          boolean partnerLaughed, boolean bothLaughed, String line, boolean canWrite,
                          boolean canLaugh) {
    }

    public record ApologyVO(String id, String toPerson, boolean mine, String reason, String draft,
                            String status, String reviewNote, String template) {
    }

    public record WorldVO(String day, String month, List<VisitVO> visits, List<GiftVO> gifts,
                          List<ViewVO> views, String friendViewLine, List<DeclareVO> declares,
                          List<CaptionVO> captions, List<CityVO> cities, List<RelativeVO> relatives,
                          List<String> packTemplate, List<VowVO> vows, GroupVO group, List<ApologyVO> apologies) {
    }

    // ========== 读：世界总览（含惰性结算：保证到期解除） ==========

    /** 两家与朋友总览。 */
    public WorldVO world(String me) {
        CoupleSpace space = requireSpace(me);
        settle(space, LocalDate.now());
        return build(space, me, LocalDate.now());
    }

    // ========== F330 拜访攻略 ==========

    /** 写一次拜访攻略（一天一次，前置任务卡 ≤8 条）。 */
    public WorldVO visitPlan(String me, String day, String hostSide, String preps) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String d = day == null || day.isBlank() ? now.toString() : validDay(day, "拜访日");
        String side = hostSide == null || hostSide.isBlank()
                ? CoupleWorldVisit.SIDE_MINE : hostSide.trim().toUpperCase();
        if (!CoupleWorldVisit.SIDE_MINE.equals(side) && !CoupleWorldVisit.SIDE_YOURS.equals(side)) {
            throw new BusinessException(400, "攻略只分「我家」和「你家」");
        }
        List<String> items = splitItems(preps, PREP_LIMIT, PREP_MAX);
        if (visitMapper.findByDay(space.getId(), d).stream().anyMatch(v -> v.getFromUser().equals(me))) {
            throw new BusinessException(400, "这天的攻略你已经写过了");
        }
        visitMapper.insert(CoupleWorldVisit.of(space.getId(), d, side, me, String.join(",", items)));
        push.pushCoupleEvent("world-visit", me, space.partnerOf(me),
                "TA 写好了 " + d + " 的拜访攻略（" + visitLabel(side) + "），要你确认 🧧");
        return build(space, me, now);
    }

    /** 确认攻略（只能对方确认）。 */
    public WorldVO visitConfirm(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldVisit row = requireVisit(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己写的攻略自己确认不算双确认");
        }
        if (row.confirmedFlag()) {
            return build(space, me, now);
        }
        row.setConfirmed(1);
        row.setUpdatedAt(System.currentTimeMillis());
        visitMapper.updateById(row);
        push.pushCoupleEventBoth("world-visit-confirmed", me, space.getUserA(), space.getUserB(),
                "拜访攻略双确认通过，雷区都提前标好了 🧧");
        return build(space, me, now);
    }

    /** 回访后写战报（写攻略的人负责交）。 */
    public WorldVO visitReport(String me, String id, String report) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldVisit row = requireVisit(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "战报由写攻略的人交，TA 负责补细节");
        }
        String r = trim(report, "战报要写一句");
        if (r.length() > REPORT_MAX) {
            throw new BusinessException(400, "战报最多 " + REPORT_MAX + " 字");
        }
        row.setReport(r);
        row.setStatus(CoupleWorldVisit.STATUS_DONE);
        row.setUpdatedAt(System.currentTimeMillis());
        visitMapper.updateById(row);
        push.pushCoupleEventBoth("world-visit-report", me, space.getUserA(), space.getUserB(),
                CoupleWorldBank.visitReportLine(CoupleRitualBank.stableHash(space.getId() + "|visit|" + row.getId())));
        return build(space, me, now);
    }

    // ========== F331 送礼互助池 ==========

    /** 收一条送礼灵感（含雷点）。 */
    public WorldVO giftAdd(String me, String person, String idea, String budget, String avoid) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String p = capped(person, GIFT_PERSON_MAX, "送谁");
        if (p.isEmpty()) {
            throw new BusinessException(400, "送谁要写");
        }
        String i = trim(idea, "灵感要写一句");
        if (i.length() > GIFT_IDEA_MAX) {
            throw new BusinessException(400, "灵感最多 " + GIFT_IDEA_MAX + " 字");
        }
        if (giftMapper.findByIdea(space.getId(), i) != null) {
            throw new BusinessException(400, "这条灵感已经在池子里了");
        }
        String b = capped(budget, GIFT_FIELD_MAX, "预算");
        String a = capped(avoid, GIFT_FIELD_MAX, "雷点");
        giftMapper.insert(CoupleWorldGift.of(space.getId(), p, i, b, a, me));
        push.pushCoupleEvent("world-gift", me, space.partnerOf(me),
                "送礼池上新：给「" + p + "」——" + i + (a.isEmpty() ? "" : "（雷点：" + a + "）") + " 🎁");
        return build(space, me, now);
    }

    /** 接单代买（只能接对方的单）。 */
    public WorldVO giftTake(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldGift row = requireGift(space, id);
        if (row.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "自己登的灵感不用自己接单，等 TA 帮你买");
        }
        if (!CoupleWorldGift.STATUS_OPEN.equals(row.getStatus())) {
            throw new BusinessException(400, "这单已经有人接了");
        }
        row.setTakerUser(me);
        row.setStatus(CoupleWorldGift.STATUS_TAKEN);
        row.setUpdatedAt(System.currentTimeMillis());
        giftMapper.updateById(row);
        push.pushCoupleEvent("world-gift-take", me, row.getOwnerUser(),
                "「" + row.getIdea() + "」被 TA 接单代买了 🎁");
        return build(space, me, now);
    }

    /** 代买完成。 */
    public WorldVO giftBought(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldGift row = requireGift(space, id);
        if (row.getTakerUser().isEmpty() || !row.getTakerUser().equals(me)) {
            throw new BusinessException(400, "只有接单的人能宣布买好了");
        }
        if (CoupleWorldGift.STATUS_BOUGHT.equals(row.getStatus())) {
            return build(space, me, now);
        }
        row.setStatus(CoupleWorldGift.STATUS_BOUGHT);
        row.setUpdatedAt(System.currentTimeMillis());
        giftMapper.updateById(row);
        push.pushCoupleEventBoth("world-gift-bought", me, space.getUserA(), space.getUserB(),
                "礼买好了：给「" + row.getPerson() + "」的「" + row.getIdea() + "」🎁");
        return build(space, me, now);
    }

    // ========== F332 朋友视角问卷 ==========

    /** 回填一题他观（线下问过朋友后）。 */
    public WorldVO friendViewFill(String me, Integer slot, String askedTo, String answer) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        int s = slot == null ? 1 : slot;
        if (s < 1 || s > CoupleWorldFriendView.SLOT_MAX) {
            throw new BusinessException(400, "他观问卷只有 1-3 题");
        }
        CoupleWorldFriendView row = ensureView(space, s);
        String a = trim(answer, "朋友的原话要写一句");
        if (a.length() > VIEW_ANSWER_MAX) {
            throw new BusinessException(400, "原话最多 " + VIEW_ANSWER_MAX + " 字");
        }
        String to = capped(askedTo, GIFT_PERSON_MAX, "问了谁");
        row.setAskedTo(to);
        row.setAnswer(a);
        row.setByUser(me);
        row.setUpdatedAt(System.currentTimeMillis());
        viewMapper.updateById(row);
        push.pushCoupleEvent("world-view", me, space.partnerOf(me),
                "他观第 " + s + " 题回填了：" + a + " 🪞");
        return build(space, me, now);
    }

    // ========== F333 官宣日 ==========

    /** 发本月官宣卡（每月一张，纯文字）。 */
    public WorldVO declare(String me, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String month = now.toString().substring(0, 7);
        String t = trim(text, "官宣要写一句");
        if (t.length() > DECLARE_MAX) {
            throw new BusinessException(400, "官宣文案最多 " + DECLARE_MAX + " 字");
        }
        if (declareMapper.findByMonth(space.getId(), month) != null) {
            throw new BusinessException(400, month + " 的官宣卡已经发过了，一个月一张");
        }
        declareMapper.insert(CoupleWorldDeclare.of(space.getId(), month, t, me));
        push.pushCoupleEventBoth("world-declare", me, space.getUserA(), space.getUserB(),
                "本月官宣卡：" + t + " —— " + CoupleWorldBank.declareLine(
                        CoupleRitualBank.stableHash(space.getId() + "|declare|" + month)));
        return build(space, me, now);
    }

    // ========== F334 文案代写 ==========

    /** 交一条候选文案（每人每天最多三候选）。 */
    public WorldVO captionSubmit(String me, Integer slot, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        int s = slot == null ? 1 : slot;
        if (s < 1 || s > CoupleWorldCaption.SLOT_MAX) {
            throw new BusinessException(400, "一天最多交三条候选");
        }
        String t = trim(text, "候选文案要写一句");
        if (t.length() > CAPTION_MAX) {
            throw new BusinessException(400, "候选最多 " + CAPTION_MAX + " 字");
        }
        List<CoupleWorldCaption> mineRows = captionMapper.findByDay(space.getId(), day).stream()
                .filter(c -> c.getFromUser().equals(me)).toList();
        CoupleWorldCaption exist = mineRows.stream()
                .filter(c -> c.getSlot() == s).findFirst().orElse(null);
        if (exist != null) {
            exist.setText(t);
            captionMapper.updateById(exist);
            return build(space, me, now);
        }
        captionMapper.insert(CoupleWorldCaption.of(space.getId(), day, me, s, t));
        push.pushCoupleEvent("world-caption", me, space.partnerOf(me),
                "TA 交了今天第 " + s + " 条候选文案，三稿齐了就能选 📝");
        return build(space, me, now);
    }

    /** 选稿（只有求稿的对方能定稿，一人一天一稿）。 */
    public WorldVO captionPick(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldCaption row = requireCaption(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己写的稿自己定不算互评选稿");
        }
        for (CoupleWorldCaption c : captionMapper.findByDay(space.getId(), row.getDay())) {
            c.setWon(c.getId().equals(row.getId()) ? 1 : 0);
            captionMapper.updateById(c);
        }
        // F334 规格要求「定稿进百科」：选中的文案存成百科词条，日后在百科书架里能翻到
        String term = "定稿文案 · " + row.getDay();
        if (codexMapper.findTerm(space.getId(), term) == null) {
            codexMapper.insert(CoupleCodexEntry.of(space.getId(), term, row.getText(),
                    "TA 替我写的文案，选定于 " + row.getDay(), "发朋友圈用", row.getFromUser()));
        }
        push.pushCoupleEventBoth("world-caption-pick", me, space.getUserA(), space.getUserB(),
                "定稿：「" + row.getText() + "」—— " + CoupleWorldBank.captionPickedLine(
                        CoupleRitualBank.stableHash(space.getId() + "|caption|" + row.getDay())));
        return build(space, me, now);
    }

    // ========== F335 进城接待方案 ==========

    /** 存/改一座城市的接待手册（行程 ≤8 条）。 */
    public WorldVO citySave(String me, String city, String arriveDay, String itinerary,
                            String transport, String packList) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String c = trim(city, "哪座城市");
        if (c.length() > CITY_MAX) {
            throw new BusinessException(400, "城市名最多 " + CITY_MAX + " 字");
        }
        String ad = arriveDay == null || arriveDay.isBlank() ? "" : validDay(arriveDay, "到访日");
        List<String> items = splitItems(itinerary, ITINERARY_LIMIT, ITINERARY_ITEM_MAX);
        String tr = capped(transport, QNA_MAX, "交通");
        List<String> packs = splitOptional(packList, 12, ITINERARY_ITEM_MAX, PACK_TOTAL_MAX);
        CoupleWorldCityPlan row = cityMapper.findByCity(space.getId(), c);
        boolean fresh = row == null;
        if (fresh) {
            row = CoupleWorldCityPlan.of(space.getId(), c, ad, me);
        }
        row.setArriveDay(ad);
        row.setItinerary(String.join(",", items));
        row.setTransport(tr);
        row.setPackList(String.join(",", packs));
        row.setUpdatedAt(System.currentTimeMillis());
        if (fresh) {
            cityMapper.insert(row);
        } else {
            cityMapper.updateById(row);
        }
        push.pushCoupleEvent("world-city", me, space.partnerOf(me),
                (fresh ? "新建了" : "更新了") + c + " 的接待手册，行程和小包清单都排好了 🧳");
        return build(space, me, now);
    }

    // ========== F336 亲戚称呼册 ==========

    /** 出一条称谓考题。 */
    public WorldVO relativeAdd(String me, String term, String question, String answer) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String t = trim(term, "称谓本体要写");
        if (t.length() > TERM_MAX) {
            throw new BusinessException(400, "称谓最多 " + TERM_MAX + " 字");
        }
        if (relativeMapper.findByTerm(space.getId(), t) != null) {
            throw new BusinessException(400, "「" + t + "」已经在册上了");
        }
        String q = trim(question, "怎么问要写一句");
        if (q.length() > QNA_MAX) {
            throw new BusinessException(400, "题面最多 " + QNA_MAX + " 字");
        }
        String a = trim(answer, "标准答案要写");
        if (a.length() > GIFT_IDEA_MAX) {
            throw new BusinessException(400, "答案最多 " + GIFT_IDEA_MAX + " 字");
        }
        relativeMapper.insert(CoupleWorldRelativesQ.of(space.getId(), t, q, a, me));
        push.pushCoupleEvent("world-relative", me, space.partnerOf(me),
                "称呼册上新一题：「" + q + "」，考前务必会 📖");
        return build(space, me, now);
    }

    /** 作答（只有被考的人能答，错题进考前强化）。 */
    public WorldVO relativeTry(String me, String id, String answer) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldRelativesQ row = requireRelative(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己出的题考不了自己，等 TA 来答");
        }
        String a = trim(answer, "答案要写一个称呼");
        boolean right = a.replace(" ", "").equals(row.getAnswer().replace(" ", ""));
        if (!right) {
            row.setWrongCount(row.getWrongCount() + 1);
            row.setLastWrongDay(now.toString());
            row.setUpdatedAt(System.currentTimeMillis());
            relativeMapper.updateById(row);
            push.pushCoupleEvent("world-relative-wrong", me, row.getFromUser(),
                    CoupleWorldBank.relativeWrongLine(row.getTerm()));
        } else {
            push.pushCoupleEvent("world-relative-right", me, row.getFromUser(),
                    "「" + row.getTerm() + "」答对了，这题可以从考前强化里撤下 📖");
        }
        return build(space, me, now);
    }

    // ========== F337 社会信用 ==========

    /** 公开声明一条保证。 */
    public WorldVO vowAdd(String me, String content, String dueDay) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String c = trim(content, "保证不做什么都要写清");
        if (c.length() > VOW_MAX) {
            throw new BusinessException(400, "保证最多 " + VOW_MAX + " 字");
        }
        String d = dueDay == null || dueDay.isBlank() ? now.plusDays(30).toString() : validDay(dueDay, "到期日");
        if (d.compareTo(now.toString()) <= 0) {
            throw new BusinessException(400, "到期日要在以后，当天保证等于没保证");
        }
        if (vowMapper.findByOwner(space.getId(), me).stream().anyMatch(v -> v.getContent().equals(c))) {
            throw new BusinessException(400, "这条保证你已经立过了");
        }
        vowMapper.insert(CoupleWorldVow.of(space.getId(), c, me, d));
        push.pushCoupleEvent("world-vow", me, space.partnerOf(me),
                "TA 公开保证：「" + c + "」，到期 " + d + "，要你见证 🤝");
        return build(space, me, now);
    }

    /** TA 见证。 */
    public WorldVO vowWitness(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldVow row = requireVow(space, id);
        if (row.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "见证人得是对方，自己见证不作数");
        }
        if (row.isWitnessed()) {
            return build(space, me, now);
        }
        row.setWitnessed(1);
        row.setUpdatedAt(System.currentTimeMillis());
        vowMapper.updateById(row);
        push.pushCoupleEvent("world-vow-witness", me, row.getOwnerUser(),
                CoupleWorldBank.vowWitnessLine(row.getContent()));
        return build(space, me, now);
    }

    /** 塌房记录（对方举报，需一句事实）。 */
    public WorldVO vowBreak(String me, String id, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldVow row = requireVow(space, id);
        if (row.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "自己不能给自己记塌房，等 TA 来说");
        }
        if (!CoupleWorldVow.STATUS_OPEN.equals(row.getStatus())) {
            throw new BusinessException(400, "这条保证已经收尾了");
        }
        String n = trim(note, "塌房要写一句事实");
        if (n.length() > BREAK_NOTE_MAX) {
            throw new BusinessException(400, "塌房记录最多 " + BREAK_NOTE_MAX + " 字");
        }
        row.setStatus(CoupleWorldVow.STATUS_BROKEN);
        row.setBrokenNote(n);
        row.setUpdatedAt(System.currentTimeMillis());
        vowMapper.updateById(row);
        push.pushCoupleEventBoth("world-vow-broken", me, space.getUserA(), space.getUserB(),
                CoupleWorldBank.vowBrokenLine(n));
        return build(space, me, now);
    }

    // ========== F338 群聊记者 ==========

    /** 交今天的一条群聊素材（本人可改写）。 */
    public WorldVO groupLine(String me, String line) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        CoupleWorldGroupReport row = ensureGroup(space, day);
        String l = trim(line, "素材要写一句");
        if (l.length() > GROUP_MAX) {
            throw new BusinessException(400, "素材最多 " + GROUP_MAX + " 字");
        }
        boolean isA = me.equals(space.getUserA());
        boolean mineFilled = (isA ? row.getLineA() : row.getLineB()).isEmpty();
        boolean partnerFilled = (isA ? row.getLineB() : row.getLineA()).isEmpty();
        if (isA) {
            row.setLineA(l);
        } else {
            row.setLineB(l);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        groupMapper.updateById(row);
        if (mineFilled) {
            push.pushCoupleEvent("world-group", me, space.partnerOf(me),
                    "今天群里的最好笑素材交上来了，等你那条" + (partnerFilled ? "" : "（TA 已交）") + " 😂");
        }
        return build(space, me, now);
    }

    /** 笑了对方那条（两人都笑 = 今日素材双双通过）。 */
    public WorldVO groupLaugh(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldGroupReport row = ensureGroup(space, now.toString());
        boolean isA = me.equals(space.getUserA());
        if (isA) {
            row.setLaughA(1);
        } else {
            row.setLaughB(1);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        groupMapper.updateById(row);
        if (row.laughAFlag() && row.laughBFlag()) {
            push.pushCoupleEventBoth("world-group-both", me, space.getUserA(), space.getUserB(),
                    CoupleWorldBank.groupLaughLine(
                            CoupleRitualBank.stableHash(space.getId() + "|group|" + row.getDay()), true));
        } else {
            push.pushCoupleEvent("world-group-laugh", me, space.partnerOf(me),
                    CoupleWorldBank.groupLaughLine(
                            CoupleRitualBank.stableHash(space.getId() + "|group|" + row.getDay()), false));
        }
        return build(space, me, now);
    }

    // ========== F339 代 TA 赔礼 ==========

    /** 写一封给 TA 亲友的赔礼信（求审阅）。 */
    public WorldVO apologyWrite(String me, String toPerson, String reason, String draft) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String p = trim(toPerson, "写给谁（称谓）");
        if (p.length() > APOLOGY_TO_MAX) {
            throw new BusinessException(400, "称谓最多 " + APOLOGY_TO_MAX + " 字");
        }
        String r = reason == null ? "" : reason.trim();
        if (r.length() > APOLOGY_REASON_MAX) {
            throw new BusinessException(400, "来龙去脉最多 " + APOLOGY_REASON_MAX + " 字");
        }
        String d = trim(draft, "信正文要写");
        if (d.length() > APOLOGY_DRAFT_MAX) {
            throw new BusinessException(400, "正文最多 " + APOLOGY_DRAFT_MAX + " 字，长了没人听得进去");
        }
        apologyMapper.insert(CoupleWorldApology.of(space.getId(), p, r, d, me));
        push.pushCoupleEvent("world-apology", me, space.partnerOf(me),
                "TA 写了给「" + p + "」的赔礼信，要你审阅才送得出去 ✉️");
        return build(space, me, now);
    }

    /** TA 审阅（通过=送达，打回=重写）。 */
    public WorldVO apologyReview(String me, String id, Boolean pass, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldApology row = requireApology(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己审自己的信不算送达");
        }
        if (!CoupleWorldApology.STATUS_OPEN.equals(row.getStatus())) {
            throw new BusinessException(400, "这封已经审过了");
        }
        boolean p = Boolean.TRUE.equals(pass);
        String n = note == null ? "" : note.trim();
        if (n.length() > REVIEW_NOTE_MAX) {
            throw new BusinessException(400, "审阅意见最多 " + REVIEW_NOTE_MAX + " 字");
        }
        if (!p && n.isEmpty()) {
            throw new BusinessException(400, "打回要写一句改哪儿");
        }
        row.setStatus(p ? CoupleWorldApology.STATUS_SENT : CoupleWorldApology.STATUS_BACK);
        row.setReviewNote(n);
        row.setReviewedBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        apologyMapper.updateById(row);
        if (p) {
            push.pushCoupleEventBoth("world-apology-sent", me, space.getUserA(), space.getUserB(),
                    "信审阅通过，替 TA 送到「" + row.getToPerson() + "」手里了 ✉️");
        } else {
            push.pushCoupleEvent("world-apology-back", me, row.getFromUser(), "信被打回：" + n);
        }
        return build(space, me, now);
    }

    /** 被打回后重写（本人）。 */
    public WorldVO apologyRewrite(String me, String id, String draft) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleWorldApology row = requireApology(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "信主本人才能改");
        }
        if (!CoupleWorldApology.STATUS_BACK.equals(row.getStatus())) {
            throw new BusinessException(400, "只有被打回的信能重写");
        }
        String d = trim(draft, "重写要写正文");
        if (d.length() > APOLOGY_DRAFT_MAX) {
            throw new BusinessException(400, "正文最多 " + APOLOGY_DRAFT_MAX + " 字");
        }
        row.setDraft(d);
        row.setStatus(CoupleWorldApology.STATUS_OPEN);
        row.setReviewNote("");
        row.setReviewedBy("");
        row.setUpdatedAt(System.currentTimeMillis());
        apologyMapper.updateById(row);
        push.pushCoupleEvent("world-apology", me, space.partnerOf(me),
                "TA 重写了给「" + row.getToPerson() + "」的赔礼信，再审一次 ✉️");
        return build(space, me, now);
    }

    // ========== 惰性结算 ==========

    private void settle(CoupleSpace space, LocalDate now) {
        String today = now.toString();
        for (CoupleWorldVow v : vowMapper.findDue(space.getId(), today)) {
            if (v.isWitnessed()) {
                v.setStatus(CoupleWorldVow.STATUS_KEPT);
                v.setUpdatedAt(System.currentTimeMillis());
                vowMapper.updateById(v);
                // 说到做到是要被奖励的：到期解除自动记一笔心动
                ledgerMapper.insert(CouplePointLedger.of(space.getId(), v.getOwnerUser(),
                        CouplePointLedger.TYPE_EARN, "说到做到：" + v.getContent(), VOW_KEPT_POINTS));
                push.pushCoupleEventBoth("world-vow-kept", v.getOwnerUser(), space.getUserA(), space.getUserB(),
                        CoupleWorldBank.vowKeptLine() + "（" + v.getContent() + "）");
            }
        }
    }

    // ========== 聚合 ==========

    private WorldVO build(CoupleSpace space, String me, LocalDate now) {
        String today = now.toString();
        String month = today.substring(0, 7);
        boolean isA = me.equals(space.getUserA());

        List<VisitVO> visits = new ArrayList<>();
        int visitKept = 0;
        for (CoupleWorldVisit v : visitMapper.findBySpace(space.getId())) {
            if (visitKept++ >= LIST_VISIT_MAX) {
                break;
            }
            List<PrepVO> preps = new ArrayList<>();
            List<String> raw = tokens(v.getPreps());
            for (int i = 0; i < raw.size(); i++) {
                preps.add(new PrepVO(i + 1, raw.get(i), prepKind(raw.get(i))));
            }
            visits.add(new VisitVO(v.getId(), v.getDay(), me.equals(v.getFromUser()), v.getHostSide(),
                    visitLabel(v.getHostSide()), preps, v.confirmedFlag(), v.getReport(), v.getStatus(),
                    !me.equals(v.getFromUser()) && !v.confirmedFlag()));
        }

        List<GiftVO> gifts = new ArrayList<>();
        int giftKept = 0;
        for (CoupleWorldGift g : giftMapper.findBySpace(space.getId())) {
            if (giftKept++ >= LIST_GIFT_MAX) {
                break;
            }
            gifts.add(new GiftVO(g.getId(), g.getPerson(), g.getIdea(), g.getBudget(), g.getAvoid(),
                    me.equals(g.getOwnerUser()), g.getTakerUser(), g.getStatus(),
                    CoupleWorldGift.STATUS_OPEN.equals(g.getStatus()) && !me.equals(g.getOwnerUser())));
        }

        List<ViewVO> views = new ArrayList<>();
        int filledViews = 0;
        for (int s = 1; s <= CoupleWorldFriendView.SLOT_MAX; s++) {
            CoupleWorldFriendView v = ensureView(space, s);
            if (v.isFilled()) {
                filledViews++;
            }
            views.add(new ViewVO(v.getSlot(), v.getQuestion(), v.getAskedTo(), v.getAnswer(),
                    v.getByUser(), v.isFilled()));
        }
        String viewLine = filledViews == CoupleWorldFriendView.SLOT_MAX
                ? CoupleWorldBank.friendViewLine(CoupleRitualBank.stableHash(space.getId() + "|view|" + today)) : "";

        List<DeclareVO> declares = new ArrayList<>();
        int declareKept = 0;
        for (CoupleWorldDeclare d : declareMapper.findBySpace(space.getId())) {
            if (declareKept++ >= LIST_DECLARE_MAX) {
                break;
            }
            declares.add(new DeclareVO(d.getMonth(), d.getText(), me.equals(d.getFromUser())));
        }

        List<CaptionVO> captions = new ArrayList<>();
        int captionKept = 0;
        for (CoupleWorldCaption c : captionMapper.findBySpace(space.getId())) {
            if (captionKept++ >= LIST_CAPTION_MAX) {
                break;
            }
            captions.add(new CaptionVO(c.getId(), c.getDay(), me.equals(c.getFromUser()), c.getSlot(),
                    c.getText(), c.wonFlag()));
        }

        List<CityVO> cities = new ArrayList<>();
        for (CoupleWorldCityPlan c : cityMapper.findBySpace(space.getId())) {
            long left = c.getArriveDay().isEmpty() ? -1
                    : java.time.temporal.ChronoUnit.DAYS.between(now, LocalDate.parse(c.getArriveDay()));
            cities.add(new CityVO(c.getId(), c.getCity(), c.getArriveDay(), me.equals(c.getFromUser()),
                    tokens(c.getItinerary()), c.getTransport(), tokens(c.getPackList()), left));
        }

        List<RelativeVO> relatives = new ArrayList<>();
        List<CoupleWorldRelativesQ> ordered = new ArrayList<>(relativeMapper.findBySpace(space.getId()));
        ordered.sort((a, b) -> a.getWrongCount() != b.getWrongCount()
                ? b.getWrongCount() - a.getWrongCount() : a.getTerm().compareTo(b.getTerm()));
        for (CoupleWorldRelativesQ q : ordered) {
            relatives.add(new RelativeVO(q.getId(), q.getTerm(), q.getQuestion(), q.getAnswer(),
                    me.equals(q.getFromUser()), q.getWrongCount(), q.getLastWrongDay(),
                    !me.equals(q.getFromUser())));
        }

        List<VowVO> vows = new ArrayList<>();
        int vowKept = 0;
        for (CoupleWorldVow v : vowMapper.findBySpace(space.getId())) {
            if (vowKept++ >= LIST_VOW_MAX) {
                break;
            }
            long left = java.time.temporal.ChronoUnit.DAYS.between(now, LocalDate.parse(v.getDueDay()));
            vows.add(new VowVO(v.getId(), v.getContent(), me.equals(v.getOwnerUser()), v.getDueDay(),
                    v.isWitnessed(), v.getStatus(), v.getBrokenNote(), left,
                    CoupleWorldVow.STATUS_OPEN.equals(v.getStatus()) && !v.isWitnessed()
                            && !me.equals(v.getOwnerUser()),
                    CoupleWorldVow.STATUS_OPEN.equals(v.getStatus()) && !me.equals(v.getOwnerUser())));
        }

        CoupleWorldGroupReport g = ensureGroup(space, today);
        String myLine = isA ? g.getLineA() : g.getLineB();
        String partnerLine = isA ? g.getLineB() : g.getLineA();
        // laugh_a 记的是「A 笑过」，所以读自己那一列；早先这里是反的（A 读到 laugh_b），
        // 结果我笑过后还能再笑一次、TA 笑过却把我锁死，partnerLaughed 也跟着错
        boolean iLaughed = isA ? g.laughAFlag() : g.laughBFlag();
        boolean partnerLaughed = isA ? g.laughBFlag() : g.laughAFlag();
        boolean both = g.laughAFlag() && g.laughBFlag();
        GroupVO groupVO = new GroupVO(today, myLine, partnerLine, iLaughed, partnerLaughed, both,
                CoupleWorldBank.groupLaughLine(
                        CoupleRitualBank.stableHash(space.getId() + "|group|" + today), both),
                myLine.isEmpty(), !myLine.isEmpty() && !partnerLine.isEmpty() && !iLaughed);

        List<ApologyVO> apologies = new ArrayList<>();
        int apologyKept = 0;
        for (CoupleWorldApology a : apologyMapper.findBySpace(space.getId())) {
            if (apologyKept++ >= LIST_APOLOGY_MAX) {
                break;
            }
            apologies.add(new ApologyVO(a.getId(), a.getToPerson(), me.equals(a.getFromUser()), a.getReason(),
                    a.getDraft(), a.getStatus(), a.getReviewNote(), me.equals(a.getFromUser())
                    ? CoupleWorldBank.APOLOGY_TEMPLATES.get(
                    Math.floorMod(CoupleRitualBank.stableHash(space.getId() + "|apo|" + a.getId()),
                            CoupleWorldBank.APOLOGY_TEMPLATES.size())) : ""));
        }

        return new WorldVO(today, month, visits, gifts, views, viewLine, declares, captions, cities,
                relatives, CoupleWorldBank.PACK_TEMPLATE, vows, groupVO, apologies);
    }

    // ========== 懒建行 ==========

    private CoupleWorldFriendView ensureView(CoupleSpace space, int slot) {
        CoupleWorldFriendView row = viewMapper.findBySlot(space.getId(), slot);
        if (row != null) {
            return row;
        }
        CoupleWorldFriendView fresh = CoupleWorldFriendView.of(space.getId(), slot,
                CoupleWorldBank.friendQuestion(
                        CoupleRitualBank.stableHash(space.getId() + "|viewq"), slot));
        viewMapper.insert(fresh);
        return fresh;
    }

    private CoupleWorldGroupReport ensureGroup(CoupleSpace space, String day) {
        CoupleWorldGroupReport row = groupMapper.findByDay(space.getId(), day);
        if (row != null) {
            return row;
        }
        CoupleWorldGroupReport fresh = CoupleWorldGroupReport.of(space.getId(), day);
        groupMapper.insert(fresh);
        return fresh;
    }

    // ========== 校验小件 ==========

    private CoupleWorldVisit requireVisit(CoupleSpace space, String id) {
        CoupleWorldVisit row = firstIn(visitMapper.findBySpace(space.getId()), id, CoupleWorldVisit::getId);
        if (row == null) {
            throw new BusinessException(400, "这份攻略不存在");
        }
        return row;
    }

    private CoupleWorldGift requireGift(CoupleSpace space, String id) {
        CoupleWorldGift row = firstIn(giftMapper.findBySpace(space.getId()), id, CoupleWorldGift::getId);
        if (row == null) {
            throw new BusinessException(400, "这条灵感不在池子里");
        }
        return row;
    }

    private CoupleWorldCaption requireCaption(CoupleSpace space, String id) {
        CoupleWorldCaption row = firstIn(captionMapper.findBySpace(space.getId()), id, CoupleWorldCaption::getId);
        if (row == null) {
            throw new BusinessException(400, "这条候选文案不存在");
        }
        return row;
    }

    private CoupleWorldRelativesQ requireRelative(CoupleSpace space, String id) {
        CoupleWorldRelativesQ row = firstIn(relativeMapper.findBySpace(space.getId()), id,
                CoupleWorldRelativesQ::getId);
        if (row == null) {
            throw new BusinessException(400, "这题不在称呼册上");
        }
        return row;
    }

    private CoupleWorldVow requireVow(CoupleSpace space, String id) {
        CoupleWorldVow row = firstIn(vowMapper.findBySpace(space.getId()), id, CoupleWorldVow::getId);
        if (row == null) {
            throw new BusinessException(400, "这条保证不存在");
        }
        return row;
    }

    private CoupleWorldApology requireApology(CoupleSpace space, String id) {
        CoupleWorldApology row = firstIn(apologyMapper.findBySpace(space.getId()), id, CoupleWorldApology::getId);
        if (row == null) {
            throw new BusinessException(400, "这封信没写过");
        }
        return row;
    }

    private <T> T firstIn(List<T> rows, String id, java.util.function.Function<T, String> idGetter) {
        if (id == null) {
            return null;
        }
        return rows.stream().filter(r -> id.equals(idGetter.apply(r))).findFirst().orElse(null);
    }

    private String visitLabel(String side) {
        return CoupleWorldVisit.SIDE_YOURS.equals(side) ? "你家" : "我家";
    }

    private String prepKind(String text) {
        if (text.startsWith("雷区")) {
            return "MINE";
        }
        if (text.startsWith("带")) {
            return "BRING";
        }
        return "TALK";
    }

    private List<String> splitItems(String raw, int limit, int eachMax) {
        List<String> out = new ArrayList<>();
        for (String r : (raw == null ? "" : raw).split("[,、\n]")) {
            String t = r.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > eachMax) {
                throw new BusinessException(400, "每条最多 " + eachMax + " 字");
            }
            if (!out.contains(t)) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            throw new BusinessException(400, "至少写一条");
        }
        if (out.size() > limit) {
            throw new BusinessException(400, "最多 " + limit + " 条，先顾眼前");
        }
        return out;
    }

    /** 小包清单可以为空（没备齐就不写），但仍受条数与字长约束。 */
    private List<String> splitOptional(String raw, int limit, int eachMax, int totalMax) {
        List<String> out = new ArrayList<>();
        for (String r : (raw == null ? "" : raw).split("[,、\n]")) {
            String t = r.trim();
            if (t.isEmpty() || out.contains(t)) {
                continue;
            }
            if (t.length() > eachMax) {
                throw new BusinessException(400, "每条最多 " + eachMax + " 字");
            }
            out.add(t);
        }
        if (out.size() > limit) {
            throw new BusinessException(400, "小包清单最多 " + limit + " 项，带不动");
        }
        if (String.join(",", out).length() > totalMax) {
            throw new BusinessException(400, "小包清单整单最多 " + totalMax + " 字，删几条再存");
        }
        return out;
    }

    private List<String> tokens(String csv) {
        List<String> out = new ArrayList<>();
        for (String raw : (csv == null ? "" : csv).split(",")) {
            String t = raw.trim();
            if (!t.isEmpty() && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private String capped(String v, int max, String label) {
        String t = v == null ? "" : v.trim();
        if (t.length() > max) {
            throw new BusinessException(400, label + "最多 " + max + " 字");
        }
        return t;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private String validDay(String v, String label) {
        try {
            return LocalDate.parse(v.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, label + "要写成 yyyy-MM-dd");
        }
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
