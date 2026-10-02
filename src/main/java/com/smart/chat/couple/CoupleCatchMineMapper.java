package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F381 雷区探测器数据访问（uk(space_id,from_user,topic) 同人同话题只一行）。 */
@Mapper
public interface CoupleCatchMineMapper extends BaseMapperCompat<CoupleCatchMine> {

    /** 空间全部雷区（新的在前，双方都读同一份）。 */
    default List<CoupleCatchMine> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchMine>()
                .eq(CoupleCatchMine::getSpaceId, spaceId)
                .orderByDesc(CoupleCatchMine::getCreated));
    }

    /** 按 uk 定位那条雷区（写前查重、盖章回填用）。 */
    default CoupleCatchMine find(String spaceId, String fromUser, String topic) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchMine>()
                .eq(CoupleCatchMine::getSpaceId, spaceId)
                .eq(CoupleCatchMine::getFromUser, fromUser)
                .eq(CoupleCatchMine::getTopic, topic));
    }
}
