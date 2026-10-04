package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleEchoBank;
import com.smart.chat.couple.infrastructure.persistence.CoupleEchoDeedPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleEchoDeedMapper;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerPO;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
    private final CoupleEchoDeedMapper deedMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final CoupleEventPublisher push;

    public CoupleEchoService(CoupleSpaceRepository spaceRepository, CoupleEchoDeedMapper deedMapper,
                             CouplePointLedgerMapper ledgerMapper, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.deedMapper = deedMapper;
        this.ledgerMapper = ledgerMapper;
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
        String text = trim(content, "好事总得写一句");
        if (text.length() > CoupleEchoDeedPO.CONTENT_MAX) {
            throw new BusinessException(400, "一件好事最多 " + CoupleEchoDeedPO.CONTENT_MAX + " 字");
        }
        String d = day == null || day.isBlank() ? now.toString() : day.trim();
        if (!d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BusinessException(400, "日期写成 yyyy-MM-dd");
        }
        if (deedMapper.findByDayContent(space.id(), me, d, text) != null) {
            throw new BusinessException(400, "这条已经记过了");
        }
        deedMapper.insert(CoupleEchoDeedPO.of(space.id(), me, text, d));
        // 写的人是「被照顾的那个」，分要给做事的那个人
        earn(space, space.partnerOf(me), DEED_REASON_PREFIX + text, DEED_POINTS);
        push.pushCoupleEventBoth("echo-deed-added", me, space.userA(), space.userB(),
                CoupleEchoBank.deedAddedLine(text));
        return build(space, me, now);
    }

    /** 记录人本人给证据点「这条救过我」（幂等；TA 的记录只能 TA 自己点；分归被记的那位 +1）。 */
    public EchoVO starDeed(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleEchoDeedPO row = id == null || id.isBlank() ? null : deedMapper.selectById(id.trim());
        if (row == null || !space.id().equals(row.getSpaceId())) {
            throw new BusinessException(400, "这条不在好事簿里");
        }
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "只有记下这条的人能加星");
        }
        if (row.starredFlag()) {
            return build(space, me, now);
        }
        row.setStarred(1);
        row.setUpdatedAt(System.currentTimeMillis());
        deedMapper.updateById(row);
        earn(space, space.partnerOf(row.getFromUser()), DEED_STAR_REASON_PREFIX + row.getContent(),
                DEED_STAR_POINTS);
        push.pushCoupleEventBoth("echo-deed-starred", me, space.userA(), space.userB(),
                CoupleEchoBank.deedStarredLine(row.getContent()));
        return build(space, me, now);
    }

    // ========== 聚合 ==========

    private EchoVO build(CoupleSpace space, String me, LocalDate now) {
        String partner = space.partnerOf(me);
        List<CoupleEchoDeedPO> mine = deedMapper.findByUser(space.id(), me);
        List<CoupleEchoDeedPO> theirs = deedMapper.findByUser(space.id(), partner);
        return new EchoVO(now.toString(),
                mine.stream().limit(DEED_PAGE).map(d -> toDeed(d, me)).toList(),
                theirs.stream().limit(DEED_PAGE).map(d -> toDeed(d, me)).toList(),
                mine.size(), theirs.size());
    }

    /** 记一笔赚分。归属人由调用点决定，重复计分的闸门在各写方法的早退里。 */
    private void earn(CoupleSpace space, String user, String item, int points) {
        ledgerMapper.insert(CouplePointLedgerPO.of(space.id(), user,
                CouplePointLedgerPO.TYPE_EARN, item, points));
    }

    private DeedVO toDeed(CoupleEchoDeedPO d, String me) {
        return new DeedVO(d.getId(), d.getFromUser(), d.getFromUser().equals(me),
                nz(d.getContent()), nz(d.getDay()), d.starredFlag(), d.getCreated() == null ? 0 : d.getCreated());
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
