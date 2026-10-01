package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F286 第一眼对视数据访问。 */
@Mapper
public interface CoupleFirstLookMapper extends BaseMapperCompat<CoupleFirstLook> {

    /** 某人一条。 */
    default CoupleFirstLook findUser(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleFirstLook>()
                .eq(CoupleFirstLook::getSpaceId, spaceId)
                .eq(CoupleFirstLook::getFromUser, fromUser));
    }

    /** 双方两条。 */
    default List<CoupleFirstLook> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFirstLook>()
                .eq(CoupleFirstLook::getSpaceId, spaceId));
    }
}
