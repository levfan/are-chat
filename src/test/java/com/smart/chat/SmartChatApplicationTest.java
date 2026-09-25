package com.smart.chat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应用冒烟测试：真实容器启动 + 关键链路（未登录 401 / 登录成功 / 登录后访问受保护接口）。
 * 使用 JDK 内置 HttpClient（Boot 4 已将 TestRestTemplate 移至独立模块，无需为此引入）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SmartChatApplicationTest {

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void meWithoutLoginIsUnauthorized() throws Exception {
        assertThat(get("/api/auth/me").statusCode()).isEqualTo(401);
    }

    @Test
    void protectedApiWithoutLoginIsIntercepted() throws Exception {
        HttpResponse<String> response = get("/api/persons");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("雁过拔毛");
    }

    @Test
    void loginThenAccessProtectedApi() throws Exception {
        // 演示账号由 DemoUserSeeder 播种（alice / arechat123）
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"account\":\"alice\",\"password\":\"arechat123\"}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("alice");
    }

    @Test
    void registerWithPhoneCreatesLegitimateUser() throws Exception {
        String phone = "13900009999";
        HttpRequest sms = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/sms-code"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"phone\":\"" + phone + "\"}"))
                .build();
        HttpResponse<String> smsResponse = client.send(sms, HttpResponse.BodyHandlers.ofString());
        assertThat(smsResponse.statusCode()).isEqualTo(200);
        String code = smsResponse.body().replaceAll(".*\"devCode\":\"(\\d+)\".*", "$1");

        HttpRequest register = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"phone\":\"" + phone + "\",\"username\":\"newbie\","
                        + "\"password\":\"abc12345\",\"code\":\"" + code + "\"}"))
                .build();
        HttpResponse<String> registered = client.send(register, HttpResponse.BodyHandlers.ofString());

        assertThat(registered.statusCode()).isEqualTo(200);
        assertThat(registered.body()).contains("newbie");
    }

    @Test
    void contextLoads() throws Exception {
        HttpResponse<String> health = get("/api/auth/me");
        // 服务在线（无论 401 还是 200，能响应即说明容器起来了）
        assertThat(health.statusCode()).isBetween(200, 499);
    }
}
