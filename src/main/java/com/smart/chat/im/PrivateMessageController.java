package com.smart.chat.im;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
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
@RequestMapping("/api/messages")
public class PrivateMessageController {

    public record SendMessageRequest(String content, String type, String replyToId) {
    }

    public record EditMessageRequest(String content) {
    }

    public record ReactionRequest(String emoji) {
    }

    private final PrivateMessageService messageService;

    public PrivateMessageController(PrivateMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{peer}")
    public ApiResponse<List<PrivateMessageService.MessageVO>> history(@PathVariable String peer,
                                                                      @RequestParam(required = false) Long before,
                                                                      @RequestParam(defaultValue = "20") int limit,
                                                                      HttpSession session) {
        return ApiResponse.ok(messageService.history(Sessions.requireUser(session), peer, before, limit));
    }

    @GetMapping("/{peer}/search")
    public ApiResponse<List<PrivateMessageService.MessageVO>> search(@PathVariable String peer,
                                                                     @RequestParam String q,
                                                                     HttpSession session) {
        return ApiResponse.ok(messageService.search(Sessions.requireUser(session), peer, q));
    }

    /** 56 导出当前会话全部消息（JSON）。 */
    @GetMapping("/{peer}/export")
    public ApiResponse<List<PrivateMessageService.MessageVO>> export(@PathVariable String peer,
                                                                     HttpSession session) {
        return ApiResponse.ok(messageService.export(Sessions.requireUser(session), peer));
    }

    @PostMapping("/{peer}")
    public ApiResponse<PrivateMessage> send(@PathVariable String peer,
                                            @RequestBody SendMessageRequest req,
                                            HttpSession session) {
        return ApiResponse.ok(messageService.send(Sessions.requireUser(session), peer, req.content(), req.type(), req.replyToId()));
    }

    @PostMapping("/{peer}/read")
    public ApiResponse<Void> markRead(@PathVariable String peer, HttpSession session) {
        messageService.markRead(Sessions.requireUser(session), peer);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/recall")
    public ApiResponse<Void> recall(@PathVariable String id, HttpSession session) {
        messageService.recall(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    /** 编辑自己 2 分钟内发出的文本消息。 */
    @PutMapping("/{id}")
    public ApiResponse<PrivateMessage> edit(@PathVariable String id, @RequestBody EditMessageRequest req,
                                            HttpSession session) {
        return ApiResponse.ok(messageService.edit(Sessions.requireUser(session), id, req.content()));
    }

    /** 表情回应 toggle：已回应则取消。 */
    @PostMapping("/{id}/reactions")
    public ApiResponse<Void> react(@PathVariable String id, @RequestBody ReactionRequest req,
                                   HttpSession session) {
        messageService.toggleReaction(Sessions.requireUser(session), id, req.emoji());
        return ApiResponse.ok();
    }

    /** 收藏/取消收藏 toggle。 */
    @PostMapping("/{id}/star")
    public ApiResponse<Void> star(@PathVariable String id, HttpSession session) {
        messageService.toggleStar(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }
}
