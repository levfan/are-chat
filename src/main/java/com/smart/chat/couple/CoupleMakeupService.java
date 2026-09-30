package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 和好系列（F61/F62）：矛盾复盘与道歉券。
 * 情绪价值设计：吵架不可怕，可怕的是不了了之——复盘让每次矛盾都变成一次了解，
 * 道歉券把「对不起」变成可以递出去的实体台阶。
 */
@Service
public class CoupleMakeupService {

    /** 每人同时有效的道歉券上限。 */
    public static final int ACTIVE_SORRY_MAX = 2;

    private final CoupleSpaceMapper spaceMapper;
    private final CouplePeaceReviewMapper reviewMapper;
    private final CoupleSorryTicketMapper sorryMapper;
    private final ImPushService push;

    public CoupleMakeupService(CoupleSpaceMapper spaceMapper, CouplePeaceReviewMapper reviewMapper,
                               CoupleSorryTicketMapper sorryMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.reviewMapper = reviewMapper;
        this.sorryMapper = sorryMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record PeaceReviewVO(String id, String byUser, String day, String myPart, String nextTime, Long created) {
    }

    /** 某天的复盘：一方或双方（锦囊要双方都写完才算完整）。 */
    public record PeaceDayVO(String day, PeaceReviewVO mine, PeaceReviewVO partner, boolean complete) {
    }

    public record SorryTicketVO(String id, String fromUser, String note, String status,
                                String usedNote, Long usedAt, Long created) {
    }

    // ========== F61 矛盾复盘 ==========

    /** 复盘列表：按天聚合（新→旧），含今天未完成的锦囊。 */
    public List<PeaceDayVO> reviews(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        List<CouplePeaceReview> all = reviewMapper.findBySpace(space.getId());
        Map<String, List<CouplePeaceReview>> byDay = all.stream()
                .collect(Collectors.groupingBy(CouplePeaceReview::getDay, java.util.LinkedHashMap::new, Collectors.toList()));
        return byDay.entrySet().stream()
                .map(entry -> {
                    List<CouplePeaceReview> rows = entry.getValue();
                    PeaceReviewVO mine = rows.stream().filter(r -> r.getByUser().equals(me)).findFirst()
                            .map(r -> toReviewVO(r)).orElse(null);
                    PeaceReviewVO theirs = rows.stream().filter(r -> r.getByUser().equals(partner)).findFirst()
                            .map(r -> toReviewVO(r)).orElse(null);
                    return new PeaceDayVO(entry.getKey(), mine, theirs, mine != null && theirs != null);
                })
                .toList();
    }

    /** 写下今天的复盘（每人每天一份，可修改）；双方都写完时合成「和好锦囊」并庆祝。 */
    public List<PeaceDayVO> saveReview(String me, String myPart, String nextTime) {
        if (myPart == null || myPart.isBlank() || nextTime == null || nextTime.isBlank()) {
            throw new BusinessException(400, "「我在意什么」和「下次怎么做」都要写哦");
        }
        if (myPart.length() > CouplePeaceReview.PART_MAX || nextTime.length() > CouplePeaceReview.PART_MAX) {
            throw new BusinessException(400, "每段最多 " + CouplePeaceReview.PART_MAX + " 字");
        }
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        List<CouplePeaceReview> todayRows = reviewMapper.findByDay(space.getId(), today);
        CouplePeaceReview mine = todayRows.stream().filter(r -> r.getByUser().equals(me)).findFirst().orElse(null);
        if (mine == null) {
            mine = CouplePeaceReview.of(space.getId(), me, myPart.trim(), nextTime.trim());
            reviewMapper.insert(mine);
        } else {
            mine.setMyPart(myPart.trim());
            mine.setNextTime(nextTime.trim());
            reviewMapper.updateById(mine);
        }
        boolean bothDone = todayRows.stream().anyMatch(r -> !r.getByUser().equals(me));
        if (bothDone) {
            push.pushCoupleEventBoth("peace-review-done", me, space.getUserA(), space.getUserB(),
                    "🕊️ 今天的「和好锦囊」已合成！两边的心里话都到齐了，去翻翻看");
        } else {
            push.pushCoupleEvent("peace-review-kept", me, space.partnerOf(me),
                    "📝 TA 写好了今天的复盘，等你的一起合成「和好锦囊」");
        }
        return reviews(me);
    }

    // ========== F62 道歉券 ==========

    public List<SorryTicketVO> sorryTickets(String me) {
        CoupleSpace space = requireSpace(me);
        return sorryMapper.findBySpace(space.getId()).stream()
                .map(t -> toTicketVO(t))
                .toList();
    }

    /** 递一张道歉券：附言写清对不起的原因；同时最多 2 张有效券。 */
    public List<SorryTicketVO> sendSorry(String me, String note) {
        if (note == null || note.isBlank() || note.length() > CoupleSorryTicket.NOTE_MAX) {
            throw new BusinessException(400, "道歉附言要写清楚（" + CoupleSorryTicket.NOTE_MAX + " 字内）");
        }
        CoupleSpace space = requireSpace(me);
        if (sorryMapper.findActiveByUser(space.getId(), me).size() >= ACTIVE_SORRY_MAX) {
            throw new BusinessException(400, "你还有 " + ACTIVE_SORRY_MAX + " 张道歉券没被收下，诚意要一张一张给");
        }
        CoupleSorryTicket ticket = CoupleSorryTicket.of(space.getId(), me, note.trim());
        sorryMapper.insert(ticket);
        push.pushCoupleEvent("sorry-received", me, space.partnerOf(me),
                "🎫 TA 递给你一张「对不起券」：" + note.trim() + " 收下这个台阶吧");
        return sorryTickets(me);
    }

    /** 收下道歉券：只有对方能收；可以附一句回话。 */
    public List<SorryTicketVO> useSorry(String me, String id, String usedNote) {
        CoupleSpace space = requireSpace(me);
        CoupleSorryTicket ticket = sorryMapper.selectById(id);
        if (ticket == null || !ticket.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这张道歉券哦");
        }
        if (ticket.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己递的券要等 TA 收哦");
        }
        if (CoupleSorryTicket.STATUS_ACTIVE.equals(ticket.getStatus())) {
            ticket.setStatus(CoupleSorryTicket.STATUS_USED);
            ticket.setUsedNote(usedNote == null || usedNote.isBlank() ? null : usedNote.trim());
            ticket.setUsedAt(System.currentTimeMillis());
            sorryMapper.updateById(ticket);
            String tail = ticket.getUsedNote() == null ? "" : " TA 还说：「" + ticket.getUsedNote() + "」";
            push.pushCoupleEvent("sorry-used", me, ticket.getFromUser(),
                    "🫶 TA 收下了你的道歉券，这个跟头没白摔。" + tail);
        }
        return sorryTickets(me);
    }

    // ========== 内部工具 ==========

    private PeaceReviewVO toReviewVO(CouplePeaceReview r) {
        return new PeaceReviewVO(r.getId(), r.getByUser(), r.getDay(), r.getMyPart(), r.getNextTime(), r.getCreated());
    }

    private SorryTicketVO toTicketVO(CoupleSorryTicket t) {
        return new SorryTicketVO(t.getId(), t.getFromUser(), t.getNote(), t.getStatus(), t.getUsedNote(), t.getUsedAt(), t.getCreated());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
