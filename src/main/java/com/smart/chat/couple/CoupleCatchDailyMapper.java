package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F388 今日一句话数据访问（uk(space_id,day,user_name) 每人每天一句）。 */
@Mapper
public interface CoupleCatchDailyMapper extends BaseMapperCompat<CoupleCatchDaily> {

    /** 空间全部留言（近的在前）。 */
    default List<CoupleCatchDaily> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchDaily>()
                .eq(CoupleCatchDaily::getSpaceId, spaceId)
                .orderByDesc(CoupleCatchDaily::getDay));
    }

    /** 按 uk 定位那天那句（写前查重、当天改写用）。 */
    default CoupleCatchDaily find(String spaceId, String day, String userName) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchDaily>()
                .eq(CoupleCatchDaily::getSpaceId, spaceId)
                .eq(CoupleCatchDaily::getDay, day)
                .eq(CoupleCatchDaily::getUserName, userName));
    }

    /** 某人最近的一句（「今天没说就回看昨天那句」用；取 day 降序首行，
     *  走 selectList + findFirst 不拼原生 limit，保持 H2 与 MariaDB 双兼容）。 */
    default CoupleCatchDaily findLatest(String spaceId, String userName) {
        return selectList(new LambdaQueryWrapper<CoupleCatchDaily>()
                        .eq(CoupleCatchDaily::getSpaceId, spaceId)
                        .eq(CoupleCatchDaily::getUserName, userName)
                        .orderByDesc(CoupleCatchDaily::getDay))
                .stream().findFirst().orElse(null);
    }
}
