package com.smart.chat.couple.infrastructure.content;

import com.smart.chat.couple.infrastructure.persistence.CoupleComfort;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 懂我与被接住内容库（F60/F63/F65/F66/F68）：
 * 安慰话术卡、陪聊话题卡、深夜关怀文案、真心话题库、心灵感应题库。
 * 全部为内置静态内容，只增不改既有条目顺序。
 */
public final class CoupleTalkBank {

    private CoupleTalkBank() {
    }

    // ========== F60 安慰话术卡（按感受分组，每种 5 条） ==========

    private static final List<String> COMFORT_SAD = List.of(
            "抱抱，不用说话，我在这儿",
            "难过就说出来，我接得住",
            "今天辛苦了，剩下的交给我",
            "你不用一直坚强，在我这里可以哭",
            "把难过分我一半，我比你扛得住"
    );

    private static final List<String> COMFORT_WRONGED = List.of(
            "委屈了？谁欺负你了，我第一个不答应",
            "你没错，先站你这边",
            "受委屈了就回来，我给你顺毛",
            "别憋着，骂出来，我听着",
            "在我这儿你永远有理（先哄再说）"
    );

    private static final List<String> COMFORT_TIRED = List.of(
            "累就歇会儿，天塌不下来",
            "今晚什么都不干，我伺候你",
            "去躺着，我来善后",
            "你已经做得很好了，真的",
            "充电吧，我是你的充电宝 🔋"
    );

    private static final List<String> COMFORT_ANXIOUS = List.of(
            "深呼吸，最坏的结果有我陪你扛",
            "一件一件来，先解决我陪你这件事",
            "焦虑的时候抱紧我，我比重力稳",
            "别怕，我们把担心写下来逐个拆掉",
            "今晚先把焦虑寄存在我这儿，明天再说"
    );

    private static final List<String> COMFORT_EMO = List.of(
            "emo 就 emo 一会儿，我陪你一起",
            "雨总会停的，我先给你撑会儿伞 ☔",
            "来，靠在我肩上把情绪放完",
            "你emo的样子我也喜欢，但我更想看你笑",
            "我把明天的太阳预定了，今晚先睡个好觉"
    );

    // ========== F63 陪聊话题卡（20 条，低落时不知道聊什么就抽一张） ==========

    private static final List<String> CHAT_TOPICS = List.of(
            "说一件今天最小但最开心的事",
            "如果现在可以瞬移，你想去哪儿？",
            "最近有没有什么想吐槽的？我当树洞",
            "你小时候最喜欢的一道菜是什么？",
            "如果给我起一个可爱外号，你会叫什么？",
            "说一个你最近注意到的小美好（比如晚霞）",
            "如果我们养一只宠物，你希望它是什么性格？",
            "你最想删掉哪一天的 memory？聊聊看",
            "现在最想吃的一样东西，说出来我记下了",
            "如果今天重来一遍，你想改变哪个瞬间？",
            "说一部你想拉我一起看的片子",
            "你最近一次心动是什么时候？（答案最好是我）",
            "如果我们现在在旅行，你会把我带去哪条街？",
            "说一个只有你才知道的小习惯",
            "你觉得我身上最好闻的地方是哪里？",
            "今晚的月亮好看吗？去看一眼再回来",
            "如果把我们的故事写成歌，歌名是什么？",
            "你希望我下周为你做的一件小事是？",
            "说一句你现在最想听到的话，我说给你听",
            "十年后的我们会在做什么？大胆猜"
    );

    // ========== F65 深夜关怀文案（按心情分组） ==========

    private static final String NIGHT_CARE_GENERIC = "🌙 深夜提醒：TA 今天心情不太好。如果 TA 还没睡，去说句晚安；如果已睡下，明早给 TA 一个大大的拥抱。";

    // ========== F66 真心话题库（30 题，每天按空间稳定一题） ==========

    private static final List<String> TRUTH_QUESTIONS = List.of(
            "你最怕我哪一点生气？（说实话）",
            "跟我在一起后，你最大的变化是什么？",
            "如果我们的感情让你给打分，现在是几分？扣的分差在哪？",
            "你有没有一个从来不敢跟我说的小心事？",
            "我做过让你最感动的一件事是什么？",
            "我做过让你最难过的一件事是什么？（趁今天说出来）",
            "你觉得我们吵架时，谁先低头比较合适？",
            "如果我有一天变得很不可爱，你还会喜欢我吗？",
            "你理想中我们十年后的生活是什么样的？",
            "你第一次见我时，真实的想法是什么？",
            "到现在为止，你最想跟我一起去的地方是哪里？",
            "你最喜欢我的哪个缺点？",
            "如果给我一次重新追你的机会，你想让我怎么做？",
            "你有没有哪次偷偷为我骄傲过？因为什么？",
            "我身上哪个习惯你最想帮我改掉？（温柔地说）",
            "你觉得我最可爱的瞬间是什么时候？",
            "如果我们吵架了，你最希望对方先说什么？",
            "你心里有没有一句一直想说却没说出口的情话？",
            "最近一次觉得「有 TA 真好」是因为什么小事？",
            "你最想让我改掉的说话方式是什么？",
            "如果可以拥有我们的一条共同记忆，你选哪个？",
            "你觉得我什么时候最需要你？",
            "我们的感情里你最想守住的一件事是什么？",
            "你有没有为了迁就我而委屈过自己？",
            "你最希望我在你家人朋友面前怎么做？",
            "你有没有什么时候特别想我但没说？",
            "如果给我们的关系加一条新规矩，你会加什么？",
            "你觉得我们最大的默契是什么？",
            "你希望我们的下一个纪念日怎么过？",
            "现在，最想对我说的一句话是什么？（不许编辑）"
    );

    // ========== F68 心灵感应题库（24 题，答案从选项里选才可判同） ==========

    private static final Object[][] TELEPATHY_QUESTIONS = {
            {"TA 现在更想吃哪一样？（不许商量！）", new String[]{"火锅", "烧烤", "奶茶", "蛋糕"}},
            {"我们下次约会去哪儿？（凭直觉选）", new String[]{"电影院", "游乐园", "海边", "家里"}},
            {"今晚的月亮更像什么？", new String[]{"银币", "香蕉", "眼睛", "小船"}},
            {"你猜 TA 现在的手机电量还剩多少？", new String[]{"很多", "一半", "快没了"}},
            {"我们的爱情现在是什么颜色？", new String[]{"粉色", "蓝色", "黄色", "白色"}},
            {"TA 今天心情怎么样？（猜猜看）", new String[]{"很好", "一般", "有点累"}},
            {"现在这个时刻，TA 最想做什么？", new String[]{"抱我", "睡觉", "吃好吃的"}},
            {"如果送 TA 一束花，选哪种？", new String[]{"玫瑰", "向日葵", "满天星", "郁金香"}},
            {"你觉得 TA 最喜欢你的哪个部位？", new String[]{"眼睛", "笑容", "手", "头发"}},
            {"我们合租的第一只宠物叫什么？", new String[]{"团团", "豆豆", "年糕", "布丁"}},
            {"今天最适合我们的两个字是？", new String[]{"贴贴", "抱抱", "约会", "躺平"}},
            {"TA 最近单曲循环的歌是情歌吗？", new String[]{"是", "不是", "没在循环"}},
            {"我们的下一个纪念日你想怎么过？", new String[]{"大餐", "旅行", "宅家", "惊喜盲盒"}},
            {"TA 最怕的小东西是什么？", new String[]{"蟑螂", "打针", "蛇", "不怕"}},
            {"现在给对方发一个表情，TA 会发哪个回来？", new String[]{"😘", "🤗", "😝", "🥰"}},
            {"我们的爱情像什么天气？", new String[]{"晴天", "小雨", "多云转晴", "彩虹"}},
            {"TA 睡前最后一件事是什么？", new String[]{"看手机", "亲我", "喝水", "发呆"}},
            {"你最想现在收到 TA 的什么？", new String[]{"语音", "拥抱", "奶茶", "情话"}},
            {"我们的下一个五年会住在哪里？", new String[]{"现在的城市", "海边", "老家", " wherever 有你"}},
            {"TA 的口头禅是什么？（三选一）", new String[]{"好呀", "随便", "爱你"}},
            {"此刻最想让 TA 对你说的一句话？", new String[]{"我爱你", "辛苦了", "我懂你"}},
            {"我们的爱情保鲜秘诀是什么？", new String[]{"多夸夸", "多贴贴", "常约会", "不冷战"}},
            {"如果爱情有味道，我们的像？", new String[]{"草莓", "巧克力", "柠檬", "白开水"}},
            {"今天最想跟 TA 一起做的一件小事？", new String[]{"散步", "看剧", "做饭", "早睡"}}
    };

    // ========== 读取与抽取 ==========

    /** 安慰话术卡：按感受随机取 count 条。 */
    public static List<String> comfortWords(String feeling, int count) {
        List<String> pool = switch (feeling) {
            case CoupleComfort.FEELING_SAD -> COMFORT_SAD;
            case CoupleComfort.FEELING_WRONGED -> COMFORT_WRONGED;
            case CoupleComfort.FEELING_TIRED -> COMFORT_TIRED;
            case CoupleComfort.FEELING_ANXIOUS -> COMFORT_ANXIOUS;
            case CoupleComfort.FEELING_EMO -> COMFORT_EMO;
            default -> COMFORT_SAD;
        };
        return pool.stream().collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(),
                list -> {
                    java.util.Collections.shuffle(list, ThreadLocalRandom.current());
                    return list.stream().limit(count).toList();
                }));
    }

    /** 深夜关怀文案。 */
    public static String nightCareLine() {
        return NIGHT_CARE_GENERIC;
    }

    /** 陪聊话题卡：随机 count 条。 */
    public static List<String> chatTopics(int count) {
        return CHAT_TOPICS.stream().collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(),
                list -> {
                    java.util.Collections.shuffle(list, ThreadLocalRandom.current());
                    return list.stream().limit(count).toList();
                }));
    }

    /** 真心话题目：按空间+天稳定一题（同一天双方同一题）。 */
    public static String pickTruth(String spaceId, String day) {
        return TRUTH_QUESTIONS.get(Math.floorMod(stableHash(spaceId + "|truth|" + day), TRUTH_QUESTIONS.size()));
    }

    /** 随机一轮心灵感应题，返回 {question, options}。 */
    public static Object[] randomTelepathy() {
        return TELEPATHY_QUESTIONS[ThreadLocalRandom.current().nextInt(TELEPATHY_QUESTIONS.length)];
    }

    /** 按题面查选项（心灵感应作答界面用），找不到返回空数组。 */
    public static String[] telepathyOptions(String question) {
        for (Object[] row : TELEPATHY_QUESTIONS) {
            if (row[0].equals(question)) {
                return (String[]) row[1];
            }
        }
        return new String[0];
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
