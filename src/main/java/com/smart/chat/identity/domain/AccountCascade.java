package com.smart.chat.identity.domain;

/**
 * 账号注销后的连带清理。
 * <p>
 * 「谁的数据谁清理」：identity 只宣告「这个账号注销了」，具体怎么散掉情侣空间、怎么摘除好友关系，
 * 由拥有那些表的上下文各自实现本接口。这样 identity 不再 import 别人的 mapper，方向固定成
 * 「实现方 → identity.domain」，原来的 identity ↔ couple / identity ↔ messaging 两个环就此断开。
 */
public interface AccountCascade {

    /** 同步执行（与注销在同一事务、同一线程内），实现方不得吞异常——失败要让注销回滚。 */
    void onDeactivated(String username);
}
