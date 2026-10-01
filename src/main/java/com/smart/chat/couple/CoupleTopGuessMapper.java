package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F282 互猜数据访问。 */
@Mapper
public interface CoupleTopGuessMapper extends BaseMapperCompat<CoupleTopGuess> {

    /** 某人猜某类目一猜。 */
    default CoupleTopGuess find(String spaceId, String category, String guesserUser) {
        return selectOne(new LambdaQueryWrapper<CoupleTopGuess>()
                .eq(CoupleTopGuess::getSpaceId, spaceId)
                .eq(CoupleTopGuess::getCategory, category)
                .eq(CoupleTopGuess::getGuesserUser, guesserUser));
    }

    /** 全部猜测。 */
    default List<CoupleTopGuess> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTopGuess>()
                .eq(CoupleTopGuess::getSpaceId, spaceId));
    }
}
