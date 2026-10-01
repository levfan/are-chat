package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F336 亲戚称呼册数据访问。 */
@Mapper
public interface CoupleWorldRelativesQMapper extends BaseMapperCompat<CoupleWorldRelativesQ> {

    /** 某个称谓的一题（uk 保证最多一条）。 */
    default CoupleWorldRelativesQ findByTerm(String spaceId, String term) {
        return selectOne(new LambdaQueryWrapper<CoupleWorldRelativesQ>()
                .eq(CoupleWorldRelativesQ::getSpaceId, spaceId)
                .eq(CoupleWorldRelativesQ::getTerm, term));
    }

    /** 空间全部称谓（错题多的在前）。 */
    default List<CoupleWorldRelativesQ> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldRelativesQ>()
                .eq(CoupleWorldRelativesQ::getSpaceId, spaceId)
                .orderByDesc(CoupleWorldRelativesQ::getWrongCount));
    }
}
