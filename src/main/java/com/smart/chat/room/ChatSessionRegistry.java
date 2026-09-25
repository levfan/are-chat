package com.smart.chat.room;

import jakarta.websocket.Session;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天室连接注册表：同一昵称允许多端同时在线（与老项目 version_03 行为一致）。
 * 纯 Java 实现，不依赖容器，方便单元测试。
 */
@Component
public class ChatSessionRegistry {

    public record Connection(String id, String username, Session session) {
        public static Connection of(String username, Session session) {
            return new Connection(UUID.randomUUID().toString(), username, session);
        }
    }

    private final Map<String, List<Connection>> clients = new ConcurrentHashMap<>();

    public boolean register(String username, Connection connection) {
        List<Connection> list = clients.computeIfAbsent(username, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            boolean exists = list.stream().anyMatch(c -> c.id().equals(connection.id()));
            if (exists) {
                return false;
            }
            list.add(connection);
            return true;
        }
    }

    public void remove(String username, String connectionId) {
        List<Connection> list = clients.get(username);
        if (list == null) {
            return;
        }
        synchronized (list) {
            list.removeIf(c -> c.id().equals(connectionId));
            if (list.isEmpty()) {
                clients.remove(username, list);
            }
        }
    }

    /** 向某个昵称的所有连接发送文本，返回成功投递的连接数。 */
    public int sendToUser(String username, String text) {
        List<Connection> list = clients.get(username);
        if (list == null) {
            return 0;
        }
        List<Connection> snapshot;
        synchronized (list) {
            snapshot = new ArrayList<>(list);
        }
        int delivered = 0;
        for (Connection connection : snapshot) {
            if (sendToSession(connection.session(), text)) {
                delivered++;
            }
        }
        return delivered;
    }

    public int connectionCount() {
        return clients.values().stream()
                .mapToInt(list -> {
                    synchronized (list) {
                        return list.size();
                    }
                })
                .sum();
    }

    public Set<String> onlineUsers() {
        return Set.copyOf(clients.keySet());
    }

    public static boolean sendToSession(Session session, String text) {
        try {
            // basic remote 非重入：并发写同一 session 会抛 IllegalStateException，必须串行化
            synchronized (session) {
                session.getBasicRemote().sendText(text);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }
}
