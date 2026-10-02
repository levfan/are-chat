package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F383 敏感日历数据访问（uk(space_id,owner_user,day,kind) 同日同类型只一行）。 */
@Mapper
public interface CoupleCatchSensitiveMapper extends BaseMapperCompat<CoupleCatchSensitive> {

    /** 空间全部标注（日子升序，日历铺排用）。 */
    default List<CoupleCatchSensitive> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchSensitive>()
                .eq(CoupleCatchSensitive::getSpaceId, spaceId)
                .orderByAsc(CoupleCatchSensitive::getDay));
    }

    /** 按 uk 定位那一条（写前查重用）。 */
    default CoupleCatchSensitive find(String spaceId, String ownerUser, String day, String kind) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchSensitive>()
                .eq(CoupleCatchSensitive::getSpaceId, spaceId)
                .eq(CoupleCatchSensitive::getOwnerUser, ownerUser)
                .eq(CoupleCatchSensitive::getDay, day)
                .eq(CoupleCatchSensitive::getKind, kind));
    }

    /** 某一天双方的标注（当天提醒/总览用）。 */
    default List<CoupleCatchSensitive> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleCatchSensitive>()
                .eq(CoupleCatchSensitive::getSpaceId, spaceId)
                .eq(CoupleCatchSensitive::getDay, day));
    }
}
