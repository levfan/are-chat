package com.smart.chat.couple.infrastructure.content;

/**
 * 加班预报与留灯内容库（保留卡 `couple-quest-overtime`，原 F372）。
 * 静态文案只增不改顺序；随功能裁剪，其余关卡话术已下线。
 */
public final class CoupleQuestBank {

    private CoupleQuestBank() {
    }

    /** 预报发出（推对方：别等饭）。 */
    public static String overtimeLine(int untilHour, String note) {
        String tail = note == null || note.isBlank() ? "" : "，附一句：「" + note + "」";
        return "🌙 今晚要忙到 " + untilHour + " 点左右，别等饭" + tail;
    }

    /** 留灯卡（推加班的人）。 */
    public static String lampLine(String text) {
        return "💡 灯给你留着：「" + text + "」——回来再晚，屋里是亮的。";
    }
}
