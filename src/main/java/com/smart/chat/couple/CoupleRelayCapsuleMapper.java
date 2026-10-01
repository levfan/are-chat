package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F296 胶囊接龙数据访问。 */
@Mapper
public interface CoupleRelayCapsuleMapper extends BaseMapperCompat<CoupleRelayCapsule> {

    /** 到点未拆、属于收信人的笔（按开启日升序，一次只能拆最早的）。 */
    default List<CoupleRelayCapsule> findDue(String spaceId, String today) {
        return selectList(new LambdaQueryWrapper<CoupleRelayCapsule>()
                .eq(CoupleRelayCapsule::getSpaceId, spaceId)
                .eq(CoupleRelayCapsule::getStatus, "SEALED")
                .le(CoupleRelayCapsule::getOpenDay, today)
                .orderByAsc(CoupleRelayCapsule::getOpenDay));
    }

    /** 全部（新→旧）。 */
    default List<CoupleRelayCapsule> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRelayCapsule>()
                .eq(CoupleRelayCapsule::getSpaceId, spaceId)
                .orderByAsc(CoupleRelayCapsule::getOpenDay));
    }
}
