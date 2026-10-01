package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F250 节气跟风打卡数据访问。 */
@Mapper
public interface CoupleTermCheckMapper extends BaseMapperCompat<CoupleTermCheck> {

    /** 某人某年某节气的一记。 */
    default CoupleTermCheck find(String spaceId, String term, String year, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleTermCheck>()
                .eq(CoupleTermCheck::getSpaceId, spaceId)
                .eq(CoupleTermCheck::getTerm, term)
                .eq(CoupleTermCheck::getYear, year)
                .eq(CoupleTermCheck::getFromUser, fromUser));
    }

    /** 某年全部跟风记录。 */
    default List<CoupleTermCheck> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleTermCheck>()
                .eq(CoupleTermCheck::getSpaceId, spaceId)
                .eq(CoupleTermCheck::getYear, year));
    }
}
