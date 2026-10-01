package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F313 戒烟戒糖互助营数据访问。 */
@Mapper
public interface CoupleBodyQuitMapper extends BaseMapperCompat<CoupleBodyQuit> {

    /** 在营中的目标（开营日升序）。 */
    default List<CoupleBodyQuit> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBodyQuit>()
                .eq(CoupleBodyQuit::getSpaceId, spaceId)
                .eq(CoupleBodyQuit::getStatus, CoupleBodyQuit.STATUS_OPEN)
                .orderByAsc(CoupleBodyQuit::getStartDay));
    }

    /** 同空间同 OWNER 的目标名（uk 保证最多一条）。 */
    default CoupleBodyQuit findOwnerName(String spaceId, String ownerUser, String name) {
        return selectOne(new LambdaQueryWrapper<CoupleBodyQuit>()
                .eq(CoupleBodyQuit::getSpaceId, spaceId)
                .eq(CoupleBodyQuit::getOwnerUser, ownerUser)
                .eq(CoupleBodyQuit::getName, name));
    }

    /** 空间全部目标（新→旧）。 */
    default List<CoupleBodyQuit> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBodyQuit>()
                .eq(CoupleBodyQuit::getSpaceId, spaceId)
                .orderByDesc(CoupleBodyQuit::getCreated));
    }
}
