package com.smart.chat.couple.domain.safeword;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.UUID;

/**
 * 一次暂停：喊停的人当天只能喊一次；事后复盘只有喊停本人能补，而且必须写点东西。
 * 「别把安全词用成口头禅」和「复盘要 TA 自己写」都是这套约定的语义，属于领域。
 */
public final class SafewordUse {

    public static final int REFLECT_MAX = 60;

    private final String id;
    private final String day;
    private final String by;
    private String reflect;

    private SafewordUse(String id, String day, String by, String reflect) {
        this.id = id;
        this.day = day;
        this.by = by;
        this.reflect = reflect;
    }

    public static SafewordUse shout(String id, String day, String by) {
        return new SafewordUse(id, day, by, null);
    }

    /** 新起一次暂停：现场发 id，复盘留空等事后补。 */
    public static SafewordUse shout(String day, String by) {
        return new SafewordUse(UUID.randomUUID().toString(), day, by, null);
    }

    public static SafewordUse restore(String id, String day, String by, String reflect) {
        return new SafewordUse(id, day, by, reflect);
    }

    /** 一天一人只记一次暂停 */
    public static void assertNotAlreadyToday(SafewordUse today) {
        if (today != null) {
            throw new RuleViolation("今天已经记过一次暂停了，别把安全词用成口头禅");
        }
    }

    /** 补复盘：只有喊停本人能写 */
    public void reflectBy(String me, String text) {
        if (!by.equals(me)) {
            throw new RuleViolation("那次是 TA 喊的停，复盘要 TA 自己写 📝");
        }
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            throw new RuleViolation("复盘写一句：当时卡在哪、后来怎么接着聊的");
        }
        if (value.length() > REFLECT_MAX) {
            throw new RuleViolation("复盘最多 " + REFLECT_MAX + " 字");
        }
        this.reflect = value;
    }

    public boolean alreadyReflected() {
        return reflect != null && !reflect.isBlank();
    }

    public String id() {
        return id;
    }

    public String day() {
        return day;
    }

    public String by() {
        return by;
    }

    public String reflect() {
        return reflect;
    }
}
