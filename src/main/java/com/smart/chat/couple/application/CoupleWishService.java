package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.wish.Wish;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.infrastructure.persistence.CoupleWishMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleWishPO;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 愿望清单：双方互相添加想要的东西，对方可以<b>偷偷标记「已准备」</b>。
 * <p>
 * 「偷偷」是这功能的全部难点，所以它落在领域里：{@link Wish#visibleStatusFor} 会把
 * PREPARED 对许愿人本人显示成 OPEN，Service 拼装 VO 只能走这个方法；
 * 并且<b>标记动作一个 WS 事件都不推</b>——推了就等于替对方把惊喜说出去了。
 * 只有许愿人自己点「我收到了」（FULFILLED）之后，双方才都看得到结果。
 */
@Service
public class CoupleWishService {

    /** 一个空间同时挂着的愿望上限（不限就有人抄一屏幕） */
    private static final int OPEN_MAX = 30;

    // ========== VO ==========

    public record WishVO(String id, String ownerUser, String creatorUser, String title, String note,
                         String status, boolean mineFlag, boolean preparedFlag, boolean preparableFlag,
                         boolean canFulfillFlag, Long preparedAt, Long fulfilledAt, Long created) {
    }

    public record WishBoardVO(List<WishVO> open, List<WishVO> prepared, List<WishVO> fulfilled,
                              int openCount, int limit, int titleMax, int noteMax) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final CoupleWishMapper wishMapper;
    private final CoupleEventPublisher push;

    public CoupleWishService(CoupleSpaceRepository spaceRepository, CoupleWishMapper wishMapper,
                             CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.wishMapper = wishMapper;
        this.push = push;
    }

    // ========== 读 ==========

    /** 愿望清单：未实现的在最前，「已准备」的分组只对标记人自己亮着。 */
    public WishBoardVO board(String me) {
        CoupleSpace space = requireSpace(me);
        List<WishVO> open = new ArrayList<>();
        List<WishVO> prepared = new ArrayList<>();
        List<WishVO> fulfilled = new ArrayList<>();
        int openTotal = 0;
        for (CoupleWishPO row : wishMapper.findBySpace(space.id())) {
            Wish wish = toDomain(row);
            String visible = wish.visibleStatusFor(me);
            if (Wish.STATUS_FULFILLED.equals(visible)) {
                fulfilled.add(toVO(wish, row, me, true));
                continue;
            }
            if (Wish.STATUS_PREPARED.equals(visible)) {
                // 只有标记人自己看得到这一组；许愿人那里它还是 OPEN
                prepared.add(toVO(wish, row, me, false));
                openTotal++;
                continue;
            }
            open.add(toVO(wish, row, me, false));
            openTotal++;
        }
        return new WishBoardVO(open, prepared, fulfilled, openTotal, OPEN_MAX, Wish.TITLE_MAX, Wish.NOTE_MAX);
    }

    // ========== 写 ==========

    /** 添加一条愿望：owner 传空表示给自己许；同名查重、上限与字数闸门都在领域与这里收口。 */
    public WishBoardVO add(String me, String title, String note, String ownerUser) {
        CoupleSpace space = requireSpace(me);
        String owner = ownerUser == null || ownerUser.isBlank() ? me : ownerUser.trim();
        if (!owner.equals(me) && !owner.equals(space.partnerOf(me))) {
            throw new BusinessException(400, "愿望只能许给自己或者你们的另一半");
        }
        Wish wish = rule(() -> Wish.add(owner, me, title, note));
        if (openCountOf(space.id()) >= OPEN_MAX) {
            throw new BusinessException(400, "愿望清单最多同时挂 " + OPEN_MAX + " 条，先实现几条再加吧");
        }
        if (wishMapper.existsSameTitle(space.id(), owner, wish.title())) {
            throw new BusinessException(400, "这条愿望已经在清单上了，别再写一遍啦");
        }
        CoupleWishPO row = CoupleWishPO.of(space.id(), owner, me, wish.title(), wish.note());
        wishMapper.insert(row);
        if (!owner.equals(me)) {
            push.pushCoupleEvent("wish-added", me, owner,
                    "TA 帮你把「" + row.getTitle() + "」记进愿望清单了 📝");
        }
        return board(me);
    }

    /** 偷偷标记「已准备」：只有对方能标自己许的愿；不推任何事件给许愿人。 */
    public WishBoardVO prepare(String me, String id) {
        return changeStatus(me, id, true);
    }

    /** 撤销「已准备」：只有当初点的人能撤。 */
    public WishBoardVO unprepare(String me, String id) {
        return changeStatus(me, id, false);
    }

    /** 许愿人确认实现：这时才公开，双方都看得见。 */
    public WishBoardVO fulfill(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleWishPO row = requireWish(space, id);
        Wish wish = toDomain(row);
        guard(() -> wish.fulfillBy(me, System.currentTimeMillis()));
        row.setStatus(wish.status());
        row.setFulfilledAt(wish.fulfilledAt());
        row.setUpdatedAt(System.currentTimeMillis());
        wishMapper.updateById(row);
        push.pushCoupleEvent("wish-fulfilled", me, space.partnerOf(me),
                "你许的「" + row.getTitle() + "」实现啦 🎉 谢谢 TA");
        return board(me);
    }

    /** 改补充说明：只有记录人能改。 */
    public WishBoardVO updateNote(String me, String id, String note) {
        CoupleSpace space = requireSpace(me);
        CoupleWishPO row = requireWish(space, id);
        Wish wish = toDomain(row);
        guard(() -> wish.editNote(me, note));
        row.setNote(wish.note());
        row.setUpdatedAt(System.currentTimeMillis());
        wishMapper.updateById(row);
        return board(me);
    }

    /** 删除：只有记录人能删，已实现的删不掉。 */
    public WishBoardVO remove(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleWishPO row = requireWish(space, id);
        guard(() -> toDomain(row).requireDeletableBy(me));
        wishMapper.deleteById(row.getId());
        return board(me);
    }

    // ========== 供其它服务只读复用 ==========

    /** 已实现的愿望条数（百日回顾用）。 */
    public long fulfilledCount(String spaceId) {
        return wishMapper.countFulfilled(spaceId);
    }

    // ========== 内部 ==========

    private WishBoardVO changeStatus(String me, String id, boolean prepare) {
        CoupleSpace space = requireSpace(me);
        CoupleWishPO row = requireWish(space, id);
        Wish wish = toDomain(row);
        if (prepare) {
            guard(() -> wish.prepareBy(me, System.currentTimeMillis()));
        } else {
            guard(() -> wish.unprepareBy(me));
        }
        row.setStatus(wish.status());
        row.setPreparedBy(wish.preparedBy());
        row.setPreparedAt(wish.preparedAt());
        row.setUpdatedAt(System.currentTimeMillis());
        wishMapper.updateById(row);
        // 刻意不推送：标记「已准备」的意义就是别让许愿的人提前知道
        return board(me);
    }

    private long openCountOf(String spaceId) {
        long count = 0;
        for (CoupleWishPO row : wishMapper.findBySpace(spaceId)) {
            if (!CoupleWishPO.STATUS_FULFILLED.equals(row.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private WishVO toVO(Wish wish, CoupleWishPO row, String me, boolean fulfilled) {
        String visible = wish.visibleStatusFor(me);
        boolean mine = wish.ownerUser().equals(me);
        // 已准备的时间只给标记人看，许愿人那边连时刻也不能露
        boolean keepsSecret = wish.keepsPreparationSecretFrom(me);
        return new WishVO(row.getId(), wish.ownerUser(), wish.creatorUser(), wish.title(), wish.note(), visible,
                mine, !keepsSecret && wish.preparedFlag(), !mine && !fulfilled && !wish.preparedFlag(),
                mine && !fulfilled, keepsSecret ? null : row.getPreparedAt(), row.getFulfilledAt(), row.getCreated());
    }

    private CoupleWishPO requireWish(CoupleSpace space, String id) {
        CoupleWishPO row = id == null ? null : wishMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.id())) {
            throw new BusinessException(404, "这条愿望不在你们的清单里");
        }
        return row;
    }

    private Wish toDomain(CoupleWishPO row) {
        return Wish.restore(row.getId(), row.getOwnerUser(), row.getCreatorUser(), row.getTitle(), row.getNote(),
                row.getStatus(), row.getPreparedBy(), row.getPreparedAt(), row.getFulfilledAt());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
