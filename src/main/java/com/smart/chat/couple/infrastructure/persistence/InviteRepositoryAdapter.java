package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.invite.Invite;
import com.smart.chat.couple.domain.invite.InviteRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** {@link InviteRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只在这里。 */
@Component
public class InviteRepositoryAdapter implements InviteRepository {

    private final CoupleInviteMapper inviteMapper;

    public InviteRepositoryAdapter(CoupleInviteMapper inviteMapper) {
        this.inviteMapper = inviteMapper;
    }

    @Override
    public Optional<Invite> findById(String id) {
        return Optional.ofNullable(inviteMapper.selectById(id)).map(InviteRepositoryAdapter::toDomain);
    }

    @Override
    public List<Invite> findPendingTo(String username) {
        return inviteMapper.findPendingTo(username).stream().map(InviteRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Invite> findPendingFrom(String username) {
        return inviteMapper.findPendingFrom(username).stream().map(InviteRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Invite> findPendingBetween(String left, String right) {
        return inviteMapper.findPendingBetween(left, right).map(InviteRepositoryAdapter::toDomain);
    }

    @Override
    public void save(Invite invite) {
        CoupleInvitePO existing = inviteMapper.selectById(invite.id());
        if (existing == null) {
            inviteMapper.insert(toPo(invite));
        } else {
            inviteMapper.updateById(toPo(invite));
        }
    }

    @Override
    public void deleteAllInvolving(String username) {
        inviteMapper.deleteAllInvolving(username);
    }

    private static Invite toDomain(CoupleInvitePO po) {
        return Invite.restore(po.getId(), po.getFromUser(), po.getToUser(), po.getMessage(), po.getStatus(),
                po.getCreated(), po.getUpdatedAt());
    }

    private static CoupleInvitePO toPo(Invite invite) {
        CoupleInvitePO po = new CoupleInvitePO();
        po.setId(invite.id());
        po.setFromUser(invite.fromUser());
        po.setToUser(invite.toUser());
        po.setMessage(invite.message());
        po.setStatus(invite.status());
        po.setCreated(invite.created());
        po.setUpdatedAt(invite.updatedAt());
        return po;
    }
}
