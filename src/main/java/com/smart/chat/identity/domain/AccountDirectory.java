package com.smart.chat.identity.domain;

/**
 * 账号目录：其他上下文只需要问「这个账号存在吗 / 规范化叫什么 / 它是不是管理员 / 昵称改掉」，
 * 不应该 import identity 的服务与实体。方向由此固定在「消费者 → identity.domain」。
 */
public interface AccountDirectory {

    /** 账号的最小可见信息（identity 的 PO 不外泄） */
    record Account(String username, String nickname, boolean admin, String maskedPhone) {
    }

    /** 用户名规范化（大小写/空白口径由 identity 独控） */
    String normalizeUsername(String raw);

    boolean exists(String username);

    /** 查不到时返回空，不抛异常 */
    java.util.Optional<Account> find(String username);

    /** 改昵称（ProfileController 的「资料卡昵称」入口，昵称合法性仍由 identity 判定） */
    void updateNickname(String username, String nickname);

    /** 通讯录搜索候选（keyword 命中用户名或昵称，排除自己） */
    java.util.List<Account> search(String keyword, String exclude, int limit);

    /** 不是管理员时抛业务异常（沿用 identity 的判定与文案口径） */
    void requireAdmin(String username);
}
