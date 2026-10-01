package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

/** F345 情侣品牌数据访问。 */
@Mapper
public interface CoupleLegacyBrandMapper extends BaseMapperCompat<CoupleLegacyBrand> {

    /** 空间的品牌卡（uk(space_id) 保证最多一条）。 */
    default CoupleLegacyBrand find(String spaceId) {
        return selectOne(new LambdaQueryWrapper<CoupleLegacyBrand>()
                .eq(CoupleLegacyBrand::getSpaceId, spaceId));
    }
}
