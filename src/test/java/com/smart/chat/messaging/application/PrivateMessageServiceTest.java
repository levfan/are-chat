package com.smart.chat.messaging.application;

import com.smart.chat.messaging.domain.conversation.PrivateMessage;
import com.smart.chat.messaging.domain.conversation.PrivateMessageRepository;
import com.smart.chat.messaging.domain.friend.Friend;
import com.smart.chat.messaging.domain.friend.FriendRepository;
import com.smart.chat.messaging.domain.pin.Conversation;
import com.smart.chat.messaging.domain.pin.ConversationPin;
import com.smart.chat.messaging.domain.pin.ConversationPinRepository;
import com.smart.chat.messaging.domain.reaction.MessageReaction;
import com.smart.chat.messaging.domain.reaction.MessageReactionRepository;
import com.smart.chat.messaging.domain.star.MessageStar;
import com.smart.chat.messaging.domain.star.MessageStarRepository;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 私聊用例：期望值（文案、条数、顺序、状态字面量）与改造前一字不差；
 * mock 从 Mapper 换成仓储端口，fixture 从 PO 的 setter 换成领域的 offer/restore。
 */
@ExtendWith(MockitoExtension.class)
class PrivateMessageServiceTest {

    @Mock
    private PrivateMessageRepository messageRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private MessageReactionRepository reactionRepository;

    @Mock
    private MessageStarRepository starRepository;

    @Mock
    private ImPushService push;

    @Mock
    private AccountDirectory accounts;

    @Mock
    private com.smart.chat.messaging.application.ModerationService moderation;

    @Mock
    private com.smart.chat.messaging.infrastructure.throttle.MessageRateLimiter rateLimiter;

    @Mock
    private ConversationPinRepository pinRepository;

    @InjectMocks
    private PrivateMessageService service;

    /** 合法用户目录：默认「输入规范化 + 用户存在」，个别用例可覆盖 */
    @org.junit.jupiter.api.BeforeEach
    void stubUserDirectory() {
        lenient().when(accounts.normalizeUsername(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).trim().toLowerCase());
        lenient().when(accounts.exists(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        // 86 敏感词过滤默认放行（返回原文）；限流默认不触发（void 方法空实现）
        lenient().when(moderation.clean(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> inv.getArgument(1, String.class));
    }

    private static final long NOW = System.currentTimeMillis();

    private static PrivateMessage text(String from, String to, String content) {
        return PrivateMessage.offer(from, to, content, PrivateMessage.TYPE_TEXT, NOW);
    }

    private static PrivateMessage textWith(String id, String from, String to, String content, String status,
                                           Integer readFlag, long created) {
        return PrivateMessage.restore(id, from, to, content, PrivateMessage.TYPE_TEXT, status, null, readFlag, null,
                null, created);
    }

    private static Friend edge(String owner, String peer) {
        return Friend.add(owner, peer, NOW);
    }

    private void stubFriendship(String me, String peer) {
        lenient().when(friendRepository.findByOwnerAndFriend(me, peer))
                .thenReturn(Optional.of(edge(me, peer)));
    }

    @Test
    void sendPersistsAndPushesToOnlinePeer() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(true);

        PrivateMessage sent = service.send("alice", "bob", "  在吗？  ", "text", null);

        assertThat(sent.fromUser()).isEqualTo("alice");
        assertThat(sent.toUser()).isEqualTo("bob");
        assertThat(sent.content()).isEqualTo("在吗？");
        assertThat(sent.status()).isEqualTo(PrivateMessage.STATUS_SENT);
        verify(messageRepository).save(sent);
        verify(push).pushDm(sent);
    }

    @Test
    void sendOfflinePeerStillStoresWithoutPush() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);

        PrivateMessage sent = service.send("alice", "bob", "留言", "text", null);

        verify(messageRepository).save(sent);
        verify(push, never()).pushDm(any());
    }

    @Test
    void sendPokeKeepsCustomSuffix() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);

        // 67 拍一拍支持自定义后缀；留空回落到默认文案
        assertThat(service.send("alice", "bob", "的小脑袋", "poke", null).content())
                .isEqualTo("的小脑袋");
        PrivateMessage blank = service.send("alice", "bob", "   ", "poke", null);
        assertThat(blank.msgType()).isEqualTo(PrivateMessage.TYPE_POKE);
        assertThat(blank.content()).isEqualTo(PrivateMessage.POKE_TEXT);
    }

    @Test
    void sendValidatesFriendshipAndInput() {
        // 合法用户校验先于好友校验：未注册的账号直接拒绝
        when(accounts.exists("不存在的人")).thenReturn(false);
        assertThatThrownBy(() -> service.send("alice", "不存在的人", "hi", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("查无此人");

        when(friendRepository.findByOwnerAndFriend("alice", "carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.send("alice", "carol", "hi", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还不是好友");

        assertThatThrownBy(() -> service.send("alice", "alice", "hi", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能给自己");

        stubFriendship("alice", "bob");
        assertThatThrownBy(() -> service.send("alice", "bob", "   ", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
        assertThatThrownBy(() -> service.send("alice", "bob", "x".repeat(2001), "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2000");
    }

    @Test
    void sendBlockedInEitherDirectionIsRejected() {
        // 我拉黑了对方
        Friend mine = edge("alice", "bob");
        mine.changeBlocked(true);
        when(friendRepository.findByOwnerAndFriend("alice", "bob")).thenReturn(Optional.of(mine));
        assertThatThrownBy(() -> service.send("alice", "bob", "hi", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已拉黑对方");

        // 对方拉黑了我
        stubFriendship("alice", "bob");
        Friend theirs = edge("bob", "alice");
        theirs.changeBlocked(true);
        when(friendRepository.findByOwnerAndFriend("bob", "alice")).thenReturn(Optional.of(theirs));
        assertThatThrownBy(() -> service.send("alice", "bob", "hi", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对方已将你拉黑");
    }

    @Test
    void historyReturnsAscendingPageWithEnrichment() {
        PrivateMessage later = text("bob", "alice", "第二条");
        PrivateMessage earlier = textWith("m-early", "alice", "bob", "第一条", PrivateMessage.STATUS_SENT, 1,
                NOW - 1000);
        when(messageRepository.findConversationPage("alice", "bob", null, 20))
                .thenReturn(List.of(later, earlier));
        when(reactionRepository.listByMsgIds(any())).thenReturn(
                List.of(MessageReaction.add(earlier.id(), "bob", "👍", NOW)));
        when(starRepository.listByUsernameAndMsgIds(eq("alice"), any())).thenReturn(
                List.of(MessageStar.add("alice", earlier.id(), NOW)));

        List<PrivateMessageService.MessageVO> history = service.history("alice", "bob", null, 20);

        assertThat(history).extracting(PrivateMessageService.MessageVO::content).containsExactly("第一条", "第二条");
        PrivateMessageService.MessageVO first = history.get(0);
        assertThat(first.read()).isTrue();
        assertThat(first.starred()).isTrue();
        assertThat(first.reactions()).hasSize(1);
        assertThat(first.reactions().get(0).username()).isEqualTo("bob");
        assertThat(history.get(1).read()).isFalse();
    }

    @Test
    void markReadUpdatesCursorAndReceipt() {
        Friend row = edge("alice", "bob");
        when(friendRepository.findByOwnerAndFriend("alice", "bob")).thenReturn(Optional.of(row));

        service.markRead("alice", "bob");

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendRepository).save(captor.capture());
        assertThat(captor.getValue().lastReadAt()).isGreaterThan(0);
        verify(messageRepository).markIncomingRead("bob", "alice");
        verify(push).pushRead("alice", "bob");

        when(friendRepository.findByOwnerAndFriend("alice", "路人甲")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead("alice", "路人甲"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还不是好友");
    }

    @Test
    void recallWithinWindowNotifiesPeer() {
        PrivateMessage message = text("alice", "bob", "说错话了");
        when(messageRepository.findById(message.id())).thenReturn(Optional.of(message));
        when(push.isOnline("bob")).thenReturn(true);

        service.recall("alice", message.id());

        assertThat(message.status()).isEqualTo(PrivateMessage.STATUS_RECALLED);
        verify(messageRepository).save(message);
        verify(push).pushRecall(message);
    }

    @Test
    void recallValidatesOwnerWindowAndState() {
        PrivateMessage other = text("bob", "alice", "别人的");
        when(messageRepository.findById(other.id())).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.recall("alice", other.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能撤回自己");

        PrivateMessage old = PrivateMessage.offer("alice", "bob", "很久之前", PrivateMessage.TYPE_TEXT,
                System.currentTimeMillis() - PrivateMessage.RECALL_WINDOW_MS - 1);
        when(messageRepository.findById(old.id())).thenReturn(Optional.of(old));
        assertThatThrownBy(() -> service.recall("alice", old.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 分钟");

        PrivateMessage recalled = textWith("m-recalled", "alice", "bob", "已撤回",
                PrivateMessage.STATUS_RECALLED, null, NOW);
        when(messageRepository.findById(recalled.id())).thenReturn(Optional.of(recalled));
        assertThatThrownBy(() -> service.recall("alice", recalled.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经撤回");

        when(messageRepository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.recall("alice", "nope"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息不存在");
    }

    @Test
    void sendImageRequiresInSiteUrl() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);

        assertThatThrownBy(() -> service.send("alice", "bob", "http://evil.com/a.png", "image", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("站内");

        PrivateMessage sent = service.send("alice", "bob", "/api/files/abc/download", "image", null);

        assertThat(sent.msgType()).isEqualTo(PrivateMessage.TYPE_IMAGE);
        assertThat(sent.content()).isEqualTo("/api/files/abc/download");
    }

    @Test
    void replyStoresQuoteIdForSameConversation() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);
        PrivateMessage quoted = text("bob", "alice", "吃火锅吗");
        when(messageRepository.findById(quoted.id())).thenReturn(Optional.of(quoted));

        PrivateMessage sent = service.send("alice", "bob", "吃！", "text", quoted.id());

        assertThat(sent.replyToId()).isEqualTo(quoted.id());
    }

    @Test
    void replyValidatesExistenceAndConversation() {
        stubFriendship("alice", "bob");

        when(messageRepository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.send("alice", "bob", "hi", "text", "nope"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");

        PrivateMessage otherChat = text("alice", "carol", "别的会话");
        when(messageRepository.findById(otherChat.id())).thenReturn(Optional.of(otherChat));
        assertThatThrownBy(() -> service.send("alice", "bob", "hi", "text", otherChat.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本会话");
    }

    @Test
    void pokeIgnoresReplyAndSearchValidatesKeyword() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);

        PrivateMessage sent = service.send("alice", "bob", null, "poke", "whatever");

        assertThat(sent.replyToId()).isNull();

        assertThatThrownBy(() -> service.search("alice", "bob", "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("关键字");
    }

    @Test
    void searchReturnsAscendingResults() {
        PrivateMessage newer = text("bob", "alice", "火锅第一");
        PrivateMessage older = textWith("m-old", "alice", "bob", "火锅第二", PrivateMessage.STATUS_SENT, null,
                NOW - 1000);
        when(messageRepository.searchConversation("alice", "bob", "火锅"))
                .thenReturn(List.of(newer, older));
        lenient().when(reactionRepository.listByMsgIds(any())).thenReturn(List.of());
        lenient().when(starRepository.listByUsernameAndMsgIds(eq("alice"), any())).thenReturn(List.of());

        List<PrivateMessageService.MessageVO> hits = service.search("alice", "bob", "火锅");

        assertThat(hits).extracting(PrivateMessageService.MessageVO::content).containsExactly("火锅第二", "火锅第一");
    }

    @Test
    void toggleReactionAddsThenRemovesAndPushesBothSides() {
        PrivateMessage message = text("bob", "alice", "晚上吃什么");
        when(messageRepository.findById(message.id())).thenReturn(Optional.of(message));
        when(reactionRepository.findByMsgIdAndUserAndEmoji(message.id(), "alice", "👍")).thenReturn(Optional.empty());

        assertThat(service.toggleReaction("alice", message.id(), "👍")).isTrue();
        verify(reactionRepository).save(any(MessageReaction.class));
        verify(push).pushReaction(message, "alice", "👍", true);

        MessageReaction existing = MessageReaction.add(message.id(), "alice", "👍", NOW);
        when(reactionRepository.findByMsgIdAndUserAndEmoji(message.id(), "alice", "👍"))
                .thenReturn(Optional.of(existing));
        assertThat(service.toggleReaction("alice", message.id(), "👍")).isFalse();
        verify(reactionRepository).deleteById(existing.id());
        verify(push).pushReaction(message, "alice", "👍", false);
    }

    @Test
    void reactionAcceptsExpandedEmojiSet() {
        PrivateMessage message = text("bob", "alice", "早呀");
        when(messageRepository.findById(message.id())).thenReturn(Optional.of(message));
        when(reactionRepository.findByMsgIdAndUserAndEmoji(message.id(), "alice", "⛵")).thenReturn(Optional.empty());

        assertThat(service.toggleReaction("alice", message.id(), "⛵")).isTrue();
        verify(reactionRepository).save(any(MessageReaction.class));
        verify(push).pushReaction(message, "alice", "⛵", true);
    }

    @Test
    void reactionValidatesEmojiAndParticipant() {
        when(messageRepository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.toggleReaction("alice", "nope", "👍"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息不存在");

        PrivateMessage otherChat = text("carol", "bob", "跟我无关");
        when(messageRepository.findById(otherChat.id())).thenReturn(Optional.of(otherChat));
        assertThatThrownBy(() -> service.toggleReaction("alice", otherChat.id(), "👍"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本会话");

        PrivateMessage message = text("bob", "alice", "在吗");
        when(messageRepository.findById(message.id())).thenReturn(Optional.of(message));
        assertThatThrownBy(() -> service.toggleReaction("alice", message.id(), "🐱"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的表情");
    }

    @Test
    void toggleStarAndListStars() {
        PrivateMessage message = text("bob", "alice", "这句要收藏");
        when(messageRepository.findById(message.id())).thenReturn(Optional.of(message));
        when(starRepository.findByUsernameAndMsgId("alice", message.id())).thenReturn(Optional.empty());

        assertThat(service.toggleStar("alice", message.id())).isTrue();
        verify(starRepository).save(any(MessageStar.class));

        when(starRepository.findByUsernameAndMsgId("alice", message.id()))
                .thenReturn(Optional.of(MessageStar.add("alice", message.id(), NOW)));
        assertThat(service.toggleStar("alice", message.id())).isFalse();

        MessageStar star = MessageStar.add("alice", message.id(), NOW);
        when(starRepository.listByUsername("alice")).thenReturn(List.of(star));
        // 收藏夹一次批量取消息：原先每条收藏 selectById 一次（N+1），改成一趟批量
        when(messageRepository.listByIds(List.of(message.id()))).thenReturn(List.of(message));

        List<PrivateMessageService.StarVO> stars = service.listStars("alice");
        assertThat(stars).hasSize(1);
        assertThat(stars.get(0).peer()).isEqualTo("bob");
        assertThat(stars.get(0).content()).isEqualTo("这句要收藏");
        verify(messageRepository).listByIds(List.of(message.id()));
    }

    @Test
    void listStarsSkipsDeletedMessagesWithoutPerStarQuery() {
        PrivateMessage alive = text("bob", "alice", "还在的");
        when(starRepository.listByUsername("alice")).thenReturn(List.of(
                MessageStar.add("alice", alive.id(), NOW), MessageStar.add("alice", "已删除的消息", NOW)));
        when(messageRepository.listByIds(any())).thenReturn(List.of(alive));

        List<PrivateMessageService.StarVO> stars = service.listStars("alice");

        // 收藏指向已删消息时静默跳过，且全程只有 1 次批量查询
        assertThat(stars).hasSize(1);
        assertThat(stars.get(0).content()).isEqualTo("还在的");
        verify(messageRepository, never()).findById(anyString());
        verify(messageRepository).listByIds(any());
    }

    @Test
    void editUpdatesContentAndBroadcasts() {
        PrivateMessage message = text("alice", "bob", "原始内容");
        when(messageRepository.findById(message.id())).thenReturn(Optional.of(message));

        PrivateMessage edited = service.edit("alice", message.id(), "  改好的内容  ");

        assertThat(edited.content()).isEqualTo("改好的内容");
        assertThat(edited.editedRaw()).isEqualTo(1);
        verify(messageRepository).save(message);
        verify(push).pushEdit(message);
    }

    @Test
    void editValidatesOwnershipTypeWindowAndState() {
        when(messageRepository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.edit("alice", "nope", "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息不存在");

        PrivateMessage other = text("bob", "alice", "别人的");
        when(messageRepository.findById(other.id())).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.edit("alice", other.id(), "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能编辑自己");

        PrivateMessage image = PrivateMessage.offer("alice", "bob", "/api/files/a/download",
                PrivateMessage.TYPE_IMAGE, NOW);
        when(messageRepository.findById(image.id())).thenReturn(Optional.of(image));
        assertThatThrownBy(() -> service.edit("alice", image.id(), "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文本消息");

        PrivateMessage old = PrivateMessage.offer("alice", "bob", "很久之前", PrivateMessage.TYPE_TEXT,
                System.currentTimeMillis() - PrivateMessage.RECALL_WINDOW_MS - 1);
        when(messageRepository.findById(old.id())).thenReturn(Optional.of(old));
        assertThatThrownBy(() -> service.edit("alice", old.id(), "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 分钟");

        PrivateMessage recalled = textWith("m-recalled-2", "alice", "bob", "已撤回",
                PrivateMessage.STATUS_RECALLED, null, NOW);
        when(messageRepository.findById(recalled.id())).thenReturn(Optional.of(recalled));
        assertThatThrownBy(() -> service.edit("alice", recalled.id(), "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已撤回");

        PrivateMessage message = text("alice", "bob", "在吗");
        when(messageRepository.findById(message.id())).thenReturn(Optional.of(message));
        assertThatThrownBy(() -> service.edit("alice", message.id(), "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
    }

    // ---------- 新一轮：82 文件消息 / 86 敏感词 / 87 限流 / 84 置顶 / 85 清空 ----------

    @Test
    void sendFileMessageRequiresInAppPayload() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);

        String payload = "{\"name\":\"报表.xlsx\",\"size\":1024,\"url\":\"/api/files/abc/download\"}";
        PrivateMessage sent = service.send("alice", "bob", payload, "file", null);
        assertThat(sent.msgType()).isEqualTo(PrivateMessage.TYPE_FILE);
        assertThat(sent.content()).isEqualTo(payload);

        // 非站内地址 / 缺名称直接拒绝
        assertThatThrownBy(() -> service.send("alice", "bob",
                "{\"name\":\"a.exe\",\"size\":1,\"url\":\"http://evil/a.exe\"}", "file", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("站内");
        assertThatThrownBy(() -> service.send("alice", "bob", "{\"size\":1,\"url\":\"/api/files/a/download\"}", "file", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("站内");
    }

    @Test
    void sensitiveWordsAreCleanedBeforeSend() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);
        lenient().when(moderation.clean(eq("alice"), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> ((String) inv.getArgument(1)).replace("赌博", "＊＊"));

        PrivateMessage sent = service.send("alice", "bob", "这里有赌博内容", "text", null);
        assertThat(sent.content()).isEqualTo("这里有＊＊内容");
    }

    @Test
    void rateLimitRejectsFloodBeforeFriendshipCheck() {
        org.mockito.Mockito.doThrow(new BusinessException(429, "发送太快了"))
                .when(rateLimiter).check("alice");
        assertThatThrownBy(() -> service.send("alice", "bob", "在吗", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发送太快");
        verify(messageRepository, never()).save(any(PrivateMessage.class));
    }

    @Test
    void pinReplacesPreviousPinAndUnpinClears() {
        stubFriendship("alice", "bob");
        PrivateMessage message = text("alice", "bob", "重点");
        when(messageRepository.findById(message.id())).thenReturn(Optional.of(message));

        PrivateMessageService.PinVO pin = service.pin("alice", "bob", message.id());
        assertThat(pin.msgId()).isEqualTo(message.id());
        // 「一个会话最多一条」由 ConversationPinRepository.replace 承担（先删后插的用例见适配器测试）；
        // 这里锁规范化后的会话双方与改造前一致：字典序小者在 A 位。
        ArgumentCaptor<ConversationPin> replace = ArgumentCaptor.forClass(ConversationPin.class);
        verify(pinRepository).replace(replace.capture());
        assertThat(replace.getValue().userA()).isEqualTo("alice");
        assertThat(replace.getValue().userB()).isEqualTo("bob");
        assertThat(replace.getValue().createdBy()).isEqualTo("alice");
        verify(push).pushPin(eq("alice"), eq("bob"), eq(message.id()), eq(true));

        service.unpin("alice", "bob");
        verify(pinRepository).clear(Conversation.between("alice", "bob"));
        verify(push).pushPin(eq("alice"), eq("bob"), org.mockito.ArgumentMatchers.isNull(), eq(false));
    }
}
