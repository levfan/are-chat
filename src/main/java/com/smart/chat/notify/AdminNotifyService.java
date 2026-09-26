package com.smart.chat.notify;

import com.alibaba.fastjson2.JSON;
import com.smart.chat.config.NotifyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 78 管理员通知服务：新注册申请通过免费渠道推给管理员，全部渠道失败也不影响申请落库
 * （审批待办始终在管理员控制台里可见，推送只是「更快知道」）。
 *
 * 渠道（配置见 NotifyProperties，可同时启用多个）：
 * - 企业微信群机器人 Webhook：免费、无条数限制，推荐首选
 * - WxPusher：免费微信公众号消息推送
 * - Server酱 Turbo：免费额度（每天 5 条）
 */
@Service
public class AdminNotifyService {

    private static final Logger log = LoggerFactory.getLogger(AdminNotifyService.class);

    private final NotifyProperties properties;
    private final HttpClient http;
    /** 单线程守护池：推送是尽力而为的旁路，不能拖慢注册请求 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "admin-notify");
        thread.setDaemon(true);
        return thread;
    });

    public AdminNotifyService(NotifyProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** 新注册申请：推送给所有已配置渠道（异步，失败只记日志） */
    public void notifyNewRegistration(String username, String maskedPhone) {
        pushTextAsync("are-chat 新用户注册申请",
                "**" + username + "**（手机号 " + maskedPhone + "）申请加入 are-chat，请到管理后台审批。");
    }

    /** 申请被处理后的提醒（可选渠道推送，如审批通过的欢迎通知） */
    public void notifyApplicationReviewed(String username, boolean approved, String reviewer) {
        String action = approved ? "已通过" : "已拒绝";
        pushTextAsync("are-chat 注册审批" + action,
                "申请 **" + username + "** " + action + "（审批人 " + reviewer + "）。");
    }

    public record ChannelResult(String channel, boolean ok, String detail) {
    }

    /** 全部渠道都未配置时返回 true：调用方可据此走纯站内提醒 */
    public boolean noChannelConfigured() {
        return !configuredWecom() && !configuredWxpusher() && !configuredServerchan();
    }

    public boolean configuredWecom() {
        return notBlank(properties.wecomWebhook());
    }

    public boolean configuredWxpusher() {
        return notBlank(properties.wxpusherAppToken()) && notBlank(properties.wxpusherUids());
    }

    public boolean configuredServerchan() {
        return notBlank(properties.serverchanSendkey());
    }

    /** 异步推送：注册请求路径上只入队，绝不阻塞 */
    public void pushTextAsync(String title, String content) {
        executor.submit(() -> {
            for (ChannelResult result : pushText(title, content)) {
                if (result.ok()) {
                    log.info("审批通知已推送（{}）", result.channel());
                } else {
                    log.warn("审批通知推送失败（{}）：{}", result.channel(), result.detail());
                }
            }
        });
    }

    /**
     * 同步推送一条文本到所有已配置渠道；返回每个渠道的执行结果（测试与日志用）。
     * 在后台线程调用时是异步尽力而为，不抛异常。
     */
    public List<ChannelResult> pushText(String title, String content) {
        List<ChannelResult> results = new ArrayList<>();
        if (configuredWecom()) {
            results.add(sendQuiet("wecom", () -> sendWecom(content)));
        }
        if (configuredWxpusher()) {
            results.add(sendQuiet("wxpusher", () -> sendWxpusher(title, content)));
        }
        if (configuredServerchan()) {
            results.add(sendQuiet("serverchan", () -> sendServerchan(title, content)));
        }
        return results;
    }

    /** 允许抛受检异常的发送动作（异常统一被 sendQuiet 吃掉转成结果） */
    @FunctionalInterface
    private interface ThrowingSend {
        void run() throws Exception;
    }

    private ChannelResult sendQuiet(String channel, ThrowingSend sender) {
        try {
            sender.run();
            return new ChannelResult(channel, true, "sent");
        } catch (Exception e) {
            return new ChannelResult(channel, false, e.getMessage());
        }
    }

    /** 企业微信群机器人：markdown 消息 */
    private void sendWecom(String content) throws Exception {
        String payload = JSON.toJSONString(java.util.Map.of(
                "msgtype", "markdown",
                "markdown", java.util.Map.of("content", content)));
        postJson(properties.wecomWebhook(), payload);
    }

    /** WxPusher：contentType 3 = markdown */
    private void sendWxpusher(String title, String content) throws Exception {
        List<String> uids = new ArrayList<>();
        for (String uid : properties.wxpusherUids().split("[,;，；]")) {
            if (notBlank(uid)) {
                uids.add(uid.trim());
            }
        }
        String summary = title;
        String payload = JSON.toJSONString(java.util.Map.of(
                "appToken", properties.wxpusherAppToken(),
                "content", title + "\n\n" + content,
                "summary", summary,
                "contentType", 3,
                "uids", uids));
        postJson("https://wxpusher.zjiecode.com/api/send/message", payload);
    }

    /** Server酱 Turbo：form-urlencoded，desp 支持 markdown */
    private void sendServerchan(String title, String content) throws Exception {
        String url = "https://sctapi.ftqq.com/" + properties.serverchanSendkey() + ".send";
        String form = "title=" + urlEncode(title) + "&desp=" + urlEncode(content);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " " + shorten(response.body()));
        }
    }

    private void postJson(String url, String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " " + shorten(response.body()));
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 120 ? text.substring(0, 120) : text;
    }
}
