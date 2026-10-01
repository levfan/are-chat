package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F258 反仪式感日数据访问。 */
@Mapper
public interface CoupleNormalDayMapper extends BaseMapperCompat<CoupleNormalDay> {

    /** 某年全部放空日。 */
    default List<CoupleNormalDay> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleNormalDay>()
                .eq(CoupleNormalDay::getSpaceId, spaceId)
                .eq(CoupleNormalDay::getYear, year)
                .orderByAsc(CoupleNormalDay::getDay));
    }

    /** 某天是否放空日。 */
    default CoupleNormalDay find(String spaceId, String year, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleNormalDay>()
                .eq(CoupleNormalDay::getSpaceId, spaceId)
                .eq(CoupleNormalDay::getYear, year)
                .eq(CoupleNormalDay::getDay, day));
    }
}
