package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * F384 话题断点存档数据访问。
 * 注意：(space_id,from_user,status) 只是普通索引 idx_catch_thread——同一人同一状态可以有多行，
 * 「在途每人 ≤5」靠 findByUserStatus 计数，重复内容靠 find(space,fromUser,topic) 查重。
 */
@Mapper
public interface CoupleCatchThreadMapper extends BaseMapperCompat<CoupleCatchThread> {

    /** 空间全部存档（新的在前，双人时间轴用）。 */
    default List<CoupleCatchThread> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchThread>()
                .eq(CoupleCatchThread::getSpaceId, spaceId)
                .orderByDesc(CoupleCatchThread::getCreated));
    }

    /** 某人某状态的一串（在途计数、列表用，早的在前）。 */
    default List<CoupleCatchThread> findByUserStatus(String spaceId, String fromUser, String status) {
        return selectList(new LambdaQueryWrapper<CoupleCatchThread>()
                .eq(CoupleCatchThread::getSpaceId, spaceId)
                .eq(CoupleCatchThread::getFromUser, fromUser)
                .eq(CoupleCatchThread::getStatus, status)
                .orderByAsc(CoupleCatchThread::getCreated));
    }
}
