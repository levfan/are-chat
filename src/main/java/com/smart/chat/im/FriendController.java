package com.smart.chat.im;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    public record ApplyRequest(String username, String message) {
    }

    public record UpdateFriendRequest(String remark, Boolean pinned, Boolean muted, String tag, Boolean blocked) {
    }

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public ApiResponse<List<FriendService.FriendVO>> list(HttpSession session) {
        return ApiResponse.ok(friendService.listFriends(Sessions.requireUser(session)));
    }

    /** 加好友联想：输入关键字返回候选用户名及与我的关系 */
    @GetMapping("/suggest")
    public ApiResponse<List<FriendService.UserSuggestion>> suggest(
            @RequestParam(name = "q", required = false) String q, HttpSession session) {
        return ApiResponse.ok(friendService.suggest(Sessions.requireUser(session), q));
    }

    @PostMapping("/requests")
    public ApiResponse<FriendService.FriendRequestVO> apply(@RequestBody ApplyRequest req, HttpSession session) {
        return ApiResponse.ok(friendService.apply(Sessions.requireUser(session), req.username(), req.message()));
    }

    @GetMapping("/requests/incoming")
    public ApiResponse<List<FriendService.FriendRequestVO>> incoming(HttpSession session) {
        return ApiResponse.ok(friendService.incoming(Sessions.requireUser(session)));
    }

    @GetMapping("/requests/outgoing")
    public ApiResponse<List<FriendService.FriendRequestVO>> outgoing(HttpSession session) {
        return ApiResponse.ok(friendService.outgoing(Sessions.requireUser(session)));
    }

    @PostMapping("/requests/{id}/accept")
    public ApiResponse<Void> accept(@PathVariable String id, HttpSession session) {
        friendService.accept(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    @PostMapping("/requests/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable String id, HttpSession session) {
        friendService.reject(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody UpdateFriendRequest req,
                                    HttpSession session) {
        friendService.updateFriend(Sessions.requireUser(session), id,
                req.remark(), req.pinned(), req.muted(), req.tag(), req.blocked());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id, HttpSession session) {
        friendService.deleteFriend(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }
}
