package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 喜好 TOP10 互猜（原 F282，系统裁剪后我们百科唯一保留项）。
 * 情绪价值设计：各写一份自己的十条，再各自猜对方的十条——揭榜时「居然没猜到你现在爱这个」
 * 才是这功能的本体，它逼人去重新认识一个你以为已经认识的人。
 */
@Service
public class CoupleCodexService {

    static final int TOP_SIZE = 10;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleTopListMapper topListMapper;
    private final CoupleTopGuessMapper topGuessMapper;
    private final ImPushService push;

    public CoupleCodexService(CoupleSpaceMapper spaceMapper, CoupleTopListMapper topListMapper,
                              CoupleTopGuessMapper topGuessMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.topListMapper = topListMapper;
        this.topGuessMapper = topGuessMapper;
        this.push = push;
    }

    public record TopBoardVO(String category, String label, List<String> mine, List<String> partner,
                             List<String> myGuess, boolean revealed, List<String> rematch) {
    }

    public record OverviewVO(String day, List<TopBoardVO> tops) {
    }

    /** 八个类目各一行：我的榜、TA 的榜、我猜的、以及猜漏的部分。 */
    public OverviewVO overview(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        List<CoupleTopList> lists = topListMapper.findBySpace(space.getId());
        List<CoupleTopGuess> guesses = topGuessMapper.findBySpace(space.getId());

        List<TopBoardVO> tops = new ArrayList<>();
        for (String cat : CoupleCodexBank.TOP_CATEGORIES) {
            List<String> mineList = itemsOf(lists, cat, me);
            List<String> partnerList = itemsOf(lists, cat, partner);
            List<String> myGuess = guessOf(guesses, cat, me);
            // 揭榜门槛：TA 写过、且我下过注，两边才摆在一起给人看
            boolean revealed = !partnerList.isEmpty() && !myGuess.isEmpty();
            List<String> rematch = new ArrayList<>();
            if (revealed) {
                Set<String> guessSet = new LinkedHashSet<>(myGuess);
                for (String item : partnerList) {
                    if (!guessSet.contains(item)) {
                        rematch.add(CoupleCodexBank.rematchLine(cat, item));
                    }
                }
            }
            tops.add(new TopBoardVO(cat, CoupleCodexBank.TOP_LABELS.getOrDefault(cat, cat),
                    mineList, partnerList, myGuess, revealed, rematch));
        }
        return new OverviewVO(LocalDate.now().toString(), tops);
    }

    /** 更新本人的类目榜单。 */
    public OverviewVO topList(String me, String category, String items) {
        CoupleSpace space = requireSpace(me);
        requireCategory(category);
        List<String> list = splitItems(items, TOP_SIZE, 60);
        CoupleTopList exist = topListMapper.find(space.getId(), category, me);
        if (exist != null) {
            exist.setItems(String.join(",", list));
            exist.setUpdatedAt(System.currentTimeMillis());
            topListMapper.updateById(exist);
        } else {
            topListMapper.insert(CoupleTopList.of(space.getId(), category, me, String.join(",", list)));
            push.pushCoupleEvent("codex-top-list", me, space.partnerOf(me),
                    "TA 更新了「" + CoupleCodexBank.TOP_LABELS.getOrDefault(category, category) + "」，快来猜 🎯");
        }
        return overview(me);
    }

    /** 猜对方的类目榜单（可改；对方榜单已存在即揭榜）。 */
    public OverviewVO topGuess(String me, String category, String items) {
        CoupleSpace space = requireSpace(me);
        requireCategory(category);
        String partner = space.partnerOf(me);
        List<String> list = splitItems(items, TOP_SIZE, 60);
        CoupleTopGuess exist = topGuessMapper.find(space.getId(), category, me);
        if (exist != null) {
            exist.setItems(String.join(",", list));
            exist.setUpdatedAt(System.currentTimeMillis());
            topGuessMapper.updateById(exist);
            return overview(me);
        }
        topGuessMapper.insert(CoupleTopGuess.of(space.getId(), category, partner, me, String.join(",", list)));
        push.pushCoupleEvent("codex-top-guess", me, partner,
                "TA 对你的「" + CoupleCodexBank.TOP_LABELS.getOrDefault(category, category) + "」下注了 🎯");
        return overview(me);
    }

    // ========== 小件 ==========

    private void requireCategory(String category) {
        if (category == null || !CoupleCodexBank.TOP_CATEGORIES.contains(category)) {
            throw new BusinessException(400, "类目不存在，从八个榜单类目里挑一个");
        }
    }

    private List<String> itemsOf(List<CoupleTopList> lists, String category, String user) {
        for (CoupleTopList l : lists) {
            if (l.getCategory().equals(category) && l.getOwnerUser().equals(user)) {
                return splitBlank(l.getItems());
            }
        }
        return List.of();
    }

    private List<String> guessOf(List<CoupleTopGuess> guesses, String category, String user) {
        for (CoupleTopGuess g : guesses) {
            if (g.getCategory().equals(category) && g.getGuesserUser().equals(user)) {
                return splitBlank(g.getItems());
            }
        }
        return List.of();
    }

    private List<String> splitItems(String raw, int max, int lenEach) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            throw new BusinessException(400, "内容不能为空");
        }
        for (String s : raw.split("[,，、]")) {
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > lenEach) {
                throw new BusinessException(400, "每条最多 " + lenEach + " 字");
            }
            out.add(t);
        }
        if (out.isEmpty()) {
            throw new BusinessException(400, "至少写一项");
        }
        if (out.size() > max) {
            throw new BusinessException(400, "最多 " + max + " 项");
        }
        return out;
    }

    private List<String> splitBlank(String raw) {
        List<String> out = new ArrayList<>();
        for (String s : (raw == null ? "" : raw).split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
