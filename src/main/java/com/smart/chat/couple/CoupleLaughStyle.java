package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * F397 幽默风格图鉴：同一个人两行——自评（rater==about_user）与互评各一行，差异出相处建议。
 * 一行一格：uk(space_id,about_user,rater) 保证「谁评谁」只一行，改评走 find() 回填 update。
 * 自不自评不另存标记列，selfRated() 直接比 about_user 与 rater 两个用户名（区分大小写）。
 */
@Data
@TableName("couple_laugh_style")
public class CoupleLaughStyle {

    /** 谐音梗 */
    public static final String STYLE_PUN = "PUN";
    /** 冷幽默 */
    public static final String STYLE_COLD = "COLD";
    /** 自嘲 */
    public static final String STYLE_SELF = "SELF";
    /** 动作派 */
    public static final String STYLE_ACTION = "ACTION";
    /** 模仿派 */
    public static final String STYLE_MIME = "MIME";
    /** 风格白名单（服务层校验，顺序即口径顺序） */
    public static final List<String> STYLES =
            List.of(STYLE_PUN, STYLE_COLD, STYLE_SELF, STYLE_ACTION, STYLE_MIME);
    /** 补一句字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int NOTE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 评的是谁（区分大小写） */
    private String aboutUser;
    /** 谁来评：等于 aboutUser 就是自评，否则互评（区分大小写） */
    private String rater;
    /** PUN 谐音梗/COLD 冷幽默/SELF 自嘲/ACTION 动作派/MIME 模仿派 */
    private String style;
    /** 补一句；空串=没写 */
    private String note;
    private Long created;
    private Long updatedAt;

    public static CoupleLaughStyle of(String spaceId, String aboutUser, String rater, String style, String note) {
        CoupleLaughStyle row = new CoupleLaughStyle();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.aboutUser = aboutUser;
        row.rater = rater;
        row.style = style;
        row.note = note == null ? "" : note;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 这一行是自己给自己评的（自评格）。 */
    public boolean selfRated() {
        return aboutUser != null && aboutUser.equals(rater);
    }
}
