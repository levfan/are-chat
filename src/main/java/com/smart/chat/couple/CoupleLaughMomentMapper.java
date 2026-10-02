package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F390 笑点存档数据访问（uk(space_id,day,from_user,title) 同人同日同题只一行）。 */
@Mapper
public interface CoupleLaughMomentMapper extends BaseMapperCompat<CoupleLaughMoment> {

    /** 空间全部笑点（日子降序，笑点簿翻给两个人看）。 */
    default List<CoupleLaughMoment> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLaughMoment>()
                .eq(CoupleLaughMoment::getSpaceId, spaceId)
                .orderByDesc(CoupleLaughMoment::getDay));
    }

    /** 某一天记下的笑点（日子降序，同人同日可多条）。 */
    default List<CoupleLaughMoment> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleLaughMoment>()
                .eq(CoupleLaughMoment::getSpaceId, spaceId)
                .eq(CoupleLaughMoment::getDay, day)
                .orderByDesc(CoupleLaughMoment::getDay));
    }

    /** 按 uk 定位那一条（写前查重用）。 */
    default CoupleLaughMoment find(String spaceId, String day, String fromUser, String title) {
        return selectOne(new LambdaQueryWrapper<CoupleLaughMoment>()
                .eq(CoupleLaughMoment::getSpaceId, spaceId)
                .eq(CoupleLaughMoment::getDay, day)
                .eq(CoupleLaughMoment::getFromUser, fromUser)
                .eq(CoupleLaughMoment::getTitle, title));
    }
}
