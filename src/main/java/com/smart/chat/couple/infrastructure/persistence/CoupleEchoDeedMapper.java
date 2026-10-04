package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F350 好事簿数据访问。 */
@Mapper
public interface CoupleEchoDeedMapper extends BaseMapperCompat<CoupleEchoDeed> {

    /** 某人记下的全部好事（新的在前，列表口径取最近 30 条由服务层截断）。 */
    default List<CoupleEchoDeed> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleEchoDeed>()
                .eq(CoupleEchoDeed::getSpaceId, spaceId)
                .eq(CoupleEchoDeed::getFromUser, fromUser)
                .orderByDesc(CoupleEchoDeed::getCreated));
    }

    /** 空间全部好事（日历/年报按天聚合用）。 */
    default List<CoupleEchoDeed> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleEchoDeed>()
                .eq(CoupleEchoDeed::getSpaceId, spaceId)
                .orderByDesc(CoupleEchoDeed::getCreated));
    }

    /** 同日同人同内容查重（内容全等才算重复，服务层 400）。 */
    default CoupleEchoDeed findByDayContent(String spaceId, String fromUser, String day, String content) {
        return selectList(new LambdaQueryWrapper<CoupleEchoDeed>()
                        .eq(CoupleEchoDeed::getSpaceId, spaceId)
                        .eq(CoupleEchoDeed::getFromUser, fromUser)
                        .eq(CoupleEchoDeed::getDay, day)
                        .eq(CoupleEchoDeed::getContent, content))
                .stream().findFirst().orElse(null);
    }
}
