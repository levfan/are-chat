package com.smart.chat.messaging.infrastructure.identity;

import com.smart.chat.identity.domain.WelcomeMessenger;
import com.smart.chat.messaging.infrastructure.persistence.PrivateMessagePO;
import com.smart.chat.messaging.infrastructure.persistence.PrivateMessageMapper;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import org.springframework.stereotype.Component;

/** 系统欢迎消息：落库 + 对方在线就立刻推一条，全部走 messaging 自己的门面。 */
@Component
public class WelcomeMessengerAdapter implements WelcomeMessenger {

    private final PrivateMessageMapper messageMapper;
    private final ImPushService push;

    public WelcomeMessengerAdapter(PrivateMessageMapper messageMapper, ImPushService push) {
        this.messageMapper = messageMapper;
        this.push = push;
    }

    @Override
    public void sendSystemWelcome(String reviewer, String toUser, String content) {
        PrivateMessagePO welcome = PrivateMessagePO.of(reviewer, toUser, content, PrivateMessagePO.TYPE_SYSTEM);
        messageMapper.insert(welcome);
        if (push.isOnline(toUser)) {
            push.pushDm(welcome);
        }
    }
}
