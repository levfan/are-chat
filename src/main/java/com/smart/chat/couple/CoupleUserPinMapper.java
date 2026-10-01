package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleUserPinMapper extends BaseMapperCompat<CoupleUserPin> {

    /** 某人的收藏行。 */
    default CoupleUserPin find(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleUserPin>()
                .eq(CoupleUserPin::getSpaceId, spaceId)
                .eq(CoupleUserPin::getFromUser, fromUser));
    }

    /** 空间内全部收藏行。 */
    default List<CoupleUserPin> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleUserPin>()
                .eq(CoupleUserPin::getSpaceId, spaceId));
    }
}
