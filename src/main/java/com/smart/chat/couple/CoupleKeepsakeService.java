package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 回忆资产·收藏系（F83/F88/F89）：甜蜜语录收藏册、恋爱电影票根、我们的歌单。
 * 情绪价值设计：甜话会过期，收藏不会；散场不散，票根为证；
 * 每首歌都藏着一段我们的故事。
 */
@Service
public class CoupleKeepsakeService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleQuoteMapper quoteMapper;
    private final CoupleTicketMapper ticketMapper;
    private final CoupleSongMapper songMapper;
    private final ImPushService push;

    public CoupleKeepsakeService(CoupleSpaceMapper spaceMapper, CoupleQuoteMapper quoteMapper,
                                 CoupleTicketMapper ticketMapper, CoupleSongMapper songMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.quoteMapper = quoteMapper;
        this.ticketMapper = ticketMapper;
        this.songMapper = songMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record QuoteVO(String id, String fromUser, String content, String context, Long created) {
    }

    public record TicketVO(String id, String fromUser, String title, String watchDay, int rating, String comment, Long created) {
    }

    public record SongVO(String id, String fromUser, String title, String artist, String reason, Long created) {
    }

    // ========== F83 甜蜜语录收藏册 ==========

    public List<QuoteVO> quotes(String me) {
        CoupleSpace space = requireSpace(me);
        return quoteMapper.findBySpace(space.getId()).stream()
                .map(q -> new QuoteVO(q.getId(), q.getFromUser(), q.getContent(), q.getContext(), q.getCreated()))
                .toList();
    }

    /** 收藏一句甜话（可记下当时的场景）。 */
    public List<QuoteVO> saveQuote(String me, String content, String context) {
        if (content == null || content.isBlank() || content.length() > CoupleQuote.CONTENT_MAX) {
            throw new BusinessException(400, "语录要写 " + CoupleQuote.CONTENT_MAX + " 字以内哦");
        }
        CoupleSpace space = requireSpace(me);
        quoteMapper.insert(CoupleQuote.of(space.getId(), me, content.trim(),
                context == null || context.isBlank() ? null : context.trim()));
        push.pushCoupleEvent("quote-kept", me, space.partnerOf(me),
                "📔 TA 把一句话收进了甜蜜语录册：「" + shortText(content.trim(), 30) + "」");
        return quotes(me);
    }

    /** 删除语录（双方都可整理册子）。 */
    public List<QuoteVO> removeQuote(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleQuote row = quoteMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这条语录哦");
        }
        quoteMapper.deleteById(id);
        return quotes(me);
    }

    // ========== F88 恋爱电影票根 ==========

    public List<TicketVO> tickets(String me) {
        CoupleSpace space = requireSpace(me);
        return ticketMapper.findBySpace(space.getId()).stream()
                .map(t -> new TicketVO(t.getId(), t.getFromUser(), t.getTitle(), t.getWatchDay(), t.getRating(), t.getComment(), t.getCreated()))
                .toList();
    }

    /** 存一张票根：片名 + 观看日期 + 评分（1-5）+ 感想。 */
    public List<TicketVO> saveTicket(String me, String title, String watchDay, Integer rating, String comment) {
        if (title == null || title.isBlank() || title.length() > CoupleTicket.TITLE_MAX) {
            throw new BusinessException(400, "片名要写 " + CoupleTicket.TITLE_MAX + " 字以内哦");
        }
        String day = watchDay == null || watchDay.isBlank() ? LocalDate.now().toString() : watchDay.trim();
        try {
            LocalDate.parse(day);
        } catch (Exception e) {
            throw new BusinessException(400, "观看日期格式是 yyyy-MM-dd 哦");
        }
        int stars = rating == null ? CoupleTicket.RATING_MAX : rating;
        if (stars < 1 || stars > CoupleTicket.RATING_MAX) {
            throw new BusinessException(400, "评分要在 1-5 星之间哦");
        }
        CoupleSpace space = requireSpace(me);
        ticketMapper.insert(CoupleTicket.of(space.getId(), me, title.trim(), day, stars,
                comment == null || comment.isBlank() ? null : comment.trim()));
        push.pushCoupleEvent("ticket-added", me, space.partnerOf(me),
                "🎫 票根墙新成员：「" + title.trim() + "」（" + day + "）");
        return tickets(me);
    }

    /** 撕掉票根（双方都可删）。 */
    public List<TicketVO> removeTicket(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleTicket row = ticketMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这张票根哦");
        }
        ticketMapper.deleteById(id);
        return tickets(me);
    }

    // ========== F89 我们的歌单 ==========

    public List<SongVO> songs(String me) {
        CoupleSpace space = requireSpace(me);
        return songMapper.findBySpace(space.getId()).stream()
                .map(s -> new SongVO(s.getId(), s.getFromUser(), s.getTitle(), s.getArtist(), s.getReason(), s.getCreated()))
                .toList();
    }

    /** 收藏一首「我们的歌」（可写为什么是我们的歌）。 */
    public List<SongVO> saveSong(String me, String title, String artist, String reason) {
        if (title == null || title.isBlank() || title.length() > CoupleSong.TITLE_MAX) {
            throw new BusinessException(400, "歌名要写 " + CoupleSong.TITLE_MAX + " 字以内哦");
        }
        CoupleSpace space = requireSpace(me);
        songMapper.insert(CoupleSong.of(space.getId(), me, title.trim(),
                artist == null || artist.isBlank() ? null : artist.trim(),
                reason == null || reason.isBlank() ? null : reason.trim()));
        push.pushCoupleEvent("song-added", me, space.partnerOf(me),
                "🎵 歌单新成员：「" + title.trim() + "」——循环到下一万个夜晚");
        return songs(me);
    }

    /** 从歌单移除（双方都可整理）。 */
    public List<SongVO> removeSong(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSong row = songMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这首歌哦");
        }
        songMapper.deleteById(id);
        return songs(me);
    }

    // ========== 内部工具 ==========

    private String shortText(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
