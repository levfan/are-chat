package com.smart.chat.couple.infrastructure.content;

import java.time.LocalDate;
import java.util.List;

/**
 * 夫妻老黄历内容库（批次二十一 F250-F259）。静态内容只增不改顺序；
 * 农历换算用标准 1900-2100 压缩表（每年一个 int：低 4 位闰月、bit4 闰月大小、bit5-16 十二月大小月、bit20 春节前的正月大小）。
 * 节气日期取常用近似日（±1 天内可跟风，容忍历法微差）。
 */
public final class CoupleTermBank {

    private CoupleTermBank() {
    }

    /** 24 节气名（小寒起按年内顺序）。 */
    public static final List<String> TERMS = List.of(
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
            "立夏", "小满", "芒种", "夏至", "小暑", "大暑", "立秋", "处暑",
            "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至");

    /** 节气近似公历日：term -> MM-dd（小寒 01-05 …冬至 12-22，步长约 15 天）。 */
    public static LocalDate termOn(String term, int year) {
        Integer md = TERM_MD.get(term);
        if (md == null) {
            return null;
        }
        return LocalDate.of(year, md / 100, md % 100);
    }

    private static final java.util.Map<String, Integer> TERM_MD = java.util.Map.ofEntries(
            java.util.Map.entry("小寒", 105), java.util.Map.entry("大寒", 120),
            java.util.Map.entry("立春", 204), java.util.Map.entry("雨水", 219),
            java.util.Map.entry("惊蛰", 306), java.util.Map.entry("春分", 321),
            java.util.Map.entry("清明", 405), java.util.Map.entry("谷雨", 420),
            java.util.Map.entry("立夏", 506), java.util.Map.entry("小满", 521),
            java.util.Map.entry("芒种", 606), java.util.Map.entry("夏至", 621),
            java.util.Map.entry("小暑", 707), java.util.Map.entry("大暑", 723),
            java.util.Map.entry("立秋", 808), java.util.Map.entry("处暑", 823),
            java.util.Map.entry("白露", 908), java.util.Map.entry("秋分", 923),
            java.util.Map.entry("寒露", 1008), java.util.Map.entry("霜降", 1023),
            java.util.Map.entry("立冬", 1107), java.util.Map.entry("小雪", 1122),
            java.util.Map.entry("大雪", 1207), java.util.Map.entry("冬至", 1222));

    /** 当日节气（近似 ±1 天窗口内命中才算）。 */
    public static String termOfToday(LocalDate today) {
        for (String t : TERMS) {
            LocalDate d = termOn(t, today.getYear());
            if (d != null && Math.abs(java.time.temporal.ChronoUnit.DAYS.between(d, today)) <= 1) {
                return t;
            }
        }
        return null;
    }

    /** 下一个节气（含临近的）。 */
    public static String nextTerm(LocalDate today) {
        String next = null;
        long best = Long.MAX_VALUE;
        for (String t : TERMS) {
            for (int y = today.getYear(); y <= today.getYear() + 1; y++) {
                LocalDate d = termOn(t, y);
                long diff = java.time.temporal.ChronoUnit.DAYS.between(today, d);
                if (diff >= 0 && diff < best) {
                    best = diff;
                    next = t;
                }
            }
        }
        return next;
    }

    /** F250 跟风晒话术池。 */
    public static final List<String> CHECK_LINES = List.of(
            "这个节气，我们没缺席", "跟风成功，仪式感 +1", "别人过节气，我们过日子",
            "节气到了，默契也该到了", "跟着节气过日子，越过越有滋味");

    /** F252 择吉日点评（宜/忌各一句，stableHash 选取）。 */
    public static String luckyComment(long seed, String matter) {
        int idx = Math.floorMod(seed, YI.size());
        int jdx = Math.floorMod(seed >>> 8, JI.size());
        return "宜 " + YI.get(idx) + "｜忌 " + JI.get(jdx) + "｜论正事：「" + matter + "」天天地是吉日，挑一天只是给它一个仪式感";
    }

    public static final List<String> YI = List.of(
            "官宣大事", "吃顿好的", "把话说开", "存一笔小目标", "牵手散步", "定个日子",
            "翻旧照片", "互相夸一句", "早起十分钟", "点外卖不纠结", "把阳台收拾干净", "给未来留一句话");

    public static final List<String> JI = List.of(
            "较真", "熬夜", "翻旧账", "空腹喝咖啡", "把「改天」说出口", "小气",
            "忘带钥匙", "不回消息", "逞强", "不吃饭", "跟外卖骑手赛跑", "把惊喜剧透");

    /** F256 生肖年运池（按 space+year+生肖 hash 抽一句）。 */
    public static String zodiacFortune(long seed) {
        int idx = Math.floorMod(seed, FORTUNES.size());
        return FORTUNES.get(idx);
    }

    public static final List<String> FORTUNES = List.of(
            "本年利同居、利吵架后先递台阶、利囤零食，忌把「随便」当口头禅",
            "本年桃花在厨房与快递柜之间，宜一起做饭少一起点外卖，感情升温看谁先洗碗",
            "本年财运平稳，双人账本宜月月对账；有小人，多半是那只不关的冰箱门",
            "本年宜出行、宜给纪念日提前三天准备，忌「到时候再说」",
            "本年口才是最大风水：一句「辛苦了」能顶三个大件快递",
            "本年适合开新坑（共同爱好），忌烂尾；双人计划表完成率达七成即大吉",
            "本年贵人在隔壁——多串门、多拼单、多借酱油",
            "本年情绪稳则诸事顺，深夜消息次晨再回，可避九成口角",
            "本年利储蓄，包括存钱、存觉、存拥抱；年底结算利息惊人",
            "本年有两次小波折，均在「谁去买菜」上，轮流应答可化解",
            "本年宜把「我们」挂嘴边，忌把「你从来不」挂嘴边",
            "本年家宅运旺，宜给家里添一束花；花开之日即好运启动之时");

    /** F258 挡打卡时的「偷得浮生」卡。 */
    public static String normalDayCard(long seed) {
        int idx = Math.floorMod(seed, NORMAL_CARDS.size());
        return NORMAL_CARDS.get(idx);
    }

    public static final List<String> NORMAL_CARDS = List.of(
            "今日是我们约定的「什么都不做日」：不打卡、不仪式、不卷。躺平即忠诚。",
            "黄历今日宜：无所事事。忌：内卷式秀恩爱。此天非弃，乃蓄。",
            "已替你挡住一次打卡。浪漫也需要留白，今天只负责在一起。");

    /** F254 八大节日键。 */
    public static final List<String> FESTIVALS = List.of("NEWYEAR", "CHUXI", "VALENTINE", "L520", "QIXI", "MIDAUTUMN", "NATIONAL", "ANNIVM");
    public static final java.util.Map<String, String> FESTIVAL_LABEL = java.util.Map.ofEntries(
            java.util.Map.entry("NEWYEAR", "元旦"), java.util.Map.entry("CHUXI", "除夕"),
            java.util.Map.entry("VALENTINE", "情人节"), java.util.Map.entry("L520", "520"),
            java.util.Map.entry("QIXI", "七夕"), java.util.Map.entry("MIDAUTUMN", "中秋"),
            java.util.Map.entry("NATIONAL", "国庆"), java.util.Map.entry("ANNIVM", "我们的周年月"));

    /** 节日公历日（农历节日换算；ANNIVM 无固定日，由调用方按空间纪念日月份处理）。 */
    public static LocalDate festivalOn(String festival, int year) {
        switch (festival) {
            case "NEWYEAR":
                return LocalDate.of(year, 1, 1);
            case "CHUXI":
                return springFestival(year).minusDays(1);
            case "VALENTINE":
                return LocalDate.of(year, 2, 14);
            case "L520":
                return LocalDate.of(year, 5, 20);
            case "QIXI":
                return lunarToSolar(year, 7, 7, false);
            case "MIDAUTUMN":
                return lunarToSolar(year, 8, 15, false);
            case "NATIONAL":
                return LocalDate.of(year, 10, 1);
            default:
                return null;
        }
    }

    /** 农历正月初一（春节）公历日。 */
    public static LocalDate springFestival(int year) {
        return lunarToSolar(year, 1, 1, false);
    }

    /** F257 内置法定长假（近似首日：元旦/春节/清明/五一/端午/中秋/国庆）。 */
    public static List<String> holidayNames() {
        return List.of("元旦", "春节", "清明", "劳动节", "端午", "中秋", "国庆");
    }

    /** 某年假期首日（近似：元旦 1-1、春节=除夕起、清明 4-4、五一 5-1、端午=农历五月初五、中秋=农历八月十五、国庆 10-1）。 */
    public static LocalDate holidayOn(String name, int year) {
        switch (name) {
            case "元旦":
                return LocalDate.of(year, 1, 1);
            case "春节":
                return springFestival(year);
            case "清明":
                return LocalDate.of(year, 4, 4);
            case "劳动节":
                return LocalDate.of(year, 5, 1);
            case "端午":
                return lunarToSolar(year, 5, 5, false);
            case "中秋":
                return lunarToSolar(year, 8, 15, false);
            case "国庆":
                return LocalDate.of(year, 10, 1);
            default:
                return null;
        }
    }

    /** 未来 n 天内即将到来的长假（返回 名称/首日/还有几天）。 */
    public static String[] nextHoliday(LocalDate today, int withinDays) {
        String bestName = null;
        LocalDate bestDay = null;
        for (int y = today.getYear(); y <= today.getYear() + 1; y++) {
            for (String name : holidayNames()) {
                LocalDate d = holidayOn(name, y);
                if (d == null || d.isBefore(today)) {
                    continue;
                }
                long diff = java.time.temporal.ChronoUnit.DAYS.between(today, d);
                if (bestDay == null || d.isBefore(bestDay)) {
                    bestDay = d;
                    bestName = name;
                }
                if (diff <= withinDays) {
                    // 继续扫下一年也不影响最早结果
                }
            }
        }
        if (bestName == null || bestDay == null) {
            return null;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(today, bestDay);
        if (days > withinDays) {
            return null;
        }
        return new String[]{bestName + bestDay.getYear(), bestName, bestDay.toString(), String.valueOf(days)};
    }

    // ==================== 农历换算（标准 1900-2100 压缩表） ====================

    private static final int[] LUNAR_INFO = {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
            0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
            0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
            0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
            0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
            0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a4d0, 0x0d150, 0x0f252,
            0x0d520
    };

    private static final LocalDate BASE_DATE = LocalDate.of(1900, 1, 31);

    /** 农历 year 年总天数（标准 348 天基 + 12 月大小位 + 闰月天数）。 */
    public static int lunarYearDays(int year) {
        int info = LUNAR_INFO[year - 1900];
        int sum = 348;
        for (int i = 0x8000; i > 0x8; i >>= 1) {
            sum += (info & i) == 0 ? 0 : 1;
        }
        return sum + leapMonthDays(year);
    }

    /** 农历闰月（1-12，0 无）。 */
    public static int leapMonth(int year) {
        return LUNAR_INFO[year - 1900] & 0xf;
    }

    /** 农历闰月天数（无闰月为 0）。 */
    private static int leapMonthDays(int year) {
        if (leapMonth(year) == 0) {
            return 0;
        }
        return (LUNAR_INFO[year - 1900] & 0x10000) == 0 ? 29 : 30;
    }

    /** 农历某月天数（leap=true 取闰月）。 */
    public static int lunarMonthDays(int year, int month, boolean leap) {
        int info = LUNAR_INFO[year - 1900];
        if (leap) {
            return leapMonth(year) == month ? ((info & 0x10000) == 0 ? 29 : 30) : 0;
        }
        return (info & (0x10000 >> month)) == 0 ? 29 : 30;
    }

    /** 农历 -> 公历；非法日期返回 null。 */
    public static LocalDate lunarToSolar(int year, int month, int day, boolean leap) {
        if (year < 1900 || year > 2100 || month < 1 || month > 12 || day < 1 || day > 30) {
            return null;
        }
        if (day > lunarMonthDays(year, month, leap)) {
            return null;
        }
        int offset = 0;
        for (int y = 1900; y < year; y++) {
            offset += lunarYearDays(y);
        }
        int lm = leapMonth(year);
        for (int m = 1; m < month; m++) {
            offset += lunarMonthDays(year, m, false);
            if (lm == m) {
                offset += lunarMonthDays(year, m, true);
            }
        }
        if (leap) {
            offset += lunarMonthDays(year, month, false);
        }
        offset += day - 1;
        return BASE_DATE.plusDays(offset);
    }

    /** 公历 -> 农历 [年, 月, 日, 是否闰月0/1]。 */
    public static int[] solarToLunar(LocalDate date) {
        long offset = java.time.temporal.ChronoUnit.DAYS.between(BASE_DATE, date);
        if (offset < 0) {
            return null;
        }
        int year = 1900;
        while (year <= 2100) {
            int yd = lunarYearDays(year);
            if (yd > offset) {
                break;
            }
            offset -= yd;
            year++;
        }
        if (year > 2100) {
            return null;
        }
        int lm = leapMonth(year);
        int month = 1;
        boolean leap = false;
        while (month <= 12) {
            int md = lunarMonthDays(year, month, false);
            if (offset < md) {
                break;
            }
            offset -= md;
            if (lm == month) {
                int ld = lunarMonthDays(year, month, true);
                if (offset < ld) {
                    leap = true;
                    break;
                }
                offset -= ld;
            }
            month++;
        }
        if (month > 12) {
            return null;
        }
        return new int[]{year, month, (int) offset + 1, leap ? 1 : 0};
    }

    /** 生肖（1900 鼠年起推）。 */
    public static String zodiac(int year) {
        String[] names = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};
        return names[Math.floorMod(year - 1900, 12)];
    }
}
