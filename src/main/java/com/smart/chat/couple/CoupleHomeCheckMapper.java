package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F279 家安月检数据访问。 */
@Mapper
public interface CoupleHomeCheckMapper extends BaseMapperCompat<CoupleHomeCheck> {

    /** 某人某月一份。 */
    default CoupleHomeCheck find(String spaceId, String month, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleHomeCheck>()
                .eq(CoupleHomeCheck::getSpaceId, spaceId)
                .eq(CoupleHomeCheck::getMonth, month)
                .eq(CoupleHomeCheck::getFromUser, fromUser));
    }

    /** 某月双份。 */
    default List<CoupleHomeCheck> findByMonth(String spaceId, String month) {
        return selectList(new LambdaQueryWrapper<CoupleHomeCheck>()
                .eq(CoupleHomeCheck::getSpaceId, spaceId)
                .eq(CoupleHomeCheck::getMonth, month));
    }
}
