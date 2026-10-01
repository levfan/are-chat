package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 常用收藏（F207）：把最常翻的卡片钉在手边。 */
@RestController
@RequestMapping("/api/couple/pin")
public class CouplePinController {

    private final CouplePinService service;

    public CouplePinController(CouplePinService service) {
        this.service = service;
    }

    /** 双方收藏清单。 */
    @GetMapping
    public ApiResponse<CouplePinService.PinVO> pins(HttpSession session) {
        return ApiResponse.ok(service.pins(Sessions.requireUser(session)));
    }

    public record PinRequest(List<String> pins) {
    }

    /** 全量覆盖我的收藏。 */
    @PostMapping
    public ApiResponse<CouplePinService.PinVO> save(@RequestBody PinRequest req, HttpSession session) {
        return ApiResponse.ok(service.savePins(Sessions.requireUser(session), req.pins()));
    }
}
