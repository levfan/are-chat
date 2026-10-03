package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 周年抽奖箱（原 F348，系统裁剪后传世系统唯一保留项）。
 * 情绪价值设计：一年攒下的每一条「对 TA 好」都会变成箱子里的一个奖位——
 * 抽到的不是随机礼物，而是你们自己记下来的那些小事。
 */
@Service
public class CoupleLegacyService {

    /** 奖位取自台账条目的原文，钳到 80 字以免一条长文案把整只箱子挤变形。 */
    static final int PRIZE_MAX = 80;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleLegacyDrawMapper drawMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final ImPushService push;

    public CoupleLegacyService(CoupleSpaceMapper spaceMapper, CoupleLegacyDrawMapper drawMapper,
                               CouplePointLedgerMapper ledgerMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.drawMapper = drawMapper;
        this.ledgerMapper = ledgerMapper;
        this.push = push;
    }

    public record DrawVO(String year, String prizeMine, String prizePartner, boolean drawnMine,
                         boolean drawnPartner, boolean remindable) {
    }

    public record LegacyVO(String day, String year, DrawVO draw) {
    }

    /** 抽奖箱总览（含周年提醒的读时惰性结算）。 */
    public LegacyVO legacy(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        settle(space, now);
        return build(space, me, now);
    }

    /** 抽今年的奖（每人一年一次，奖池见 {@link #drawPool}）。 */
    public LegacyVO draw(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String y = String.valueOf(now.getYear());
        CoupleLegacyDraw row = ensureDraw(space, y);
        boolean isA = me.equals(space.getUserA());
        if (isA ? row.drawnAFlag() : row.drawnBFlag()) {
            throw new BusinessException(400, "今年你已经抽过了，剩下的那次是 TA 的");
        }
        List<String> pool = drawPool(space, y);
        String prize = pool.get(Math.floorMod(
                CoupleRitualBank.stableHash(space.getId() + "|draw|" + y + "|" + me), pool.size()));
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
        return build(space, me, now);
    }

    /**
     * 奖池：吃「当年攒的迷你愿望」——本年积分台账里 EARN 出来的条目就是他们自己攒下的愿望位，
     * 去重后按稳定哈希取一条；今年一条都没攒过，才回落 Bank 的固定迷你愿望位，保证箱子里永远有东西可抽。
     */
    private List<String> drawPool(CoupleSpace space, String y) {
        List<String> own = ledgerMapper.findBySpace(space.getId()).stream()
                .filter(l -> CouplePointLedger.TYPE_EARN.equals(l.getType()))
                .filter(l -> yearOf(l.getCreated()) == Integer.parseInt(y))
                .map(l -> l.getItem() == null ? "" : l.getItem().trim())
                .filter(s -> !s.isEmpty())
                .map(s -> s.length() > PRIZE_MAX ? s.substring(0, PRIZE_MAX) : s)
                .distinct()
                .toList();
        return own.isEmpty() ? CoupleLegacyBank.PRIZES : own;
    }

    /** 周年提醒走读时惰性结算，不新建定时任务。 */
    private void settle(CoupleSpace space, LocalDate now) {
        String y = String.valueOf(now.getYear());
        CoupleLegacyDraw row = drawMapper.findByYear(space.getId(), y);
        if (row == null || row.notifiedFlag() || space.getAnniversary() == null || space.getAnniversary().isBlank()) {
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

    /** 把「我这一侧」视角的抽奖箱装配成整份 VO。 */
    private LegacyVO build(CoupleSpace space, String me, LocalDate now) {
        String year = String.valueOf(now.getYear());
        CoupleLegacyDraw draw = ensureDraw(space, year);
        boolean isA = me.equals(space.getUserA());
        DrawVO vo = new DrawVO(year, isA ? draw.getPrizeA() : draw.getPrizeB(),
                isA ? draw.getPrizeB() : draw.getPrizeA(),
                isA ? draw.drawnAFlag() : draw.drawnBFlag(), isA ? draw.drawnBFlag() : draw.drawnAFlag(),
                draw.notifiedFlag() && !(isA ? draw.drawnAFlag() : draw.drawnBFlag()));
        return new LegacyVO(now.toString(), year, vo);
    }

    private int yearOf(Long ts) {
        return ts == null ? 0 : LocalDate.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).getYear();
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

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
