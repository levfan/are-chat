package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleCloudDateMapper extends BaseMapperCompat<CoupleCloudDate> {

    /** 空间的云约会（待完成的在前）。 */
    default List<CoupleCloudDate> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCloudDate>()
                .eq(CoupleCloudDate::getSpaceId, spaceId)
                .orderByAsc(CoupleCloudDate::getStatus)
                .orderByDesc(CoupleCloudDate::getCreated)
                .last("LIMIT 50"));
    }
}
