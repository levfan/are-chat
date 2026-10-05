package com.smart.chat.couple.domain.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * 百日回顾里那句「一句话总结」的生成规则。
 * <p>
 * 刻意<b>不调用任何外部大模型</b>：本仓库没有 LLM 依赖、构建是离线的（{@code mvn -o}），
 * 也没有密钥配置——为了一个页面把依赖链和部署条件一起换掉不值当。取舍理由见 docs/adr/0007。
 * 规则本身也够诚实：只说数据里真有的事，一个数字都不编。
 */
public final class RelationSummary {

    /** 生成一句话所需的全部事实，缺一项就少说半句。 */
    public record Facts(long daysTogether, int confirmedDays, int longestStreak, int makeupDays,
                        int bothAnsweredDays, int fulfilledWishes, String intimacyTitle, String latestUnlockLabel) {
    }

    private RelationSummary() {
    }

    /** 拼成一句人话：开头是天数与打卡，中间是问答与愿望，收尾给称号与最新解锁。 */
    public static String compose(Facts facts) {
        java.util.List<String> sentences = new ArrayList<>();

        StringBuilder opening = new StringBuilder();
        opening.append("在一起 ").append(facts.daysTogether()).append(" 天，")
                .append("你们一起打卡 ").append(facts.confirmedDays()).append(" 天");
        if (facts.longestStreak() > 0) {
            opening.append("，最长连着 ").append(facts.longestStreak()).append(" 天");
        }
        if (facts.makeupDays() > 0) {
            opening.append("（其中 ").append(facts.makeupDays()).append(" 天是补签回来的 😉）");
        }
        sentences.add(opening.append("。").toString());

        List<String> middle = new ArrayList<>();
        if (facts.bothAnsweredDays() > 0) {
            middle.add("答完了 " + facts.bothAnsweredDays() + " 道每日一问");
        }
        if (facts.fulfilledWishes() > 0) {
            middle.add("偷偷帮对方实现了 " + facts.fulfilledWishes() + " 个愿望");
        }
        if (!middle.isEmpty()) {
            sentences.add(String.join("，", middle) + "。");
        }

        List<String> tail = new ArrayList<>();
        if (facts.intimacyTitle() != null && !facts.intimacyTitle().isBlank()) {
            tail.add("现在的你们是「" + facts.intimacyTitle() + "」");
        }
        if (facts.latestUnlockLabel() != null && !facts.latestUnlockLabel().isBlank()) {
            tail.add("最近解锁了「" + facts.latestUnlockLabel() + "」");
        }
        if (!tail.isEmpty()) {
            sentences.add(String.join("，", tail) + "。");
        }
        sentences.add("往后的日子接着答、接着打卡。");
        return String.join("", sentences);
    }
}
