package com.smart.chat.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminAuditMapper extends BaseMapperCompat<AdminAudit> {

    default List<AdminAudit> findLatest(int limit) {
        return selectList(new LambdaQueryWrapper<AdminAudit>()
                .orderByDesc(AdminAudit::getCreated)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200))));
    }
}
