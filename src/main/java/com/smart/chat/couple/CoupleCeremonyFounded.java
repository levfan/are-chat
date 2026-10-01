package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F230 建国纪念日：自定义「我们的小日子」，与官方纪念日区分。 */
@Data
@TableName("couple_ceremony_founded")
public class CoupleCeremonyFounded {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String startDay;
    private Integer repeatYear;
    private Long created;
    private Long updatedAt;

    public static CoupleCeremonyFounded of(String spaceId, String name, String startDay, boolean repeatYear) {
        CoupleCeremonyFounded row = new CoupleCeremonyFounded();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.startDay = startDay;
        row.repeatYear = repeatYear ? 1 : 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean repeats() {
        return repeatYear != null && repeatYear == 1;
    }
}
