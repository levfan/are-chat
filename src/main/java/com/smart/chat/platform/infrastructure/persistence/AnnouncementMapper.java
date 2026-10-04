package com.smart.chat.platform.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 公告表 Mapper：只认 {@link AnnouncementPO}，不感知领域类型（翻译在仓储适配器里）。
 * 两个 default 读法的 SQL 口径与改造前逐字一致（生效公告取最新一条、列表取最近 100 条倒序）。
 */
@Mapper
public interface AnnouncementMapper extends BaseMapperCompat<AnnouncementPO> {

    default Optional<AnnouncementPO> findLatestEnabled() {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<AnnouncementPO>()
                .eq(AnnouncementPO::getEnabled, true)
                .orderByDesc(AnnouncementPO::getCreated)
                .last("LIMIT 1")));
    }

    default List<AnnouncementPO> findAllOrdered() {
        return selectList(new LambdaQueryWrapper<AnnouncementPO>()
                .orderByDesc(AnnouncementPO::getCreated)
                .last("LIMIT 100"));
    }
}
