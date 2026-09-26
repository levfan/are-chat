package com.smart.chat.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementReadMapper extends BaseMapperCompat<AnnouncementRead> {

    default boolean exists(String username, String announcementId) {
        return selectCount(new LambdaQueryWrapper<AnnouncementRead>()
                .eq(AnnouncementRead::getUsername, username)
                .eq(AnnouncementRead::getAnnouncementId, announcementId)) > 0;
    }
}
