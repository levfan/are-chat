package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F394 快乐突袭数据访问（uk(space_id,from_user,day) 每人每天一发）。 */
@Mapper
public interface CoupleLaughAttackMapper extends BaseMapperCompat<CoupleLaughAttack> {

    /** 空间全部突袭（日子降序，突袭流水）。 */
    default List<CoupleLaughAttack> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLaughAttack>()
                .eq(CoupleLaughAttack::getSpaceId, spaceId)
                .orderByDesc(CoupleLaughAttack::getDay));
    }

    /** 某一天的突袭（日子降序，两人同一天各一发）。 */
    default List<CoupleLaughAttack> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleLaughAttack>()
                .eq(CoupleLaughAttack::getSpaceId, spaceId)
                .eq(CoupleLaughAttack::getDay, day)
                .orderByDesc(CoupleLaughAttack::getDay));
    }

    /** 按 uk 定位那一条（写前查重、中弹回填用）。 */
    default CoupleLaughAttack find(String spaceId, String fromUser, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleLaughAttack>()
                .eq(CoupleLaughAttack::getSpaceId, spaceId)
                .eq(CoupleLaughAttack::getFromUser, fromUser)
                .eq(CoupleLaughAttack::getDay, day));
    }
}
