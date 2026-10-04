package com.smart.chat.messaging.domain;

import java.util.Optional;

/** 资料卡只读视图：昵称/头像/生日。别人的表别人读，取到的是发布出来的最小记录。 */
public interface PeerProfileReader {

    record PeerProfile(String username, String nickname, String avatar, String birthday) {
    }

    Optional<PeerProfile> read(String username);
}
