package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.deed.Deed;
import com.smart.chat.couple.domain.deed.DeedRepository;
import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import com.smart.chat.couple.infrastructure.content.CoupleEchoBank;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 好事簿（保留卡 `couple-echo-deed`，原 F351/F352）：写下「TA 为我做的事」，
 * 这条记录既是「我被爱着」的证据，也是积分的第一个进水口——分给做事的那个人。
 *
 * 系统裁剪：鼓励语罐、能量补给、感谢慢递、高光重放、夸夸回执、电量预报、
 * 写给低落的自己、被爱日历、回音壁年报全部下线。
 */
@Service
public class CoupleEchoService {

    /** 一侧列表最多给多少条。 */
    static final int DEED_PAGE = 30;
    /** 记一笔好事给被记的那位 +2。 */
    static final int DEED_POINTS = 2;
    /** 被加星再 +1。 */
    static final int DEED_STAR_POINTS = 1;
    static final String DEED_REASON_PREFIX = "好事簿：";
    static final String DEED_STAR_REASON_PREFIX = "好事簿被加星：";

    private final CoupleSpaceRepository spaceRepository;
    private final DeedRepository deedRepository;
    private final PointLedgerRepository ledgerRepository;
    private final CoupleEventPublisher push;

    public CoupleEchoService(CoupleSpaceRepository spaceRepository, DeedRepository deedRepository,
                             PointLedgerRepository ledgerRepository, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.deedRepository = deedRepository;
        this.ledgerRepository = ledgerRepository;
        this.push = push;
    }

    // ========== VO ==========

    public record DeedVO(String id, String fromUser, boolean mine, String content, String day,
                         boolean starred, long created) {
    }

    /** 好事簿看板：写接口全部原样返回这份聚合，前端整体替换。 */
    public record EchoVO(String day, List<DeedVO> deeds, List<DeedVO> partnerDeeds,
                         int mineCount, int partnerCount) {
    }

    // ========== 读 ==========

    /** 好事簿看板（GET /vault）。 */
    public EchoVO vault(String me) {
        return build(requireSpace(me), me, LocalDate.now());
    }

    // ========== 写 ==========

    /** 记一件「TA 为我做的事」（同日同人同内容重复 400；新增推双方，并给被记的那位记一笔分）。 */
    public EchoVO addDeed(String me, String content, String day) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        Deed deed = rule(() -> Deed.record(space.id(), me, content, day, now));
        if (deedRepository.alreadyRecorded(space.id(), me, deed.day(), deed.content())) {
            throw new BusinessException(400, "这条已经记过了");
        }
        deedRepository.save(deed);
        // 写的人是「被照顾的那个」，分要给做事的那个人
        earn(space, space.partnerOf(me), DEED_REASON_PREFIX + deed.content(), DEED_POINTS);
        push.pushCoupleEventBoth("echo-deed-added", me, space.userA(), space.userB(),
                CoupleEchoBank.deedAddedLine(deed.content()));
        return build(space, me, now);
    }

    /** 记录人本人给证据点「这条救过我」（幂等；TA 的记录只能 TA 自己点；分归被记的那位 +1）。 */
    public EchoVO starDeed(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        Deed deed = deedRepository.findById(id == null || id.isBlank() ? "" : id.trim())
                .filter(d -> space.id().equals(d.spaceId()))
                .orElseThrow(() -> new BusinessException(400, "这条不在好事簿里"));
        // 归属闸门与幂等都在 Deed.starBy 里：不是记录人抛原话，已经点过返回 false
        boolean lit = rule(() -> deed.starBy(me));
        if (!lit) {
            return build(space, me, now);
        }
        deedRepository.save(deed);
        earn(space, space.partnerOf(deed.fromUser()), DEED_STAR_REASON_PREFIX + deed.content(),
                DEED_STAR_POINTS);
        push.pushCoupleEventBoth("echo-deed-starred", me, space.userA(), space.userB(),
                CoupleEchoBank.deedStarredLine(deed.content()));
        return build(space, me, now);
    }

    // ========== 聚合 ==========

    private EchoVO build(CoupleSpace space, String me, LocalDate now) {
        String partner = space.partnerOf(me);
        List<Deed> mine = deedRepository.listByRecorder(space.id(), me);
        List<Deed> theirs = deedRepository.listByRecorder(space.id(), partner);
        return new EchoVO(now.toString(),
                mine.stream().limit(DEED_PAGE).map(d -> toDeed(d, me)).toList(),
                theirs.stream().limit(DEED_PAGE).map(d -> toDeed(d, me)).toList(),
                mine.size(), theirs.size());
    }

    /** 记一笔赚分。归属人由调用点决定，重复计分的闸门在各写方法的早退里。 */
    private void earn(CoupleSpace space, String user, String item, int points) {
        ledgerRepository.append(PointEntry.earn(space.id(), user, item, points));
    }

    private DeedVO toDeed(Deed d, String me) {
        return new DeedVO(d.id(), d.fromUser(), d.fromUser().equals(me),
                nz(d.content()), nz(d.day()), d.starred(), d.created());
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
