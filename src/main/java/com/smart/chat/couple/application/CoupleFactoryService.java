package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.chore.SpinTask;
import com.smart.chat.couple.domain.chore.SpinTaskRepository;
import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.infrastructure.content.CoupleFactoryBank;
import com.smart.chat.couple.infrastructure.content.CoupleRitualBank;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 家务轮盘（原 F270，系统裁剪后二人制造厂唯一保留项）。
 * 情绪价值设计：家务最怕「谁觉得自己该干」——一转定分工，对方认账，干完打勾。
 * 本模块同时是积分体系的第二个赚分入口：干活的人拿分，全清双方都拿分。
 * <p>
 * 这里是编排器：取空间 → 让 {@link SpinTask} 自己守「一周一转／双签」→ 落端口 → 记台账 → 推 WS → 投 VO。
 */
@Service
public class CoupleFactoryService {

    /** 欠账栏回看的周数。 */
    static final int SPIN_OWE_LOOKBACK = 3;

    /** 积分口径：天选之人干完自己那格 +3，本周全部清空双方各 +2。 */
    static final int SPIN_DONE_POINTS = 3;
    static final int SPIN_CLEAR_POINTS = 2;
    static final String SPIN_DONE_PREFIX = "家务轮盘干完：";
    static final String SPIN_CLEAR_REASON = "家务轮盘本周全清";

    private final CoupleSpaceRepository spaceRepository;
    private final SpinTaskRepository spinRepository;
    private final PointLedgerRepository ledgerRepository;
    private final CoupleEventPublisher push;

    public CoupleFactoryService(CoupleSpaceRepository spaceRepository, SpinTaskRepository spinRepository,
                                PointLedgerRepository ledgerRepository, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.spinRepository = spinRepository;
        this.ledgerRepository = ledgerRepository;
        this.push = push;
    }

    public record SpinVO(String id, String week, String item, String assignedUser, boolean mine,
                         boolean confirmed, boolean done) {
    }

    public record BoardVO(String day, String week, List<SpinVO> spins, List<String> owed, String spinLine) {
    }

    /** 本周轮盘 + 前几周没干完的欠账。 */
    public BoardVO board(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String week = monday(now);

        List<SpinVO> spins = spinRepository.findByWeek(space.id(), week).stream()
                .map(t -> toSpinVO(t, me))
                .toList();
        List<String> owed = new ArrayList<>();
        for (int i = 1; i <= SPIN_OWE_LOOKBACK; i++) {
            spinRepository.findByWeek(space.id(), monday(now.minusWeeks(i))).stream()
                    .filter(SpinTask::owed)
                    .forEach(t -> owed.add(t.week() + " · " + t.item() + "（" + t.assignedUser() + "）"));
        }
        String spinLine = CoupleFactoryBank.spinOpenLine(
                CoupleRitualBank.stableHash(space.id() + "|spin|" + week));
        return new BoardVO(now.toString(), week, spins, owed, spinLine);
    }

    /** 一转定分工：逗号分隔事项 ≤8 条，按 hash 交替分配，一周一转。 */
    public BoardVO spin(String me, String items) {
        CoupleSpace space = requireSpace(me);
        String week = monday(LocalDate.now());
        long dice = CoupleRitualBank.stableHash(space.id() + "|spin|" + week);
        List<SpinTask> drawn = rule(() -> SpinTask.draw(space.id(), week, items, space.userA(), space.userB(),
                dice, spinRepository.alreadySpun(space.id(), week)));
        drawn.forEach(spinRepository::save);
        push.pushCoupleEventBoth("factory-spin-open", me, space.userA(), space.userB(),
                CoupleFactoryBank.spinOpenLine(dice));
        return board(me);
    }

    /** 对方给天选之人的任务认账（双签生效）。 */
    public BoardVO confirmSpin(String me, String id) {
        CoupleSpace space = requireSpace(me);
        SpinTask task = requireSpin(space, id);
        // 「自己的活自己认」与「已经认过了」都在 SpinTask.confirmBy 里：前者抛原话，后者返回 false 走早退
        if (!rule(() -> task.confirmBy(me))) {
            return board(me);
        }
        spinRepository.save(task);
        push.pushCoupleEvent("factory-spin-confirm", me, task.assignedUser(),
                "「" + task.item() + "」对方认账了，就等你干完 ✍️");
        return board(me);
    }

    /** 天选之人干完打勾并拿分；本周全干完推 both 清空卡，双方各记一笔。 */
    public BoardVO doneSpin(String me, String id) {
        CoupleSpace space = requireSpace(me);
        SpinTask task = requireSpin(space, id);
        // 归属闸门、双签前置、重复打勾幂等，全在 SpinTask.markDoneBy
        if (!rule(() -> task.markDoneBy(me))) {
            return board(me);
        }
        spinRepository.save(task);
        earn(space, me, SPIN_DONE_PREFIX + task.item(), SPIN_DONE_POINTS);
        if (SpinTask.weekCleared(spinRepository.findByWeek(space.id(), task.week()))) {
            earn(space, space.userA(), SPIN_CLEAR_REASON, SPIN_CLEAR_POINTS);
            earn(space, space.userB(), SPIN_CLEAR_REASON, SPIN_CLEAR_POINTS);
            push.pushCoupleEventBoth("factory-spin-clear", me, space.userA(), space.userB(),
                    "本周家务全部干完，车间熄灯放假 🎉");
        } else {
            push.pushCoupleEvent("factory-spin-item-done", me, space.partnerOf(me),
                    "TA 把「" + task.item() + "」干完了，今日份靠谱 +1");
        }
        return board(me);
    }

    // ========== 小件 ==========

    /** 记一笔赚分。积分只有这一个入口进本模块，重复调用点由上面的幂等早退挡住。 */
    private void earn(CoupleSpace space, String user, String item, int points) {
        ledgerRepository.append(PointEntry.earn(space.id(), user, item, points));
    }

    private String monday(LocalDate d) {
        return d.with(DayOfWeek.MONDAY).toString();
    }

    private SpinTask requireSpin(CoupleSpace space, String id) {
        return spinRepository.findByIdIn(id, space.id())
                .orElseThrow(() -> new BusinessException(400, "这条任务不存在"));
    }

    private SpinVO toSpinVO(SpinTask task, String me) {
        return new SpinVO(task.id(), task.week(), task.item(), task.assignedUser(), task.assignedTo(me),
                task.confirmed(), task.done());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
