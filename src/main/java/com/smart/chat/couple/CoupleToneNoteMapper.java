package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F267 语气翻译数据访问。 */
@Mapper
public interface CoupleToneNoteMapper extends BaseMapperCompat<CoupleToneNote> {

    /** 某人某天一条。 */
    default CoupleToneNote find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleToneNote>()
                .eq(CoupleToneNote::getSpaceId, spaceId)
                .eq(CoupleToneNote::getDay, day)
                .eq(CoupleToneNote::getFromUser, fromUser));
    }

    /** 某天双方语气。 */
    default List<CoupleToneNote> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleToneNote>()
                .eq(CoupleToneNote::getSpaceId, spaceId)
                .eq(CoupleToneNote::getDay, day));
    }
}
