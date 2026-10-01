package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F341 记忆库年审数据访问。 */
@Mapper
public interface CoupleLegacyAuditMapper extends BaseMapperCompat<CoupleLegacyAudit> {

    /** 某年年审意见（双方各一条，按人排）。 */
    default List<CoupleLegacyAudit> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleLegacyAudit>()
                .eq(CoupleLegacyAudit::getSpaceId, spaceId)
                .eq(CoupleLegacyAudit::getYear, year)
                .orderByAsc(CoupleLegacyAudit::getFromUser));
    }

    /** 空间全部年审（年份新→旧）。 */
    default List<CoupleLegacyAudit> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLegacyAudit>()
                .eq(CoupleLegacyAudit::getSpaceId, spaceId)
                .orderByDesc(CoupleLegacyAudit::getYear));
    }
}
