package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleUserPinPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleUserPinMapper;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/** F207 常用收藏：每人 pin ≤6 个功能卡键，页签顶部「我的常用」用。 */
@Service
public class CouplePinService {

    static final int MAX_PINS = 6;
    static final int KEY_MAX = 40;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleUserPinMapper pinMapper;

    public CouplePinService(CoupleSpaceMapper spaceMapper, CoupleUserPinMapper pinMapper) {
        this.spaceMapper = spaceMapper;
        this.pinMapper = pinMapper;
    }

    public record PinVO(List<String> mine, List<String> partner) {
    }

    /** 双方收藏。 */
    public PinVO pins(String me) {
        CoupleSpacePO space = requireSpace(me);
        return vo(space, me);
    }

    /** 保存我的收藏（全量覆盖），返回双方最新收藏。 */
    public PinVO savePins(String me, List<String> pins) {
        CoupleSpacePO space = requireSpace(me);
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (pins != null) {
            for (String raw : pins) {
                if (raw == null) {
                    continue;
                }
                String key = raw.trim();
                if (key.isEmpty()) {
                    continue;
                }
                if (key.length() > KEY_MAX) {
                    throw new BusinessException(400, "收藏键太长啦");
                }
                keys.add(key);
            }
        }
        if (keys.size() > MAX_PINS) {
            throw new BusinessException(400, "最多收藏 " + MAX_PINS + " 个，先放下一个再钉新的");
        }
        String joined = String.join(",", keys);
        CoupleUserPinPO row = pinMapper.find(space.getId(), me);
        if (row == null) {
            pinMapper.insert(CoupleUserPinPO.of(space.getId(), me, joined));
        } else {
            row.setPins(joined);
            row.setUpdatedAt(System.currentTimeMillis());
            pinMapper.updateById(row);
        }
        return new PinVO(List.copyOf(keys), parse(pinMapper.find(space.getId(), space.partnerOf(me))));
    }

    private PinVO vo(CoupleSpacePO space, String me) {
        return new PinVO(parse(pinMapper.find(space.getId(), me)),
                parse(pinMapper.find(space.getId(), space.partnerOf(me))));
    }

    static List<String> parse(CoupleUserPinPO row) {
        if (row == null || row.getPins() == null || row.getPins().isBlank()) {
            return List.of();
        }
        return Arrays.stream(row.getPins().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private CoupleSpacePO requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
