package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * 人生关卡（批次三十三 F370-F379）：关卡预告、出关战报、加班预报、生病陪护单、考试周静音舱、
 * 搬家互助、低谷通行证、小胜利账本、关卡成就墙、下次关卡预约。
 * 情绪命题：「你的人生大事，我不缺席」要能落到系统里，而不是靠记性和嘴。
 * 口径：所有写接口原样返回整份 QuestVO 聚合（GET /board 的形状），前端整体替换；
 * 双人列（新家第一晚）沿用 userA/userB 口径；日期一律 yyyy-MM-dd，CSV 打卡位用 MMdd。
 */
@Service
public class CoupleQuestService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleQuestOvertimeMapper overtimeMapper;
    private final CoupleQuestNurseMapper nurseMapper;
    private final CoupleQuestCareMarkMapper careMarkMapper;
    private final ImPushService push;

    private static final int LIST_NURSE = 6;

    private static final DateTimeFormatter MMDD = DateTimeFormatter.ofPattern("MMdd", Locale.ROOT);

    public CoupleQuestService(CoupleSpaceMapper spaceMapper, CoupleQuestOvertimeMapper overtimeMapper, CoupleQuestNurseMapper nurseMapper, CoupleQuestCareMarkMapper careMarkMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.overtimeMapper = overtimeMapper;
        this.nurseMapper = nurseMapper;
        this.careMarkMapper = careMarkMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** F372 今晚的加班预报（含对方留的灯）。 */
    public record OvertimeVO(String id, int untilHour, String note, boolean mine, String lamp, String lampBy) {
    }

    /** F373 一次代记打卡。 */
    public record CareMarkVO(String day, String kind, String kindLabel, String byUser, boolean mine) {
    }

    /** F373 陪护单（含代记统计）。 */
    public record NurseVO(String id, String patientUser, String carerUser, boolean mineAsCarer, boolean open,
                          String openDay, String closeDay, String symptom, String message,
                          int waterCount, int medCount, long days, List<CareMarkVO> marks) {
    }

    /** 关卡总览：所有写接口都原样返回这份。 */
    public record QuestVO(String day, String weekStart,
                          OvertimeVO myOvertime, OvertimeVO partnerOvertime, boolean canLeaveLamp,
                          NurseVO myNurse, NurseVO partnerNurse, List<NurseVO> nurses) {
    }

    // ========== 读 ==========

    /** 关卡总览（GET /board）。 */
    public QuestVO board(String me) {
        CoupleSpace space = requireSpace(me);
        return build(space, me, LocalDate.now());
    }

    // ========== F370 关卡预告 ==========

    // ========== F371 出关战报 ==========

    // ========== F372 加班预报 ==========

    /** F372 预报今晚忙到几点（每人每天一行可改写，13-23 钳制，note ≤40）。 */
    public QuestVO overtime(String me, Integer untilHour, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        int h = untilHour == null ? CoupleQuestOvertime.HOUR_DEFAULT
                : Math.max(CoupleQuestOvertime.HOUR_MIN, Math.min(CoupleQuestOvertime.HOUR_MAX, untilHour));
        String n = note == null ? "" : note.trim();
        if (n.length() > CoupleQuestOvertime.NOTE_MAX) {
            throw new BusinessException(400, "一句说明最多 " + CoupleQuestOvertime.NOTE_MAX + " 字");
        }
        CoupleQuestOvertime row = overtimeMapper.find(space.getId(), day, me);
        if (row == null) {
            row = CoupleQuestOvertime.of(space.getId(), day, me, h, n);
            overtimeMapper.insert(row);
        } else {
            row.setUntilHour(h);
            row.setNote(n);
            row.setUpdatedAt(System.currentTimeMillis());
            overtimeMapper.updateById(row);
        }
        push.pushCoupleEvent("quest-overtime", me, space.partnerOf(me),
                CoupleQuestBank.overtimeLine(h, n));
        return build(space, me, now);
    }

    /** F372 给对方留一张「到家灯给你留着」卡（只有对方能留，且 TA 今晚确实预报了加班）。 */
    public QuestVO leaveLamp(String me, String id, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestOvertime row = requireOvertime(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "灯是给加班的人留的，自己留不算 💡");
        }
        String t = trim(text, "灯下想留的那句话写一句");
        if (t.length() > CoupleQuestOvertime.LAMP_MAX) {
            throw new BusinessException(400, "灯卡最多 " + CoupleQuestOvertime.LAMP_MAX + " 字");
        }
        row.leaveLamp(me, t);
        overtimeMapper.updateById(row);
        push.pushCoupleEvent("quest-lamp", me, space.partnerOf(me), CoupleQuestBank.lampLine(t));
        return build(space, me, now);
    }

    // ========== F373 生病陪护单 ==========

    /** F373 为 TA 开陪护单（只有对方能开——生病的人自己顾不上记；在途每人 ≤1，症状 ≤60）。 */
    public QuestVO openNurse(String me, String symptom) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String s = symptom == null ? "" : symptom.trim();
        if (s.length() > CoupleQuestNurse.SYMPTOM_MAX) {
            throw new BusinessException(400, "症状一句话最多 " + CoupleQuestNurse.SYMPTOM_MAX + " 字");
        }
        String patient = space.partnerOf(me);
        if (nurseMapper.findOpen(space.getId(), patient) != null) {
            throw new BusinessException(400, "TA 的陪护单还在途，一张够了 🤒");
        }
        nurseMapper.insert(CoupleQuestNurse.of(space.getId(), patient, me, now.toString(), s));
        push.pushCoupleEvent("quest-nurse-open", me, patient, CoupleQuestBank.nurseOpenLine(s));
        return build(space, me, now);
    }

    /** F373 陪护人代记一次打卡（WATER/MED，一天每种只记一次）。 */
    public QuestVO careMark(String me, String nurseId, String kind) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestNurse nurse = requireNurse(space, nurseId);
        if (!nurse.open()) {
            throw new BusinessException(400, "这张单已经关了，不用继续记了");
        }
        if (!me.equals(nurse.getCarerUser())) {
            throw new BusinessException(400, "陪护单是 " + nz(nurse.getCarerUser()) + " 在陪，代记轮不到别人 💧");
        }
        String k = kind == null ? "" : kind.trim().toUpperCase(Locale.ROOT);
        if (!CoupleQuestCareMark.KINDS.contains(k)) {
            throw new BusinessException(400, "只能记喝水或吃药两种");
        }
        String day = now.toString();
        if (careMarkMapper.find(nurseId, day, k, me) != null) {
            throw new BusinessException(400, "今天的" + ("WATER".equals(k) ? "喝水" : "吃药") + "已经记过啦");
        }
        careMarkMapper.insert(CoupleQuestCareMark.of(space.getId(), nurseId, day, k, me));
        List<CoupleQuestCareMark> all = careMarkMapper.findByNurse(nurseId);
        push.pushCoupleEvent("quest-care-mark", me, nurse.getPatientUser(),
                CoupleQuestBank.careMarkLine("WATER".equals(k) ? "喝水" : "吃药", all.size()));
        return build(space, me, now);
    }

    /** F373 陪护人写/改病中留言（≤80 字，只有陪护人能写）。 */
    public QuestVO nurseMessage(String me, String nurseId, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestNurse nurse = requireNurse(space, nurseId);
        if (!nurse.open()) {
            throw new BusinessException(400, "单已经关了，留言就留在记录里吧");
        }
        if (!me.equals(nurse.getCarerUser())) {
            throw new BusinessException(400, "病中留言是陪护人写的 ✍️");
        }
        String t = trim(text, "留言写一句再存");
        if (t.length() > CoupleQuestNurse.MESSAGE_MAX) {
            throw new BusinessException(400, "留言最多 " + CoupleQuestNurse.MESSAGE_MAX + " 字");
        }
        nurse.setMessage(t);
        nurse.setUpdatedAt(System.currentTimeMillis());
        nurseMapper.updateById(nurse);
        push.pushCoupleEvent("quest-nurse-message", me, nurse.getPatientUser(), "💌 陪护留言：「" + t + "」");
        return build(space, me, now);
    }

    /** F373 病人自己宣布痊愈关单（陪护人不能替 TA 说好，只有双人才算关单庆典）。 */
    public QuestVO closeNurse(String me, String nurseId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestNurse nurse = requireNurse(space, nurseId);
        if (!nurse.open()) {
            return build(space, me, now);
        }
        if (!me.equals(nurse.getPatientUser())) {
            throw new BusinessException(400, "痊愈要病人自己说，陪护的人不能替 TA 宣布 🎉");
        }
        List<CoupleQuestCareMark> marks = careMarkMapper.findByNurse(nurseId);
        nurse.close(now.toString(), null);
        nurseMapper.updateById(nurse);
        long days = Math.max(1, daysBetween(nurse.getOpenDay(), now.toString()) + 1);
        int water = (int) marks.stream().filter(m -> CoupleQuestCareMark.KIND_WATER.equals(m.getKind())).count();
        int med = (int) marks.stream().filter(m -> CoupleQuestCareMark.KIND_MED.equals(m.getKind())).count();
        push.pushCoupleEventBoth("quest-nurse-close", me, space.getUserA(), space.getUserB(),
                CoupleQuestBank.nurseCloseLine(days, water, med));
        return build(space, me, now);
    }

    // ========== F374 考试周静音舱 ==========

    // ========== F375 搬家互助 ==========

    // ========== F376 低谷通行证 ==========

    // ========== F377 小胜利账本 ==========

    // ========== F379 下次关卡预约 ==========

    // ========== 聚合 ==========

    private QuestVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        String weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();

        List<CoupleQuestOvertime> todays = overtimeMapper.findByDay(space.getId(), day);
        OvertimeVO myOvertime = null;
        OvertimeVO partnerOvertime = null;
        for (CoupleQuestOvertime o : todays) {
            OvertimeVO vo = new OvertimeVO(o.getId(), o.getUntilHour() == null ? CoupleQuestOvertime.HOUR_DEFAULT
                    : o.getUntilHour(), nz(o.getNote()), me.equals(o.getFromUser()), nz(o.getLamp()), nz(o.getLampBy()));
            if (me.equals(o.getFromUser())) {
                myOvertime = vo;
            } else {
                partnerOvertime = vo;
            }
        }

        NurseVO myNurse = toNurse(nurseMapper.findOpen(space.getId(), me), me);
        NurseVO partnerNurse = toNurse(nurseMapper.findOpen(space.getId(), space.partnerOf(me)), me);
        List<NurseVO> nurses = nurseMapper.findBySpace(space.getId()).stream()
                .limit(LIST_NURSE)
                .map(n -> toNurse(n, me))
                .toList();

        return new QuestVO(day, weekStart, myOvertime, partnerOvertime,
                partnerOvertime != null && (partnerOvertime.lampBy == null || partnerOvertime.lampBy.isEmpty()),
                myNurse, partnerNurse, nurses);
    }

    private NurseVO toNurse(CoupleQuestNurse nurse, String me) {
        if (nurse == null) {
            return null;
        }
        List<CoupleQuestCareMark> marks = careMarkMapper.findByNurse(nurse.getId());
        int water = (int) marks.stream().filter(m -> CoupleQuestCareMark.KIND_WATER.equals(m.getKind())).count();
        int med = (int) marks.stream().filter(m -> CoupleQuestCareMark.KIND_MED.equals(m.getKind())).count();
        String endDay = nurse.open() ? LocalDate.now().toString() : nz(nurse.getCloseDay());
        long days = Math.max(1, daysBetween(nurse.getOpenDay(), endDay) + 1);
        List<CareMarkVO> markVos = marks.stream()
                .map(m -> new CareMarkVO(nz(m.getDay()), nz(m.getKind()),
                        CoupleQuestCareMark.KIND_WATER.equals(m.getKind()) ? "喝水" : "吃药", nz(m.getByUser()),
                        me.equals(m.getByUser())))
                .toList();
        return new NurseVO(nurse.getId(), nz(nurse.getPatientUser()), nz(nurse.getCarerUser()),
                me.equals(nurse.getCarerUser()), nurse.open(), nz(nurse.getOpenDay()), nz(nurse.getCloseDay()),
                nz(nurse.getSymptom()), nz(nurse.getMessage()), water, med, days, markVos);
    }

    // ========== 小件 ==========

    private CoupleQuestOvertime requireOvertime(CoupleSpace space, String id) {
        CoupleQuestOvertime row = id == null || id.isBlank() ? null : overtimeMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到今晚的加班预报，先让 TA 报一下 🌙");
        }
        return row;
    }

    private CoupleQuestNurse requireNurse(CoupleSpace space, String id) {
        CoupleQuestNurse row = id == null || id.isBlank() ? null : nurseMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这张陪护单 🤒");
        }
        return row;
    }

    private LocalDate parseDay(String day, String what) {
        String d = day == null ? "" : day.trim();
        if (!d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BusinessException(400, what + "写成 yyyy-MM-dd");
        }
        try {
            return LocalDate.parse(d);
        } catch (RuntimeException ex) {
            throw new BusinessException(400, what + "写成 yyyy-MM-dd");
        }
    }

    private String normalizeYear(String year, LocalDate now) {
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份写成 yyyy");
        }
        return y;
    }

    /** 两个日期串相差几天（解析不了给 0，正负都按原样给，调用处自己取绝对值）。 */
    private long daysBetween(String fromDay, String toDay) {
        try {
            return LocalDate.parse(toDay).toEpochDay() - LocalDate.parse(fromDay).toEpochDay();
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private int yearOf(String day) {
        if (day == null || day.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(day.substring(0, 4));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
