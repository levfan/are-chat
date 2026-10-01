package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F300 今日身份签数据访问。 */
@Mapper
public interface CoupleRoleDayMapper extends BaseMapperCompat<CoupleRoleDay> {

    default CoupleRoleDay findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleRoleDay>()
                .eq(CoupleRoleDay::getSpaceId, spaceId)
                .eq(CoupleRoleDay::getDay, day));
    }

    /** 近期身份签（新→旧）。 */
    default List<CoupleRoleDay> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRoleDay>()
                .eq(CoupleRoleDay::getSpaceId, spaceId)
                .orderByDesc(CoupleRoleDay::getDay));
    }
}
