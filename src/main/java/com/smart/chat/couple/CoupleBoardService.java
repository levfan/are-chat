package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 我们公司（F240-F249，批次二十）：头衔任命、董事会决议、年度股东大会、升职公示栏、
 * 发薪日、金点子箱、请假式会议签到、公司名片、公司版经营周报。
 * 情绪价值设计：把两个人的小日子开成一家「公司」——封官、议事、述职、发感谢工资，
 * 用一本正经的仪式感说「我认真对待我们的每一件事」。
 */
@Service
public class CoupleBoardService {

    static final int ROLE_TITLE_MAX = 60;
    static final int VOTE_TITLE_MAX = 140;
    static final int REVIEW_MAX = 500;
    static final int GOAL_MAX = 200;
    static final int THANKS_MAX = 200;
    static final int IDEA_MAX = 140;
    static final int ROLE_PROPOSE_MAX = 2;
    static final int SALARY_POINTS = 5;
    static final long ATTEND_WINDOW_MS = 10_000L;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleBoardRoleMapper roleMapper;
    private final CoupleBoardVoteMapper voteMapper;
    private final CoupleBoardReportMapper reportMapper;
    private final CoupleBoardSalaryMapper salaryMapper;
    private final CoupleBoardIdeaMapper ideaMapper;
    private final CoupleBoardAttendMapper attendMapper;
    private final CouplePointLedgerMapper pointLedgerMapper;
    private final ImPushService push;

    public CoupleBoardService(CoupleSpaceMapper spaceMapper, CoupleBoardRoleMapper roleMapper,
                              CoupleBoardVoteMapper voteMapper, CoupleBoardReportMapper reportMapper,
                              CoupleBoardSalaryMapper salaryMapper, CoupleBoardIdeaMapper ideaMapper,
                              CoupleBoardAttendMapper attendMapper, CouplePointLedgerMapper pointLedgerMapper,
                              ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.roleMapper = roleMapper;
        this.voteMapper = voteMapper;
        this.reportMapper = reportMapper;
        this.salaryMapper = salaryMapper;
        this.ideaMapper = ideaMapper;
        this.attendMapper = attendMapper;
        this.pointLedgerMapper = pointLedgerMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record RoleVO(String id, String fromUser, String toUser, boolean mine, String title, boolean appointed) {
    }

    public record VoteVO(String id, String title, String proposer, boolean mine, String status,
                         String vetoBy, Long created, Long decidedAt, boolean canVote) {
    }

    public record ReportVO(String year, String mineReview, String mineGoal,
                           String partnerReview, String partnerGoal, boolean bothIn) {
    }

    public record SalaryVO(String month, String mineThanks, String partnerThanks, boolean bothPaid,
                           Integer payDay, Integer monthsPaid) {
    }

    public record IdeaVO(String id, String fromUser, boolean mine, String content,
                         boolean adopted, String voteId, Long created) {
    }

    public record AttendVO(String day, boolean mineAttended, boolean partnerAttended, boolean convened) {
    }

    public record MemberVO(String user, List<String> titles, int earned, String rank,
                           String nextRank, Integer pointsToNext) {
    }

    public record CardVO(List<String> lines) {
    }

    public record WeeklyVO(String week, int votes, int ideas, int pointsEarned) {
    }

    public record OverviewVO(String day, String week, List<RoleVO> roles, List<VoteVO> votes, ReportVO report,
                             SalaryVO salary, List<IdeaVO> ideas, AttendVO attend, List<MemberVO> members,
                             CardVO card, WeeklyVO weekly) {
    }

    // ========== 读：公司总览 ==========

    /** 我们公司总览（任命/决议/述职/发薪/点子/签到/职级/名片/周报一次拉齐）。 */
    public OverviewVO overview(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        String week = LocalDate.now().with(DayOfWeek.MONDAY).toString();
        List<CoupleBoardRole> roles = roleMapper.findBySpace(space.getId());
        List<CoupleBoardVote> votes = voteMapper.findBySpace(space.getId());
        List<CoupleBoardIdea> ideas = ideaMapper.findBySpace(space.getId());
        List<CoupleBoardSalary> salaries = salaryMapper.findBySpace(space.getId());
        List<CouplePointLedger> ledger = pointLedgerMapper.findBySpace(space.getId());
        return new OverviewVO(day, week, roleList(space, me, roles), voteList(space, me, votes),
                reportState(space, me), salaryState(space, me, day, salaries), ideaList(space, me, ideas),
                attendState(space, me, day), members(space, roles, ledger),
                card(space, day, roles, votes, salaries, ledger), weekly(space, week, votes, ideas, ledger));
    }

    // ========== F240 头衔任命 ==========

    /** 给 TA 封一个在家的职位（每人待任命最多 2 个）。 */
    public OverviewVO proposeRole(String me, String title) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(title, ROLE_TITLE_MAX, "职位叫什么好呢（如财政部长）");
        String partner = space.partnerOf(me);
        long pending = roleMapper.findBySpace(space.getId()).stream()
                .filter(r -> me.equals(r.getFromUser()) && !r.isAppointed()).count();
        if (pending >= ROLE_PROPOSE_MAX) {
            throw new BusinessException(400, "一次最多封 " + ROLE_PROPOSE_MAX + " 个职位，等 TA 盖章后再封");
        }
        roleMapper.insert(CoupleBoardRole.of(space.getId(), me, partner, text));
        push.pushCoupleEvent("board-role", me, partner, "TA 想封你当「" + text + "」，快去任命");
        return overview(me);
    }

    /** 被任命者点「任命」盖章生效。 */
    public OverviewVO appoint(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleBoardRole role = requireRole(space, id);
        if (!me.equals(role.getToUser())) {
            throw new BusinessException(400, "任命章要本人盖，TA 才能生效");
        }
        if (role.isAppointed()) {
            throw new BusinessException(400, "这个职位已经生效啦");
        }
        role.setAppointed(1);
        role.setUpdatedAt(System.currentTimeMillis());
        roleMapper.updateById(role);
        push.pushCoupleEvent("board-appointed", me, role.getFromUser(), "你被封的「" + role.getTitle() + "」正式上任！");
        return overview(me);
    }

    // ========== F241 董事会决议 ==========

    /** 提交一件大事提案（待对方表决）。 */
    public OverviewVO proposeVote(String me, String title) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(title, VOTE_TITLE_MAX, "议案写一件大事吧");
        voteMapper.insert(CoupleBoardVote.of(space.getId(), me, text));
        push.pushCoupleEvent("board-proposal", me, space.partnerOf(me), "董事会来议案了：" + text);
        return overview(me);
    }

    /** 表决：附议通过或一票否决（提案人不能裁自己的案）。 */
    public OverviewVO vote(String me, String id, boolean agree) {
        CoupleSpace space = requireSpace(me);
        CoupleBoardVote vote = requireVote(space, id);
        if (!vote.isPending()) {
            throw new BusinessException(400, "这个决议已经有结论了");
        }
        if (me.equals(vote.getProposer())) {
            throw new BusinessException(400, "自己的议案不能自己裁");
        }
        vote.setStatus(agree ? CoupleBoardVote.STATUS_PASSED : CoupleBoardVote.STATUS_VETOED);
        vote.setVetoBy(agree ? "" : me);
        vote.setDecidedAt(System.currentTimeMillis());
        voteMapper.updateById(vote);
        if (agree) {
            push.pushCoupleEventBoth("board-passed", me, space.getUserA(), space.getUserB(),
                    "决议全票通过✓ " + vote.getTitle());
        } else {
            push.pushCoupleEventBoth("board-vetoed", me, space.getUserA(), space.getUserB(),
                    "决议被一票否决✗ " + vote.getTitle());
        }
        return overview(me);
    }

    // ========== F242 年度股东大会 ==========

    /** 交年度述职+明年一个小目标（同年可改写；双提交互见）。 */
    public OverviewVO saveReport(String me, String year, String review, String goal) {
        CoupleSpace space = requireSpace(me);
        String y = year == null || year.isBlank() ? String.valueOf(LocalDate.now().getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年度写四位数字就行");
        }
        String text = requireText(review, REVIEW_MAX, "述职多写点也没关系");
        String target = requireText(goal, GOAL_MAX, "明年想达成什么小目标？");
        String partner = space.partnerOf(me);
        CoupleBoardReport existing = reportMapper.find(space.getId(), y, me);
        if (existing == null) {
            reportMapper.insert(CoupleBoardReport.of(space.getId(), y, me, text, target));
            if (reportMapper.find(space.getId(), y, partner) != null) {
                push.pushCoupleEventBoth("board-report-both", me, space.getUserA(), space.getUserB(),
                        y + " 年度述职：两份都交齐了，可以互相看了");
            } else {
                push.pushCoupleEvent("board-report-mine", me, partner, "TA 交了年度述职，就差你一份");
            }
        } else {
            existing.setReview(text);
            existing.setGoal(target);
            existing.setUpdatedAt(System.currentTimeMillis());
            reportMapper.updateById(existing);
        }
        return overview(me);
    }

    // ========== F244 发薪日 ==========

    /** 发本月「感谢工资」：一句感谢 + 5 积分自动入账台账（一月一次）。 */
    public OverviewVO paySalary(String me, String thanks) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(thanks, THANKS_MAX, "本月感谢工资，先说一句谢谢");
        String month = YearMonth.now().toString();
        if (salaryMapper.find(space.getId(), month, me) != null) {
            throw new BusinessException(400, "本月工资已发放，感谢留到下个月再说");
        }
        String partner = space.partnerOf(me);
        salaryMapper.insert(CoupleBoardSalary.of(space.getId(), month, me, text));
        pointLedgerMapper.insert(CouplePointLedger.of(space.getId(), me,
                CouplePointLedger.TYPE_EARN, "发薪日感谢工资", SALARY_POINTS));
        if (salaryMapper.find(space.getId(), month, partner) != null) {
            push.pushCoupleEventBoth("board-salary", me, space.getUserA(), space.getUserB(),
                    month + " 的发薪日：双方工资都到账，谢谢这一年的彼此");
        } else {
            push.pushCoupleEvent("board-salary", me, partner, "发薪日到啦，TA 已发放感谢工资");
        }
        return overview(me);
    }

    // ========== F245 金点子箱 ==========

    /** 投一条一句话经营提案。 */
    public OverviewVO addIdea(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(content, IDEA_MAX, "金点子就写一句话");
        ideaMapper.insert(CoupleBoardIdea.of(space.getId(), me, text));
        push.pushCoupleEvent("board-idea", me, space.partnerOf(me), "金点子箱来了一条：" + text);
        return overview(me);
    }

    /** 采纳 TA 的点子（自动生成董事会决议，走表决流程）。 */
    public OverviewVO adoptIdea(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleBoardIdea idea = requireIdea(space, id);
        if (me.equals(idea.getFromUser())) {
            throw new BusinessException(400, "自己的点子要对方来采纳才算数");
        }
        if (idea.isAdopted()) {
            throw new BusinessException(400, "这个点子已经转成决议了");
        }
        CoupleBoardVote vote = CoupleBoardVote.of(space.getId(), idea.getFromUser(), "金点子：" + idea.getContent());
        voteMapper.insert(vote);
        idea.setAdopted(1);
        idea.setVoteId(vote.getId());
        ideaMapper.updateById(idea);
        push.pushCoupleEventBoth("board-idea-adopted", me, space.getUserA(), space.getUserB(),
                "点子被采纳转议案：" + idea.getContent());
        return overview(me);
    }

    // ========== F247 会议签到 ==========

    /** 按键签到；10 秒窗口内双方都签到=本次会议召开（每天一次）。 */
    public OverviewVO attend(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        String partner = space.partnerOf(me);
        long now = System.currentTimeMillis();
        CoupleBoardAttend mine = attendMapper.find(space.getId(), day, me);
        if (mine == null) {
            mine = CoupleBoardAttend.of(space.getId(), day, me);
            attendMapper.insert(mine);
        } else if (!mine.isConvened()) {
            mine.setAttended(1);
            mine.setUpdatedAt(now);
            attendMapper.updateById(mine);
        }
        CoupleBoardAttend partnerRow = attendMapper.find(space.getId(), day, partner);
        if (partnerRow != null && !mine.isConvened() && !partnerRow.isConvened()
                && now - partnerRow.getUpdatedAt() <= ATTEND_WINDOW_MS) {
            mine.setConvened(1);
            partnerRow.setConvened(1);
            attendMapper.updateById(mine);
            attendMapper.updateById(partnerRow);
            push.pushCoupleEventBoth("board-convened", me, space.getUserA(), space.getUserB(),
                    "10 秒内双签到，今天的「我们公司」例会正式召开");
        }
        return overview(me);
    }

    // ========== 聚合装配（F243/F248/F249 无表） ==========

    private List<RoleVO> roleList(CoupleSpace space, String me, List<CoupleBoardRole> roles) {
        List<RoleVO> list = new ArrayList<>();
        for (CoupleBoardRole role : roles) {
            list.add(new RoleVO(role.getId(), role.getFromUser(), role.getToUser(),
                    me.equals(role.getFromUser()), role.getTitle(), role.isAppointed()));
        }
        return list;
    }

    private List<VoteVO> voteList(CoupleSpace space, String me, List<CoupleBoardVote> votes) {
        List<VoteVO> list = new ArrayList<>();
        for (CoupleBoardVote vote : votes) {
            list.add(new VoteVO(vote.getId(), vote.getTitle(), vote.getProposer(), me.equals(vote.getProposer()),
                    vote.getStatus(), vote.getVetoBy(), vote.getCreated(), vote.getDecidedAt(),
                    vote.isPending() && !me.equals(vote.getProposer())));
        }
        return list;
    }

    /** 述职：对方那份只在双提交后才可见（F242 互见规则）。 */
    private ReportVO reportState(CoupleSpace space, String me) {
        String year = String.valueOf(LocalDate.now().getYear());
        String partner = space.partnerOf(me);
        CoupleBoardReport mine = reportMapper.find(space.getId(), year, me);
        CoupleBoardReport theirs = reportMapper.find(space.getId(), year, partner);
        boolean bothIn = mine != null && theirs != null;
        return new ReportVO(year, mine == null ? null : mine.getReview(), mine == null ? null : mine.getGoal(),
                bothIn ? theirs.getReview() : null, bothIn ? theirs.getGoal() : null, bothIn);
    }

    private SalaryVO salaryState(CoupleSpace space, String me, String day, List<CoupleBoardSalary> salaries) {
        String month = day.substring(0, 7);
        CoupleBoardSalary mine = null;
        CoupleBoardSalary partner = null;
        Integer payDay = null;
        Set<String> months = new LinkedHashSet<>();
        for (CoupleBoardSalary row : salaries) {
            months.add(row.getMonth());
            if (month.equals(row.getMonth())) {
                if (row.getFromUser().equals(me)) {
                    mine = row;
                } else {
                    partner = row;
                }
                int createdDay = java.time.Instant.ofEpochMilli(row.getCreated())
                        .atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth();
                payDay = payDay == null ? createdDay : Math.min(payDay, createdDay);
            }
        }
        return new SalaryVO(month, mine == null ? null : mine.getThanks(), partner == null ? null : partner.getThanks(),
                mine != null && partner != null, payDay, months.size());
    }

    private List<IdeaVO> ideaList(CoupleSpace space, String me, List<CoupleBoardIdea> ideas) {
        List<IdeaVO> list = new ArrayList<>();
        for (CoupleBoardIdea idea : ideas) {
            list.add(new IdeaVO(idea.getId(), idea.getFromUser(), me.equals(idea.getFromUser()),
                    idea.getContent(), idea.isAdopted(), idea.getVoteId(), idea.getCreated()));
        }
        list.sort((a, b) -> Long.compare(b.created(), a.created()));
        return list;
    }

    private AttendVO attendState(CoupleSpace space, String me, String day) {
        CoupleBoardAttend mine = attendMapper.find(space.getId(), day, me);
        CoupleBoardAttend partner = attendMapper.find(space.getId(), day, space.partnerOf(me));
        boolean convened = (mine != null && mine.isConvened()) || (partner != null && partner.isConvened());
        return new AttendVO(day, mine != null, partner != null, convened);
    }

    /** 双方职级档案（F243：按累计赚分定档，头衔公示）。 */
    private List<MemberVO> members(CoupleSpace space, List<CoupleBoardRole> roles, List<CouplePointLedger> ledger) {
        List<MemberVO> list = new ArrayList<>();
        for (String user : List.of(space.getUserA(), space.getUserB())) {
            List<String> titles = roles.stream()
                    .filter(r -> user.equals(r.getToUser()) && r.isAppointed())
                    .map(CoupleBoardRole::getTitle).toList();
            int earned = ledger.stream()
                    .filter(l -> user.equals(l.getFromUser()) && CouplePointLedger.TYPE_EARN.equals(l.getType()))
                    .mapToInt(l -> l.getPoints() == null ? 0 : l.getPoints()).sum();
            list.add(new MemberVO(user, titles, earned, CoupleBoardBank.rank(earned),
                    CoupleBoardBank.nextRank(earned), CoupleBoardBank.pointsToNext(earned)));
        }
        return list;
    }

    /** F248 公司名片：职位+职级+决议数+发薪日自动拼一张文字名片。 */
    private CardVO card(CoupleSpace space, String day, List<CoupleBoardRole> roles, List<CoupleBoardVote> votes,
                        List<CoupleBoardSalary> salaries, List<CouplePointLedger> ledger) {
        List<String> lines = new ArrayList<>();
        lines.add("【我们公司】" + day);
        for (String user : List.of(space.getUserA(), space.getUserB())) {
            int earned = ledger.stream()
                    .filter(l -> user.equals(l.getFromUser()) && CouplePointLedger.TYPE_EARN.equals(l.getType()))
                    .mapToInt(l -> l.getPoints() == null ? 0 : l.getPoints()).sum();
            String titles = roles.stream().filter(r -> user.equals(r.getToUser()) && r.isAppointed())
                    .map(CoupleBoardRole::getTitle).reduce((a, b) -> a + "、" + b).orElse("暂无头衔");
            lines.add(user + "：" + CoupleBoardBank.rank(earned) + "｜" + titles);
        }
        long passed = votes.stream().filter(v -> CoupleBoardVote.STATUS_PASSED.equals(v.getStatus())).count();
        long vetoed = votes.stream().filter(v -> CoupleBoardVote.STATUS_VETOED.equals(v.getStatus())).count();
        lines.add("议事：通过 " + passed + " 项，否决 " + vetoed + " 项");
        Integer payDay = null;
        String month = day.substring(0, 7);
        for (CoupleBoardSalary row : salaries) {
            if (month.equals(row.getMonth())) {
                int d = java.time.Instant.ofEpochMilli(row.getCreated())
                        .atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth();
                payDay = payDay == null ? d : Math.min(payDay, d);
            }
        }
        lines.add(payDay == null ? "发薪日：随缘，感谢随时到账" : "发薪日：每月 " + payDay + " 号");
        lines.add(CoupleBoardBank.cardTail(space.getId()));
        return new CardVO(lines);
    }

    /** F249 公司版经营周报：本周决议/点子/赚分。 */
    private WeeklyVO weekly(CoupleSpace space, String week, List<CoupleBoardVote> votes,
                            List<CoupleBoardIdea> ideas, List<CouplePointLedger> ledger) {
        long weekStart = LocalDate.parse(week).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        int voteCount = (int) votes.stream().filter(v -> v.getCreated() != null && v.getCreated() >= weekStart).count();
        int ideaCount = (int) ideas.stream().filter(i -> i.getCreated() != null && i.getCreated() >= weekStart).count();
        int earned = ledger.stream()
                .filter(l -> l.getCreated() != null && l.getCreated() >= weekStart
                        && CouplePointLedger.TYPE_EARN.equals(l.getType()))
                .mapToInt(l -> l.getPoints() == null ? 0 : l.getPoints()).sum();
        return new WeeklyVO(week, voteCount, ideaCount, earned);
    }

    // ========== 通用 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private CoupleBoardRole requireRole(CoupleSpace space, String id) {
        CoupleBoardRole role = roleMapper.selectById(id);
        if (role == null || !role.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这个职位任命不存在");
        }
        return role;
    }

    private CoupleBoardVote requireVote(CoupleSpace space, String id) {
        CoupleBoardVote vote = voteMapper.selectById(id);
        if (vote == null || !vote.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这个决议不存在");
        }
        return vote;
    }

    private CoupleBoardIdea requireIdea(CoupleSpace space, String id) {
        CoupleBoardIdea idea = ideaMapper.selectById(id);
        if (idea == null || !idea.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这个点子不存在");
        }
        return idea;
    }

    private String requireText(String value, int max, String emptyMsg) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, emptyMsg);
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BusinessException(400, "最多 " + max + " 个字，心意不在字数");
        }
        return trimmed;
    }
}
