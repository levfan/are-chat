package com.smart.chat.couple;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 惊喜与期待内容库（F50-F59）：刮刮乐券面、盲盒任务灵感、花语库、幸运签文。
 * 全部为内置静态内容，只增不改既有条目顺序；刮刮乐按「空间+周」稳定抽取，
 * 玫瑰/幸运签等带随机成分的取值由 Service 决定。
 */
public final class CoupleSurpriseBank {

    private CoupleSurpriseBank() {
    }

    // ========== F50 刮刮乐券库（kind, 券面文案，24 种） ==========

    private static final String[][] SCRATCH_PRIZES = {
            {"hug", "一个不少于 10 秒的用力抱抱"},
            {"kiss", "一个额头上的晚安吻"},
            {"breakfast", "一顿由 TA 亲手做的早餐"},
            {"movie", "一场电影，片单你说了算"},
            {"massage", "一次 15 分钟的肩颈按摩"},
            {"snack", "一份 TA 亲手买的小零食"},
            {"chore", "一次家务全包（洗碗拖地都归 TA）"},
            {"late", "一张「晚睡 30 分钟」赦免券"},
            {"nap", "一次午睡叫醒服务（温柔版）"},
            {"walk", "一次饭后的牵手散步"},
            {"song", "一首为你们点播的歌"},
            {"photo", "一次 TA 亲自下厨的治愈晚餐"},
            {"game", "一局双排，输的人说三句情话"},
            {"bubble", "一次热水袋+暖手宝全套供暖服务"},
            {"hair", "一次温柔的摸头杀"},
            {"story", "一个睡前小故事（现编的）"},
            {"milk", "一杯睡前热牛奶"},
            {"noangry", "一张「小事免生气」金牌（24 小时）"},
            {"date", "一次由 TA 全权安排的约会"},
            {"star", "一起看一次星星或日出"},
            {"hand", "一整天的牵手权"},
            {"praise", "十句不重样的夸夸"},
            {"errand", "一次跑腿服务（奶茶/宵夜皆可）"},
            {"wish", "一个小愿望直通车（合理范围 TA 买单）"}
    };

    // ========== F51 盲盒任务灵感（16 条，装任务盒时可一键填入） ==========

    private static final List<String> BOX_TASK_IDEAS = List.of(
            "去镜子前对自己笑一下，然后回来告诉我镜子里的你很好看",
            "喝一杯水，然后回来领奖励",
            "给 3 小时后的自己定一个开心的小计划",
            "出门看一眼天空，回来形容给我听",
            "抱一抱身边离你最近的东西（枕头也算）",
            "深呼吸三次，把烦恼呼出去",
            "去洗把脸，回来告诉我你有多好看",
            "找出一件你觉得我可能会喜欢的东西，拍下来考考我",
            "写下今天最想对我说的一句话，明天告诉我",
            "伸个懒腰，转三圈，然后原地站好等我夸",
            "打开窗户透气一分钟，回来报告天气",
            "给自己泡杯热饮，顺便替我也干一杯",
            "找一个我们都没去过的地方，加入下次约会清单",
            "唱一句你最喜欢的歌给我听",
            "说出我的三个优点，少一个都不行",
            "现在起立，做个专属你我的胜利姿势"
    );

    // ========== F55 花语库（8 种花，每种配 emoji 与花语集） ==========

    private static final Object[][] FLOWERS = {
            {"rose", "🌹", new String[]{"热烈的爱，只对你", "爱你在心口难开？不存在的", "一朵玫瑰，一颗真心"}},
            {"tulip", "🌷", new String[]{"温柔的牵挂，藏不住了", "给你一点春天的颜色", "郁金香说：你是我的偏爱"}},
            {"sunflower", "🌻", new String[]{"你是我的太阳，我负责向着你", "向阳而生，因你而甜", "今天也要做你的小太阳"}},
            {"lily", "百合🤍", new String[]{"纯净的喜欢，像初见那天", "百事合意，包括我们", "安静地爱你，不吵不闹"}},
            {"carnation", "🌸", new String[]{"谢谢你把我照顾得很好", "一点点感谢，一大片心意", "康乃馨说：辛苦了，我的爱人"}},
            {"daisy", "🌼", new String[]{"小小的花，大大的喜欢", "藏进心底的秘密：是你呀", "雏菊的愿望是每天都见到你"}},
            {"lavender", "💜", new String[]{"等待是甜的，因为等的是你", "给你的梦里加一点香气", "薰衣草的安静，是想念你的声音"}},
            {"gypsophila", "✨", new String[]{"满天星说：你是我的主角", "不做主角也没关系，围着你转就好", "漫天星光，不如你眼里一格"}}
    };

    // ========== F56 幸运签文（20 支，含等级） ==========

    private static final String[][] FORTUNE_SLIPS = {
            {"大吉", "今天会有一件小事顺利得不可思议，记得留意"},
            {"大吉", "TA 今天的运气由你保管，随便挥霍"},
            {"大吉", "心想事成签：先想一件小小的，练练手"},
            {"大吉", "今日宜牵手，宜大笑，宜说「我爱你」"},
            {"中吉", "今天的烦恼会自己走丢，不用追"},
            {"中吉", "宜偷懒十分钟，效率反而会变高"},
            {"中吉", "今天会遇到一句让你心头一暖的话（比如现在这句）"},
            {"中吉", "宜吃点好的，你的快乐今天打九折"},
            {"中吉", "今天的小情绪都会被接住，放心说"},
            {"中吉", "宜把没说出口的谢谢说出口"},
            {"小吉", "平平无奇的一天里，藏着一点点小甜"},
            {"小吉", "今天的你比昨天更可爱了一点点（科学统计）"},
            {"小吉", "宜早睡十分钟，明天会感谢你"},
            {"小吉", "今天适合慢一点，反正有我陪你"},
            {"小吉", "宜给爱的人发消息，对方正在等"},
            {"锦鲤", "转发这条签的不是你，是签想留在你身边"},
            {"锦鲤", "抽到即中：本周必有一件好事发生"},
            {"锦鲤", "锦鲤本鲤已上线，今天说什么都灵"},
            {"锦鲤", "好运已发货，签收人：你"},
            {"锦鲤", "把这支签贴在心上，霉运绕道走"}
    };

    /** 券库（只读），元素为 {kind, text}。 */
    public static String[][] scratchPrizes() {
        return SCRATCH_PRIZES;
    }

    /** 盲盒任务灵感（只读）。 */
    public static List<String> boxTaskIdeas() {
        return BOX_TASK_IDEAS;
    }

    /** 按周+空间稳定抽一张券（同一周双方收到的券固定不变）。 */
    public static String[] pickScratchPrize(String spaceId, String weekKey, String owner) {
        int index = Math.floorMod(stableHash(spaceId + "|scratch|" + weekKey + "|" + owner), SCRATCH_PRIZES.length);
        return SCRATCH_PRIZES[index];
    }

    /** 随机一条盲盒任务灵感。 */
    public static String randomBoxTaskIdea() {
        return BOX_TASK_IDEAS.get(ThreadLocalRandom.current().nextInt(BOX_TASK_IDEAS.size()));
    }

    /** 花库（只读），元素为 {key, emoji, words[]}。 */
    public static Object[][] flowers() {
        return FLOWERS;
    }

    /** 按花种随机一条花语。 */
    public static String randomFlowerWord(String flowerKey) {
        for (Object[] flower : FLOWERS) {
            if (flower[0].equals(flowerKey)) {
                String[] words = (String[]) flower[2];
                return words[ThreadLocalRandom.current().nextInt(words.length)];
            }
        }
        return "喜欢你，没道理";
    }

    /** 花种对应的 emoji（找不到回退 🌹）。 */
    public static String flowerEmoji(String flowerKey) {
        for (Object[] flower : FLOWERS) {
            if (flower[0].equals(flowerKey)) {
                return (String) flower[1];
            }
        }
        return "🌹";
    }

    /** 随机一支幸运签，返回 {level, content}。 */
    public static String[] randomFortuneSlip() {
        return FORTUNE_SLIPS[ThreadLocalRandom.current().nextInt(FORTUNE_SLIPS.length)];
    }

    /** 稳定字符串哈希（FNV-1a 32 位），与 CoupleRitualBank 一致。 */
    public static int stableHash(String input) {
        int hash = 0x811c9dc5;
        for (int i = 0; i < input.length(); i++) {
            hash ^= input.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }
}
