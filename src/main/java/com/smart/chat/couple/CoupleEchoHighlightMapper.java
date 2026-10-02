package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F355 高光重放数据访问。 */
@Mapper
public interface CoupleEchoHighlightMapper extends BaseMapperCompat<CoupleEchoHighlight> {

    /** 某人的精选夹（新的在前，上限判定用）。 */
    default List<CoupleEchoHighlight> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleEchoHighlight>()
                .eq(CoupleEchoHighlight::getSpaceId, spaceId)
                .eq(CoupleEchoHighlight::getFromUser, fromUser)
                .orderByDesc(CoupleEchoHighlight::getCreated));
    }

    /** 空间双方高光（总览/补给用）。 */
    default List<CoupleEchoHighlight> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleEchoHighlight>()
                .eq(CoupleEchoHighlight::getSpaceId, spaceId)
                .orderByDesc(CoupleEchoHighlight::getCreated));
    }
}
