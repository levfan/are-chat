package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F328 修复礼盒数据访问。 */
@Mapper
public interface CoupleRepairBoxMapper extends BaseMapperCompat<CoupleRepairBox> {

    /** 待完成的补偿任务盒（登记时间老→新）。 */
    default List<CoupleRepairBox> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRepairBox>()
                .eq(CoupleRepairBox::getSpaceId, spaceId)
                .eq(CoupleRepairBox::getStatus, CoupleRepairBox.STATUS_OPEN)
                .orderByAsc(CoupleRepairBox::getCreated));
    }

    /** 某人某天的礼盒（uk 保证最多一条）。 */
    default CoupleRepairBox findByDayUser(String spaceId, String day, String ownerUser) {
        return selectOne(new LambdaQueryWrapper<CoupleRepairBox>()
                .eq(CoupleRepairBox::getSpaceId, spaceId)
                .eq(CoupleRepairBox::getDay, day)
                .eq(CoupleRepairBox::getOwnerUser, ownerUser));
    }

    /** 空间全部礼盒（掉盒日新→旧）。 */
    default List<CoupleRepairBox> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRepairBox>()
                .eq(CoupleRepairBox::getSpaceId, spaceId)
                .orderByDesc(CoupleRepairBox::getDay));
    }
}
