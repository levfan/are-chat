package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F386 聆听方式协议数据访问（uk(space_id,from_user) 每人一行，可改写）。 */
@Mapper
public interface CoupleCatchProtocolMapper extends BaseMapperCompat<CoupleCatchProtocol> {

    /** 空间两份协议（写的先后升序，双方对照展示用）。 */
    default List<CoupleCatchProtocol> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchProtocol>()
                .eq(CoupleCatchProtocol::getSpaceId, spaceId)
                .orderByAsc(CoupleCatchProtocol::getCreated));
    }

    /** 某人那份协议（upsert 定位用）。 */
    default CoupleCatchProtocol find(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchProtocol>()
                .eq(CoupleCatchProtocol::getSpaceId, spaceId)
                .eq(CoupleCatchProtocol::getFromUser, fromUser));
    }
}
