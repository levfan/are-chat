package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 共同生活：甜蜜记账本、家务轮值、约会规划、双人习惯养成、暗号小本本。
 * 情绪价值设计：把「一起过日子」具象化——钱算得清（不伤感情）、家务有人认领、
 * 约会有日期、习惯互相监督、暗号只有彼此懂。
 */
@Service
public class CoupleLifeService {

    // ========== VO ==========

    public record ExpenseVO(String id, String username, long amount, String category, String note,
                            String spentDay, Long created) {
    }

    /** 月度账单汇总：双方支出对比 + AA 差额提示。 */
    public record ExpenseMonthVO(String month, List<ExpenseVO> expenses, long mineTotal, long partnerTotal,
                                 long total, Long diff, String tip) {
    }

    public record ChoreVO(String id, String title, String rotate, String turn, boolean myTurn,
                          int doneCount, String lastDoneDay, String lastDoneBy, Long created) {
    }

    public record DatePlanVO(String id, String title, String planDay, String place, String items,
                             String status, Long doneAt, String createdBy, Long created) {
    }

    /** 双人习惯：今日打卡状态 + 双人连续天数。 */
    public record HabitVO(String id, String title, boolean myToday, boolean partnerToday,
                          int bothStreak, int totalDays, String createdBy, Long created) {
    }

    public record CipherVO(String id, String keyword, String meaning, String createdBy, Long created) {
    }

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleExpenseMapper expenseMapper;
    private final CoupleChoreMapper choreMapper;
    private final CoupleDatePlanMapper datePlanMapper;
    private final CoupleHabitMapper habitMapper;
    private final CoupleHabitLogMapper habitLogMapper;
    private final CoupleCipherMapper cipherMapper;
    private final ImPushService push;

    @SuppressWarnings("java:S107")
    public CoupleLifeService(CoupleSpaceMapper spaceMapper, CoupleExpenseMapper expenseMapper,
                             CoupleChoreMapper choreMapper, CoupleDatePlanMapper datePlanMapper,
                             CoupleHabitMapper habitMapper, CoupleHabitLogMapper habitLogMapper,
                             CoupleCipherMapper cipherMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.expenseMapper = expenseMapper;
        this.choreMapper = choreMapper;
        this.datePlanMapper = datePlanMapper;
        this.habitMapper = habitMapper;
        this.habitLogMapper = habitLogMapper;
        this.cipherMapper = cipherMapper;
        this.push = push;
    }

    // ========== F21 甜蜜记账本 ==========

    /** 记一笔开销（amount 单位：分）。 */
    public ExpenseVO addExpense(String me, long amount, String category, String note, String spentDay) {
        CoupleSpace space = requireSpace(me);
        if (amount <= 0 || amount > 100_000_000L) {
            throw new BusinessException(400, "金额要在 0.01 元 - 100 万元之间");
        }
        if (!CoupleExpense.isValidCategory(category)) {
            throw new BusinessException(400, "分类不认识哦");
        }
        String memo = requireOptional(note, "备注最多 100 字", 100);
        String day = spentDay == null || spentDay.isBlank()
                ? LocalDate.now().toString() : normalizeDay(spentDay, "日期格式应为 yyyy-MM-dd");
        CoupleExpense row = CoupleExpense.of(space.getId(), me, amount, category, memo, day);
        expenseMapper.insert(row);
        return toExpenseVO(row);
    }

    /** 某月账单（yyyy-MM，默认当月）：明细 + 双方合计 + AA 差额提示。 */
    public ExpenseMonthVO monthExpenses(String me, String month) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String target = month == null || month.isBlank()
                ? LocalDate.now().toString().substring(0, 7) : month.trim();
        if (!target.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException(400, "月份格式应为 yyyy-MM");
        }
        List<ExpenseVO> expenses = expenseMapper.findByMonth(space.getId(), target).stream()
                .map(CoupleLifeService::toExpenseVO)
                .toList();
        long mine = expenses.stream().filter(e -> e.username().equals(me)).mapToLong(ExpenseVO::amount).sum();
        long theirs = expenses.stream().filter(e -> e.username().equals(partner)).mapToLong(ExpenseVO::amount).sum();
        long total = mine + theirs;
        Long diff = total == 0 ? null : Math.abs(mine - theirs);
        String tip;
        if (total == 0) {
            tip = "这个月还没有账单，一起花的钱记得记一笔～";
        } else if (mine == theirs) {
            tip = "这个月双方支出一模一样，天生的默契 💰";
        } else {
            String payer = mine > theirs ? "我" : "TA";
            tip = "这个月" + payer + "付得多一些，差额 " + yuan(diff) + " 元，下个月记得补一补 😊";
        }
        return new ExpenseMonthVO(target, expenses, mine, theirs, total, diff, tip);
    }

    /** 删除一笔账单（双方都可，只是记账）。 */
    public void deleteExpense(String me, String expenseId) {
        CoupleSpace space = requireSpace(me);
        CoupleExpense row = expenseMapper.selectById(expenseId);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这笔账单不存在");
        }
        expenseMapper.deleteById(row.getId());
    }

    // ========== F22 家务轮值 ==========

    /** 添加家务（SINGLE 固定给我 / ALTERNATE 从我开始轮换）。 */
    public ChoreVO addChore(String me, String title, String rotate) {
        CoupleSpace space = requireSpace(me);
        String name = requireText(title, "写下家务名（1-60 字）", CoupleChore.TITLE_MAX);
        String mode = CoupleChore.ROTATE_SINGLE.equals(rotate) ? CoupleChore.ROTATE_SINGLE
                : CoupleChore.ROTATE_ALTERNATE;
        String turn = CoupleChore.ROTATE_SINGLE.equals(mode) ? me : me;
        CoupleChore row = CoupleChore.of(space.getId(), name, mode, turn);
        choreMapper.insert(row);
        push.pushCoupleEvent("chore-added", me, space.partnerOf(me),
                "TA 添加了家务「" + name + "」，当前值日生：" + (row.getTurn().equals(me) ? "TA 自己" : "你"));
        return toChoreVO(row, me);
    }

    /** 家务列表（新→旧）。 */
    public List<ChoreVO> listChores(String me) {
        CoupleSpace space = requireSpace(me);
        return choreMapper.findBySpace(space.getId()).stream()
                .map(c -> toChoreVO(c, me))
                .toList();
    }

    /** 完成打卡：值日生本人打卡，ALTERNATE 模式自动轮换给对方。 */
    public ChoreVO doneChore(String me, String choreId) {
        CoupleSpace space = requireSpace(me);
        CoupleChore chore = requireChore(choreId, space);
        if (!chore.getTurn().equals(me)) {
            throw new BusinessException(403, "今天不是你的值日哦，是 " + displayOf(chore.getTurn(), me, space));
        }
        chore.setDoneCount(chore.getDoneCount() + 1);
        chore.setLastDoneDay(LocalDate.now().toString());
        chore.setLastDoneBy(me);
        chore.setUpdatedAt(System.currentTimeMillis());
        if (CoupleChore.ROTATE_ALTERNATE.equals(chore.getRotate())) {
            chore.setTurn(space.partnerOf(me));
        }
        choreMapper.updateById(chore);
        push.pushCoupleEvent("chore-done", me, space.partnerOf(me),
                "TA 完成了家务「" + chore.getTitle() + "」✅ 已经做了 " + chore.getDoneCount() + " 次，表扬！");
        return toChoreVO(chore, me);
    }

    /** 删除家务。 */
    public void deleteChore(String me, String choreId) {
        CoupleSpace space = requireSpace(me);
        CoupleChore chore = requireChore(choreId, space);
        choreMapper.deleteById(chore.getId());
    }

    // ========== F23 约会规划 ==========

    /** 计划一场约会。 */
    public DatePlanVO addDatePlan(String me, String title, String planDay, String place, String items) {
        CoupleSpace space = requireSpace(me);
        String name = requireText(title, "写下约会主题（1-60 字）", CoupleDatePlan.TITLE_MAX);
        String day = normalizeDay(planDay, "约会日期格式应为 yyyy-MM-dd");
        String where = requireOptional(place, "地点最多 100 字", CoupleDatePlan.PLACE_MAX);
        String list = requireOptional(items, "想做的事最多 500 字", CoupleDatePlan.ITEMS_MAX);
        CoupleDatePlan row = CoupleDatePlan.of(space.getId(), me, name, day, where, list);
        datePlanMapper.insert(row);
        push.pushCoupleEvent("date-plan-added", me, space.partnerOf(me),
                "TA 计划了一场约会：「" + name + "」（" + day + "）📝 快去看看想做什么！");
        return toDatePlanVO(row);
    }

    /** 约会列表：计划中在前，已完成归档在后。 */
    public List<DatePlanVO> listDatePlans(String me) {
        CoupleSpace space = requireSpace(me);
        return datePlanMapper.findBySpace(space.getId()).stream()
                .sorted((a, b) -> {
                    boolean aDone = CoupleDatePlan.STATUS_DONE.equals(a.getStatus());
                    boolean bDone = CoupleDatePlan.STATUS_DONE.equals(b.getStatus());
                    if (aDone != bDone) {
                        return aDone ? 1 : -1;
                    }
                    return a.getPlanDay().compareTo(b.getPlanDay());
                })
                .map(CoupleLifeService::toDatePlanVO)
                .toList();
    }

    /** 标记约会完成/恢复计划（完成会推送给双方）。 */
    public DatePlanVO doneDatePlan(String me, String planId, boolean done) {
        CoupleSpace space = requireSpace(me);
        CoupleDatePlan plan = requireDatePlan(planId, space);
        plan.setStatus(done ? CoupleDatePlan.STATUS_DONE : CoupleDatePlan.STATUS_PLANNED);
        plan.setDoneAt(done ? System.currentTimeMillis() : null);
        datePlanMapper.updateById(plan);
        if (done) {
            push.pushCoupleEventBoth("date-plan-done", me, space.getUserA(), space.getUserB(),
                    "约会「" + plan.getTitle() + "」圆满结束 💕 已存进你们的回忆！");
        }
        return toDatePlanVO(plan);
    }

    /** 删除约会。 */
    public void deleteDatePlan(String me, String planId) {
        CoupleSpace space = requireSpace(me);
        CoupleDatePlan plan = requireDatePlan(planId, space);
        datePlanMapper.deleteById(plan.getId());
    }

    // ========== F24 双人习惯养成 ==========

    /** 创建共同习惯（进行中）。 */
    public HabitVO addHabit(String me, String title) {
        CoupleSpace space = requireSpace(me);
        String name = requireText(title, "写下习惯名（1-60 字）", CoupleHabit.TITLE_MAX);
        CoupleHabit row = CoupleHabit.of(space.getId(), me, name);
        habitMapper.insert(row);
        push.pushCoupleEvent("habit-added", me, space.partnerOf(me),
                "TA 发起了一起坚持的习惯：「" + name + "」💪 每天记得来打卡！");
        return habitVO(row, space, me);
    }

    /** 习惯列表（含今日打卡状态与双人连续天数）。 */
    public List<HabitVO> listHabits(String me) {
        CoupleSpace space = requireSpace(me);
        return habitMapper.findBySpace(space.getId()).stream()
                .map(h -> habitVO(h, space, me))
                .toList();
    }

    /** 今日打卡（幂等：重复打卡直接返回）。 */
    public HabitVO checkinHabit(String me, String habitId) {
        CoupleSpace space = requireSpace(me);
        CoupleHabit habit = requireHabit(habitId, space);
        if (!habit.activeFlag()) {
            throw new BusinessException(409, "这个习惯已经结束啦");
        }
        String today = LocalDate.now().toString();
        if (habitLogMapper.find(habit.getId(), me, today) == null) {
            try {
                habitLogMapper.insert(CoupleHabitLog.of(habit.getId(), me, today));
            } catch (Exception e) {
                // 唯一键兜底：并发重复打卡视为幂等
            }
        }
        boolean bothToday = habitLogMapper.find(habit.getId(), space.partnerOf(me), today) != null;
        if (bothToday) {
            push.pushCoupleEventBoth("habit-both-done", me, space.getUserA(), space.getUserB(),
                    "「" + habit.getTitle() + "」今天两个人都打卡啦 ✅ 共同坚持第 "
                            + habitVO(habit, space, me).bothStreak() + " 天，继续保持！");
        } else {
            push.pushCoupleEvent("habit-checkin", me, space.partnerOf(me),
                    "TA 完成了今天「" + habit.getTitle() + "」的打卡 ✅ 就等你啦！");
        }
        return habitVO(habit, space, me);
    }

    /** 结束/重启习惯。 */
    public HabitVO toggleHabit(String me, String habitId, boolean active) {
        CoupleSpace space = requireSpace(me);
        CoupleHabit habit = requireHabit(habitId, space);
        habit.setActive(active ? 1 : 0);
        habitMapper.updateById(habit);
        return habitVO(habit, space, me);
    }

    /** 删除习惯（连同打卡日志）。 */
    public void deleteHabit(String me, String habitId) {
        CoupleSpace space = requireSpace(me);
        CoupleHabit habit = requireHabit(habitId, space);
        habitLogMapper.delete(new LambdaQueryWrapper<CoupleHabitLog>()
                .eq(CoupleHabitLog::getHabitId, habit.getId()));
        habitMapper.deleteById(habit.getId());
    }

    // ========== F25 暗号小本本 ==========

    /** 记一条暗号。 */
    public CipherVO addCipher(String me, String keyword, String meaning) {
        CoupleSpace space = requireSpace(me);
        String word = requireText(keyword, "暗号词 1-40 字", CoupleCipher.KEYWORD_MAX);
        String sense = requireText(meaning, "它的意思 1-200 字", CoupleCipher.MEANING_MAX);
        CoupleCipher row = CoupleCipher.of(space.getId(), me, word, sense);
        cipherMapper.insert(row);
        push.pushCoupleEvent("cipher-added", me, space.partnerOf(me),
                "TA 记了一条新暗号：「" + word + "」🔑 快去小本本看看什么意思");
        return toCipherVO(row);
    }

    /** 暗号列表（新→旧）。 */
    public List<CipherVO> listCiphers(String me) {
        CoupleSpace space = requireSpace(me);
        return cipherMapper.findBySpace(space.getId()).stream()
                .map(CoupleLifeService::toCipherVO)
                .toList();
    }

    /** 删除暗号。 */
    public void deleteCipher(String me, String cipherId) {
        CoupleSpace space = requireSpace(me);
        CoupleCipher row = cipherMapper.selectById(cipherId);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这条暗号不存在");
        }
        cipherMapper.deleteById(row.getId());
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private CoupleChore requireChore(String id, CoupleSpace space) {
        CoupleChore chore = choreMapper.selectById(id);
        if (chore == null || !chore.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这条家务不存在");
        }
        return chore;
    }

    private CoupleDatePlan requireDatePlan(String id, CoupleSpace space) {
        CoupleDatePlan plan = datePlanMapper.selectById(id);
        if (plan == null || !plan.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这场约会不存在");
        }
        return plan;
    }

    private CoupleHabit requireHabit(String id, CoupleSpace space) {
        CoupleHabit habit = habitMapper.selectById(id);
        if (habit == null || !habit.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这个习惯不存在");
        }
        return habit;
    }

    /** 双人连续打卡天数：从今天（不足则从昨天）往前数，双方都打过才连续。 */
    private int bothStreak(CoupleSpace space, String habitId) {
        String userA = space.getUserA();
        String userB = space.getUserB();
        LocalDate cursor = LocalDate.now();
        // 今天还没齐不算断，从昨天开始数
        boolean todayBoth = habitLogMapper.find(habitId, userA, cursor.toString()) != null
                && habitLogMapper.find(habitId, userB, cursor.toString()) != null;
        if (!todayBoth) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        for (int i = 0; i < 3650; i++) {
            boolean both = habitLogMapper.find(habitId, userA, cursor.toString()) != null
                    && habitLogMapper.find(habitId, userB, cursor.toString()) != null;
            if (!both) {
                break;
            }
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private HabitVO habitVO(CoupleHabit habit, CoupleSpace space, String me) {
        String today = LocalDate.now().toString();
        boolean myToday = habitLogMapper.find(habit.getId(), me, today) != null;
        boolean partnerToday = habitLogMapper.find(habit.getId(), space.partnerOf(me), today) != null;
        List<CoupleHabitLog> logs = habitLogMapper.findByHabit(habit.getId());
        Set<String> days = new java.util.HashSet<>();
        logs.forEach(l -> days.add(l.getLogDay()));
        return new HabitVO(habit.getId(), habit.getTitle(), myToday, partnerToday,
                bothStreak(space, habit.getId()), days.size(), habit.getCreatedBy(), habit.getCreated());
    }

    private String displayOf(String username, String me, CoupleSpace space) {
        return username.equals(me) ? "我" : space.partnerOf(me).equals(username) ? "TA" : username;
    }

    private static ExpenseVO toExpenseVO(CoupleExpense row) {
        return new ExpenseVO(row.getId(), row.getUsername(), row.getAmount(), row.getCategory(),
                row.getNote() == null ? "" : row.getNote(), row.getSpentDay(), row.getCreated());
    }

    private ChoreVO toChoreVO(CoupleChore chore, String me) {
        return new ChoreVO(chore.getId(), chore.getTitle(), chore.getRotate(), chore.getTurn(),
                chore.getTurn().equals(me), chore.getDoneCount(), chore.getLastDoneDay(),
                chore.getLastDoneBy(), chore.getCreated());
    }

    private static DatePlanVO toDatePlanVO(CoupleDatePlan plan) {
        return new DatePlanVO(plan.getId(), plan.getTitle(), plan.getPlanDay(),
                plan.getPlace() == null ? "" : plan.getPlace(),
                plan.getItems() == null ? "" : plan.getItems(),
                plan.getStatus(), plan.getDoneAt(), plan.getCreatedBy(), plan.getCreated());
    }

    private static CipherVO toCipherVO(CoupleCipher row) {
        return new CipherVO(row.getId(), row.getKeyword(), row.getMeaning(), row.getCreatedBy(), row.getCreated());
    }

    private String yuan(long fen) {
        return String.valueOf(fen / 100.0);
    }

    private String requireText(String value, String message, int max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > max) {
            throw new BusinessException(400, message);
        }
        return text;
    }

    private String requireOptional(String value, String message, int max) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > max) {
            throw new BusinessException(400, message);
        }
        return text;
    }

    private String normalizeDay(String value, String message) {
        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (Exception e) {
            throw new BusinessException(400, message);
        }
    }
}
