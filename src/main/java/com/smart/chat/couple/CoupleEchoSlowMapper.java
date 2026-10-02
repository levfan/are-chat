package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F354 感谢慢递数据访问。 */
@Mapper
public interface CoupleEchoSlowMapper extends BaseMapperCompat<CoupleEchoSlow> {

    /** 某人寄出的全部慢递（新的在前，在途上限与在途列表用）。 */
    default List<CoupleEchoSlow> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleEchoSlow>()
                .eq(CoupleEchoSlow::getSpaceId, spaceId)
                .eq(CoupleEchoSlow::getFromUser, fromUser)
                .orderByDesc(CoupleEchoSlow::getCreated));
    }

    /** 到期待送达：delivered=0 且 open_day<=今天（读时惰性结算按送达日先后推）。 */
    default List<CoupleEchoSlow> findDue(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleEchoSlow>()
                .eq(CoupleEchoSlow::getSpaceId, spaceId)
                .eq(CoupleEchoSlow::getDelivered, 0)
                .le(CoupleEchoSlow::getOpenDay, day)
                .orderByAsc(CoupleEchoSlow::getOpenDay));
    }

    /** 空间全部慢递（总览双方在途/最近送达、年报送达数用）。 */
    default List<CoupleEchoSlow> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleEchoSlow>()
                .eq(CoupleEchoSlow::getSpaceId, spaceId)
                .orderByDesc(CoupleEchoSlow::getCreated));
    }
}
