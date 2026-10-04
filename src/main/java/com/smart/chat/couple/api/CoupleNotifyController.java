package com.smart.chat.couple.api;

import com.smart.chat.couple.infrastructure.persistence.CoupleNotifyMapper;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * F41 情侣空间通知中心：事件补看 / 全部已读。
 */
@RestController
@RequestMapping("/api/couple/notify")
public class CoupleNotifyController {

    public record NotifyVO(String id, String event, String actor, String detail, boolean read, Long created) {
    }

    public record NotifyListVO(List<NotifyVO> items, long unread) {
    }

    private final CoupleNotifyMapper notifyMapper;

    public CoupleNotifyController(CoupleNotifyMapper notifyMapper) {
        this.notifyMapper = notifyMapper;
    }

    /** 我的最近 50 条通知 + 未读数。 */
    @GetMapping
    public ApiResponse<NotifyListVO> mine(HttpSession session) {
        String me = Sessions.requireUser(session);
        List<NotifyVO> items = notifyMapper.findMine(me).stream()
                .map(n -> new NotifyVO(n.getId(), n.getEvent(), n.getActor(), n.getDetail(),
                        n.getReadFlag() != null && n.getReadFlag() == 1, n.getCreated()))
                .toList();
        return ApiResponse.ok(new NotifyListVO(items, notifyMapper.countUnread(me)));
    }

    /** 全部标记已读。 */
    @PostMapping("/read-all")
    public ApiResponse<Void> readAll(HttpSession session) {
        notifyMapper.markAllRead(Sessions.requireUser(session));
        return ApiResponse.ok();
    }
}
