package com.smart.chat.identity.domain.audit;

import java.util.UUID;

/**
 * 93 管理员操作审计：<b>刻意保持薄的流水实体</b>。
 * <p>
 * {@code admin_audit} 是 append-only 台账（审批 / 禁用启用 / 重置密码留痕），没有状态迁移、没有并发裁决，
 * 所以这里只有校验过的工厂和访问器，<b>不编造行为</b>——按 {@code docs/ddd/05-tactical-playbook.md}
 * 第 2.2 条，给流水表硬造聚合比空聚合更糟。业务动作与文案由 {@code AdminService} 决定后写进来。
 */
public final class AdminAudit {

    private final String id;
    private final String actor;
    private final String action;
    private final String target;
    private final String detail;
    private final Long created;

    private AdminAudit(String id, String actor, String action, String target, String detail, Long created) {
        this.id = id;
        this.actor = actor;
        this.action = action;
        this.target = target == null ? "" : target;
        this.detail = detail == null ? "" : detail;
        this.created = created;
    }

    /** 记一笔：操作人与动作必填，对象与详情留空落成空串（与改造前的 {@code AdminAudit.of} 一致） */
    public static AdminAudit written(String actor, String action, String target, String detail) {
        return new AdminAudit(UUID.randomUUID().toString(), actor, action, target, detail, System.currentTimeMillis());
    }

    /** 从存储重建，不做校验 */
    public static AdminAudit restore(String id, String actor, String action, String target, String detail,
                                     Long created) {
        return new AdminAudit(id, actor, action, target, detail, created);
    }

    public String id() {
        return id;
    }

    public String actor() {
        return actor;
    }

    public String action() {
        return action;
    }

    public String target() {
        return target;
    }

    public String detail() {
        return detail;
    }

    public Long created() {
        return created;
    }
}
