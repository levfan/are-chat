package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F385 真话翻译机：本人申报「嘴上说的 → 实际意思」词条，对方只见结果不可改。
 * 写权限口径：fromUser 是唯一作者，uk(space,from_user,say) 同人说辞只一行；每人 ≤10 条由服务层限。
 */
@Data
@TableName("couple_catch_say")
public class CoupleCatchSay {

    /** 嘴上说的字数上限（列 varchar(80) 已按 4 倍宽度放宽） */
    public static final int SAY_MAX = 20;
    /** 实际意思字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int MEANS_MAX = 60;
    /** 每人词条条数上限（服务层用） */
    public static final int PER_USER_MAX = 10;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 申报的人（区分大小写） */
    private String fromUser;
    /** 我嘴上说的 */
    private String say;
    /** 实际意思；空串=没解释 */
    private String means;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchSay of(String spaceId, String fromUser, String say, String means) {
        CoupleCatchSay row = new CoupleCatchSay();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.say = say;
        row.means = means == null ? "" : means;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
