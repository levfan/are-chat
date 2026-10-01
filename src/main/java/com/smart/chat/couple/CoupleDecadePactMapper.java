package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDecadePactMapper extends BaseMapperCompat<CoupleDecadePact> {

    /** 空间的十年之约（每人一条）。 */
    default List<CoupleDecadePact> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDecadePact>()
                .eq(CoupleDecadePact::getSpaceId, spaceId)
                .orderByAsc(CoupleDecadePact::getCreated));
    }

    /** 某人的十年之约。 */
    default CoupleDecadePact findByUser(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleDecadePact>()
                .eq(CoupleDecadePact::getSpaceId, spaceId)
                .eq(CoupleDecadePact::getFromUser, fromUser));
    }
}
