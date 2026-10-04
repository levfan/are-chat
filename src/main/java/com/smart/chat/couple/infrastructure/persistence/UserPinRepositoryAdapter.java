package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.pin.UserPin;
import com.smart.chat.couple.domain.pin.UserPinRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link UserPinRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 取行复用 {@link CoupleUserPinMapper#find} 已有的 default 方法（Mapper 不感知领域类型），
 * 「每人每空间一行」的定位口径因此与改造前逐字相同。
 * <p>
 * 写回沿用 {@code CoupleSpaceRepositoryAdapter} 确立的纪律：<b>只回写聚合纳管的那两列</b>
 * （{@code pins} 与 {@code updated_at}）；{@code space_id}/{@code from_user}/{@code created} 发布即冻结，
 * 所以更新分支先 {@code selectById} 取回原行、改完再写回，绝不拿聚合重建整行。
 */
@Component
public class UserPinRepositoryAdapter implements UserPinRepository {

    private final CoupleUserPinMapper pinMapper;

    public UserPinRepositoryAdapter(CoupleUserPinMapper pinMapper) {
        this.pinMapper = pinMapper;
    }

    @Override
    public Optional<UserPin> findBySpaceAndUser(String spaceId, String username) {
        return Optional.ofNullable(pinMapper.find(spaceId, username)).map(UserPinRepositoryAdapter::toDomain);
    }

    @Override
    public void save(UserPin pin) {
        CoupleUserPinPO existing = pinMapper.selectById(pin.id());
        if (existing == null) {
            pinMapper.insert(toPo(pin));
            return;
        }
        applyOwnedFields(existing, pin);
        pinMapper.updateById(existing);
    }

    /** 聚合负责维护的列：只有卡集与更新时间。 */
    private static void applyOwnedFields(CoupleUserPinPO po, UserPin pin) {
        po.setPins(pin.joined());
        po.setUpdatedAt(pin.updatedAt());
    }

    private static CoupleUserPinPO toPo(UserPin pin) {
        CoupleUserPinPO po = new CoupleUserPinPO();
        po.setId(pin.id());
        po.setSpaceId(pin.spaceId());
        po.setFromUser(pin.fromUser());
        po.setPins(pin.joined());
        po.setCreated(pin.created());
        po.setUpdatedAt(pin.updatedAt() == null ? pin.created() : pin.updatedAt());
        return po;
    }

    private static UserPin toDomain(CoupleUserPinPO po) {
        return UserPin.restore(po.getId(), po.getSpaceId(), po.getFromUser(), po.getPins(), po.getCreated(),
                po.getUpdatedAt());
    }
}
