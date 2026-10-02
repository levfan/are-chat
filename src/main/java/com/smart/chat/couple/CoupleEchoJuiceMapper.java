package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F352 鼓励语罐数据访问。 */
@Mapper
public interface CoupleEchoJuiceMapper extends BaseMapperCompat<CoupleEchoJuice> {

    /** 某人罐里的全部鼓励语（按槽位升序，找空槽用）。 */
    default List<CoupleEchoJuice> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleEchoJuice>()
                .eq(CoupleEchoJuice::getSpaceId, spaceId)
                .eq(CoupleEchoJuice::getFromUser, fromUser)
                .orderByAsc(CoupleEchoJuice::getIdx));
    }

    /** 空间双方罐子（总览/补给用）。 */
    default List<CoupleEchoJuice> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleEchoJuice>()
                .eq(CoupleEchoJuice::getSpaceId, spaceId)
                .orderByAsc(CoupleEchoJuice::getFromUser)
                .orderByAsc(CoupleEchoJuice::getIdx));
    }
}
