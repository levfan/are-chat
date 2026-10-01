package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDocSceneMapper extends BaseMapperCompat<CoupleDocScene> {

    /** 全部分镜（按创建倒序）。 */
    default List<CoupleDocScene> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDocScene>()
                .eq(CoupleDocScene::getSpaceId, spaceId)
                .orderByDesc(CoupleDocScene::getCreated));
    }
}
