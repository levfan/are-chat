package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F280 词条数据访问。 */
@Mapper
public interface CoupleCodexEntryMapper extends BaseMapperCompat<CoupleCodexEntry> {

    /** 全部词条（按创建升序，出题取前 5）。 */
    default List<CoupleCodexEntry> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCodexEntry>()
                .eq(CoupleCodexEntry::getSpaceId, spaceId)
                .orderByAsc(CoupleCodexEntry::getCreated));
    }

    /** 同名词条一条。 */
    default CoupleCodexEntry findTerm(String spaceId, String term) {
        return selectOne(new LambdaQueryWrapper<CoupleCodexEntry>()
                .eq(CoupleCodexEntry::getSpaceId, spaceId)
                .eq(CoupleCodexEntry::getTerm, term));
    }
}
