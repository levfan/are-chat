package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F283 外号考据数据访问。 */
@Mapper
public interface CouplePetnameStoryMapper extends BaseMapperCompat<CouplePetnameStory> {

    /** 某外号一条。 */
    default CouplePetnameStory findNick(String spaceId, String nickname) {
        return selectOne(new LambdaQueryWrapper<CouplePetnameStory>()
                .eq(CouplePetnameStory::getSpaceId, spaceId)
                .eq(CouplePetnameStory::getNickname, nickname));
    }

    /** 全部故事（新→旧）。 */
    default List<CouplePetnameStory> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePetnameStory>()
                .eq(CouplePetnameStory::getSpaceId, spaceId)
                .orderByDesc(CouplePetnameStory::getCreated));
    }
}
