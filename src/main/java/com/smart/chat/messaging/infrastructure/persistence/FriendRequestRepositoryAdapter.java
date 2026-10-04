package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.friend.FriendRequest;
import com.smart.chat.messaging.domain.friend.FriendRequestRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link FriendRequestRepository} 的 MyBatis-Plus 适配器。
 * <p>
 * 更新只回写聚合持有的 {@code message/status/updated_at}：{@code from_user/to_user/created}
 * 是这张申请单的身份证，改一次处理就把申请人写错了。
 */
@Component
public class FriendRequestRepositoryAdapter implements FriendRequestRepository {

    private final FriendRequestMapper requestMapper;

    public FriendRequestRepositoryAdapter(FriendRequestMapper requestMapper) {
        this.requestMapper = requestMapper;
    }

    @Override
    public Optional<FriendRequest> find(String id) {
        return Optional.ofNullable(requestMapper.selectById(id)).map(FriendRequestRepositoryAdapter::toDomain);
    }

    @Override
    public List<FriendRequest> listIncoming(String me) {
        return requestMapper.findIncoming(me).stream().map(FriendRequestRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<FriendRequest> listOutgoing(String me) {
        return requestMapper.findOutgoing(me).stream().map(FriendRequestRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<FriendRequest> findPendingBetween(String from, String to) {
        return requestMapper.findPendingBetween(from, to).map(FriendRequestRepositoryAdapter::toDomain);
    }

    @Override
    public void save(FriendRequest request) {
        FriendRequestPO existing = requestMapper.selectById(request.id());
        if (existing == null) {
            FriendRequestPO po = new FriendRequestPO();
            po.setId(request.id());
            po.setFromUser(request.fromUser());
            po.setToUser(request.toUser());
            po.setMessage(request.message());
            po.setStatus(request.status());
            po.setCreated(request.created());
            po.setUpdatedAt(request.updatedAt());
            requestMapper.insert(po);
            return;
        }
        existing.setMessage(request.message());
        existing.setStatus(request.status());
        existing.setUpdatedAt(request.updatedAt());
        requestMapper.updateById(existing);
    }

    private static FriendRequest toDomain(FriendRequestPO po) {
        return FriendRequest.restore(po.getId(), po.getFromUser(), po.getToUser(), po.getMessage(), po.getStatus(),
                po.getCreated(), po.getUpdatedAt());
    }
}
