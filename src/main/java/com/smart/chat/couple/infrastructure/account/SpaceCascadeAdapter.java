package com.smart.chat.couple.infrastructure.account;

import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.infrastructure.persistence.CoupleInviteMapper;
import com.smart.chat.identity.domain.AccountCascade;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 账号注销时的情侣空间连带清理：解散所在空间（对方收推送）并删掉相关邀请。
 * <p>
 * 原先这段长在 identity 的 AppUserService 里、直接改 PO 状态；现在归 couple，
 * 且「解散」这件事交给聚合的 {@link CoupleSpace#dissolve(long)} 守门——状态机不允许可重复解散。
 */
@Component
public class SpaceCascadeAdapter implements AccountCascade {

    private final CoupleSpaceRepository spaces;
    private final CoupleInviteMapper inviteMapper;
    private final CoupleEventPublisher publisher;

    public SpaceCascadeAdapter(CoupleSpaceRepository spaces, CoupleInviteMapper inviteMapper,
                               CoupleEventPublisher publisher) {
        this.spaces = spaces;
        this.inviteMapper = inviteMapper;
        this.publisher = publisher;
    }

    @Override
    public void onDeactivated(String username) {
        spaces.findActiveByMember(username).ifPresent(space -> {
            String partner = space.partnerOf(username);
            space.dissolve(System.currentTimeMillis());
            spaces.save(space);
            publisher.pushCoupleEvent("dissolved", username, partner, "对方账号已注销，情侣空间自动解除 😢");
        });
        inviteMapper.deleteAllInvolving(username);
    }
}
