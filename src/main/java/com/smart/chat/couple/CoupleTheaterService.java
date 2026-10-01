package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 扮演剧场（F300-F309，批次二十六）：今日身份签、一日互换日记、师徒日、时空电话亭、
 * 黑话大全、每日奥斯卡、如果我是你爸妈、双角色追剧、今日客服、冷知识颁奖礼。
 * 情绪价值设计：换位不是讲道理，是真的过一天对方的日子——
 * 演过别人的人，回到自己身上会更温柔一点。
 */
@Service
public class CoupleTheaterService {

    static final int DIARY_MAX = 300;
    static final int BOOTH_MAX = 300;
    static final int TERM_MAX = 40;
    static final int REF_FIELD_MAX = 200;
    static final int EVIDENCE_MAX = 140;
    static final int FAMILY_ANSWER_MAX = 200;
    static final int WORK_MAX = 40;
    static final int ROLE_NAME_MAX = 20;
    static final int MOVIE_ENTRY_MAX = 200;
    static final int MOVIE_DIARY_MAX = 600;
    static final int NOTE_MAX = 80;
    static final int REVIEW_MAX = 100;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleRoleDayMapper roleMapper;
    private final CoupleSwapDiaryMapper diaryMapper;
    private final CoupleMasterDayMapper masterMapper;
    private final CoupleBoothNoteMapper boothMapper;
    private final CouplePrivateRefMapper refMapper;
    private final CoupleActAwardMapper awardMapper;
    private final CoupleIfFamilyMapper familyMapper;
    private final CoupleRoleMovieMapper movieMapper;
    private final CoupleServiceTicketMapper ticketMapper;
    private final ImPushService push;

    public CoupleTheaterService(CoupleSpaceMapper spaceMapper, CoupleRoleDayMapper roleMapper,
                                CoupleSwapDiaryMapper diaryMapper, CoupleMasterDayMapper masterMapper,
                                CoupleBoothNoteMapper boothMapper, CouplePrivateRefMapper refMapper,
                                CoupleActAwardMapper awardMapper, CoupleIfFamilyMapper familyMapper,
                                CoupleRoleMovieMapper movieMapper, CoupleServiceTicketMapper ticketMapper,
                                ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.roleMapper = roleMapper;
        this.diaryMapper = diaryMapper;
        this.masterMapper = masterMapper;
        this.boothMapper = boothMapper;
        this.refMapper = refMapper;
        this.awardMapper = awardMapper;
        this.familyMapper = familyMapper;
        this.movieMapper = movieMapper;
        this.ticketMapper = ticketMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record RoleVO(String day, String roleName, String guide, boolean mineRated,
                         Integer myRate, Integer partnerRate, boolean bothRated) {
    }

    public record DiaryVO(String day, String mine, String partner, boolean bothIn) {
    }

    public record MasterVO(String week, String masterUser, String apprenticeUser, boolean iAmMaster,
                           int serveCount, int serveTarget, boolean servedToday, boolean canReview,
                           String review, String grade) {
    }

    public record BoothVO(String id, boolean mine, String kind, String text, String openDay,
                          String status, long daysLeft, String line) {
    }

    public record RefVO(String id, String term, String meaning, String origin, boolean mine,
                        String quizAnswer, String quizBy, String judged, boolean canQuiz, boolean canJudge) {
    }

    public record AwardVO(String day, String fromUser, String aboutUser, String evidence,
                          boolean mine, String line) {
    }

    public record FamilyVO(String day, String question, String myAnswer, String partnerAnswer, boolean bothIn) {
    }

    public record MovieVO(String work, String myRole, String myDiary, String partnerRole, String partnerDiary,
                          String status, boolean bothClaimed, boolean finished) {
    }

    public record TicketVO(String id, String note, String status, String customerUser, boolean mineCustomer,
                           boolean canAnswer, boolean canScore, boolean canAppeal, boolean onTime,
                           Integer score, String appeal, long waitMinutes) {
    }

    public record GalaVO(String day, String prize, String line, int nominations, int terms,
                         int quizzed, int rights, int diaryDays, int onTimeOrders, int orders) {
    }

    public record TheaterVO(String day, String week, RoleVO role, List<DiaryVO> diaries, MasterVO master,
                            List<BoothVO> booths, List<RefVO> refs, List<AwardVO> awards, FamilyVO family,
                            List<MovieVO> movies, List<TicketVO> tickets, GalaVO gala) {
    }

    // ========== 读：今日剧场（含惰性结算：电话亭到点接通） ==========

    /** 今日剧场总览。 */
    public TheaterVO today(String me) {
        CoupleSpace space = requireSpace(me);
        settle(space, LocalDate.now());
        return build(space, me, LocalDate.now());
    }

    // ========== F300 今日身份签 ==========

    /** 日终给今天扮演的 TA 打分（1-5，本人一次，双方齐了互见）。 */
    public TheaterVO rateRole(String me, Integer score) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRoleDay row = ensureRole(space, now.toString());
        int v = score == null ? 0 : score;
        if (v < 1 || v > 5) {
            throw new BusinessException(400, "演技分 1-5，别打太狠");
        }
        boolean isA = me.equals(space.getUserA());
        if (isA) {
            if (row.getRateA() != null) {
                return build(space, me, now);
            }
            row.setRateA(v);
        } else {
            if (row.getRateB() != null) {
                return build(space, me, now);
            }
            row.setRateB(v);
        }
        roleMapper.updateById(row);
        if (row.getRateA() != null && row.getRateB() != null) {
            push.pushCoupleEventBoth("theater-role-rated", me, space.getUserA(), space.getUserB(),
                    "今天的「" + row.getRoleName() + "」双方都打了分，片场收工 🎬");
        } else {
            push.pushCoupleEvent("theater-role-rated", me, space.partnerOf(me),
                    "TA 已给今天的你打好演技分，等你那一笔 🎬");
        }
        return build(space, me, now);
    }

    // ========== F301 一日互换日记 ==========

    /** 以 TA 的身份写今天这一页（当日本人可改写，不重推）。 */
    public TheaterVO swapDiary(String me, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String t = trim(text, "这一页总得写点什么");
        if (t.length() > DIARY_MAX) {
            throw new BusinessException(400, "最多 300 字，扮演不是写论文");
        }
        CoupleSwapDiary mine = null;
        List<CoupleSwapDiary> rows = diaryMapper.findByDay(space.getId(), day);
        for (CoupleSwapDiary d : rows) {
            if (me.equals(d.getFromUser())) {
                mine = d;
            }
        }
        boolean partnerIn = rows.stream().anyMatch(d -> !me.equals(d.getFromUser()));
        if (mine != null) {
            mine.setText(t);
            mine.setUpdatedAt(System.currentTimeMillis());
            diaryMapper.updateById(mine);
        } else {
            diaryMapper.insert(CoupleSwapDiary.of(space.getId(), day, me, t));
            if (partnerIn) {
                push.pushCoupleEventBoth("theater-diary-both", me, space.getUserA(), space.getUserB(),
                        "两页「作为对方的一天」都写完了，可以对着读 📖");
            } else {
                push.pushCoupleEvent("theater-diary", me, space.partnerOf(me),
                        "TA 已经写完今天那页你的日记，等你那页 📖");
            }
        }
        return build(space, me, now);
    }

    // ========== F302 师徒日 ==========

    /** 徒弟今日侍奉打卡（每日一次）。 */
    public TheaterVO masterServe(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleMasterDay row = ensureMaster(space, now);
        if (!row.getApprenticeUser().equals(me)) {
            throw new BusinessException(400, "这周你是师父，侍奉记录由徒弟来写");
        }
        // 记号用周几 1-7（serves 列只有 40 宽，存 ISO 日期三天就满，一周要能记七天）
        String token = String.valueOf(now.getDayOfWeek().getValue());
        if (row.hasServed(token)) {
            return build(space, me, now);
        }
        if (row.serveCount() >= 7) {
            throw new BusinessException(400, "一周最多侍奉 7 天，师父不是地主");
        }
        row.setServes(row.getServes() == null || row.getServes().isEmpty()
                ? token : row.getServes() + "," + token);
        row.setUpdatedAt(System.currentTimeMillis());
        masterMapper.updateById(row);
        push.pushCoupleEvent("theater-serve", me, row.getMasterUser(),
                "徒弟今日侍奉已打卡（" + row.serveCount() + "/" + CoupleMasterDay.SERVE_TARGET + "），师父可以验收 🧎");
        return build(space, me, now);
    }

    /** 师父期满评语定级（至少 3 次侍奉才出师）。 */
    public TheaterVO masterReview(String me, String review, String grade) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleMasterDay row = ensureMaster(space, now);
        if (!row.getMasterUser().equals(me)) {
            throw new BusinessException(400, "评语和定级要师父本人写");
        }
        if (!row.getGrade().isEmpty()) {
            throw new BusinessException(400, "这周已经定过级了，下周再收一次徒");
        }
        String g = grade == null ? "" : grade.trim().toUpperCase();
        if (!"GRADUATED".equals(g) && !"REPEAT".equals(g)) {
            throw new BusinessException(400, "定级只有出师和留级两个章");
        }
        if ("GRADUATED".equals(g) && row.serveCount() < CoupleMasterDay.SERVE_TARGET) {
            throw new BusinessException(400, "侍奉不满 " + CoupleMasterDay.SERVE_TARGET + " 次，先留级吧");
        }
        String r = review == null ? "" : review.trim();
        if (r.length() > REVIEW_MAX) {
            throw new BusinessException(400, "评语最多 100 字");
        }
        row.setReview(r);
        row.setGrade(g);
        row.setUpdatedAt(System.currentTimeMillis());
        masterMapper.updateById(row);
        push.pushCoupleEventBoth("theater-master-grade", me, space.getUserA(), space.getUserB(),
                CoupleTheaterBank.masterGradeLine(g));
        return build(space, me, now);
    }

    // ========== F303 时空电话亭 ==========

    /** 拨一通跨时空电话：FUTURE 一年后接通，PAST 当场接通（杂音彩蛋）。 */
    public TheaterVO booth(String me, String kind, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String k = kind == null || kind.isBlank() ? CoupleBoothNote.KIND_FUTURE : kind.trim().toUpperCase();
        if (!CoupleBoothNote.KIND_FUTURE.equals(k) && !CoupleBoothNote.KIND_PAST.equals(k)) {
            throw new BusinessException(400, "电话只有两种去向：给一年后，或给一年前");
        }
        String t = trim(text, "都拨号了，总要说一句");
        if (t.length() > BOOTH_MAX) {
            throw new BusinessException(400, "最多 300 字，跨时空长途按字收费");
        }
        CoupleBoothNote note = CoupleBoothNote.of(space.getId(), me, k, t,
                CoupleBoothNote.KIND_PAST.equals(k) ? now.toString() : now.plusYears(1).toString());
        if (CoupleBoothNote.KIND_PAST.equals(k)) {
            note.setStatus(CoupleBoothNote.STATUS_SENT);
            note.setSentAt(System.currentTimeMillis());
        }
        boothMapper.insert(note);
        if (CoupleBoothNote.KIND_PAST.equals(k)) {
            push.pushCoupleEventBoth("theater-booth-past", me, space.getUserA(), space.getUserB(),
                    CoupleTheaterBank.boothStaticLine(CoupleRitualBank.stableHash(space.getId() + "|booth|" + note.getId())));
        } else {
            push.pushCoupleEvent("theater-booth-sealed", me, space.partnerOf(me),
                    "TA 给一年后的我们打了一通电话，你的那通也在等你 📞");
        }
        return build(space, me, now);
    }

    // ========== F304 黑话大全 ==========

    /** 收录一条只有俩人懂的梗。 */
    public TheaterVO addRef(String me, String term, String meaning, String origin) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String tm = trim(term, "黑话本体要写");
        if (tm.length() > TERM_MAX) {
            throw new BusinessException(400, "词条最多 40 字");
        }
        if (refMapper.findByTerm(space.getId(), tm) != null) {
            throw new BusinessException(400, "这个梗已经收进大全了");
        }
        String m = trim(meaning, "意思要写，不然明年谁都看不懂");
        if (m.length() > REF_FIELD_MAX) {
            throw new BusinessException(400, "释义最多 200 字");
        }
        String o = origin == null ? "" : origin.trim();
        if (o.length() > REF_FIELD_MAX) {
            throw new BusinessException(400, "出处最多 200 字");
        }
        refMapper.insert(CouplePrivateRef.of(space.getId(), tm, m, o, me));
        push.pushCoupleEvent("theater-ref", me, space.partnerOf(me),
                "大全又收了一条：「" + tm + "」——下次抽查考你 📖");
        return build(space, me, now);
    }

    /** 抽查作答（自己收录的词条不能考自己）。 */
    public TheaterVO quizRef(String me, String term, String answer) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CouplePrivateRef row = requireRef(space, term);
        if (me.equals(row.getFromUser())) {
            throw new BusinessException(400, "自己收的梗不能考自己，让 TA 来");
        }
        String a = trim(answer, "凭记忆写一句意思");
        if (a.length() > REF_FIELD_MAX) {
            throw new BusinessException(400, "作答最多 200 字");
        }
        row.setQuizAnswer(a);
        row.setQuizBy(me);
        row.setJudged("");
        row.setUpdatedAt(System.currentTimeMillis());
        refMapper.updateById(row);
        push.pushCoupleEvent("theater-ref-quiz", me, row.getFromUser(),
                "TA 答完了「" + row.getTerm() + "」，等你这个收录人判卷 ✍️");
        return build(space, me, now);
    }

    /** 收录人判卷：谁先忘。 */
    public TheaterVO judgeRef(String me, String term, Boolean right) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CouplePrivateRef row = requireRef(space, term);
        if (!me.equals(row.getFromUser())) {
            throw new BusinessException(400, "词条判卷归收录人，别人不配打分");
        }
        if (row.getQuizBy() == null || row.getQuizBy().isEmpty() || me.equals(row.getQuizBy())) {
            throw new BusinessException(400, "TA 还没作答，判什么卷");
        }
        boolean r = Boolean.TRUE.equals(right);
        row.setJudged(r ? "RIGHT" : "WRONG");
        row.setUpdatedAt(System.currentTimeMillis());
        refMapper.updateById(row);
        push.pushCoupleEventBoth("theater-ref-judged", me, space.getUserA(), space.getUserB(),
                CoupleTheaterBank.refJudgeLine(r));
        return build(space, me, now);
    }

    // ========== F305 每日奥斯卡 ==========

    /** 今日最佳演技提名（一人一天一次，改主意可覆盖当日本人那次）。 */
    public TheaterVO nominate(String me, String evidence) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String e = trim(evidence, "提名要写一句证据");
        if (e.length() > EVIDENCE_MAX) {
            throw new BusinessException(400, "证据最多 140 字");
        }
        CoupleActAward exist = awardMapper.findByDayUser(space.getId(), day, me);
        String about = space.partnerOf(me);
        if (exist != null) {
            exist.setEvidence(e);
            awardMapper.updateById(exist);
        } else {
            awardMapper.insert(CoupleActAward.of(space.getId(), day, me, about, e));
            push.pushCoupleEvent("theater-award", me, about,
                    "今日奥斯卡提名已递出，理由写得挺狠 🏆");
        }
        return build(space, me, now);
    }

    // ========== F306 如果我是你爸妈 ==========

    /** 作答今日家长题（双方都答完才互见）。 */
    public TheaterVO familyAnswer(String me, String answer) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleIfFamily row = ensureFamily(space, now.toString());
        String a = trim(answer, "被家长问了，总得答一句");
        if (a.length() > FAMILY_ANSWER_MAX) {
            throw new BusinessException(400, "作答最多 200 字");
        }
        boolean isA = me.equals(space.getUserA());
        boolean mineFilled = isA ? !row.getAnswerA().isEmpty() : !row.getAnswerB().isEmpty();
        boolean partnerFilled = isA ? !row.getAnswerB().isEmpty() : !row.getAnswerA().isEmpty();
        if (isA) {
            row.setAnswerA(a);
        } else {
            row.setAnswerB(a);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        familyMapper.updateById(row);
        if (!mineFilled) {
            if (partnerFilled) {
                push.pushCoupleEventBoth("theater-family-both", me, space.getUserA(), space.getUserB(),
                        "家长题两份答卷齐了——原来你们挡的是同一件事 👨‍👩‍👦");
            } else {
                push.pushCoupleEvent("theater-family", me, space.partnerOf(me),
                        "TA 已答完今天的家长题，等你落笔 👨‍👩‍👦");
            }
        }
        return build(space, me, now);
    }

    // ========== F307 双角色追剧 ==========

    /** 认领角色并追更一段角色日记（同人同日可续写）。 */
    public TheaterVO movieWrite(String me, String work, String roleName, String entry) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String w = trim(work, "剧目名要写");
        if (w.length() > WORK_MAX) {
            throw new BusinessException(400, "剧目名最多 40 字");
        }
        String rn = trim(roleName, "认领哪个角色");
        if (rn.length() > ROLE_NAME_MAX) {
            throw new BusinessException(400, "角色名最多 20 字");
        }
        String en = entry == null ? "" : entry.trim();
        if (en.length() > MOVIE_ENTRY_MAX) {
            throw new BusinessException(400, "单次日记最多 200 字，追更不是一次写完");
        }
        CoupleRoleMovie row = null;
        for (CoupleRoleMovie m : movieMapper.findByWork(space.getId(), w)) {
            if (me.equals(m.getFromUser())) {
                row = m;
            }
        }
        if (row != null) {
            if (row.getRoleName().isEmpty()) {
                row.setRoleName(rn);
            }
            String merged = row.getDiary().isEmpty() ? en : row.getDiary() + "\n" + en;
            if (merged.length() > MOVIE_DIARY_MAX) {
                throw new BusinessException(400, "角色日记最多 600 字，先剧终归档再开新的");
            }
            row.setDiary(merged);
            row.setUpdatedAt(System.currentTimeMillis());
            movieMapper.updateById(row);
            return build(space, me, now);
        }
        CoupleRoleMovie mine = CoupleRoleMovie.of(space.getId(), w, me, rn);
        mine.setDiary(en);
        movieMapper.insert(mine);
        push.pushCoupleEvent("theater-movie", me, space.partnerOf(me),
                "TA 在「" + w + "」认领了「" + rn + "」，等你来演另一个 🎭");
        return build(space, me, now);
    }

    /** 我这一路剧终（双方都剧终才合成双视角剧本）。 */
    public TheaterVO movieFinish(String me, String work) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String w = trim(work, "要剧终哪一部");
        CoupleRoleMovie mine = null;
        List<CoupleRoleMovie> rows = movieMapper.findByWork(space.getId(), w);
        for (CoupleRoleMovie m : rows) {
            if (me.equals(m.getFromUser())) {
                mine = m;
            }
        }
        if (mine == null) {
            throw new BusinessException(400, "你还没在这部剧里认领角色");
        }
        if (CoupleRoleMovie.STATUS_FINISHED.equals(mine.getStatus())) {
            return build(space, me, now);
        }
        mine.setStatus(CoupleRoleMovie.STATUS_FINISHED);
        mine.setUpdatedAt(System.currentTimeMillis());
        movieMapper.updateById(mine);
        boolean partnerFinished = rows.stream()
                .anyMatch(m -> !me.equals(m.getFromUser()) && CoupleRoleMovie.STATUS_FINISHED.equals(m.getStatus()));
        if (partnerFinished) {
            push.pushCoupleEventBoth("theater-movie-script", me, space.getUserA(), space.getUserB(),
                    "「" + w + "」双视角剧本合上了——"
                            + CoupleTheaterBank.movieScriptLine(CoupleRitualBank.stableHash(space.getId() + "|script|" + w)));
        } else {
            push.pushCoupleEvent("theater-movie-finish", me, space.partnerOf(me),
                    "TA 那一路已经剧终，等你收尾合剧本 🎬");
        }
        return build(space, me, now);
    }

    // ========== F308 今日客服 ==========

    /** 下一张服务工单（合理小事）。 */
    public TheaterVO placeOrder(String me, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String n = trim(note, "工单要写清要什么");
        if (n.length() > NOTE_MAX) {
            throw new BusinessException(400, "工单最多 80 字");
        }
        ticketMapper.insert(CoupleServiceTicket.of(space.getId(), n, me));
        push.pushCoupleEvent("theater-order", me, space.partnerOf(me),
                "客服请注意：新工单「" + n + "」，30 分钟内响应 🛎️");
        return build(space, me, now);
    }

    /** 客服接单响应（只有非下单人可接）。 */
    public TheaterVO answerOrder(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleServiceTicket t = requireTicket(space, id);
        if (t.getCustomerUser().equals(me)) {
            throw new BusinessException(400, "自己的单自己接不了，等 TA 上线");
        }
        if (!CoupleServiceTicket.STATUS_OPEN.equals(t.getStatus())) {
            throw new BusinessException(400, "这单已经有人接过了");
        }
        long ts = System.currentTimeMillis();
        t.setStatus(CoupleServiceTicket.STATUS_ANSWERED);
        t.setAnsweredAt(ts);
        t.setOnTime(ts - t.getCreated() <= CoupleServiceTicket.ON_TIME_MS ? 1 : 0);
        t.setUpdatedAt(ts);
        ticketMapper.updateById(t);
        push.pushCoupleEvent("theater-answered", me, t.getCustomerUser(),
                (t.getOnTime() == 1 ? "客服 30 分钟内响应 ✅" : "客服响应到了，但超时了 ⏰")
                        + "：「" + t.getNote() + "」");
        return build(space, me, now);
    }

    /** 顾客评分（1-5）。 */
    public TheaterVO scoreOrder(String me, String id, Integer score) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleServiceTicket t = requireTicket(space, id);
        if (!t.getCustomerUser().equals(me)) {
            throw new BusinessException(400, "评分归顾客，客服不能给自己打分");
        }
        if (!CoupleServiceTicket.STATUS_ANSWERED.equals(t.getStatus())) {
            throw new BusinessException(400, "还没人接单，评什么分");
        }
        int v = score == null ? 0 : score;
        if (v < 1 || v > 5) {
            throw new BusinessException(400, "评分 1-5");
        }
        t.setStatus(CoupleServiceTicket.STATUS_RATED);
        t.setScore(v);
        t.setUpdatedAt(System.currentTimeMillis());
        ticketMapper.updateById(t);
        push.pushCoupleEvent("theater-rated", me, space.partnerOf(me),
                "顾客给出了 " + v + " 星评价" + (v <= 2 ? "，客服可以申诉一次 🧾" : "，客服本月绩效稳了 ⭐"));
        return build(space, me, now);
    }

    /** 客服对差评申诉（仅 1-2 星可申诉一次）。 */
    public TheaterVO appealOrder(String me, String id, String appeal) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleServiceTicket t = requireTicket(space, id);
        if (t.getCustomerUser().equals(me)) {
            throw new BusinessException(400, "顾客只负责打分，申诉是客服的活儿");
        }
        if (!CoupleServiceTicket.STATUS_RATED.equals(t.getStatus())) {
            throw new BusinessException(400, "只有评过分的差评单能申诉");
        }
        if (t.getScore() == null || t.getScore() > 2) {
            throw new BusinessException(400, "这是好评，不用申诉，收下就行");
        }
        String a = trim(appeal, "申诉要写一句理由");
        if (a.length() > NOTE_MAX) {
            throw new BusinessException(400, "申诉最多 80 字");
        }
        t.setStatus(CoupleServiceTicket.STATUS_APPEALED);
        t.setAppeal(a);
        t.setUpdatedAt(System.currentTimeMillis());
        ticketMapper.updateById(t);
        push.pushCoupleEventBoth("theater-appeal", me, space.getUserA(), space.getUserB(),
                "客服申诉：「" + a + "」——" + CoupleTheaterBank.appealLine(
                        CoupleRitualBank.stableHash(space.getId() + "|appeal|" + t.getId())));
        return build(space, me, now);
    }

    // ========== F309 冷知识颁奖礼（读时聚合，无表，数据在 TheaterVO.gala） ==========

    // ========== 惰性结算 ==========

    private void settle(CoupleSpace space, LocalDate now) {
        String today = now.toString();
        for (CoupleBoothNote n : boothMapper.findDue(space.getId(), today)) {
            n.setStatus(CoupleBoothNote.STATUS_SENT);
            n.setSentAt(System.currentTimeMillis());
            boothMapper.updateById(n);
            push.pushCoupleEventBoth("theater-booth-connected", n.getFromUser(), space.getUserA(), space.getUserB(),
                    "跨时空电话接通了——" + CoupleTheaterBank.boothStaticLine(
                            CoupleRitualBank.stableHash(space.getId() + "|booth|" + n.getId())));
        }
    }

    // ========== 聚合 ==========

    private TheaterVO build(CoupleSpace space, String me, LocalDate now) {
        String partner = space.partnerOf(me);
        String today = now.toString();
        String week = now.with(DayOfWeek.MONDAY).toString();
        boolean isA = me.equals(space.getUserA());

        CoupleRoleDay role = ensureRole(space, today);
        Integer myRate = isA ? role.getRateA() : role.getRateB();
        Integer partnerRate = isA ? role.getRateB() : role.getRateA();
        RoleVO roleVO = new RoleVO(today, role.getRoleName(), role.getGuide(), myRate != null,
                myRate, partnerRate, role.getRateA() != null && role.getRateB() != null);

        List<DiaryVO> diaries = new ArrayList<>();
        Map<String, CoupleSwapDiary[]> byDay = new LinkedHashMap<>();
        for (CoupleSwapDiary d : diaryMapper.findBySpace(space.getId())) {
            byDay.computeIfAbsent(d.getDay(), k -> new CoupleSwapDiary[2]);
            CoupleSwapDiary[] pair = byDay.get(d.getDay());
            if (me.equals(d.getFromUser())) {
                pair[0] = d;
            } else {
                pair[1] = d;
            }
        }
        for (Map.Entry<String, CoupleSwapDiary[]> e : byDay.entrySet()) {
            CoupleSwapDiary mine = e.getValue()[0];
            CoupleSwapDiary other = e.getValue()[1];
            boolean bothIn = mine != null && other != null;
            diaries.add(new DiaryVO(e.getKey(), mine == null ? "" : mine.getText(),
                    bothIn ? other.getText() : "", bothIn));
        }

        CoupleMasterDay master = ensureMaster(space, now);
        MasterVO masterVO = new MasterVO(week, master.getMasterUser(), master.getApprenticeUser(),
                me.equals(master.getMasterUser()), master.serveCount(), CoupleMasterDay.SERVE_TARGET,
                master.hasServed(String.valueOf(now.getDayOfWeek().getValue())),
                me.equals(master.getMasterUser()) && master.getGrade().isEmpty(),
                master.getReview(), master.getGrade());

        List<BoothVO> booths = new ArrayList<>();
        for (CoupleBoothNote n : boothMapper.findBySpace(space.getId())) {
            boolean mine = me.equals(n.getFromUser());
            boolean sealed = CoupleBoothNote.STATUS_SEALED.equals(n.getStatus());
            long left = sealed ? ChronoUnit.DAYS.between(now, LocalDate.parse(n.getOpenDay())) : 0;
            String line = sealed ? "" : CoupleTheaterBank.boothStaticLine(
                    CoupleRitualBank.stableHash(space.getId() + "|booth|" + n.getId()));
            booths.add(new BoothVO(n.getId(), mine, n.getKind(), n.getText(), n.getOpenDay(),
                    n.getStatus(), left, line));
        }

        List<RefVO> refs = new ArrayList<>();
        int quizzed = 0;
        int rights = 0;
        for (CouplePrivateRef r : refMapper.findBySpace(space.getId())) {
            boolean mine = me.equals(r.getFromUser());
            boolean iAnswered = me.equals(r.getQuizBy());
            boolean partnerAnswered = r.getQuizBy() != null && !r.getQuizBy().isEmpty() && !iAnswered;
            if (!r.getQuizBy().isEmpty()) {
                quizzed++;
            }
            if ("RIGHT".equals(r.getJudged())) {
                rights++;
            }
            refs.add(new RefVO(r.getId(), r.getTerm(), r.getMeaning(), r.getOrigin(), mine,
                    r.getQuizAnswer(), r.getQuizBy(), r.getJudged(), !mine && !iAnswered,
                    mine && partnerAnswered && r.getJudged().isEmpty()));
        }

        List<AwardVO> awards = new ArrayList<>();
        for (CoupleActAward a : awardMapper.findBySpace(space.getId())) {
            awards.add(new AwardVO(a.getDay(), a.getFromUser(), a.getAboutUser(), a.getEvidence(),
                    me.equals(a.getFromUser()), CoupleTheaterBank.actAwardLine(
                            CoupleRitualBank.stableHash(space.getId() + "|act|" + a.getDay() + "|" + a.getFromUser()),
                            partner)));
        }

        CoupleIfFamily family = ensureFamily(space, today);
        String myAnswer = isA ? family.getAnswerA() : family.getAnswerB();
        String otherAnswer = isA ? family.getAnswerB() : family.getAnswerA();
        FamilyVO familyVO = new FamilyVO(today, family.getQuestion(), myAnswer,
                (!myAnswer.isEmpty() && !otherAnswer.isEmpty()) ? otherAnswer : "",
                !myAnswer.isEmpty() && !otherAnswer.isEmpty());

        Map<String, CoupleRoleMovie[]> moviePairs = new LinkedHashMap<>();
        for (CoupleRoleMovie m : movieMapper.findBySpace(space.getId())) {
            moviePairs.computeIfAbsent(m.getWork(), k -> new CoupleRoleMovie[2]);
            CoupleRoleMovie[] pair = moviePairs.get(m.getWork());
            if (me.equals(m.getFromUser())) {
                pair[0] = m;
            } else {
                pair[1] = m;
            }
        }
        List<MovieVO> movies = new ArrayList<>();
        for (Map.Entry<String, CoupleRoleMovie[]> e : moviePairs.entrySet()) {
            CoupleRoleMovie mine = e.getValue()[0];
            CoupleRoleMovie other = e.getValue()[1];
            boolean finished = mine != null && CoupleRoleMovie.STATUS_FINISHED.equals(mine.getStatus());
            boolean bothFinished = finished && other != null
                    && CoupleRoleMovie.STATUS_FINISHED.equals(other.getStatus());
            movies.add(new MovieVO(e.getKey(),
                    mine == null ? "" : mine.getRoleName(), mine == null ? "" : mine.getDiary(),
                    other == null ? "" : other.getRoleName(),
                    bothFinished && other != null ? other.getDiary() : "",
                    bothFinished ? CoupleRoleMovie.STATUS_FINISHED : CoupleRoleMovie.STATUS_ONGOING,
                    mine != null && other != null, bothFinished));
        }

        List<TicketVO> tickets = new ArrayList<>();
        long nowTs = System.currentTimeMillis();
        int orders = 0;
        int onTimeOrders = 0;
        for (CoupleServiceTicket t : ticketMapper.findBySpace(space.getId())) {
            boolean mineCustomer = me.equals(t.getCustomerUser());
            boolean openish = CoupleServiceTicket.STATUS_OPEN.equals(t.getStatus());
            boolean answeredish = CoupleServiceTicket.STATUS_ANSWERED.equals(t.getStatus());
            boolean rated = CoupleServiceTicket.STATUS_RATED.equals(t.getStatus());
            boolean appealable = rated && t.getScore() != null && t.getScore() <= 2;
            tickets.add(new TicketVO(t.getId(), t.getNote(), t.getStatus(), t.getCustomerUser(), mineCustomer,
                    openish && !mineCustomer, answeredish && mineCustomer,
                    appealable && !mineCustomer, t.getOnTime() != null && t.getOnTime() == 1, t.getScore(),
                    t.getAppeal(), openish ? Math.max(0, (nowTs - t.getCreated()) / 60000L) : 0));
            orders++;
            if (t.getOnTime() != null && t.getOnTime() == 1) {
                onTimeOrders++;
            }
        }

        int diaryDays = (int) diaries.stream().filter(DiaryVO::bothIn).count();
        int nominations = awards.size();
        GalaVO galaVO = new GalaVO(today, CoupleTheaterBank.galaPrize(
                        CoupleRitualBank.stableHash(space.getId() + "|gala|" + today)),
                CoupleTheaterBank.galaLine(CoupleRitualBank.stableHash(space.getId() + "|gala|" + today), nominations),
                nominations, refs.size(), quizzed, rights, diaryDays, onTimeOrders, orders);

        return new TheaterVO(today, week, roleVO, diaries, masterVO, booths, refs, awards, familyVO,
                movies, tickets, galaVO);
    }

    // ========== 懒建行 ==========

    private CoupleRoleDay ensureRole(CoupleSpace space, String day) {
        CoupleRoleDay row = roleMapper.findByDay(space.getId(), day);
        if (row != null) {
            return row;
        }
        String[] picked = CoupleTheaterBank.roleOfToday(
                CoupleRitualBank.stableHash(space.getId() + "|role|" + day));
        CoupleRoleDay fresh = CoupleRoleDay.of(space.getId(), day, picked[0], picked[1]);
        roleMapper.insert(fresh);
        return fresh;
    }

    private CoupleMasterDay ensureMaster(CoupleSpace space, LocalDate now) {
        String week = now.with(DayOfWeek.MONDAY).toString();
        CoupleMasterDay row = masterMapper.findByWeek(space.getId(), week);
        if (row != null) {
            return row;
        }
        boolean masterIsA = CoupleRitualBank.stableHash(space.getId() + "|master|" + week) % 2 == 0;
        String masterUser = masterIsA ? space.getUserA() : space.getUserB();
        String apprentice = masterIsA ? space.getUserB() : space.getUserA();
        CoupleMasterDay fresh = CoupleMasterDay.of(space.getId(), week, masterUser, apprentice);
        masterMapper.insert(fresh);
        return fresh;
    }

    private CoupleIfFamily ensureFamily(CoupleSpace space, String day) {
        CoupleIfFamily row = familyMapper.findByDay(space.getId(), day);
        if (row != null) {
            return row;
        }
        String q = CoupleTheaterBank.familyQuestionOfToday(
                CoupleRitualBank.stableHash(space.getId() + "|family|" + day));
        CoupleIfFamily fresh = CoupleIfFamily.of(space.getId(), day, q);
        familyMapper.insert(fresh);
        return fresh;
    }

    // ========== 校验小件 ==========

    private CouplePrivateRef requireRef(CoupleSpace space, String term) {
        String tm = trim(term, "要抽查哪个词条");
        CouplePrivateRef row = refMapper.findByTerm(space.getId(), tm);
        if (row == null) {
            throw new BusinessException(400, "大全里查不到这个词条");
        }
        return row;
    }

    private CoupleServiceTicket requireTicket(CoupleSpace space, String id) {
        CoupleServiceTicket t = id == null || id.isBlank() ? null : ticketMapper.selectById(id);
        if (t == null || !space.getId().equals(t.getSpaceId())) {
            throw new BusinessException(400, "这张工单不存在");
        }
        return t;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
