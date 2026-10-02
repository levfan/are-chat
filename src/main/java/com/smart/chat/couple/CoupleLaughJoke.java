package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F392 冷笑话结冰榜：互发冷笑话，对方判「结没结冰」，年度结冰最多者封冷场之王。
 * 判定口径（V48 头注）：frozen 是 1/0 位、没判时也存 0，「判没判」一律靠 judged_by 是否为 null 区分，
 * 不能用 frozen 推（0 既可能是「没冰」也可能是「还没人判」）。
 * uk(space_id,from_user,content) 一人一句冷笑话只一行（判分回填走 find()）。
 */
@Data
@TableName("couple_laugh_joke")
public class CoupleLaughJoke {

    /** 冷笑话正文字数上限（列 varchar(320) 已按 4 倍宽度放宽） */
    public static final int CONTENT_MAX = 80;
    /** 每人每天最多发 3 条（服务层用） */
    public static final int PER_USER_DAY_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 发出日 yyyy-MM-dd */
    private String day;
    /** 讲冷笑话的人（区分大小写） */
    private String fromUser;
    /** 冷笑话正文 */
    private String content;
    /** 1=判为结冰，0=没冰（未判也存 0，靠 judgedBy 区分） */
    private Integer frozen;
    /** 判的人=对方（null=还没判，一条只判一次） */
    private String judgedBy;
    /** 判分时间毫秒；null=还没判 */
    private Long judgedAt;
    private Long created;
    private Long updatedAt;

    public static CoupleLaughJoke of(String spaceId, String day, String fromUser, String content) {
        CoupleLaughJoke row = new CoupleLaughJoke();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.content = content;
        row.frozen = 0;
        row.judgedBy = null;
        row.judgedAt = null;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 这条被判过了没有（没判的 frozen=0 不代表「没冰」）。 */
    public boolean judged() {
        return judgedBy != null && !judgedBy.isBlank();
    }

    /** 已判且判成了结冰。 */
    public boolean frozenFlag() {
        return frozen != null && frozen == 1;
    }

    /**
     * 对方判结冰/没冰：一条只判一次，已判过返回 false 不改写（服务层不重推）。
     * 成功时 frozen 置 1/0，并写齐 judged_by、judged_at 与更新时间。
     */
    public boolean judge(String by, boolean freeze) {
        if (judged()) {
            return false;
        }
        frozen = freeze ? 1 : 0;
        judgedBy = by;
        judgedAt = System.currentTimeMillis();
        updatedAt = judgedAt;
        return true;
    }
}
