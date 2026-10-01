package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F241 董事会决议：提案→附议→通过/否决（一票否决），全程留痕。 */
@Data
@TableName("couple_board_vote")
public class CoupleBoardVote {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_VETOED = "VETOED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    private String proposer;
    private String status;
    private String vetoBy;
    private Long decidedAt;
    private Long created;

    public static CoupleBoardVote of(String spaceId, String proposer, String title) {
        CoupleBoardVote row = new CoupleBoardVote();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.proposer = proposer;
        row.title = title;
        row.status = STATUS_PENDING;
        row.vetoBy = "";
        row.decidedAt = 0L;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }
}
