package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleCipherMapper extends BaseMapperCompat<CoupleCipher> {

    /** 某空间全部暗号（新→旧）。 */
    default List<CoupleCipher> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCipher>()
                .eq(CoupleCipher::getSpaceId, spaceId)
                .orderByDesc(CoupleCipher::getCreated));
    }
}
