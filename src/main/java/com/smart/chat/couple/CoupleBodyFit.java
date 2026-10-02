package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** F314 运动链：同日同项目两人各报计数，30 分钟窗口内双报算接上链。 */
@Data
@TableName("couple_body_fit")
public class CoupleBodyFit {

    public static final List<String> KINDS = List.of("PUSHUP", "SQUAT", "PLANK", "RUN", "STRETCH");
    public static final long LINK_WINDOW_MS = 30 * 60 * 1000L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String kind;
    private Integer countA;
    private Integer countB;
    private Long atA;
    private Long atB;
    private Integer linked;
    private Long created;
    private Long updatedAt;

    public static CoupleBodyFit of(String spaceId, String day, String kind) {
        CoupleBodyFit row = new CoupleBodyFit();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.kind = kind;
        row.countA = 0;
        row.countB = 0;
        row.atA = 0L;
        row.atB = 0L;
        row.linked = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean linkedFlag() {
        return linked != null && linked == 1;
    }
}
