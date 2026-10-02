package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F358 写给低落的自己数据访问。 */
@Mapper
public interface CoupleEchoSelfLetterMapper extends BaseMapperCompat<CoupleEchoSelfLetter> {

    /** 某人的在途信（SEALED 查询约束：存在即不许再写，一人同时一封）。 */
    default CoupleEchoSelfLetter findSealedByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleEchoSelfLetter>()
                        .eq(CoupleEchoSelfLetter::getSpaceId, spaceId)
                        .eq(CoupleEchoSelfLetter::getFromUser, fromUser)
                        .eq(CoupleEchoSelfLetter::getStatus, CoupleEchoSelfLetter.STATUS_SEALED))
                .stream().findFirst().orElse(null);
    }

    /** 某人的全部信（新的在前，含已读存档）。 */
    default List<CoupleEchoSelfLetter> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleEchoSelfLetter>()
                .eq(CoupleEchoSelfLetter::getSpaceId, spaceId)
                .eq(CoupleEchoSelfLetter::getFromUser, fromUser)
                .orderByDesc(CoupleEchoSelfLetter::getCreated));
    }
}
