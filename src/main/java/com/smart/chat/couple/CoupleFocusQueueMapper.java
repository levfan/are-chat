package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F362 攒一句话数据访问（在途=read_at 为空）。 */
@Mapper
public interface CoupleFocusQueueMapper extends BaseMapperCompat<CoupleFocusQueue> {

    /** 收件箱：某人收到的全部留言（新的在前）。 */
    default List<CoupleFocusQueue> findByToUser(String spaceId, String toUser) {
        return selectList(new LambdaQueryWrapper<CoupleFocusQueue>()
                .eq(CoupleFocusQueue::getSpaceId, spaceId)
                .eq(CoupleFocusQueue::getToUser, toUser)
                .orderByDesc(CoupleFocusQueue::getCreated));
    }

    /** 某人攒出的全部留言（在途上限用）。 */
    default List<CoupleFocusQueue> findByFromUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleFocusQueue>()
                .eq(CoupleFocusQueue::getSpaceId, spaceId)
                .eq(CoupleFocusQueue::getFromUser, fromUser)
                .orderByDesc(CoupleFocusQueue::getCreated));
    }
}
