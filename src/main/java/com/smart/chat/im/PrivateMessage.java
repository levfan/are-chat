package com.smart.chat.im;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 点对点私聊消息：text / image / poke / system，SENT / RECALLED。
 * image 的 content 存站内下载地址（/api/files/{id}/download）；replyToId 为引用的消息 id。
 */
@TableName("private_message")
public class PrivateMessage {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_POKE = "poke";
    public static final String TYPE_SYSTEM = "system";
    /** 72 好友名片卡片：content 为 JSON（username/nickname/signature/avatar） */
    public static final String TYPE_CARD = "card";
    /** 73 位置分享卡片：content 为 JSON（name/address） */
    public static final String TYPE_LOCATION = "location";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_RECALLED = "RECALLED";
    public static final String POKE_TEXT = "[拍一拍]";
    /** 拍一拍自定义后缀最长长度（67） */
    public static final int POKE_SUFFIX_MAX = 100;
    public static final String IMAGE_URL_PREFIX = "/api/files/";
    public static final long RECALL_WINDOW_MS = 2 * 60 * 1000L;

    @TableId(value = "ID", type = IdType.INPUT)
    private String id;
    private String fromUser;
    private String toUser;
    private String content;
    private String msgType;
    private String status;
    private String replyToId;
    private Integer readFlag;
    private Integer edited;
    private Long created;

    public PrivateMessage() {
    }

    public static PrivateMessage of(String from, String to, String content, String msgType) {
        PrivateMessage row = new PrivateMessage();
        row.id = UUID.randomUUID().toString();
        row.fromUser = from;
        row.toUser = to;
        row.content = content;
        row.msgType = msgType;
        row.status = STATUS_SENT;
        row.created = System.currentTimeMillis();
        return row;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(String replyToId) {
        this.replyToId = replyToId;
    }

    public Integer getReadFlag() {
        return readFlag;
    }

    public void setReadFlag(Integer readFlag) {
        this.readFlag = readFlag;
    }

    public Integer getEdited() {
        return edited;
    }

    public void setEdited(Integer edited) {
        this.edited = edited;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
