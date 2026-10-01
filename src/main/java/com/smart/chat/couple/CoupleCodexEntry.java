package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F280 词条共建：「我们的词」考据，双人共编（改后推 TA）。 */
@Data
@TableName("couple_codex_entry")
public class CoupleCodexEntry {

    public static final int TERM_MAX = 40;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String term;
    private String definition;
    private String origin;
    private String usageNote;
    private String fromUser;
    private String updatedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleCodexEntry of(String spaceId, String term, String definition, String origin,
                                      String usageNote, String fromUser) {
        CoupleCodexEntry row = new CoupleCodexEntry();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.term = term;
        row.definition = definition == null ? "" : definition;
        row.origin = origin == null ? "" : origin;
        row.usageNote = usageNote == null ? "" : usageNote;
        row.fromUser = fromUser;
        row.updatedBy = fromUser;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
