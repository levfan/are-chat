package com.smart.chat.messaging.domain.conversation;

import com.smart.chat.messaging.domain.RuleViolation;

import java.util.UUID;

/**
 * 一条私聊消息（{@code private_message} 表的业务名，见 CONTEXT.md「私信」）。
 * <p>
 * 消息流水本身是薄的：发出去就不改历史。值得写成领域的只有三类真规则——
 * <b>内容形态</b>（文本非空且 ≤2000、拍一拍留空回落默认文案且后缀 ≤100、图片必须是站内地址、
 * 名片/位置必须是 JSON）、<b>收发双方视角</b>（{@link #peerOf}「对方」、{@link #between} 双向会话谓词）、
 * <b>两条时间窗内的改写</b>（撤回、编辑），加上心动时刻的归属闸门。
 * <p>
 * 状态字面量 SENT/RECALLED 与类型字面量是数据库与前端的契约，照 PO 原值写死。
 * {@code readFlag} 是<b>只读列</b>：已读由 {@link PrivateMessageRepository#markIncomingRead} 批量刷，
 * 聚合读它是为了「我发出去的消息 TA 读了没」，{@code save} 不回写它。
 * <p>
 * 有意留在 application 的两件事：敏感词过滤（{@code ModerationService} 读配置，属应用服务）、
 * 文件消息 payload 解析（领域层不许 import fastjson，见 02 第五节守卫第 1 条）。
 */
public final class PrivateMessage {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_POKE = "poke";
    public static final String TYPE_SYSTEM = "system";
    /** 72 好友名片卡片：content 为 JSON（username/nickname/signature/avatar） */
    public static final String TYPE_CARD = "card";
    /** 73 位置分享卡片：content 为 JSON（name/address） */
    public static final String TYPE_LOCATION = "location";
    /** 82 文件消息：content 为 JSON（name/size/url），url 必须是站内下载地址 */
    public static final String TYPE_FILE = "file";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_RECALLED = "RECALLED";
    public static final String POKE_TEXT = "[拍一拍]";
    /** 拍一拍自定义后缀最长长度（67） */
    public static final int POKE_SUFFIX_MAX = 100;
    public static final String IMAGE_URL_PREFIX = "/api/files/";
    /** 撤回与编辑共用的两分钟窗口 */
    public static final long RECALL_WINDOW_MS = 2 * 60 * 1000L;
    /** 文本消息最长 2000 字（与列宽同值） */
    public static final int CONTENT_MAX = 2000;

    private final String id;
    private final String fromUser;
    private final String toUser;
    private final String msgType;
    private final Long created;
    private final Integer readFlag;
    private String content;
    private String status;
    private String replyToId;
    private Integer edited;
    /** F36 心动时刻标记时间（毫秒，null = 未标记），消息双方均可标记/取消 */
    private Long heartAt;

    private PrivateMessage(String id, String fromUser, String toUser, String content, String msgType, String status,
                           String replyToId, Integer readFlag, Integer edited, Long heartAt, Long created) {
        this.id = id;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.content = content;
        this.msgType = msgType;
        this.status = status;
        this.replyToId = replyToId;
        this.readFlag = readFlag;
        this.edited = edited;
        this.heartAt = heartAt;
        this.created = created;
    }

    /**
     * 发一条消息：按类型过内容形态闸门。传入的 content 必须是<b>已经过敏感词处理</b>的文本
     * （过滤属应用服务），且与改造前一样按调用方 trim 后的值判定。
     */
    public static PrivateMessage offer(String from, String to, String content, String msgType, long at) {
        String text = requireWellFormed(msgType, content);
        return new PrivateMessage(UUID.randomUUID().toString(), from, to, text, msgType, STATUS_SENT,
                null, null, null, null, at);
    }

    /** 从存储重建：不校验，历史行里的空值与怪内容必须读得出来。 */
    public static PrivateMessage restore(String id, String fromUser, String toUser, String content, String msgType,
                                         String status, String replyToId, Integer readFlag, Integer edited,
                                         Long heartAt, Long created) {
        return new PrivateMessage(id, fromUser, toUser, content, msgType, status, replyToId, readFlag, edited,
                heartAt, created);
    }

    /**
     * 请求里的 {@code type} 落到哪个消息类型：认不出的统统按文本处理（沿用改造前的兜底口径）。
     */
    public static String resolveType(String requested) {
        if (TYPE_POKE.equals(requested)) {
            return TYPE_POKE;
        }
        if (TYPE_IMAGE.equals(requested)) {
            return TYPE_IMAGE;
        }
        if (TYPE_CARD.equals(requested)) {
            return TYPE_CARD;
        }
        if (TYPE_LOCATION.equals(requested)) {
            return TYPE_LOCATION;
        }
        if (TYPE_FILE.equals(requested)) {
            return TYPE_FILE;
        }
        return TYPE_TEXT;
    }

    /**
     * 内容形态裁决：顺序与改造前逐字对齐（先长度再站内地址，文案与 code 一字不动）。
     * 系统消息与文件消息不做文本长度校验——前者文案由服务端写死、后者 content 是 JSON payload，
     * 这两条在改造前就没有校验，这里不新增。
     */
    private static String requireWellFormed(String msgType, String content) {
        if (TYPE_POKE.equals(msgType)) {
            String suffix = content == null || content.isEmpty() ? POKE_TEXT : content;
            if (suffix.length() > POKE_SUFFIX_MAX) {
                throw RuleViolation.of("拍一拍后缀最长 " + POKE_SUFFIX_MAX + " 字");
            }
            return suffix;
        }
        if (TYPE_FILE.equals(msgType) || TYPE_SYSTEM.equals(msgType)) {
            return content;
        }
        String text = content == null ? "" : content;
        if (text.isEmpty()) {
            throw RuleViolation.of("消息内容不能为空");
        }
        if (text.length() > CONTENT_MAX) {
            throw RuleViolation.of("消息最长 " + CONTENT_MAX + " 字");
        }
        if (TYPE_IMAGE.equals(msgType) && !text.startsWith(IMAGE_URL_PREFIX)) {
            throw RuleViolation.of("图片消息必须先上传到站内");
        }
        if ((TYPE_CARD.equals(msgType) || TYPE_LOCATION.equals(msgType)) && !text.startsWith("{")) {
            throw RuleViolation.of("卡片消息内容格式不正确");
        }
        return text;
    }

    /** 撤回：只有发送方能撤、只能撤一次、只有两分钟内有效。 */
    public void recallBy(String me, long now) {
        if (!sentBy(me)) {
            throw RuleViolation.forbidden("只能撤回自己发的消息");
        }
        if (recalledFlag()) {
            throw RuleViolation.conflict("这条消息已经撤回过了");
        }
        if (now - created > RECALL_WINDOW_MS) {
            throw RuleViolation.of("超过 2 分钟，撤不回来了～");
        }
        this.status = STATUS_RECALLED;
    }

    /** 编辑：两分钟内改自己发的文本消息，改完打上「已编辑」标记。 */
    public void editBy(String me, long now, String newContent) {
        if (!sentBy(me)) {
            throw RuleViolation.forbidden("只能编辑自己发的消息");
        }
        if (!TYPE_TEXT.equals(msgType)) {
            throw RuleViolation.of("只有文本消息可以编辑");
        }
        if (recalledFlag()) {
            throw RuleViolation.conflict("消息已撤回，不能编辑");
        }
        if (now - created > RECALL_WINDOW_MS) {
            throw RuleViolation.of("超过 2 分钟，不能编辑了");
        }
        String text = newContent == null ? "" : newContent.trim();
        if (text.isEmpty()) {
            throw RuleViolation.of("消息内容不能为空");
        }
        if (text.length() > CONTENT_MAX) {
            throw RuleViolation.of("消息最长 " + CONTENT_MAX + " 字");
        }
        this.content = text;
        this.edited = 1;
    }

    /** F36 心动时刻：只有消息双方能标记/取消，撤回的消息不能再标。 */
    public void heartBy(String me, boolean hearted, long now) {
        if (!participant(me)) {
            throw RuleViolation.forbidden("只能标记你们俩的聊天记录哦");
        }
        if (recalledFlag()) {
            throw RuleViolation.conflict("撤回的消息不能标记");
        }
        this.heartAt = hearted ? now : null;
    }

    /** 引用回复：只能引本会话（同一对人）的消息。 */
    public void quote(String quotedId) {
        this.replyToId = quotedId;
    }

    /** 双向会话谓词：这条消息是不是 {@code one} 与 {@code other} 之间的一来一回。 */
    public void requireBetween(String one, String other) {
        boolean sameConversation = (one.equals(fromUser) && other.equals(toUser))
                || (other.equals(fromUser) && one.equals(toUser));
        if (!sameConversation) {
            throw RuleViolation.of("只能引用本会话内的消息");
        }
    }

    /** 84 置顶：已撤回的消息不能当置顶（顶一条看不见的消息没有意义）。 */
    public void requirePinable() {
        if (recalledFlag()) {
            throw RuleViolation.of("已撤回的消息不能置顶");
        }
    }

    /** 只能动本会话里的消息（回应/收藏/置顶用的闸门，文案沿用改造前）。 */
    public void requireParticipant(String me) {
        if (!participant(me)) {
            throw RuleViolation.forbidden("只能操作本会话内的消息");
        }
    }

    public boolean sentBy(String me) {
        return fromUser.equals(me);
    }

    public boolean receivedBy(String me) {
        return toUser.equals(me);
    }

    public boolean participant(String me) {
        return sentBy(me) || receivedBy(me);
    }

    /** 站在 {@code me} 视角看这条消息的「对方」是谁（不校验 me 是否参与，沿用改造前的三元表达式口径）。 */
    public String peerOf(String me) {
        return fromUser.equals(me) ? toUser : fromUser;
    }

    public boolean recalledFlag() {
        return STATUS_RECALLED.equals(status);
    }

    public boolean editedFlag() {
        return Integer.valueOf(1).equals(edited);
    }

    /** 只读列：我发出去的那条，对方读了没。 */
    public boolean readFlag() {
        return Integer.valueOf(1).equals(readFlag);
    }

    public String id() {
        return id;
    }

    public String fromUser() {
        return fromUser;
    }

    public String toUser() {
        return toUser;
    }

    public String content() {
        return content;
    }

    public String msgType() {
        return msgType;
    }

    public String status() {
        return status;
    }

    public String replyToId() {
        return replyToId;
    }

    /** 已读位的原始值（1/0/null）；判定请用 {@link #readFlag()}。 */
    public Integer readFlagRaw() {
        return readFlag;
    }

    /** 编辑位的原始值（1/0/null）；判定请用 {@link #editedFlag()}。 */
    public Integer editedRaw() {
        return edited;
    }

    public Long heartAt() {
        return heartAt;
    }

    public Long created() {
        return created;
    }
}
