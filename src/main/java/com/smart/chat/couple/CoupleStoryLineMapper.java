package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleStoryLineMapper extends BaseMapperCompat<CoupleStoryLine> {

    /** 某条故事链的句子（按句序）。 */
    default List<CoupleStoryLine> findByChain(String spaceId, String chainId) {
        return selectList(new LambdaQueryWrapper<CoupleStoryLine>()
                .eq(CoupleStoryLine::getSpaceId, spaceId)
                .eq(CoupleStoryLine::getChainId, chainId)
                .orderByAsc(CoupleStoryLine::getSeq));
    }

    /** 空间最近的句子（倒序，前端按链聚合）。 */
    default List<CoupleStoryLine> findRecent(String spaceId, int limit) {
        return selectList(new LambdaQueryWrapper<CoupleStoryLine>()
                .eq(CoupleStoryLine::getSpaceId, spaceId)
                .orderByDesc(CoupleStoryLine::getCreated)
                .last("LIMIT " + Math.max(1, limit)));
    }

    /** 是否存在未完结的故事链。 */
    default CoupleStoryLine findLastOfChain(String spaceId, String chainId) {
        return selectOne(new LambdaQueryWrapper<CoupleStoryLine>()
                .eq(CoupleStoryLine::getSpaceId, spaceId)
                .eq(CoupleStoryLine::getChainId, chainId)
                .orderByDesc(CoupleStoryLine::getSeq)
                .last("LIMIT 1"));
    }
}
