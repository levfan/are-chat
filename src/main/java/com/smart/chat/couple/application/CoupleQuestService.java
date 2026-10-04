package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleQuestBank;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestOvertime;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestOvertimeMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpace;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 加班预报与留灯（保留卡 `couple-quest-overtime`，原 F372）：
 * 说一句今晚要忙到几点，灯卡只有对方能留——「到家灯给你留着」这句话不该由自己说。
 *
 * 系统裁剪：关卡预告、出关战报、生病陪护单、考试周静音舱、搬家互助、新家第一晚、
 * 低谷通行证、小胜利账本、关口预约全部下线。
 */
@Service
public class CoupleQuestService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleQuestOvertimeMapper overtimeMapper;
    private final ImPushService push;

    public CoupleQuestService(CoupleSpaceMapper spaceMapper, CoupleQuestOvertimeMapper overtimeMapper,
                              ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.overtimeMapper = overtimeMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** 今晚的加班预报（含对方留的灯）。 */
    public record OvertimeVO(String id, int untilHour, String note, boolean mine, String lamp, String lampBy) {
    }

    /** 加班看板：所有写接口都原样返回这份。 */
    public record QuestVO(String day, OvertimeVO myOvertime, OvertimeVO partnerOvertime, boolean canLeaveLamp) {
    }

    // ========== 读 ==========

    /** 加班看板（GET /board）。 */
    public QuestVO board(String me) {
        return build(requireSpace(me), me, LocalDate.now());
    }

    // ========== 加班预报与留灯 ==========

    /** 预报今晚忙到几点（每人每天一行可改写，13-23 钳制，note ≤40）。 */
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
        push.pushCoupleEvent("quest-overtime", me, space.partnerOf(me), CoupleQuestBank.overtimeLine(h, n));
        return build(space, me, now);
    }

    /** 给对方留一张「到家灯给你留着」卡（只有对方能留，且 TA 今晚确实预报了加班）。 */
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

    // ========== 聚合 ==========

    private QuestVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        OvertimeVO myOvertime = null;
        OvertimeVO partnerOvertime = null;
        for (CoupleQuestOvertime o : overtimeMapper.findByDay(space.getId(), day)) {
            OvertimeVO vo = new OvertimeVO(o.getId(),
                    o.getUntilHour() == null ? CoupleQuestOvertime.HOUR_DEFAULT : o.getUntilHour(),
                    nz(o.getNote()), me.equals(o.getFromUser()), nz(o.getLamp()), nz(o.getLampBy()));
            if (me.equals(o.getFromUser())) {
                myOvertime = vo;
            } else {
                partnerOvertime = vo;
            }
        }
        // 灯只有对方能留：TA 今晚预报了加班、且还没人留过灯，这边才亮着按钮
        boolean canLeaveLamp = partnerOvertime != null && partnerOvertime.lampBy.isEmpty();
        return new QuestVO(day, myOvertime, partnerOvertime, canLeaveLamp);
    }

    // ========== 取行与校验 ==========

    private CoupleQuestOvertime requireOvertime(CoupleSpace space, String id) {
        CoupleQuestOvertime row = id == null || id.isBlank() ? null : overtimeMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到今晚那条加班预报 💡");
        }
        return row;
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
