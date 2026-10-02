package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F375 搬家八区块数据访问（uk(space_id,slot) 一格只一行）。 */
@Mapper
public interface CoupleQuestMoveMapper extends BaseMapperCompat<CoupleQuestMove> {

    /** 空间全部区块（区块位升序，八格看板用）。 */
    default List<CoupleQuestMove> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestMove>()
                .eq(CoupleQuestMove::getSpaceId, spaceId)
                .orderByAsc(CoupleQuestMove::getSlot));
    }

    /** 某个区块位那一行（按 uk 取，没有=还没开这一格）。 */
    default CoupleQuestMove findBySlot(String spaceId, int slot) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestMove>()
                .eq(CoupleQuestMove::getSpaceId, spaceId)
                .eq(CoupleQuestMove::getSlot, slot));
    }
}
