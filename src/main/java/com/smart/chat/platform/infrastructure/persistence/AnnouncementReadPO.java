package com.smart.chat.platform.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 88 公告已读记录的**持久化模型**：用户点「我知道了」后不再展示该条公告。
 * 业务名 {@code AnnouncementRead} 让给 domain，这里只做表映射。
 */
@Data
@TableName("announcement_read")
public class AnnouncementReadPO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String username;
    private String announcementId;
    private Long readAt;
}
