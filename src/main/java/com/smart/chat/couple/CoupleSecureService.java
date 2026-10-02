package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 确定感·安全感（F120-F129，批次八）：安全感账户、恋爱体检、十年之约、愿景板、
 * 承诺博物馆、信任存折、恋爱年轮、大日子分类、双人契约、守护兽。
 * 情绪价值设计：把「安心」「信任」「承诺」变成可累积、可看见的资产，
 * 让确定感有账可查；守护兽用惰性衰减提醒两人持续照料关系。
 */
@Service
public class CoupleSecureService {

    /** 信任存折：每人每天最多给对方存的币数（防刷，留仪式感） */
    private static final int TRUST_COIN_DAILY_LIMIT = 1;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleSecurityBankMapper securityMapper;
    private final CoupleDecadePactMapper decadeMapper;
    private final CoupleVisionCardMapper visionMapper;
    private final CoupleOathMapper oathMapper;
    private final CoupleTrustCoinMapper trustMapper;
    private final CoupleSelfContractMapper contractMapper;
    private final CouplePetMapper petMapper;
    private final CoupleCheckinMapper checkinMapper;
    private final CoupleMoodMapper moodMapper;
    private final CouplePromiseMapper promiseMapper;
    private final CoupleActionMapper actionMapper;
    private final CoupleLetterMapper letterMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final ImPushService push;

    public CoupleSecureService(CoupleSpaceMapper spaceMapper, CoupleSecurityBankMapper securityMapper,
                               CoupleDecadePactMapper decadeMapper, CoupleVisionCardMapper visionMapper,
                               CoupleOathMapper oathMapper, CoupleTrustCoinMapper trustMapper,
                               CoupleSelfContractMapper contractMapper, CouplePetMapper petMapper,
                               CoupleCheckinMapper checkinMapper, CoupleMoodMapper moodMapper,
                               CouplePromiseMapper promiseMapper, CoupleActionMapper actionMapper,
                               CoupleLetterMapper letterMapper, CoupleAnniversaryMapper anniversaryMapper,
                               ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.securityMapper = securityMapper;
        this.decadeMapper = decadeMapper;
        this.visionMapper = visionMapper;
        this.oathMapper = oathMapper;
        this.trustMapper = trustMapper;
        this.contractMapper = contractMapper;
        this.petMapper = petMapper;
        this.checkinMapper = checkinMapper;
        this.moodMapper = moodMapper;
        this.promiseMapper = promiseMapper;
        this.actionMapper = actionMapper;
        this.letterMapper = letterMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record SecurityVO(String id, String fromUser, String content, String status,
                             boolean mine, Long created) {
    }

    public record SecurityBoardVO(long balance, List<SecurityVO> recent) {
    }

    public record CheckupItemVO(String name, int score, String advice) {
    }

    public record CheckupVO(int total, String level, List<CheckupItemVO> items) {
    }

    public record DecadeVO(CoupleDecadePact mine, CoupleDecadePact partner, boolean complete) {
    }

    public record VisionVO(String id, String fromUser, String word, String note,
                           boolean resonate, boolean mine, Long created) {
    }

    public record OathVO(String id, String fromUser, String content,
                         boolean stampMine, boolean stampPartner, boolean exhibited, Long created) {
    }

    public record TrustBoardVO(long mineBalance, long partnerBalance, List<CoupleTrustCoin> recent) {
    }

    public record RingVO(int year, long days, long events) {
    }

    public record RingBoardVO(int years, List<RingVO> rings) {
    }

    public record ContractVO(CoupleSelfContract row, int myCount, int partnerCount) {
    }

    public record PetVO(String id, String name, String kind, int careCount,
                        String mood, String moodLine, Long lastCareAt, Long created) {
    }

    // ========== F120 安全感账户 ==========

    public SecurityBoardVO securityBank(String me) {
        CoupleSpace space = requireSpace(me);
        return toSecurityBoard(space, me);
    }

    /** 存一句安心话（对方收下后才计余额）。 */
    public SecurityBoardVO depositSecurity(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CoupleSecurityBank.CONTENT_MAX, "安心话写 " + CoupleSecurityBank.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "写一句能让 TA 安心的话吧 🫙");
        }
        securityMapper.insert(CoupleSecurityBank.of(space.getId(), me, text));
        push.pushCoupleEvent("security-deposit", me, space.partnerOf(me),
                "🫙 TA 往安全感账户里存了一句话：「" + text + "」——收下它，就是你的底气。");
        return toSecurityBoard(space, me);
    }

    /** 收下一句安心话（只能收对方存的）。 */
    public SecurityBoardVO acceptSecurity(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSecurityBank row = securityMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这句安心话哦");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己存的话要 TA 来收哦");
        }
        if (CoupleSecurityBank.STATUS_DEPOSITED.equals(row.getStatus())) {
            row.setStatus(CoupleSecurityBank.STATUS_ACCEPTED);
            row.setAcceptedAt(System.currentTimeMillis());
            securityMapper.updateById(row);
            long balance = securityMapper.countAccepted(space.getId());
            push.pushCoupleEventBoth("security-accepted", me, space.getUserA(), space.getUserB(),
                    "🫙 安全感账户 +1！已攒下 " + balance + " 句安心话。");
        }
        return toSecurityBoard(space, me);
    }

    private SecurityBoardVO toSecurityBoard(CoupleSpace space, String me) {
        List<SecurityVO> recent = securityMapper.findBySpace(space.getId()).stream()
                .map(r -> new SecurityVO(r.getId(), r.getFromUser(), r.getContent(), r.getStatus(),
                        r.getFromUser().equals(me), r.getCreated()))
                .toList();
        return new SecurityBoardVO(securityMapper.countAccepted(space.getId()), recent);
    }

    // ========== F121 恋爱体检（聚合） ==========

    /** 五项体检：晚安打卡 / 心情分享 / 约定完成 / 贴贴互动 / 心里话。 */
    public CheckupVO checkup(String me) {
        CoupleSpace space = requireSpace(me);
        long nights = checkinMapper.selectCount(new LambdaQueryWrapper<CoupleCheckin>()
                .eq(CoupleCheckin::getSpaceId, space.getId())
                .eq(CoupleCheckin::getKind, CoupleCheckin.KIND_NIGHT));
        long moods = moodMapper.selectCount(new LambdaQueryWrapper<CoupleMood>()
                .eq(CoupleMood::getSpaceId, space.getId()));
        long promisesDone = promiseMapper.selectCount(new LambdaQueryWrapper<CouplePromise>()
                .eq(CouplePromise::getSpaceId, space.getId())
                .eq(CouplePromise::getStatus, CouplePromise.STATUS_DONE));
        long actions = actionMapper.selectCount(new LambdaQueryWrapper<CoupleAction>()
                .eq(CoupleAction::getSpaceId, space.getId()));
        long letters = letterMapper.selectCount(new LambdaQueryWrapper<CoupleLetter>()
                .eq(CoupleLetter::getSpaceId, space.getId()));

        List<CheckupItemVO> items = List.of(
                checkItem("互道晚安", nights, 30, "睡前一句晚安，是关系最稳的压舱石"),
                checkItem("心情分享", moods, 20, "把心情说出来，误会会少一半"),
                checkItem("约定完成", promisesDone, 10, "说到做到，是最踏实的浪漫"),
                checkItem("贴贴互动", actions, 30, "日常的小动作，攒着攒着就是习惯"),
                checkItem("心里话", letters, 10, "有些话写下来，比说出来更完整"));
        int total = items.stream().mapToInt(CheckupItemVO::score).sum() / items.size();
        String level = total >= 80 ? "稳固 💒" : total >= 60 ? "健康 💚" : total >= 40 ? "成长中 🌱" : "需要多浇浇水 💧";
        return new CheckupVO(total, level, items);
    }

    private CheckupItemVO checkItem(String name, long count, int target, String advice) {
        int score = (int) Math.min(100, count * 100 / target);
        return new CheckupItemVO(name, score, advice);
    }

    // ========== F122 十年之约 ==========

    public DecadeVO decadePact(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleDecadePact> rows = decadeMapper.findBySpace(space.getId());
        CoupleDecadePact mine = rows.stream().filter(r -> r.getFromUser().equals(me)).findFirst().orElse(null);
        CoupleDecadePact partner = rows.stream().filter(r -> !r.getFromUser().equals(me)).findFirst().orElse(null);
        return new DecadeVO(mine, partner, mine != null && partner != null);
    }

    /** 写/改我的十年之约（一人一条，可改）。 */
    public DecadeVO saveDecadePact(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CoupleDecadePact.CONTENT_MAX, "十年之约写 " + CoupleDecadePact.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "写下十年后的我们是什么样子吧 ⏳");
        }
        CoupleDecadePact row = decadeMapper.findByUser(space.getId(), me);
        if (row == null) {
            row = CoupleDecadePact.of(space.getId(), me, text);
            decadeMapper.insert(row);
        } else {
            row.setContent(text);
            decadeMapper.updateById(row);
        }
        if (decadeMapper.findBySpace(space.getId()).size() >= 2) {
            push.pushCoupleEventBoth("decade-complete", me, space.getUserA(), space.getUserB(),
                    "⏳ 十年之约凑齐啦！两个人都写下了十年后的我们。");
        } else {
            push.pushCoupleEvent("decade-written", me, space.partnerOf(me),
                    "⏳ TA 写下了十年之约，等你也写一句，凑成我们的十年。");
        }
        return decadePact(me);
    }

    // ========== F123 愿景板 ==========

    public List<VisionVO> visions(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleVisionCard> rows = visionMapper.findBySpace(space.getId());
        Map<String, Long> wordCount = new HashMap<>();
        rows.forEach(r -> wordCount.merge(r.getWord(), 1L, Long::sum));
        return rows.stream()
                .map(r -> new VisionVO(r.getId(), r.getFromUser(), r.getWord(), r.getNote(),
                        wordCount.getOrDefault(r.getWord(), 0L) >= 2, r.getFromUser().equals(me), r.getCreated()))
                .toList();
    }

    /** 贴一张愿景卡；对方写同一个词即共鸣。 */
    public List<VisionVO> addVision(String me, String word, String note) {
        CoupleSpace space = requireSpace(me);
        String w = trimLimit(word, CoupleVisionCard.WORD_MAX, "愿景关键词最多 " + CoupleVisionCard.WORD_MAX + " 个字哦");
        if (w == null) {
            throw new BusinessException(400, "用一个词写下你想要的未来吧 ✨");
        }
        boolean resonate = visionMapper.findBySpace(space.getId()).stream()
                .anyMatch(r -> r.getWord().equals(w) && !r.getFromUser().equals(me));
        visionMapper.insert(CoupleVisionCard.of(space.getId(), me, w,
                trimLimit(note, CoupleVisionCard.NOTE_MAX, null)));
        if (resonate) {
            push.pushCoupleEventBoth("vision-resonate", me, space.getUserA(), space.getUserB(),
                    "✨ 愿景共鸣！你们俩都写下了「" + w + "」——这就是要一起实现的事。");
        } else {
            push.pushCoupleEvent("vision-added", me, space.partnerOf(me),
                    "✨ TA 把「" + w + "」贴上了愿景板，写下同一个词就是共鸣。");
        }
        return visions(me);
    }

    // ========== F124 承诺博物馆 ==========

    public List<OathVO> oaths(String me) {
        CoupleSpace space = requireSpace(me);
        boolean meIsA = space.getUserA().equals(me);
        return oathMapper.findBySpace(space.getId()).stream()
                .map(o -> new OathVO(o.getId(), o.getFromUser(), o.getContent(),
                        meIsA ? o.getStampA() == 1 : o.getStampB() == 1,
                        meIsA ? o.getStampB() == 1 : o.getStampA() == 1,
                        o.fullyStamped(), o.getCreated()))
                .toList();
    }

    /** 立一份郑重承诺。 */
    public List<OathVO> makeOath(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CoupleOath.CONTENT_MAX, "承诺写 " + CoupleOath.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "郑重的承诺，值得认真写下来 🖋️");
        }
        oathMapper.insert(CoupleOath.of(space.getId(), me, text));
        push.pushCoupleEvent("oath-made", me, space.partnerOf(me),
                "🖋️ TA 立了一份承诺送进博物馆：「" + text + "」——等你也盖章展出。");
        return oaths(me);
    }

    /** 给承诺盖章（谁都可以盖，双方都盖即展出）。 */
    public List<OathVO> stampOath(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleOath row = oathMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这份承诺哦");
        }
        if (space.getUserA().equals(me)) {
            row.setStampA(1);
        } else {
            row.setStampB(1);
        }
        oathMapper.updateById(row);
        if (row.fullyStamped()) {
            push.pushCoupleEventBoth("oath-exhibited", me, space.getUserA(), space.getUserB(),
                    "🏛️ 承诺已双章齐备，正式进馆展出：「" + row.getContent() + "」");
        } else {
            push.pushCoupleEvent("oath-stamped", me, space.partnerOf(me),
                    "🔖 TA 给承诺盖了章，等你盖下第二个章。");
        }
        return oaths(me);
    }

    // ========== F125 信任存折 ==========

    public TrustBoardVO trustBank(String me) {
        CoupleSpace space = requireSpace(me);
        return new TrustBoardVO(
                trustMapper.countByTo(space.getId(), me),
                trustMapper.countByTo(space.getId(), space.partnerOf(me)),
                trustMapper.findBySpace(space.getId()));
    }

    /** 给对方存一枚信任币（每人每天最多一枚）。 */
    public TrustBoardVO depositTrust(String me, String reason) {
        CoupleSpace space = requireSpace(me);
        // 日界用毫秒区间而不是 DATE_FORMAT：后者是 MySQL 方言，H2(MODE=MySQL) 没有这个函数
        long dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long nextDayStart = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long todayCount = trustMapper.selectCount(new LambdaQueryWrapper<CoupleTrustCoin>()
                .eq(CoupleTrustCoin::getSpaceId, space.getId())
                .eq(CoupleTrustCoin::getFromUser, me)
                .ge(CoupleTrustCoin::getCreated, dayStart)
                .lt(CoupleTrustCoin::getCreated, nextDayStart));
        if (todayCount >= TRUST_COIN_DAILY_LIMIT) {
            throw new BusinessException(400, "今天的信任币已经送出啦，明天再来 🪙");
        }
        trustMapper.insert(CoupleTrustCoin.of(space.getId(), me, space.partnerOf(me),
                trimLimit(reason, CoupleTrustCoin.REASON_MAX, null)));
        long partnerBalance = trustMapper.countByTo(space.getId(), space.partnerOf(me));
        push.pushCoupleEvent("trust-deposit", me, space.partnerOf(me),
                "🪙 TA 给你存了一枚信任币（余额 " + partnerBalance + "）"
                        + (reason == null ? "" : "：" + reason));
        return trustBank(me);
    }

    // ========== F126 恋爱年轮（聚合） ==========

    /** 逐年年轮：每年的天数与纪念日大事数。 */
    public RingBoardVO rings(String me) {
        CoupleSpace space = requireSpace(me);
        int startYear = toLocalDate(space.getCreated() == null ? System.currentTimeMillis() : space.getCreated()).getYear();
        int thisYear = LocalDate.now().getYear();
        List<CoupleAnniversary> anniversaries = anniversaryMapper.selectList(
                new LambdaQueryWrapper<CoupleAnniversary>().eq(CoupleAnniversary::getSpaceId, space.getId()));
        List<RingVO> rings = new ArrayList<>();
        for (int year = startYear; year <= thisYear; year++) {
            final int y = year;
            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = year == thisYear ? LocalDate.now() : LocalDate.of(year, 12, 31);
            LocalDate created = toLocalDate(space.getCreated() == null ? System.currentTimeMillis() : space.getCreated());
            if (from.isBefore(created)) {
                from = created;
            }
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            long events = anniversaries.stream()
                    .filter(a -> a.getEventDate() != null && a.getEventDate().length() >= 4)
                    .filter(a -> Integer.parseInt(a.getEventDate().substring(0, 4)) == y)
                    .count();
            rings.add(new RingVO(y, days, events));
        }
        return new RingBoardVO(rings.size(), rings);
    }

    // ========== F128 双人契约 ==========

    public List<ContractVO> contracts(String me) {
        CoupleSpace space = requireSpace(me);
        boolean meIsA = space.getUserA().equals(me);
        return contractMapper.findBySpace(space.getId()).stream()
                .map(c -> new ContractVO(c,
                        meIsA ? c.getCountA() : c.getCountB(),
                        meIsA ? c.getCountB() : c.getCountA()))
                .toList();
    }

    /** 立一份双人契约。 */
    public List<ContractVO> makeContract(String me, String title, String content) {
        CoupleSpace space = requireSpace(me);
        String t = trimLimit(title, CoupleSelfContract.TITLE_MAX, "契约名最多 " + CoupleSelfContract.TITLE_MAX + " 字哦");
        if (t == null) {
            throw new BusinessException(400, "给契约起个名字吧（如：每天说晚安）📜");
        }
        contractMapper.insert(CoupleSelfContract.of(space.getId(), t,
                trimLimit(content, CoupleSelfContract.CONTENT_MAX, null)));
        push.pushCoupleEvent("contract-made", me, space.partnerOf(me),
                "📜 TA 立了一份双人契约：「" + t + "」，一起完成攒默契。");
        return contracts(me);
    }

    /** 契约打卡 +1（我的计数）。 */
    public List<ContractVO> checkContract(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSelfContract row = contractMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这份契约哦");
        }
        if (space.getUserA().equals(me)) {
            row.setCountA((row.getCountA() == null ? 0 : row.getCountA()) + 1);
        } else {
            row.setCountB((row.getCountB() == null ? 0 : row.getCountB()) + 1);
        }
        contractMapper.updateById(row);
        int my = space.getUserA().equals(me) ? row.getCountA() : row.getCountB();
        int partner = space.getUserA().equals(me) ? row.getCountB() : row.getCountA();
        push.pushCoupleEvent("contract-checkin", me, space.partnerOf(me),
                "📜 契约「" + row.getTitle() + "」打卡：你 " + my + " 次，TA " + partner + " 次。");
        return contracts(me);
    }

    // ========== F129 守护兽 ==========

    /** 领养守护兽（每空间一只）。 */
    public PetVO adoptPet(String me, String name, String kind) {
        CoupleSpace space = requireSpace(me);
        if (petMapper.findBySpace(space.getId()) != null) {
            throw new BusinessException(400, "已经有守护兽啦，它会很吃醋的 🦊");
        }
        String n = trimLimit(name, CouplePet.NAME_MAX, "名字最多 " + CouplePet.NAME_MAX + " 个字哦");
        if (n == null) {
            throw new BusinessException(400, "给守护兽起个名字吧");
        }
        String k = List.of(CouplePet.KIND_FOX, CouplePet.KIND_CAT, CouplePet.KIND_BEAR, CouplePet.KIND_BUNNY)
                .contains(kind) ? kind : CouplePet.KIND_FOX;
        CouplePet row = CouplePet.of(space.getId(), n, k);
        row.setLastCareAt(System.currentTimeMillis());
        row.setCareCount(1);
        petMapper.insert(row);
        push.pushCoupleEventBoth("pet-adopted", me, space.getUserA(), space.getUserB(),
                "🐾 你们领养了守护兽「" + n + "」，记得常来照料它。");
        return toPetVO(row);
    }

    public PetVO pet(String me) {
        CoupleSpace space = requireSpace(me);
        CouplePet row = petMapper.findBySpace(space.getId());
        return row == null ? null : toPetVO(row);
    }

    /** 照料守护兽（喂食/抚摸），心情恢复。 */
    public PetVO carePet(String me) {
        CoupleSpace space = requireSpace(me);
        CouplePet row = petMapper.findBySpace(space.getId());
        if (row == null) {
            throw new BusinessException(404, "还没有守护兽，先领养一只吧");
        }
        row.setCareCount((row.getCareCount() == null ? 0 : row.getCareCount()) + 1);
        row.setLastCareAt(System.currentTimeMillis());
        petMapper.updateById(row);
        push.pushCoupleEventBoth("pet-cared", me, space.getUserA(), space.getUserB(),
                "🐾 「" + row.getName() + "」被照料了，它蹭了蹭你们。");
        return toPetVO(row);
    }

    /** 心情按最近照料天数惰性衰减：<=1 开心 / <=3 平静 / <=5 想念 / 其余 蔫蔫。 */
    private PetVO toPetVO(CouplePet row) {
        long days = row.getLastCareAt() == null ? 99
                : ChronoUnit.DAYS.between(toLocalDate(row.getLastCareAt()), LocalDate.now());
        String mood;
        String line;
        if (days <= 1) {
            mood = "HAPPY";
            line = "开心地转圈圈，尾巴摇成风 🎠";
        } else if (days <= 3) {
            mood = "CALM";
            line = "安静地趴着，偶尔看看你们 🛋️";
        } else if (days <= 5) {
            mood = "MISS";
            line = "趴在门口，好像在等谁来摸摸头 🥺";
        } else {
            mood = "SAD";
            line = "蔫蔫的，粮碗空了……快来看看它吧 💔";
        }
        return new PetVO(row.getId(), row.getName(), row.getKind(),
                row.getCareCount() == null ? 0 : row.getCareCount(), mood, line, row.getLastCareAt(), row.getCreated());
    }

    // ========== 内部工具 ==========

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

    private LocalDate toLocalDate(long ms) {
        return LocalDate.ofInstant(java.time.Instant.ofEpochMilli(ms), ZoneId.systemDefault());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
