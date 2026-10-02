package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F397 幽默风格图鉴数据访问（uk(space_id,about_user,rater) 谁评谁只一行）。 */
@Mapper
public interface CoupleLaughStyleMapper extends BaseMapperCompat<CoupleLaughStyle> {

    /** 空间全部风格票（旧的在前，四格图鉴按人归组用）。 */
    default List<CoupleLaughStyle> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLaughStyle>()
                .eq(CoupleLaughStyle::getSpaceId, spaceId)
                .orderByAsc(CoupleLaughStyle::getCreated));
    }

    /** 按 uk 定位那一格（改评查重用）。 */
    default CoupleLaughStyle find(String spaceId, String aboutUser, String rater) {
        return selectOne(new LambdaQueryWrapper<CoupleLaughStyle>()
                .eq(CoupleLaughStyle::getSpaceId, spaceId)
                .eq(CoupleLaughStyle::getAboutUser, aboutUser)
                .eq(CoupleLaughStyle::getRater, rater));
    }
}
