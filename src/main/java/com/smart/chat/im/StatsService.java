package com.smart.chat.im;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 55 会话统计：好友数 / 收发消息数 / 收藏数 / 最活跃好友。
 * 面向演示规模（H2/MySQL 单表万条内）直接内存聚合。
 */
@Service
public class StatsService {

    public record StatsVO(long friends, long sent, long received, long stars,
                          String mostActivePeer, long mostActiveCount) {
    }

    private final FriendMapper friendMapper;
    private final PrivateMessageMapper messageMapper;
    private final MessageStarMapper starMapper;

    public StatsService(FriendMapper friendMapper, PrivateMessageMapper messageMapper, MessageStarMapper starMapper) {
        this.friendMapper = friendMapper;
        this.messageMapper = messageMapper;
        this.starMapper = starMapper;
    }

    public StatsVO stats(String me) {
        long friends = friendMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Friend>()
                        .eq(Friend::getOwnerUsername, me));
        long sent = messageMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getFromUser, me));
        long received = messageMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getToUser, me));
        long stars = starMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MessageStar>()
                        .eq(MessageStar::getUsername, me));

        // 最活跃好友：与我互通消息最多的对端（并列取用户名字典序最小）
        Map<String, Long> perPeer = new HashMap<>();
        messageMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrivateMessage>()
                        .and(w -> w.eq(PrivateMessage::getFromUser, me).or().eq(PrivateMessage::getToUser, me))
                        .last("LIMIT 10000"))
                .forEach(m -> {
                    String peer = m.getFromUser().equals(me) ? m.getToUser() : m.getFromUser();
                    perPeer.merge(peer, 1L, Long::sum);
                });
        Map.Entry<String, Long> top = null;
        for (Map.Entry<String, Long> entry : perPeer.entrySet()) {
            if (top == null || entry.getValue() > top.getValue()
                    || (entry.getValue().equals(top.getValue()) && entry.getKey().compareTo(top.getKey()) < 0)) {
                top = entry;
            }
        }
        String mostActivePeer = top == null ? null : top.getKey();
        long mostActiveCount = top == null ? 0L : top.getValue();
        return new StatsVO(friends, sent, received, stars, mostActivePeer, mostActiveCount);
    }
}
