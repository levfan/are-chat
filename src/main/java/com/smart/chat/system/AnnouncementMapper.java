package com.smart.chat.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AnnouncementMapper extends BaseMapperCompat<Announcement> {

    default Optional<Announcement> findLatestEnabled() {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getEnabled, true)
                .orderByDesc(Announcement::getCreated)
                .last("LIMIT 1")));
    }

    default List<Announcement> findAllOrdered() {
        return selectList(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getCreated)
                .last("LIMIT 100"));
    }
}
