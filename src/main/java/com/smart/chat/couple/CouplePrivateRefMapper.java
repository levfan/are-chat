package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F304 黑话大全数据访问。 */
@Mapper
public interface CouplePrivateRefMapper extends BaseMapperCompat<CouplePrivateRef> {

    default CouplePrivateRef findByTerm(String spaceId, String term) {
        return selectOne(new LambdaQueryWrapper<CouplePrivateRef>()
                .eq(CouplePrivateRef::getSpaceId, spaceId)
                .eq(CouplePrivateRef::getTerm, term));
    }

    default List<CouplePrivateRef> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePrivateRef>()
                .eq(CouplePrivateRef::getSpaceId, spaceId)
                .orderByDesc(CouplePrivateRef::getCreated));
    }
}
