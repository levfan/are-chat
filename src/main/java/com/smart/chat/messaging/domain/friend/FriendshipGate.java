package com.smart.chat.messaging.domain.friend;

import com.smart.chat.messaging.domain.RuleViolation;

import java.util.Optional;

/**
 * 关系与会话的入场闸门：纯规则、无状态（见 docs/ddd/05-tactical-playbook.md 第 2.2 条）。
 * <p>
 * 「能不能开始 / 继续这段会话」的裁决集中在这里，因为它们的<b>文案与 code 都是对外契约</b>
 * （403 越权、409 状态冲突、400 输入不合规，GlobalExceptionHandler 直接拿 code 当 HTTP 状态码）。
 * 改造前这些 if 散在两个 Service 里，同一句「还不是好友」在不同入口有三个版本，
 * 改一处就会漏另一处——尤其「拉黑双向拦截」必须两侧各判一次。
 * <p>
 * 取数（谁和谁的边、有没有待处理申请）仍然归 application：闸门只吃已经查好的事实。
 */
public final class FriendshipGate {

    private FriendshipGate() {
    }

    /**
     * 加好友：对方必须是注册过的合法账号。文案比私信那条长——这里是「按手机号或用户名找人」的入口，
     * 需要把两种输法都提示到，沿用改造前的两版文案，不合并。
     */
    public static void requireKnownAccountToApply(boolean exists) {
        if (!exists) {
            throw RuleViolation.of("查无此人：对方还没用手机号注册，或手机号/用户名输错了");
        }
    }

    /** 发私信：对方必须是注册过的合法账号。 */
    public static void requireKnownAccountToMail(boolean exists) {
        if (!exists) {
            throw RuleViolation.of("查无此人：对方还没有用手机号注册");
        }
    }

    /** 发私信：得先有个「对方」。 */
    public static void requireMessagingPartner(String me, String peer) {
        if (me.equals(peer)) {
            throw RuleViolation.of("不能给自己发私信");
        }
    }

    /** 发申请前三查：已经是好友、我已经发过一张、对方先向我发了一张。 */
    public static void requireApplyPossible(boolean alreadyFriends, boolean pendingOut, boolean pendingIn) {
        if (alreadyFriends) {
            throw RuleViolation.conflict("你们已经是好友了");
        }
        if (pendingOut) {
            throw RuleViolation.conflict("好友申请已发送，等对方处理吧");
        }
        if (pendingIn) {
            throw RuleViolation.conflict("对方已经先向你发起了申请，去「好友申请」处理吧");
        }
    }

    /** 发私信：要求关系存在，且两个方向的拉黑位都没落下。 */
    public static void requireMessagingAllowed(Optional<Friend> mine, Optional<Friend> theirs) {
        if (mine.isEmpty()) {
            throw RuleViolation.forbidden("还不是好友，先加个好友吧");
        }
        if (mine.get().blockedFlag()) {
            throw RuleViolation.forbidden("已拉黑对方，解除后才能发消息");
        }
        if (theirs.isPresent() && theirs.get().blockedFlag()) {
            throw RuleViolation.forbidden("对方已将你拉黑");
        }
    }

    /** 会话级操作（置顶/取消置顶/清空/附件）：只看关系在不在，不看拉黑——被拉黑的一方仍要能收尾自己的会话。 */
    public static void requireConversationUsable(Optional<Friend> mine) {
        if (mine.isEmpty()) {
            throw RuleViolation.forbidden("还不是好友，无法操作会话");
        }
    }

    /** 资料卡：昵称/签名/头像只对好友可见，陌生人来查同样是 403（不透露这个人存在与否）。 */
    public static void requireProfileCardVisible(boolean areFriends) {
        if (!areFriends) {
            throw RuleViolation.forbidden("只有好友才能查看资料卡");
        }
    }
}
