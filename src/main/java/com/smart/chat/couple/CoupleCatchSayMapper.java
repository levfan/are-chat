package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F385 真话翻译词条数据访问（uk(space_id,from_user,say) 同人同说辞只一行）。 */
@Mapper
public interface CoupleCatchSayMapper extends BaseMapperCompat<CoupleCatchSay> {

    /** 空间全部词条（申报先后升序，对方的翻译词典按此铺排）。 */
    default List<CoupleCatchSay> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchSay>()
                .eq(CoupleCatchSay::getSpaceId, spaceId)
                .orderByAsc(CoupleCatchSay::getCreated));
    }

    /** 某人申报的词条（申报先后升序，本人编辑页用）。 */
    default List<CoupleCatchSay> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleCatchSay>()
                .eq(CoupleCatchSay::getSpaceId, spaceId)
                .eq(CoupleCatchSay::getFromUser, fromUser)
                .orderByAsc(CoupleCatchSay::getCreated));
    }

    /** 按 uk 定位那条（写前查重用）。 */
    default CoupleCatchSay find(String spaceId, String fromUser, String say) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchSay>()
                .eq(CoupleCatchSay::getSpaceId, spaceId)
                .eq(CoupleCatchSay::getFromUser, fromUser)
                .eq(CoupleCatchSay::getSay, say));
    }
}
