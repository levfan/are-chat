package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F356 夸夸回执数据访问。 */
@Mapper
public interface CoupleEchoReceiptMapper extends BaseMapperCompat<CoupleEchoReceipt> {

    /** 某人给某句夸夸的回执（uk(space_id,quote_id,from_user) 最多一条，幂等判定用）。 */
    default CoupleEchoReceipt findByQuoteUser(String spaceId, String quoteId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleEchoReceipt>()
                .eq(CoupleEchoReceipt::getSpaceId, spaceId)
                .eq(CoupleEchoReceipt::getQuoteId, quoteId)
                .eq(CoupleEchoReceipt::getFromUser, fromUser));
    }

    /** 我的回执列表（新的在前）。 */
    default List<CoupleEchoReceipt> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleEchoReceipt>()
                .eq(CoupleEchoReceipt::getSpaceId, spaceId)
                .eq(CoupleEchoReceipt::getFromUser, fromUser)
                .orderByDesc(CoupleEchoReceipt::getCreated));
    }

    /** 空间全部回执（年报计数用）。 */
    default List<CoupleEchoReceipt> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleEchoReceipt>()
                .eq(CoupleEchoReceipt::getSpaceId, spaceId)
                .orderByDesc(CoupleEchoReceipt::getCreated));
    }
}
