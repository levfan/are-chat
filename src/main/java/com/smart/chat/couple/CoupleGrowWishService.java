package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 共同养成·心愿系（F73/F75/F79）：心愿互换、旅行心愿地图、下次一定清单。
 * 情绪价值设计：心愿有人接、想去的地方有人陪、随口的承诺不散场。
 */
@Service
public class CoupleGrowWishService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleWishExchangeMapper wishMapper;
    private final CoupleTravelWishMapper travelMapper;
    private final CoupleNextTimeMapper nextTimeMapper;
    private final ImPushService push;

    public CoupleGrowWishService(CoupleSpaceMapper spaceMapper, CoupleWishExchangeMapper wishMapper,
                                 CoupleTravelWishMapper travelMapper, CoupleNextTimeMapper nextTimeMapper,
                                 ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.wishMapper = wishMapper;
        this.travelMapper = travelMapper;
        this.nextTimeMapper = nextTimeMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record WishVO(String id, String fromUser, String wish, String status, String doneNote, Long created) {
    }

    public record TravelVO(String id, String fromUser, String place, String wantTodo, boolean visited,
                           String visitedNote, Long created) {
    }

    public record NextTimeVO(String id, String fromUser, String content, String status, Long doneAt, Long created) {
    }

    // ========== F73 心愿互换 ==========

    public List<WishVO> wishes(String me) {
        CoupleSpace space = requireSpace(me);
        return wishMapper.findBySpace(space.getId()).stream()
                .map(w -> toWishVO(w))
                .toList();
    }

    /** 许一个「想让 TA 帮你实现的心愿」。 */
    public List<WishVO> makeWish(String me, String wish) {
        if (wish == null || wish.isBlank() || wish.length() > CoupleWishExchange.WISH_MAX) {
            throw new BusinessException(400, "心愿要写 " + CoupleWishExchange.WISH_MAX + " 字以内哦");
        }
        CoupleSpace space = requireSpace(me);
        CoupleWishExchange row = CoupleWishExchange.of(space.getId(), me, wish.trim());
        wishMapper.insert(row);
        push.pushCoupleEvent("wish-received", me, space.partnerOf(me),
                "🌠 TA 许了个心愿等你接单：「" + wish.trim() + "」");
        return wishes(me);
    }

    /** TA 接单我的心愿；接单后许愿人会收到「心愿已被认领」。 */
    public List<WishVO> acceptWish(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleWishExchange row = requireWish(space.getId(), id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己的心愿不能自己接单哦");
        }
        if (CoupleWishExchange.STATUS_PENDING.equals(row.getStatus())) {
            row.setStatus(CoupleWishExchange.STATUS_ACCEPTED);
            row.setAcceptedAt(System.currentTimeMillis());
            wishMapper.updateById(row);
            push.pushCoupleEvent("wish-accepted", me, row.getFromUser(),
                    "🙌 TA 认领了你的心愿：「" + row.getWish() + "」，坐等实现吧");
        }
        return wishes(me);
    }

    /** 实现 TA 的心愿（可附一句实现感想）。 */
    public List<WishVO> fulfillWish(String me, String id, String doneNote) {
        CoupleSpace space = requireSpace(me);
        CoupleWishExchange row = requireWish(space.getId(), id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己的心愿等 TA 来实现哦");
        }
        if (!CoupleWishExchange.STATUS_DONE.equals(row.getStatus())) {
            row.setStatus(CoupleWishExchange.STATUS_DONE);
            row.setDoneNote(doneNote == null || doneNote.isBlank() ? null : doneNote.trim());
            row.setDoneAt(System.currentTimeMillis());
            wishMapper.updateById(row);
            String tail = row.getDoneNote() == null ? "" : " TA 说：「" + row.getDoneNote() + "」";
            push.pushCoupleEvent("wish-done", me, row.getFromUser(),
                    "✨ 你的心愿已实现：「" + row.getWish() + "」" + tail);
        }
        return wishes(me);
    }

    // ========== F75 旅行心愿地图 ==========

    public List<TravelVO> travels(String me) {
        CoupleSpace space = requireSpace(me);
        return travelMapper.findBySpace(space.getId()).stream()
                .map(t -> toTravelVO(t))
                .toList();
    }

    /** 添加一个「想一起去」的地方。 */
    public List<TravelVO> addTravel(String me, String place, String wantTodo) {
        if (place == null || place.isBlank() || place.length() > CoupleTravelWish.PLACE_MAX) {
            throw new BusinessException(400, "目的地要写 " + CoupleTravelWish.PLACE_MAX + " 字以内哦");
        }
        CoupleSpace space = requireSpace(me);
        travelMapper.insert(CoupleTravelWish.of(space.getId(), me, place.trim(),
                wantTodo == null || wantTodo.isBlank() ? null : wantTodo.trim()));
        push.pushCoupleEvent("travel-added", me, space.partnerOf(me),
                "🗺️ TA 把「" + place.trim() + "」钉上了你们的旅行心愿地图");
        return travels(me);
    }

    /** 打卡去过（双方都可打勾），可附感想。 */
    public List<TravelVO> visitTravel(String me, String id, String visitedNote) {
        CoupleSpace space = requireSpace(me);
        CoupleTravelWish row = travelMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个旅行心愿哦");
        }
        if (!row.isVisited()) {
            row.setVisited(true);
            row.setVisitedAt(System.currentTimeMillis());
            row.setVisitedNote(visitedNote == null || visitedNote.isBlank() ? null : visitedNote.trim());
            travelMapper.updateById(row);
            push.pushCoupleEventBoth("travel-visited", me, space.getUserA(), space.getUserB(),
                    "🧳 心愿地图打卡成功：「" + row.getPlace() + "」已从想去变成去过！");
        }
        return travels(me);
    }

    // ========== F79 下次一定清单 ==========

    public List<NextTimeVO> nextTimes(String me) {
        CoupleSpace space = requireSpace(me);
        return nextTimeMapper.findBySpace(space.getId()).stream()
                .map(n -> toNextTimeVO(n))
                .toList();
    }

    /** 登记一句「下次一定」（谁承诺谁登记，或对方帮你记下来）。 */
    public List<NextTimeVO> addNextTime(String me, String byUser, String content) {
        if (content == null || content.isBlank() || content.length() > CoupleNextTime.CONTENT_MAX) {
            throw new BusinessException(400, "「下次一定」的内容要写清楚（" + CoupleNextTime.CONTENT_MAX + " 字内）");
        }
        CoupleSpace space = requireSpace(me);
        String promisor = byUser == null || byUser.isBlank() ? me : byUser;
        if (!promisor.equals(me) && !promisor.equals(space.partnerOf(me))) {
            throw new BusinessException(400, "只能登记你或 TA 的承诺哦");
        }
        nextTimeMapper.insert(CoupleNextTime.of(space.getId(), promisor, content.trim()));
        if (!promisor.equals(me)) {
            push.pushCoupleEvent("nexttime-added", me, promisor,
                    "📝 你随口说的「下次一定」被记下来啦：" + content.trim());
        }
        return nextTimes(me);
    }

    /** 催 TA 兑现「下次一定」（同一条 1 小时内只催一次）。 */
    public List<NextTimeVO> nudgeNextTime(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleNextTime row = nextTimeMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这条「下次一定」哦");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己承诺的事不用自己催自己");
        }
        if (!CoupleNextTime.STATUS_PENDING.equals(row.getStatus())) {
            throw new BusinessException(400, "这条已经兑现过啦");
        }
        long now = System.currentTimeMillis();
        if (row.getNudgedAt() != null && now - row.getNudgedAt() < CoupleNextTime.NUDGE_COOLDOWN_MS) {
            throw new BusinessException(400, "刚催过啦，给 TA 一点时间（1 小时内只催一次）");
        }
        row.setNudgedAt(now);
        nextTimeMapper.updateById(row);
        push.pushCoupleEvent("nexttime-nudged", me, row.getFromUser(), CoupleGrowthBank.nudgeLine(row.getContent()));
        return nextTimes(me);
    }

    /** 兑现「下次一定」（承诺人自己兑现，兑现时双方都开心）。 */
    public List<NextTimeVO> fulfillNextTime(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleNextTime row = nextTimeMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这条「下次一定」哦");
        }
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(403, "这是 TA 的承诺，等 TA 自己兑现");
        }
        if (CoupleNextTime.STATUS_PENDING.equals(row.getStatus())) {
            row.setStatus(CoupleNextTime.STATUS_DONE);
            row.setDoneAt(System.currentTimeMillis());
            nextTimeMapper.updateById(row);
            push.pushCoupleEvent("nexttime-done", me, space.partnerOf(me),
                    "✅ TA 的「下次一定」兑现啦：「" + row.getContent() + "」说到做到！");
        }
        return nextTimes(me);
    }

    // ========== 内部工具 ==========

    private CoupleWishExchange requireWish(String spaceId, String id) {
        CoupleWishExchange row = wishMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这条心愿哦");
        }
        return row;
    }

    private WishVO toWishVO(CoupleWishExchange w) {
        return new WishVO(w.getId(), w.getFromUser(), w.getWish(), w.getStatus(), w.getDoneNote(), w.getCreated());
    }

    private TravelVO toTravelVO(CoupleTravelWish t) {
        return new TravelVO(t.getId(), t.getFromUser(), t.getPlace(), t.getWantTodo(), t.isVisited(), t.getVisitedNote(), t.getCreated());
    }

    private NextTimeVO toNextTimeVO(CoupleNextTime n) {
        return new NextTimeVO(n.getId(), n.getFromUser(), n.getContent(), n.getStatus(), n.getDoneAt(), n.getCreated());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
