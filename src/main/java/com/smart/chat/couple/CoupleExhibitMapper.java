package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleExhibitMapper extends BaseMapperCompat<CoupleExhibit> {

    /** 全部展品（按藏品日期倒序，缺日期排后）。 */
    default List<CoupleExhibit> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleExhibit>()
                .eq(CoupleExhibit::getSpaceId, spaceId)
                .orderByDesc(CoupleExhibit::getObtainedDay)
                .orderByDesc(CoupleExhibit::getCreated));
    }
}
