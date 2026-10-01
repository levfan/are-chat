package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F244 发薪日数据访问。 */
@Mapper
public interface CoupleBoardSalaryMapper extends BaseMapperCompat<CoupleBoardSalary> {

    /** 某人某月的一次发薪。 */
    default CoupleBoardSalary find(String spaceId, String month, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleBoardSalary>()
                .eq(CoupleBoardSalary::getSpaceId, spaceId)
                .eq(CoupleBoardSalary::getMonth, month)
                .eq(CoupleBoardSalary::getFromUser, fromUser));
    }

    /** 空间内全部发薪。 */
    default List<CoupleBoardSalary> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBoardSalary>()
                .eq(CoupleBoardSalary::getSpaceId, spaceId));
    }
}
