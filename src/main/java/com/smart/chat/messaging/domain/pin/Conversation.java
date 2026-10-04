package com.smart.chat.messaging.domain.pin;

/**
 * 双人会话的规范化标识（值对象）：字典序小的一方落在 A 位，所以「一个会话一条置顶」在存储里
 * 由 {@code uq_conv_pin(user_a,user_b)} 唯一索引兜住。
 * <p>
 * 排序这件事在改造前被抄了三次（置顶、取消置顶、查当前置顶各写一遍 {@code compareTo}），
 * 少写一次就会出现「换个方向看是同一条会话、库里却是两行」。比较沿用 Java 的 {@code String.compareTo}
 * ——即<b>区分大小写</b>的码元序，与改造前逐字一致。
 */
public final class Conversation {

    private final String userA;
    private final String userB;

    private Conversation(String userA, String userB) {
        this.userA = userA;
        this.userB = userB;
    }

    /** 由任意一方视角规范化出这对会话成员。 */
    public static Conversation between(String one, String other) {
        return one.compareTo(other) <= 0 ? new Conversation(one, other) : new Conversation(other, one);
    }

    /** 站在 {@code me} 视角的对方（不校验 me 是否在其中，沿用改造前口径）。 */
    public String peerOf(String me) {
        return me.equals(userA) ? userB : userA;
    }

    public String userA() {
        return userA;
    }

    public String userB() {
        return userB;
    }
}
