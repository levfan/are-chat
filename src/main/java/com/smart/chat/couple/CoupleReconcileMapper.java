package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleReconcileMapper extends BaseMapperCompat<CoupleReconcile> {

    /** 某空间全部和好卡（新→旧）。 */
    default List<CoupleReconcile> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleReconcile>()
                .eq(CoupleReconcile::getSpaceId, spaceId)
                .orderByDesc(CoupleReconcile::getCreated));
    }

    /** 已接受（和好完成）的次数。 */
    default long countAccepted(String spaceId) {
        return selectCount(new LambdaQueryWrapper<CoupleReconcile>()
                .eq(CoupleReconcile::getSpaceId, spaceId)
                .eq(CoupleReconcile::getStatus, CoupleReconcile.STATUS_ACCEPTED));
    }
}
