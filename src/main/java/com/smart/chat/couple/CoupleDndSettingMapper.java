package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDndSettingMapper extends BaseMapperCompat<CoupleDndSetting> {

    /** 某人的免打扰设置。 */
    default CoupleDndSetting find(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleDndSetting>()
                .eq(CoupleDndSetting::getSpaceId, spaceId)
                .eq(CoupleDndSetting::getFromUser, fromUser));
    }

    /** 双方的免打扰设置。 */
    default List<CoupleDndSetting> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDndSetting>()
                .eq(CoupleDndSetting::getSpaceId, spaceId));
    }
}
