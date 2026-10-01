package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F264 换位信数据访问。 */
@Mapper
public interface CoupleSwapLetterMapper extends BaseMapperCompat<CoupleSwapLetter> {

    /** 某人某日一封。 */
    default CoupleSwapLetter find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleSwapLetter>()
                .eq(CoupleSwapLetter::getSpaceId, spaceId)
                .eq(CoupleSwapLetter::getDay, day)
                .eq(CoupleSwapLetter::getFromUser, fromUser));
    }

    /** 已到开放日仍未拆的信。 */
    default List<CoupleSwapLetter> findDue(String spaceId, String today) {
        return selectList(new LambdaQueryWrapper<CoupleSwapLetter>()
                .eq(CoupleSwapLetter::getSpaceId, spaceId)
                .eq(CoupleSwapLetter::getStatus, CoupleSwapLetter.STATUS_SEALED)
                .le(CoupleSwapLetter::getOpenDay, today));
    }

    /** 全部（近→旧）。 */
    default List<CoupleSwapLetter> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSwapLetter>()
                .eq(CoupleSwapLetter::getSpaceId, spaceId)
                .orderByDesc(CoupleSwapLetter::getCreated));
    }
}
