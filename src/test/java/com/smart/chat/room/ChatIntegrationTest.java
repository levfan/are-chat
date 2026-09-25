package com.smart.chat.room;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * IM WebSocket 真实链路集成测试：启动嵌入式 Tomcat，用 Jakarta WebSocket 客户端验证心跳与 typing 转发。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatIntegrationTest {

    @LocalServerPort
    int port;

    private WebSocketContainer container;
    private WsClient clientA;
    private WsClient clientB;

    @BeforeEach
    void setUp() throws Exception {
        container = ContainerProvider.getWebSocketContainer();
        clientA = connect("小A");
        clientB = connect("小B");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (clientA != null) {
            clientA.close();
        }
        if (clientB != null) {
            clientB.close();
        }
    }

    private WsClient connect(String name) throws Exception {
        WsClient client = new WsClient();
        URI uri = URI.create("ws://localhost:" + port + "/ws/chat/"
                + URLEncoder.encode(name, StandardCharsets.UTF_8));
        Session session = container.connectToServer(client, uri);
        // 不依赖客户端 @OnOpen 的执行时序，直接使用 connectToServer 返回的会话
        client.attach(session);
        return client;
    }

    @Test
    void heartbeatGetsAck() throws Exception {
        clientA.send("{\"type\":\"heart\"}");
        String ack = clientA.await(msg -> msg.contains("heart-ack"));
        assertThat(ack).contains("heart-ack");
    }

    @Test
    void typingIsDeliveredToTargetUser() throws Exception {
        clientA.send("{\"type\":\"typing\",\"name\":\"伪造成别人\",\"subject\":\"小B\",\"msg\":\"1\"}");
        String received = clientB.await(msg -> msg.contains("\"type\":\"typing\""));
        // 发送者以连接昵称为准，不信任客户端字段
        assertThat(received).contains("\"name\":\"小A\"");
        assertThat(received).contains("\"subject\":\"小B\"");
    }

    @Test
    void serverOnlyChatTypeIsRejected() throws Exception {
        clientA.send("{\"type\":\"chat\",\"name\":\"小A\",\"subject\":\"小B\",\"msg\":\"hi\"}");
        String reply = clientA.await(msg -> msg.contains("服务端消息不允许客户端伪造"));
        assertThat(reply).contains("chat");
    }

    // 必须是 public：Tomcat 通过反射跨包调用回调方法，package-private 类的成员不可访问
    @ClientEndpoint
    public static class WsClient {
        private volatile Session session;
        private final List<String> received = new CopyOnWriteArrayList<>();

        @OnOpen
        public void onOpen(Session session) {
            this.session = session;
        }

        void attach(Session session) {
            if (this.session == null) {
                this.session = session;
            }
        }

        @OnMessage
        public void onMessage(String message) {
            received.add(message);
        }

        void send(String text) throws IOException, InterruptedException {
            // connectToServer 返回时客户端 @OnOpen 可能尚未执行完，等 session 就绪再发
            long deadline = System.currentTimeMillis() + 5000;
            while (session == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            session.getBasicRemote().sendText(text);
        }

        void close() throws IOException {
            if (session != null && session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "bye"));
            }
        }

        String await(java.util.function.Predicate<String> predicate) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                Optional<String> hit = received.stream().filter(predicate).findFirst();
                if (hit.isPresent()) {
                    return hit.get();
                }
                Thread.sleep(100);
            }
            fail("等待消息超时，已收到：" + received);
            return null;
        }
    }
}
