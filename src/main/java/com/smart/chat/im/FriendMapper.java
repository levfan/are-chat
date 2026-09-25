package com.smart.chat.im;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FriendMapper extends BaseMapperCompat<Friend> {

    default Optional<Friend> findByOwnerAndFriend(String owner, String friend) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getOwnerUsername, owner)
                .eq(Friend::getFriendUsername, friend)
                .last("LIMIT 1")));
    }

    default List<Friend> findAllByOwner(String owner) {
        return selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getOwnerUsername, owner));
    }

    /** 删除双向好友关系。 */
    default void deletePair(String a, String b) {
        delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getOwnerUsername, a)
                .eq(Friend::getFriendUsername, b));
        delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getOwnerUsername, b)
                .eq(Friend::getFriendUsername, a));
    }

    /** 刷新某用户在所有好友列表里的「最近在线」时间。 */
    default void updateLastSeenByUsername(String friendUsername, Long lastSeenAt) {
        update(null, new LambdaUpdateWrapper<Friend>()
                .eq(Friend::getFriendUsername, friendUsername)
                .set(Friend::getLastSeenAt, lastSeenAt));
    }
}
