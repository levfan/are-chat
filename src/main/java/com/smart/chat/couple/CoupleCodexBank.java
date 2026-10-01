package com.smart.chat.couple;

import java.util.List;
import java.util.Map;

/**
 * 我们百科内容库（批次二十四 F280-F289）。静态内容只增不改顺序。
 */
public final class CoupleCodexBank {

    private CoupleCodexBank() {
    }

    /** F281 出题指令模板（填空题干围绕词条生成）。 */
    public static String quizPrompt(String term) {
        return "我们的黑话「" + term + "」指的是什么？（用自己的话答，越具体越默契）";
    }

    /** F281 默契率评语池。 */
    public static String quizComment(long seed, int match) {
        String line = QUIZ_COMMENTS.get(Math.floorMod(seed, QUIZ_COMMENTS.size()));
        return "本场默契 " + match + "/5：" + line;
    }

    private static final List<String> QUIZ_COMMENTS = List.of(
            "同一个脑子租的两个住户。",
            "词典没写下的，你们自己都懂。",
            "差一点点，但差的那点也很可爱。",
            "这题你们俩的答案该写进百科。",
            "默契不是满分，是愿意对答案。");

    /** F282 类目常量（键→展示名，只增不改序）。 */
    public static final List<String> TOP_CATEGORIES =
            List.of("FOOD", "MOVIE", "SONG", "COLOR", "PLACE_EAT", "SHOW", "SEAT", "SNACK");
    public static final Map<String, String> TOP_LABELS = Map.of(
            "FOOD", "爱吃 Top10", "MOVIE", "爱看影片 Top10", "SONG", "循环歌单 Top10",
            "COLOR", "心动颜色 Top10", "PLACE_EAT", "想约的店 Top10", "SHOW", "爱看的剧 Top10",
            "SEAT", "家里最爱待的角落 Top10", "SNACK", "冰箱常客 Top10");

    /** F282 重新认识清单话术。 */
    public static String rematchLine(String category, String item) {
        return TOP_LABELS.getOrDefault(category, category) + "：「" + item + "」——原来 TA 现在喜欢这个";
    }

    /** F286 互见开场话术。 */
    public static String firstLookLine(long seed, boolean same) {
        return same ? FIRST_SAME.get(Math.floorMod(seed, FIRST_SAME.size()))
                : FIRST_DIFF.get(Math.floorMod(seed, FIRST_DIFF.size()));
    }

    private static final List<String> FIRST_SAME = List.of(
            "你们想起的是同一秒——原来第一眼对视真的存在双向信号。",
            "同刻命中！这一刻值得追加进第一次清单。");
    private static final List<String> FIRST_DIFF = List.of(
            "你们的第一眼不是同一帧，但都为对方留了档——差的那几秒叫心动延迟。",
            "记忆会骗人，但不会骗心意。两个版本都收进百科。");

    /** F289 八题四维卷（每题两个选项，答案 1/2；题序即轴序 E/I S/N T/F P/J）。 */
    public static final List<String[]> TYPE_QUESTIONS = List.of(
            new String[]{"周末更想怎么过？", "组一大局叫上所有人", "就俩人窝着各干各的"},
            new String[]{"聊天更常聊什么？", "正在发生的具体事", "想法、可能性和未来"},
            new String[]{"朋友来诉苦，你先？", "帮着把问题捋出方案", "先共情说一句「太难了」"},
            new String[]{"旅行计划习惯？", "订好每天的行程表", "订张机票走到哪算哪"},
            new String[]{"能量恢复靠？", "见人多热闹场合", "独处安静回血"},
            new String[]{"记住的更多是？", "细节和事实", "整体的感觉和印象"},
            new String[]{"吵完架更想？", "先讲清楚谁对谁错", "先和好，道理回头再说"},
            new String[]{"对待待办清单？", "提前做完才安心", "压着 deadline 更有灵感"});

    /** 轴对（第 i 题选 1 记左字母，选 2 记右字母；第 4/8 题「提前计划」为 J）。 */
    public static final char[][] TYPE_AXES = {
            {'E', 'I'}, {'S', 'N'}, {'T', 'F'}, {'J', 'P'} };

    /** F289 组合差异解读话术（按同/异轴数）。 */
    public static String typeDiffLine(long seed, int sameAxes) {
        String line = TYPE_DIFFS.get(Math.floorMod(seed, TYPE_DIFFS.size()));
        return "四维里 " + sameAxes + "/4 轴相同：" + line;
    }

    private static final List<String> TYPE_DIFFS = List.of(
            "互补的意思是：你俩加起来刚好是一个完整的人。",
            "同轴的地方速通，异轴的地方别争，分工就好。",
            "差异不是 bug，是二人世界的功能扩展包。",
            "留一半世界给对方解释，是我们百科存在的意义。");
}
