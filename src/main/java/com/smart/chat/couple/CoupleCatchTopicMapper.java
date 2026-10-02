package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F387 话题许愿池数据访问（uk(space_id,from_user,title) 同人同话题只一行）。 */
@Mapper
public interface CoupleCatchTopicMapper extends BaseMapperCompat<CoupleCatchTopic> {

    /** 空间全部许愿（新的在前，总览用）。 */
    default List<CoupleCatchTopic> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchTopic>()
                .eq(CoupleCatchTopic::getSpaceId, spaceId)
                .orderByDesc(CoupleCatchTopic::getCreated));
    }

    /** 按 uk 定位那条（写前查重、接单/聊完回填用）。 */
    default CoupleCatchTopic find(String spaceId, String fromUser, String title) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchTopic>()
                .eq(CoupleCatchTopic::getSpaceId, spaceId)
                .eq(CoupleCatchTopic::getFromUser, fromUser)
                .eq(CoupleCatchTopic::getTitle, title));
    }

    /** 某状态的一串（TAKEN 按接单时间升序=最早接单的先催，一周期限从 takenAt 起算）。 */
    default List<CoupleCatchTopic> findByStatus(String spaceId, String status) {
        return selectList(new LambdaQueryWrapper<CoupleCatchTopic>()
                .eq(CoupleCatchTopic::getSpaceId, spaceId)
                .eq(CoupleCatchTopic::getStatus, status)
                .orderByAsc(CoupleCatchTopic::getTakenAt));
    }
}
