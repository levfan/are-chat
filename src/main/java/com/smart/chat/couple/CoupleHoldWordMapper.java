package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F265 早想说数据访问。 */
@Mapper
public interface CoupleHoldWordMapper extends BaseMapperCompat<CoupleHoldWord> {

    /** 已到放行日仍封存的（按放行日升序，队列头部在前）。 */
    default List<CoupleHoldWord> findDue(String spaceId, String today) {
        return selectList(new LambdaQueryWrapper<CoupleHoldWord>()
                .eq(CoupleHoldWord::getSpaceId, spaceId)
                .eq(CoupleHoldWord::getStatus, CoupleHoldWord.STATUS_HELD)
                .le(CoupleHoldWord::getOpenDay, today)
                .orderByAsc(CoupleHoldWord::getOpenDay));
    }

    /** 仍在封存队列的下一条（最早放行日）。 */
    default CoupleHoldWord findNextHeld(String spaceId, String today) {
        return selectOne(new LambdaQueryWrapper<CoupleHoldWord>()
                .eq(CoupleHoldWord::getSpaceId, spaceId)
                .eq(CoupleHoldWord::getStatus, CoupleHoldWord.STATUS_HELD)
                .gt(CoupleHoldWord::getOpenDay, today)
                .orderByAsc(CoupleHoldWord::getOpenDay)
                .last("limit 1"));
    }

    /** 全部（新→旧）。 */
    default List<CoupleHoldWord> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHoldWord>()
                .eq(CoupleHoldWord::getSpaceId, spaceId)
                .orderByDesc(CoupleHoldWord::getCreated));
    }
}
