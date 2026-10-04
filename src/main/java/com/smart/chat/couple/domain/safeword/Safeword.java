package com.smart.chat.couple.domain.safeword;

import com.smart.chat.couple.domain.RuleViolation;

/**
 * 安全词：一人一格，词与说明都是「先约好，才喊得出口」。
 * 没约过词就喊停是不成立的——这条闸门原先长在 Service 里。
 */
public final class Safeword {

    public static final int WORD_MAX = 20;
    public static final int NOTE_MAX = 60;

    private final String id;
    private final String spaceId;
    private final String fromUser;
    private final String word;
    private final String note;

    private Safeword(String id, String spaceId, String fromUser, String word, String note) {
        this.id = id;
        this.spaceId = spaceId;
        this.fromUser = fromUser;
        this.word = word;
        this.note = note;
    }

    /** 约定或改写安全词 */
    public static Safeword agree(String word, String note) {
        String text = word == null ? "" : word.trim();
        if (text.isEmpty()) {
            throw new RuleViolation("暂停词总得有个词");
        }
        if (text.length() > WORD_MAX) {
            throw new RuleViolation("安全词最多 " + WORD_MAX + " 字");
        }
        String trimmed = note == null ? "" : note.trim();
        if (trimmed.length() > NOTE_MAX) {
            throw new RuleViolation("用了之后希望最多 " + NOTE_MAX + " 字");
        }
        return new Safeword(null, null, null, text, trimmed.isEmpty() ? null : trimmed);
    }

    /** 从存储重建：不校验——历史词必须读得出来，带上归属（谁的词、哪个空间）供看板投影。 */
    public static Safeword restore(String id, String spaceId, String fromUser, String word, String note) {
        return new Safeword(id, spaceId, fromUser, word, note);
    }

    /** 喊停的前提：词已经约好 */
    public static void requireAgreed(Safeword agreed) {
        if (agreed == null) {
            throw new RuleViolation("先约一个安全词，才喊得出口 🛑");
        }
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String fromUser() {
        return fromUser;
    }

    public String word() {
        return word;
    }

    public String note() {
        return note;
    }
}
