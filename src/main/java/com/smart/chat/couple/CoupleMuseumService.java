package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 时光博物馆系（F190-F199，批次十五）：纪录片分镜、博物馆展品、去年今日对比镜、银发情话机、
 * 恋爱高频词、隐藏彩蛋成就、家规宪法、免打扰时段、首页问候引擎、年度记忆书目录。
 * 情绪价值设计：回忆不是堆在角落的旧物，是被登记、被放映、被编目的馆藏；
 * 系统贴心不是打扰，是知道什么时候该安静。
 */
@Service
public class CoupleMuseumService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleDocSceneMapper sceneMapper;
    private final CoupleExhibitMapper exhibitMapper;
    private final CoupleHiddenAchievementMapper achievementMapper;
    private final CoupleHouseRuleMapper ruleMapper;
    private final CoupleDndSettingMapper dndMapper;
    private final CoupleThanksNoteMapper thanksNoteMapper;
    private final CoupleJournalMapper journalMapper;
    private final CoupleHeartFlashMapper flashMapper;
    private final CoupleWhatIfMapper whatIfMapper;
    private final CoupleSecretSignalMapper signalMapper;
    private final CoupleSyncTapMapper tapMapper;
    private final ImPushService push;

    public CoupleMuseumService(CoupleSpaceMapper spaceMapper, CoupleDocSceneMapper sceneMapper,
                               CoupleExhibitMapper exhibitMapper, CoupleHiddenAchievementMapper achievementMapper,
                               CoupleHouseRuleMapper ruleMapper, CoupleDndSettingMapper dndMapper,
                               CoupleThanksNoteMapper thanksNoteMapper, CoupleJournalMapper journalMapper,
                               CoupleHeartFlashMapper flashMapper, CoupleWhatIfMapper whatIfMapper,
                               CoupleSecretSignalMapper signalMapper, CoupleSyncTapMapper tapMapper,
                               ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.sceneMapper = sceneMapper;
        this.exhibitMapper = exhibitMapper;
        this.achievementMapper = achievementMapper;
        this.ruleMapper = ruleMapper;
        this.dndMapper = dndMapper;
        this.thanksNoteMapper = thanksNoteMapper;
        this.journalMapper = journalMapper;
        this.flashMapper = flashMapper;
        this.whatIfMapper = whatIfMapper;
        this.signalMapper = signalMapper;
        this.tapMapper = tapMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record DocSceneVO(String id, String title, String actOne, String actTwo, String actThree,
                             String fromUser, boolean mine, Long created) {
    }

    public record ExhibitVO(String id, String name, String story, String obtainedDay,
                            String fromUser, boolean mine, Long created) {
    }

    public record AchievementVO(String code, String name, String emoji, String desc,
                                boolean unlocked, String unlockedBy, Long unlockedAt) {
    }

    public record YearCounterVO(int thanks, int journal, int flash) {
    }

    public record LastYearMirrorVO(String lastYearDay, String thisYearDay,
                                   YearCounterVO lastYear, YearCounterVO thisYear, String summary) {
    }

    public record SilverLineVO(String day, String line) {
    }

    public record WordVO(String word, Integer count) {
    }

    public record RuleVO(String id, String kind, String refId, String content, String proposedBy,
                         boolean mine, boolean signed, String signedBy, Long created) {
    }

    public record DndVO(String fromUser, boolean mine, String startTime, String endTime, boolean enabled, Long updatedAt) {
    }

    public record GreetingVO(String period, String icon, String text, Long daysTogether, boolean quietNow) {
    }

    public record ChapterVO(String month, String title, String line) {
    }

    public record AnnualBookVO(String year, List<ChapterVO> chapters) {
    }

    // ========== F190 恋爱纪录片分镜 ==========

    public List<DocSceneVO> scenes(String me) {
        CoupleSpace space = requireSpace(me);
        return sceneMapper.findBySpace(space.getId()).stream()
                .map(s -> new DocSceneVO(s.getId(), s.getTitle(), s.getActOne(), s.getActTwo(), s.getActThree(),
                        s.getFromUser(), s.getFromUser().equals(me), s.getCreated()))
                .toList();
    }

    /** 把一段回忆写成三幕剧本。 */
    public List<DocSceneVO> addScene(String me, String title, String actOne, String actTwo, String actThree) {
        CoupleSpace space = requireSpace(me);
        String t = trimLimit(title, CoupleDocScene.TITLE_MAX, "片名写 " + CoupleDocScene.TITLE_MAX + " 字以内哦");
        String a1 = trimLimit(actOne, CoupleDocScene.ACT_MAX, "第一幕写 " + CoupleDocScene.ACT_MAX + " 字以内哦");
        String a2 = trimLimit(actTwo, CoupleDocScene.ACT_MAX, "第二幕写 " + CoupleDocScene.ACT_MAX + " 字以内哦");
        String a3 = trimLimit(actThree, CoupleDocScene.ACT_MAX, "第三幕写 " + CoupleDocScene.ACT_MAX + " 字以内哦");
        if (t == null || a1 == null || a2 == null || a3 == null) {
            throw new BusinessException(400, "纪录片要有片名，三幕一幕都不能少 🎬");
        }
        sceneMapper.insert(CoupleDocScene.of(space.getId(), me, t, a1, a2, a3));
        push.pushCoupleEvent("doc-scene", me, space.partnerOf(me),
                "🎬 TA 把你们的回忆写成了三幕剧本《" + t + "》，快去当第一位观众。");
        return scenes(me);
    }

    // ========== F191 恋爱博物馆展品 ==========

    public List<ExhibitVO> exhibits(String me) {
        CoupleSpace space = requireSpace(me);
        return exhibitMapper.findBySpace(space.getId()).stream()
                .map(e -> new ExhibitVO(e.getId(), e.getName(), e.getStory(), e.getObtainedDay(),
                        e.getFromUser(), e.getFromUser().equals(me), e.getCreated()))
                .toList();
    }

    /** 登记一件文字展品。 */
    public List<ExhibitVO> addExhibit(String me, String name, String story, String obtainedDay) {
        CoupleSpace space = requireSpace(me);
        String n = trimLimit(name, CoupleExhibit.NAME_MAX, "展品名写 " + CoupleExhibit.NAME_MAX + " 字以内哦");
        if (n == null) {
            throw new BusinessException(400, "先给这件宝贝起个馆内名称，比如「第一场电影票根」 🏛️");
        }
        String s = trimLimitOptional(story, CoupleExhibit.STORY_MAX, "展品故事写 " + CoupleExhibit.STORY_MAX + " 字以内哦");
        String day = normalizeDay(obtainedDay, "藏品日期要是 2024-05-20 这样的日期哦");
        exhibitMapper.insert(CoupleExhibit.of(space.getId(), me, n, s, day));
        push.pushCoupleEvent("exhibit-added", me, space.partnerOf(me),
                "🏛️ 博物馆新登记一件展品：「" + n + "」，馆长了你一个。");
        return exhibits(me);
    }

    // ========== F192 去年今日对比镜（聚合，无新表） ==========

    public LastYearMirrorVO lastYearMirror(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate today = LocalDate.now();
        String yearThis = String.valueOf(today.getYear());
        String yearLast = String.valueOf(today.getYear() - 1);

        int[] last = countsByYear(space, yearLast);
        int[] now = countsByYear(space, yearThis);
        YearCounterVO l = new YearCounterVO(last[0], last[1], last[2]);
        YearCounterVO t = new YearCounterVO(now[0], now[1], now[2]);

        String summary;
        if (l.thanks() + l.journal() + l.flash() == 0) {
            summary = "去年今日还没有存档——那正好，今年多写一点，明年就有对照了 ✨";
        } else if (t.thanks() + t.journal() + t.flash() >= l.thanks() + l.journal() + l.flash()) {
            summary = "今年到现在为止，你们记下的甜蜜已经追平或超过去年同一时段了 📈";
        } else {
            summary = "去年这时候你们记性更好，今年也别吝啬动笔呀 ✍️";
        }
        return new LastYearMirrorVO(today.minusYears(1).toString(), today.toString(), l, t, summary);
    }

    // ========== F193 银发情话机（无表） ==========

    public SilverLineVO silverLine(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        return new SilverLineVO(day, CoupleMuseumBank.pickSilverLine(space.getId(), day));
    }

    // ========== F194 恋爱高频词（聚合，无新表） ==========

    private static final Set<String> STOP_BIGRAMS = Set.of(
            "什么", "一直", "可以", "这个", "那个", "就是", "不是", "知道", "现在", "时候", "一点", "今天", "明天", "我们", "一起"
    );

    public List<WordVO> topWords(String me) {
        CoupleSpace space = requireSpace(me);
        Map<String, Integer> counts = new HashMap<>();
        for (String text : allMemoryTexts(space)) {
            for (String bg : bigrams(text)) {
                if (!STOP_BIGRAMS.contains(bg)) {
                    counts.merge(bg, 1, Integer::sum);
                }
            }
        }
        return counts.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(12)
                .map(e -> new WordVO(e.getKey(), e.getValue()))
                .toList();
    }

    /** 连续汉字串里取相邻二字组合（bigram）。 */
    static List<String> bigrams(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        StringBuilder run = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= '一' && c <= '鿿') {
                run.append(c);
            } else {
                flushRun(run, out);
            }
        }
        flushRun(run, out);
        return out;
    }

    private static void flushRun(StringBuilder run, List<String> out) {
        for (int i = 0; i + 1 < run.length(); i++) {
            out.add(run.substring(i, i + 2));
        }
        run.setLength(0);
    }

    // ========== F195 隐藏彩蛋成就（达标自动解锁） ==========

    public List<AchievementVO> achievements(String me) {
        CoupleSpace space = requireSpace(me);
        Map<String, Integer> counts = achievementCounts(space);
        List<CoupleHiddenAchievement> unlocked = achievementMapper.findBySpace(space.getId());
        List<AchievementVO> out = new ArrayList<>();
        for (CoupleMuseumBank.HiddenAchievement def : CoupleMuseumBank.ACHIEVEMENTS) {
            CoupleHiddenAchievement hit = unlocked.stream().filter(u -> u.getCode().equals(def.code())).findFirst().orElse(null);
            if (hit == null && CoupleMuseumBank.reached(def, counts.getOrDefault(def.source(), 0))) {
                hit = CoupleHiddenAchievement.of(space.getId(), def.code(), me);
                achievementMapper.insert(hit);
                unlocked = achievementMapper.findBySpace(space.getId());
                push.pushCoupleEventBoth("hidden-achievement", me, space.getUserA(), space.getUserB(),
                        "🎆 隐藏成就解锁：「" + def.name() + "」" + def.desc() + "——这是日子替你们藏的彩蛋。");
            }
            out.add(new AchievementVO(def.code(), def.name(), def.emoji(), def.desc(),
                    hit != null, hit == null ? null : hit.getUnlockedBy(), hit == null ? null : hit.getCreated()));
        }
        return out;
    }

    private Map<String, Integer> achievementCounts(CoupleSpace space) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("thanks", thanksNoteMapper.findBySpace(space.getId()).size());
        counts.put("flash", flashMapper.findBySpace(space.getId()).size());
        counts.put("journal", journalMapper.findBySpace(space.getId()).size());
        counts.put("signal", signalMapper.findBySpace(space.getId()).size());
        long whatIfBothDays = whatIfMapper.findBySpace(space.getId()).stream()
                .collect(java.util.stream.Collectors.groupingBy(CoupleWhatIf::getDay,
                        java.util.stream.Collectors.mapping(CoupleWhatIf::getFromUser, java.util.stream.Collectors.toSet())))
                .values().stream().filter(users -> users.size() >= 2).count();
        counts.put("whatif", (int) whatIfBothDays);
        counts.put("sync", tapMapper.findBySpace(space.getId()).stream()
                .mapToInt(CoupleSyncTap::getHits).sum());
        return counts;
    }

    // ========== F196 家规宪法 ==========

    public List<RuleVO> rules(String me) {
        CoupleSpace space = requireSpace(me);
        return ruleMapper.findBySpace(space.getId()).stream()
                .map(r -> new RuleVO(r.getId(), r.getKind(), r.getRefId(), r.getContent(), r.getProposedBy(),
                        r.getProposedBy().equals(me), Integer.valueOf(1).equals(r.getSigned()), r.getSignedBy(), r.getCreated()))
                .toList();
    }

    /** 提交条款/修正案。 */
    public List<RuleVO> addRule(String me, String kind, String refId, String content) {
        CoupleSpace space = requireSpace(me);
        String c = trimLimit(content, CoupleHouseRule.CONTENT_MAX, "条款写 " + CoupleHouseRule.CONTENT_MAX + " 字以内哦");
        if (c == null) {
            throw new BusinessException(400, "家规要写清楚，一句一条 ⚖️");
        }
        String k = CoupleHouseRule.KIND_AMENDMENT.equalsIgnoreCase(kind == null ? "" : kind.trim())
                ? CoupleHouseRule.KIND_AMENDMENT : CoupleHouseRule.KIND_RULE;
        String ref = null;
        if (CoupleHouseRule.KIND_AMENDMENT.equals(k) && refId != null && !refId.isBlank()) {
            CoupleHouseRule origin = ruleMapper.selectById(refId);
            if (origin == null || !origin.getSpaceId().equals(space.getId())) {
                throw new BusinessException(404, "修正案指向的条款不存在");
            }
            ref = refId;
        }
        ruleMapper.insert(CoupleHouseRule.of(space.getId(), k, ref, c, me));
        push.pushCoupleEvent("house-rule", me, space.partnerOf(me),
                "⚖️ TA 提交了一条" + (CoupleHouseRule.KIND_AMENDMENT.equals(k) ? "家规修正案" : "新家规") + "：「" + c + "」，等你来签字。");
        return rules(me);
    }

    /** 对方签字生效（提案人自己不能签）。 */
    public List<RuleVO> signRule(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleHouseRule row = ruleMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "找不到这条家规");
        }
        if (row.getProposedBy().equals(me)) {
            throw new BusinessException(400, "自己提的条款要等对方签字才算约法 🤝");
        }
        if (Integer.valueOf(1).equals(row.getSigned())) {
            return rules(me);
        }
        row.setSigned(1);
        row.setSignedBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        ruleMapper.updateById(row);
        push.pushCoupleEventBoth("house-rule-signed", me, space.getUserA(), space.getUserB(),
                "🤝 家规「" + row.getContent() + "」双方签字生效。");
        return rules(me);
    }

    // ========== F197 通知免打扰时段 ==========

    public List<DndVO> dndSettings(String me) {
        CoupleSpace space = requireSpace(me);
        return dndMapper.findBySpace(space.getId()).stream()
                .map(d -> new DndVO(d.getFromUser(), d.getFromUser().equals(me), d.getStartTime(), d.getEndTime(),
                        Integer.valueOf(1).equals(d.getEnabled()), d.getUpdatedAt()))
                .toList();
    }

    /** 保存我的免打扰时段（HH:mm，可跨零点）。 */
    public List<DndVO> saveDnd(String me, String startTime, String endTime, Boolean enabled) {
        CoupleSpace space = requireSpace(me);
        String s = normalizeHhmm(startTime, "开始时间要是 23:00 这样的时刻哦");
        String e = normalizeHhmm(endTime, "结束时间要是 07:00 这样的时刻哦");
        if (s.equals(e)) {
            throw new BusinessException(400, "开始和结束时刻相同的话，就全天静音啦，换一个吧");
        }
        int en = Boolean.FALSE.equals(enabled) ? 0 : 1;
        CoupleDndSetting row = dndMapper.find(space.getId(), me);
        if (row == null) {
            dndMapper.insert(CoupleDndSetting.of(space.getId(), me, s, e, en));
        } else {
            row.setStartTime(s);
            row.setEndTime(e);
            row.setEnabled(en);
            row.setUpdatedAt(System.currentTimeMillis());
            dndMapper.updateById(row);
        }
        return dndSettings(me);
    }

    // ========== F198 恋爱首页问候引擎（聚合，无新表） ==========

    public GreetingVO greeting(String me) {
        CoupleSpace space = requireSpace(me);
        int hour = LocalTime.now().getHour();
        String[] tpl = CoupleMuseumBank.greetingOf(hour);
        long days = space.getCreated() == null ? 0
                : Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(space.getCreated()).atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()));
        String text = tpl[2].replace("{days}", String.valueOf(days));
        CoupleDndSetting mine = dndMapper.find(space.getId(), me);
        boolean quiet = mine != null && mine.covers(String.format("%02d:%02d", hour, LocalTime.now().getMinute()));
        return new GreetingVO(tpl[0], tpl[1], text, days, quiet);
    }

    // ========== F199 年度记忆书目录（聚合，无新表） ==========

    public AnnualBookVO annualBook(String me) {
        CoupleSpace space = requireSpace(me);
        String year = String.valueOf(LocalDate.now().getYear());
        List<CoupleThanksNote> thanks = thanksNoteMapper.findBySpace(space.getId());
        List<CoupleJournal> journals = journalMapper.findBySpace(space.getId());
        List<CoupleHeartFlash> flashes = flashMapper.findBySpace(space.getId());

        List<ChapterVO> chapters = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String month = year + "-" + String.format("%02d", m);
            int t = (int) thanks.stream().filter(x -> month.equals(monthOf(x.getCreated()))).count();
            int j = (int) journals.stream().filter(x -> x.getDay() != null && month.equals(x.getDay().substring(0, 7))).count();
            int f = (int) flashes.stream().filter(x -> month.equals(monthOf(x.getCreated()))).count();
            int total = t + j + f;
            String line = total == 0
                    ? "空白页，等你们落笔"
                    : "感恩便签 " + t + " 张 · 手账 " + j + " 页 · 心动闪光 " + f + " 次";
            chapters.add(new ChapterVO(month, "第 " + m + " 章", line));
        }
        return new AnnualBookVO(year, chapters);
    }

    // ========== 内部工具 ==========

    /** 汇总空间内全部「文字回忆」文本（供高频词与对比镜使用）。 */
    private List<String> allMemoryTexts(CoupleSpace space) {
        List<String> texts = new ArrayList<>();
        thanksNoteMapper.findBySpace(space.getId()).forEach(n -> texts.add(n.getContent()));
        journalMapper.findBySpace(space.getId()).forEach(j -> texts.add(j.getText()));
        flashMapper.findBySpace(space.getId()).forEach(f -> texts.add(f.getMoment()));
        whatIfMapper.findBySpace(space.getId()).forEach(w -> texts.add(w.getAnswer()));
        return texts;
    }

    /** 按自然年统计 感恩/手账/闪光 数量。 */
    private int[] countsByYear(CoupleSpace space, String year) {
        int thanks = (int) thanksNoteMapper.findBySpace(space.getId()).stream()
                .filter(n -> year.equals(monthOf(n.getCreated()).substring(0, 4))).count();
        int journal = (int) journalMapper.findBySpace(space.getId()).stream()
                .filter(j -> year.equals(j.getDay().substring(0, 4))).count();
        int flash = (int) flashMapper.findBySpace(space.getId()).stream()
                .filter(f -> year.equals(monthOf(f.getCreated()).substring(0, 4))).count();
        return new int[]{thanks, journal, flash};
    }

    private static String monthOf(Long epochMilli) {
        return LocalDate.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault()).toString().substring(0, 7);
    }

    private String normalizeHhmm(String time, String message) {
        if (time == null || !time.trim().matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            throw new BusinessException(400, message);
        }
        return time.trim();
    }

    private LocalDate parseDay(String day, String message) {
        try {
            return LocalDate.parse(day.trim());
        } catch (Exception e) {
            throw new BusinessException(400, message);
        }
    }

    private String normalizeDay(String day, String message) {
        if (day == null || day.isBlank()) {
            return null;
        }
        return parseDay(day, message).toString();
    }

    private String trimLimit(String text, int max, String message) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        if (t.length() > max) {
            throw new BusinessException(400, message);
        }
        return t;
    }

    private String trimLimitOptional(String text, int max, String message) {
        String t = trimLimit(text, max, message);
        return t == null ? "" : t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
