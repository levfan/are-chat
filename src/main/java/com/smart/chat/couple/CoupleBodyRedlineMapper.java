package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F316 忌口红线本数据访问。 */
@Mapper
public interface CoupleBodyRedlineMapper extends BaseMapperCompat<CoupleBodyRedline> {

    /** 同空间同名忌口项（uk 保证最多一条）。 */
    default CoupleBodyRedline findByItem(String spaceId, String item) {
        return selectOne(new LambdaQueryWrapper<CoupleBodyRedline>()
                .eq(CoupleBodyRedline::getSpaceId, spaceId)
                .eq(CoupleBodyRedline::getItem, item));
    }

    /** 空间全部红线（登记先后升序，饭桌拼标用）。 */
    default List<CoupleBodyRedline> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBodyRedline>()
                .eq(CoupleBodyRedline::getSpaceId, spaceId)
                .orderByAsc(CoupleBodyRedline::getCreated));
    }
}
