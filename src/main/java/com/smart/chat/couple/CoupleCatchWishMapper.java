package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F380 暗中心愿本数据访问（uk(space_id,owner_user,content) 同主人同心愿只一行）。 */
@Mapper
public interface CoupleCatchWishMapper extends BaseMapperCompat<CoupleCatchWish> {

    /** 空间全部心愿（新的在前，记账人侧全量可见）。 */
    default List<CoupleCatchWish> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchWish>()
                .eq(CoupleCatchWish::getSpaceId, spaceId)
                .orderByDesc(CoupleCatchWish::getCreated));
    }

    /** 某人的心愿列表（新的在前；揭晓前对主人隐藏由服务层按 secret() 过滤）。 */
    default List<CoupleCatchWish> findByOwner(String spaceId, String ownerUser) {
        return selectList(new LambdaQueryWrapper<CoupleCatchWish>()
                .eq(CoupleCatchWish::getSpaceId, spaceId)
                .eq(CoupleCatchWish::getOwnerUser, ownerUser)
                .orderByDesc(CoupleCatchWish::getCreated));
    }

    /** 按 uk 定位那一条（写前查重用）。 */
    default CoupleCatchWish find(String spaceId, String ownerUser, String content) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchWish>()
                .eq(CoupleCatchWish::getSpaceId, spaceId)
                .eq(CoupleCatchWish::getOwnerUser, ownerUser)
                .eq(CoupleCatchWish::getContent, content));
    }
}
