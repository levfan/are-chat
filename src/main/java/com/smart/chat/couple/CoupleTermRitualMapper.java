package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F251 节气过法数据访问。 */
@Mapper
public interface CoupleTermRitualMapper extends BaseMapperCompat<CoupleTermRitual> {

    /** 某节气的全部过法。 */
    default List<CoupleTermRitual> findByTerm(String spaceId, String term) {
        return selectList(new LambdaQueryWrapper<CoupleTermRitual>()
                .eq(CoupleTermRitual::getSpaceId, spaceId)
                .eq(CoupleTermRitual::getTerm, term));
    }

    /** 全部过法（年度小结聚合用）。 */
    default List<CoupleTermRitual> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTermRitual>()
                .eq(CoupleTermRitual::getSpaceId, spaceId));
    }
}
