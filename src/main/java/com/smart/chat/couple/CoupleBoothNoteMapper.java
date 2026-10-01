package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F303 时空电话亭数据访问。 */
@Mapper
public interface CoupleBoothNoteMapper extends BaseMapperCompat<CoupleBoothNote> {

    /** 封存中且回放日已到（读时接通）。 */
    default List<CoupleBoothNote> findDue(String spaceId, String today) {
        return selectList(new LambdaQueryWrapper<CoupleBoothNote>()
                .eq(CoupleBoothNote::getSpaceId, spaceId)
                .eq(CoupleBoothNote::getStatus, CoupleBoothNote.STATUS_SEALED)
                .le(CoupleBoothNote::getOpenDay, today));
    }

    default List<CoupleBoothNote> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBoothNote>()
                .eq(CoupleBoothNote::getSpaceId, spaceId)
                .orderByDesc(CoupleBoothNote::getCreated));
    }
}
