package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日仪式升级：甜蜜任务卡、默契大考验、情话抽卡、恋爱运势、晚安故事。
 * 情绪价值设计：每天一件小事让彼此「被想到」，默契对局制造惊喜瞬间，
 * 运势签给平淡日子一个甜甜的解释。
 */
@Service
public class CoupleRitualService {

    // ========== VO ==========

    public record TaskVO(String id, String day, String username, String content, String status,
                         Long doneAt, boolean mine) {
    }

    public record TacitVO(String id, String question, String myAnswer, String partnerAnswer,
                          String status, Long created, Long settledAt) {
    }

    public record TacitStateVO(TacitVO pending, long matchedCount, long totalCount) {
    }

    /** 恋爱运势签：宜 / 忌 / 幸运物 / 一句话签文 / 综合指数（60-99）。 */
    public record FortuneVO(String day, String good, String bad, String lucky, String line, int score) {
    }

    public record StoryVO(String day, String title, String content) {
    }

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleTaskMapper taskMapper;
    private final CoupleTacitMapper tacitMapper;
    private final ImPushService push;

    public CoupleRitualService(CoupleSpaceMapper spaceMapper, CoupleTaskMapper taskMapper,
                               CoupleTacitMapper tacitMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.taskMapper = taskMapper;
        this.tacitMapper = tacitMapper;
        this.push = push;
    }

    // ========== 甜蜜任务卡 ==========

    /** 拉取今天的任务卡（没有就按天生成一张；重复拉取同一张），双方各一张。 */
    public TaskVO todayTask(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleTask task = taskMapper.find(space.getId(), day, me);
        if (task == null) {
            // 唯一键兜底：并发场景重复生成视为幂等
            String content = CoupleRitualBank.pickTask(space.getId(), day, me);
            task = CoupleTask.of(space.getId(), day, me, content);
            try {
                taskMapper.insert(task);
            } catch (Exception e) {
                task = taskMapper.find(space.getId(), day, me);
            }
        }
        return toTaskVO(task, me);
    }

    /** 任务卡列表（最近 14 天，新→旧），包含双方的。 */
    public List<TaskVO> recentTasks(String me) {
        CoupleSpace space = requireSpace(me);
        String startDay = LocalDate.now().minusDays(13).toString();
        return spaceTasks(space).stream()
                .filter(t -> t.getTaskDay().compareTo(startDay) >= 0)
                .sorted((a, b) -> b.getTaskDay().compareTo(a.getTaskDay()))
                .map(t -> toTaskVO(t, me))
                .toList();
    }

    /** 打卡完成任务。 */
    public TaskVO doneTask(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleTask task = taskMapper.find(space.getId(), day, me);
        if (task == null) {
            task = CoupleTask.of(space.getId(), day, me, CoupleRitualBank.pickTask(space.getId(), day, me));
            taskMapper.insert(task);
        }
        if (CoupleTask.STATUS_DONE.equals(task.getStatus())) {
            throw new BusinessException(409, "今天的任务已经打卡过啦");
        }
        task.setStatus(CoupleTask.STATUS_DONE);
        task.setDoneAt(System.currentTimeMillis());
        taskMapper.updateById(task);
        push.pushCoupleEvent("task-done", me, space.partnerOf(me),
                "TA 完成了今天的甜蜜任务 ✅：" + task.getContent());
        return toTaskVO(task, me);
    }

    // ========== 默契大考验 ==========

    /** 默契状态：进行中的一局（若有，双方看到同一题）+ 累计默契数。 */
    public TacitStateVO tacitState(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleTacit pending = tacitMapper.findPending(space.getId());
        return new TacitStateVO(pending == null ? null : toTacitVO(pending, me),
                tacitMapper.countMatched(space.getId()), tacitMapper.findBySpace(space.getId()).size());
    }

    /** 发起一局默契考验（有未结算的局不能重复发起）。 */
    public TacitVO startTacit(String me) {
        CoupleSpace space = requireSpace(me);
        if (tacitMapper.findPending(space.getId()) != null) {
            throw new BusinessException(409, "有一局还在进行中，等双方都答完吧");
        }
        String question = CoupleRitualBank.pickTacit(space.getId(), LocalDate.now().toString()
                + "|" + tacitMapper.findBySpace(space.getId()).size());
        CoupleTacit tacit = CoupleTacit.of(space.getId(), question);
        tacitMapper.insert(tacit);
        push.pushCoupleEvent("tacit-started", me, space.partnerOf(me),
                "TA 发起了一场默契大考验 🎯 你也来答答看，看你们是不是心有灵犀！");
        return toTacitVO(tacit, me);
    }

    /** 提交我的答案：我先答存答案等 TA；TA 后答立即结算并推送结果。 */
    public TacitVO answerTacit(String me, String answer) {
        CoupleSpace space = requireSpace(me);
        String text = answer == null ? "" : answer.trim();
        if (text.isEmpty() || text.length() > 60) {
            throw new BusinessException(400, "写一个 1-60 字的答案吧");
        }
        CoupleTacit tacit = tacitMapper.findPending(space.getId());
        if (tacit == null) {
            throw new BusinessException(404, "没有进行中的对局，先发起一场吧");
        }
        boolean isA = space.getUserA().equals(me);
        String mine = isA ? tacit.getAnswerA() : tacit.getAnswerB();
        if (mine != null) {
            throw new BusinessException(409, "你已经答过这一局啦，等 TA 提交答案");
        }
        if (isA) {
            tacit.setAnswerA(text);
        } else {
            tacit.setAnswerB(text);
        }
        // 双方都答了 → 结算
        if (tacit.getAnswerA() != null && tacit.getAnswerB() != null) {
            boolean match = tacit.getAnswerA().equals(tacit.getAnswerB());
            tacit.setMatch(match ? 1 : 0);
            tacit.setSettledAt(System.currentTimeMillis());
            String detail = match
                    ? "默契大考验结果揭晓 🎉 答案一模一样！你们真是心有灵犀：" + tacit.getQuestion()
                    : "默契大考验结果揭晓：这次答案不一样～快去看看 TA 是怎么想的 💭";
            push.pushCoupleEventBoth("tacit-settled", me, space.getUserA(), space.getUserB(), detail);
        } else {
            push.pushCoupleEvent("tacit-answered", me, space.partnerOf(me),
                    "TA 已经写下默契考验的答案了，就等你啦 ⏳");
        }
        tacitMapper.updateById(tacit);
        return toTacitVO(tacit, me);
    }

    /** 默契历史（最近 20 局，新→旧）。 */
    public List<TacitVO> tacitHistory(String me) {
        CoupleSpace space = requireSpace(me);
        return tacitMapper.findBySpace(space.getId()).stream()
                .limit(20)
                .map(t -> toTacitVO(t, me))
                .toList();
    }

    // ========== 情话抽卡 / 恋爱运势 / 晚安故事 ==========

    /** 随机抽一句情话（每次不同，可以抽到喜欢的那句塞进 TA 的信箱）。 */
    public String drawLoveWord(String me) {
        requireSpace(me);
        List<String> bank = CoupleRitualBank.loveWords();
        return bank.get((int) Math.floorMod(System.nanoTime(), bank.size()));
    }

    /** 今日恋爱运势（同一天双方同一张签）。 */
    public FortuneVO fortune(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        String[] picked = CoupleRitualBank.pickFortune(space.getId(), day);
        return new FortuneVO(day, picked[0], picked[1], picked[2], picked[3], Integer.parseInt(picked[4]));
    }

    /** 今晚的晚安故事（同一天双方同一个故事；晚安打卡后看更配哦）。 */
    public StoryVO goodnightStory(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        String[] story = CoupleRitualBank.pickStory(space.getId(), day);
        return new StoryVO(day, story[0], story[1]);
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private List<CoupleTask> spaceTasks(CoupleSpace space) {
        return taskMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoupleTask>()
                .eq(CoupleTask::getSpaceId, space.getId()));
    }

    private TaskVO toTaskVO(CoupleTask task, String me) {
        return new TaskVO(task.getId(), task.getTaskDay(), task.getUsername(), task.getContent(),
                task.getStatus(), task.getDoneAt(), task.getUsername().equals(me));
    }

    private TacitVO toTacitVO(CoupleTacit tacit, String me) {
        CoupleSpace space = requireSpace(me);
        boolean imA = space.getUserA().equals(me);
        String my = imA ? tacit.getAnswerA() : tacit.getAnswerB();
        String theirs = imA ? tacit.getAnswerB() : tacit.getAnswerA();
        String status = tacit.isSettled() ? (tacit.getMatch() == 1 ? "MATCHED" : "MISS") : "WAITING";
        return new TacitVO(tacit.getId(), tacit.getQuestion(), my, theirs, status, tacit.getCreated(),
                tacit.getSettledAt());
    }
}
