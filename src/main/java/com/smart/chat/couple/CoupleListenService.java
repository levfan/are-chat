package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 误会倒带（原 F262，系统裁剪后倾听与发声唯一保留项）。
 * 情绪价值设计：吵架最怕的是各说各的——同一件争执，两人各自写「我当时以为」和「我猜你其实想」，
 * 两份齐了才并排回放。多数时候对视一眼就发现：原来你当时是这么想的。
 */
@Service
public class CoupleListenService {

    static final int MIS_MAX = 200;
    static final int MIS_TOPIC_MAX = 60;
    /** 只回看最近 30 天的倒带，太久远的争执该翻编年史而不是留在操作台上。 */
    static final int MIS_LOOKBACK_DAYS = 30;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleMisrewindMapper misMapper;
    private final ImPushService push;

    public CoupleListenService(CoupleSpaceMapper spaceMapper, CoupleMisrewindMapper misMapper,
                               ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.misMapper = misMapper;
        this.push = push;
    }

    public record MisVO(String day, String topic, String mineThought, String mineGuess,
                        String partnerThought, String partnerGuess, boolean both) {
    }

    public record TodayVO(String day, List<MisVO> misrewinds) {
    }

    /** 最近 30 天的倒带对照。 */
    public TodayVO today(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        return new TodayVO(now.toString(), misrewinds(space, me, now));
    }

    /** 就一次争执写下「我当时以为/我猜你其实想」，双份齐并排回放。 */
    public TodayVO misrewind(String me, String topic, String mine, String theirs) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        String tp = trim(topic, "争执主题要写一句");
        if (tp.length() > MIS_TOPIC_MAX) {
            throw new BusinessException(400, "主题最多 " + MIS_TOPIC_MAX + " 字");
        }
        String m = trim(mine, "");
        String g = trim(theirs, "");
        if (m.isEmpty() && g.isEmpty()) {
            throw new BusinessException(400, "两边至少写一边，倒带才有画面对");
        }
        if (m.length() > MIS_MAX || g.length() > MIS_MAX) {
            throw new BusinessException(400, "每条最多 " + MIS_MAX + " 字");
        }
        CoupleMisrewind exist = misMapper.find(space.getId(), day, tp, me);
        if (exist != null) {
            exist.setMine(m);
            exist.setTheirs(g);
            misMapper.updateById(exist);
            return today(me);
        }
        misMapper.insert(CoupleMisrewind.of(space.getId(), day, tp, me, m, g));
        String partner = space.partnerOf(me);
        if (misMapper.find(space.getId(), day, tp, partner) != null) {
            push.pushCoupleEventBoth("misrewind-done", me, space.getUserA(), space.getUserB(),
                    "「" + tp + "」的误会倒带双份齐了，并排看看 📼");
        } else {
            push.pushCoupleEvent("misrewind", me, partner, "TA 倒带了一次「" + tp + "」，等你的那份");
        }
        return today(me);
    }

    /** 按「日子+主题」把两份各归一侧；自己没写的那次只看到 TA 的，不猜自己。 */
    private List<MisVO> misrewinds(CoupleSpace space, String me, LocalDate now) {
        Map<String, CoupleMisrewind[]> byTopic = new LinkedHashMap<>();
        for (CoupleMisrewind m : misMapper.findRecent(space.getId(), now.minusDays(MIS_LOOKBACK_DAYS).toString())) {
            CoupleMisrewind[] pair = byTopic.computeIfAbsent(m.getDay() + "|" + m.getTopic(),
                    k -> new CoupleMisrewind[2]);
            if (m.getFromUser().equals(me)) {
                pair[0] = m;
            } else {
                pair[1] = m;
            }
        }
        List<MisVO> mis = new ArrayList<>();
        for (Map.Entry<String, CoupleMisrewind[]> e : byTopic.entrySet()) {
            CoupleMisrewind mineR = e.getValue()[0];
            CoupleMisrewind other = e.getValue()[1];
            if (mineR == null && other == null) {
                continue;
            }
            mis.add(new MisVO(e.getKey().split("\\|")[0], e.getKey().split("\\|", 2)[1],
                    mineR == null ? "" : mineR.getMine(), mineR == null ? "" : mineR.getTheirs(),
                    other == null ? "" : other.getMine(), other == null ? "" : other.getTheirs(),
                    mineR != null && other != null));
        }
        return mis;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty() && !failMessage.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
