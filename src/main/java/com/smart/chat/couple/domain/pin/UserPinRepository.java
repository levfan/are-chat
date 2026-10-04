package com.smart.chat.couple.domain.pin;

import java.util.Optional;

/**
 * 收藏卡的仓储端口：领域只会说「某人钉了哪些卡」和「把这组卡存回去」。
 * <p>
 * 没有 delete：现役用例里「清空常用」是存一行空的卡集，不是删行，不编造行为。
 */
public interface UserPinRepository {

    /** 某人这一行的收藏卡（每人每空间一行，没钉过就是空）。 */
    Optional<UserPin> findBySpaceAndUser(String spaceId, String username);

    /** 存回聚合：新行整行插入；已存在则<b>只回写聚合纳管的列</b>（pins 与 updated_at）。 */
    void save(UserPin pin);
}
