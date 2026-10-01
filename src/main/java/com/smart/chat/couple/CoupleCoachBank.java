package com.smart.chat.couple;

import java.util.List;
import java.util.Map;

/**
 * 成长系内容库（F152/F154/F156 静态库，只增不改顺序）：
 * 情绪颗粒度词表、共读一分钟短文、早安能量站。抽取用 {@link CoupleRitualBank#stableHash}（FNV-1a）。
 */
public final class CoupleCoachBank {

    private CoupleCoachBank() {
    }

    // ========== F152 情绪颗粒度词表（5 族 × 8 词，只增不改顺序） ==========

    public record FeelFamily(String family, String emoji, List<String> words) {
    }

    private static final List<FeelFamily> FEEL_FAMILIES = List.of(
            new FeelFamily("开心族", "🌈", List.of("雀跃", "满足", "心动", "治愈", "踏实", "骄傲", "松弛", "期待")),
            new FeelFamily("低落族", "🌧️", List.of("委屈", "失落", "怅然", "疲惫", "孤单", "无力", "心酸", "委屈巴巴")),
            new FeelFamily("焦虑族", "🌪️", List.of("忐忑", "紧张", "担忧", "烦躁", "不安", "慌张", "压力山大", "心神不宁")),
            new FeelFamily("生气族", "🔥", List.of("恼火", "失望", "不服气", "被冒犯", "憋屈", "心寒", "着急上火", "气鼓鼓")),
            new FeelFamily("柔软族", "🫧", List.of("感动", "依恋", "心疼", "思念", "害羞", "感恩", "被理解", "被偏爱"))
    );

    // ========== F154 共读一分钟（原创短段，每段 2-3 句） ==========

    private static final List<String> PASSAGES = List.of(
            "爱不是找到一个完美的人，而是学会用完美的眼光，欣赏一个不完美的人。今天试着用欣赏的眼光看看对方吧。",
            "好的感情不是没有争吵，而是争吵之后依然选择理解。分歧是了解彼此的入口，不是感情的出口。",
            "真正的陪伴，是我在忙，但你有事我随时都在。安全感就是从这些小瞬间里一点点攒出来的。",
            "表达爱意要具体：不说「你真好」，而说「你今天记得帮我带伞，我很感动」。具体的爱更容易被接住。",
            "两个人在一起，不是变成一个人，而是两个完整的人并肩走。你先照顾好自己，才有力气照顾这段感情。",
            "爱需要练习，就像肌肉需要锻炼。每天一句肯定、一个拥抱、一次认真倾听，都是在给感情做训练。",
            "别把最坏的脾气留给最亲近的人。因为确定对方不会走，就肆意挥霍，是最可惜的浪费。",
            "亲密关系里最珍贵的不是「我们从不误会」，而是「每次误会我们都会澄清」。及时澄清，别让误会过夜。",
            "幸福不是宏大的目标，而是今天有没有好好吃一顿饭、好好说一次话。把日子过小，把爱过具体。",
            "每个人表达爱的语言不同：有人用陪伴，有人用行动。看见对方的语言，比要求对方学会自己的语言更重要。",
            "信任像一张白纸，皱了即使抚平也有痕迹。所以重要的事不隐瞒，小事也说到做到。",
            "吵架时问自己：我想赢，还是想让我们更好？想通这句话，很多架就吵不起来了。",
            "偶尔把「你怎么又这样」换成「我需要你帮我」，指责变成请求，对方会更容易靠近你。",
            "爱是动词：不是「我爱上你」的那一刻，而是「我选择爱你」的每一天。",
            "我们不必时刻黏在一起，但重要时刻一定要在场。距离产生的是想念，不是疏远。",
            "记得给彼此留一点独处的时间。最好的亲密是：在一起很甜，分开也不慌。",
            "把「谢谢」用在小事上：谢谢你来接我，谢谢你记得我不吃香菜。感恩说得越勤，爱就越存越多。",
            "对方的情绪不需要你立刻解决，有时候只需要你陪着。先接住情绪，再处理事情。",
            "长期关系靠的不是激情，而是那些「说到做到」的小承诺。靠谱是最迷人的浪漫。",
            "今天也试着做一件让对方感到被偏爱的小事吧。偏爱不用昂贵，用心就行。"
    );

    // ========== F156 早安能量站（早安语 / 今日幸运小事 / 幸运色） ==========

    public record Morning(String greeting, String luckyThing, String luckyColor) {
    }

    private static final List<Morning> MORNINGS = List.of(
            new Morning("早安！今天的太阳是专门为你们升起的。", "会遇到一件让你偷偷笑出声的小事", "暖橙色"),
            new Morning("早安，今天也请好好喜欢这个世界和对方。", "会收到一句意料之外的夸奖", "云朵白"),
            new Morning("早上好！记得给 TA 一句早安，比咖啡提神。", "午饭会特别好吃", "抹茶绿"),
            new Morning("早安！把今天的烦恼交给晚上的自己，现在只管发光。", "路上会一路绿灯", "天空蓝"),
            new Morning("早安，你比昨天的你更厉害了一点点。", "会想起一个甜甜的回忆", "樱花粉"),
            new Morning("早上好！今天适合把「我爱你」大声说出来。", "会捡到一个小幸运（比如最后的包子）", "蜂蜜黄"),
            new Morning("早安！今天的拥抱配额：不限量，随时领取。", "会顺利搞定一件拖了很久的事", "薄荷绿"),
            new Morning("早上好，愿你被这个世界温柔相待，也温柔待人。", "会听到一首刚好喜欢的歌", "薰衣草紫"),
            new Morning("早安！今天的风里有一点甜甜的消息。", "会收到 TA 的主动关心", "奶油米"),
            new Morning("早上好！记住：你已经很棒了，剩下的慢慢来。", "会睡一个好觉", "月光银"),
            new Morning("早安，今天也要理直气壮地开心哦。", "会吃到一个特别合口味的东西", "橘红色"),
            new Morning("早上好！给对方一个早安吻，今天运气值拉满。", "会发现一件值得期待的小事", "海盐蓝"),
            new Morning("早安！新的一天，也是新的机会让你更爱 TA 一点。", "会有一个灵光一闪的好点子", "晨光金"),
            new Morning("早上好！别忘了，你是被偏爱着的那个人。", "会得到一次无条件帮忙", "珊瑚粉"),
            new Morning("早安！今天宜：牵手、微笑、说废话。", "会撞见一场好看的晚霞", "晚霞紫"),
            new Morning("早上好，愿你的咖啡够浓，待办够少。", "工作/学习会超顺利", "炭灰黑"),
            new Morning("早安！你笑起来的样子，是今天最好的风景。", "会被陌生人善意对待", "青柠绿"),
            new Morning("早上好！把日子过成喜欢的样子，从今天开始。", "会完成一个小目标", "湖水青"),
            new Morning("早安！记得喝水，记得想我，记得开心。", "会省下一笔小钱", "奶茶棕"),
            new Morning("早上好！今天也在被爱着，别怀疑。", "会收到一个拥抱", "玫瑰红")
    );

    public static List<FeelFamily> feelFamilies() {
        return FEEL_FAMILIES;
    }

    /** 全部情绪词（扁平）。 */
    public static List<String> feelWords() {
        return FEEL_FAMILIES.stream().flatMap(f -> f.words().stream()).toList();
    }

    /** 今日共读段落：按 space+day 稳定。 */
    public static String pickPassage(String spaceId, String day) {
        return PASSAGES.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|read|" + day), PASSAGES.size()));
    }

    /** 今日早安能量：按 space+day 稳定。 */
    public static Morning pickMorning(String spaceId, String day) {
        return MORNINGS.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|morning|" + day), MORNINGS.size()));
    }

    /** 情绪词校验：允许词表内任意词。 */
    public static boolean isKnownFeelWord(String word) {
        return feelWords().contains(word);
    }

    /** 词所属族（用于前端着色），找不到返回 null。 */
    public static Map.Entry<String, String> familyOf(String word) {
        for (FeelFamily family : FEEL_FAMILIES) {
            if (family.words().contains(word)) {
                return Map.entry(family.family(), family.emoji());
            }
        }
        return null;
    }
}
