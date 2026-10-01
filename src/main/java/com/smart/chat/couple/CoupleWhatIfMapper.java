package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleWhatIfMapper extends BaseMapperCompat<CoupleWhatIf> {

    /** 某人某天的作答。 */
    default CoupleWhatIf find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleWhatIf>()
                .eq(CoupleWhatIf::getSpaceId, spaceId)
                .eq(CoupleWhatIf::getDay, day)
                .eq(CoupleWhatIf::getFromUser, fromUser));
    }

    /** 全部作答（按创建倒序）。 */
    default List<CoupleWhatIf> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWhatIf>()
                .eq(CoupleWhatIf::getSpaceId, spaceId)
                .orderByDesc(CoupleWhatIf::getCreated));
    }
}
