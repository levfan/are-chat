package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F291 大事拆步：一步一格，本人完成或 TA 补进展章。 */
@Data
@TableName("couple_bucket_step")
public class CoupleBucketStep {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String bucketId;
    private Integer seq;
    private String text;
    private Integer done;
    private String doneBy;
    private Long doneAt;
    private Long created;

    public static CoupleBucketStep of(String spaceId, String bucketId, int seq, String text) {
        CoupleBucketStep row = new CoupleBucketStep();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.bucketId = bucketId;
        row.seq = seq;
        row.text = text;
        row.done = 0;
        row.doneBy = "";
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean doneFlag() {
        return Integer.valueOf(1).equals(done);
    }
}
