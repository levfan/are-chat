package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 异地恋·时空同步（F110-F119，批次七）：隔空牵手、双城时刻卡（前端渲染）、想念计量所、
 * 见面能量瓶、我们的作息表、下次见面信、云约会清单、平安卡、见面日记、异地恋报告。
 * 情绪价值设计：让距离产生可触摸的仪式（牵手/想念/平安卡）、让见面有盼头（能量瓶/见面信）、
 * 让时间重叠可见（作息表）、让异地岁月有账可查（见面日记/异地恋报告）。
 */
@Service
public class CoupleDistanceService {

    /** 见面能量瓶充满所需天数（离上次见面）。 */
    private static final int ENERGY_CYCLE_DAYS = 30;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleHandholdMapper handholdMapper;
    private final CoupleMissDailyMapper missMapper;
    private final CoupleRoutineMapper routineMapper;
    private final CoupleReunionLetterMapper letterMapper;
    private final CoupleCloudDateMapper cloudDateMapper;
    private final CoupleSafetyPingMapper safetyMapper;
    private final CoupleReunionLogMapper reunionMapper;
    private final ImPushService push;

    public CoupleDistanceService(CoupleSpaceMapper spaceMapper, CoupleHandholdMapper handholdMapper,
                                 CoupleMissDailyMapper missMapper, CoupleRoutineMapper routineMapper,
                                 CoupleReunionLetterMapper letterMapper, CoupleCloudDateMapper cloudDateMapper,
                                 CoupleSafetyPingMapper safetyMapper, CoupleReunionLogMapper reunionMapper,
                                 ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.handholdMapper = handholdMapper;
        this.missMapper = missMapper;
        this.routineMapper = routineMapper;
        this.letterMapper = letterMapper;
        this.cloudDateMapper = cloudDateMapper;
        this.safetyMapper = safetyMapper;
        this.reunionMapper = reunionMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record HandholdVO(boolean todayMine, boolean todayPartner, boolean todayBoth,
                             long totalDays, String milestone, List<CoupleHandhold> recent) {
    }

    public record MissVO(boolean todayMine, boolean todayPartner, boolean todayBoth,
                         long bothTimes, String milestone, List<CoupleMissDaily> recent) {
    }

    public record OverlapVO(String start, String end) {
    }

    public record RoutineVO(CoupleRoutine mine, CoupleRoutine partner, List<OverlapVO> overlaps) {
    }

    public record LetterVO(String id, String fromUser, boolean mine, String status,
                           String content, Long openedAt, boolean canOpen, Long created) {
    }

    public record CloudDateVO(String id, String fromUser, String item, String status,
                              String doneNote, Long doneAt, Long created) {
    }

    public record SafetyVO(String id, String fromUser, String kind, String note, Long created) {
    }

    public record ReunionLogVO(String id, String meetDay, String note, String byUser,
                               Long intervalDays, Long created) {
    }

    public record EnergyVO(Integer daysSince, int energy, String line) {
    }

    public record DistanceReportVO(long totalDays, long meetCount, Long avgIntervalDays,
                                   long missBothDays, long handholdDays, long cloudDoneCount,
                                   long sealedLetters, String summary) {
    }

    // ========== F110 隔空牵手 ==========

    public HandholdVO handhold(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CoupleHandhold row = handholdMapper.findByDay(space.getId(), today);
        boolean meIsA = space.getUserA().equals(me);
        boolean mine = row != null && (meIsA ? row.getHoldA() == 1 : row.getHoldB() == 1);
        boolean partner = row != null && (meIsA ? row.getHoldB() == 1 : row.getHoldA() == 1);
        long total = handholdMapper.countBoth(space.getId());
        return new HandholdVO(mine, partner, row != null && row.bothHold(), total,
                CoupleDistanceBank.handholdMilestone(total), handholdMapper.findBySpace(space.getId()));
    }

    /** 点亮今天的手：双方都点亮即牵手成功。 */
    public HandholdVO holdHand(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CoupleHandhold row = handholdMapper.findByDay(space.getId(), today);
        boolean meIsA = space.getUserA().equals(me);
        if (row == null) {
            row = CoupleHandhold.of(space.getId(), today);
            if (meIsA) {
                row.setHoldA(1);
                row.setHoldAtA(System.currentTimeMillis());
            } else {
                row.setHoldB(1);
                row.setHoldAtB(System.currentTimeMillis());
            }
            handholdMapper.insert(row);
        } else if (meIsA ? row.getHoldA() != 1 : row.getHoldB() != 1) {
            if (meIsA) {
                row.setHoldA(1);
                row.setHoldAtA(System.currentTimeMillis());
            } else {
                row.setHoldB(1);
                row.setHoldAtB(System.currentTimeMillis());
            }
            handholdMapper.updateById(row);
        }
        if (row.bothHold()) {
            long total = handholdMapper.countBoth(space.getId());
            String milestone = CoupleDistanceBank.handholdMilestone(total);
            push.pushCoupleEventBoth("handhold-both", me, space.getUserA(), space.getUserB(),
                    "🤝 今天也牵手成功啦！已累计牵手 " + total + " 天" + (milestone == null ? "" : "。" + milestone));
        } else {
            push.pushCoupleEvent("handhold-lit", me, space.partnerOf(me),
                    "🤚 TA 伸出了手，快点亮你的那只，今天也要牵手！");
        }
        return handhold(me);
    }

    // ========== F112 想念计量所 ==========

    public MissVO miss(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CoupleMissDaily row = missMapper.findByDay(space.getId(), today);
        boolean meIsA = space.getUserA().equals(me);
        boolean mine = row != null && (meIsA ? row.getMissA() == 1 : row.getMissB() == 1);
        boolean partner = row != null && (meIsA ? row.getMissB() == 1 : row.getMissA() == 1);
        long both = missMapper.countBoth(space.getId());
        return new MissVO(mine, partner, row != null && row.bothMiss(), both,
                CoupleDistanceBank.missMilestone(both), missMapper.findBySpace(space.getId()));
    }

    /** 点亮「今天想你了」；同天互想 = 双向奔赴。 */
    public MissVO lightMiss(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CoupleMissDaily row = missMapper.findByDay(space.getId(), today);
        boolean meIsA = space.getUserA().equals(me);
        if (row == null) {
            row = CoupleMissDaily.of(space.getId(), today);
            if (meIsA) {
                row.setMissA(1);
                row.setMissAtA(System.currentTimeMillis());
            } else {
                row.setMissB(1);
                row.setMissAtB(System.currentTimeMillis());
            }
            missMapper.insert(row);
        } else if (meIsA ? row.getMissA() != 1 : row.getMissB() != 1) {
            if (meIsA) {
                row.setMissA(1);
                row.setMissAtA(System.currentTimeMillis());
            } else {
                row.setMissB(1);
                row.setMissAtB(System.currentTimeMillis());
            }
            missMapper.updateById(row);
        }
        if (row.bothMiss()) {
            if (row.getBothAt() == null) {
                row.setBothAt(System.currentTimeMillis());
                missMapper.updateById(row);
            }
            long both = missMapper.countBoth(space.getId());
            String milestone = CoupleDistanceBank.missMilestone(both);
            push.pushCoupleEventBoth("miss-both", me, space.getUserA(), space.getUserB(),
                    "💞 双向奔赴达成！今天你们都在想对方，第 " + both + " 次"
                            + (milestone == null ? "" : "。" + milestone));
        } else {
            push.pushCoupleEvent("miss-lit", me, space.partnerOf(me),
                    "🌙 TA 刚刚点亮了「今天想你了」——是现在，就在想你。");
        }
        return miss(me);
    }

    // ========== F114 我们的作息表 ==========

    public RoutineVO routine(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleRoutine> rows = routineMapper.findBySpace(space.getId());
        CoupleRoutine mine = rows.stream().filter(r -> r.getOwnerUser().equals(me)).findFirst().orElse(null);
        CoupleRoutine partner = rows.stream().filter(r -> !r.getOwnerUser().equals(me)).findFirst().orElse(null);
        return new RoutineVO(mine, partner, computeOverlaps(mine, partner));
    }

    /** 保存我的作息（起床/上班/下班/睡觉）。 */
    public RoutineVO saveRoutine(String me, String wakeTime, String workStart, String workEnd, String sleepTime) {
        CoupleSpace space = requireSpace(me);
        String wake = requireTime(wakeTime, "起床时间");
        String workStartT = requireTime(workStart, "上班开始");
        String workEndT = requireTime(workEnd, "下班时间");
        String sleep = requireTime(sleepTime, "睡觉时间");
        CoupleRoutine row = routineMapper.findByOwner(space.getId(), me);
        if (row == null) {
            row = CoupleRoutine.of(space.getId(), me, wake, workStartT, workEndT, sleep);
            routineMapper.insert(row);
        } else {
            row.setWakeTime(wake);
            row.setWorkStart(workStartT);
            row.setWorkEnd(workEndT);
            row.setSleepTime(sleep);
            row.setUpdatedAt(System.currentTimeMillis());
            routineMapper.updateById(row);
        }
        push.pushCoupleEvent("routine-updated", me, space.partnerOf(me),
                "⏰ TA 更新了作息表，去把我们的时间叠一叠。");
        return routine(me);
    }

    // ========== F115 下次见面信 ==========

    public List<LetterVO> reunionLetters(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleReunionLog> logs = reunionMapper.findBySpace(space.getId());
        return letterMapper.findBySpace(space.getId()).stream()
                .map(l -> {
                    boolean canOpen = hasMetSince(logs, l.getCreated());
                    boolean mine = l.getFromUser().equals(me);
                    boolean opened = CoupleReunionLetter.STATUS_OPENED.equals(l.getStatus());
                    return new LetterVO(l.getId(), l.getFromUser(), mine, l.getStatus(),
                            opened || mine ? l.getContent() : null, l.getOpenedAt(), canOpen && !opened, l.getCreated());
                })
                .toList();
    }

    private boolean hasMetSince(List<CoupleReunionLog> logs, long createdMs) {
        String createdDay = toDay(createdMs);
        return logs.stream().anyMatch(g -> g.getMeetDay().compareTo(createdDay) >= 0);
    }

    /** 写一封见面信（封存，见面打卡后可拆）。 */
    public List<LetterVO> writeLetter(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CoupleReunionLetter.CONTENT_MAX, "信写 " + CoupleReunionLetter.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "给见面时的 TA 写点什么吧 ✉️");
        }
        letterMapper.insert(CoupleReunionLetter.of(space.getId(), me, text));
        push.pushCoupleEvent("letter-sealed", me, space.partnerOf(me),
                "✉️ TA 写了一封「下次见面信」，封存好了——等见面那天一起拆。");
        return reunionLetters(me);
    }

    /** 拆信：见面打卡后任意一方可拆。 */
    public List<LetterVO> openLetter(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleReunionLetter row = requireLetter(space.getId(), id);
        if (CoupleReunionLetter.STATUS_SEELED.equals(row.getStatus())) {
            List<CoupleReunionLog> logs = reunionMapper.findBySpace(space.getId());
            if (!hasMetSince(logs, row.getCreated())) {
                throw new BusinessException(400, "还没见面呢～先在见面日记里记一笔，才能拆这封信 ✉️");
            }
            row.setStatus(CoupleReunionLetter.STATUS_OPENED);
            row.setOpenedAt(System.currentTimeMillis());
            letterMapper.updateById(row);
            push.pushCoupleEventBoth("letter-opened", me, space.getUserA(), space.getUserB(),
                    "💌 「下次见面信」被拆开啦！去看看里面写了什么。");
        }
        return reunionLetters(me);
    }

    // ========== F116 云约会清单 ==========

    public List<CloudDateVO> cloudDates(String me) {
        return cloudDateMapper.findBySpace(requireSpace(me).getId()).stream()
                .map(c -> new CloudDateVO(c.getId(), c.getFromUser(), c.getItem(), c.getStatus(),
                        c.getDoneNote(), c.getDoneAt(), c.getCreated()))
                .toList();
    }

    /** 添加云约会（item 为空时从灵感库抽一条）。 */
    public List<CloudDateVO> addCloudDate(String me, String item) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(item, CoupleCloudDate.ITEM_MAX, "云约会写 " + CoupleCloudDate.ITEM_MAX + " 字以内哦");
        if (text == null) {
            text = CoupleDistanceBank.pickCloudDate(space.getId(), (int) (cloudDateMapper.findBySpace(space.getId()).size() + 1));
        }
        cloudDateMapper.insert(CoupleCloudDate.of(space.getId(), me, text));
        push.pushCoupleEvent("cloud-added", me, space.partnerOf(me),
                "☁️ TA 加了一个云约会：「" + text + "」，找个时间一起完成呀。");
        return cloudDates(me);
    }

    /** 完成云约会打卡。 */
    public List<CloudDateVO> doneCloudDate(String me, String id, String note) {
        CoupleSpace space = requireSpace(me);
        CoupleCloudDate row = cloudDateMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个云约会哦");
        }
        if (CoupleCloudDate.STATUS_OPEN.equals(row.getStatus())) {
            row.setStatus(CoupleCloudDate.STATUS_DONE);
            row.setDoneNote(trimLimit(note, CoupleCloudDate.NOTE_MAX, null));
            row.setDoneAt(System.currentTimeMillis());
            cloudDateMapper.updateById(row);
            String tail = row.getDoneNote() == null ? "" : " TA 说：" + row.getDoneNote();
            push.pushCoupleEventBoth("cloud-done", me, space.getUserA(), space.getUserB(),
                    "☁️ 云约会完成：「" + row.getItem() + "」" + tail);
        }
        return cloudDates(me);
    }

    // ========== F117 平安卡 ==========

    public List<SafetyVO> safeties(String me) {
        return safetyMapper.findBySpace(requireSpace(me).getId()).stream()
                .map(s -> new SafetyVO(s.getId(), s.getFromUser(), s.getKind(), s.getNote(), s.getCreated()))
                .toList();
    }

    /** 一键报平安：出发/到家。 */
    public List<SafetyVO> pingSafety(String me, String kind, String note) {
        CoupleSpace space = requireSpace(me);
        String k = CoupleSafetyPing.KIND_GO_OUT.equals(kind) ? kind
                : CoupleSafetyPing.KIND_ARRIVE.equals(kind) ? kind : null;
        if (k == null) {
            throw new BusinessException(400, "平安卡只有「出发了」和「到家啦」两种哦");
        }
        safetyMapper.insert(CoupleSafetyPing.of(space.getId(), me, k,
                trimLimit(note, CoupleSafetyPing.NOTE_MAX, null)));
        push.pushCoupleEvent("safety-ping", me, space.partnerOf(me), k.equals(CoupleSafetyPing.KIND_GO_OUT)
                ? "🚕 TA 出发啦" + (note == null ? "，路上注意安全，我等你消息。" : "：" + note)
                : "🏠 TA 到家啦" + (note == null ? "，放下心啦。" : "：" + note));
        return safeties(me);
    }

    // ========== F118 见面日记 ==========

    public List<ReunionLogVO> reunions(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleReunionLog> logs = reunionMapper.findBySpace(space.getId());
        List<ReunionLogVO> result = new ArrayList<>();
        for (int i = 0; i < logs.size(); i++) {
            CoupleReunionLog log = logs.get(i);
            Long interval = null;
            if (i + 1 < logs.size()) {
                interval = ChronoUnit.DAYS.between(LocalDate.parse(logs.get(i + 1).getMeetDay()),
                        LocalDate.parse(log.getMeetDay()));
            }
            result.add(new ReunionLogVO(log.getId(), log.getMeetDay(), log.getNote(), log.getByUser(), interval, log.getCreated()));
        }
        return result;
    }

    /** 记一笔见面（同一天只记一条，可补记过去的日期）。 */
    public List<ReunionLogVO> logReunion(String me, String meetDay, String note) {
        CoupleSpace space = requireSpace(me);
        String day = requireDay(meetDay);
        if (day.compareTo(LocalDate.now().toString()) > 0) {
            throw new BusinessException(400, "见面日记只能记已经发生的日子哦");
        }
        if (reunionMapper.findBySpace(space.getId()).stream().anyMatch(g -> g.getMeetDay().equals(day))) {
            throw new BusinessException(400, "这天已经记过啦，换一天或去编辑那条吧");
        }
        reunionMapper.insert(CoupleReunionLog.of(space.getId(), me, day, trimLimit(note, CoupleReunionLog.NOTE_MAX, null)));
        long count = reunionMapper.findBySpace(space.getId()).size();
        push.pushCoupleEventBoth("reunion-logged", me, space.getUserA(), space.getUserB(),
                "📅 见面日记 +1！这是我们的第 " + count + " 次见面，能量瓶已充满归零。");
        return reunions(me);
    }

    // ========== F113 见面能量瓶（聚合见面日记） ==========

    /** 离上次见面越久能量越满（30 天充满），见面打卡自动归零。 */
    public EnergyVO energy(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleReunionLog latest = reunionMapper.findLatest(space.getId());
        if (latest == null) {
            return new EnergyVO(null, 100, "还没有见面记录——第一次见面后能量瓶开始充能 🫙");
        }
        long days = ChronoUnit.DAYS.between(LocalDate.parse(latest.getMeetDay()), LocalDate.now());
        int energy = (int) Math.min(100, days * 100 / ENERGY_CYCLE_DAYS);
        return new EnergyVO((int) days, energy, CoupleDistanceBank.energyLine(energy));
    }

    // ========== F119 异地恋报告（聚合） ==========

    public DistanceReportVO report(String me) {
        CoupleSpace space = requireSpace(me);
        long totalDays = space.getCreated() == null ? 0
                : ChronoUnit.DAYS.between(toLocalDate(space.getCreated()), LocalDate.now()) + 1;
        List<CoupleReunionLog> logs = reunionMapper.findBySpace(space.getId());
        long avg = 0;
        if (logs.size() >= 2) {
            long span = ChronoUnit.DAYS.between(LocalDate.parse(logs.get(logs.size() - 1).getMeetDay()),
                    LocalDate.parse(logs.get(0).getMeetDay()));
            avg = Math.round((double) span / (logs.size() - 1));
        }
        long sealed = letterMapper.findBySpace(space.getId()).stream()
                .filter(l -> CoupleReunionLetter.STATUS_SEELED.equals(l.getStatus())).count();
        long cloudDone = cloudDateMapper.findBySpace(space.getId()).stream()
                .filter(c -> CoupleCloudDate.STATUS_DONE.equals(c.getStatus())).count();
        long missBoth = missMapper.countBoth(space.getId());
        long handholds = handholdMapper.countBoth(space.getId());
        String summary = "你们在一起 " + totalDays + " 天，见了 " + logs.size() + " 次面，双向奔赴 " + missBoth
                + " 次、隔空牵手 " + handholds + " 天。距离从没拦住你们——每一次想念都在往见面那天走。";
        return new DistanceReportVO(totalDays, logs.size(), logs.size() >= 2 ? avg : null,
                missBoth, handholds, cloudDone, sealed, summary);
    }

    // ========== 内部工具 ==========

    private List<OverlapVO> computeOverlaps(CoupleRoutine mine, CoupleRoutine partner) {
        if (mine == null || partner == null) {
            return List.of();
        }
        List<OverlapVO> result = new ArrayList<>();
        // 空闲段 = 起床→上班、下班→睡觉；取两人空闲段的交集
        for (int[] mySeg : freeSegments(mine)) {
            for (int[] pSeg : freeSegments(partner)) {
                int start = Math.max(mySeg[0], pSeg[0]);
                int end = Math.min(mySeg[1], pSeg[1]);
                if (end - start >= 30) {
                    result.add(new OverlapVO(toHm(start), toHm(end)));
                }
            }
        }
        return result;
    }

    private List<int[]> freeSegments(CoupleRoutine r) {
        List<int[]> segs = new ArrayList<>();
        segs.add(new int[]{toMinutes(r.getWakeTime()), toMinutes(r.getWorkStart())});
        segs.add(new int[]{toMinutes(r.getWorkEnd()), toMinutes(r.getSleepTime())});
        return segs;
    }

    private int toMinutes(String hm) {
        String[] parts = hm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private String toHm(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private String requireTime(String value, String label) {
        if (value == null || !value.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            throw new BusinessException(400, label + "要按 HH:mm 填哦，比如 07:30");
        }
        return value;
    }

    private String requireDay(String value) {
        if (value == null || !value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new BusinessException(400, "日期要按 yyyy-MM-dd 填哦");
        }
        try {
            LocalDate.parse(value);
        } catch (Exception e) {
            throw new BusinessException(400, "日期格式不对哦");
        }
        return value;
    }

    private String toDay(long ms) {
        return LocalDate.ofInstant(java.time.Instant.ofEpochMilli(ms), ZoneId.systemDefault()).toString();
    }

    private LocalDate toLocalDate(long ms) {
        return LocalDate.ofInstant(java.time.Instant.ofEpochMilli(ms), ZoneId.systemDefault());
    }

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

    private CoupleReunionLetter requireLetter(String spaceId, String id) {
        CoupleReunionLetter row = letterMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这封见面信哦");
        }
        return row;
    }
}
