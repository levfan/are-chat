package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F240 头衔任命数据访问。 */
@Mapper
public interface CoupleBoardRoleMapper extends BaseMapperCompat<CoupleBoardRole> {

    /** 空间内全部任命。 */
    default List<CoupleBoardRole> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBoardRole>()
                .eq(CoupleBoardRole::getSpaceId, spaceId));
    }
}
