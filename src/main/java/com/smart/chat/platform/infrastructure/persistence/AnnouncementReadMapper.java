package com.smart.chat.platform.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementReadMapper extends BaseMapperCompat<AnnouncementRead> {

    default boolean exists(String username, String announcementId) {
        return selectCount(new LambdaQueryWrapper<AnnouncementRead>()
                .eq(AnnouncementRead::getUsername, username)
                .eq(AnnouncementRead::getAnnouncementId, announcementId)) > 0;
    }
}
