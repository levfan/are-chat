package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 文字浪漫系（F160-F169，批次十二）：情诗接龙、三行情书、醒来第一条、心情漂流瓶、
 * 数字密码情书、灵魂提问盲盒、贴纸手账、恋爱语录机、情书模板库、手账贴纸库。
 * 情绪价值设计：让表达被仪式化——每天一句诗、睡前一句话次日送达、坏心情有回信、
 * 情话可以加密成只有彼此看得懂的密码。
 */
@Service
public class CouplePoemService {

    private final CoupleSpaceMapper spaceMapper;
    private final CouplePoemChainMapper chainMapper;
    private final CouplePoem3LineMapper poem3Mapper;
    private final CoupleMorningNoteMapper morningMapper;
    private final CoupleDriftBottleMapper bottleMapper;
    private final CoupleCipherNoteMapper cipherMapper;
    private final CoupleSoulAnswerMapper soulMapper;
    private final CoupleJournalMapper journalMapper;
    private final ImPushService push;

    public CouplePoemService(CoupleSpaceMapper spaceMapper, CouplePoemChainMapper chainMapper,
                             CouplePoem3LineMapper poem3Mapper, CoupleMorningNoteMapper morningMapper,
                             CoupleDriftBottleMapper bottleMapper, CoupleCipherNoteMapper cipherMapper,
                             CoupleSoulAnswerMapper soulMapper, CoupleJournalMapper journalMapper,
                             ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.chainMapper = chainMapper;
        this.poem3Mapper = poem3Mapper;
        this.morningMapper = morningMapper;
        this.bottleMapper = bottleMapper;
        this.cipherMapper = cipherMapper;
        this.soulMapper = soulMapper;
        this.journalMapper = journalMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record PoemLineVO(String id, String day, String fromUser, boolean mine, String line, Long created) {
    }

    public record PoemVO(List<PoemLineVO> lines, String todayWriter, boolean myTurn, boolean writtenToday) {
    }

    public record Poem3VO(String id, String fromUser, boolean mine, String line1, String line2, String line3,
                          boolean liked, Long likedAt, Long created) {
    }

    public record MorningNoteVO(String id, String fromUser, boolean mine, String content,
                                String deliverDay, boolean arrived, boolean read, Long created) {
    }

    public record BottleVO(String id, String fromUser, boolean mine, String mood, String content,
                           String reply, String status, Long repliedAt, Long created) {
    }

    public record CipherVO(String id, String fromUser, boolean mine, String cipher, String hint,
                           String decodedBy, Long decodedAt, Long created) {
    }

    public record SoulVO(String question, CoupleSoulAnswer mine, CoupleSoulAnswer partner, boolean bothAnswered) {
    }

    public record JournalVO(String id, String day, String fromUser, boolean mine, String sticker, String text,
                            Long updatedAt, Long created) {
    }

    // ========== F160 情诗接龙 ==========

    public PoemVO poem(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        List<PoemLineVO> lines = chainMapper.findBySpace(space.getId()).stream()
                .map(l -> new PoemLineVO(l.getId(), l.getDay(), l.getFromUser(), l.getFromUser().equals(me), l.getLine(), l.getCreated()))
                .toList();
        String todayWriter = CoupleRitualBank.stableHash(space.getId() + "|poem-writer|" + day) % 2 == 0
                ? space.getUserA() : space.getUserB();
        boolean writtenToday = chainMapper.find(space.getId(), day, me) != null;
        return new PoemVO(lines, todayWriter, todayWriter.equals(me), writtenToday);
    }

    /** 写今天这一句诗（每人每天一句）。 */
    public PoemVO addLine(String me, String line) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(line, CouplePoemChain.LINE_MAX, "一句诗写 " + CouplePoemChain.LINE_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "写下今天的这一句，长短都可以 🖋️");
        }
        String day = LocalDate.now().toString();
        if (chainMapper.find(space.getId(), day, me) != null) {
            throw new BusinessException(400, "今天这句已经写啦，明天再续 🖋️");
        }
        chainMapper.insert(CouplePoemChain.of(space.getId(), day, me, text));
        push.pushCoupleEvent("poem-line", me, space.partnerOf(me),
                "🖋️ TA 给我们的诗添了一句：「" + abbreviate(text, 20) + "」，该你接了！");
        return poem(me);
    }

    // ========== F161 三行情书 ==========

    public List<Poem3VO> poems3(String me) {
        CoupleSpace space = requireSpace(me);
        return poem3Mapper.findBySpace(space.getId()).stream()
                .map(p -> new Poem3VO(p.getId(), p.getFromUser(), p.getFromUser().equals(me),
                        p.getLine1(), p.getLine2(), p.getLine3(), p.getLikedBy() != null, p.getLikedAt(), p.getCreated()))
                .toList();
    }

    /** 写一封三行情书。 */
    public List<Poem3VO> addPoem3(String me, String line1, String line2, String line3) {
        CoupleSpace space = requireSpace(me);
        String l1 = trimLimit(line1, CouplePoem3Line.LINE_MAX, "每行写 " + CouplePoem3Line.LINE_MAX + " 字以内哦");
        String l2 = trimLimit(line2, CouplePoem3Line.LINE_MAX, "每行写 " + CouplePoem3Line.LINE_MAX + " 字以内哦");
        String l3 = trimLimit(line3, CouplePoem3Line.LINE_MAX, "每行写 " + CouplePoem3Line.LINE_MAX + " 字以内哦");
        if (l1 == null || l2 == null || l3 == null) {
            throw new BusinessException(400, "三行都要写，最深的话用最短的诗说 ✍️");
        }
        poem3Mapper.insert(CouplePoem3Line.of(space.getId(), me, l1, l2, l3));
        push.pushCoupleEvent("poem-made", me, space.partnerOf(me),
                "✍️ TA 写了一封三行情书，快去读——顺便点个赞。");
        return poems3(me);
    }

    /** 对方点赞三行情书。 */
    public List<Poem3VO> likePoem3(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CouplePoem3Line row = poem3Mapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这封情书哦");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己的诗要等 TA 来点赞呀 🥰");
        }
        if (row.getLikedBy() != null) {
            throw new BusinessException(400, "这封已经点过赞啦 ❤️");
        }
        row.setLikedBy(me);
        row.setLikedAt(System.currentTimeMillis());
        poem3Mapper.updateById(row);
        push.pushCoupleEvent("poem-liked", me, row.getFromUser(),
                "❤️ TA 给你的三行情书点了赞，诗人心事被接住了。");
        return poems3(me);
    }

    // ========== F162 醒来第一条 ==========

    /** 我的留言 + 已送达给我的留言。 */
    public record MorningBoxVO(List<MorningNoteVO> mine, List<MorningNoteVO> delivered) {
    }

    public MorningBoxVO morningNotes(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        List<MorningNoteVO> mine = morningMapper.findByUser(space.getId(), me).stream()
                .map(n -> new MorningNoteVO(n.getId(), n.getFromUser(), true, n.getContent(),
                        n.getDeliverDay(), today.compareTo(n.getDeliverDay()) >= 0, n.getReadAt() != null, n.getCreated()))
                .toList();
        List<MorningNoteVO> delivered = morningMapper.findDelivered(space.getId(), me, today).stream()
                .map(n -> new MorningNoteVO(n.getId(), n.getFromUser(), false, n.getContent(),
                        n.getDeliverDay(), true, n.getReadAt() != null, n.getCreated()))
                .toList();
        return new MorningBoxVO(mine, delivered);
    }

    /** 睡前封一条：次日早晨才送达对方。 */
    public MorningBoxVO sealMorningNote(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CoupleMorningNote.CONTENT_MAX, "留言写 " + CoupleMorningNote.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "先写下想让 TA 醒来第一眼看到的话 🌙");
        }
        String tomorrow = LocalDate.now().plusDays(1).toString();
        morningMapper.insert(CoupleMorningNote.of(space.getId(), me, text, tomorrow));
        push.pushCoupleEvent("morning-note-sealed", me, space.partnerOf(me),
                "🌙 TA 给明早的你封了一条「醒来第一条」，明天睁眼见。");
        return morningNotes(me);
    }

    /** 对方已读。 */
    public MorningBoxVO readMorningNote(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleMorningNote row = morningMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这条留言哦");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "这是你自己写的留言呀");
        }
        if (row.getReadAt() == null) {
            row.setReadAt(System.currentTimeMillis());
            morningMapper.updateById(row);
            push.pushCoupleEvent("morning-note-read", me, row.getFromUser(),
                    "☀️ TA 读到了你的「醒来第一条」，一天的好心情已发货。");
        }
        return morningNotes(me);
    }

    // ========== F163 心情漂流瓶 ==========

    public List<BottleVO> bottles(String me) {
        CoupleSpace space = requireSpace(me);
        return bottleMapper.findBySpace(space.getId()).stream()
                .map(b -> new BottleVO(b.getId(), b.getFromUser(), b.getFromUser().equals(me), b.getMood(),
                        b.getContent(), b.getReply(), b.getStatus(), b.getRepliedAt(), b.getCreated()))
                .toList();
    }

    /** 扔一个漂流瓶。 */
    public List<BottleVO> tossBottle(String me, String mood, String content) {
        CoupleSpace space = requireSpace(me);
        String m = trimLimit(mood, 20, "心情写 20 字以内哦");
        String text = trimLimit(content, CoupleDriftBottle.CONTENT_MAX, "瓶中信写 " + CoupleDriftBottle.CONTENT_MAX + " 字以内哦");
        if (m == null || text == null) {
            throw new BusinessException(400, "写上心情和想说的话，再扔进海里 🌊");
        }
        bottleMapper.insert(CoupleDriftBottle.of(space.getId(), me, m, text));
        push.pushCoupleEvent("bottle-tossed", me, space.partnerOf(me),
                "🌊 TA 扔来一个漂流瓶（" + m + "），捡起来看看，回封信吧。");
        return bottles(me);
    }

    /** 对方捡到回信。 */
    public List<BottleVO> replyBottle(String me, String id, String reply) {
        CoupleSpace space = requireSpace(me);
        CoupleDriftBottle row = bottleMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个漂流瓶哦");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己的瓶子要等 TA 来捡哦");
        }
        if (CoupleDriftBottle.STATUS_REPLIED.equals(row.getStatus())) {
            throw new BusinessException(400, "这个瓶子已经回过信啦 💌");
        }
        String text = trimLimit(reply, CoupleDriftBottle.REPLY_MAX, "回信写 " + CoupleDriftBottle.REPLY_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "写一句回应再寄回去吧 💌");
        }
        row.setReply(text);
        row.setRepliedAt(System.currentTimeMillis());
        row.setStatus(CoupleDriftBottle.STATUS_REPLIED);
        bottleMapper.updateById(row);
        push.pushCoupleEvent("bottle-replied", me, row.getFromUser(),
                "💌 你扔的漂流瓶有回信了：「" + abbreviate(text, 20) + "」");
        return bottles(me);
    }

    // ========== F164 数字密码情书 ==========

    public List<CipherVO> cipherNotes(String me) {
        CoupleSpace space = requireSpace(me);
        return cipherMapper.findBySpace(space.getId()).stream()
                .map(c -> new CipherVO(c.getId(), c.getFromUser(), c.getFromUser().equals(me),
                        c.getCipher(), c.getHint(), c.getDecodedBy(), c.getDecodedAt(), c.getCreated()))
                .toList();
    }

    /** 写一封密码情书。 */
    public List<CipherVO> makeCipherNote(String me, String cipher, String hint) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(cipher, CoupleCipherNote.CIPHER_MAX, "密码写 " + CoupleCipherNote.CIPHER_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "先把情话加密成数字密码吧 🔐");
        }
        cipherMapper.insert(CoupleCipherNote.of(space.getId(), me, text,
                trimLimit(hint, CoupleCipherNote.HINT_MAX, null)));
        push.pushCoupleEvent("cipher-note-made", me, space.partnerOf(me),
                "🔐 TA 留了一串神秘数字，解开就是 TA 想对你说的话。");
        return cipherNotes(me);
    }

    /** 对方解码（前端解码成功后上报）。 */
    public List<CipherVO> crackCipherNote(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleCipherNote row = cipherMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这封密码情书哦");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己写的密码自己看呀 🔐");
        }
        if (row.getDecodedBy() != null) {
            throw new BusinessException(400, "这封已经被你解开过啦 🔓");
        }
        row.setDecodedBy(me);
        row.setDecodedAt(System.currentTimeMillis());
        cipherMapper.updateById(row);
        push.pushCoupleEvent("cipher-note-cracked", me, row.getFromUser(),
                "🔓 TA 解开了你的密码情书！这串数字现在只属于你们俩。");
        return cipherNotes(me);
    }

    // ========== F165 灵魂提问盲盒 ==========

    public SoulVO soul(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleSoulAnswer mine = soulMapper.find(space.getId(), day, me);
        CoupleSoulAnswer partner = soulMapper.find(space.getId(), day, space.partnerOf(me));
        boolean both = mine != null && partner != null;
        // 双答才互见对方答案
        return new SoulVO(CouplePoemBank.pickSoulQuestion(space.getId(), day), mine, both ? partner : null, both);
    }

    /** 回答今日灵魂一问（可改，双答互见）。 */
    public SoulVO answerSoul(String me, String answer) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(answer, CoupleSoulAnswer.ANSWER_MAX, "答案写 " + CoupleSoulAnswer.ANSWER_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "认真答一答，深聊一次胜过闲聊一百次 🎁");
        }
        String day = LocalDate.now().toString();
        CoupleSoulAnswer row = soulMapper.find(space.getId(), day, me);
        if (row == null) {
            soulMapper.insert(CoupleSoulAnswer.of(space.getId(), day, me, text));
        } else {
            row.setAnswer(text);
            soulMapper.updateById(row);
        }
        if (soulMapper.find(space.getId(), day, space.partnerOf(me)) != null) {
            push.pushCoupleEventBoth("soul-both", me, space.getUserA(), space.getUserB(),
                    "🎁 今日灵魂一问双方都作答了，快去看看你们想到了一起吗。");
        } else {
            push.pushCoupleEvent("soul-answered", me, space.partnerOf(me),
                    "🎁 TA 已拆开今日灵魂提问并作答，就等你了。");
        }
        return soul(me);
    }

    // ========== F166 贴纸手账 ==========

    public List<JournalVO> journal(String me) {
        CoupleSpace space = requireSpace(me);
        return journalMapper.findBySpace(space.getId()).stream()
                .map(j -> new JournalVO(j.getId(), j.getDay(), j.getFromUser(), j.getFromUser().equals(me),
                        j.getSticker(), j.getText(), j.getUpdatedAt(), j.getCreated()))
                .toList();
    }

    /** 写今天的手账页（可改）。 */
    public List<JournalVO> saveJournal(String me, String sticker, String text) {
        CoupleSpace space = requireSpace(me);
        String body = trimLimit(text, CoupleJournal.TEXT_MAX, "手账写 " + CoupleJournal.TEXT_MAX + " 字以内哦");
        if (body == null) {
            throw new BusinessException(400, "写一句今天的小事，贴纸会帮你记住 📔");
        }
        String s = sticker == null || sticker.isBlank() ? CoupleJournal.DEFAULT_STICKER : sticker.trim();
        if (!CouplePoemBank.stickers().contains(s)) {
            throw new BusinessException(400, "贴纸要从贴纸库里挑哦 ✨");
        }
        String day = LocalDate.now().toString();
        CoupleJournal row = journalMapper.find(space.getId(), day, me);
        if (row == null) {
            journalMapper.insert(CoupleJournal.of(space.getId(), day, me, s, body));
        } else {
            row.setSticker(s);
            row.setText(body);
            row.setUpdatedAt(System.currentTimeMillis());
            journalMapper.updateById(row);
        }
        push.pushCoupleEvent("journal-updated", me, space.partnerOf(me),
                "📔 TA 写了今天的手账：「" + s + " " + abbreviate(body, 18) + "」");
        return journal(me);
    }

    // ========== F167 恋爱语录机（无表） ==========

    /** 用真实数据拼一句专属情话。 */
    public String quote(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        String template = CouplePoemBank.quoteTemplates()
                .get(Math.floorMod(CoupleRitualBank.stableHash(space.getId() + "|quote|" + day), CouplePoemBank.quoteTemplates().size()));
        long days = Math.max(1, (System.currentTimeMillis() - space.getCreated()) / (24 * 60 * 60 * 1000L) + 1);
        return template.replace("{days}", String.valueOf(days)).replace("{partner}", space.partnerOf(me));
    }

    // ========== F168 情书模板库（无表） ==========

    public List<CouplePoemBank.LetterTemplate> letterTemplates() {
        return CouplePoemBank.letterTemplates();
    }

    // ========== F169 手账贴纸库（无表） ==========

    public List<String> stickers() {
        return CouplePoemBank.stickers();
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
}
