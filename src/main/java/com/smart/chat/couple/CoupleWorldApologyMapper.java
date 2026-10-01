package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F339 代 TA 赔礼数据访问。 */
@Mapper
public interface CoupleWorldApologyMapper extends BaseMapperCompat<CoupleWorldApology> {

    /** 等 TA 审阅的信（写信时间新→旧）。 */
    default List<CoupleWorldApology> findPending(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldApology>()
                .eq(CoupleWorldApology::getSpaceId, spaceId)
                .eq(CoupleWorldApology::getStatus, CoupleWorldApology.STATUS_OPEN)
                .orderByDesc(CoupleWorldApology::getCreated));
    }

    /** 某人求代写的信（写信时间新→旧）。 */
    default List<CoupleWorldApology> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleWorldApology>()
                .eq(CoupleWorldApology::getSpaceId, spaceId)
                .eq(CoupleWorldApology::getFromUser, fromUser)
                .orderByDesc(CoupleWorldApology::getCreated));
    }

    /** 空间全部赔礼信（写信时间新→旧）。 */
    default List<CoupleWorldApology> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldApology>()
                .eq(CoupleWorldApology::getSpaceId, spaceId)
                .orderByDesc(CoupleWorldApology::getCreated));
    }
}
