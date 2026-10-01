package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleMorningNoteMapper extends BaseMapperCompat<CoupleMorningNote> {

    /** 某人的全部留言（按创建倒序）。 */
    default List<CoupleMorningNote> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleMorningNote>()
                .eq(CoupleMorningNote::getSpaceId, spaceId)
                .eq(CoupleMorningNote::getFromUser, fromUser)
                .orderByDesc(CoupleMorningNote::getCreated));
    }

    /** 送达给某人的留言（deliver_day <= today，按日期倒序）。 */
    default List<CoupleMorningNote> findDelivered(String spaceId, String toUser, String today) {
        return selectList(new LambdaQueryWrapper<CoupleMorningNote>()
                .eq(CoupleMorningNote::getSpaceId, spaceId)
                .ne(CoupleMorningNote::getFromUser, toUser)
                .le(CoupleMorningNote::getDeliverDay, today)
                .orderByDesc(CoupleMorningNote::getDeliverDay));
    }
}
