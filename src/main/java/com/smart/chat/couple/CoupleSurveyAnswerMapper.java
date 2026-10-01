package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSurveyAnswerMapper extends BaseMapperCompat<CoupleSurveyAnswer> {

    /** 某人的全部作答（题号升序）。 */
    default List<CoupleSurveyAnswer> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleSurveyAnswer>()
                .eq(CoupleSurveyAnswer::getSpaceId, spaceId)
                .eq(CoupleSurveyAnswer::getFromUser, fromUser)
                .orderByAsc(CoupleSurveyAnswer::getQNo));
    }

    /** 某人某题的作答。 */
    default CoupleSurveyAnswer find(String spaceId, String fromUser, int qNo) {
        return selectOne(new LambdaQueryWrapper<CoupleSurveyAnswer>()
                .eq(CoupleSurveyAnswer::getSpaceId, spaceId)
                .eq(CoupleSurveyAnswer::getFromUser, fromUser)
                .eq(CoupleSurveyAnswer::getQNo, qNo));
    }
}
