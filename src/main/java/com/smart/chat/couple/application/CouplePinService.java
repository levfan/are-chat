package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.pin.UserPin;
import com.smart.chat.couple.domain.pin.UserPinRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.smart.chat.couple.application.DomainRules.guard;

/** F207 常用收藏：每人 pin ≤6 个功能卡键，页签顶部「我的常用」用。 */
@Service
public class CouplePinService {

    private final CoupleSpaceRepository spaceRepository;
    private final UserPinRepository pinRepository;

    public CouplePinService(CoupleSpaceRepository spaceRepository, UserPinRepository pinRepository) {
        this.spaceRepository = spaceRepository;
        this.pinRepository = pinRepository;
    }

    public record PinVO(List<String> mine, List<String> partner) {
    }

    /** 双方收藏。 */
    public PinVO pins(String me) {
        CoupleSpace space = requireSpace(me);
        return vo(space, me);
    }

    /** 保存我的收藏（全量覆盖），返回双方最新收藏。上限与键合法性都在 {@link UserPin#replaceWith}。 */
    public PinVO savePins(String me, List<String> pins) {
        CoupleSpace space = requireSpace(me);
        UserPin mine = pinRepository.findBySpaceAndUser(space.id(), me)
                .orElseGet(() -> UserPin.blank(space.id(), me));
        guard(() -> mine.replaceWith(pins));
        pinRepository.save(mine);
        return new PinVO(mine.keys(), pinned(space.id(), space.partnerOf(me)));
    }

    private PinVO vo(CoupleSpace space, String me) {
        return new PinVO(pinned(space.id(), me), pinned(space.id(), space.partnerOf(me)));
    }

    private List<String> pinned(String spaceId, String user) {
        return pinRepository.findBySpaceAndUser(spaceId, user).map(UserPin::keys).orElse(List.of());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
