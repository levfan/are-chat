package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F289 人格双报数据访问。 */
@Mapper
public interface CoupleTypeReportMapper extends BaseMapperCompat<CoupleTypeReport> {

    /** 某人某年一报。 */
    default CoupleTypeReport find(String spaceId, String year, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleTypeReport>()
                .eq(CoupleTypeReport::getSpaceId, spaceId)
                .eq(CoupleTypeReport::getYear, year)
                .eq(CoupleTypeReport::getFromUser, fromUser));
    }

    /** 全部年报（新→旧）。 */
    default List<CoupleTypeReport> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTypeReport>()
                .eq(CoupleTypeReport::getSpaceId, spaceId)
                .orderByDesc(CoupleTypeReport::getYear));
    }
}
