package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F307 双角色追剧：同一部剧各认领一个角色写角色日记。 */
@Data
@TableName("couple_role_movie")
public class CoupleRoleMovie {

    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_FINISHED = "FINISHED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String work;
    private String fromUser;
    private String roleName;
    private String diary;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleRoleMovie of(String spaceId, String work, String fromUser, String roleName) {
        CoupleRoleMovie row = new CoupleRoleMovie();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.work = work;
        row.fromUser = fromUser;
        row.roleName = roleName;
        row.diary = "";
        row.status = STATUS_ONGOING;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
