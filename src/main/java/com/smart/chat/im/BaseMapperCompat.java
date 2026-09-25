package com.smart.chat.im;

import org.apache.ibatis.annotations.Mapper;

/**
 * MP 3.5.17 拆包后 BaseMapper 位于 com.baomidou.mybatisplus.core.mapper，
 * 为省样板代码统一在这里继承一次。
 */
public interface BaseMapperCompat<T> extends com.baomidou.mybatisplus.core.mapper.BaseMapper<T> {
}
