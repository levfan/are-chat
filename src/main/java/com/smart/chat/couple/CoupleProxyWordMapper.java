package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F261 替我说数据访问。 */
@Mapper
public interface CoupleProxyWordMapper extends BaseMapperCompat<CoupleProxyWord> {

    /** 某人的在途草稿。 */
    default CoupleProxyWord findDraft(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleProxyWord>()
                .eq(CoupleProxyWord::getSpaceId, spaceId)
                .eq(CoupleProxyWord::getFromUser, fromUser)
                .eq(CoupleProxyWord::getStatus, CoupleProxyWord.STATUS_DRAFT)
                .orderByDesc(CoupleProxyWord::getCreated)
                .last("limit 1"));
    }

    /** 按状态列表（新→旧）。 */
    default List<CoupleProxyWord> findByStatus(String spaceId, String status) {
        return selectList(new LambdaQueryWrapper<CoupleProxyWord>()
                .eq(CoupleProxyWord::getSpaceId, spaceId)
                .eq(CoupleProxyWord::getStatus, status)
                .orderByDesc(CoupleProxyWord::getCreated));
    }
}
