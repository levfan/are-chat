package com.smart.chat.im;

import com.smart.chat.auth.AppUserService;
import com.smart.chat.common.BusinessException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrivateMessageServiceTest {

    @Mock
    private PrivateMessageMapper messageMapper;

    @Mock
    private FriendMapper friendMapper;

    @Mock
    private MessageReactionMapper reactionMapper;

    @Mock
    private MessageStarMapper starMapper;

    @Mock
    private ImPushService push;

    @Mock
    private AppUserService userService;

    @Mock
    private com.smart.chat.im.ModerationService moderation;

    @Mock
    private com.smart.chat.im.MessageRateLimiter rateLimiter;

    @Mock
    private ConversationPinMapper pinMapper;

    @InjectMocks
    private PrivateMessageService service;

    /** 合法用户目录：默认「输入规范化 + 用户存在」，个别用例可覆盖 */
    @org.junit.jupiter.api.BeforeEach
    void stubUserDirectory() {
        lenient().when(userService.normalizeUsername(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).trim().toLowerCase());
        lenient().when(userService.exists(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        // 86 敏感词过滤默认放行（返回原文）；限流默认不触发（void 方法空实现）
        lenient().when(moderation.clean(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> inv.getArgument(1, String.class));
    }

    private void stubFriendship(String me, String peer) {
        lenient().when(friendMapper.findByOwnerAndFriend(me, peer))
                .thenReturn(Optional.of(Friend.of(me, peer)));
    }

    @Test
    void sendPersistsAndPushesToOnlinePeer() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(true);

        PrivateMessage sent = service.send("alice", "bob", "  在吗？  ", "text", null);

        assertThat(sent.getFromUser()).isEqualTo("alice");
        assertThat(sent.getToUser()).isEqualTo("bob");
        assertThat(sent.getContent()).isEqualTo("在吗？");
        assertThat(sent.getStatus()).isEqualTo(PrivateMessage.STATUS_SENT);
        verify(messageMapper).insert(sent);
        verify(push).pushDm(sent);
    }

    @Test
    void sendOfflinePeerStillStoresWithoutPush() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);

        PrivateMessage sent = service.send("alice", "bob", "留言", "text", null);

        verify(messageMapper).insert(sent);
        verify(push, never()).pushDm(any());
    }

    @Test
    void sendPokeKeepsCustomSuffix() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);

        // 67 拍一拍支持自定义后缀；留空回落到默认文案
        assertThat(service.send("alice", "bob", "的小脑袋", "poke", null).getContent())
                .isEqualTo("的小脑袋");
        PrivateMessage blank = service.send("alice", "bob", "   ", "poke", null);
        assertThat(blank.getMsgType()).isEqualTo(PrivateMessage.TYPE_POKE);
        assertThat(blank.getContent()).isEqualTo(PrivateMessage.POKE_TEXT);
    }

    @Test
    void sendValidatesFriendshipAndInput() {
        // 合法用户校验先于好友校验：未注册的账号直接拒绝
        when(userService.exists("不存在的人")).thenReturn(false);
        assertThatThrownBy(() -> service.send("alice", "不存在的人", "hi", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("查无此人");

        when(friendMapper.findByOwnerAndFriend("alice", "carol")).thenReturn(Optional.empty());
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
        Friend mine = Friend.of("alice", "bob");
        mine.setBlocked(1);
        when(friendMapper.findByOwnerAndFriend("alice", "bob")).thenReturn(Optional.of(mine));
        assertThatThrownBy(() -> service.send("alice", "bob", "hi", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已拉黑对方");

        // 对方拉黑了我
        stubFriendship("alice", "bob");
        Friend theirs = Friend.of("bob", "alice");
        theirs.setBlocked(1);
        when(friendMapper.findByOwnerAndFriend("bob", "alice")).thenReturn(Optional.of(theirs));
        assertThatThrownBy(() -> service.send("alice", "bob", "hi", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对方已将你拉黑");
    }

    @Test
    void historyReturnsAscendingPageWithEnrichment() {
        PrivateMessage later = PrivateMessage.of("bob", "alice", "第二条", PrivateMessage.TYPE_TEXT);
        PrivateMessage earlier = PrivateMessage.of("alice", "bob", "第一条", PrivateMessage.TYPE_TEXT);
        earlier.setCreated(later.getCreated() - 1000);
        earlier.setReadFlag(1);
        when(messageMapper.findConversationPage("alice", "bob", null, 20))
                .thenReturn(List.of(later, earlier));
        when(reactionMapper.findByMsgIds(any())).thenReturn(
                List.of(MessageReaction.of(earlier.getId(), "bob", "👍")));
        when(starMapper.findByUsernameAndMsgIds(eq("alice"), any())).thenReturn(
                List.of(MessageStar.of("alice", earlier.getId())));

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
        Friend row = Friend.of("alice", "bob");
        row.setLastReadAt(0L);
        when(friendMapper.findByOwnerAndFriend("alice", "bob")).thenReturn(Optional.of(row));

        service.markRead("alice", "bob");

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendMapper).updateById(captor.capture());
        assertThat(captor.getValue().getLastReadAt()).isGreaterThan(0);
        verify(messageMapper).markIncomingRead("bob", "alice");
        verify(push).pushRead("alice", "bob");

        when(friendMapper.findByOwnerAndFriend("alice", "路人甲")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead("alice", "路人甲"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还不是好友");
    }

    @Test
    void recallWithinWindowNotifiesPeer() {
        PrivateMessage message = PrivateMessage.of("alice", "bob", "说错话了", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(message.getId())).thenReturn(message);
        when(push.isOnline("bob")).thenReturn(true);

        service.recall("alice", message.getId());

        assertThat(message.getStatus()).isEqualTo(PrivateMessage.STATUS_RECALLED);
        verify(messageMapper).updateById(message);
        verify(push).pushRecall(message);
    }

    @Test
    void recallValidatesOwnerWindowAndState() {
        PrivateMessage other = PrivateMessage.of("bob", "alice", "别人的", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(other.getId())).thenReturn(other);
        assertThatThrownBy(() -> service.recall("alice", other.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能撤回自己");

        PrivateMessage old = PrivateMessage.of("alice", "bob", "很久之前", PrivateMessage.TYPE_TEXT);
        old.setCreated(System.currentTimeMillis() - PrivateMessage.RECALL_WINDOW_MS - 1);
        when(messageMapper.selectById(old.getId())).thenReturn(old);
        assertThatThrownBy(() -> service.recall("alice", old.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 分钟");

        PrivateMessage recalled = PrivateMessage.of("alice", "bob", "已撤回", PrivateMessage.TYPE_TEXT);
        recalled.setStatus(PrivateMessage.STATUS_RECALLED);
        when(messageMapper.selectById(recalled.getId())).thenReturn(recalled);
        assertThatThrownBy(() -> service.recall("alice", recalled.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经撤回");

        when(messageMapper.selectById("nope")).thenReturn(null);
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

        assertThat(sent.getMsgType()).isEqualTo(PrivateMessage.TYPE_IMAGE);
        assertThat(sent.getContent()).isEqualTo("/api/files/abc/download");
    }

    @Test
    void replyStoresQuoteIdForSameConversation() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);
        PrivateMessage quoted = PrivateMessage.of("bob", "alice", "吃火锅吗", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(quoted.getId())).thenReturn(quoted);

        PrivateMessage sent = service.send("alice", "bob", "吃！", "text", quoted.getId());

        assertThat(sent.getReplyToId()).isEqualTo(quoted.getId());
    }

    @Test
    void replyValidatesExistenceAndConversation() {
        stubFriendship("alice", "bob");

        when(messageMapper.selectById("nope")).thenReturn(null);
        assertThatThrownBy(() -> service.send("alice", "bob", "hi", "text", "nope"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");

        PrivateMessage otherChat = PrivateMessage.of("alice", "carol", "别的会话", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(otherChat.getId())).thenReturn(otherChat);
        assertThatThrownBy(() -> service.send("alice", "bob", "hi", "text", otherChat.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本会话");
    }

    @Test
    void pokeIgnoresReplyAndSearchValidatesKeyword() {
        stubFriendship("alice", "bob");
        when(push.isOnline("bob")).thenReturn(false);

        PrivateMessage sent = service.send("alice", "bob", null, "poke", "whatever");

        assertThat(sent.getReplyToId()).isNull();

        assertThatThrownBy(() -> service.search("alice", "bob", "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("关键字");
    }

    @Test
    void searchReturnsAscendingResults() {
        PrivateMessage newer = PrivateMessage.of("bob", "alice", "火锅第一", PrivateMessage.TYPE_TEXT);
        PrivateMessage older = PrivateMessage.of("alice", "bob", "火锅第二", PrivateMessage.TYPE_TEXT);
        older.setCreated(newer.getCreated() - 1000);
        when(messageMapper.searchConversation("alice", "bob", "火锅"))
                .thenReturn(List.of(newer, older));
        lenient().when(reactionMapper.findByMsgIds(any())).thenReturn(List.of());
        lenient().when(starMapper.findByUsernameAndMsgIds(eq("alice"), any())).thenReturn(List.of());

        List<PrivateMessageService.MessageVO> hits = service.search("alice", "bob", "火锅");

        assertThat(hits).extracting(PrivateMessageService.MessageVO::content).containsExactly("火锅第二", "火锅第一");
    }

    @Test
    void toggleReactionAddsThenRemovesAndPushesBothSides() {
        PrivateMessage message = PrivateMessage.of("bob", "alice", "晚上吃什么", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(message.getId())).thenReturn(message);
        when(reactionMapper.findUnique(message.getId(), "alice", "👍")).thenReturn(null);

        assertThat(service.toggleReaction("alice", message.getId(), "👍")).isTrue();
        verify(reactionMapper).insert(any(MessageReaction.class));
        verify(push).pushReaction(message, "alice", "👍", true);

        MessageReaction existing = MessageReaction.of(message.getId(), "alice", "👍");
        when(reactionMapper.findUnique(message.getId(), "alice", "👍")).thenReturn(existing);
        assertThat(service.toggleReaction("alice", message.getId(), "👍")).isFalse();
        verify(reactionMapper).deleteById(existing.getId());
        verify(push).pushReaction(message, "alice", "👍", false);
    }

    @Test
    void reactionValidatesEmojiAndParticipant() {
        when(messageMapper.selectById("nope")).thenReturn(null);
        assertThatThrownBy(() -> service.toggleReaction("alice", "nope", "👍"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息不存在");

        PrivateMessage otherChat = PrivateMessage.of("carol", "bob", "跟我无关", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(otherChat.getId())).thenReturn(otherChat);
        assertThatThrownBy(() -> service.toggleReaction("alice", otherChat.getId(), "👍"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本会话");

        PrivateMessage message = PrivateMessage.of("bob", "alice", "在吗", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(message.getId())).thenReturn(message);
        assertThatThrownBy(() -> service.toggleReaction("alice", message.getId(), "🐱"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的表情");
    }

    @Test
    void toggleStarAndListStars() {
        PrivateMessage message = PrivateMessage.of("bob", "alice", "这句要收藏", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(message.getId())).thenReturn(message);
        when(starMapper.findUnique("alice", message.getId())).thenReturn(null);

        assertThat(service.toggleStar("alice", message.getId())).isTrue();
        verify(starMapper).insert(any(MessageStar.class));

        when(starMapper.findUnique("alice", message.getId()))
                .thenReturn(MessageStar.of("alice", message.getId()));
        assertThat(service.toggleStar("alice", message.getId())).isFalse();

        MessageStar star = MessageStar.of("alice", message.getId());
        when(starMapper.findByUsername("alice")).thenReturn(List.of(star));
        when(messageMapper.selectById(message.getId())).thenReturn(message);

        List<PrivateMessageService.StarVO> stars = service.listStars("alice");
        assertThat(stars).hasSize(1);
        assertThat(stars.get(0).peer()).isEqualTo("bob");
        assertThat(stars.get(0).content()).isEqualTo("这句要收藏");
    }

    @Test
    void editUpdatesContentAndBroadcasts() {
        PrivateMessage message = PrivateMessage.of("alice", "bob", "原始内容", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(message.getId())).thenReturn(message);

        PrivateMessage edited = service.edit("alice", message.getId(), "  改好的内容  ");

        assertThat(edited.getContent()).isEqualTo("改好的内容");
        assertThat(edited.getEdited()).isEqualTo(1);
        verify(messageMapper).updateById(message);
        verify(push).pushEdit(message);
    }

    @Test
    void editValidatesOwnershipTypeWindowAndState() {
        when(messageMapper.selectById("nope")).thenReturn(null);
        assertThatThrownBy(() -> service.edit("alice", "nope", "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息不存在");

        PrivateMessage other = PrivateMessage.of("bob", "alice", "别人的", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(other.getId())).thenReturn(other);
        assertThatThrownBy(() -> service.edit("alice", other.getId(), "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能编辑自己");

        PrivateMessage image = PrivateMessage.of("alice", "bob", "/api/files/a/download", PrivateMessage.TYPE_IMAGE);
        when(messageMapper.selectById(image.getId())).thenReturn(image);
        assertThatThrownBy(() -> service.edit("alice", image.getId(), "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文本消息");

        PrivateMessage old = PrivateMessage.of("alice", "bob", "很久之前", PrivateMessage.TYPE_TEXT);
        old.setCreated(System.currentTimeMillis() - PrivateMessage.RECALL_WINDOW_MS - 1);
        when(messageMapper.selectById(old.getId())).thenReturn(old);
        assertThatThrownBy(() -> service.edit("alice", old.getId(), "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 分钟");

        PrivateMessage recalled = PrivateMessage.of("alice", "bob", "已撤回", PrivateMessage.TYPE_TEXT);
        recalled.setStatus(PrivateMessage.STATUS_RECALLED);
        when(messageMapper.selectById(recalled.getId())).thenReturn(recalled);
        assertThatThrownBy(() -> service.edit("alice", recalled.getId(), "hi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已撤回");

        PrivateMessage message = PrivateMessage.of("alice", "bob", "在吗", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(message.getId())).thenReturn(message);
        assertThatThrownBy(() -> service.edit("alice", message.getId(), "   "))
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
        assertThat(sent.getMsgType()).isEqualTo(PrivateMessage.TYPE_FILE);
        assertThat(sent.getContent()).isEqualTo(payload);

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
        assertThat(sent.getContent()).isEqualTo("这里有＊＊内容");
    }

    @Test
    void rateLimitRejectsFloodBeforeFriendshipCheck() {
        org.mockito.Mockito.doThrow(new BusinessException(429, "发送太快了"))
                .when(rateLimiter).check("alice");
        assertThatThrownBy(() -> service.send("alice", "bob", "在吗", "text", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发送太快");
        verify(messageMapper, never()).insert(any(PrivateMessage.class));
    }

    @Test
    void pinReplacesPreviousPinAndUnpinClears() {
        stubFriendship("alice", "bob");
        PrivateMessage message = PrivateMessage.of("alice", "bob", "重点", PrivateMessage.TYPE_TEXT);
        when(messageMapper.selectById(message.getId())).thenReturn(message);

        PrivateMessageService.PinVO pin = service.pin("alice", "bob", message.getId());
        assertThat(pin.msgId()).isEqualTo(message.getId());
        verify(pinMapper).deleteForConversation("alice", "bob");
        verify(pinMapper).insert(any(ConversationPin.class));
        verify(push).pushPin(eq("alice"), eq("bob"), eq(message.getId()), eq(true));

        service.unpin("alice", "bob");
        verify(pinMapper, org.mockito.Mockito.times(2)).deleteForConversation("alice", "bob");
        verify(push).pushPin(eq("alice"), eq("bob"), org.mockito.ArgumentMatchers.isNull(), eq(false));
    }
}
