package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper extends BaseMapperCompat<UserProfile> {
}
