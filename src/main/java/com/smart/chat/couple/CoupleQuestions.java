package com.smart.chat.couple;

import java.time.LocalDate;
import java.util.List;

/**
 * 今日一问题库：内置的情侣深度对话（deep talk）题库，按日期轮换（同一天全球同一题）。
 * <p>题目按主题分组整理，共 11 个主题 105 题；轮换天然形成「主题周」——
 * 连续若干天聚焦同一主题，适合情侣每晚睡前聊一个。取题按 epochDay 对题库长度取模，
 * 题库只增不改既有题目顺序。
 */
public final class CoupleQuestions {

    private CoupleQuestions() {
    }

    /** 一道题 = 主题 + 题面。 */
    public record BankQuestion(String topic, String text) {
    }

    private static final List<BankQuestion> BANK = List.of(
            // ── 一、重新认识彼此 ──
            new BankQuestion("重新认识彼此", "如果用三个词形容你自己，你会选哪三个？为什么是它们？"),
            new BankQuestion("重新认识彼此", "你最近一次感到特别开心，是因为什么小事？"),
            new BankQuestion("重新认识彼此", "有什么兴趣爱好是你一直想尝试、但还没开始的？"),
            new BankQuestion("重新认识彼此", "你觉得自己身上哪个优点，最容易被别人忽略？"),
            new BankQuestion("重新认识彼此", "如果可以拥有一种超能力，你会选什么？想用它做什么？"),
            new BankQuestion("重新认识彼此", "你最喜欢自己性格里的哪一点？"),
            new BankQuestion("重新认识彼此", "你手机里最舍不得删的一张照片是什么？背后有什么故事？"),
            new BankQuestion("重新认识彼此", "你最近在单曲循环的歌是什么？它让你想起什么？"),
            new BankQuestion("重新认识彼此", "如果给你一天完全属于自己的时间，你会怎么安排？"),
            new BankQuestion("重新认识彼此", "你希望别人第一次认识你时，对你留下什么印象？"),

            // ── 二、过去与成长 ──
            new BankQuestion("过去与成长", "童年里最让你怀念的一个瞬间是什么？"),
            new BankQuestion("过去与成长", "小时候的你梦想成为什么样的人？现在离它更近了还是更远了？"),
            new BankQuestion("过去与成长", "有没有一句话、一本书或一部电影，曾经真正改变过你？"),
            new BankQuestion("过去与成长", "你的名字有什么故事？你喜欢它吗？"),
            new BankQuestion("过去与成长", "成长过程中谁对你的影响最大？TA 教会了你什么？"),
            new BankQuestion("过去与成长", "你经历过最难熬的一段日子是怎么走过来的？"),
            new BankQuestion("过去与成长", "如果能回到过去对 18 岁的自己说一句话，你想说什么？"),
            new BankQuestion("过去与成长", "有没有什么是你以前深信不疑、现在却动摇了的？"),
            new BankQuestion("过去与成长", "你第一次离开家独自生活时，是什么感觉？"),
            new BankQuestion("过去与成长", "你身上有没有一个「从父母那里继承来的习惯」？你喜欢它吗？"),

            // ── 三、价值观与金钱观 ──
            new BankQuestion("价值观与金钱观", "对你来说，「有钱」意味着什么？多少钱算够？"),
            new BankQuestion("价值观与金钱观", "你觉得钱更应该用来享受当下，还是为未来储蓄？"),
            new BankQuestion("价值观与金钱观", "如果中了五百万，你的第一笔钱会花在哪里？"),
            new BankQuestion("价值观与金钱观", "你愿意为了高薪去做一份不喜欢的工作吗？底线在哪？"),
            new BankQuestion("价值观与金钱观", "你怎么看情侣之间的钱应该怎么管？aa、共同账户还是别的？"),
            new BankQuestion("价值观与金钱观", "事业和家庭发生冲突时，你会怎么权衡？"),
            new BankQuestion("价值观与金钱观", "你最不能忍受别人身上的什么品质？"),
            new BankQuestion("价值观与金钱观", "有没有一件小事，你觉得自己永远不会让步？"),
            new BankQuestion("价值观与金钱观", "你觉得一个人的「成功」应该由什么来定义？"),
            new BankQuestion("价值观与金钱观", "如果明天是世界末日，你今天会做什么？"),

            // ── 四、爱情观与我们 ──
            new BankQuestion("爱情观与我们", "你觉得「爱一个人」和「喜欢一个人」的区别是什么？"),
            new BankQuestion("爱情观与我们", "你理想中的爱情，十年后是什么样子？"),
            new BankQuestion("爱情观与我们", "你是怎么确定喜欢我的？哪个瞬间让你心动？"),
            new BankQuestion("爱情观与我们", "你觉得我们最像哪一对荧幕情侣？为什么？"),
            new BankQuestion("爱情观与我们", "爱情里你最看重什么：陪伴、理解、激情，还是别的？"),
            new BankQuestion("爱情观与我们", "你觉得恋爱中的仪式感重要吗？哪些时刻必须有仪式感？"),
            new BankQuestion("爱情观与我们", "如果用一个比喻来形容我们的关系，你会用什么？"),
            new BankQuestion("爱情观与我们", "你觉得我们在一起后，你最大的变化是什么？"),
            new BankQuestion("爱情观与我们", "你希望我表达不满时更直接一点，还是委婉一点？"),
            new BankQuestion("爱情观与我们", "如果给我们的爱情写一句标语，你会写什么？"),

            // ── 五、情绪与安全感 ──
            new BankQuestion("情绪与安全感", "你难过的时候，最希望我做什么、不做什么？"),
            new BankQuestion("情绪与安全感", "你生气时是希望被哄，还是想先自己静静？"),
            new BankQuestion("情绪与安全感", "什么事情最容易让你没有安全感？我能做点什么？"),
            new BankQuestion("情绪与安全感", "你上一次对我撒娇（或想撒娇但忍住了）是什么时候？"),
            new BankQuestion("情绪与安全感", "我们吵架时，你觉得最伤人的话是什么？以后避开它好吗？"),
            new BankQuestion("情绪与安全感", "你什么时候会觉得「有 TA 真好」？"),
            new BankQuestion("情绪与安全感", "你有什么压力是不太愿意告诉我、怕我担心的？"),
            new BankQuestion("情绪与安全感", "如果我心情低落却假装没事，你希望我怎么发现？"),
            new BankQuestion("情绪与安全感", "在我们之间，你希望「道歉」和「讲道理」哪个先来？"),
            new BankQuestion("情绪与安全感", "有什么话是你想告诉我、却一直没找到合适时机的？"),

            // ── 六、未来与共同规划 ──
            new BankQuestion("未来与共同规划", "你想在哪里安家？一座什么样的城市？"),
            new BankQuestion("未来与共同规划", "你理想中的家是什么样子？阳台、书房还是大厨房？"),
            new BankQuestion("未来与共同规划", "你想要孩子吗？想给孩子怎样的成长环境？"),
            new BankQuestion("未来与共同规划", "五年后的我们，你觉得应该在做什么？"),
            new BankQuestion("未来与共同规划", "你希望我们老了以后，过着怎样的生活？"),
            new BankQuestion("未来与共同规划", "如果可以去世界上任何一个地方定居一年，你选哪里？"),
            new BankQuestion("未来与共同规划", "你希望我们的婚礼（如果办的话）是什么风格？"),
            new BankQuestion("未来与共同规划", "你觉得我们会因为什么事吵得最凶？先聊聊怎么预防？"),
            new BankQuestion("未来与共同规划", "有什么是你想在 35 岁前完成的？我可以怎么帮你？"),
            new BankQuestion("未来与共同规划", "如果我们八十岁还牵着彼此的手，你觉得那时我们在聊什么？"),

            // ── 七、生活与习惯磨合 ──
            new BankQuestion("生活与习惯磨合", "你是早睡派还是熬夜派？希望我怎么配合你的作息？"),
            new BankQuestion("生活与习惯磨合", "家务你希望怎么分工？哪件你最讨厌、哪件其实不介意？"),
            new BankQuestion("生活与习惯磨合", "恋爱后你能接受保有多少「独处时间」？"),
            new BankQuestion("生活与习惯磨合", "你喜欢周末宅家还是出门？我们怎么轮流迁就彼此？"),
            new BankQuestion("生活与习惯磨合", "旅行时你是做攻略派还是随缘派？"),
            new BankQuestion("生活与习惯磨合", "你的「雷区」习惯有哪些？我先坦白我的，好吗？"),
            new BankQuestion("生活与习惯磨合", "你希望我们的纪念日怎么过？每年都要惊喜吗？"),
            new BankQuestion("生活与习惯磨合", "朋友聚会和家庭聚会之间，你怎么分配时间？"),
            new BankQuestion("生活与习惯磨合", "你觉得情侣之间应该完全透明吗？手机可以互相看吗？"),
            new BankQuestion("生活与习惯磨合", "如果生活习惯完全冲突，你倾向改变自己、改变对方还是找折中？"),

            // ── 八、梦想与心愿清单 ──
            new BankQuestion("梦想与心愿清单", "你的「人生愿望清单」上排第一的是什么？"),
            new BankQuestion("梦想与心愿清单", "有没有一件你一直想做、却怕被人笑话的事？"),
            new BankQuestion("梦想与心愿清单", "如果不考虑钱和能力，你最想从事什么职业？"),
            new BankQuestion("梦想与心愿清单", "你想学一样什么新技能？我们一起学怎么样？"),
            new BankQuestion("梦想与心愿清单", "你最想和我们一起完成的一件事是什么？"),
            new BankQuestion("梦想与心愿清单", "你有没有特别想再见一面的人？为什么是 TA？"),
            new BankQuestion("梦想与心愿清单", "如果可以去任何一个时代生活一个月，你选哪个？"),
            new BankQuestion("梦想与心愿清单", "退休前你最想打卡的十个地方，先说三个？"),
            new BankQuestion("梦想与心愿清单", "你想给这个世界留下什么？作品、影响，还是被某些人记住？"),
            new BankQuestion("梦想与心愿清单", "如果为「我们」设立一个年度目标，你想定什么？"),

            // ── 九、脑洞与趣味深聊 ──
            new BankQuestion("脑洞与趣味深聊", "如果我们能互换身体一天，你最想体验我生活的哪一部分？"),
            new BankQuestion("脑洞与趣味深聊", "给你一次问我任何问题、我必须绝对诚实的机会，你想问什么？"),
            new BankQuestion("脑洞与趣味深聊", "如果我们是一只动物组合，你是什么、我是什么？"),
            new BankQuestion("脑洞与趣味深聊", "你觉得我们的相遇，是巧合还是命中注定？"),
            new BankQuestion("脑洞与趣味深聊", "如果我们的爱情是一部电影，片名会是什么？谁是主角？"),
            new BankQuestion("脑洞与趣味深聊", "一百年后，你希望后人怎么讲述我们的故事？"),
            new BankQuestion("脑洞与趣味深聊", "如果记忆可以存档，你最想永久保存我们在一起的哪个瞬间？"),
            new BankQuestion("脑洞与趣味深聊", "如果今天可以违反一条规则且不会被发现，你会违反什么？"),
            new BankQuestion("脑洞与趣味深聊", "你觉得平行世界的另一个你，现在在做什么？会羡慕你吗？"),
            new BankQuestion("脑洞与趣味深聊", "如果给十年后的我们写一封信，你想在里面许下什么约定？"),

            // ── 十、关系复盘与承诺 ──
            new BankQuestion("关系复盘与承诺", "这段时间里，你觉得我们做得最好的一件事是什么？"),
            new BankQuestion("关系复盘与承诺", "你觉得我们最近一次不开心（或冷战）的根本原因是什么？"),
            new BankQuestion("关系复盘与承诺", "有什么误会，是你一直想找我澄清的？今天说开好吗？"),
            new BankQuestion("关系复盘与承诺", "你觉得我最近有什么变化？你希望我保持或改掉什么？"),
            new BankQuestion("关系复盘与承诺", "如果满分 100，你给我们现在的感情打多少分？扣的分在哪？"),
            new BankQuestion("关系复盘与承诺", "下个月，你想为我们的关系主动做一件什么事？"),
            new BankQuestion("关系复盘与承诺", "你希望我们每年做一次「关系年度总结」吗？你会先说什么？"),
            new BankQuestion("关系复盘与承诺", "有什么事是你一直想感谢我、却不好意思说的？"),
            new BankQuestion("关系复盘与承诺", "我最想让你改掉的一个小习惯和保留的一个小习惯，你觉得是什么？"),
            new BankQuestion("关系复盘与承诺", "现在看着我的眼睛，说一件你想对我承诺一辈子的事。"),

            // ── 十一、深夜电台 ──
            new BankQuestion("深夜电台", "你觉得人这一生，最重要的是什么？"),
            new BankQuestion("深夜电台", "你害怕变老吗？你害怕孤独吗？"),
            new BankQuestion("深夜电台", "有没有哪一刻，你突然意识到父母老了？"),
            new BankQuestion("深夜电台", "如果人生可以重来，你会做哪些不一样的选择？"),
            new BankQuestion("深夜电台", "你希望我记住现在的你的什么样子？"),
            new BankQuestion("深夜电台", "你觉得自己被谁真正「看见」过吗？包括我吗？"),
            new BankQuestion("深夜电台", "如果有来生，你还愿意遇见我吗？希望以什么方式？"),
            new BankQuestion("深夜电台", "你希望离开这个世界的时候，身边是什么样子？"),
            new BankQuestion("深夜电台", "深夜睡不着的时候，你通常在想什么？"),
            new BankQuestion("深夜电台", "如果此刻许一个只能对我们俩生效的愿望，你许什么？")
    );

    /** 按日期取题：同一天恒定，不同天轮换；连续取题天然按主题分组轮播。 */
    public static BankQuestion pick(String day) {
        LocalDate date;
        try {
            date = LocalDate.parse(day);
        } catch (Exception e) {
            date = LocalDate.now();
        }
        return BANK.get((int) Math.floorMod(date.toEpochDay(), BANK.size()));
    }

    public static int size() {
        return BANK.size();
    }
}
