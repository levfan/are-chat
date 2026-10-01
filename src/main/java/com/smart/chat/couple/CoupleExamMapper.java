package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F284 友情测验数据访问。 */
@Mapper
public interface CoupleExamMapper extends BaseMapperCompat<CoupleExam> {

    /** 题面唯一。 */
    default CoupleExam findQuestion(String spaceId, String question) {
        return selectOne(new LambdaQueryWrapper<CoupleExam>()
                .eq(CoupleExam::getSpaceId, spaceId)
                .eq(CoupleExam::getQuestion, question));
    }

    /** 某人被考的题卷（新→旧）。 */
    default List<CoupleExam> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleExam>()
                .eq(CoupleExam::getSpaceId, spaceId)
                .orderByDesc(CoupleExam::getCreated));
    }
}
