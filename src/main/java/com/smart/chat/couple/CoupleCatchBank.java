package com.smart.chat.couple;

import java.util.List;

/**
 * 聆听者内容库（批次三十四 F380-F389）。静态内容只增不改顺序。
 * 口径：爱是「你随口一说，我一直记得」——暗中心愿、雷区预警、安全词、敏感日历、
 * 说到哪了、真话翻译、聆听方式、话题许愿池、今日一句话、聆听者年报。
 * 文案按 seed（stableHash）稳定取值，无副作用。
 */
public final class CoupleCatchBank {

    private CoupleCatchBank() {
    }

    // ========== 字典 ==========

    // ========== F380 暗中心愿 ==========

    /** F380 记下（只推记账人自己，绝不推给对方——这是保密的）。 */
    public static String wishSavedLine(String content) {
        return "🤫 已悄悄记下：TA 想要「" + content + "」。这句话只有你看得见。";
    }

    /** F380 兑现即揭晓（推心愿主人）。 */
    public static String wishFulfilledLine(String content, String sourceDay, String scene) {
        String tail = scene == null || scene.isBlank() ? "" : "，当时在" + scene;
        return "🎁 揭晓：原来你 " + sourceDay + " 说过想要「" + content + "」" + tail + "。有人一直记着。";
    }

    // ========== F381 雷区 ==========

    /** F381 挂雷（推对方：先告诉你，别踩）。 */
    public static String minePlantedLine(String topic) {
        return "💣 TA 挂了一颗雷：「" + topic + "」。去盖个「已知晓」，以后绕着走。";
    }

    /** F381 知晓盖章（推挂雷的人）。 */
    public static String mineAckLine(String topic) {
        return "✅ 「" + topic + "」这颗雷对方已经知晓，踩雷的误会少一个。";
    }

    /** F381 记一次成功避雷。 */
    public static String mineAvoidLine(String topic) {
        return "🛡️ 「" + topic + "」这次绕过去了，记一功。";
    }

    // ========== F382 安全词 ==========

    /** F382 约定/改写安全词（推对方）。 */
    public static String safewordSetLine(String word, String note) {
        String tail = note == null || note.isBlank() ? "" : "，用了之后希望：" + note;
        return "🛑 安全词定了：" + word + tail + "。听到这个词就停，不追问。";
    }

    /** F382 使用安全词（推对方）。 */
    public static String safewordUseLine(String word, String who) {
        return "🛑 " + who + " 喊了「" + word + "」。这不是认输，是不想把架吵到没法收场——先停十分钟。";
    }

    /** F382 事后复盘（推双方）。 */
    public static String safewordReflectLine(String reflect) {
        return "📝 那次暂停的复盘：「" + reflect + "」。能喊停，也能事后接着聊，这才叫机制。";
    }

    // ========== F383 敏感日历 ==========

    /** F383 标注（推照护的那位：日子你替 TA 记着）。 */
    public static String sensitiveSetLine(String day, String kindLabel, String care) {
        String tail = care == null || care.isBlank() ? "" : "，TA 说想被" + care;
        return "📌 已帮 TA 标了敏感日：" + day + "（" + kindLabel + "）" + tail + "。前一天系统会提醒你。";
    }

    /** F383 提前一天提醒（推照护人）。 */
    public static String sensitiveRemindLine(String day, String owner, String care) {
        String tail = care == null || care.isBlank() ? "先问一句需要我怎么陪你" : care;
        return "🔔 明天 " + day + " 是 " + owner + " 的敏感日，" + tail + "。" + "别问为什么，照做就行。";
    }

    // ========== F384 说到哪了 ==========

    /** F384 存档（推对方：我们的话头我保管着）。 */
    public static String threadSavedLine(String topic) {
        return "🧵 话头被打断了，我先把「" + topic + "」存进线轴，回头接着说。";
    }

    /** F384 续完销档（推双方）。 */
    public static String threadDoneLine(String topic) {
        return "✂️ 「" + topic + "」这个话题终于说完了，线轴收起来。";
    }

    // ========== F385 真话翻译 ==========

    /** F385 申报词条（推对方：以后我听得懂）。 */
    public static String saySetLine(String say, String means) {
        return "🔤 TA 交了一份反话对照：嘴上说「" + say + "」，其实是「" + means + "」。你只能看，改不了。";
    }

    // ========== F386 聆听方式协议 ==========

    /** F386 写好协议（推对方）。 */
    public static String protocolLine(String modeLabel, String note) {
        String tail = note == null || note.isBlank() ? "" : "（补充：" + note + "）";
        return "🎧 TA 的难过说明书更新了：难过的时候要的是「" + modeLabel + "」" + tail + "。下次照这个来。";
    }

    // ========== F387 话题许愿池 ==========

    /** F387 许愿（推对方：该你接单）。 */
    public static String topicWishLine(String title) {
        return "💭 TA 希望你们多聊聊「" + title + "」。接个单，一周内找个时间好好说。";
    }

    /** F387 接单（推许愿人）。 */
    public static String topicTakeLine(String title) {
        return "📥 「" + title + "」被接单了，这周内一定聊。";
    }

    /** F387 聊完（推双方，overdue 时换一句）。 */
    public static String topicTalkLine(String title, boolean overdue, String reflect) {
        String head = overdue
                ? "🕐 「" + title + "」聊完了——虽然超过一周，记了个超时，但总算是聊了。"
                : "✅ 「" + title + "」一周内聊完了，准时。";
        String tail = reflect == null || reflect.isBlank() ? "" : "一句感想：「" + reflect + "」";
        return head + tail;
    }

    // ========== F388 今日一句话 ==========

    /** F388 留了话（推对方）。 */
    public static String dailyLine(String content) {
        return "💬 今日一句话：「" + content + "」";
    }

    /** F388 今天没留，回看昨天那句（挂在总览上）。 */
    public static String dailyFallbackLine(String yesterday, String content) {
        return "🕯️ 今天还没说。昨天那句还在这儿：" + yesterday + "「" + content + "」";
    }

    // ========== F389 聆听者年报 ==========

    /** F389 年报收尾句池。 */
    private static final List<String> YEAR_TAILS = List.of(
            "被听见这件事，比被原谅还难，你们做到了。",
            "随口一说的话有人接住，就是这个家的承重墙。",
            "会喊停、会补复盘、会把话头收好——这不叫吵架，叫维护。",
            "听你说话这件事，你们是一年比一年认真。"
    );

    /** F389 年报称号（按捕捉兑现与安全词使用综合定档）。 */
    public static String yearTitle(int fulfilled, int avoids, int uses, int talked, int dailies) {
        int score = fulfilled * 3 + avoids * 2 + talked * 2 + uses + dailies / 10;
        if (score >= 40) {
            return "金牌听众";
        }
        if (score >= 24) {
            return "记得比你妈还清楚的人";
        }
        if (score >= 12) {
            return "会喊停的成年人";
        }
        if (score >= 5) {
            return "开始认真听了";
        }
        return "刚拿起小本本";
    }

    /** F389 年报 summary。 */
    public static String yearSummary(String year, int wishes, int fulfilled, int mines, int acked, int avoids,
                                     int uses, int reflected, int sensitives, int threads, int finished,
                                     int says, int talked, int onTime, int dailies, long seed) {
        return "👂 " + year + " 年聆听者年报：偷偷记了 " + wishes + " 个心愿，兑现揭晓 " + fulfilled
                + " 个；挂了 " + mines + " 颗雷，对方知晓 " + acked + " 颗、成功绕开 " + avoids
                + " 次；安全词喊了 " + uses + " 次，" + reflected + " 次补了复盘；标了 " + sensitives
                + " 个敏感日；存档 " + threads + " 个话头，续完 " + finished + " 个；申报 " + says
                + " 条反话；话题池聊完 " + talked + " 个（" + onTime + " 个没超时）；今日一句话留了 " + dailies
                + " 句——称号「" + yearTitle(fulfilled, avoids, uses, talked, dailies) + "」。"
                + YEAR_TAILS.get((int) Math.floorMod(seed, YEAR_TAILS.size()));
    }
}
