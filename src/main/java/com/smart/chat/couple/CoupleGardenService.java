package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 爱情花园 · 每日玫瑰 · 幸运签（F54-F56）。
 * 情绪价值设计：花园把「关系的日常维护」具象成一棵共同的小树（浇水长大、缺水会蔫），
 * 玫瑰把「我想到你」变成每天 3 朵的限量表达，幸运签把好运打包寄给对方。
 */
@Service
public class CoupleGardenService {

    private static final String[] STAGE_NAMES = {"种子", "发芽", "幼苗", "枝干", "含苞", "盛开", "繁茂"};
    private static final String[] STAGE_EMOJIS = {"🌰", "🌱", "🌿", "🪴", "🌷", "🌺", "🌳"};

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleGardenMapper gardenMapper;
    private final CoupleRoseMapper roseMapper;
    private final CoupleFortuneSlipMapper slipMapper;
    private final ImPushService push;

    public CoupleGardenService(CoupleSpaceMapper spaceMapper, CoupleGardenMapper gardenMapper,
                               CoupleRoseMapper roseMapper, CoupleFortuneSlipMapper slipMapper,
                               ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.gardenMapper = gardenMapper;
        this.roseMapper = roseMapper;
        this.slipMapper = slipMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record GardenVO(int stage, String stageName, String emoji, int totalWater,
                           boolean wateredTodayMe, boolean wateredTodayPartner, boolean withered,
                           int revivedCount, int waterToNextStage, long daysSinceWater) {
    }

    public record RoseVO(String id, String fromUser, String flowerKey, String emoji, String word, Long created) {
    }

    public record RoseBoardVO(int todayMine, int todayPartner, int remainingToday,
                              List<RoseVO> today, List<RoseVO> recent) {
    }

    public record SlipVO(String id, String fromUser, String day, String level, String content, Long created) {
    }

    public record SlipBoardVO(SlipVO mySlipToday, SlipVO receivedToday, List<SlipVO> recent) {
    }

    // ========== 爱情花园（F54） ==========

    /** 花园状态（首次访问自动开垦；顺带结算「缺水蔫掉」的状态）。 */
    public GardenVO garden(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleGarden garden = ensureGarden(space);
        refreshWither(garden, space, false);
        int nextNeed = garden.getStage() >= CoupleGarden.STAGE_MAX
                ? 0
                : (garden.getStage() + 1) * CoupleGarden.WATER_PER_STAGE - garden.getTotalWater();
        String today = LocalDate.now().toString();
        long daysSince = daysSinceWater(garden, today);
        return new GardenVO(garden.getStage(), STAGE_NAMES[garden.getStage()], STAGE_EMOJIS[garden.getStage()],
                garden.getTotalWater(),
                today.equals(isUserA(space, me) ? garden.getLastWaterDayA() : garden.getLastWaterDayB()),
                today.equals(isUserA(space, me) ? garden.getLastWaterDayB() : garden.getLastWaterDayA()),
                garden.isWithered(), garden.getRevivedCount(), nextNeed, daysSince);
    }

    /** 浇水：每人每天一次；蔫了的花园浇水即复活；浇满升阶段并庆祝。 */
    public GardenVO water(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleGarden garden = ensureGarden(space);
        String today = LocalDate.now().toString();
        boolean iAmA = isUserA(space, me);
        String myDay = iAmA ? garden.getLastWaterDayA() : garden.getLastWaterDayB();
        if (today.equals(myDay)) {
            throw new BusinessException(400, "今天已经浇过水啦，小树也要休息的 🚿");
        }
        boolean wasWithered = garden.isWithered();
        int oldStage = garden.getStage();
        if (iAmA) {
            garden.setLastWaterDayA(today);
        } else {
            garden.setLastWaterDayB(today);
        }
        garden.setTotalWater(garden.getTotalWater() + 1);
        if (wasWithered) {
            garden.setWithered(false);
            garden.setRevivedCount(garden.getRevivedCount() + 1);
        }
        garden.setStage(Math.min(CoupleGarden.STAGE_MAX, garden.getTotalWater() / CoupleGarden.WATER_PER_STAGE));
        garden.setUpdatedAt(System.currentTimeMillis());
        gardenMapper.updateById(garden);
        if (wasWithered) {
            push.pushCoupleEventBoth("garden-revived", me, space.getUserA(), space.getUserB(),
                    "🌱 蔫掉的小树被 TA 救活啦！爱意是最好的营养剂");
        } else {
            push.pushCoupleEvent("garden-watered", me, space.partnerOf(me),
                    "💧 TA 刚刚给你们的爱情花园浇了水，小树又长高了一点点");
        }
        if (garden.getStage() > oldStage) {
            push.pushCoupleEventBoth("garden-stageup", me, space.getUserA(), space.getUserB(),
                    "🎉 爱情花园升到「" + STAGE_NAMES[garden.getStage()] + "」阶段啦！"
                            + STAGE_EMOJIS[garden.getStage()] + " 每一滴水都没有白浇");
        }
        return garden(me);
    }

    /** 每日巡检：连续 3 天没人浇水 → 蔫掉并提醒双方（只提醒一次）。 */
    public void checkWither() {
        for (CoupleGarden garden : gardenMapper.findAll()) {
            CoupleSpace space = spaceMapper.selectById(garden.getSpaceId());
            if (space == null || !CoupleSpace.STATUS_ACTIVE.equals(space.getStatus())) {
                continue;
            }
            refreshWither(garden, space, true);
        }
    }

    /** 结算蔫掉状态；notify=true 时刚蔫掉就推送提醒。 */
    private void refreshWither(CoupleGarden garden, CoupleSpace space, boolean notify) {
        String today = LocalDate.now().toString();
        long daysSince = daysSinceWater(garden, today);
        boolean shouldWither = daysSince >= CoupleGarden.WITHER_AFTER_DAYS;
        if (shouldWither && !garden.isWithered()) {
            garden.setWithered(true);
            garden.setUpdatedAt(System.currentTimeMillis());
            gardenMapper.updateById(garden);
            if (notify) {
                push.pushCoupleEventBoth("garden-withered", "system", space.getUserA(), space.getUserB(),
                        "🥀 你们的爱情花园 " + daysSince + " 天没人浇水，有点蔫了…快去救救它！");
            }
        } else if (!shouldWither && garden.isWithered()) {
            // 数据修正：白天可能有人浇过水
            garden.setWithered(false);
            gardenMapper.updateById(garden);
        }
    }

    private long daysSinceWater(CoupleGarden garden, String today) {
        String last = null;
        if (garden.getLastWaterDayA() != null && (last == null || garden.getLastWaterDayA().compareTo(last) > 0)) {
            last = garden.getLastWaterDayA();
        }
        if (garden.getLastWaterDayB() != null && (last == null || garden.getLastWaterDayB().compareTo(last) > 0)) {
            last = garden.getLastWaterDayB();
        }
        if (last == null) {
            last = java.time.Instant.ofEpochMilli(garden.getCreated())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
        }
        return ChronoUnit.DAYS.between(LocalDate.parse(last), LocalDate.parse(today));
    }

    private CoupleGarden ensureGarden(CoupleSpace space) {
        CoupleGarden garden = gardenMapper.findBySpace(space.getId());
        if (garden == null) {
            garden = CoupleGarden.of(space.getId());
            gardenMapper.insert(garden);
        }
        return garden;
    }

    private boolean isUserA(CoupleSpace space, String username) {
        return space.getUserA().equals(username);
    }

    // ========== 每日玫瑰（F55） ==========

    /** 玫瑰看板：今天双方送的花 + 我还能送几朵 + 最近记录。 */
    public RoseBoardVO roseBoard(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        List<CoupleRose> todayRoses = roseMapper.findByDay(space.getId(), today);
        int todayMine = (int) todayRoses.stream().filter(r -> r.getFromUser().equals(me)).count();
        int todayPartner = todayRoses.size() - todayMine;
        List<RoseVO> recent = roseMapper.findRecent(space.getId(), 50).stream()
                .map(r -> toRoseVO(r))
                .toList();
        List<RoseVO> todayList = todayRoses.stream().map(r -> toRoseVO(r)).toList();
        return new RoseBoardVO(todayMine, todayPartner, CoupleRose.DAILY_LIMIT - todayMine, todayList, recent);
    }

    private RoseVO toRoseVO(CoupleRose r) {
        return new RoseVO(r.getId(), r.getFromUser(), r.getFlowerKey(),
                CoupleSurpriseBank.flowerEmoji(r.getFlowerKey()), r.getWord(), r.getCreated());
    }

    /** 送一朵玫瑰：每天限 3 朵，花语随机附上。 */
    public RoseBoardVO sendRose(String me, String flowerKey) {
        CoupleSpace space = requireSpace(me);
        if (!flowerExists(flowerKey)) {
            throw new BusinessException(400, "花店里没有这种花哦，换一朵吧");
        }
        String today = LocalDate.now().toString();
        long sent = roseMapper.countByUserAndDay(space.getId(), me, today);
        if (sent >= CoupleRose.DAILY_LIMIT) {
            throw new BusinessException(400, "今天 3 朵玫瑰都送完啦，明天再来吧 🌹");
        }
        String word = CoupleSurpriseBank.randomFlowerWord(flowerKey);
        roseMapper.insert(CoupleRose.of(space.getId(), me, today, flowerKey, word));
        push.pushCoupleEvent("rose-received", me, space.partnerOf(me),
                CoupleSurpriseBank.flowerEmoji(flowerKey) + " 收到一朵来自 TA 的花！花语：「" + word + "」");
        return roseBoard(me);
    }

    private boolean flowerExists(String flowerKey) {
        for (Object[] flower : CoupleSurpriseBank.flowers()) {
            if (flower[0].equals(flowerKey)) {
                return true;
            }
        }
        return false;
    }

    // ========== 幸运签（F56） ==========

    /** 签板：我今天为 TA 抽的签 + 今天 TA 为我抽的签 + 最近记录。 */
    public SlipBoardVO slipBoard(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        CoupleFortuneSlip mine = slipMapper.find(space.getId(), me, today);
        CoupleFortuneSlip received = slipMapper.find(space.getId(), partner, today);
        List<SlipVO> recent = slipMapper.findBySpace(space.getId(), 30).stream()
                .map(s -> new SlipVO(s.getId(), s.getFromUser(), s.getDay(), s.getLevel(), s.getContent(), s.getCreated()))
                .toList();
        return new SlipBoardVO(toSlipVO(mine), toSlipVO(received), recent);
    }

    /** 为 TA 抽一支今日幸运签（可重抽覆盖，签运由天定）。 */
    public SlipBoardVO drawSlip(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        String[] slip = CoupleSurpriseBank.randomFortuneSlip();
        CoupleFortuneSlip existing = slipMapper.find(space.getId(), me, today);
        if (existing != null) {
            existing.setSlipKey("slip-" + System.currentTimeMillis());
            existing.setContent(slip[1]);
            existing.setLevel(slip[0]);
            existing.setCreated(System.currentTimeMillis());
            slipMapper.updateById(existing);
        } else {
            existing = CoupleFortuneSlip.of(space.getId(), me, today, "slip-" + System.currentTimeMillis(),
                    slip[1], slip[0]);
            slipMapper.insert(existing);
        }
        push.pushCoupleEvent("slip-received", me, partner,
                "🔮 TA 为你抽了一支「" + slip[0] + "」签：" + slip[1]);
        return slipBoard(me);
    }

    private SlipVO toSlipVO(CoupleFortuneSlip slip) {
        return slip == null ? null
                : new SlipVO(slip.getId(), slip.getFromUser(), slip.getDay(), slip.getLevel(),
                        slip.getContent(), slip.getCreated());
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
