package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F252 择吉日数据访问。 */
@Mapper
public interface CoupleLuckyDayMapper extends BaseMapperCompat<CoupleLuckyDay> {

    /** 同一天同一事的唯一记录。 */
    default CoupleLuckyDay find(String spaceId, String day, String matter) {
        return selectOne(new LambdaQueryWrapper<CoupleLuckyDay>()
                .eq(CoupleLuckyDay::getSpaceId, spaceId)
                .eq(CoupleLuckyDay::getDay, day)
                .eq(CoupleLuckyDay::getMatter, matter));
    }

    /** 未来的吉日（含今天，按日期升序）。 */
    default List<CoupleLuckyDay> findUpcoming(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleLuckyDay>()
                .eq(CoupleLuckyDay::getSpaceId, spaceId)
                .ge(CoupleLuckyDay::getDay, fromDay)
                .orderByAsc(CoupleLuckyDay::getDay));
    }

    /** 空间全部吉日。 */
    default List<CoupleLuckyDay> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLuckyDay>()
                .eq(CoupleLuckyDay::getSpaceId, spaceId));
    }
}
