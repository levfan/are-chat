package com.smart.chat.couple;

import java.util.List;
import java.util.Map;

/**
 * 二人制造厂内容库（批次二十三 F270-F279）。静态内容只增不改顺序。
 */
public final class CoupleFactoryBank {

    private CoupleFactoryBank() {
    }

    /** F270 轮盘开场话术池。 */
    public static String spinOpenLine(long seed) {
        return SPIN_OPEN.get(Math.floorMod(seed, SPIN_OPEN.size()));
    }

    private static final List<String> SPIN_OPEN = List.of(
            "命运转盘开始转动，本周家务听天由命 🎡",
            "愿者上钩，不服下周再转。",
            "转盘无罪，干活有理。",
            "抽到就是天选打工人，恭喜上岗。");

    /** F271 月榜头衔。 */
    public static final String SHOP_CHAMPION = "本月生活委员 🧺";

    /** F272 临期话术。 */
    public static String expireLine(String item) {
        return "冰箱里的「" + item + "」快到赏味期了，今晚吃掉它？";
    }

    /** F273 感谢章积分（走 F186 台账 EARN）。 */
    public static final int PARCEL_EARN_POINTS = 2;
    public static final String PARCEL_REASON = "代拿快递感谢章";

    /** F276 同起话术。 */
    public static final String STANDUP_BOTH = "两人都站起来了，同起 +1 ☑ 别让椅子记住你们";

    /** F277 清账卡。 */
    public static final String ADVANCE_SETTLED = "垫付清零，无债一身轻 🧾 今晚加菜庆祝（一分钟版）";

    /** F279 家安六项（编码与标签成对，只增不改序）。 */
    public static final Map<String, String> CHECK_ITEMS = Map.of(
            "GAS", "燃气阀门", "WATER", "水管漏水", "ELEC", "插座发热",
            "WINDOW", "门窗密闭", "LOCK", "门锁钥匙", "FIRSTAID", "急救箱齐全");
    public static final List<String> CHECK_ORDER = List.of("GAS", "WATER", "ELEC", "WINDOW", "LOCK", "FIRSTAID");

    /** F279 漏检提醒话术。 */
    public static String checkMissLine(String month) {
        return month + " 的家安月检还没交齐，水电气不等人 ⚠️";
    }

    /** F278 战利品打分评论语。 */
    public static String groceryScoreLine(int score) {
        if (score >= 4) {
            return "TA 真的很懂你，购物车都是共同语言 💞";
        }
        if (score >= 2) {
            return "猜对一半，默契还有增长空间 📈";
        }
        return "下次采购记得报备，别让 TA 猜哑谜 😌";
    }
}
