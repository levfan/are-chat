package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F257 长假愿望数据访问。 */
@Mapper
public interface CoupleHolidayWishMapper extends BaseMapperCompat<CoupleHolidayWish> {

    /** 某个假期的一条愿望。 */
    default CoupleHolidayWish find(String spaceId, String holiday) {
        return selectOne(new LambdaQueryWrapper<CoupleHolidayWish>()
                .eq(CoupleHolidayWish::getSpaceId, spaceId)
                .eq(CoupleHolidayWish::getHoliday, holiday));
    }

    /** 空间全部假期愿望。 */
    default List<CoupleHolidayWish> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHolidayWish>()
                .eq(CoupleHolidayWish::getSpaceId, spaceId));
    }
}
