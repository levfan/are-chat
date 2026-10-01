package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F342 续约发布会数据访问。 */
@Mapper
public interface CoupleLegacySpeechMapper extends BaseMapperCompat<CoupleLegacySpeech> {

    /** 某年发言稿（双方各一条，按人排）。 */
    default List<CoupleLegacySpeech> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleLegacySpeech>()
                .eq(CoupleLegacySpeech::getSpaceId, spaceId)
                .eq(CoupleLegacySpeech::getYear, year)
                .orderByAsc(CoupleLegacySpeech::getFromUser));
    }

    /** 空间全部发言稿（年份新→旧）。 */
    default List<CoupleLegacySpeech> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLegacySpeech>()
                .eq(CoupleLegacySpeech::getSpaceId, spaceId)
                .orderByDesc(CoupleLegacySpeech::getYear));
    }
}
