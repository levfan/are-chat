package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F335 进城接待方案数据访问。 */
@Mapper
public interface CoupleWorldCityPlanMapper extends BaseMapperCompat<CoupleWorldCityPlan> {

    /** 某城的接待手册（uk 保证最多一条）。 */
    default CoupleWorldCityPlan findByCity(String spaceId, String city) {
        return selectOne(new LambdaQueryWrapper<CoupleWorldCityPlan>()
                .eq(CoupleWorldCityPlan::getSpaceId, spaceId)
                .eq(CoupleWorldCityPlan::getCity, city));
    }

    /** 空间全部手册（最近更新新→旧）。 */
    default List<CoupleWorldCityPlan> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldCityPlan>()
                .eq(CoupleWorldCityPlan::getSpaceId, spaceId)
                .orderByDesc(CoupleWorldCityPlan::getUpdatedAt));
    }
}
