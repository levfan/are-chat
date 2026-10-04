package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.surprise.MysteryBox;
import com.smart.chat.couple.domain.surprise.MysteryBoxRepository;
import com.smart.chat.couple.domain.surprise.Scratch;
import com.smart.chat.couple.domain.surprise.ScratchRepository;
import com.smart.chat.couple.infrastructure.content.CoupleSurpriseBank;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.List;

import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 刮刮乐与盲盒（保留卡 `couple-surprise`，原 F50/F51）：
 * 券由 TA 送我刮、只有送券人能点「已兑现」（兑现即 +5 分归送券人）；
 * 盒子中装着一个约定日子的惊喜，到日才打得开。
 * <p>
 * 这里是编排器：取空间 → 让 {@link Scratch}／{@link MysteryBox} 自己守「收券人刮、送券人核销、
 * 到日才可拆、装盒人不能自拆」→ 落端口 → 记台账 → 推 WS → 投 VO。
 * <p>
 * 系统裁剪：心动闹钟、思念速递、藏宝图任务、告白重现全部下线（含其定时任务），
 * 保留卡内的定时推送只剩生日贺卡一条。
 */
@Service
public class CoupleSurpriseService {

    /** 积分口径：刮刮乐的券面被送券人真兑现了才计分，一次 +5。 */
    static final int SCRATCH_POINTS = 5;
    static final String SCRATCH_REASON_PREFIX = "刮刮乐兑现：";

    private final CoupleSpaceRepository spaceRepository;
    private final ScratchRepository scratchRepository;
    private final MysteryBoxRepository boxRepository;
    private final PointLedgerRepository ledgerRepository;
    private final CoupleEventPublisher push;

    public CoupleSurpriseService(CoupleSpaceRepository spaceRepository, ScratchRepository scratchRepository,
                                 MysteryBoxRepository boxRepository, PointLedgerRepository ledgerRepository,
                                 CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.scratchRepository = scratchRepository;
        this.boxRepository = boxRepository;
        this.ledgerRepository = ledgerRepository;
        this.push = push;
    }

    // ========== VO ==========

    public record ScratchVO(String id, String weekKey, String fromUser, String prizeKind,
                            /** 未刮开时对收券人隐藏券面 */
                            String prizeText, boolean scratched, boolean redeemed, Long scratchedAt) {
    }

    public record BoxVO(String id, String fromUser, String kind, String content, String openDay,
                        boolean opened, boolean canOpen, Long created) {
    }

    // ========== 爱情刮刮乐（F50） ==========

    /** 我的刮刮乐列表（自动补发本周的卡：双方各一张来自对方的券）。 */
    public List<ScratchVO> myScratches(String me) {
        CoupleSpace space = requireSpace(me);
        ensureWeekCards(space, weekKey(LocalDate.now()));
        return scratchRepository.findByOwner(space.id(), me).stream()
                .map(c -> toScratchVO(c, me))
                .toList();
    }

    /** 刮开我的券：只有收券人能刮，刮开后券面对双方可见。 */
    public ScratchVO scratch(String me, String id) {
        CoupleSpace space = requireSpace(me);
        Scratch card = scratchRepository.findByIdIn(id, space.id())
                .orElseThrow(() -> new BusinessException(404, "没有找到这张刮刮乐哦"));
        // 归属闸门与「已经刮过了」都在 Scratch.scratchBy 里：前者抛 403 原话，后者返回 false 走早退
        if (!rule(() -> card.scratchBy(me))) {
            return toScratchVO(card, me);
        }
        scratchRepository.save(card);
        push.pushCoupleEvent("scratch-scratched", me, card.fromUser(),
                "TA 刮开了你送的刮刮乐，抽中了「" + card.prizeText() + "」🎟️ 快准备兑现吧！");
        return toScratchVO(card, me);
    }

    /** 核销：只有送券人能点「已兑现」，让承诺闭环。 */
    public ScratchVO redeemScratch(String me, String id) {
        CoupleSpace space = requireSpace(me);
        Scratch card = scratchRepository.findByIdIn(id, space.id())
                .orElseThrow(() -> new BusinessException(404, "没有找到这张刮刮乐哦"));
        // 送券人闸门、未刮开不许核销、重复核销幂等，全在 Scratch.redeemBy
        if (!rule(() -> card.redeemBy(me))) {
            return toScratchVO(card, card.owner());
        }
        scratchRepository.save(card);
        // 积分重接三个入口之三：券是送的人兑现的，分记在送券人头上；
        // 闸门就是 redeemBy 的返回值，重复点核销不会再补分
        ledgerRepository.append(PointEntry.earn(space.id(), me,
                SCRATCH_REASON_PREFIX + card.prizeText(), SCRATCH_POINTS));
        push.pushCoupleEvent("scratch-redeemed", me, card.owner(),
                "你抽中的「" + card.prizeText() + "」已兑现 🎫 承诺 +1，甜度 +1！");
        return toScratchVO(card, card.owner());
    }

    /** 本周没有卡就补发：A→B、B→A 各一张，券面按周稳定抽取。 */
    private void ensureWeekCards(CoupleSpace space, String weekKey) {
        List<Scratch> existing = scratchRepository.findByWeek(space.id(), weekKey);
        for (String owner : List.of(space.userA(), space.userB())) {
            if (existing.stream().noneMatch(c -> c.ownedBy(owner))) {
                String from = space.partnerOf(owner);
                String[] prize = CoupleSurpriseBank.pickScratchPrize(space.id(), weekKey, owner);
                scratchRepository.save(Scratch.issue(space.id(), weekKey, from, owner, prize[0], prize[1]));
            }
        }
    }

    /** ISO 周标识（如 2026-W40）。 */
    private String weekKey(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private ScratchVO toScratchVO(Scratch card, String viewer) {
        // 没刮开时，只有送券人自己能看到券面（其实券面内容双方都能管中窥豹，这里对收券人隐藏以保留惊喜）
        return new ScratchVO(card.id(), card.weekKey(), card.fromUser(), card.prizeKind(),
                card.visiblePrizeFor(viewer), card.scratched(), card.redeemed(), card.scratchedAt());
    }

    // ========== 恋爱盲盒（F51） ==========

    public List<BoxVO> boxes(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate today = LocalDate.now();
        return boxRepository.findBySpace(space.id()).stream()
                .map(b -> toBoxVO(b, me, today))
                .toList();
    }

    /** 装一个盲盒：最早明天开箱，装好后对方立刻知道「有个盒子在等 TA」。 */
    public BoxVO createBox(String me, String kind, String content, String openDay) {
        // 现役顺序是先过内容与日期闸门、再取空间：没建空间的人提交坏内容仍收 400，所以先 pack 后 requireSpace
        MysteryBox packed = rule(() -> MysteryBox.pack(me, kind, content, openDay, LocalDate.now()));
        CoupleSpace space = requireSpace(me);
        MysteryBox box = packed.intoSpace(space.id());
        boxRepository.save(box);
        push.pushCoupleEvent("box-received", me, space.partnerOf(me),
                "🎁 TA 给你塞了一个神秘盲盒，" + box.openDate().getMonthValue() + " 月 "
                        + box.openDate().getDayOfMonth() + " 日开箱！期待值已拉满");
        return toBoxVO(box, me, LocalDate.now());
    }

    /** 开盲盒：只有 TA 能拆，且要到开箱日。 */
    public BoxVO openBox(String me, String id) {
        CoupleSpace space = requireSpace(me);
        MysteryBox box = requireBox(space, id);
        LocalDate today = LocalDate.now();
        // 装盒人不能自拆、到日才可拆、拆过不再推第二遍，全在 MysteryBox.openBy
        if (!rule(() -> box.openBy(me, today))) {
            return toBoxVO(box, me, today);
        }
        boxRepository.save(box);
        push.pushCoupleEvent("box-opened", me, box.fromUser(),
                "TA 拆开了你装的盲盒 🎉 快去看看 TA 的反应吧");
        return toBoxVO(box, me, today);
    }

    private BoxVO toBoxVO(MysteryBox box, String viewer, LocalDate today) {
        return new BoxVO(box.id(), box.fromUser(), box.kind(), box.visibleContentFor(viewer, today), box.openDay(),
                box.opened(), box.openableBy(viewer, today), box.created());
    }

    // ========== 心动闹钟（F52） ==========

    // ========== 思念速递（F53） ==========

    // ========== 藏宝图任务（F58） ==========

    // ========== 告白重现（F57） ==========

    // ========== 内部工具 ==========

    private MysteryBox requireBox(CoupleSpace space, String id) {
        return boxRepository.findByIdIn(id, space.id())
                .orElseThrow(() -> new BusinessException(404, "没有找到这个盲盒哦"));
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
