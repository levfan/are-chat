package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F307 双角色追剧数据访问。 */
@Mapper
public interface CoupleRoleMovieMapper extends BaseMapperCompat<CoupleRoleMovie> {

    default List<CoupleRoleMovie> findByWork(String spaceId, String work) {
        return selectList(new LambdaQueryWrapper<CoupleRoleMovie>()
                .eq(CoupleRoleMovie::getSpaceId, spaceId)
                .eq(CoupleRoleMovie::getWork, work));
    }

    default List<CoupleRoleMovie> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRoleMovie>()
                .eq(CoupleRoleMovie::getSpaceId, spaceId)
                .orderByDesc(CoupleRoleMovie::getCreated));
    }
}
