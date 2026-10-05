package com.smart.chat.couple.api;

import com.smart.chat.couple.domain.question.QuestionAnswerRepository;
import com.smart.chat.couple.domain.streak.StreakDayRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * F45 管理看板：情侣空间运营总览（仅管理员）。
 * 统计项只吃仍在线的功能：互动打卡天数与每日一问回答数（2026-10-05 二轮裁剪后
 * 贴贴动作与好事簿已随功能下线，原来的两个计数换成这两个）。
 */
@RestController
@RequestMapping("/api/couple/admin")
public class CoupleAdminController {

    public record CoupleStatsVO(long activeSpaces, long dissolvedSpaces, long avgDays, long totalCheckinDays,
                                long totalAnswers, long spacesCreatedThisMonth) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final StreakDayRepository streakDayRepository;
    private final QuestionAnswerRepository answerRepository;
    private final AccountDirectory accounts;

    @SuppressWarnings("java:S107")
    public CoupleAdminController(CoupleSpaceRepository spaceRepository,
                                 StreakDayRepository streakDayRepository,
                                 QuestionAnswerRepository answerRepository, AccountDirectory accounts) {
        this.spaceRepository = spaceRepository;
        this.streakDayRepository = streakDayRepository;
        this.answerRepository = answerRepository;
        this.accounts = accounts;
    }

    /** 情侣空间运营统计（仅管理员）。 */
    @GetMapping("/stats")
    public ApiResponse<CoupleStatsVO> stats(HttpSession session) {
        String me = Sessions.requireUser(session);
        try {
            accounts.requireAdmin(me);
        } catch (BusinessException e) {
            throw new BusinessException(403, "仅管理员可查看运营看板");
        }
        List<CoupleSpace> all = spaceRepository.findAll();
        long active = all.stream().filter(CoupleSpace::isActive).count();
        long dissolved = all.size() - active;
        LocalDate now = LocalDate.now();
        long avgDays = active == 0 ? 0
                : (long) all.stream()
                        .filter(CoupleSpace::isActive)
                        .mapToLong(s -> {
                            LocalDate start;
                            try {
                                start = LocalDate.parse(s.anniversary());
                            } catch (Exception e) {
                                start = Instant.ofEpochMilli(s.created()).atZone(ZoneId.systemDefault()).toLocalDate();
                            }
                            return Math.max(ChronoUnit.DAYS.between(start, now) + 1, 1);
                        })
                        .average().orElse(0);
        String monthPrefix = now.toString().substring(0, 7);
        long createdThisMonth = all.stream()
                .filter(s -> Instant.ofEpochMilli(s.created()).atZone(ZoneId.systemDefault())
                        .toLocalDate().toString().startsWith(monthPrefix))
                .count();
        return ApiResponse.ok(new CoupleStatsVO(active, dissolved, avgDays,
                streakDayRepository.countAll(), answerRepository.countAll(), createdThisMonth));
    }
}
