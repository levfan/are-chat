package com.smart.chat.couple.domain.intimatelog;

import com.smart.chat.couple.domain.RuleViolation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * 一次亲密记录：边缘亲昵（EDGE）或性生活（SEX），一行只记一次。
 * <p>
 * 同一天允许有多条——这是真实情况，表上故意没有唯一键；前端按钮的 loading 只防手滑连点。
 * 记录人只做留痕，不做权限闸门：双方都能记、都能删，这是两个人共享的私密账本。
 */
public final class IntimateLog {

    public static final String KIND_EDGE = "EDGE";
    public static final String KIND_SEX = "SEX";
    public static final int NOTE_MAX = 60;

    private final String id;
    private final String spaceId;
    private final String day;
    private final String kind;
    private final String note;
    private final String recorderUser;
    private final Long created;

    private IntimateLog(String id, String spaceId, String day, String kind, String note, String recorderUser,
                        Long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.day = day;
        this.kind = kind;
        this.note = note;
        this.recorderUser = recorderUser;
        this.created = created;
    }

    /** 新记一次：日期不能是将来，类型只收两档，备注可空、最多 60 字。 */
    public static IntimateLog record(String spaceId, String day, String kind, String note, String recorderUser,
                                     LocalDate today) {
        LocalDate date = requireDay(day);
        if (date.isAfter(today)) {
            throw new RuleViolation("还没到的日子记不了呀，今天之后的事留给明天 💞");
        }
        String recorder = recorderUser == null ? "" : recorderUser.trim();
        if (recorder.isEmpty()) {
            throw new RuleViolation("得知道是谁记的呀");
        }
        return new IntimateLog(UUID.randomUUID().toString(), spaceId, date.toString(), requireKind(kind),
                normalizeNote(note), recorder, System.currentTimeMillis());
    }

    public static IntimateLog restore(String id, String spaceId, String day, String kind, String note,
                                      String recorderUser, Long created) {
        return new IntimateLog(id, spaceId, day, kind, note, recorderUser, created);
    }

    private static LocalDate requireDay(String text) {
        if (text == null || text.isBlank()) {
            throw new RuleViolation("哪天呀？日子得选一个");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new RuleViolation("日子得是 yyyy-MM-dd 这样的");
        }
    }

    private static String requireKind(String text) {
        String value = text == null ? "" : text.trim().toUpperCase();
        if (KIND_EDGE.equals(value) || KIND_SEX.equals(value)) {
            return value;
        }
        throw new RuleViolation("这类记录认不出来，选「边缘」或「生活」就好");
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String value = note.trim();
        if (value.length() > NOTE_MAX) {
            throw new RuleViolation("备注最多 " + NOTE_MAX + " 个字，留白也很甜");
        }
        return value;
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String day() {
        return day;
    }

    public String kind() {
        return kind;
    }

    public String note() {
        return note;
    }

    public String recorderUser() {
        return recorderUser;
    }

    public Long created() {
        return created;
    }
}
