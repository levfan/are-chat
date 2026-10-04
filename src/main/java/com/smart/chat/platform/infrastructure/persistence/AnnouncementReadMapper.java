package com.smart.chat.platform.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告已读记录 Mapper：只认 {@link AnnouncementReadPO}。
 * {@code exists} 的判重口径（uq_ann_read：username + announcement_id）与改造前一致。
 */
@Mapper
public interface AnnouncementReadMapper extends BaseMapperCompat<AnnouncementReadPO> {

    default boolean exists(String username, String announcementId) {
        return selectCount(new LambdaQueryWrapper<AnnouncementReadPO>()
                .eq(AnnouncementReadPO::getUsername, username)
                .eq(AnnouncementReadPO::getAnnouncementId, announcementId)) > 0;
    }
}
