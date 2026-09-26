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

    // ---------- 新增：81 全局搜索 / 84 置顶 / 85 清空 / 95 附件 ----------

    /** 81 全局消息搜索：我参与的全部会话（未撤回文本，最近 50 条） */
    @GetMapping("/search/global")
    public ApiResponse<List<PrivateMessageService.GlobalSearchHitVO>> searchGlobal(@RequestParam String q,
                                                                                   HttpSession session) {
        return ApiResponse.ok(messageService.searchGlobal(Sessions.requireUser(session), q));
    }

    /** 84 置顶当前会话消息 */
    @PostMapping("/{peer}/pin")
    public ApiResponse<PrivateMessageService.PinVO> pin(@PathVariable String peer,
                                                        @RequestBody PinRequest req,
                                                        HttpSession session) {
        return ApiResponse.ok(messageService.pin(Sessions.requireUser(session), peer, req.msgId()));
    }

    /** 84 取消置顶 */
    @DeleteMapping("/{peer}/pin")
    public ApiResponse<Void> unpin(@PathVariable String peer, HttpSession session) {
        messageService.unpin(Sessions.requireUser(session), peer);
        return ApiResponse.ok();
    }

    /** 84 当前会话置顶信息（无置顶时 data 为 null） */
    @GetMapping("/{peer}/pin")
    public ApiResponse<PrivateMessageService.PinVO> currentPin(@PathVariable String peer, HttpSession session) {
        return ApiResponse.ok(messageService.currentPin(Sessions.requireUser(session), peer).orElse(null));
    }

    /** 85 清空当前会话全部聊天记录（双方视角，需前端二次确认） */
    @DeleteMapping("/{peer}")
    public ApiResponse<ClearedVO> clear(@PathVariable String peer, HttpSession session) {
        long deleted = messageService.clearConversation(Sessions.requireUser(session), peer);
        return ApiResponse.ok(new ClearedVO(deleted));
    }

    /** 95 会话附件：图片墙 / 文件列表（type=image|file，默认 image） */
    @GetMapping("/{peer}/attachments")
    public ApiResponse<List<PrivateMessageService.AttachmentVO>> attachments(
            @PathVariable String peer,
            @RequestParam(defaultValue = "image") String type,
            HttpSession session) {
        return ApiResponse.ok(messageService.attachments(Sessions.requireUser(session), peer, type));
    }

    public record PinRequest(String msgId) {
    }

    public record ClearedVO(long deleted) {
    }
}
