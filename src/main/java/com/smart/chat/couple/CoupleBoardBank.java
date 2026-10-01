package com.smart.chat.couple;

import java.util.List;

/** 我们公司系静态内容：职级阶梯、名片收尾话术（F243/F248，无表、只增不改）。 */
public final class CoupleBoardBank {

    private CoupleBoardBank() {
    }

    /** 职级门槛（累计赚得积分达到即得称号，F243 晋升公示口径）。 */
    public static final int[] RANK_THRESHOLDS = {0, 20, 60, 150, 300, 600};
    public static final String[] RANK_NAMES = {"实习生", "正式职员", "小组主管", "部门经理", "公司总监", "合伙人"};

    /** 公司名片收尾话术。 */
    public static final List<String> CARD_TAIL = List.of(
            "本公司唯一 KPI：在一起，越久越好。",
            "本公司不分红，分红都分给心动。",
            "本公司章程第一条：偏爱写在制度里。",
            "本公司常年招聘：岗位是「一辈子」。"
    );

    /** 当前职级名。 */
    public static String rank(int earned) {
        String name = RANK_NAMES[0];
        for (int i = 0; i < RANK_THRESHOLDS.length; i++) {
            if (earned >= RANK_THRESHOLDS[i]) {
                name = RANK_NAMES[i];
            }
        }
        return name;
    }

    /** 下一职级名（已是最高为 null）。 */
    public static String nextRank(int earned) {
        for (int i = 0; i < RANK_THRESHOLDS.length; i++) {
            if (earned < RANK_THRESHOLDS[i]) {
                return RANK_NAMES[i];
            }
        }
        return null;
    }

    /** 距下一职级还差多少积分（已是最高为 null）。 */
    public static Integer pointsToNext(int earned) {
        for (int threshold : RANK_THRESHOLDS) {
            if (earned < threshold) {
                return threshold - earned;
            }
        }
        return null;
    }

    /** 名片收尾一句（按空间稳定）。 */
    public static String cardTail(String spaceId) {
        return CARD_TAIL.get(Math.floorMod(
                CoupleRitualBank.stableHash(spaceId + "|board-card"), CARD_TAIL.size()));
    }
}
