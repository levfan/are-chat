package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleFactoryBank;
import com.smart.chat.couple.infrastructure.content.CoupleRitualBank;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerPO;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpinTaskPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpinTaskMapper;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 家务轮盘（原 F270，系统裁剪后二人制造厂唯一保留项）。
 * 情绪价值设计：家务最怕「谁觉得自己该干」——一转定分工，对方认账，干完打勾。
 * 本模块同时是积分体系的第二个赚分入口：干活的人拿分，全清双方都拿分。
 */
@Service
public class CoupleFactoryService {

    static final int SPIN_ITEMS_MAX = 8;
    static final int SPIN_ITEM_LEN = 40;
    /** 欠账栏回看的周数。 */
    static final int SPIN_OWE_LOOKBACK = 3;

    /** 积分口径：天选之人干完自己那格 +3，本周全部清空双方各 +2。 */
    static final int SPIN_DONE_POINTS = 3;
    static final int SPIN_CLEAR_POINTS = 2;
    static final String SPIN_DONE_PREFIX = "家务轮盘干完：";
    static final String SPIN_CLEAR_REASON = "家务轮盘本周全清";

    private final CoupleSpaceRepository spaceRepository;
    private final CoupleSpinTaskMapper spinMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final CoupleEventPublisher push;

    public CoupleFactoryService(CoupleSpaceRepository spaceRepository, CoupleSpinTaskMapper spinMapper,
                                CouplePointLedgerMapper ledgerMapper, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.spinMapper = spinMapper;
        this.ledgerMapper = ledgerMapper;
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

        List<SpinVO> spins = new ArrayList<>();
        for (CoupleSpinTaskPO t : spinMapper.findByWeek(space.id(), week)) {
            spins.add(new SpinVO(t.getId(), t.getWeek(), t.getItem(), t.getAssignedUser(),
                    t.getAssignedUser().equals(me), t.confirmedFlag(), t.doneFlag()));
        }
        List<String> owed = new ArrayList<>();
        for (int i = 1; i <= SPIN_OWE_LOOKBACK; i++) {
            for (CoupleSpinTaskPO t : spinMapper.findByWeek(space.id(), monday(now.minusWeeks(i)))) {
                if (!t.doneFlag()) {
                    owed.add(t.getWeek() + " · " + t.getItem() + "（" + t.getAssignedUser() + "）");
                }
            }
        }
        String spinLine = CoupleFactoryBank.spinOpenLine(
                CoupleRitualBank.stableHash(space.id() + "|spin|" + week));
        return new BoardVO(now.toString(), week, spins, owed, spinLine);
    }

    /** 一转定分工：逗号分隔事项 ≤8 条，按 hash 交替分配，一周一转。 */
    public BoardVO spin(String me, String items) {
        CoupleSpace space = requireSpace(me);
        String week = monday(LocalDate.now());
        if (!spinMapper.findByWeek(space.id(), week).isEmpty()) {
            throw new BusinessException(400, "本周已经转过盘了，下周再来一赌");
        }
        List<String> list = splitItems(items, SPIN_ITEMS_MAX, SPIN_ITEM_LEN);
        if (list.size() < 2) {
            throw new BusinessException(400, "至少写两件事，不然不用转");
        }
        boolean startA = Math.floorMod(CoupleRitualBank.stableHash(space.id() + "|spin|" + week), 2) == 0;
        String first = startA ? space.userA() : space.userB();
        String second = startA ? space.userB() : space.userA();
        for (int i = 0; i < list.size(); i++) {
            spinMapper.insert(CoupleSpinTaskPO.of(space.id(), week, list.get(i), i % 2 == 0 ? first : second));
        }
        push.pushCoupleEventBoth("factory-spin-open", me, space.userA(), space.userB(),
                CoupleFactoryBank.spinOpenLine(CoupleRitualBank.stableHash(space.id() + "|spin|" + week)));
        return board(me);
    }

    /** 对方给天选之人的任务认账（双签生效）。 */
    public BoardVO confirmSpin(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSpinTaskPO row = requireSpin(space, id);
        if (row.getAssignedUser().equals(me)) {
            throw new BusinessException(400, "自己的活自己认，TA 的活等 TA 认");
        }
        if (row.confirmedFlag()) {
            return board(me);
        }
        row.setConfirmed(1);
        spinMapper.updateById(row);
        push.pushCoupleEvent("factory-spin-confirm", me, row.getAssignedUser(),
                "「" + row.getItem() + "」对方认账了，就等你干完 ✍️");
        return board(me);
    }

    /** 天选之人干完打勾并拿分；本周全干完推 both 清空卡，双方各记一笔。 */
    public BoardVO doneSpin(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSpinTaskPO row = requireSpin(space, id);
        if (!row.getAssignedUser().equals(me)) {
            throw new BusinessException(400, "这活不是你的，抢功也得等下周");
        }
        if (row.doneFlag()) {
            return board(me);
        }
        if (!row.confirmedFlag()) {
            throw new BusinessException(400, "先等对方认账，干了也白干");
        }
        row.setDone(1);
        row.setDoneAt(System.currentTimeMillis());
        spinMapper.updateById(row);
        earn(space, me, SPIN_DONE_PREFIX + row.getItem(), SPIN_DONE_POINTS);
        List<CoupleSpinTaskPO> siblings = spinMapper.findByWeek(space.id(), row.getWeek());
        boolean allDone = siblings.stream().allMatch(CoupleSpinTaskPO::doneFlag);
        if (allDone) {
            earn(space, space.userA(), SPIN_CLEAR_REASON, SPIN_CLEAR_POINTS);
            earn(space, space.userB(), SPIN_CLEAR_REASON, SPIN_CLEAR_POINTS);
            push.pushCoupleEventBoth("factory-spin-clear", me, space.userA(), space.userB(),
                    "本周家务全部干完，车间熄灯放假 🎉");
        } else {
            push.pushCoupleEvent("factory-spin-item-done", me, space.partnerOf(me),
                    "TA 把「" + row.getItem() + "」干完了，今日份靠谱 +1");
        }
        return board(me);
    }

    // ========== 小件 ==========

    /** 记一笔赚分。积分只有这一个入口进本模块，重复调用点由上面的 doneFlag 早退挡住。 */
    private void earn(CoupleSpace space, String user, String item, int points) {
        ledgerMapper.insert(CouplePointLedgerPO.of(space.id(), user,
                CouplePointLedgerPO.TYPE_EARN, item, points));
    }

    private List<String> splitItems(String raw, int max, int lenEach) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            throw new BusinessException(400, "内容不能为空");
        }
        for (String s : raw.split("[,，、]")) {
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > lenEach) {
                throw new BusinessException(400, "每条最多 " + lenEach + " 字");
            }
            if (out.contains(t)) {
                throw new BusinessException(400, "有重复项");
            }
            out.add(t);
        }
        if (out.isEmpty()) {
            throw new BusinessException(400, "至少写一项");
        }
        if (out.size() > max) {
            throw new BusinessException(400, "最多 " + max + " 项，贪多干不完");
        }
        return out;
    }

    private String monday(LocalDate d) {
        return d.with(DayOfWeek.MONDAY).toString();
    }

    private CoupleSpinTaskPO requireSpin(CoupleSpace space, String id) {
        CoupleSpinTaskPO row = id == null ? null : spinMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.id())) {
            throw new BusinessException(400, "这条任务不存在");
        }
        return row;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
