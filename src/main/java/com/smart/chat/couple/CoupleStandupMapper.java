package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F276 久坐互拍数据访问。 */
@Mapper
public interface CoupleStandupMapper extends BaseMapperCompat<CoupleStandup> {

    /** 某人某天一拍。 */
    default CoupleStandup find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleStandup>()
                .eq(CoupleStandup::getSpaceId, spaceId)
                .eq(CoupleStandup::getDay, day)
                .eq(CoupleStandup::getFromUser, fromUser));
    }

    /** 近 n 天（同起统计）。 */
    default List<CoupleStandup> findRecent(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleStandup>()
                .eq(CoupleStandup::getSpaceId, spaceId)
                .ge(CoupleStandup::getDay, fromDay));
    }
}
