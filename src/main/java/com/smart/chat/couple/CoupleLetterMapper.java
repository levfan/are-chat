package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleLetterMapper extends BaseMapperCompat<CoupleLetter> {

    /** 某空间全部信件（信箱列表，新→旧）。 */
    default List<CoupleLetter> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLetter>()
                .eq(CoupleLetter::getSpaceId, spaceId)
                .orderByDesc(CoupleLetter::getCreated));
    }

    /** 我的可拆未拆信件数（overview 未读红点）：收件人是我、还没拆、已到可拆时间。 */
    default long countOpenable(String spaceId, String recipient, long now) {
        return selectCount(new LambdaQueryWrapper<CoupleLetter>()
                .eq(CoupleLetter::getSpaceId, spaceId)
                .eq(CoupleLetter::getRecipient, recipient)
                .eq(CoupleLetter::getStatus, CoupleLetter.STATUS_SEALED)
                .and(w -> w.isNull(CoupleLetter::getDeliverAt).or().le(CoupleLetter::getDeliverAt, now)));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleLetter>().eq(CoupleLetter::getSpaceId, spaceId));
    }
}
