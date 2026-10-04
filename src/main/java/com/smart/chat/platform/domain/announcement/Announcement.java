package com.smart.chat.platform.domain.announcement;

import com.smart.chat.platform.domain.RuleViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 全站公告聚合：一条横幅的完整生命周期——发布（生效）→ 关闭（停用），单向不可逆。
 * <p>
 * 这个聚合存在的理由是三条真实规则，改造前它们散在 Service 的 if 里：
 * <ol>
 *   <li><b>内容闸门</b>：正文去掉首尾空白后不能为空，最长 {@value #CONTENT_MAX} 个字，文案是产品口径；</li>
 *   <li><b>发布即顶掉旧公告</b>：同一时刻全站只允许最新一条生效（{@link #supersede}）；</li>
 *   <li><b>状态单向</b>：生效 → 停用；「关闭」对已停用的公告是幂等操作，不报错——
 *       管理员重复点一次就弹「已经关过了」会把一个无害操作变成失败，所以这里刻意不设闸门。
 *       也没有「重新启用」的入口（现役路由里没有），不编造行为。</li>
 * </ol>
 * 归属闸门不在这里：能不能发公告由 api 层经 {@code identity.domain.AccountDirectory.requireAdmin} 判
 * （那是账号上下文的角色规则，不是公告自身的状态规则），所以聚合里不重复持有「谁是管理员」。
 */
public final class Announcement {

    /** 公告正文长度上限 */
    public static final int CONTENT_MAX = 500;

    private final String id;
    private final String content;
    private final String createdBy;
    private final long created;
    private boolean enabled;

    private Announcement(String id, String content, String createdBy, boolean enabled, long created) {
        this.id = id;
        this.content = content;
        this.createdBy = createdBy;
        this.enabled = enabled;
        this.created = created;
    }

    /**
     * 发一条新公告：清洗正文、过内容闸门，现场发 id 与发布时间（口径与改造前一致），初始状态为生效。
     *
     * @throws RuleViolation 正文为空或超长（对外 400，文案原样）
     */
    public static Announcement post(String content, String createdBy) {
        String clean = content == null ? "" : content.trim();
        if (clean.isEmpty()) {
            throw new RuleViolation("公告内容不能为空");
        }
        if (clean.length() > CONTENT_MAX) {
            throw new RuleViolation("公告最长 " + CONTENT_MAX + " 个字");
        }
        return new Announcement(UUID.randomUUID().toString(), clean, createdBy, true, System.currentTimeMillis());
    }

    /** 从存储重建：不校验——存量行（含已停用的历史公告）必须读得出来，enabled 为空按「未生效」看待。 */
    public static Announcement restore(String id, String content, String createdBy, Boolean enabled, Long created) {
        return new Announcement(id, content, createdBy, Boolean.TRUE.equals(enabled),
                created == null ? 0L : created);
    }

    /** 关闭（撤回）这条公告：生效 → 停用；对已停用的公告重复调用是幂等的。 */
    public void close() {
        this.enabled = false;
    }

    /**
     * 发布即顶掉旧公告：把给定窗口内仍生效的旧公告逐个关闭，返回被关掉的那些（调用方逐条存回）。
     * 窗口就是现有列表读法的范围（最近 100 条倒序），不改变取数口径。
     */
    public List<Announcement> supersede(List<Announcement> window) {
        List<Announcement> retired = new ArrayList<>();
        for (Announcement earlier : window) {
            if (earlier.enabled) {
                earlier.close();
                retired.add(earlier);
            }
        }
        return retired;
    }

    public String id() {
        return id;
    }

    public String content() {
        return content;
    }

    public String createdBy() {
        return createdBy;
    }

    /** 是否生效（横幅展示中） */
    public boolean enabled() {
        return enabled;
    }

    public long created() {
        return created;
    }
}
