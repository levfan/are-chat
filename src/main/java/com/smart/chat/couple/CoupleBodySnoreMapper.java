package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F311 呼噜自报数据访问。 */
@Mapper
public interface CoupleBodySnoreMapper extends BaseMapperCompat<CoupleBodySnore> {

    /** 当日一行（两人共用一行，uk 保证最多一条）。 */
    default CoupleBodySnore findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleBodySnore>()
                .eq(CoupleBodySnore::getSpaceId, spaceId)
                .eq(CoupleBodySnore::getDay, day));
    }

    /** fromDay 起的档位记录（旧→新，连击统计用）。 */
    default List<CoupleBodySnore> findRecent(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleBodySnore>()
                .eq(CoupleBodySnore::getSpaceId, spaceId)
                .ge(CoupleBodySnore::getDay, fromDay)
                .orderByAsc(CoupleBodySnore::getDay));
    }
}
