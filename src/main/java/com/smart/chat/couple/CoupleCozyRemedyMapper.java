package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F227 疼痛对策本数据访问。 */
@Mapper
public interface CoupleCozyRemedyMapper extends BaseMapperCompat<CoupleCozyRemedy> {

    /** 给某人的疼痛对策。 */
    default CoupleCozyRemedy find(String spaceId, String forUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCozyRemedy>()
                .eq(CoupleCozyRemedy::getSpaceId, spaceId)
                .eq(CoupleCozyRemedy::getForUser, forUser));
    }
}
