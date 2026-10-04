package com.smart.chat.messaging.domain.conversation;

import com.smart.chat.messaging.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 消息领域的两条真规则：<b>内容形态</b>（按类型各自的闸门）与<b>时间窗内的改写</b>（撤回/编辑/心动）。
 * 文案与 code 沿用改造前写在 Service 里的那一套，逐条锁住。
 */
class PrivateMessageTest {

    private static final long NOW = System.currentTimeMillis();

    private static PrivateMessage text(String from, String to, String content) {
        return PrivateMessage.offer(from, to, content, PrivateMessage.TYPE_TEXT, NOW);
    }

    @Test
    void sentMessageStartsUnreadUneditedAndSent() {
        PrivateMessage message = text("alice", "bob", "在吗？");

        assertThat(message.id()).isNotBlank();
        assertThat(message.status()).isEqualTo(PrivateMessage.STATUS_SENT);
        assertThat(message.readFlag()).isFalse();
        assertThat(message.editedFlag()).isFalse();
        assertThat(message.heartAt()).isNull();
        assertThat(message.created()).isEqualTo(NOW);
    }

    @Test
    void blankAndOverlongTextAreRejectedWithTheirOwnWords() {
        assertThatThrownBy(() -> text("alice", "bob", ""))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("消息内容不能为空");
        assertThatThrownBy(() -> text("alice", "bob", "x".repeat(2001)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("消息最长 2000 字");
    }

    @Test
    void imageMustComeFromInAppStorageAndCardMustBeJson() {
        assertThatThrownBy(() -> PrivateMessage.offer("alice", "bob", "http://evil/a.png",
                PrivateMessage.TYPE_IMAGE, NOW))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("图片消息必须先上传到站内");
        assertThatThrownBy(() -> PrivateMessage.offer("alice", "bob", "不是JSON",
                PrivateMessage.TYPE_CARD, NOW))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("卡片消息内容格式不正确");

        assertThat(PrivateMessage.offer("alice", "bob", "/api/files/f1/download",
                PrivateMessage.TYPE_IMAGE, NOW).msgType()).isEqualTo(PrivateMessage.TYPE_IMAGE);
    }

    @Test
    void pokeFallsBackToDefaultSuffixAndCapsItsLength() {
        assertThat(PrivateMessage.offer("alice", "bob", "", PrivateMessage.TYPE_POKE, NOW).content())
                .isEqualTo(PrivateMessage.POKE_TEXT);
        assertThat(PrivateMessage.offer("alice", "bob", "的小脑袋", PrivateMessage.TYPE_POKE, NOW).content())
                .isEqualTo("的小脑袋");
        assertThatThrownBy(() -> PrivateMessage.offer("alice", "bob", "x".repeat(101), PrivateMessage.TYPE_POKE, NOW))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("拍一拍后缀最长 100 字");
    }

    @Test
    void systemAndFileContentSkipTheTextGates() {
        // 系统文案由服务端写死、文件消息 content 是 JSON payload：改造前就没有长度校验，不许新增
        String longPayload = "x".repeat(3_000);
        assertThat(PrivateMessage.offer("system", "bob", longPayload, PrivateMessage.TYPE_SYSTEM, NOW).content())
                .isEqualTo(longPayload);
        assertThat(PrivateMessage.offer("alice", "bob", longPayload, PrivateMessage.TYPE_FILE, NOW).content())
                .isEqualTo(longPayload);
    }

    @Test
    void typeResolutionFallsBackToText() {
        assertThat(PrivateMessage.resolveType("poke")).isEqualTo(PrivateMessage.TYPE_POKE);
        assertThat(PrivateMessage.resolveType("image")).isEqualTo(PrivateMessage.TYPE_IMAGE);
        assertThat(PrivateMessage.resolveType("card")).isEqualTo(PrivateMessage.TYPE_CARD);
        assertThat(PrivateMessage.resolveType("location")).isEqualTo(PrivateMessage.TYPE_LOCATION);
        assertThat(PrivateMessage.resolveType("file")).isEqualTo(PrivateMessage.TYPE_FILE);
        assertThat(PrivateMessage.resolveType("dance")).isEqualTo(PrivateMessage.TYPE_TEXT);
        assertThat(PrivateMessage.resolveType(null)).isEqualTo(PrivateMessage.TYPE_TEXT);
    }

    @Test
    void onlySenderCanRecallOnceInsideTheWindow() {
        PrivateMessage other = text("bob", "alice", "别人的");
        assertThatThrownBy(() -> other.recallBy("alice", NOW))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("只能撤回自己发的消息")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);

        PrivateMessage old = PrivateMessage.offer("alice", "bob", "很久之前", PrivateMessage.TYPE_TEXT,
                NOW - PrivateMessage.RECALL_WINDOW_MS - 1);
        assertThatThrownBy(() -> old.recallBy("alice", NOW))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("超过 2 分钟，撤不回来了～")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(400);

        PrivateMessage message = text("alice", "bob", "说错话了");
        message.recallBy("alice", NOW);
        assertThat(message.status()).isEqualTo(PrivateMessage.STATUS_RECALLED);
        assertThat(message.recalledFlag()).isTrue();
        assertThatThrownBy(() -> message.recallBy("alice", NOW))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("这条消息已经撤回过了")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(409);
    }

    @Test
    void onlySenderCanEditTextOnceInsideTheWindow() {
        PrivateMessage image = PrivateMessage.offer("alice", "bob", "/api/files/a/download",
                PrivateMessage.TYPE_IMAGE, NOW);
        assertThatThrownBy(() -> image.editBy("alice", NOW, "hi"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("只有文本消息可以编辑");

        PrivateMessage recalled = PrivateMessage.restore("m1", "alice", "bob", "已撤回", PrivateMessage.TYPE_TEXT,
                PrivateMessage.STATUS_RECALLED, null, null, null, null, NOW);
        assertThatThrownBy(() -> recalled.editBy("alice", NOW, "hi"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("消息已撤回，不能编辑")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(409);

        PrivateMessage message = text("alice", "bob", "原始内容");
        message.editBy("alice", NOW, "  改好的内容  ");
        assertThat(message.content()).isEqualTo("改好的内容");
        assertThat(message.editedRaw()).isEqualTo(1);
        assertThat(message.editedFlag()).isTrue();
    }

    @Test
    void editKeepsTheGateOrderOwnerThenWindowThenContent() {
        PrivateMessage old = PrivateMessage.offer("bob", "alice", "很久之前", PrivateMessage.TYPE_TEXT,
                NOW - PrivateMessage.RECALL_WINDOW_MS - 1);
        // 先撞归属闸门（403），不会被窗口的 400 抢话
        assertThatThrownBy(() -> old.editBy("alice", NOW, "hi"))
                .hasMessage("只能编辑自己发的消息");
        PrivateMessage mine = PrivateMessage.offer("alice", "bob", "很久之前", PrivateMessage.TYPE_TEXT,
                NOW - PrivateMessage.RECALL_WINDOW_MS - 1);
        assertThatThrownBy(() -> mine.editBy("alice", NOW, "hi")).hasMessage("超过 2 分钟，不能编辑了");
        assertThatThrownBy(() -> mine.editBy("alice", NOW - PrivateMessage.RECALL_WINDOW_MS + 1, "   "))
                .hasMessage("消息内容不能为空");
    }

    @Test
    void bothSidesCanMarkHeartButRecalledCannot() {
        PrivateMessage message = text("alice", "bob", "这一刻");
        message.heartBy("bob", true, 4_242L);
        assertThat(message.heartAt()).isEqualTo(4_242L);
        message.heartBy("alice", false, 5L);
        assertThat(message.heartAt()).isNull();

        PrivateMessage stranger = text("alice", "bob", "跟你无关");
        assertThatThrownBy(() -> stranger.heartBy("carol", true, NOW))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("只能标记你们俩的聊天记录哦")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);

        PrivateMessage recalled = PrivateMessage.restore("m2", "alice", "bob", "已撤回", PrivateMessage.TYPE_TEXT,
                PrivateMessage.STATUS_RECALLED, null, null, null, null, NOW);
        assertThatThrownBy(() -> recalled.heartBy("alice", true, NOW))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("撤回的消息不能标记")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(409);
    }

    @Test
    void recalledMessageCannotBePinnedButOthersCan() {
        PrivateMessage recalled = PrivateMessage.restore("m3", "alice", "bob", "已撤回", PrivateMessage.TYPE_TEXT,
                PrivateMessage.STATUS_RECALLED, null, null, null, null, NOW);
        assertThatThrownBy(() -> recalled.requirePinable())
                .isInstanceOf(RuleViolation.class)
                .hasMessage("已撤回的消息不能置顶")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(400);
        assertThatThrownBy(() -> text("alice", "bob", "在吗").requireParticipant("carol"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("只能操作本会话内的消息")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);
    }

    @Test
    void quoteMustComeFromTheSameConversationWhicheverDirection() {
        PrivateMessage mine = text("alice", "bob", "吃！");
        mine.quote(text("bob", "alice", "吃火锅吗").id());
        assertThat(mine.replyToId()).isNotBlank();

        assertThatThrownBy(() -> text("alice", "carol", "别的会话").requireBetween("alice", "bob"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("只能引用本会话内的消息")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(400);
        // 反方向（对方发给我的）算同一个会话
        text("bob", "alice", "吃火锅吗").requireBetween("alice", "bob");
    }

    @Test
    void peerOfAnswersFromWhicheverSideYouStand() {
        PrivateMessage message = text("alice", "bob", "在吗");

        assertThat(message.peerOf("alice")).isEqualTo("bob");
        assertThat(message.peerOf("bob")).isEqualTo("alice");
        assertThat(message.sentBy("alice")).isTrue();
        assertThat(message.receivedBy("bob")).isTrue();
        assertThat(message.readFlagRaw()).isNull();
    }
}
