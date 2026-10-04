package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface PrivateMessageMapper extends BaseMapperCompat<PrivateMessagePO> {

    /** 双向会话过滤：(me→peer) OR (peer→me)。 */
    default LambdaQueryWrapper<PrivateMessagePO> conversationWrapper(String me, String peer) {
        return new LambdaQueryWrapper<PrivateMessagePO>()
                .and(w -> w
                        .and(w1 -> w1.eq(PrivateMessagePO::getFromUser, me).eq(PrivateMessagePO::getToUser, peer))
                        .or(w2 -> w2.eq(PrivateMessagePO::getFromUser, peer).eq(PrivateMessagePO::getToUser, me)));
    }

    /** 倒序取一页（before 为游标：早于该时间），调用方自行反转为正序。 */
    default List<PrivateMessagePO> findConversationPage(String me, String peer, Long before, int limit) {
        LambdaQueryWrapper<PrivateMessagePO> wrapper = conversationWrapper(me, peer)
                .lt(before != null && before > 0, PrivateMessagePO::getCreated, before)
                .orderByDesc(PrivateMessagePO::getCreated)
                .last("LIMIT " + limit);
        return selectList(wrapper);
    }

    default Optional<PrivateMessagePO> findLatestBetween(String me, String peer) {
        return Optional.ofNullable(selectOne(conversationWrapper(me, peer)
                .orderByDesc(PrivateMessagePO::getCreated)
                .last("LIMIT 1")));
    }

    /**
     * 联系人列表专用：一次拿到「我与每个对端之间最后一条消息的时间点」。
     * <p>原先是每人一次 {@link #findLatestBetween}——那条 SQL 的谓词是
     * {@code (from_user=me AND to_user=peer) OR (to_user=me AND from_user=peer)}，
     * OR 跨了两个不同索引前缀，优化器只能全表扫（V49 补索引后实测仍是 ~200ms/次），
     * N 个好友就是 N 次全表扫。改成两个方向各一次 GROUP BY（各自能吃下 V49 的复合索引），
     * 实测 20 万消息量下两次合计 ~30ms，且与好友数无关。
     *
     * @return key=对端用户名，value=与该对端之间最后一条消息的 created
     */
    default Map<String, Long> findLatestCreatedPerPeer(String me) {
        Map<String, Long> out = new HashMap<>();
        for (Map<String, Object> row : selectMaps(new QueryWrapper<PrivateMessagePO>()
                .select("to_user AS peer", "MAX(created) AS latest")
                .eq("from_user", me)
                .groupBy("to_user"))) {
            mergeLatest(out, row);
        }
        for (Map<String, Object> row : selectMaps(new QueryWrapper<PrivateMessagePO>()
                .select("from_user AS peer", "MAX(created) AS latest")
                .eq("to_user", me)
                .groupBy("from_user"))) {
            mergeLatest(out, row);
        }
        return out;
    }

    private static void mergeLatest(Map<String, Long> out, Map<String, Object> row) {
        Object peer = row.get("peer");
        Object latest = row.get("latest");
        if (peer == null || !(latest instanceof Number number)) {
            return;
        }
        out.merge(String.valueOf(peer), number.longValue(), Math::max);
    }

    /**
     * 按上一步算出的时间点，一次把那些「最后一条」整行捞回来（两个方向各一趟，都是索引前缀等值 + IN）。
     * <p>不直接用 OR 合并成一趟：OR 跨前后缀同样退化成全表扫（实测 247ms vs 这里 12-13ms）。
     */
    default List<PrivateMessagePO> findMessagesAtCreated(String me, Collection<String> peers, Collection<Long> timestamps) {
        if (peers.isEmpty() || timestamps.isEmpty()) {
            return List.of();
        }
        List<PrivateMessagePO> out = new ArrayList<>(selectList(new LambdaQueryWrapper<PrivateMessagePO>()
                .eq(PrivateMessagePO::getFromUser, me)
                .in(PrivateMessagePO::getToUser, peers)
                .in(PrivateMessagePO::getCreated, timestamps)));
        out.addAll(selectList(new LambdaQueryWrapper<PrivateMessagePO>()
                .eq(PrivateMessagePO::getToUser, me)
                .in(PrivateMessagePO::getFromUser, peers)
                .in(PrivateMessagePO::getCreated, timestamps)));
        return out;
    }

    default long countUnread(String me, String peer, Long lastReadAt) {
        return selectCount(conversationWrapper(me, peer)
                .eq(PrivateMessagePO::getFromUser, peer)
                .eq(PrivateMessagePO::getStatus, PrivateMessagePO.STATUS_SENT)
                .gt(PrivateMessagePO::getCreated, lastReadAt == null ? 0L : lastReadAt));
    }

    /** 会话内关键字搜索（只搜未撤回），倒序取最近 50 条，调用方反转为正序。 */
    default List<PrivateMessagePO> searchConversation(String me, String peer, String keyword) {
        return selectList(conversationWrapper(me, peer)
                .eq(PrivateMessagePO::getStatus, PrivateMessagePO.STATUS_SENT)
                .like(PrivateMessagePO::getContent, keyword)
                .orderByDesc(PrivateMessagePO::getCreated)
                .last("LIMIT 50"));
    }

    /** 56 导出：全量拉取双向会话消息（正序由调用方保证，上限 10000 条）。 */
    default List<PrivateMessagePO> findConversationAll(String me, String peer) {
        return selectList(conversationWrapper(me, peer)
                .orderByAsc(PrivateMessagePO::getCreated)
                .last("LIMIT 10000"));
    }

    /** 已读回执：把 from 发给 to 的未读消息全部标记已读。 */
    default int markIncomingRead(String from, String to) {
        return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PrivateMessagePO>()
                .eq(PrivateMessagePO::getFromUser, from)
                .eq(PrivateMessagePO::getToUser, to)
                .eq(PrivateMessagePO::getReadFlag, 0)
                .set(PrivateMessagePO::getReadFlag, 1));
    }

    /** 81 全局消息搜索：我参与的全部会话里按关键字搜未撤回文本，倒序取最近 50 条。 */
    default List<PrivateMessagePO> searchGlobal(String me, String keyword) {
        return selectList(new LambdaQueryWrapper<PrivateMessagePO>()
                .and(w -> w.eq(PrivateMessagePO::getFromUser, me).or().eq(PrivateMessagePO::getToUser, me))
                .eq(PrivateMessagePO::getStatus, PrivateMessagePO.STATUS_SENT)
                .eq(PrivateMessagePO::getMsgType, PrivateMessagePO.TYPE_TEXT)
                .like(PrivateMessagePO::getContent, keyword)
                .orderByDesc(PrivateMessagePO::getCreated)
                .last("LIMIT 50"));
    }

    /** 95 会话附件：双向会话中指定类型的消息（image/file），倒序取最近 100 条。 */
    default List<PrivateMessagePO> findAttachments(String me, String peer, String msgType) {
        return selectList(conversationWrapper(me, peer)
                .eq(PrivateMessagePO::getStatus, PrivateMessagePO.STATUS_SENT)
                .eq(PrivateMessagePO::getMsgType, msgType)
                .orderByDesc(PrivateMessagePO::getCreated)
                .last("LIMIT 100"));
    }

    /** 85 清空聊天记录：删除双向会话全部消息，返回删除条数。 */
    default int deleteConversation(String me, String peer) {
        return delete(conversationWrapper(me, peer));
    }

    /** F36 心动时刻：我参与的双向会话里被标记的消息（可限定 peer），倒序取最近 100 条。 */
    default List<PrivateMessagePO> findHeartMoments(String me, String peer) {
        LambdaQueryWrapper<PrivateMessagePO> wrapper = new LambdaQueryWrapper<PrivateMessagePO>()
                .and(w -> w.eq(PrivateMessagePO::getFromUser, me).or().eq(PrivateMessagePO::getToUser, me))
                .isNotNull(PrivateMessagePO::getHeartAt)
                .orderByDesc(PrivateMessagePO::getHeartAt)
                .last("LIMIT 100");
        if (peer != null && !peer.isBlank()) {
            wrapper.and(w -> w
                    .and(w1 -> w1.eq(PrivateMessagePO::getFromUser, me).eq(PrivateMessagePO::getToUser, peer))
                    .or(w2 -> w2.eq(PrivateMessagePO::getFromUser, peer).eq(PrivateMessagePO::getToUser, me)));
        }
        return selectList(wrapper);
    }
}
