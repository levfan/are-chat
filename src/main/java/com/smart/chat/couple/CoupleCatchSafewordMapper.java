package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F382 安全词约定数据访问（uk(space_id,from_user) 每人一行，可改写）。 */
@Mapper
public interface CoupleCatchSafewordMapper extends BaseMapperCompat<CoupleCatchSafeword> {

    /** 空间两个安全词（与安全词使用记录无关，按约定先后升序）。 */
    default List<CoupleCatchSafeword> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchSafeword>()
                .eq(CoupleCatchSafeword::getSpaceId, spaceId)
                .orderByAsc(CoupleCatchSafeword::getCreated));
    }

    /** 某人约定的那个词（upsert 定位用）。 */
    default CoupleCatchSafeword find(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchSafeword>()
                .eq(CoupleCatchSafeword::getSpaceId, spaceId)
                .eq(CoupleCatchSafeword::getFromUser, fromUser));
    }
}
