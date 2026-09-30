package com.smart.chat.couple;

import java.util.List;

/**
 * 沟通增强内容库（F100/F103/F106/F109，批次六静态库，只增不改顺序）：
 * 恋爱翻译词典、你比划我猜词库、情话合成模板与素材、晚安电台文案。
 * 抽取用 {@link CoupleRitualBank#stableHash}（FNV-1a）保证稳定。
 */
public final class CoupleChatBank {

    private CoupleChatBank() {
    }

    // ========== F100 恋爱翻译词典：短语 → {潜台词, 建议回应} ==========

    /** 一条翻译：短语 + 潜台词 + 建议回应。 */
    public record Translation(String phrase, String subtext, String reply) {
    }

    private static final List<Translation> TRANSLATIONS = List.of(
            new Translation("哦", "不是没话说，是有点小失落，想被哄一下",
                    "「感觉你有点不开心？抱抱，跟我说说嘛 🫂」"),
            new Translation("嗯", "在听，但想让你多说一点点",
                    "「再多说两句嘛，我想听完整的版本～」"),
            new Translation("嗯嗯", "乖巧应答中，其实心情不错",
                    "「你今天回得很快哦，开心！」"),
            new Translation("我没事", "有事，而且想让你再问一次",
                    "「我听着呢，你想说的时候我都在 🌙」"),
            new Translation("随便", "不是无所谓，是想看你会不会懂我",
                    "「那我安排啦：先吃你上次惦记的那家，好吗？」"),
            new Translation("都行", "想要你拿主意，然后夸你安排得好",
                    "「听你的！我选了个我觉得你会喜欢的～」"),
            new Translation("在忙", "真的忙，但希望你说句『不急，我等你』",
                    "「不急，你先忙，我在这儿等你 💛」"),
            new Translation("别管我", "管我！现在就需要被管",
                    "「偏要管。给我 5 分钟，说完就走 🫶」"),
            new Translation("睡吧", "有点难过，想听你说『再陪我一会儿』",
                    "「再陪我 10 分钟好不好？就 10 分钟～」"),
            new Translation("睡了", "还不想睡，在等你挽留",
                    "「那我们一起躺下，语音陪你入睡 🌙」"),
            new Translation("呵呵", "生气预警！这是最危险的语气词",
                    "「糟糕，我好像说错话了。重新说一遍好不好？」"),
            new Translation("你开心就好", "不开心，但不想解释，想要台阶",
                    "「你开心我才开心。我们聊聊刚才的事？」"),
            new Translation("随你", "随你=你在意我，我就都行",
                    "「那我在意你，所以认真选了 A，理由是…」"),
            new Translation("滚", "口头禅式撒娇，或真的生气了，先道歉再确认",
                    "「对不起对不起，我错了。滚回来抱你可以吗 🥺」"),
            new Translation("不想说话", "想说，但需要你先开口抱抱",
                    "「那我们安静待 5 分钟，我陪着你 🤫」"),
            new Translation("挺好的", "其实没那么好，想被看穿",
                    "「真的吗？我怎么觉得差一点什么，说说看？」"),
            new Translation("不用了", "用！请再主动一次",
                    "「那我再主动一次：这周末我请你，怎么样？」"),
            new Translation("收到", "公务式回复，想你打破砂锅",
                    "「收到请回复：今天想我没有？🥰」"),
            new Translation("哦哦", "知道了，但话题有点无聊，求换台",
                    "「换个有趣的话题：我今天遇到一件超好笑的事…」"),
            new Translation("早点睡", "想让你说『等你一起睡』",
                    "「等你一起睡，10 分钟后语音见 🌙」"),
            new Translation("在吗", "想你了，但只用两个字起步",
                    "「在！一直都在。怎么啦，想我啦？😄」"),
            new Translation("吃了吗", "想开启聊天，等你接一个长长的话题",
                    "「吃了，但你没说和我吃的是什么，罚你描述一下！」"),
            new Translation("我错了", "道歉是开始，想听你说『没关系』",
                    "「没关系，抱抱。下次我们换个方式说？」"),
            new Translation("你要这么想我也没办法", "经典高危句！翻译：我委屈但我不会说",
                    "「我可能误会了，你能从头告诉我你的想法吗 🙏」")
    );

    // ========== F103 你比划我猜词库 ==========

    private static final List<String> GUESS_WORDS = List.of(
            "小猪佩奇", "女朋友", "吃火锅", "熬夜", "买买买", "奶茶", "哄睡", "亲亲",
            "吵架", "看电影", "异地恋", "纪念日", "玫瑰", "微信", "红包", "撒娇",
            "加班", "减肥", "广场舞", "奶茶三分糖", "老公", "手机没电", "下雨没带伞",
            "猫", "生日蛋糕", "合并报表", "迟到的约会", "早八", "外卖", "健身",
            "双十一", "压马路", "牵手", "流星", "哇塞", "咱妈"
    );

    // ========== F106 情话合成器：模板 + 素材 ==========

    private static final List<String> SWEET_TEMPLATES = List.of(
            "我攒了三句话：第一句想你，第二句还是很想你，第三句是%noun%帮我说的：也想你。",
            "你是我%time%里最亮的那颗星，别的星星都只是背景板。",
            "如果可爱需要收费，那你一出生就是%noun%了。",
            "我的人生规划：前半段遇见你，后半段%verb%你。",
            "今天的风很温柔，像你%noun%时候的样子。",
            "别人问我最近在忙什么，我说在%verb%，忙着你。",
            "你是我的%time%，是我的例外，是我所有%adjective%的来源。",
            "想把你做成%noun%含在嘴里，甜到别人牙疼。",
            "我对你的喜欢，比%time%还要长久一点。",
            "你负责%verb%，我负责爱你，分工明确，永不跳槽。"
    );

    private static final List<String> SWEET_NOUNS = List.of("小熊", "月亮", "晚风", "糖", "春天", "运气", "宝藏", "小星球");
    private static final List<String> SWEET_VERBS = List.of("亲亲", "抱抱", "照顾", "研究", "宠着", "粘着");
    private static final List<String> SWEET_TIMES = List.of("余生", "四季", "每个清晨", "所有深夜", "漫长岁月");
    private static final List<String> SWEET_ADJECTIVES = List.of("快乐", "心动", "温柔", "底气", "好运");

    // ========== F109 晚安电台文案（歌单为空时的兜底晚安语） ==========

    private static final List<String> RADIO_LINES = List.of(
            "今晚的电波里只有一首歌的名字：《想你的夜》🎧 晚安，梦里见。",
            "晚安电台：今夜播送星河一片，收听人：你。电台台长：想你的我 🌙",
            "本台提醒：睡前记得想我三秒，有助于做甜甜的梦 ✨ 晚安。",
            "晚安电台正在播放：《全世界最可爱的人睡着了》，演唱者：我 💛",
            "今天也在想你，按次数收费的话我该破产了。晚安，免费爱你到天亮 🌙",
            "夜深了，把今天的烦恼打包扔掉，明天的快乐我帮你保管 🎁 晚安。"
    );

    // ========== 抽取方法 ==========

    public static List<Translation> translations() {
        return TRANSLATIONS;
    }

    /** 恋爱翻译：命中短语则给潜台词；未命中给通用解读。 */
    public static Translation translate(String text) {
        String t = text == null ? "" : text.trim();
        for (Translation tr : TRANSLATIONS) {
            if (tr.phrase().equals(t)) {
                return tr;
            }
        }
        // 包含匹配（比如「我没事啦」）
        for (Translation tr : TRANSLATIONS) {
            if (t.contains(tr.phrase())) {
                return tr;
            }
        }
        return new Translation(t.isBlank() ? "……" : t,
                "这句话没有固定潜台词，但 TA 的情绪值得被认真对待",
                "「刚刚那句话，我想听你展开讲讲，我很在意 💛」");
    }

    public static List<String> guessWords() {
        return GUESS_WORDS;
    }

    /** 按空间+天+序号稳定抽词（同一轮双方看到同一词）。 */
    public static String pickGuessWord(String spaceId, String day, int seq) {
        return GUESS_WORDS.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|guess|" + day + "|" + seq), GUESS_WORDS.size()));
    }

    /** 情话合成：seed 可由前端随机传入，同 seed 结果稳定。 */
    public static String synthSweet(String spaceId, long seed) {
        int h = Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|sweet|" + seed), 1_000_003);
        String tpl = SWEET_TEMPLATES.get(h % SWEET_TEMPLATES.size());
        String noun = SWEET_NOUNS.get((h / 7 + 1) % SWEET_NOUNS.size());
        String verb = SWEET_VERBS.get((h / 13 + 3) % SWEET_VERBS.size());
        String time = SWEET_TIMES.get((h / 17 + 5) % SWEET_TIMES.size());
        String adj = SWEET_ADJECTIVES.get((h / 23 + 7) % SWEET_ADJECTIVES.size());
        return tpl.replace("%noun%", noun).replace("%verb%", verb)
                .replace("%time%", time).replace("%adjective%", adj);
    }

    /** 晚安电台兜底文案：按空间+天稳定。 */
    public static String pickRadioLine(String spaceId, String day) {
        return RADIO_LINES.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|radio|" + day), RADIO_LINES.size()));
    }
}
