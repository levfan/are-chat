package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleSurpriseBank;
import com.smart.chat.couple.infrastructure.persistence.CoupleMysteryBoxPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleMysteryBoxMapper;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerPO;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleScratchPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleScratchMapper;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 刮刮乐与盲盒（保留卡 `couple-surprise`，原 F50/F51）：
 * 券由 TA 送我刮、只有送券人能点「已兑现」（兑现即 +5 分归送券人）；
 * 盒子中装着一个约定日子的惊喜，到日才打得开。
 *
 * 系统裁剪：心动闹钟、思念速递、藏宝图任务、告白重现全部下线（含其定时任务），
 * 保留卡内的定时推送只剩生日贺卡一条。
 */
@Service
public class CoupleSurpriseService {

    /** 积分口径：刮刮乐的券面被送券人真兑现了才计分，一次 +5。 */
    static final int SCRATCH_POINTS = 5;
    static final String SCRATCH_REASON_PREFIX = "刮刮乐兑现：";

    private final CoupleSpaceRepository spaceRepository;
    private final CoupleScratchMapper scratchMapper;
    private final CoupleMysteryBoxMapper boxMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final CoupleEventPublisher push;

    public CoupleSurpriseService(CoupleSpaceRepository spaceRepository, CoupleScratchMapper scratchMapper,
                                 CoupleMysteryBoxMapper boxMapper, CouplePointLedgerMapper ledgerMapper,
                                 CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.scratchMapper = scratchMapper;
        this.boxMapper = boxMapper;
        this.ledgerMapper = ledgerMapper;
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
        return scratchMapper.findByOwner(space.id(), me).stream()
                .map(c -> toScratchVO(c, me))
                .toList();
    }

    /** 刮开我的券：只有收券人能刮，刮开后券面对双方可见。 */
    public ScratchVO scratch(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleScratchPO card = scratchMapper.selectById(id);
        if (card == null || !card.getSpaceId().equals(space.id())) {
            throw new BusinessException(404, "没有找到这张刮刮乐哦");
        }
        if (!card.getOwner().equals(me)) {
            throw new BusinessException(403, "这是 TA 的刮刮乐，不能替 TA 刮哦");
        }
        if (card.isScratched()) {
            return toScratchVO(card, me);
        }
        card.setScratched(true);
        card.setScratchedAt(System.currentTimeMillis());
        scratchMapper.updateById(card);
        push.pushCoupleEvent("scratch-scratched", me, card.getFromUser(),
                "TA 刮开了你送的刮刮乐，抽中了「" + card.getPrizeText() + "」🎟️ 快准备兑现吧！");
        return toScratchVO(card, me);
    }

    /** 核销：只有送券人能点「已兑现」，让承诺闭环。 */
    public ScratchVO redeemScratch(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleScratchPO card = scratchMapper.selectById(id);
        if (card == null || !card.getSpaceId().equals(space.id())) {
            throw new BusinessException(404, "没有找到这张刮刮乐哦");
        }
        if (!card.getFromUser().equals(me)) {
            throw new BusinessException(403, "只有送券的人才能点核销哦");
        }
        if (!card.isScratched()) {
            throw new BusinessException(400, "TA 还没刮开这张券呢");
        }
        if (card.getRedeemedAt() == null) {
            card.setRedeemedAt(System.currentTimeMillis());
            scratchMapper.updateById(card);
            // 积分重接三个入口之三：券是送的人兑现的，分记在送券人头上；
            // 闸门就是这一层的 redeemedAt==null，重复点核销不会再补分
            ledgerMapper.insert(CouplePointLedgerPO.of(space.id(), me,
                    CouplePointLedgerPO.TYPE_EARN, SCRATCH_REASON_PREFIX + card.getPrizeText(), SCRATCH_POINTS));
            push.pushCoupleEvent("scratch-redeemed", me, card.getOwner(),
                    "你抽中的「" + card.getPrizeText() + "」已兑现 🎫 承诺 +1，甜度 +1！");
        }
        return toScratchVO(card, card.getOwner());
    }

    /** 本周没有卡就补发：A→B、B→A 各一张，券面按周稳定抽取。 */
    private void ensureWeekCards(CoupleSpace space, String weekKey) {
        List<CoupleScratchPO> existing = scratchMapper.findByWeek(space.id(), weekKey);
        for (String owner : List.of(space.userA(), space.userB())) {
            if (existing.stream().noneMatch(c -> c.getOwner().equals(owner))) {
                String from = space.partnerOf(owner);
                String[] prize = CoupleSurpriseBank.pickScratchPrize(space.id(), weekKey, owner);
                scratchMapper.insert(CoupleScratchPO.of(space.id(), weekKey, from, owner, prize[0], prize[1]));
            }
        }
    }

    /** ISO 周标识（如 2026-W40）。 */
    private String weekKey(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private ScratchVO toScratchVO(CoupleScratchPO card, String viewer) {
        // 没刮开时，只有送券人自己能看到券面（其实券面内容双方都能管中窥豹，这里对收券人隐藏以保留惊喜）
        String prizeText = card.isScratched() || card.getFromUser().equals(viewer) ? card.getPrizeText() : null;
        return new ScratchVO(card.getId(), card.getWeekKey(), card.getFromUser(), card.getPrizeKind(),
                prizeText, card.isScratched(), card.getRedeemedAt() != null, card.getScratchedAt());
    }

    // ========== 恋爱盲盒（F51） ==========

    public List<BoxVO> boxes(String me) {
        CoupleSpace space = requireSpace(me);
        return boxMapper.findBySpace(space.id()).stream()
                .map(b -> toBoxVO(b, me))
                .toList();
    }

    /** 装一个盲盒：最早明天开箱，装好后对方立刻知道「有个盒子在等 TA」。 */
    public BoxVO createBox(String me, String kind, String content, String openDay) {
        if (!CoupleMysteryBoxPO.isValidKind(kind)) {
            throw new BusinessException(400, "盲盒只能是悄悄话或小任务哦");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "盒子里总要放点什么吧～");
        }
        if (content.length() > CoupleMysteryBoxPO.CONTENT_MAX) {
            throw new BusinessException(400, "盒子太小啦，最多装 " + CoupleMysteryBoxPO.CONTENT_MAX + " 个字");
        }
        LocalDate open;
        try {
            open = LocalDate.parse(openDay);
        } catch (Exception e) {
            throw new BusinessException(400, "开箱日期不认识，选一个明天以后的日子吧");
        }
        if (!open.isAfter(LocalDate.now())) {
            throw new BusinessException(400, "盲盒最早明天才能拆哦，期待感要留足 ✨");
        }
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        CoupleMysteryBoxPO box = CoupleMysteryBoxPO.of(space.id(), me, kind, content.trim(), open);
        boxMapper.insert(box);
        push.pushCoupleEvent("box-received", me, partner,
                "🎁 TA 给你塞了一个神秘盲盒，" + open.getMonthValue() + " 月 " + open.getDayOfMonth()
                        + " 日开箱！期待值已拉满");
        return toBoxVO(box, me);
    }

    /** 开盲盒：只有 TA 能拆，且要到开箱日。 */
    public BoxVO openBox(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleMysteryBoxPO box = boxMapper.selectById(id);
        if (box == null || !box.getSpaceId().equals(space.id())) {
            throw new BusinessException(404, "没有找到这个盲盒哦");
        }
        if (box.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己装的盒子自己拆，就不惊喜了呀 😝");
        }
        String today = LocalDate.now().toString();
        if (box.getOpenDay().compareTo(today) > 0) {
            long waitDays = LocalDate.parse(box.getOpenDay()).toEpochDay() - LocalDate.now().toEpochDay();
            throw new BusinessException(400, "还没到开箱日！再等 " + waitDays + " 天，让期待多飞一会儿 🎈");
        }
        if (!box.isOpened()) {
            box.setOpened(true);
            box.setOpenedAt(System.currentTimeMillis());
            boxMapper.updateById(box);
            push.pushCoupleEvent("box-opened", me, box.getFromUser(),
                    "TA 拆开了你装的盲盒 🎉 快去看看 TA 的反应吧");
        }
        return toBoxVO(box, me);
    }

    private boolean canOpen(CoupleMysteryBoxPO box, String today, String me) {
        return !box.getFromUser().equals(me) && !box.isOpened() && box.getOpenDay().compareTo(today) <= 0;
    }

    private BoxVO toBoxVO(CoupleMysteryBoxPO box, String viewer) {
        String today = LocalDate.now().toString();
        String content = box.isOpened() || box.getFromUser().equals(viewer) || box.getOpenDay().compareTo(today) <= 0
                ? box.getContent() : null;
        return new BoxVO(box.getId(), box.getFromUser(), box.getKind(), content, box.getOpenDay(),
                box.isOpened(), canOpen(box, today, viewer), box.getCreated());
    }

    // ========== 心动闹钟（F52） ==========

    // ========== 思念速递（F53） ==========

    // ========== 藏宝图任务（F58） ==========

    // ========== 告白重现（F57） ==========

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
