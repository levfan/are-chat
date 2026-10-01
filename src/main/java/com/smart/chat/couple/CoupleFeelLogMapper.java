package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleFeelLogMapper extends BaseMapperCompat<CoupleFeelLog> {

    /** 某人某天的情绪记录。 */
    default CoupleFeelLog find(String spaceId, String fromUser, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleFeelLog>()
                .eq(CoupleFeelLog::getSpaceId, spaceId)
                .eq(CoupleFeelLog::getFromUser, fromUser)
                .eq(CoupleFeelLog::getDay, day));
    }

    /** 全部情绪日记（按创建倒序）。 */
    default List<CoupleFeelLog> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFeelLog>()
                .eq(CoupleFeelLog::getSpaceId, spaceId)
                .orderByDesc(CoupleFeelLog::getCreated));
    }
}
