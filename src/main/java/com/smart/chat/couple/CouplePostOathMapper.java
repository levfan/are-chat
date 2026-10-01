package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F290 新年卡数据访问。 */
@Mapper
public interface CouplePostOathMapper extends BaseMapperCompat<CouplePostOath> {

    /** 待放行的到期卡（SEALED 且投递日已到）。 */
    default List<CouplePostOath> findDue(String spaceId, String today) {
        return selectList(new LambdaQueryWrapper<CouplePostOath>()
                .eq(CouplePostOath::getSpaceId, spaceId)
                .eq(CouplePostOath::getStatus, "SEALED")
                .le(CouplePostOath::getDeliverDay, today));
    }

    /** 全部年卡（新年→旧年）。 */
    default List<CouplePostOath> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePostOath>()
                .eq(CouplePostOath::getSpaceId, spaceId)
                .orderByDesc(CouplePostOath::getYear));
    }
}
