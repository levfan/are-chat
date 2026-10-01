package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleTrustCoinMapper extends BaseMapperCompat<CoupleTrustCoin> {

    /** 空间的信任币（新的在前）。 */
    default List<CoupleTrustCoin> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTrustCoin>()
                .eq(CoupleTrustCoin::getSpaceId, spaceId)
                .orderByDesc(CoupleTrustCoin::getCreated)
                .last("LIMIT 50"));
    }

    /** 某人收到的信任币数（余额）。 */
    default long countByTo(String spaceId, String toUser) {
        return selectCount(new LambdaQueryWrapper<CoupleTrustCoin>()
                .eq(CoupleTrustCoin::getSpaceId, spaceId)
                .eq(CoupleTrustCoin::getToUser, toUser));
    }
}
