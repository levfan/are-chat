package com.smart.chat.couple.domain.chore;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 家务轮盘的一格：本周轮到某个人的某件家务，<b>双签才生效</b>。
 * <p>
 * 这条聚合守的是 {@code CONTEXT.md} 给家务轮盘写下的三条约束：
 * <ol>
 *   <li><b>一周一转</b>：{@link #draw} 拿到「这周已经转过」这个事实就直接拒绝（读存储本身归端口，
 *       见 {@link SpinTaskRepository#alreadySpun}，判定与话术留在这里）；</li>
 *   <li><b>交替分配</b>：事项按顺序在两个人之间轮流，谁先谁后由外部摇出的骰子决定，
 *       同一空间同一周摇两次结果一样（{@code stableHash} 口径不在这里）；</li>
 *   <li><b>双签</b>：认账只能由对方点（{@link #confirmBy}），打勾只能由天选之人在对方认账之后点
 *       （{@link #markDoneBy}）——「自己的活自己认」不算签，「没认账就干」不算数。</li>
 * </ol>
 * 两个签都是幂等的：重复点第二次不报错也不改行，返回 {@code false} 让用例走早退。
 * 「本周是否全清」是整周格子的共同事实，不塞进单格，见 {@link #weekCleared}。
 */
public final class SpinTask {

    /** 一转最多写几件事（现役口径，超出就是贪多干不完）。 */
    public static final int ITEMS_MAX = 8;
    /** 一格的字数上限（与 {@code couple_spin_task.item} 的列宽一致）。 */
    public static final int ITEM_LEN = 40;

    private final String id;
    private final String spaceId;
    private final String week;
    private final String item;
    private final String assignedUser;
    private boolean confirmed;
    private boolean done;
    private Long doneAt;
    private final long created;

    private SpinTask(String id, String spaceId, String week, String item, String assignedUser, boolean confirmed,
                     boolean done, Long doneAt, long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.week = week;
        this.item = item;
        this.assignedUser = assignedUser;
        this.confirmed = confirmed;
        this.done = done;
        this.doneAt = doneAt;
        this.created = created;
    }

    /**
     * 转一次盘：清洗事项、逐项过闸，然后按骰子决定谁先上岗，把事项交替分给两个人。
     * 一次调用产出整批格子，任一闸门没过就不产生任何格子（现役「先校验后落库」的顺序）。
     *
     * @param dice       本周的摇骰值（{@code stableHash(spaceId + "|spin|" + week)}），只取奇偶
     * @param alreadySpun 本周是否已经有格子——一周一转这条闸需要读存储，事实由用例递进来
     * @throws RuleViolation 本周转过盘／事项为空或超长或重复／不足两件（对外 400，文案原样）
     */
    public static List<SpinTask> draw(String spaceId, String week, String rawItems, String userA, String userB,
                                      long dice, boolean alreadySpun) {
        if (alreadySpun) {
            throw new RuleViolation("本周已经转过盘了，下周再来一赌");
        }
        List<String> items = splitItems(rawItems);
        if (items.size() < 2) {
            throw new RuleViolation("至少写两件事，不然不用转");
        }
        // 骰子只决定「从 A 开始还是从 B 开始」，之后严格按顺序交替，双方看到的分工必然互补
        boolean startA = Math.floorMod(dice, 2) == 0;
        String first = startA ? userA : userB;
        String second = startA ? userB : userA;
        long at = System.currentTimeMillis();
        List<SpinTask> drawn = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            drawn.add(new SpinTask(UUID.randomUUID().toString(), spaceId, week, items.get(i),
                    i % 2 == 0 ? first : second, false, false, null, at));
        }
        return drawn;
    }

    /** 事项清单：逗号（含中文逗号、顿号）分隔，去空、限长、去重。 */
    private static List<String> splitItems(String raw) {
        if (raw == null) {
            throw new RuleViolation("内容不能为空");
        }
        List<String> out = new ArrayList<>();
        for (String s : raw.split("[,，、]")) {
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > ITEM_LEN) {
                throw new RuleViolation("每条最多 " + ITEM_LEN + " 字");
            }
            if (out.contains(t)) {
                throw new RuleViolation("有重复项");
            }
            out.add(t);
        }
        if (out.isEmpty()) {
            throw new RuleViolation("至少写一项");
        }
        if (out.size() > ITEMS_MAX) {
            throw new RuleViolation("最多 " + ITEMS_MAX + " 项，贪多干不完");
        }
        return out;
    }

    /** 从存储重建：不校验——存量行必须读得出来，confirmed/done 为空按「没签、没干」看待。 */
    public static SpinTask restore(String id, String spaceId, String week, String item, String assignedUser,
                                   boolean confirmed, boolean done, Long doneAt, Long created) {
        return new SpinTask(id, spaceId, week, item, assignedUser, confirmed, done, doneAt,
                created == null ? 0L : created);
    }

    /**
     * 对方给这格认账（双签的第一签）。
     *
     * @return true=这一签真的落下了；false=早就认过了（幂等，用例据此早退）
     * @throws RuleViolation 是自己的活（对外 400，文案原样）
     */
    public boolean confirmBy(String me) {
        if (assignedUser.equals(me)) {
            throw new RuleViolation("自己的活自己认，TA 的活等 TA 认");
        }
        if (confirmed) {
            return false;
        }
        confirmed = true;
        return true;
    }

    /**
     * 天选之人打勾（双签的第二签，必须对方已经认过账）。
     *
     * @return true=这一格刚被干完；false=早就打勾了（幂等，用例据此早退，不重复计分）
     * @throws RuleViolation 活不归你（400）／对方还没认账（400，文案原样）
     */
    public boolean markDoneBy(String me) {
        if (!assignedUser.equals(me)) {
            throw new RuleViolation("这活不是你的，抢功也得等下周");
        }
        if (done) {
            return false;
        }
        if (!confirmed) {
            throw new RuleViolation("先等对方认账，干了也白干");
        }
        done = true;
        doneAt = System.currentTimeMillis();
        return true;
    }

    /** 整周是否全清：一格不剩才算本周干完（空周不算，免得没转过盘也发清空奖）。 */
    public static boolean weekCleared(List<SpinTask> week) {
        return !week.isEmpty() && week.stream().allMatch(SpinTask::done);
    }

    /** 这一格轮到谁。 */
    public boolean assignedTo(String me) {
        return assignedUser.equals(me);
    }

    /** 欠账 = 上一周（及更早）留下的没干完的格子。 */
    public boolean owed() {
        return !done;
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String week() {
        return week;
    }

    public String item() {
        return item;
    }

    public String assignedUser() {
        return assignedUser;
    }

    public boolean confirmed() {
        return confirmed;
    }

    public boolean done() {
        return done;
    }

    public Long doneAt() {
        return doneAt;
    }

    public long created() {
        return created;
    }
}
