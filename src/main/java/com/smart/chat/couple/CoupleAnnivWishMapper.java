package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F298 周年愿望台账数据访问。 */
@Mapper
public interface CoupleAnnivWishMapper extends BaseMapperCompat<CoupleAnnivWish> {

    /** 某人某年一愿。 */
    default CoupleAnnivWish find(String spaceId, String year, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleAnnivWish>()
                .eq(CoupleAnnivWish::getSpaceId, spaceId)
                .eq(CoupleAnnivWish::getYear, year)
                .eq(CoupleAnnivWish::getFromUser, fromUser));
    }

    /** 全部愿望（新年→旧年）。 */
    default List<CoupleAnnivWish> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleAnnivWish>()
                .eq(CoupleAnnivWish::getSpaceId, spaceId)
                .orderByDesc(CoupleAnnivWish::getYear));
    }
}
