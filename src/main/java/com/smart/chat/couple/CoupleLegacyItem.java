package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** F347 传世清单：想留给 TA 的东西（地点/口令类），双签才算封存。 */
@Data
@TableName("couple_legacy_item")
public class CoupleLegacyItem {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_SEALED = "SEALED";
    public static final List<String> KINDS = List.of("PLACE", "PASSWORD", "THING", "WORD");

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String item;
    private String kind;
    private String detail;
    private String ownerUser;
    private String status;
    private String signedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleLegacyItem of(String spaceId, String item, String kind, String detail, String ownerUser) {
        CoupleLegacyItem row = new CoupleLegacyItem();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.item = item;
        row.kind = kind;
        row.detail = detail;
        row.ownerUser = ownerUser;
        row.status = STATUS_OPEN;
        row.signedBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isSealed() {
        return STATUS_SEALED.equals(status);
    }
}
