package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F393 尴尬回收站数据访问（uk(space_id,day,from_user) 同人同社死日只一行）。 */
@Mapper
public interface CoupleLaughCringeMapper extends BaseMapperCompat<CoupleLaughCringe> {

    /** 空间全部社死记录（日子降序，翻旧账用）。 */
    default List<CoupleLaughCringe> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLaughCringe>()
                .eq(CoupleLaughCringe::getSpaceId, spaceId)
                .orderByDesc(CoupleLaughCringe::getDay));
    }

    /** 按 uk 定位那一条（写前查重、盖章回填用）。 */
    default CoupleLaughCringe find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleLaughCringe>()
                .eq(CoupleLaughCringe::getSpaceId, spaceId)
                .eq(CoupleLaughCringe::getDay, day)
                .eq(CoupleLaughCringe::getFromUser, fromUser));
    }
}
