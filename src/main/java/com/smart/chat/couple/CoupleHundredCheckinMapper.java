package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHundredCheckinMapper extends BaseMapperCompat<CoupleHundredCheckin> {

    /** 某约定全部打卡（按日期升序）。 */
    default List<CoupleHundredCheckin> findByPact(String pactId) {
        return selectList(new LambdaQueryWrapper<CoupleHundredCheckin>()
                .eq(CoupleHundredCheckin::getPactId, pactId)
                .orderByAsc(CoupleHundredCheckin::getDay));
    }

    /** 某人某天的打卡记录。 */
    default CoupleHundredCheckin find(String pactId, String byUser, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleHundredCheckin>()
                .eq(CoupleHundredCheckin::getPactId, pactId)
                .eq(CoupleHundredCheckin::getByUser, byUser)
                .eq(CoupleHundredCheckin::getDay, day));
    }
}
