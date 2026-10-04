package com.smart.chat.couple;

import com.smart.chat.auth.AppUserService;
import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.BusinessException;
import com.smart.chat.common.Sessions;
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
 * 统计项只保留仍在线的功能：贴贴动作与好事簿，其余随功能下线一并移除。
 */
@RestController
@RequestMapping("/api/couple/admin")
public class CoupleAdminController {

    public record CoupleStatsVO(long activeSpaces, long dissolvedSpaces, long avgDays, long totalActions,
                                long totalDeeds, long spacesCreatedThisMonth) {
    }

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleActionMapper actionMapper;
    private final CoupleEchoDeedMapper deedMapper;
    private final AppUserService userService;

    @SuppressWarnings("java:S107")
    public CoupleAdminController(CoupleSpaceMapper spaceMapper, CoupleActionMapper actionMapper,
                                 CoupleEchoDeedMapper deedMapper, AppUserService userService) {
        this.spaceMapper = spaceMapper;
        this.actionMapper = actionMapper;
        this.deedMapper = deedMapper;
        this.userService = userService;
    }

    /** 情侣空间运营统计（仅管理员）。 */
    @GetMapping("/stats")
    public ApiResponse<CoupleStatsVO> stats(HttpSession session) {
        String me = Sessions.requireUser(session);
        try {
            userService.requireAdmin(me);
        } catch (BusinessException e) {
            throw new BusinessException(403, "仅管理员可查看运营看板");
        }
        List<CoupleSpace> all = spaceMapper.selectList(null);
        long active = all.stream().filter(s -> CoupleSpace.STATUS_ACTIVE.equals(s.getStatus())).count();
        long dissolved = all.size() - active;
        LocalDate now = LocalDate.now();
        long avgDays = active == 0 ? 0
                : (long) all.stream()
                        .filter(s -> CoupleSpace.STATUS_ACTIVE.equals(s.getStatus()))
                        .mapToLong(s -> {
                            LocalDate start;
                            try {
                                start = LocalDate.parse(s.getAnniversary());
                            } catch (Exception e) {
                                start = Instant.ofEpochMilli(s.getCreated()).atZone(ZoneId.systemDefault()).toLocalDate();
                            }
                            return Math.max(ChronoUnit.DAYS.between(start, now) + 1, 1);
                        })
                        .average().orElse(0);
        String monthPrefix = now.toString().substring(0, 7);
        long createdThisMonth = all.stream()
                .filter(s -> Instant.ofEpochMilli(s.getCreated()).atZone(ZoneId.systemDefault())
                        .toLocalDate().toString().startsWith(monthPrefix))
                .count();
        return ApiResponse.ok(new CoupleStatsVO(active, dissolved, avgDays,
                actionMapper.selectCount(null), deedMapper.selectCount(null), createdThisMonth));
    }
}
