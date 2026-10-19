package com.smart.chat.identity.infrastructure.persistence;

import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminAuditMapper extends BaseMapperCompat<AdminAuditPO> {
}
