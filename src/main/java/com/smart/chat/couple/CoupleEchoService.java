package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 回音壁（系统裁剪后保留四张卡：好事簿、鼓励语罐、能量补给、电量预报）。
 * 情绪价值设计：把「TA 对我做过什么」从易被忘掉的小事变成一册可翻的证据，
 * 低落时一键就能把这些证据连本带利取回来。
 * 好事簿同时是积分体系的第一个赚分入口：对 TA 好的人拿分。
 */
@Service
public class CoupleEchoService {

    static final int DEED_PAGE = 30;
    static final int REFILL_DEEDS = 3;

    /** 积分口径：记一笔「TA 为我做的事」给被记的那位 +2，记录人再加一颗星给同一位 +1。 */
    static final int DEED_POINTS = 2;
    static final int DEED_STAR_POINTS = 1;
    static final String DEED_REASON_PREFIX = "好事簿：";
    static final String DEED_STAR_REASON_PREFIX = "好事簿被加星：";

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleEchoDeedMapper deedMapper;
    private final CoupleEchoJuiceMapper juiceMapper;
    private final CoupleEchoRefillLogMapper refillLogMapper;
    private final CoupleEchoBatteryMapper batteryMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final ImPushService push;

    public CoupleEchoService(CoupleSpaceMapper spaceMapper, CoupleEchoDeedMapper deedMapper,
                             CoupleEchoJuiceMapper juiceMapper, CoupleEchoRefillLogMapper refillLogMapper,
                             CoupleEchoBatteryMapper batteryMapper, CouplePointLedgerMapper ledgerMapper,
                             ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.deedMapper = deedMapper;
        this.juiceMapper = juiceMapper;
        this.refillLogMapper = refillLogMapper;
        this.batteryMapper = batteryMapper;
        this.ledgerMapper = ledgerMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record DeedVO(String id, String fromUser, boolean mine, String content, String day,
                         boolean starred, long created) {
    }

    public record JuiceVO(String id, String fromUser, boolean mine, int idx, String content, long created) {
    }

    public record BatteryVO(String fromUser, boolean mine, int level, String want, String hint) {
    }

    public record RefillVO(boolean mineToday, boolean partnerToday, List<DeedVO> deeds, List<JuiceVO> juices,
                           String line) {
    }

    /** 回音壁总览：写接口全部原样返回这份聚合，前端整体替换。 */
    public record EchoVO(String day, List<DeedVO> deeds, List<DeedVO> partnerDeeds, List<JuiceVO> juices,
                         RefillVO refill, List<BatteryVO> battery) {
    }

    /** 回音壁总览。 */
    public EchoVO vault(String me) {
        CoupleSpace space = requireSpace(me);
        return build(space, me, LocalDate.now(), null);
    }

    // ========== 好事簿 ==========

    /** 记一件「TA 为我做的事」（同日同人同内容重复 400；新增推双方，并给被记的那位记一笔分）。 */
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
        // 写的人是「被照顾的那个」，分要给做事的那个人
        earn(space, space.partnerOf(me), DEED_REASON_PREFIX + text, DEED_POINTS);
        push.pushCoupleEventBoth("echo-deed-added", me, space.getUserA(), space.getUserB(),
                CoupleEchoBank.deedAddedLine(text));
        return build(space, me, now, null);
    }

    /** 记录人本人给证据点「这条救过我」（幂等；TA 的记录只能 TA 自己点；同归于被记的那位 +1）。 */
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
            return build(space, me, now, null);
        }
        row.setStarred(1);
        row.setUpdatedAt(System.currentTimeMillis());
        deedMapper.updateById(row);
        earn(space, space.partnerOf(row.getFromUser()), DEED_STAR_REASON_PREFIX + row.getContent(),
                DEED_STAR_POINTS);
        push.pushCoupleEventBoth("echo-deed-starred", me, space.getUserA(), space.getUserB(),
                CoupleEchoBank.deedStarredLine(row.getContent()));
        return build(space, me, now, null);
    }

    // ========== 鼓励语罐 ==========

    /** 往自己罐里塞一张鼓励语（≤5 条，第 6 条 400；槽位复用删掉的空格）。 */
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
        return build(space, me, now, null);
    }

    /** 删掉自己罐里的一张（只能删本人的）。 */
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
        return build(space, me, now, null);
    }

    // ========== 能量补给 ==========

    /** 领今天的能量补给（每人每天一次；随机翻自己的证据 + 双方各一张鼓励语；推双方）。 */
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
        return build(space, me, now, pack);
    }

    /** 拆补给包：我的证据随机 ≤3 条 + 双方各自的鼓励语各 1 条。 */
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
        return new RefillVO(true, refillLogMapper.findByDayUser(space.getId(), partner, day) != null,
                deeds, juices, CoupleEchoBank.refillLine(seed));
    }

    // ========== 电量预报 ==========

    /** 报今天的电量（1-5 钳制，null 按默认格；本人当天可改写）。 */
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
        return build(space, me, now, null);
    }

    // ========== 聚合 ==========

    private EchoVO build(CoupleSpace space, String me, LocalDate now, RefillVO refillOverride) {
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
                List.of(), List.of(), "");
        List<BatteryVO> battery = batteryMapper.findByDay(space.getId(), day).stream()
                .map(b -> toBattery(b, me, space, day)).toList();

        return new EchoVO(day, deeds, partnerDeeds, juices, refill, battery);
    }

    // ========== 小件 ==========

    /** 记一笔赚分。归属人由调用点决定，重复计分的闸门在各写方法的早退里。 */
    private void earn(CoupleSpace space, String user, String item, int points) {
        ledgerMapper.insert(CouplePointLedger.of(space.getId(), user,
                CouplePointLedger.TYPE_EARN, item, points));
    }

    private DeedVO toDeed(CoupleEchoDeed d, String me) {
        return new DeedVO(d.getId(), d.getFromUser(), d.getFromUser().equals(me),
                nz(d.getContent()), nz(d.getDay()), d.starredFlag(), d.getCreated() == null ? 0 : d.getCreated());
    }

    private JuiceVO toJuice(CoupleEchoJuice j, String me) {
        return new JuiceVO(j.getId(), j.getFromUser(), j.getFromUser().equals(me),
                j.getIdx() == null ? 0 : j.getIdx(), nz(j.getContent()),
                j.getCreated() == null ? 0 : j.getCreated());
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

    private <T> T pickByHash(List<T> rows, String seedKey) {
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(Math.floorMod(CoupleRitualBank.stableHash(seedKey), rows.size()));
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
