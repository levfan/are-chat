package com.smart.chat.messaging.infrastructure.identity;

import com.smart.chat.messaging.domain.conversation.PrivateMessage;
import com.smart.chat.messaging.domain.conversation.PrivateMessageRepository;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.identity.domain.WelcomeMessenger;
import org.springframework.stereotype.Component;

/**
 * 系统欢迎消息：落库 + 对方在线就立刻推一条，全部走 messaging 自己的门面。
 * 系统消息不做文本形态校验（文案由服务端写死），与改造前 {@code PrivateMessagePO.of} 的口径一致。
 */
@Component
public class WelcomeMessengerAdapter implements WelcomeMessenger {

    private final PrivateMessageRepository messageRepository;
    private final ImPushService push;

    public WelcomeMessengerAdapter(PrivateMessageRepository messageRepository, ImPushService push) {
        this.messageRepository = messageRepository;
        this.push = push;
    }

    @Override
    public void sendSystemWelcome(String reviewer, String toUser, String content) {
        PrivateMessage welcome = PrivateMessage.offer(reviewer, toUser, content, PrivateMessage.TYPE_SYSTEM,
                System.currentTimeMillis());
        messageRepository.save(welcome);
        if (push.isOnline(toUser)) {
            push.pushDm(welcome);
        }
    }
}
