package com.smart.chat.couple.domain.chore;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 家务轮盘聚合的单测：一周一转、事项闸门、交替分配、双签两条闸（自认不算、未认不许打勾）与幂等早退。
 * 这些规则原来散在 Service 的 if 里，搬进聚合之后必须能单独被钉住。
 */
class SpinTaskTest {

    private static List<SpinTask> draw(String rawItems, long dice) {
        return SpinTask.draw("s1", "2026-10-05", rawItems, "alice", "bob", dice, false);
    }

    @Test
    void oneSpinPerWeekIsRefusedBeforeAnythingElse() {
        assertThatThrownBy(() -> SpinTask.draw("s1", "2026-10-05", "洗碗,拖地", "alice", "bob", 0L, true))
                .isInstanceOf(RuleViolation.class).hasMessage("本周已经转过盘了，下周再来一赌");
    }

    @Test
    void itemsAreCleanedAndGated() {
        assertThat(draw(" 洗碗 ， 拖地 ", 0L)).extracting(SpinTask::item).containsExactly("洗碗", "拖地");
        assertThatThrownBy(() -> draw(null, 0L)).isInstanceOf(RuleViolation.class).hasMessage("内容不能为空");
        assertThatThrownBy(() -> draw(" , ", 0L)).isInstanceOf(RuleViolation.class).hasMessage("至少写一项");
        assertThatThrownBy(() -> draw("洗碗,洗碗", 0L)).isInstanceOf(RuleViolation.class).hasMessage("有重复项");
        assertThatThrownBy(() -> draw("一".repeat(41) + ",拖地", 0L))
                .isInstanceOf(RuleViolation.class).hasMessage("每条最多 40 字");
        assertThatThrownBy(() -> draw("洗碗", 0L)).isInstanceOf(RuleViolation.class).hasMessage("至少写两件事，不然不用转");
        assertThatThrownBy(() -> draw("a,b,c,d,e,f,g,h,i", 0L))
                .isInstanceOf(RuleViolation.class).hasMessage("最多 8 项，贪多干不完");
    }

    @Test
    void diceOnlyDecidesWhoStartsAndItemsAlternate() {
        List<SpinTask> even = draw("洗碗,拖地,倒垃圾", 2L);
        assertThat(even).extracting(SpinTask::assignedUser).containsExactly("alice", "bob", "alice");
        List<SpinTask> odd = draw("洗碗,拖地,倒垃圾", 3L);
        assertThat(odd).extracting(SpinTask::assignedUser).containsExactly("bob", "alice", "bob");
        assertThat(even).allSatisfy(t -> {
            assertThat(t.confirmed()).isFalse();
            assertThat(t.done()).isFalse();
            assertThat(t.id()).isNotBlank();
        });
    }

    @Test
    void ownConfirmationIsNotASecondSignature() {
        SpinTask task = draw("洗碗,拖地", 0L).get(0);
        assertThatThrownBy(() -> task.confirmBy(task.assignedUser()))
                .isInstanceOf(RuleViolation.class).hasMessage("自己的活自己认，TA 的活等 TA 认");

        String counterpart = task.assignedTo("alice") ? "bob" : "alice";
        assertThat(task.confirmBy(counterpart)).isTrue();
        // 再点一次：不报错也不改签，返回 false 让用例早退
        assertThat(task.confirmBy(counterpart)).isFalse();
        assertThat(task.confirmed()).isTrue();
    }

    @Test
    void onlyTheChosenOneChecksOffAfterConfirmation() {
        SpinTask task = draw("洗碗,拖地", 0L).get(0);
        String owner = task.assignedUser();
        String counterpart = task.assignedTo("alice") ? "bob" : "alice";

        // 抢 TA 的活
        assertThatThrownBy(() -> task.markDoneBy(counterpart))
                .isInstanceOf(RuleViolation.class).hasMessage("这活不是你的，抢功也得等下周");
        // 对方还没认账，干了也白干
        assertThatThrownBy(() -> task.markDoneBy(owner))
                .isInstanceOf(RuleViolation.class).hasMessage("先等对方认账，干了也白干");
        assertThat(task.done()).isFalse();

        task.confirmBy(counterpart);
        assertThat(task.markDoneBy(owner)).isTrue();
        assertThat(task.done()).isTrue();
        assertThat(task.doneAt()).isNotNull();
        assertThat(task.owed()).isFalse();
        // 重复打勾幂等：不再计分的那道闸就在这返回值上
        assertThat(task.markDoneBy(owner)).isFalse();
    }

    @Test
    void weekIsClearedOnlyWhenEveryCellIsDone() {
        List<SpinTask> week = draw("洗碗,拖地", 0L);
        assertThat(SpinTask.weekCleared(List.of())).isFalse();
        assertThat(SpinTask.weekCleared(week)).isFalse();
        week.forEach(t -> {
            t.confirmBy(t.assignedTo("alice") ? "bob" : "alice");
            t.markDoneBy(t.assignedUser());
        });
        assertThat(SpinTask.weekCleared(week)).isTrue();
        assertThat(week).allSatisfy(t -> assertThat(t.owed()).isFalse());
    }

    @Test
    void restoreKeepsLegacyRowAsIs() {
        SpinTask legacy = SpinTask.restore("t1", "s1", "2026-09-28", "擦窗", "bob", true, false, null, null);
        assertThat(legacy.confirmed()).isTrue();
        assertThat(legacy.done()).isFalse();
        assertThat(legacy.owed()).isTrue();
        // created 为空的老行读出来是 0，不能抛
        assertThat(legacy.created()).isZero();
        assertThat(legacy.id()).isEqualTo("t1");
    }
}
