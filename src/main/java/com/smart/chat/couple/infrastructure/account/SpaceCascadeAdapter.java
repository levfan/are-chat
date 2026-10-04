package com.smart.chat.couple.infrastructure.account;

import com.smart.chat.couple.infrastructure.persistence.CoupleInviteMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpace;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.identity.domain.AccountCascade;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 账号注销时的情侣空间连带清理：解散 TA 所在空间（对方收推送）并删掉相关邀请。
 * <p>
 * 这段逻辑原先长在 identity 的 AppUserService 里，让账号上下文直接摸 couple 的 mapper 与状态常量——
 * 挪到 couple 侧之后，「注销要发生什么」由拥有数据的上下文自己说，identity 只负责通知。
 */
@Component
public class SpaceCascadeAdapter implements AccountCascade {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleInviteMapper inviteMapper;
    private final CoupleEventPublisher push;

    public SpaceCascadeAdapter(CoupleSpaceMapper spaceMapper, CoupleInviteMapper inviteMapper, CoupleEventPublisher push) {
        this.spaceMapper = spaceMapper;
        this.inviteMapper = inviteMapper;
        this.push = push;
    }

    @Override
    public void onDeactivated(String username) {
        spaceMapper.findActiveByUser(username).ifPresent(space -> {
            space.setStatus(CoupleSpace.STATUS_DISSOLVED);
            space.setDissolvedAt(System.currentTimeMillis());
            spaceMapper.updateById(space);
            push.pushCoupleEvent("dissolved", username, space.partnerOf(username),
                    "对方账号已注销，情侣空间自动解除 😢");
        });
        inviteMapper.deleteAllInvolving(username);
    }
}
