package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleQuestBank;
import com.smart.chat.couple.domain.quest.QuestOvertime;
import com.smart.chat.couple.domain.quest.QuestOvertimeRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

/**
 * 加班预报与留灯（保留卡 `couple-quest-overtime`，原 F372）：
 * 说一句今晚要忙到几点，灯卡只有对方能留——「到家灯给你留着」这句话不该由自己说。
 *
 * 系统裁剪：关卡预告、出关战报、生病陪护单、考试周静音舱、搬家互助、新家第一晚、
 * 低谷通行证、小胜利账本、关口预约全部下线。
 *
 * DDD 收口：小时钳制、说明上限与「灯只能对方留」沉到 {@link QuestOvertime}，取数经 {@link QuestOvertimeRepository}；
 * 本类只编排、投影 VO、推 WS。判定经 {@link DomainRules} 翻译，文案原样。
 */
import static com.smart.chat.couple.application.DomainRules.rule;
@Service
public class CoupleQuestService {

    private final CoupleSpaceRepository spaceRepository;
    private final QuestOvertimeRepository overtimeRepository;
    private final CoupleEventPublisher push;

    public CoupleQuestService(CoupleSpaceRepository spaceRepository, QuestOvertimeRepository overtimeRepository,
                              CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.overtimeRepository = overtimeRepository;
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
        Optional<QuestOvertime> existing = overtimeRepository.findBySpaceAndUserAndDay(space.id(), me, day);
        QuestOvertime overtime = rule(() -> existing.isPresent()
                ? existing.get().reforecast(untilHour, note)
                : QuestOvertime.forecast(space.id(), day, me, untilHour, note));
        overtimeRepository.save(overtime);
        push.pushCoupleEvent("quest-overtime", me, space.partnerOf(me),
                CoupleQuestBank.overtimeLine(overtime.untilHour(), overtime.note()));
        return build(space, me, now);
    }

    /** 给对方留一张「到家灯给你留着」卡（只有对方能留，且 TA 今晚确实预报了加班）。 */
    public QuestVO leaveLamp(String me, String id, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        QuestOvertime overtime = rule(() -> requireOvertime(space, id).leaveLampBy(me, text));
        overtimeRepository.save(overtime);
        push.pushCoupleEvent("quest-lamp", me, space.partnerOf(me), CoupleQuestBank.lampLine(overtime.lamp()));
        return build(space, me, now);
    }

    // ========== 聚合 ==========

    private QuestVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        OvertimeVO myOvertime = null;
        OvertimeVO partnerOvertime = null;
        for (QuestOvertime o : overtimeRepository.listByDay(space.id(), day)) {
            OvertimeVO vo = new OvertimeVO(o.id(), o.untilHour(), nz(o.note()),
                    me.equals(o.fromUser()), nz(o.lamp()), nz(o.lampBy()));
            if (me.equals(o.fromUser())) {
                myOvertime = vo;
            } else {
                partnerOvertime = vo;
            }
        }
        // 灯只有对方能留：TA 今晚预报了加班、且还没人留过灯，这边才亮着按钮
        boolean canLeaveLamp = partnerOvertime != null && partnerOvertime.lampBy().isEmpty();
        return new QuestVO(day, myOvertime, partnerOvertime, canLeaveLamp);
    }

    // ========== 取行与校验 ==========

    private QuestOvertime requireOvertime(CoupleSpace space, String id) {
        return overtimeRepository.findByIdInSpace(space.id(), id)
                .orElseThrow(() -> new BusinessException(404, "找不到今晚那条加班预报 💡"));
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
