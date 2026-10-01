package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 确定感·安全感（F120-F129，批次八）：安全感账户 / 恋爱体检 / 十年之约 / 愿景板 /
 * 承诺博物馆 / 信任存折 / 恋爱年轮 / 双人契约 / 守护兽。
 */
@RestController
@RequestMapping("/api/couple/secure")
public class CoupleSecureController {

    public record SecurityRequest(String content) {
    }

    public record DecadeRequest(String content) {
    }

    public record VisionRequest(String word, String note) {
    }

    public record OathRequest(String content) {
    }

    public record TrustRequest(String reason) {
    }

    public record ContractRequest(String title, String content) {
    }

    public record PetRequest(String name, String kind) {
    }

    private final CoupleSecureService secureService;

    public CoupleSecureController(CoupleSecureService secureService) {
        this.secureService = secureService;
    }

    // ---------- F120 安全感账户 ----------

    @GetMapping("/security")
    public ApiResponse<CoupleSecureService.SecurityBoardVO> security(HttpSession session) {
        return ApiResponse.ok(secureService.securityBank(Sessions.requireUser(session)));
    }

    /** 存一句安心话。 */
    @PostMapping("/security")
    public ApiResponse<CoupleSecureService.SecurityBoardVO> depositSecurity(@RequestBody SecurityRequest req,
                                                                            HttpSession session) {
        return ApiResponse.ok(secureService.depositSecurity(Sessions.requireUser(session), req.content()));
    }

    /** 收下一句安心话（只能收对方存的）。 */
    @PostMapping("/security/{id}/accept")
    public ApiResponse<CoupleSecureService.SecurityBoardVO> acceptSecurity(@PathVariable String id,
                                                                           HttpSession session) {
        return ApiResponse.ok(secureService.acceptSecurity(Sessions.requireUser(session), id));
    }

    // ---------- F121 恋爱体检 ----------

    @GetMapping("/checkup")
    public ApiResponse<CoupleSecureService.CheckupVO> checkup(HttpSession session) {
        return ApiResponse.ok(secureService.checkup(Sessions.requireUser(session)));
    }

    // ---------- F122 十年之约 ----------

    @GetMapping("/decade")
    public ApiResponse<CoupleSecureService.DecadeVO> decade(HttpSession session) {
        return ApiResponse.ok(secureService.decadePact(Sessions.requireUser(session)));
    }

    /** 写/改我的十年之约（一人一条）。 */
    @PostMapping("/decade")
    public ApiResponse<CoupleSecureService.DecadeVO> saveDecade(@RequestBody DecadeRequest req,
                                                                HttpSession session) {
        return ApiResponse.ok(secureService.saveDecadePact(Sessions.requireUser(session), req.content()));
    }

    // ---------- F123 愿景板 ----------

    @GetMapping("/visions")
    public ApiResponse<List<CoupleSecureService.VisionVO>> visions(HttpSession session) {
        return ApiResponse.ok(secureService.visions(Sessions.requireUser(session)));
    }

    /** 贴一张愿景卡。 */
    @PostMapping("/visions")
    public ApiResponse<List<CoupleSecureService.VisionVO>> addVision(@RequestBody VisionRequest req,
                                                                     HttpSession session) {
        return ApiResponse.ok(secureService.addVision(Sessions.requireUser(session),
                req.word(), req.note()));
    }

    // ---------- F124 承诺博物馆 ----------

    @GetMapping("/oaths")
    public ApiResponse<List<CoupleSecureService.OathVO>> oaths(HttpSession session) {
        return ApiResponse.ok(secureService.oaths(Sessions.requireUser(session)));
    }

    /** 立一份郑重承诺。 */
    @PostMapping("/oaths")
    public ApiResponse<List<CoupleSecureService.OathVO>> makeOath(@RequestBody OathRequest req,
                                                                  HttpSession session) {
        return ApiResponse.ok(secureService.makeOath(Sessions.requireUser(session), req.content()));
    }

    /** 给承诺盖章（双方都盖即展出）。 */
    @PostMapping("/oaths/{id}/stamp")
    public ApiResponse<List<CoupleSecureService.OathVO>> stampOath(@PathVariable String id,
                                                                   HttpSession session) {
        return ApiResponse.ok(secureService.stampOath(Sessions.requireUser(session), id));
    }

    // ---------- F125 信任存折 ----------

    @GetMapping("/trust")
    public ApiResponse<CoupleSecureService.TrustBoardVO> trust(HttpSession session) {
        return ApiResponse.ok(secureService.trustBank(Sessions.requireUser(session)));
    }

    /** 给对方存一枚信任币（每天最多一枚）。 */
    @PostMapping("/trust")
    public ApiResponse<CoupleSecureService.TrustBoardVO> depositTrust(@RequestBody(required = false) TrustRequest req,
                                                                      HttpSession session) {
        return ApiResponse.ok(secureService.depositTrust(Sessions.requireUser(session),
                req == null ? null : req.reason()));
    }

    // ---------- F126 恋爱年轮 ----------

    @GetMapping("/rings")
    public ApiResponse<CoupleSecureService.RingBoardVO> rings(HttpSession session) {
        return ApiResponse.ok(secureService.rings(Sessions.requireUser(session)));
    }

    // ---------- F128 双人契约 ----------

    @GetMapping("/contracts")
    public ApiResponse<List<CoupleSecureService.ContractVO>> contracts(HttpSession session) {
        return ApiResponse.ok(secureService.contracts(Sessions.requireUser(session)));
    }

    /** 立一份双人契约。 */
    @PostMapping("/contracts")
    public ApiResponse<List<CoupleSecureService.ContractVO>> makeContract(@RequestBody ContractRequest req,
                                                                          HttpSession session) {
        return ApiResponse.ok(secureService.makeContract(Sessions.requireUser(session),
                req.title(), req.content()));
    }

    /** 契约打卡 +1。 */
    @PostMapping("/contracts/{id}/checkin")
    public ApiResponse<List<CoupleSecureService.ContractVO>> checkContract(@PathVariable String id,
                                                                           HttpSession session) {
        return ApiResponse.ok(secureService.checkContract(Sessions.requireUser(session), id));
    }

    // ---------- F129 守护兽 ----------

    @GetMapping("/pet")
    public ApiResponse<CoupleSecureService.PetVO> pet(HttpSession session) {
        return ApiResponse.ok(secureService.pet(Sessions.requireUser(session)));
    }

    /** 领养守护兽。 */
    @PostMapping("/pet")
    public ApiResponse<CoupleSecureService.PetVO> adoptPet(@RequestBody PetRequest req,
                                                           HttpSession session) {
        return ApiResponse.ok(secureService.adoptPet(Sessions.requireUser(session),
                req.name(), req.kind()));
    }

    /** 照料守护兽。 */
    @PostMapping("/pet/care")
    public ApiResponse<CoupleSecureService.PetVO> carePet(HttpSession session) {
        return ApiResponse.ok(secureService.carePet(Sessions.requireUser(session)));
    }
}
