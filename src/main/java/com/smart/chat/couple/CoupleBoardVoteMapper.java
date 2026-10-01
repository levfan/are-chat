package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F241 董事会决议数据访问。 */
@Mapper
public interface CoupleBoardVoteMapper extends BaseMapperCompat<CoupleBoardVote> {

    /** 空间内全部决议（留痕倒序）。 */
    default List<CoupleBoardVote> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBoardVote>()
                .eq(CoupleBoardVote::getSpaceId, spaceId)
                .orderByDesc(CoupleBoardVote::getCreated));
    }
}
