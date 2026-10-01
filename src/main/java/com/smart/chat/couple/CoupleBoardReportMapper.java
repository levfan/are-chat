package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F242 年度述职数据访问。 */
@Mapper
public interface CoupleBoardReportMapper extends BaseMapperCompat<CoupleBoardReport> {

    /** 某人某年的一份述职。 */
    default CoupleBoardReport find(String spaceId, String year, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleBoardReport>()
                .eq(CoupleBoardReport::getSpaceId, spaceId)
                .eq(CoupleBoardReport::getYear, year)
                .eq(CoupleBoardReport::getFromUser, fromUser));
    }

    /** 空间内全部述职。 */
    default List<CoupleBoardReport> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBoardReport>()
                .eq(CoupleBoardReport::getSpaceId, spaceId));
    }
}
