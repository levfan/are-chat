package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会说情话·沟通增强（F100-F109，批次六）：恋爱翻译器、冷静角、情绪接力棒、
 * 你比划我猜、故事接龙、专属词典小考、情话合成器、道歉三部曲、情绪词汇足迹、晚安电台。
 * 情绪价值设计：让话变软（翻译/冷静角/道歉三部曲）、让情绪被接住（接力棒）、
 * 让两个人玩起来（比划猜/接龙/小考）、让每天有一句甜的（合成器/晚安电台）。
 */
@Service
public class CoupleCommService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCoolDownMapper coolMapper;
    private final CoupleMoodRelayMapper relayMapper;
    private final CoupleGuessRoundMapper guessMapper;
    private final CoupleStoryLineMapper storyMapper;
    private final CoupleApologyCardMapper apologyMapper;
    private final CoupleFeelingWordMapper feelingMapper;
    private final CoupleDictWordMapper dictMapper;
    private final CoupleSongMapper songMapper;
    private final ImPushService push;

    public CoupleCommService(CoupleSpaceMapper spaceMapper, CoupleCoolDownMapper coolMapper,
                             CoupleMoodRelayMapper relayMapper, CoupleGuessRoundMapper guessMapper,
                             CoupleStoryLineMapper storyMapper, CoupleApologyCardMapper apologyMapper,
                             CoupleFeelingWordMapper feelingMapper, CoupleDictWordMapper dictMapper,
                             CoupleSongMapper songMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.coolMapper = coolMapper;
        this.relayMapper = relayMapper;
        this.guessMapper = guessMapper;
        this.storyMapper = storyMapper;
        this.apologyMapper = apologyMapper;
        this.feelingMapper = feelingMapper;
        this.dictMapper = dictMapper;
        this.songMapper = songMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record CoolVO(String id, String fromUser, String reason, String status, Long endAt,
                         String softA, String softB, Long healedAt, Long created) {
    }

    public record RelayVO(String id, String fromUser, String moodWord, String moodEmoji, String note,
                          String status, String catchNote, Long caughtAt, Long created) {
    }

    public record GuessVO(String id, String day, String fromUser, String word, String clue, String guess,
                          int attempts, String status, Long settledAt, Long created) {
    }

    public record StoryLineVO(String id, String chainId, int seq, String byUser, String content,
                              boolean isFinal, Long created) {
    }

    public record StoryVO(String chainId, List<StoryLineVO> lines, boolean finished, Long updated) {
    }

    public record ApologyVO(String id, String fromUser, String whatWrong, String whyWrong, String willDo,
                            String status, Long acceptedAt, Long created) {
    }

    public record FeelingVO(String id, String fromUser, String day, String word, String note, Long created) {
    }

    public record DictQuizVO(String wordId, String word, List<String> options, int correctIndex) {
    }

    public record RadioVO(String title, String artist, String reason, String line, boolean hasSong) {
    }

    // ========== F100 恋爱翻译器（静态） ==========

    /** 翻译 TA 的短语：潜台词 + 建议回应。 */
    public CoupleChatBank.Translation translate(String text) {
        return CoupleChatBank.translate(text);
    }

    // ========== F101 冷静角 ==========

    public List<CoupleCoolDown> coolDowns(String me) {
        return coolMapper.findBySpace(requireSpace(me).getId());
    }

    /** 发起冷静角：同一时间只允许一个进行中。 */
    public List<CoupleCoolDown> startCoolDown(String me, String reason) {
        CoupleSpace space = requireSpace(me);
        if (reason != null && reason.length() > CoupleCoolDown.REASON_MAX) {
            throw new BusinessException(400, "原因写 " + CoupleCoolDown.REASON_MAX + " 字以内就好啦");
        }
        if (coolMapper.findActive(space.getId()) != null) {
            throw new BusinessException(400, "冷静角里还有一场冷静没结束哦");
        }
        CoupleCoolDown row = CoupleCoolDown.of(space.getId(), me,
                reason == null || reason.isBlank() ? null : reason.trim());
        coolMapper.insert(row);
        push.pushCoupleEventBoth("cool-started", me, space.getUserA(), space.getUserB(),
                "🧊 有一方进了冷静角：30 分钟后，我们各留一句软话再好好说，好吗？");
        return coolDowns(me);
    }

    /** 留一句软话；双方都留且冷静期已结束 → 自动和好。 */
    public List<CoupleCoolDown> soften(String me, String id, String content) {
        CoupleSpace space = requireSpace(me);
        CoupleCoolDown row = requireCool(space.getId(), id);
        String soft = content == null || content.isBlank() ? null : content.trim();
        if (soft == null) {
            throw new BusinessException(400, "软话不能为空哦，哪怕一句「抱抱」也行 🫂");
        }
        if (soft.length() > CoupleCoolDown.SOFT_MAX) {
            throw new BusinessException(400, "软话写 " + CoupleCoolDown.SOFT_MAX + " 字以内，留给当面说～");
        }
        if (row.getStatus().equals(CoupleCoolDown.STATUS_ACTIVE)) {
            if (System.currentTimeMillis() < row.getEndAt()) {
                throw new BusinessException(400, "冷静期还没结束，再等等，到点我们好好说 🧊");
            }
        }
        boolean meIsA = space.getUserA().equals(me);
        if (meIsA ? row.getSoftA() != null : row.getSoftB() != null) {
            return coolDowns(me);
        }
        if (meIsA) {
            row.setSoftA(soft);
            row.setSoftAtA(System.currentTimeMillis());
        } else {
            row.setSoftB(soft);
            row.setSoftAtB(System.currentTimeMillis());
        }
        if (row.getSoftA() != null && row.getSoftB() != null) {
            row.setStatus(CoupleCoolDown.STATUS_HEALED);
            row.setHealedAt(System.currentTimeMillis());
        }
        coolMapper.updateById(row);
        if (CoupleCoolDown.STATUS_HEALED.equals(row.getStatus())) {
            push.pushCoupleEventBoth("cool-healed", me, space.getUserA(), space.getUserB(),
                    "🕊️ 冷静角已和好：软话都收到了，抱抱，我们继续好好爱。");
        } else {
            push.pushCoupleEvent("cool-soften", me, space.partnerOf(me),
                    "🕊️ TA 在冷静角留了一句软话：「" + soft + "」");
        }
        return coolDowns(me);
    }

    // ========== F102 情绪接力棒 ==========

    public List<CoupleMoodRelay> relays(String me) {
        return relayMapper.findBySpace(requireSpace(me).getId());
    }

    /** 把心情抛给 TA。 */
    public List<CoupleMoodRelay> tossRelay(String me, String moodWord, String moodEmoji, String note) {
        CoupleSpace space = requireSpace(me);
        String word = trimLimit(moodWord, CoupleMoodRelay.MOOD_MAX, "心情词要写 " + CoupleMoodRelay.MOOD_MAX + " 字以内哦");
        if (relayMapper.findPending(space.getId()) != null) {
            throw new BusinessException(400, "接力棒还在路上，等 TA 接住再抛下一棒呀");
        }
        CoupleMoodRelay row = CoupleMoodRelay.of(space.getId(), me, word,
                trimEmoji(moodEmoji), trimLimit(note, CoupleMoodRelay.NOTE_MAX, null));
        relayMapper.insert(row);
        push.pushCoupleEvent("relay-tossed", me, space.partnerOf(me),
                "🥎 TA 把心情抛给你了：「" + word + "」，快去接住 TA！");
        return relays(me);
    }

    /** 接住 TA 的接力棒，回应一句，再抛回自己的心情。 */
    public List<CoupleMoodRelay> catchRelay(String me, String id, String catchNote,
                                            String myMood, String myEmoji, String myNote) {
        CoupleSpace space = requireSpace(me);
        CoupleMoodRelay row = requireRelay(space.getId(), id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己的接力棒不能自己接哦");
        }
        if (CoupleMoodRelay.STATUS_PENDING.equals(row.getStatus())) {
            row.setStatus(CoupleMoodRelay.STATUS_CAUGHT);
            row.setCatchNote(trimLimit(catchNote, CoupleMoodRelay.NOTE_MAX, null));
            row.setCaughtAt(System.currentTimeMillis());
            relayMapper.updateById(row);
            String tail = row.getCatchNote() == null ? "" : " TA 说：「" + row.getCatchNote() + "」";
            push.pushCoupleEvent("relay-caught", me, row.getFromUser(),
                    "🫂 你的心情被接住啦：「" + row.getMoodWord() + "」" + tail);
            // 接棒即回抛：给 TA 一个新的接力棒
            if (myMood != null && !myMood.isBlank()) {
                CoupleMoodRelay back = CoupleMoodRelay.of(space.getId(), me,
                        trimLimit(myMood, CoupleMoodRelay.MOOD_MAX, null), trimEmoji(myEmoji),
                        trimLimit(myNote, CoupleMoodRelay.NOTE_MAX, null));
                relayMapper.insert(back);
                push.pushCoupleEvent("relay-tossed", me, space.partnerOf(me),
                        "🥎 TA 也把心情抛给你了：「" + back.getMoodWord() + "」");
            }
        }
        return relays(me);
    }

    // ========== F103 你比划我猜 ==========

    public List<CoupleGuessRound> guessRounds(String me) {
        return guessMapper.findBySpace(requireSpace(me).getId());
    }

    /** 开一轮：系统抽词，只有比划人能看到词。 */
    public List<CoupleGuessRound> startGuess(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        if (guessMapper.countByDay(space.getId(), day) >= CoupleGuessRound.DAILY_ROUNDS) {
            throw new BusinessException(400, "今天 " + CoupleGuessRound.DAILY_ROUNDS + " 轮额度用完啦，明天再来玩～");
        }
        CoupleGuessRound row = CoupleGuessRound.of(space.getId(), day, me,
                CoupleChatBank.pickGuessWord(space.getId(), day,
                        (int) (guessMapper.countByDay(space.getId(), day) + 1)));
        guessMapper.insert(row);
        push.pushCoupleEvent("guess-started", me, space.partnerOf(me),
                "🙈 TA 开了一轮「你比划我猜」，等你来猜！");
        return guessRounds(me);
    }

    /** 比划人出提示（不能包含原词）。 */
    public List<CoupleGuessRound> clueGuess(String me, String id, String clue) {
        CoupleSpace space = requireSpace(me);
        CoupleGuessRound row = requireGuess(space.getId(), id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(403, "只有比划的人才能出提示哦");
        }
        if (!CoupleGuessRound.STATUS_DRAWN.equals(row.getStatus())) {
            return guessRounds(me);
        }
        String c = clue == null || clue.isBlank() ? null : clue.trim();
        if (c == null) {
            throw new BusinessException(400, "提示不能为空，也不能直接说答案哦 🙊");
        }
        if (c.contains(row.getWord())) {
            throw new BusinessException(400, "提示里不能包含「" + row.getWord() + "」本身哦 🙈");
        }
        if (c.length() > CoupleGuessRound.CLUE_MAX) {
            throw new BusinessException(400, "提示写 " + CoupleGuessRound.CLUE_MAX + " 字以内啦");
        }
        row.setClue(c);
        row.setStatus(CoupleGuessRound.STATUS_CLUED);
        guessMapper.updateById(row);
        push.pushCoupleEvent("guess-clued", me, space.partnerOf(me),
                "🙉 提示来啦：「" + c + "」，猜猜是什么词？");
        return guessRounds(me);
    }

    /** 猜词人猜词：中则 HIT，错满 3 次则 MISSED。 */
    public List<CoupleGuessRound> doGuess(String me, String id, String word) {
        CoupleSpace space = requireSpace(me);
        CoupleGuessRound row = requireGuess(space.getId(), id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(403, "比划的人不能自己猜哦");
        }
        if (!CoupleGuessRound.STATUS_CLUED.equals(row.getStatus())) {
            return guessRounds(me);
        }
        String w = word == null ? "" : word.trim();
        row.setGuess(w);
        row.setAttempts(row.getAttempts() + 1);
        boolean hit = !w.isBlank() && w.equals(row.getWord());
        if (hit) {
            row.setStatus(CoupleGuessRound.STATUS_HIT);
            row.setSettledAt(System.currentTimeMillis());
        } else if (row.getAttempts() >= CoupleGuessRound.MAX_ATTEMPTS) {
            row.setStatus(CoupleGuessRound.STATUS_MISSED);
            row.setSettledAt(System.currentTimeMillis());
        }
        guessMapper.updateById(row);
        if (hit) {
            push.pushCoupleEventBoth("guess-hit", me, space.getUserA(), space.getUserB(),
                    "🎉 猜中啦！答案是「" + row.getWord() + "」，默契值 +1");
        } else if (CoupleGuessRound.STATUS_MISSED.equals(row.getStatus())) {
            push.pushCoupleEventBoth("guess-missed", me, space.getUserA(), space.getUserB(),
                    "🙃 3 次没猜中，答案是「" + row.getWord() + "」。比划的人检讨，猜的人重来！");

        } else {
            push.pushCoupleEvent("guess-wrong", me, row.getFromUser(),
                    "❌ 猜了「" + w + "」不对哦，还剩 " + (CoupleGuessRound.MAX_ATTEMPTS - row.getAttempts()) + " 次机会");
        }
        return guessRounds(me);
    }

    // ========== F104 故事接龙 ==========

    public List<StoryVO> stories(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleStoryLine> lines = storyMapper.findRecent(space.getId(), 300);
        Map<String, List<CoupleStoryLine>> grouped = new LinkedHashMap<>();
        for (CoupleStoryLine line : lines) {
            grouped.computeIfAbsent(line.getChainId(), k -> new ArrayList<>()).add(line);
        }
        List<StoryVO> result = new ArrayList<>();
        for (List<CoupleStoryLine> chain : grouped.values()) {
            chain.sort(Comparator.comparingInt(CoupleStoryLine::getSeq));
            boolean finished = chain.get(chain.size() - 1).isFinalLine();
            result.add(new StoryVO(chain.get(0).getChainId(), chain.stream().map(this::toStoryLineVO).toList(),
                    finished, chain.get(chain.size() - 1).getCreated()));
        }
        result.sort(Comparator.comparingLong(StoryVO::updated).reversed());
        return result;
    }

    /** 开新篇（有连载中的故事时不能开新的）。 */
    public List<StoryVO> startStory(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CoupleStoryLine.CONTENT_MAX, "一句话写 " + CoupleStoryLine.CONTENT_MAX + " 字以内哦");
        List<StoryVO> current = stories(me);
        if (!current.isEmpty() && !current.get(0).finished()) {
            throw new BusinessException(400, "上一篇还没写完呢，先完结再开新篇吧 📖");
        }
        // chainId = 首句 id：先生成 id 再落库
        CoupleStoryLine first = CoupleStoryLine.of(space.getId(), "pending", 1, me, text, false);
        first.setId(java.util.UUID.randomUUID().toString());
        first.setChainId(first.getId());
        storyMapper.insert(first);
        push.pushCoupleEvent("story-line", me, space.partnerOf(me),
                "📖 TA 开了新故事：「" + text + "」，该你接啦！");
        return stories(me);
    }

    /** 接一句（必须轮到对方）。 */
    public List<StoryVO> addStoryLine(String me, String chainId, String content) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CoupleStoryLine.CONTENT_MAX, "一句话写 " + CoupleStoryLine.CONTENT_MAX + " 字以内哦");
        CoupleStoryLine last = storyMapper.findLastOfChain(space.getId(), chainId);
        if (last == null) {
            throw new BusinessException(404, "没有找到这个故事哦");
        }
        if (last.isFinalLine()) {
            throw new BusinessException(400, "这个故事已经完结啦，开个新篇继续 📖");
        }
        if (last.getByUser().equals(me)) {
            throw new BusinessException(403, "接龙要轮着来，这句该 TA 写哦");
        }
        CoupleStoryLine line = CoupleStoryLine.of(space.getId(), chainId, last.getSeq() + 1, me, text, false);
        storyMapper.insert(line);
        push.pushCoupleEvent("story-line", me, space.partnerOf(me),
                "📖 故事更新：" + text + " 该你接啦！");
        return stories(me);
    }

    /** 完结本篇。 */
    public List<StoryVO> finishStory(String me, String chainId) {
        CoupleSpace space = requireSpace(me);
        CoupleStoryLine last = storyMapper.findLastOfChain(space.getId(), chainId);
        if (last == null || last.isFinalLine()) {
            return stories(me);
        }
        last.setIsFinal(1);
        storyMapper.updateById(last);
        push.pushCoupleEventBoth("story-done", me, space.getUserA(), space.getUserB(),
                "📚 我们的故事又完结一篇啦！去「故事接龙」合上这一页。");
        return stories(me);
    }

    // ========== F105 专属词典小考（聚合恋爱词典，无状态） ==========

    /** 从词典抽词出选择题；词库不足 4 条时给兜底选项。 */
    public DictQuizVO dictQuiz(String me) {
        CoupleSpace space = requireSpace(me);
        List<CoupleDictWord> words = dictMapper.findBySpace(space.getId());
        if (words.isEmpty()) {
            throw new BusinessException(404, "恋爱词典还是空的，先去收录几个专属词汇吧");
        }
        int h = Math.floorMod(CoupleRitualBank.stableHash(space.getId() + "|dictquiz|" + LocalDate.now()), 1_000_003);
        CoupleDictWord target = words.get(h % words.size());
        List<String> options = new ArrayList<>();
        options.add(target.getMeaning());
        String fallbackA = "只有我们俩懂的那种意思（这题你在糊弄我）";
        String fallbackB = "TA 对我的专属爱称的谐音梗";
        String fallbackC = "某次约会翻车现场的暗号";
        for (CoupleDictWord w : words) {
            if (options.size() >= 4) {
                break;
            }
            if (!w.getId().equals(target.getId()) && !options.contains(w.getMeaning())) {
                options.add(w.getMeaning());
            }
        }
        if (options.size() < 2) {
            options.add(fallbackA);
        }
        if (options.size() < 3) {
            options.add(fallbackB);
        }
        if (options.size() < 4) {
            options.add(fallbackC);
        }
        // 打乱（稳定）
        List<String> shuffled = new ArrayList<>(options);
        java.util.Collections.shuffle(shuffled, new java.util.Random(h));
        return new DictQuizVO(target.getId(), target.getWord(), shuffled, shuffled.indexOf(target.getMeaning()));
    }

    // ========== F106 情话合成器（静态） ==========

    /** 合成一句专属情话；seed 由前端随机重摇。 */
    public String synthSweet(String me, long seed) {
        CoupleSpace space = requireSpace(me);
        return CoupleChatBank.synthSweet(space.getId(), seed);
    }

    // ========== F107 道歉三部曲 ==========

    public List<CoupleApologyCard> apologies(String me) {
        return apologyMapper.findBySpace(requireSpace(me).getId());
    }

    /** 送出一张道歉三部曲。 */
    public List<CoupleApologyCard> sendApology(String me, String whatWrong, String whyWrong, String willDo) {
        CoupleSpace space = requireSpace(me);
        String a = trimLimit(whatWrong, CoupleApologyCard.SECTION_MAX, "「我错了」要写 " + CoupleApologyCard.SECTION_MAX + " 字以内哦");
        String b = trimLimit(whyWrong, CoupleApologyCard.SECTION_MAX, "「错在哪」要写 " + CoupleApologyCard.SECTION_MAX + " 字以内哦");
        String c = trimLimit(willDo, CoupleApologyCard.SECTION_MAX, "「以后我会」要写 " + CoupleApologyCard.SECTION_MAX + " 字以内哦");
        if (a == null || b == null || c == null) {
            throw new BusinessException(400, "三步都要认真写完哦，TA 值得一个完整的道歉 🙇");
        }
        CoupleApologyCard row = CoupleApologyCard.of(space.getId(), me, a, b, c);
        apologyMapper.insert(row);
        push.pushCoupleEvent("apology-sent", me, space.partnerOf(me),
                "🙇 TA 送来一张道歉三部曲：「" + a + "」，等你收下。");
        return apologies(me);
    }

    /** TA 收下道歉。 */
    public List<CoupleApologyCard> acceptApology(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleApologyCard row = requireApology(space.getId(), id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己的道歉自己收下可不行哦");
        }
        if (CoupleApologyCard.STATUS_SENT.equals(row.getStatus())) {
            row.setStatus(CoupleApologyCard.STATUS_ACCEPTED);
            row.setAcceptedAt(System.currentTimeMillis());
            apologyMapper.updateById(row);
            push.pushCoupleEvent("apology-accepted", me, row.getFromUser(),
                    "🫶 TA 收下了你的道歉，说「我们和好啦」。");
        }
        return apologies(me);
    }

    // ========== F108 情绪词汇足迹 ==========

    public List<CoupleFeelingWord> feelings(String me) {
        return feelingMapper.findBySpace(requireSpace(me).getId(), 60);
    }

    /** 记录/更新今天的心情词（每人每天一词）。 */
    public List<CoupleFeelingWord> saveFeeling(String me, String word, String note) {
        CoupleSpace space = requireSpace(me);
        String w = trimLimit(word, CoupleFeelingWord.WORD_MAX, "心情词写 " + CoupleFeelingWord.WORD_MAX + " 字以内哦");
        if (w == null) {
            throw new BusinessException(400, "用一个词形容下今天吧，比如「被治愈」");
        }
        String day = LocalDate.now().toString();
        CoupleFeelingWord row = feelingMapper.findBySpace(space.getId(), 60).stream()
                .filter(f -> f.getDay().equals(day) && f.getFromUser().equals(me))
                .findFirst().orElse(null);
        if (row == null) {
            row = CoupleFeelingWord.of(space.getId(), me, day, w,
                    trimLimit(note, CoupleFeelingWord.NOTE_MAX, null));
            feelingMapper.insert(row);
            push.pushCoupleEvent("feeling-word", me, space.partnerOf(me),
                    "📖 TA 用一个词形容了今天：「" + w + "」，去看看吧。");
        } else {
            row.setWord(w);
            row.setNote(trimLimit(note, CoupleFeelingWord.NOTE_MAX, null));
            feelingMapper.updateById(row);
        }
        return feelings(me);
    }

    // ========== F109 晚安电台（聚合歌单 + 静态文案） ==========

    /** 今晚的电台：从「我们的歌单」按空间+天稳定抽一首；歌单为空给兜底文案。 */
    public RadioVO goodnightRadio(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        List<CoupleSong> songs = songMapper.findBySpace(space.getId());
        if (songs.isEmpty()) {
            return new RadioVO(null, null, null, CoupleChatBank.pickRadioLine(space.getId(), day), false);
        }
        CoupleSong song = songs.get(Math.floorMod(
                CoupleRitualBank.stableHash(space.getId() + "|radio|" + day), songs.size()));
        String line = "今晚电台为你点播《" + song.getTitle() + "》"
                + (song.getArtist() == null ? "" : " · " + song.getArtist())
                + "，听完就睡，梦里也要牵手哦 🌙";
        return new RadioVO(song.getTitle(), song.getArtist(), song.getReason(), line, true);
    }

    // ========== 内部工具 ==========

    private CoupleCoolDown requireCool(String spaceId, String id) {
        CoupleCoolDown row = coolMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这场冷静哦");
        }
        return row;
    }

    private CoupleMoodRelay requireRelay(String spaceId, String id) {
        CoupleMoodRelay row = relayMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这根接力棒哦");
        }
        return row;
    }

    private CoupleGuessRound requireGuess(String spaceId, String id) {
        CoupleGuessRound row = guessMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这轮对局哦");
        }
        return row;
    }

    private CoupleApologyCard requireApology(String spaceId, String id) {
        CoupleApologyCard row = apologyMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这张道歉卡哦");
        }
        return row;
    }

    private StoryLineVO toStoryLineVO(CoupleStoryLine line) {
        return new StoryLineVO(line.getId(), line.getChainId(), line.getSeq(), line.getByUser(),
                line.getContent(), line.isFinalLine(), line.getCreated());
    }

    /** 文本校验：非空 + 长度上限（message 非空时超限抛错）。 */
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

    private String trimEmoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            return null;
        }
        String e = emoji.trim();
        return e.length() <= 10 ? e : null;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
