package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleCatchBank;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordUsePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordUseMapper;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.safeword.Safeword;
import com.smart.chat.couple.domain.safeword.SafewordUse;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 安全词与暂停复盘（保留卡 `couple-catch-safeword`，原 F382）。
 * 情绪价值设计：吵架时有一个可以喊的停——喊停要留痕、事后要能补一句复盘，
 * 而不是把「算了」当和解。
 *
 * 系统裁剪：暗中心愿本、雷区、敏感日历、话头存档、反话词典、聆听协议、话题池、
 * 今日一句话、年报全部下线，本类只剩安全词一条链路。
 */
import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;
@Service
public class CoupleCatchService {

    private final CoupleSpaceRepository spaceRepository;
    private final CoupleCatchSafewordMapper safewordMapper;
    private final CoupleCatchSafewordUseMapper useMapper;
    private final CoupleEventPublisher push;

    private static final int LIST_USE = 20;

    public CoupleCatchService(CoupleSpaceRepository spaceRepository, CoupleCatchSafewordMapper safewordMapper,
                              CoupleCatchSafewordUseMapper useMapper, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.safewordMapper = safewordMapper;
        this.useMapper = useMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** 某人的安全词 + TA 喊过几次停。 */
    public record SafewordVO(String id, boolean mine, String word, String note, int useCount) {
    }

    /** 一次暂停使用（reflect 为空表示还没复盘）。 */
    public record UseVO(String id, String day, boolean mine, String word, String reflect) {
    }

    /** 安全词看板：写接口原样返回整份。 */
    public record CatchVO(String day, String week, SafewordVO myWord, SafewordVO partnerWord,
                          List<UseVO> uses, int monthUses, boolean usedTodayMine, boolean usedTodayPartner) {
    }

    // ========== 读 ==========

    /** 看板（GET /board）。 */
    public CatchVO board(String me) {
        return build(requireSpace(me), me, LocalDate.now());
    }

    // ========== 安全词 ==========

    /** 约定/改写自己的安全词（每人一格，word ≤20、note ≤60）。 */
    public CatchVO setSafeword(String me, String word, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        Safeword agreed = rule(() -> Safeword.agree(word, note));
        String w = agreed.word();
        String n = agreed.note();
        CoupleCatchSafewordPO row = safewordMapper.find(space.id(), me);
        if (row == null) {
            safewordMapper.insert(CoupleCatchSafewordPO.of(space.id(), me, w, n));
        } else {
            row.setWord(w);
            row.setNote(n);
            row.setUpdatedAt(System.currentTimeMillis());
            safewordMapper.updateById(row);
        }
        push.pushCoupleEvent("catch-safeword", me, space.partnerOf(me), CoupleCatchBank.safewordSetLine(w, n));
        return build(space, me, now);
    }

    /** 喊了一次暂停（一天一人只记一次，复盘可后补）。 */
    public CatchVO useSafeword(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchSafewordPO mine = safewordMapper.find(space.id(), me);
        guard(() -> Safeword.requireAgreed(mine == null ? null : Safeword.agree(mine.getWord(), mine.getNote())));
        String day = now.toString();
        CoupleCatchSafewordUsePO todayRow = useMapper.find(space.id(), day, me);
        guard(() -> SafewordUse.assertNotAlreadyToday(todayRow == null ? null : SafewordUse.restore(
                todayRow.getId(), todayRow.getDay(), todayRow.getUserName(), todayRow.getReflect())));
        useMapper.insert(CoupleCatchSafewordUsePO.of(space.id(), day, me));
        push.pushCoupleEvent("catch-safeword-use", me, space.partnerOf(me),
                CoupleCatchBank.safewordUseLine(nz(mine.getWord()), me));
        return build(space, me, now);
    }

    /** 事后补一句复盘（只有喊停本人能补自己那天的记录）。 */
    public CatchVO reflectUse(String me, String id, String reflect) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchSafewordUsePO use = requireUse(space, id);
        SafewordUse pause = SafewordUse.restore(use.getId(), use.getDay(), use.getUserName(), use.getReflect());
        guard(() -> pause.reflectBy(me, reflect));
        use.setReflect(pause.reflect());
        use.setUpdatedAt(System.currentTimeMillis());
        useMapper.updateById(use);
        push.pushCoupleEventBoth("catch-safeword-reflect", me, space.userA(), space.userB(),
                CoupleCatchBank.safewordReflectLine(pause.reflect()));
        return build(space, me, now);
    }

    // ========== 聚合 ==========

    private CatchVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        String week = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();

        List<CoupleCatchSafewordPO> words = safewordMapper.findBySpace(space.id());
        CoupleCatchSafewordPO myRow = words.stream().filter(w -> me.equals(w.getFromUser())).findFirst().orElse(null);
        CoupleCatchSafewordPO partnerRow = words.stream().filter(w -> !me.equals(w.getFromUser())).findFirst().orElse(null);
        List<CoupleCatchSafewordUsePO> useRows = useMapper.findBySpace(space.id());
        String myWordText = myRow == null ? "" : nz(myRow.getWord());
        String partnerWordText = partnerRow == null ? "" : nz(partnerRow.getWord());

        SafewordVO myWord = myRow == null ? null : new SafewordVO(myRow.getId(), true, myWordText,
                nz(myRow.getNote()), countBy(useRows, me));
        SafewordVO partnerWord = partnerRow == null ? null : new SafewordVO(partnerRow.getId(), false,
                partnerWordText, nz(partnerRow.getNote()), countBy(useRows, partnerRow.getFromUser()));

        List<UseVO> uses = useRows.stream().limit(LIST_USE)
                .map(u -> new UseVO(u.getId(), nz(u.getDay()), me.equals(u.getUserName()),
                        me.equals(u.getUserName()) ? myWordText : partnerWordText, nz(u.getReflect())))
                .toList();
        int monthUses = (int) useRows.stream()
                .filter(u -> nz(u.getDay()).startsWith(day.substring(0, 7))).count();
        boolean usedTodayMine = useRows.stream()
                .anyMatch(u -> day.equals(nz(u.getDay())) && me.equals(u.getUserName()));
        boolean usedTodayPartner = useRows.stream()
                .anyMatch(u -> day.equals(nz(u.getDay())) && !me.equals(u.getUserName()));

        return new CatchVO(day, week, myWord, partnerWord, uses, monthUses, usedTodayMine, usedTodayPartner);
    }

    private int countBy(List<CoupleCatchSafewordUsePO> rows, String user) {
        return (int) rows.stream().filter(u -> user != null && user.equals(u.getUserName())).count();
    }

    // ========== 取行与校验 ==========

    private CoupleCatchSafewordUsePO requireUse(CoupleSpace space, String id) {
        CoupleCatchSafewordUsePO row = id == null || id.isBlank() ? null : useMapper.selectById(id);
        if (row == null || !space.id().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到那次暂停记录 🛑");
        }
        return row;
    }

    private String limit(String text, int max, String what) {
        String t = text == null ? "" : text.trim();
        if (t.length() > max) {
            throw new BusinessException(400, what + "最多 " + max + " 字");
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

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
