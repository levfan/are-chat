package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F382 安全词使用记录数据访问（uk(space_id,day,user_name) 一天一人一次）。 */
@Mapper
public interface CoupleCatchSafewordUseMapper extends BaseMapperCompat<CoupleCatchSafewordUse> {

    /** 空间全部使用记录（近的在前）。 */
    default List<CoupleCatchSafewordUse> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchSafewordUse>()
                .eq(CoupleCatchSafewordUse::getSpaceId, spaceId)
                .orderByDesc(CoupleCatchSafewordUse::getDay));
    }

    /** 按 uk 定位那天那条（写前查重、复盘回填用）。 */
    default CoupleCatchSafewordUse find(String spaceId, String day, String userName) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchSafewordUse>()
                .eq(CoupleCatchSafewordUse::getSpaceId, spaceId)
                .eq(CoupleCatchSafewordUse::getDay, day)
                .eq(CoupleCatchSafewordUse::getUserName, userName));
    }

    /** 某月使用记录（month 形如 yyyy-MM，按月报计数用，旧的在前）。 */
    default List<CoupleCatchSafewordUse> findByMonth(String spaceId, String month) {
        return selectList(new LambdaQueryWrapper<CoupleCatchSafewordUse>()
                .eq(CoupleCatchSafewordUse::getSpaceId, spaceId)
                .likeRight(CoupleCatchSafewordUse::getDay, month + "-")
                .orderByAsc(CoupleCatchSafewordUse::getDay));
    }
}
