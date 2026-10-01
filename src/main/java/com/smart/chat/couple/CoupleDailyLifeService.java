package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 深度陪伴·生活分享（F140-F149，批次十）：今日主题曲、梦境手账、美食地图、
 * TA 使用手册、情绪 SOS、每日三问、夸夸生成器、接头暗号、自定义成就、恋爱仪表盘。
 * 情绪价值设计：把「日常琐碎」变成互相收藏的生活（梦/店/怪癖），把「情绪低谷」做成
 * 一键求抱抱（SOS），把「睡前几分钟」变成每日三问，把「夸 TA」做成每天自动送上门的三个句子。
 */
@Service
public class CoupleDailyLifeService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleDreamMapper dreamMapper;
    private final CoupleFoodNoteMapper foodMapper;
    private final CouplePartnerFactMapper factMapper;
    private final CoupleSosPingMapper sosMapper;
    private final CoupleDailyThreeMapper threeMapper;
    private final CoupleCustomBadgeMapper badgeMapper;
    private final ImPushService push;

    public CoupleDailyLifeService(CoupleSpaceMapper spaceMapper, CoupleDreamMapper dreamMapper,
                                  CoupleFoodNoteMapper foodMapper, CouplePartnerFactMapper factMapper,
                                  CoupleSosPingMapper sosMapper, CoupleDailyThreeMapper threeMapper,
                                  CoupleCustomBadgeMapper badgeMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.dreamMapper = dreamMapper;
        this.foodMapper = foodMapper;
        this.factMapper = factMapper;
        this.sosMapper = sosMapper;
        this.threeMapper = threeMapper;
        this.badgeMapper = badgeMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record ThemeSongVO(String title, String artist, String reason) {
    }

    public record DreamVO(String id, String fromUser, boolean mine, String content, Long created) {
    }

    public record FoodNoteVO(String id, String fromUser, String shop, String dish, String status,
                             Integer rating, String comment, Long created) {
    }

    public record FactVO(String id, String fromUser, boolean mine, String kind, String content, Long created) {
    }

    public record SosVO(String id, String fromUser, boolean mine, String message,
                        String status, Long heldAt, Long created) {
    }

    public record ThreeVO(String day,
                          CoupleDailyThree mine,
                          CoupleDailyThree partner) {
    }

    public record PraiseVO(List<String> praises, String codeword) {
    }

    public record BadgeVO(String id, String fromUser, String title, String condition,
                          String status, Long issuedAt, Long created) {
    }

    /** F149 仪表盘：今日甜蜜待办 + 近期回忆。 */
    public record DashboardTodoVO(String kind, String text) {
    }

    public record DashboardMemoryVO(String kind, String text, Long created) {
    }

    public record DashboardVO(List<DashboardTodoVO> todos, List<DashboardMemoryVO> memories) {
    }

    // ========== F140 今日主题曲（无表） ==========

    /** 今日主题曲：按 space+day 稳定。 */
    public ThemeSongVO themeSong(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleDailyLifeBank.Song song = CoupleDailyLifeBank.pickSong(space.getId(), LocalDate.now().toString());
        return new ThemeSongVO(song.title(), song.artist(), song.reason());
    }

    // ========== F141 梦境手账 ==========

    public List<DreamVO> dreams(String me) {
        CoupleSpace space = requireSpace(me);
        return dreamMapper.findBySpace(space.getId()).stream()
                .map(d -> new DreamVO(d.getId(), d.getFromUser(), d.getFromUser().equals(me), d.getContent(), d.getCreated()))
                .toList();
    }

    /** 写下一个梦（醒来第一时间记下）。 */
    public List<DreamVO> writeDream(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CoupleDream.CONTENT_MAX, "梦境写 " + CoupleDream.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "先写下来梦到了什么 🌙");
        }
        dreamMapper.insert(CoupleDream.of(space.getId(), me, text));
        push.pushCoupleEvent("dream-written", me, space.partnerOf(me),
                "🌙 TA 梦到你了：「" + abbreviate(text, 20) + "」——快去看看这场梦。");
        return dreams(me);
    }

    // ========== F142 美食地图 ==========

    public List<FoodNoteVO> foods(String me) {
        CoupleSpace space = requireSpace(me);
        return foodMapper.findBySpace(space.getId()).stream()
                .map(f -> new FoodNoteVO(f.getId(), f.getFromUser(), f.getShop(), f.getDish(),
                        f.getStatus(), f.getRating(), f.getComment(), f.getCreated()))
                .toList();
    }

    /** 添加想吃的店（WANT）。 */
    public List<FoodNoteVO> addFood(String me, String shop, String dish) {
        CoupleSpace space = requireSpace(me);
        String s = trimLimit(shop, CoupleFoodNote.SHOP_MAX, "店名写 " + CoupleFoodNote.SHOP_MAX + " 字以内哦");
        String d = trimLimit(dish, CoupleFoodNote.DISH_MAX, "菜名写 " + CoupleFoodNote.DISH_MAX + " 字以内哦");
        if (s == null || d == null) {
            throw new BusinessException(400, "店名和招牌菜都要写哦 🍜");
        }
        foodMapper.insert(CoupleFoodNote.of(space.getId(), me, s, d));
        push.pushCoupleEvent("food-added", me, space.partnerOf(me),
                "🍜 TA 想吃「" + s + "」的" + d + "，什么时候安排？");
        return foods(me);
    }

    /** 打卡：吃过啦（评分 + 吃后感）。 */
    public List<FoodNoteVO> checkinFood(String me, String id, Integer rating, String comment) {
        CoupleSpace space = requireSpace(me);
        CoupleFoodNote row = requireFood(space.getId(), id);
        if (CoupleFoodNote.STATUS_EATEN.equals(row.getStatus())) {
            throw new BusinessException(400, "这家已经打过卡啦，换一家想吃 ✨");
        }
        row.setStatus(CoupleFoodNote.STATUS_EATEN);
        row.setRating(rating == null ? 5 : Math.max(0, Math.min(5, rating)));
        row.setComment(trimLimit(comment, CoupleFoodNote.COMMENT_MAX, null));
        foodMapper.updateById(row);
        push.pushCoupleEventBoth("food-checkin", me, space.getUserA(), space.getUserB(),
                "🍽️ 美食地图 +1：「" + row.getShop() + "」打卡成功，评分 " + row.getRating() + " 星！");
        return foods(me);
    }

    // ========== F143 TA 使用手册 ==========

    public List<FactVO> facts(String me) {
        CoupleSpace space = requireSpace(me);
        return factMapper.findBySpace(space.getId()).stream()
                .map(f -> new FactVO(f.getId(), f.getFromUser(), f.getFromUser().equals(me), f.getKind(), f.getContent(), f.getCreated()))
                .toList();
    }

    /** 补一页 TA 的说明书（口味/雷区/心头好/小怪癖）。 */
    public List<FactVO> addFact(String me, String kind, String content) {
        CoupleSpace space = requireSpace(me);
        if (!CouplePartnerFact.KIND_TASTE.equals(kind) && !CouplePartnerFact.KIND_NOGO.equals(kind)
                && !CouplePartnerFact.KIND_FAV.equals(kind) && !CouplePartnerFact.KIND_QUIRK.equals(kind)) {
            throw new BusinessException(400, "条目类型要是 TASTE/NOGO/FAV/QUIRK 之一哦");
        }
        String text = trimLimit(content, CouplePartnerFact.CONTENT_MAX, "内容写 " + CouplePartnerFact.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "先写下来这一页写了什么 📖");
        }
        factMapper.insert(CouplePartnerFact.of(space.getId(), me, kind, text));
        push.pushCoupleEvent("fact-added", me, space.partnerOf(me),
                "📖 TA 的使用手册更新了一页，去看看你被写了什么。");
        return facts(me);
    }

    // ========== F144 情绪 SOS ==========

    public List<SosVO> soses(String me) {
        CoupleSpace space = requireSpace(me);
        return sosMapper.findBySpace(space.getId()).stream()
                .map(s -> new SosVO(s.getId(), s.getFromUser(), s.getFromUser().equals(me),
                        s.getMessage(), s.getStatus(), s.getHeldAt(), s.getCreated()))
                .toList();
    }

    /** 一键求抱抱。 */
    public List<SosVO> pingSos(String me, String message) {
        CoupleSpace space = requireSpace(me);
        // 上一条还没被接住时不允许重复发
        CoupleSosPing last = sosMapper.findLatestByUser(space.getId(), me);
        if (last != null && CoupleSosPing.STATUS_SENT.equals(last.getStatus())) {
            throw new BusinessException(400, "上一条抱抱还在路上，再等等 TA 🫂");
        }
        sosMapper.insert(CoupleSosPing.of(space.getId(), me,
                trimLimit(message, CoupleSosPing.MESSAGE_MAX, null)));
        push.pushCoupleEvent("sos-ping", me, space.partnerOf(me),
                "🆘 TA 现在就需要一个抱抱！点进空间接住 TA 🫂");
        return soses(me);
    }

    /** 抱住：接住对方的 SOS。 */
    public List<SosVO> holdSos(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSosPing row = sosMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这条求抱抱哦");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己的抱抱要 TA 来接哦 🫂");
        }
        if (CoupleSosPing.STATUS_HELD.equals(row.getStatus())) {
            throw new BusinessException(400, "这个抱抱已经被接住啦");
        }
        row.setStatus(CoupleSosPing.STATUS_HELD);
        row.setHeldAt(System.currentTimeMillis());
        sosMapper.updateById(row);
        push.pushCoupleEvent("sos-held", me, row.getFromUser(),
                "🫂 TA 抱住你了——现在你是全世界最安全的人。");
        return soses(me);
    }

    // ========== F145 每日三问 ==========

    public ThreeVO dailyThree(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        return new ThreeVO(day,
                threeMapper.find(space.getId(), me, day),
                threeMapper.find(space.getId(), space.partnerOf(me), day));
    }

    /** 提交/修改今日三问。 */
    public ThreeVO saveDailyThree(String me, String joy, String touched, String wantToSay) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleDailyThree row = threeMapper.find(space.getId(), me, day);
        if (row == null) {
            row = CoupleDailyThree.of(space.getId(), me, day);
        }
        row.setJoy(trimLimit(joy, CoupleDailyThree.FIELD_MAX, null));
        row.setTouched(trimLimit(touched, CoupleDailyThree.FIELD_MAX, null));
        row.setWantToSay(trimLimit(wantToSay, CoupleDailyThree.FIELD_MAX, null));
        row.setUpdatedAt(System.currentTimeMillis());
        if (threeMapper.selectById(row.getId()) == null) {
            threeMapper.insert(row);
        } else {
            threeMapper.updateById(row);
        }
        boolean partnerDone = threeMapper.find(space.getId(), space.partnerOf(me), day) != null;
        if (partnerDone) {
            push.pushCoupleEventBoth("three-both", me, space.getUserA(), space.getUserB(),
                    "🌙 今日三问都答完啦，睡前互读对方的今天吧。");
        } else {
            push.pushCoupleEvent("three-saved", me, space.partnerOf(me),
                    "🌙 TA 已答完今日三问，就等你的版本了。");
        }
        return dailyThree(me);
    }

    // ========== F146 夸夸生成器 + F147 接头暗号（无表） ==========

    /** 今日三条夸夸 + 接头暗号。 */
    public PraiseVO praise(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        return new PraiseVO(CoupleDailyLifeBank.pickPraises(space.getId(), day),
                CoupleDailyLifeBank.pickCodeword(space.getId(), day));
    }

    // ========== F148 自定义成就 ==========

    public List<BadgeVO> badges(String me) {
        CoupleSpace space = requireSpace(me);
        return badgeMapper.findBySpace(space.getId()).stream()
                .map(b -> new BadgeVO(b.getId(), b.getFromUser(), b.getTitle(), b.getCondition(),
                        b.getStatus(), b.getIssuedAt(), b.getCreated()))
                .toList();
    }

    /** 立一个成就。 */
    public List<BadgeVO> addBadge(String me, String title, String condition) {
        CoupleSpace space = requireSpace(me);
        String t = trimLimit(title, CoupleCustomBadge.TITLE_MAX, "成就名写 " + CoupleCustomBadge.TITLE_MAX + " 字以内哦");
        if (t == null) {
            throw new BusinessException(400, "先给成就起个名字吧 🏅");
        }
        badgeMapper.insert(CoupleCustomBadge.of(space.getId(), me, t,
                trimLimit(condition, CoupleCustomBadge.CONDITION_MAX, null)));
        push.pushCoupleEvent("badge-added", me, space.partnerOf(me),
                "🏅 TA 立了一个成就：「" + t + "」——一起冲！");
        return badges(me);
    }

    /** 达成颁发：任一方宣布达成，即颁发双人证书。 */
    public List<BadgeVO> issueBadge(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleCustomBadge row = badgeMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个成就哦");
        }
        if (CoupleCustomBadge.STATUS_ISSUED.equals(row.getStatus())) {
            throw new BusinessException(400, "这个成就已经颁发过证书啦 🏅");
        }
        row.setStatus(CoupleCustomBadge.STATUS_ISSUED);
        row.setIssuedAt(System.currentTimeMillis());
        badgeMapper.updateById(row);
        push.pushCoupleEventBoth("badge-issued", me, space.getUserA(), space.getUserB(),
                "🎖️ 双人证书颁发：「" + row.getTitle() + "」达成！我们的成就是自己定义的。");
        return badges(me);
    }

    // ========== F149 恋爱仪表盘（聚合，无新表） ==========

    /** 今日甜蜜待办 + 近期回忆一览。 */
    public DashboardVO dashboard(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        List<DashboardTodoVO> todos = new ArrayList<>();
        List<DashboardMemoryVO> memories = new ArrayList<>();

        // 待办 1：每日三问未答
        if (threeMapper.find(space.getId(), me, day) == null) {
            todos.add(new DashboardTodoVO("three", "🌙 今日三问还没答，睡前 3 分钟安排上。"));
        }
        // 待办 2：对方未接住的 SOS
        CoupleSosPing mySos = sosMapper.findLatestByUser(space.getId(), me);
        if (mySos != null && CoupleSosPing.STATUS_SENT.equals(mySos.getStatus())) {
            todos.add(new DashboardTodoVO("sos", "🆘 你的求抱抱还没被接住，再摇一摇 TA。"));
        }
        CoupleSosPing partnerSos = sosMapper.findLatestByUser(space.getId(), space.partnerOf(me));
        if (partnerSos != null && CoupleSosPing.STATUS_SENT.equals(partnerSos.getStatus())) {
            todos.add(new DashboardTodoVO("sos-hold", "🫂 TA 的求抱抱在等你，快去接住！"));
        }
        // 待办 3：想吃清单前三家
        for (CoupleFoodNote f : foodMapper.findBySpace(space.getId())) {
            if (CoupleFoodNote.STATUS_WANT.equals(f.getStatus())) {
                todos.add(new DashboardTodoVO("food", "🍜 还欠一顿：「" + f.getShop() + "」的" + f.getDish() + "。"));
                if (todos.size() >= 5) {
                    break;
                }
            }
        }
        // 待办 4：挑战中的成就
        for (CoupleCustomBadge b : badgeMapper.findBySpace(space.getId())) {
            if (CoupleCustomBadge.STATUS_OPEN.equals(b.getStatus())) {
                todos.add(new DashboardTodoVO("badge", "🏅 成就挑战中：「" + b.getTitle() + "」。"));
                if (todos.size() >= 6) {
                    break;
                }
            }
        }

        // 回忆：最新梦境 / 今日主题曲 / 今日暗号
        List<CoupleDream> dreams = dreamMapper.findBySpace(space.getId());
        if (!dreams.isEmpty()) {
            memories.add(new DashboardMemoryVO("dream", "🌙 最近一场梦：" + abbreviate(dreams.get(0).getContent(), 24), dreams.get(0).getCreated()));
        }
        CoupleDailyLifeBank.Song song = CoupleDailyLifeBank.pickSong(space.getId(), day);
        memories.add(new DashboardMemoryVO("song", "🎵 今日主题曲：《" + song.title() + "》", System.currentTimeMillis()));
        memories.add(new DashboardMemoryVO("codeword", "🔑 今日暗号：" + CoupleDailyLifeBank.pickCodeword(space.getId(), day), System.currentTimeMillis()));
        List<CoupleSosPing> soses = sosMapper.findBySpace(space.getId());
        if (!soses.isEmpty() && CoupleSosPing.STATUS_HELD.equals(soses.get(0).getStatus())) {
            memories.add(new DashboardMemoryVO("sos", "🫂 最近一次抱抱已被接住", soses.get(0).getHeldAt()));
        }
        return new DashboardVO(todos, memories);
    }

    // ========== 内部工具 ==========

    private String abbreviate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String trimLimit(String text, int max, String message) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        if (message != null && t.length() > max) {
            throw new BusinessException(400, message);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private CoupleFoodNote requireFood(String spaceId, String id) {
        CoupleFoodNote row = foodMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这家店哦");
        }
        return row;
    }
}
