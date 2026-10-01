package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F285 去过的地方数据访问。 */
@Mapper
public interface CouplePlaceMapper extends BaseMapperCompat<CouplePlace> {

    /** 同名一处。 */
    default CouplePlace findName(String spaceId, String name) {
        return selectOne(new LambdaQueryWrapper<CouplePlace>()
                .eq(CouplePlace::getSpaceId, spaceId)
                .eq(CouplePlace::getName, name));
    }

    /** 全部足迹（按年份升序，年表用）。 */
    default List<CouplePlace> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePlace>()
                .eq(CouplePlace::getSpaceId, spaceId)
                .orderByAsc(CouplePlace::getYear));
    }
}
