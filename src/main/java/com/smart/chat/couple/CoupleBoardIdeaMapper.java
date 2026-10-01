package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F245 金点子箱数据访问。 */
@Mapper
public interface CoupleBoardIdeaMapper extends BaseMapperCompat<CoupleBoardIdea> {

    /** 空间内全部点子。 */
    default List<CoupleBoardIdea> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBoardIdea>()
                .eq(CoupleBoardIdea::getSpaceId, spaceId));
    }
}
