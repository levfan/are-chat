package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 爱情花园 · 每日玫瑰 · 幸运签（F54-F56）：共同养成的小树、限量鲜花与每日好运签。
 */
@RestController
@RequestMapping("/api/couple/garden")
public class CoupleGardenController {

    public record RoseSendRequest(String flowerKey) {
    }

    private final CoupleGardenService gardenService;

    public CoupleGardenController(CoupleGardenService gardenService) {
        this.gardenService = gardenService;
    }

    /** 花园状态（首次访问自动开垦）。 */
    @GetMapping
    public ApiResponse<CoupleGardenService.GardenVO> garden(HttpSession session) {
        return ApiResponse.ok(gardenService.garden(Sessions.requireUser(session)));
    }

    /** 浇水（每人每天一次；蔫了浇水即复活；浇满升阶段）。 */
    @PostMapping("/water")
    public ApiResponse<CoupleGardenService.GardenVO> water(HttpSession session) {
        return ApiResponse.ok(gardenService.water(Sessions.requireUser(session)));
    }

    /** 玫瑰看板：今天双方的花 + 我还能送几朵。 */
    @GetMapping("/roses")
    public ApiResponse<CoupleGardenService.RoseBoardVO> roseBoard(HttpSession session) {
        return ApiResponse.ok(gardenService.roseBoard(Sessions.requireUser(session)));
    }

    /** 送一朵玫瑰（每天限 3 朵，花语随机）。 */
    @PostMapping("/roses")
    public ApiResponse<CoupleGardenService.RoseBoardVO> sendRose(@RequestBody RoseSendRequest req,
                                                                 HttpSession session) {
        return ApiResponse.ok(gardenService.sendRose(Sessions.requireUser(session), req.flowerKey()));
    }

    /** 签板：我今天为 TA 抽的签 + TA 为我抽的签 + 最近记录。 */
    @GetMapping("/slips")
    public ApiResponse<CoupleGardenService.SlipBoardVO> slipBoard(HttpSession session) {
        return ApiResponse.ok(gardenService.slipBoard(Sessions.requireUser(session)));
    }

    /** 为 TA 抽一支今日幸运签（可重抽覆盖）。 */
    @PostMapping("/slips")
    public ApiResponse<CoupleGardenService.SlipBoardVO> drawSlip(HttpSession session) {
        return ApiResponse.ok(gardenService.drawSlip(Sessions.requireUser(session)));
    }
}
