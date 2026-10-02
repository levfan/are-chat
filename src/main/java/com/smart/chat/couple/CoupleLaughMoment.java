package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F390 笑点存档：笑到肚子疼的时刻按天记一笔，对方可以事后补一句「现场证词」。
 * 判定口径（V48 头注）：证词不拆 tick_a/tick_b 双列——笑点一人一条、能补证词的人只可能是对方，
 * 所以只记 witness_by 用户名（NULL=还没补），witness 存证词正文，witnessed() 判有没有。
 * 限流：每人每天 ≤3 条（PER_DAY_MAX，服务层数），uk(space_id,day,from_user,title) 挡住同人同日同题重记。
 */
@Data
@TableName("couple_laugh_moment")
public class CoupleLaughMoment {

    /** 笑点名字字数上限（列 varchar(120) 已按 4 倍宽度放宽） */
    public static final int TITLE_MAX = 30;
    /** 现场还原字数上限（列 varchar(400) 已按 4 倍宽度放宽） */
    public static final int SCENE_MAX = 100;
    /** 谁干的上限（列 varchar(50) 与用户名列同宽，够 20 字） */
    public static final int CULPRIT_MAX = 20;
    /** 现场证词字数上限（列 varchar(400) 已按 4 倍宽度放宽） */
    public static final int WITNESS_MAX = 100;
    /** 好笑度下限（服务层钳制） */
    public static final int LEVEL_MIN = 1;
    /** 好笑度上限（服务层钳制） */
    public static final int LEVEL_MAX = 5;
    /** 每人每天最多记 3 条（服务层用） */
    public static final int PER_DAY_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 发生的日子 yyyy-MM-dd */
    private String day;
    /** 记账的人（区分大小写） */
    private String fromUser;
    /** 这条笑点叫什么 */
    private String title;
    /** 谁干的（用户名或外号）；空串=没写 */
    private String culprit;
    /** 现场还原；空串=没写 */
    private String scene;
    /** 好笑度 1-5（服务层钳制，缺省 3） */
    private Integer funLevel;
    /** 对方的现场证词；空串=还没补 */
    private String witness;
    /** 补证词的人=对方（null=没补过） */
    private String witnessBy;
    private Long created;
    private Long updatedAt;

    public static CoupleLaughMoment of(String spaceId, String day, String fromUser, String title,
                                       String culprit, String scene, int funLevel) {
        CoupleLaughMoment row = new CoupleLaughMoment();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.title = title;
        row.culprit = culprit == null ? "" : culprit;
        row.scene = scene == null ? "" : scene;
        row.funLevel = funLevel;
        row.witness = "";
        row.witnessBy = null;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 证词补过了没有（witness_by 非空才算，光有正文不算）。 */
    public boolean witnessed() {
        return witnessBy != null && !witnessBy.isBlank();
    }

    /**
     * 补现场证词：一人一条笑点只补一次，已经盖过就不再改写（返回 false，服务层不重推）。
     * 成功时写 witness 正文 + witness_by=补词的人，并刷新更新时间。
     */
    public boolean witness(String by, String text) {
        if (witnessed()) {
            return false;
        }
        witness = text == null ? "" : text;
        witnessBy = by;
        updatedAt = System.currentTimeMillis();
        return true;
    }
}
