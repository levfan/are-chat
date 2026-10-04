package com.smart.chat.identity.domain;

/**
 * 资料卡建档：账号建好后需要一张资料卡，但资料卡数据归 messaging。
 * 由 identity 声明「我需要什么」，messaging 提供实现——原先这段在 identity 里直接 insert UserProfile，
 * 且在 AppUserService 与 AdminBootstrapper 各抄了一份。
 */
public interface ProfileProvisioner {

    /** 幂等：已存在就不动 */
    void provision(String username, String nickname, String avatar);
}
