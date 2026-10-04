package com.smart.chat.messaging.application;

import com.alibaba.fastjson2.JSON;
import com.smart.chat.messaging.domain.conversation.PrivateMessage;
import com.smart.chat.messaging.infrastructure.persistence.PrivateMessagePO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对外 JSON 契约：改造前 {@code POST /api/messages} 与 {@code PUT /api/messages/{id}} 直接把
 * {@code private_message} 那一行（PO）序列化给前端；改造后 Service 返回领域类型、api 投成
 * {@link PrivateMessageService.SentMessageVO}。字段名、字段顺序与 null 处理必须逐字节一致，
 * 否则前端逐字段对账的页面会整块空白。
 * <p>这条用例是「改名不影响契约」的证据：它把旧形状（PO）与新形状（VO）放一起比序列化结果。
 */
class PrivateMessageResponseContractTest {

    private static PrivateMessagePO populatedPo() {
        PrivateMessagePO po = new PrivateMessagePO();
        po.setId("m1");
        po.setFromUser("alice");
        po.setToUser("bob");
        po.setContent("在吗？");
        po.setMsgType(PrivateMessage.TYPE_TEXT);
        po.setStatus(PrivateMessage.STATUS_SENT);
        po.setReplyToId("m0");
        po.setReadFlag(1);
        po.setEdited(1);
        po.setHeartAt(123L);
        po.setCreated(456L);
        return po;
    }

    @Test
    void fullRowSerializesIntoTheSameKeysInSameOrder() {
        PrivateMessagePO po = populatedPo();
        PrivateMessage message = PrivateMessage.restore(po.getId(), po.getFromUser(), po.getToUser(), po.getContent(),
                po.getMsgType(), po.getStatus(), po.getReplyToId(), po.getReadFlag(), po.getEdited(), po.getHeartAt(),
                po.getCreated());

        assertThat(JSON.toJSONString(PrivateMessageService.SentMessageVO.of(message)))
                .isEqualTo(JSON.toJSONString(po));
    }

    @Test
    void sparseRowStillMatchesIncludingTheNullsBothSidesDrop() {
        PrivateMessagePO po = new PrivateMessagePO();
        po.setId("m2");
        po.setFromUser("alice");
        po.setToUser("bob");
        po.setContent("[拍一拍]");
        po.setMsgType(PrivateMessage.TYPE_POKE);
        po.setStatus(PrivateMessage.STATUS_SENT);
        po.setCreated(9L);

        PrivateMessage message = PrivateMessage.restore(po.getId(), po.getFromUser(), po.getToUser(), po.getContent(),
                po.getMsgType(), po.getStatus(), po.getReplyToId(), po.getReadFlag(), po.getEdited(), po.getHeartAt(),
                po.getCreated());
        String projected = JSON.toJSONString(PrivateMessageService.SentMessageVO.of(message));

        assertThat(projected).isEqualTo(JSON.toJSONString(po));
        assertThat(projected).doesNotContain("readFlag");
    }

    @Test
    void projectionCarriesEveryColumnOfTheRow() {
        PrivateMessagePO po = populatedPo();
        PrivateMessage message = PrivateMessage.restore(po.getId(), po.getFromUser(), po.getToUser(), po.getContent(),
                po.getMsgType(), po.getStatus(), po.getReplyToId(), po.getReadFlag(), po.getEdited(), po.getHeartAt(),
                po.getCreated());

        PrivateMessageService.SentMessageVO vo = PrivateMessageService.SentMessageVO.of(message);

        assertThat(vo.id()).isEqualTo("m1");
        assertThat(vo.fromUser()).isEqualTo("alice");
        assertThat(vo.toUser()).isEqualTo("bob");
        assertThat(vo.content()).isEqualTo("在吗？");
        assertThat(vo.msgType()).isEqualTo(PrivateMessage.TYPE_TEXT);
        assertThat(vo.status()).isEqualTo(PrivateMessage.STATUS_SENT);
        assertThat(vo.replyToId()).isEqualTo("m0");
        assertThat(vo.readFlag()).isEqualTo(1);
        assertThat(vo.edited()).isEqualTo(1);
        assertThat(vo.heartAt()).isEqualTo(123L);
        assertThat(vo.created()).isEqualTo(456L);
    }
}
