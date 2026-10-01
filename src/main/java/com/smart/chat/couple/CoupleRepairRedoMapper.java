package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F322 重来卡数据访问。 */
@Mapper
public interface CoupleRepairRedoMapper extends BaseMapperCompat<CoupleRepairRedo> {

    /** 某人某季的那张卡（uk 保证最多一条）。 */
    default CoupleRepairRedo findByQuarter(String spaceId, String quarter) {
        return selectOne(new LambdaQueryWrapper<CoupleRepairRedo>()
                .eq(CoupleRepairRedo::getSpaceId, spaceId)
                .eq(CoupleRepairRedo::getQuarter, quarter));
    }

    /** 空间全部重来卡（季度新→旧）。 */
    default List<CoupleRepairRedo> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRepairRedo>()
                .eq(CoupleRepairRedo::getSpaceId, spaceId)
                .orderByDesc(CoupleRepairRedo::getQuarter));
    }
}
