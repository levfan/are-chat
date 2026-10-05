package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 情侣空间：一对一关系，user_a/user_b 为规范化排序（字典序小者在前）的双方用户名。
 * 建立后双方共享约定、每日仪式与共享清单；解除后历史数据保留但不再可见。
 */
@Data
@TableName("couple_space")
public class CoupleSpacePO {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISSOLVED = "DISSOLVED";

    /** F27 空间主题白名单 */
    public static final java.util.Set<String> THEMES =
            java.util.Set.of("classic", "cherry", "ocean", "forest", "night");


    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String userA;
    private String userB;
    private String status;
    /** 在一起纪念日（yyyy-MM-dd，空则按 created 计算在一起天数） */
    private String anniversary;
    /** 用户 A 所在城市（手填，异地恋助手用于匹配内置城市库算时差/距离） */
    private String cityA;
    /** 用户 B 所在城市 */
    private String cityB;
    /** user_a 的专属爱称（由对方起，如「宝宝」「猪猪」，空 = 未起） */
    private String nickA;
    /** user_b 的专属爱称（由对方起） */
    private String nickB;
    /** F26 我们的宣言：只有彼此懂的一句话（60 字内，可空） */
    private String slogan;
    /** F27 空间主题（classic/cherry/ocean/forest/night，默认 classic） */
    private String theme;
    /** F28 贴纸墙佩戴的贴纸 key（逗号分隔，最多 6 枚，可空） */
    private Long created;
    private Long dissolvedAt;

    public static CoupleSpacePO of(String userA, String userB) {
        CoupleSpacePO space = new CoupleSpacePO();
        space.id = UUID.randomUUID().toString();
        space.userA = userA;
        space.userB = userB;
        space.status = STATUS_ACTIVE;
        space.created = System.currentTimeMillis();
        return space;
    }

    /** 双方用户名的规范化顺序：字典序小者为 user_a。 */
    public static String[] ordered(String left, String right) {
        return left.compareTo(right) <= 0 ? new String[]{left, right} : new String[]{right, left};
    }

    /** 我在这段关系里的另一半。 */
    public String partnerOf(String me) {
        return userA.equals(me) ? userB : userA;
    }

    /** 某人的专属爱称（由对方起的那个），空返回 null。 */
    public String nickOf(String username) {
        return userA.equals(username) ? nickA : nickB;
    }

    /** 设置某人的专属爱称（调用方应是 TA 的另一半）。 */
    public void setNickOf(String username, String nick) {
        if (userA.equals(username)) {
            this.nickA = nick;
        } else {
            this.nickB = nick;
        }
    }

    public boolean contains(String username) {
        return userA.equals(username) || userB.equals(username);
    }
}
