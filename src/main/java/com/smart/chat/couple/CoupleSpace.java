package com.smart.chat.couple;

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
public class CoupleSpace {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISSOLVED = "DISSOLVED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String userA;
    private String userB;
    private String status;
    /** 在一起纪念日（yyyy-MM-dd，空则按 created 计算在一起天数） */
    private String anniversary;
    private Long created;
    private Long dissolvedAt;

    public static CoupleSpace of(String userA, String userB) {
        CoupleSpace space = new CoupleSpace();
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

    public boolean contains(String username) {
        return userA.equals(username) || userB.equals(username);
    }
}
