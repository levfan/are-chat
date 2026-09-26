package com.smart.chat.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 88 公告已读记录：用户点「我知道了」后不再展示该条公告 */
@Data
@TableName("announcement_read")
public class AnnouncementRead {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String username;
    private String announcementId;
    private Long readAt;

    public static AnnouncementRead of(String username, String announcementId) {
        AnnouncementRead record = new AnnouncementRead();
        record.id = UUID.randomUUID().toString();
        record.username = username;
        record.announcementId = announcementId;
        record.readAt = System.currentTimeMillis();
        return record;
    }
}
