package com.smart.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 78 注册审批通知渠道配置：全部为免费渠道，按需配置，可同时启用多个。
 *
 * - wecom-webhook：企业微信群机器人 Webhook（免费、无条数限制）。企业微信里建群 →
 *   群设置 → 群机器人 → 添加 → 复制 Webhook 地址填到这里；手机微信关注该群即可收消息。
 * - wxpusher-app-token / wxpusher-uids：WxPusher（https://wxpusher.zjiecode.com，免费），
 *   创建应用拿 appToken，扫码关注后把 uid（多个用逗号分隔）配进来。
 * - serverchan-sendkey：Server酱（https://sct.ftqq.com，免费额度每天 5 条），
 *   微信扫码登录后复制 SendKey。
 * - xtuis-sendkey：虾推啥（https://www.xtuis.cn，免费每天 300 条 / 每分钟 30 条），
 *   微信关注「虾推啥」公众号后自动收到 token。
 *
 * 一个渠道都不配时也不影响流程：审批待办始终落在管理员的站内控制台（79）。
 */
@ConfigurationProperties(prefix = "arechat.notify")
public record NotifyProperties(String wecomWebhook, String wxpusherAppToken, String wxpusherUids,
                               String serverchanSendkey, String xtuisSendkey) {
}
