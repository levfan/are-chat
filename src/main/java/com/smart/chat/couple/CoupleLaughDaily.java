package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * F391 每日一逗：每天只有一格，谁负责逗由服务层按周轮换算出来（抢班 400），对方判笑没笑。
 * 判定口径（V48 头注）：一天一格、判分人只可能是对方，所以 judged_by/judged_at 记用户名与毫秒，
 * 不拆 A/B 双列；verdict 空串=还没判。
 * uk(space_id,day) 保证一天一行，judge() 只生效一次，判过了不再改口。
 */
@Data
@TableName("couple_laugh_daily")
public class CoupleLaughDaily {

    /** 笑了 */
    public static final String VERDICT_HAPPY = "HAPPY";
    /** 没笑 */
    public static final String VERDICT_FLAT = "FLAT";
    /** 强撑 */
    public static final String VERDICT_FAKE = "FAKE";
    /** 判分白名单（服务层校验，顺序即口径顺序） */
    public static final List<String> VERDICTS = List.of(VERDICT_HAPPY, VERDICT_FLAT, VERDICT_FAKE);
    /** 逗的内容字数上限（列 varchar(400) 已按 4 倍宽度放宽） */
    public static final int CONTENT_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 哪一天 yyyy-MM-dd（一天一格） */
    private String day;
    /** 今天负责逗的人（区分大小写，由周轮换算出） */
    private String ownerUser;
    /** 逗的内容；空串=还没交作业 */
    private String content;
    /** HAPPY 笑了/FLAT 没笑/FAKE 强撑；空串=还没判 */
    private String verdict;
    /** 判分的人=对方（null=还没判） */
    private String judgedBy;
    /** 判分时间毫秒；null=还没判 */
    private Long judgedAt;
    private Long created;
    private Long updatedAt;

    public static CoupleLaughDaily of(String spaceId, String day, String ownerUser, String content) {
        CoupleLaughDaily row = new CoupleLaughDaily();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.ownerUser = ownerUser;
        row.content = content == null ? "" : content;
        row.verdict = "";
        row.judgedBy = null;
        row.judgedAt = null;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 这一格判过了没有（judged_at 落下来才算）。 */
    public boolean judged() {
        return judgedAt != null;
    }

    /** 今天是不是轮到这个人逗（归属校验用，区分大小写）。 */
    public boolean ownerIs(String user) {
        return ownerUser != null && ownerUser.equals(user);
    }

    /** 对方判分：一天只判一次，判过了不再改口（服务层据此决定推不推事件）。 */
    public void judge(String by, String verdict) {
        if (judged()) {
            return;
        }
        this.verdict = verdict;
        this.judgedBy = by;
        this.judgedAt = System.currentTimeMillis();
        this.updatedAt = this.judgedAt;
    }
}
