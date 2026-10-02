package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F392 冷笑话结冰榜数据访问（uk(space_id,from_user,content) 一人一句只一行）。 */
@Mapper
public interface CoupleLaughJokeMapper extends BaseMapperCompat<CoupleLaughJoke> {

    /** 空间全部冷笑话（新的在前，结冰榜按人计数用）。 */
    default List<CoupleLaughJoke> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLaughJoke>()
                .eq(CoupleLaughJoke::getSpaceId, spaceId)
                .orderByDesc(CoupleLaughJoke::getCreated));
    }

    /** 按 uk 定位那一条（写前查重、判分与默契考前回填用）。 */
    default CoupleLaughJoke find(String spaceId, String fromUser, String content) {
        return selectOne(new LambdaQueryWrapper<CoupleLaughJoke>()
                .eq(CoupleLaughJoke::getSpaceId, spaceId)
                .eq(CoupleLaughJoke::getFromUser, fromUser)
                .eq(CoupleLaughJoke::getContent, content));
    }
}
