package com.smart.chat.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminAuditMapper extends BaseMapperCompat<AdminAuditPO> {

    default List<AdminAuditPO> findLatest(int limit) {
        return selectList(new LambdaQueryWrapper<AdminAuditPO>()
                .orderByDesc(AdminAuditPO::getCreated)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200))));
    }
}
