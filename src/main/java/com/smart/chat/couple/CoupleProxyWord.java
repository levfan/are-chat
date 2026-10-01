package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F261 替我说：以 TA 口吻写的草稿，TA 定稿即 ADOPTED。 */
@Data
@TableName("couple_proxy_word")
public class CoupleProxyWord {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ADOPTED = "ADOPTED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String content;
    private String fromUser;
    private String status;
    private String finalText;
    private String adoptedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleProxyWord of(String spaceId, String content, String fromUser) {
        CoupleProxyWord row = new CoupleProxyWord();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.content = content;
        row.fromUser = fromUser;
        row.status = STATUS_DRAFT;
        row.finalText = "";
        row.adoptedBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
