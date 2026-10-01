package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 传世系统（F340-F349，批次三十，v6 收官批）：年度十问、记忆库年审、续约发布会、里程碑倒推、
 * 恋爱汇率、情侣品牌、我们的一年、传世清单、周年抽奖箱、空间等级。
 * 情绪价值设计：把一年攒下的琐碎互动升级成「资产」——有年报、有汇率、有商标、有传世清单，
 * 让「我们」这件事看起来值得长期持有。
 */
@Service
public class CoupleLegacyService {

    static final int ANSWER_MAX = CoupleLegacyTen.ANSWER_MAX;
    static final int THREE_MAX = CoupleLegacyAudit.THREE_MAX;
    static final int SPEECH_MAX = 600;
    static final int NOTE_MAX = 140;
    static final int FX_MIN = 1;
    static final int FX_MAX = 20;
    static final int BRAND_NAME_MAX = 30;
    static final int BRAND_SLOGAN_MAX = 60;
    static final int BRAND_INTRO_MAX = 300;
    static final int ITEM_MAX = 60;
    static final int ITEM_DETAIL_MAX = 200;
    static final int PRIZE_MAX = 80;
    static final int DEFAULT_GOAL = 300;
    static final int LEVEL_MAX = 99;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleLegacyTenMapper tenMapper;
    private final CoupleLegacyAuditMapper auditMapper;
    private final CoupleLegacySpeechMapper speechMapper;
    private final CoupleLegacyFxMapper fxMapper;
    private final CoupleLegacyBrandMapper brandMapper;
    private final CoupleLegacyReviewMapper reviewMapper;
    private final CoupleLegacyItemMapper itemMapper;
    private final CoupleLegacyDrawMapper drawMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final ImPushService push;

    public CoupleLegacyService(CoupleSpaceMapper spaceMapper, CoupleLegacyTenMapper tenMapper,
                               CoupleLegacyAuditMapper auditMapper, CoupleLegacySpeechMapper speechMapper,
                               CoupleLegacyFxMapper fxMapper, CoupleLegacyBrandMapper brandMapper,
                               CoupleLegacyReviewMapper reviewMapper, CoupleLegacyItemMapper itemMapper,
                               CoupleLegacyDrawMapper drawMapper, CouplePointLedgerMapper ledgerMapper,
                               ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.tenMapper = tenMapper;
        this.auditMapper = auditMapper;
        this.speechMapper = speechMapper;
        this.fxMapper = fxMapper;
        this.brandMapper = brandMapper;
        this.reviewMapper = reviewMapper;
        this.itemMapper = itemMapper;
        this.drawMapper = drawMapper;
        this.ledgerMapper = ledgerMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record TenVO(String year, boolean mine, String myAnswersJoined, String partnerAnswersJoined,
                        List<String> questions, List<String> myAnswers, List<String> partnerAnswers,
                        int answeredCount, boolean bothDone) {
    }

    public record AuditVO(String year, boolean mine, List<String> keepThree, List<String> deleteThree,
                          String note, int submitted) {
    }

    public record SpeechVO(String year, boolean mine, String text, Integer score, String scoreNote,
                           String ratedBy, boolean canRate) {
    }

    public record FxVO(String fromUser, int kissToHug, int hugToWord, String settledYear, String settleLine) {
    }

    public record BrandVO(String name, String slogan, String intro, boolean published, boolean mine,
                          String line) {
    }

    public record ReviewVO(String year, boolean mine, String content) {
    }

    public record ItemVO(String id, String item, String kind, String detail, boolean mine, String status,
                         String signedBy, boolean canSeal) {
    }

    public record DrawVO(String year, String prizeMine, String prizePartner, boolean drawnMine,
                         boolean drawnPartner, boolean remindable) {
    }

    public record MilestoneVO(int goal, int achieved, int last30, long estimateDays,
                              String estimateDay, String advice) {
    }

    public record LevelVO(int level, String title, int total, int ledgerCount, int legacyCount, String line) {
    }

    public record LegacyVO(String day, String year, List<TenVO> tens, List<AuditVO> audits,
                           List<SpeechVO> speeches, List<FxVO> fxes, BrandVO brand, List<ReviewVO> reviews,
                           List<ItemVO> items, DrawVO draw, MilestoneVO milestone, LevelVO level) {
    }

    // ========== 读 ==========

    /** 传世系统总览（milestone 目标可传，缺省按 300 次互动估）。 */
    public LegacyVO legacy(String me, Integer goal) {
        CoupleSpace space = requireSpace(me);
        settle(space, LocalDate.now());
        return build(space, me, LocalDate.now(), goal == null ? DEFAULT_GOAL : goal);
    }

    // ========== F340 年度十问 ==========

    /** 答一题（一题一填，本人可改写当题）。 */
    public LegacyVO tenAnswer(String me, String year, Integer slot, String answer) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份写成 yyyy");
        }
        int s = slot == null ? 1 : slot;
        if (s < 1 || s > CoupleLegacyTen.QUESTION_COUNT) {
            throw new BusinessException(400, "十问只有 1-10 题");
        }
        String a = trim(answer, "这一题总得答一句").replace("\n", " ").replace("\r", " ");
        if (a.length() > ANSWER_MAX) {
            throw new BusinessException(400, "每题最多 " + ANSWER_MAX + " 字");
        }
        CoupleLegacyTen row = tenMapper.findByYearUser(space.getId(), y, me);
        boolean fresh = row == null;
        if (fresh) {
            row = CoupleLegacyTen.of(space.getId(), y, me);
        }
        List<String> answers = lines(row.getAnswers(), CoupleLegacyTen.QUESTION_COUNT);
        boolean wasEmpty = answers.get(s - 1).isEmpty();
        answers.set(s - 1, a);
        row.setAnswers(String.join(CoupleLegacyTen.SEP, answers));
        row.setUpdatedAt(System.currentTimeMillis());
        if (fresh) {
            tenMapper.insert(row);
        } else {
            tenMapper.updateById(row);
        }
        if (wasEmpty && answeredCount(row.getAnswers()) == CoupleLegacyTen.QUESTION_COUNT) {
            push.pushCoupleEvent("legacy-ten-done", me, space.partnerOf(me),
                    "TA 把 " + y + " 的十问答满了，等你那一份 📜");
        }
        return build(space, me, now, DEFAULT_GOAL);
    }

    // ========== F341 记忆库年审 ==========

    /** 提交年审意见（最想留/最想删各 ≤3 条，本人可改）。 */
    public LegacyVO audit(String me, String year, String keepThree, String deleteThree, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份写成 yyyy");
        }
        List<String> keep = splitThree(keepThree, "最想留");
        List<String> del = splitThree(deleteThree, "最想删");
        if (keep.isEmpty() && del.isEmpty()) {
            throw new BusinessException(400, "至少留一条：要么想留要么想删");
        }
        String n = note == null ? "" : note.trim();
        if (n.length() > NOTE_MAX) {
            throw new BusinessException(400, "一句话最多 " + NOTE_MAX + " 字");
        }
        CoupleLegacyAudit row = auditMapper.findByYear(space.getId(), y).stream()
                .filter(a -> a.getFromUser().equals(me)).findFirst().orElse(null);
        if (row == null) {
            row = CoupleLegacyAudit.of(space.getId(), y, me);
            auditMapper.insert(row);
        }
        row.setKeepThree(String.join(",", keep));
        row.setDeleteThree(String.join(",", del));
        row.setNote(n);
        row.setUpdatedAt(System.currentTimeMillis());
        auditMapper.updateById(row);
        push.pushCoupleEvent("legacy-audit", me, space.partnerOf(me),
                y + " 的记忆库年审有人交卷了：留 " + keep.size() + " 条，删 " + del.size() + " 条 🗄️");
        return build(space, me, now, DEFAULT_GOAL);
    }

    // ========== F342 续约发布会 ==========

    /** 发这年的发言稿（本人可改，改后需重评）。 */
    public LegacyVO speech(String me, String year, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份写成 yyyy");
        }
        String t = trim(text, "发言稿总要说一句");
        if (t.length() > SPEECH_MAX) {
            throw new BusinessException(400, "发言稿最多 " + SPEECH_MAX + " 字");
        }
        CoupleLegacySpeech row = speechMapper.findByYear(space.getId(), y).stream()
                .filter(s -> s.getFromUser().equals(me)).findFirst().orElse(null);
        if (row == null) {
            speechMapper.insert(CoupleLegacySpeech.of(space.getId(), y, me, t));
            push.pushCoupleEvent("legacy-speech", me, space.partnerOf(me),
                    "TA 发布了 " + y + " 年度发言，等你按评分卡打分 🎤");
        } else {
            row.setText(t);
            row.setScore(null);
            row.setScoreNote("");
            row.setRatedBy("");
            row.setUpdatedAt(System.currentTimeMillis());
            speechMapper.updateById(row);
            push.pushCoupleEvent("legacy-speech", me, space.partnerOf(me),
                    "TA 重发了发言，之前的分作废，再打一次 🎤");
        }
        return build(space, me, now, DEFAULT_GOAL);
    }

    /** 对方按评分卡打分（1-5）。 */
    public LegacyVO speechRate(String me, String year, Integer score, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear()) : year.trim();
        CoupleLegacySpeech row = speechMapper.findByYear(space.getId(), y).stream()
                .filter(s -> !s.getFromUser().equals(me)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "TA 还没发这年的言");
        }
        if (row.isRated()) {
            throw new BusinessException(400, "这年的分已经打过了");
        }
        int v = score == null ? 0 : score;
        if (v < 1 || v > 5) {
            throw new BusinessException(400, "评分卡只有 1-5 档");
        }
        String n = note == null ? "" : note.trim();
        if (n.length() > NOTE_MAX) {
            throw new BusinessException(400, "评语最多 " + NOTE_MAX + " 字");
        }
        row.setScore(v);
        row.setScoreNote(n);
        row.setRatedBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        speechMapper.updateById(row);
        push.pushCoupleEventBoth("legacy-speech-rated", me, space.getUserA(), space.getUserB(),
                "评分卡 " + v + " 分：" + CoupleLegacyBank.speechScoreLine(v) + (n.isEmpty() ? "" : "｜" + n));
        return build(space, me, now, DEFAULT_GOAL);
    }

    // ========== F344 恋爱汇率 ==========

    /** 报汇率（本人可改，两人都报才算生效）。 */
    public LegacyVO fx(String me, Integer kissToHug, Integer hugToWord) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        int k = clampRate(kissToHug, "1 个亲亲 = 几个抱抱");
        int h = clampRate(hugToWord, "1 个抱抱 = 句夸夸");
        CoupleLegacyFx row = fxMapper.findByUser(space.getId(), me);
        if (row == null) {
            fxMapper.insert(CoupleLegacyFx.of(space.getId(), me, k, h));
        } else {
            row.setKissToHug(k);
            row.setHugToWord(h);
            row.setUpdatedAt(System.currentTimeMillis());
            fxMapper.updateById(row);
        }
        push.pushCoupleEvent("legacy-fx", me, space.partnerOf(me),
                "TA 报了新汇率：1 亲亲 = " + k + " 抱抱 = " + (k * h) + " 句夸夸 💱");
        return build(space, me, now, DEFAULT_GOAL);
    }

    /** 年末趣味结算（两人都报了才能结，同年只结一次）。 */
    public LegacyVO fxSettle(String me, String year) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear()) : year.trim();
        List<CoupleLegacyFx> rows = fxMapper.findBySpace(space.getId());
        if (rows.size() < 2) {
            throw new BusinessException(400, "两人都报过汇率才结得了账");
        }
        for (CoupleLegacyFx f : rows) {
            if (y.equals(f.getSettledYear())) {
                throw new BusinessException(400, y + " 已经结算过了，明年重新开盘");
            }
        }
        CoupleLegacyFx mine = rows.stream().filter(f -> f.getFromUser().equals(me)).findFirst().orElseThrow();
        push.pushCoupleEventBoth("legacy-fx-settled", me, space.getUserA(), space.getUserB(),
                CoupleLegacyBank.fxSettleLine(CoupleRitualBank.stableHash(space.getId() + "|fx|" + y),
                        mine.getKissToHug(), mine.getHugToWord()));
        for (CoupleLegacyFx f : rows) {
            f.setSettledYear(y);
            f.setUpdatedAt(System.currentTimeMillis());
            fxMapper.updateById(f);
        }
        return build(space, me, now, DEFAULT_GOAL);
    }

    private int yearOf(Long ts) {
        return ts == null ? 0 : LocalDate.ofInstant(java.time.Instant.ofEpochMilli(ts),
                java.time.ZoneId.systemDefault()).getYear();
    }

    private int clampRate(Integer v, String label) {
        int x = v == null ? 0 : v;
        if (x < FX_MIN || x > FX_MAX) {
            throw new BusinessException(400, label + "，只能填 " + FX_MIN + "-" + FX_MAX);
        }
        return x;
    }

    // ========== F345 情侣品牌 ==========

    /** 建/改品牌并申请发布（发布要对方确认）。 */
    public LegacyVO brand(String me, String name, String slogan, String intro) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String n = trim(name, "关系叫什么名");
        if (n.length() > BRAND_NAME_MAX) {
            throw new BusinessException(400, "名字最多 " + BRAND_NAME_MAX + " 字");
        }
        String s = capped(slogan, BRAND_SLOGAN_MAX, "slogan");
        String i = capped(intro, BRAND_INTRO_MAX, "产品简介");
        CoupleLegacyBrand row = brandMapper.find(space.getId());
        if (row == null) {
            brandMapper.insert(CoupleLegacyBrand.of(space.getId(), n, s, i, me));
            push.pushCoupleEvent("legacy-brand", me, space.partnerOf(me),
                    "TA 拟了品牌「" + n + "」，发布要你确认 🏷️");
            return build(space, me, now, DEFAULT_GOAL);
        }
        row.setName(n);
        row.setSlogan(s);
        row.setIntro(i);
        row.setByUser(me);
        row.setPublished(0);
        row.setConfirmedBy("");
        row.setUpdatedAt(System.currentTimeMillis());
        brandMapper.updateById(row);
        push.pushCoupleEvent("legacy-brand", me, space.partnerOf(me),
                "品牌改过了，重新发布要再确认一次 🏷️");
        return build(space, me, now, DEFAULT_GOAL);
    }

    /** 确认发布（只有非编辑者能确认，发布后显示在空间头部）。 */
    public LegacyVO brandConfirm(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleLegacyBrand row = brandMapper.find(space.getId());
        if (row == null) {
            throw new BusinessException(400, "还没有品牌可发布");
        }
        if (row.getByUser().equals(me)) {
            throw new BusinessException(400, "拟品牌的人自己确认不作数");
        }
        if (row.isPublished()) {
            return build(space, me, now, DEFAULT_GOAL);
        }
        row.setPublished(1);
        row.setConfirmedBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        brandMapper.updateById(row);
        push.pushCoupleEventBoth("legacy-brand-published", me, space.getUserA(), space.getUserB(),
                CoupleLegacyBank.brandLine(CoupleRitualBank.stableHash(space.getId() + "|brand|" + row.getName())));
        return build(space, me, now, DEFAULT_GOAL);
    }

    // ========== F346 我们的一年 ==========

    /** 一键生成年度盘点（真数字，本人可重生覆盖当年那份）。 */
    public LegacyVO review(String me, String year) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear() - 1) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份写成 yyyy");
        }
        String content = composeReview(space, y, now);
        CoupleLegacyReview row = reviewMapper.findByYear(space.getId(), y);
        if (row == null) {
            reviewMapper.insert(CoupleLegacyReview.of(space.getId(), y, content, me));
        } else {
            row.setContent(content);
            row.setUpdatedAt(System.currentTimeMillis());
            reviewMapper.updateById(row);
        }
        push.pushCoupleEvent("legacy-review", me, space.partnerOf(me), y + " 的年度盘点生成好了，来看 📚");
        return build(space, me, now, DEFAULT_GOAL);
    }

    private String composeReview(CoupleSpace space, String y, LocalDate now) {
        List<CoupleLegacyTen> tens = tenMapper.findByYear(space.getId(), y);
        int tenAnswers = tens.stream().mapToInt(t -> answeredCount(t.getAnswers())).sum();
        int audits = auditMapper.findByYear(space.getId(), y).size();
        List<CoupleLegacySpeech> speeches = speechMapper.findByYear(space.getId(), y);
        int rated = (int) speeches.stream().filter(CoupleLegacySpeech::isRated).count();
        int items = (int) itemMapper.findBySpace(space.getId()).stream()
                .filter(i -> i.getCreated() != null && yearOf(i.getCreated()) == Integer.parseInt(y)).count();
        int ledger = (int) ledgerMapper.findBySpace(space.getId()).stream()
                .filter(l -> yearOf(l.getCreated()) == Integer.parseInt(y)).count();
        List<CoupleLegacyAudit> mineAudits = auditMapper.findByYear(space.getId(), y).stream()
                .filter(a -> a.getFromUser().equals(space.getUserA())).toList();
        String keeps = mineAudits.isEmpty() ? "" : mineAudits.get(0).getKeepThree();
        return y + " 年我们的一年：" + ledger + " 笔互动进了台账，十问共答了 " + tenAnswers + " 条，"
                + "记忆库年审交了 " + audits + " 份，年度发言 " + speeches.size() + " 篇（已评分 " + rated + " 篇），"
                + "传世清单在册 " + items + " 项。"
                + (keeps.isEmpty() ? "" : "最想留下的是：" + keeps + "。")
                + CoupleLegacyBank.reviewTail(CoupleRitualBank.stableHash(space.getId() + "|review|" + y));
    }

    // ========== F347 传世清单 ==========

    /** 登记一条「想留给你」。 */
    public LegacyVO itemAdd(String me, String item, String kind, String detail) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String it = trim(item, "留给 TA 的东西叫什么");
        if (it.length() > ITEM_MAX) {
            throw new BusinessException(400, "条目名最多 " + ITEM_MAX + " 字");
        }
        if (itemMapper.findByItem(space.getId(), it) != null) {
            throw new BusinessException(400, "「" + it + "」已经在清单上了");
        }
        String k = kind == null || kind.isBlank() ? "THING" : kind.trim().toUpperCase();
        if (!CoupleLegacyItem.KINDS.contains(k)) {
            throw new BusinessException(400, "类型只有 PLACE/PASSWORD/THING/WORD");
        }
        String d = detail == null ? "" : detail.trim();
        if (d.length() > ITEM_DETAIL_MAX) {
            throw new BusinessException(400, "说明最多 " + ITEM_DETAIL_MAX + " 字");
        }
        itemMapper.insert(CoupleLegacyItem.of(space.getId(), it, k, d, me));
        push.pushCoupleEvent("legacy-item", me, space.partnerOf(me),
                "传世清单新添「" + it + "」（" + itemKindLabel(k) + "），要你双签封存 🗝️");
        return build(space, me, now, DEFAULT_GOAL);
    }

    /** 对方加签封存。 */
    public LegacyVO itemSeal(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleLegacyItem row = id == null ? null : itemMapper.findBySpace(space.getId()).stream()
                .filter(i -> i.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这条不在清单上");
        }
        if (row.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "封存要对方签字，自己签不封");
        }
        if (row.isSealed()) {
            return build(space, me, now, DEFAULT_GOAL);
        }
        row.setStatus(CoupleLegacyItem.STATUS_SEALED);
        row.setSignedBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        itemMapper.updateById(row);
        push.pushCoupleEventBoth("legacy-sealed", me, space.getUserA(), space.getUserB(),
                CoupleLegacyBank.sealedLine(CoupleRitualBank.stableHash(space.getId() + "|seal|" + row.getId())));
        return build(space, me, now, DEFAULT_GOAL);
    }

    // ========== F348 周年抽奖箱 ==========

    /** 抽今年的奖（每人一年一次，奖池用 Bank 迷你愿望位）。 */
    public LegacyVO draw(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String y = String.valueOf(now.getYear());
        CoupleLegacyDraw row = ensureDraw(space, y);
        boolean isA = me.equals(space.getUserA());
        if (isA ? row.isDrawnA() : row.isDrawnB()) {
            throw new BusinessException(400, "今年你已经抽过了，剩下的那次是 TA 的");
        }
        int idx = Math.floorMod(CoupleRitualBank.stableHash(space.getId() + "|draw|" + y + "|" + me),
                CoupleLegacyBank.PRIZES.size());
        String prize = CoupleLegacyBank.PRIZES.get(idx);
        if (isA) {
            row.setPrizeA(prize);
            row.setDrawnA(1);
        } else {
            row.setPrizeB(prize);
            row.setDrawnB(1);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        drawMapper.updateById(row);
        push.pushCoupleEvent("legacy-draw", me, space.partnerOf(me),
                "TA 抽到了：「" + prize + "」🎰 你也来一次");
        return build(space, me, now, DEFAULT_GOAL);
    }

    // ========== 惰性结算（周年提醒，不建 Job） ==========

    private void settle(CoupleSpace space, LocalDate now) {
        String y = String.valueOf(now.getYear());
        CoupleLegacyDraw row = drawMapper.findByYear(space.getId(), y);
        if (row == null || row.isNotified() || space.getAnniversary() == null || space.getAnniversary().isBlank()) {
            return;
        }
        LocalDate anniv = parseMd(space.getAnniversary(), now.getYear());
        if (anniv == null || now.isBefore(anniv)) {
            return;
        }
        row.setNotified(1);
        row.setUpdatedAt(System.currentTimeMillis());
        drawMapper.updateById(row);
        push.pushCoupleEventBoth("legacy-draw-remind", space.getUserA(), space.getUserA(), space.getUserB(),
                CoupleLegacyBank.drawRemindLine());
    }

    private CoupleLegacyDraw ensureDraw(CoupleSpace space, String year) {
        CoupleLegacyDraw row = drawMapper.findByYear(space.getId(), year);
        if (row != null) {
            return row;
        }
        CoupleLegacyDraw fresh = CoupleLegacyDraw.of(space.getId(), year);
        drawMapper.insert(fresh);
        return fresh;
    }

    // ========== 聚合 ==========

    private LegacyVO build(CoupleSpace space, String me, LocalDate now, int goal) {
        String year = String.valueOf(now.getYear());
        String partner = space.partnerOf(me);

        List<TenVO> tens = new ArrayList<>();
        for (String y : List.of(year, String.valueOf(now.getYear() - 1))) {
            CoupleLegacyTen mineRow = tenMapper.findByYearUser(space.getId(), y, me);
            CoupleLegacyTen otherRow = tenMapper.findByYearUser(space.getId(), y, partner);
            List<String> myLines = lines(mineRow == null ? "" : mineRow.getAnswers(), CoupleLegacyTen.QUESTION_COUNT);
            List<String> otherLines = lines(otherRow == null ? "" : otherRow.getAnswers(), CoupleLegacyTen.QUESTION_COUNT);
            tens.add(new TenVO(y, true, String.join(" / ", myLines), String.join(" / ", otherLines),
                    CoupleLegacyBank.TEN_QUESTIONS, myLines, otherLines, answeredCount(
                    mineRow == null ? "" : mineRow.getAnswers()),
                    answeredCount(mineRow == null ? "" : mineRow.getAnswers()) == CoupleLegacyTen.QUESTION_COUNT
                            && answeredCount(otherRow == null ? "" : otherRow.getAnswers())
                            == CoupleLegacyTen.QUESTION_COUNT));
        }

        List<AuditVO> audits = new ArrayList<>();
        for (CoupleLegacyAudit a : auditMapper.findBySpace(space.getId())) {
            audits.add(new AuditVO(a.getYear(), me.equals(a.getFromUser()), csv(a.getKeepThree()),
                    csv(a.getDeleteThree()), a.getNote(),
                    auditMapper.findByYear(space.getId(), a.getYear()).size()));
        }

        List<SpeechVO> speeches = new ArrayList<>();
        for (CoupleLegacySpeech s : speechMapper.findBySpace(space.getId())) {
            speeches.add(new SpeechVO(s.getYear(), me.equals(s.getFromUser()), s.getText(), s.getScore(),
                    s.getScoreNote(), s.getRatedBy(), !me.equals(s.getFromUser()) && !s.isRated()));
        }

        List<FxVO> fxes = new ArrayList<>();
        for (CoupleLegacyFx f : fxMapper.findBySpace(space.getId())) {
            fxes.add(new FxVO(f.getFromUser(), f.getKissToHug(), f.getHugToWord(), f.getSettledYear(),
                    f.getSettledYear().isEmpty() ? "" : CoupleLegacyBank.fxSettleLine(
                            CoupleRitualBank.stableHash(space.getId() + "|fx|" + f.getSettledYear()),
                            f.getKissToHug(), f.getHugToWord())));
        }

        CoupleLegacyBrand brandRow = brandMapper.find(space.getId());
        BrandVO brand = brandRow == null ? new BrandVO("", "", "", false, false, "")
                : new BrandVO(brandRow.getName(), brandRow.getSlogan(), brandRow.getIntro(),
                brandRow.isPublished(), me.equals(brandRow.getByUser()),
                brandRow.isPublished() ? CoupleLegacyBank.brandLine(
                        CoupleRitualBank.stableHash(space.getId() + "|brand|" + brandRow.getName())) : "");

        List<ReviewVO> reviews = new ArrayList<>();
        for (CoupleLegacyReview r : reviewMapper.findBySpace(space.getId())) {
            reviews.add(new ReviewVO(r.getYear(), me.equals(r.getFromUser()), r.getContent()));
        }

        List<ItemVO> items = new ArrayList<>();
        for (CoupleLegacyItem i : itemMapper.findBySpace(space.getId())) {
            items.add(new ItemVO(i.getId(), i.getItem(), i.getKind(), i.getDetail(),
                    me.equals(i.getOwnerUser()), i.getStatus(), i.getSignedBy(),
                    !me.equals(i.getOwnerUser()) && !i.isSealed()));
        }

        CoupleLegacyDraw draw = ensureDraw(space, year);
        boolean isA = me.equals(space.getUserA());
        DrawVO drawVO = new DrawVO(year, isA ? draw.getPrizeA() : draw.getPrizeB(),
                isA ? draw.getPrizeB() : draw.getPrizeA(),
                isA ? draw.isDrawnA() : draw.isDrawnB(), isA ? draw.isDrawnB() : draw.isDrawnA(),
                draw.isNotified() && !(isA ? draw.isDrawnA() : draw.isDrawnB()));

        int g = goal < 10 || goal > 100000 ? DEFAULT_GOAL : goal;
        MilestoneVO milestone = milestone(space, g, now);
        LevelVO level = level(space);

        return new LegacyVO(now.toString(), year, tens, audits, speeches, fxes, brand, reviews, items,
                drawVO, milestone, level);
    }

    // ========== F343 里程碑倒推 / F349 空间等级（读时算，无缓存） ==========

    private MilestoneVO milestone(CoupleSpace space, int goal, LocalDate now) {
        List<CouplePointLedger> rows = ledgerMapper.findBySpace(space.getId());
        long cutoff = now.minusDays(30).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        int achieved = rows.size();
        int last30 = (int) rows.stream().filter(r -> r.getCreated() != null && r.getCreated() >= cutoff).count();
        long remaining = Math.max(0, goal - achieved);
        long estimateDays = last30 == 0 ? -1 : (long) Math.ceil(remaining * 30.0 / last30);
        return new MilestoneVO(goal, achieved, last30, estimateDays,
                estimateDays < 0 ? "" : now.plusDays(estimateDays).toString(),
                last30 == 0 ? "近 30 天没有互动记录，先攒一周再来倒推。"
                        : CoupleLegacyBank.speedupLine(CoupleRitualBank.stableHash(space.getId() + "|ms|" + goal)));
    }

    private LevelVO level(CoupleSpace space) {
        int ledgerCount = ledgerMapper.findBySpace(space.getId()).size();
        int legacyCount = tenMapper.findBySpace(space.getId()).size()
                + auditMapper.findBySpace(space.getId()).size()
                + speechMapper.findBySpace(space.getId()).size()
                + itemMapper.findBySpace(space.getId()).size()
                + reviewMapper.findBySpace(space.getId()).size();
        int total = ledgerCount + legacyCount;
        int idx = 0;
        for (int i = 0; i < CoupleLegacyBank.LEVEL_TITLES.size(); i++) {
            if (total >= Integer.parseInt(CoupleLegacyBank.LEVEL_TITLES.get(i)[0])) {
                idx = i;
            }
        }
        int level = Math.min(LEVEL_MAX, idx + 1 + total / 50);
        String title = CoupleLegacyBank.LEVEL_TITLES.get(idx)[1];
        return new LevelVO(level, title, total, ledgerCount, legacyCount,
                CoupleLegacyBank.levelLine(level, title));
    }

    // ========== 小件 ==========

    private List<String> lines(String raw, int count) {
        List<String> out = new ArrayList<>();
        String[] parts = (raw == null ? "" : raw).split(CoupleLegacyTen.SEP, -1);
        for (int i = 0; i < count; i++) {
            out.add(i < parts.length ? parts[i].trim() : "");
        }
        return out;
    }

    private int answeredCount(String raw) {
        return (int) lines(raw, CoupleLegacyTen.QUESTION_COUNT).stream().filter(s -> !s.isEmpty()).count();
    }

    private List<String> csv(String raw) {
        List<String> out = new ArrayList<>();
        for (String r : (raw == null ? "" : raw).split(",")) {
            String t = r.trim();
            if (!t.isEmpty() && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private List<String> splitThree(String raw, String label) {
        List<String> out = csv(raw);
        if (out.size() > THREE_MAX) {
            throw new BusinessException(400, label + "最多 " + THREE_MAX + " 条，留最想的那几条");
        }
        for (String t : out) {
            if (t.length() > 60) {
                throw new BusinessException(400, label + "每条最多 60 字");
            }
        }
        return out;
    }

    private String itemKindLabel(String kind) {
        return switch (kind) {
            case "PLACE" -> "地点";
            case "PASSWORD" -> "口令";
            case "WORD" -> "一句话";
            default -> "东西";
        };
    }

    private LocalDate parseMd(String value, int year) {
        try {
            String v = value.trim();
            if (v.length() == 5) {
                return LocalDate.of(year, Integer.parseInt(v.substring(0, 2)), Integer.parseInt(v.substring(3)));
            }
            return LocalDate.parse(v.length() >= 10 ? v.substring(0, 10) : v);
        } catch (RuntimeException e) {
            return null;
        }
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

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
