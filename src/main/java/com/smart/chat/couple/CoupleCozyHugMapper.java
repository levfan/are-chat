package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F228 抱抱计量器数据访问。 */
@Mapper
public interface CoupleCozyHugMapper extends BaseMapperCompat<CoupleCozyHug> {

    /** 空间全部抱抱流水（自早到晚）。 */
    default List<CoupleCozyHug> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCozyHug>()
                .eq(CoupleCozyHug::getSpaceId, spaceId)
                .orderByAsc(CoupleCozyHug::getCreated));
    }
}
