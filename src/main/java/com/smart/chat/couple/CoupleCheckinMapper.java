package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CoupleCheckinMapper extends BaseMapperCompat<CoupleCheckin> {

    /** 某人某天某类打卡（自然日唯一）。 */
    default Optional<CoupleCheckin> find(String spaceId, String username, String kind, String day) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<CoupleCheckin>()
                .eq(CoupleCheckin::getSpaceId, spaceId)
                .eq(CoupleCheckin::getUsername, username)
                .eq(CoupleCheckin::getKind, kind)
                .eq(CoupleCheckin::getCheckinDay, day)
                .last("LIMIT 1")));
    }

    /** 某空间指定日期范围内的全部打卡（计算 streak 用）。 */
    default List<CoupleCheckin> findBySpaceAndKind(String spaceId, String kind) {
        return selectList(new LambdaQueryWrapper<CoupleCheckin>()
                .eq(CoupleCheckin::getSpaceId, spaceId)
                .eq(CoupleCheckin::getKind, kind));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleCheckin>().eq(CoupleCheckin::getSpaceId, spaceId));
    }
}
