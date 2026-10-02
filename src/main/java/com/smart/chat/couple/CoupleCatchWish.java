package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F380 暗中心愿本：偷偷记下 TA 随口说想要的，兑现登记之后才揭晓。
 * 保密口径：revealed_at 为 null = 还藏着（ownerUser 侧一律不可见全文），
 * fulfill() 是唯一揭晓入口——「兑现即揭晓」，二者同一行同时翻转，不存在只兑现不揭晓或反过来。
 */
@Data
@TableName("couple_catch_wish")
public class CoupleCatchWish {

    /** 心愿内容字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int CONTENT_MAX = 60;
    /** 说的场合字数上限（列 varchar(160) 已按 4 倍宽度放宽） */
    public static final int SCENE_MAX = 40;
    /** 每人被偷偷记的心愿条数上限（服务层用） */
    public static final int PER_OWNER_MAX = 12;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 心愿的主人=被记的那位（区分大小写，对 TA 保密） */
    private String ownerUser;
    /** 记账的人=另一方（区分大小写） */
    private String recorderUser;
    /** TA 随口说想要的 */
    private String content;
    /** 出处日期 yyyy-MM-dd（TA 是什么时候说的，揭晓时引用）；空串=没记 */
    private String sourceDay;
    /** 在什么场合说的；空串=没说 */
    private String scene;
    /** 是否已兑现登记（1=已给） */
    private Integer fulfilled;
    /** 揭晓时间毫秒；null=还保密 */
    private Long revealedAt;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchWish of(String spaceId, String ownerUser, String recorderUser,
                                     String content, String sourceDay, String scene) {
        CoupleCatchWish row = new CoupleCatchWish();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.ownerUser = ownerUser;
        row.recorderUser = recorderUser;
        row.content = content;
        row.sourceDay = sourceDay == null ? "" : sourceDay;
        row.scene = scene == null ? "" : scene;
        row.fulfilled = 0;
        row.revealedAt = null;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 是否还藏着（没揭晓）。 */
    public boolean secret() {
        return revealedAt == null;
    }

    /** 是否已兑现登记。 */
    public boolean filled() {
        return fulfilled != null && fulfilled == 1;
    }

    /** 兑现登记：置 fulfilled=1 并同时写 revealedAt——「兑现即揭晓」，一次动作两列同翻。 */
    public void fulfill() {
        fulfilled = 1;
        revealedAt = System.currentTimeMillis();
        updatedAt = revealedAt;
    }
}
