package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F333 官宣日数据访问。 */
@Mapper
public interface CoupleWorldDeclareMapper extends BaseMapperCompat<CoupleWorldDeclare> {

    /** 某月的官宣卡（uk 保证最多一条）。 */
    default CoupleWorldDeclare findByMonth(String spaceId, String month) {
        return selectOne(new LambdaQueryWrapper<CoupleWorldDeclare>()
                .eq(CoupleWorldDeclare::getSpaceId, spaceId)
                .eq(CoupleWorldDeclare::getMonth, month));
    }

    /** 空间官宣编年（官宣月新→旧）。 */
    default List<CoupleWorldDeclare> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldDeclare>()
                .eq(CoupleWorldDeclare::getSpaceId, spaceId)
                .orderByDesc(CoupleWorldDeclare::getMonth));
    }
}
