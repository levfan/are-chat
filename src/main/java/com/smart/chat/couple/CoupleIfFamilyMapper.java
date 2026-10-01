package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F306 如果我是你爸妈数据访问。 */
@Mapper
public interface CoupleIfFamilyMapper extends BaseMapperCompat<CoupleIfFamily> {

    default CoupleIfFamily findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleIfFamily>()
                .eq(CoupleIfFamily::getSpaceId, spaceId)
                .eq(CoupleIfFamily::getDay, day));
    }

    default List<CoupleIfFamily> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleIfFamily>()
                .eq(CoupleIfFamily::getSpaceId, spaceId)
                .orderByDesc(CoupleIfFamily::getDay));
    }
}
