package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 共同养成·内容系（F74/F76/F78）：共读计划、追剧清单、恋爱词典。
 * 情绪价值设计：进度条各自爬但方向一致；专属词汇是两个人独有的语言体系。
 */
@Service
public class CoupleEntertainService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleReadPlanMapper readPlanMapper;
    private final CoupleReadProgressMapper readProgressMapper;
    private final CoupleWatchlistMapper watchlistMapper;
    private final CoupleDictWordMapper dictMapper;
    private final ImPushService push;

    public CoupleEntertainService(CoupleSpaceMapper spaceMapper, CoupleReadPlanMapper readPlanMapper,
                                  CoupleReadProgressMapper readProgressMapper, CoupleWatchlistMapper watchlistMapper,
                                  CoupleDictWordMapper dictMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.readPlanMapper = readPlanMapper;
        this.readProgressMapper = readProgressMapper;
        this.watchlistMapper = watchlistMapper;
        this.dictMapper = dictMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record ReadPlanVO(String id, String title, int totalUnits, String unitLabel, String status,
                             Integer myUnit, Integer partnerUnit, String myNote, String partnerNote, Long created) {
    }

    public record WatchVO(String id, String title, int currentUnit, Integer totalUnit, String updatedBy,
                          boolean updatedByMine, String status, Long created) {
    }

    public record DictVO(String id, String fromUser, String word, String meaning, Long created) {
    }

    // ========== F74 共读计划 ==========

    public List<ReadPlanVO> readPlans(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        return readPlanMapper.findBySpace(space.getId()).stream()
                .map(plan -> {
                    List<CoupleReadProgress> progresses = readProgressMapper.findByPlan(plan.getId());
                    CoupleReadProgress mine = progresses.stream()
                            .filter(p -> p.getFromUser().equals(me)).findFirst().orElse(null);
                    CoupleReadProgress theirs = progresses.stream()
                            .filter(p -> p.getFromUser().equals(partner)).findFirst().orElse(null);
                    return new ReadPlanVO(plan.getId(), plan.getTitle(), plan.getTotalUnits(), plan.getUnitLabel(),
                            plan.getStatus(),
                            mine == null ? null : mine.getUnit(), theirs == null ? null : theirs.getUnit(),
                            mine == null ? null : mine.getNote(), theirs == null ? null : theirs.getNote(),
                            plan.getCreated());
                })
                .toList();
    }

    /** 开一个共读计划：书名 + 总章数 + 单位（章/集/课）。 */
    public List<ReadPlanVO> createReadPlan(String me, String title, int totalUnits, String unitLabel) {
        if (title == null || title.isBlank() || title.length() > CoupleReadPlan.TITLE_MAX) {
            throw new BusinessException(400, "书名/剧名要写 " + CoupleReadPlan.TITLE_MAX + " 字以内哦");
        }
        if (totalUnits <= 0 || totalUnits > CoupleReadPlan.UNIT_MAX) {
            throw new BusinessException(400, "总" + (unitLabel == null || unitLabel.isBlank() ? "章" : unitLabel) + "数要大于 0");
        }
        CoupleSpace space = requireSpace(me);
        String label = unitLabel == null || unitLabel.isBlank() ? "章" : unitLabel.trim();
        readPlanMapper.insert(CoupleReadPlan.of(space.getId(), title.trim(), totalUnits, label));
        push.pushCoupleEvent("read-started", me, space.partnerOf(me),
                "📚 TA 发起了共读计划：「" + title.trim() + "」（共 " + totalUnits + " " + label + "），一起翻到最后一页");
        return readPlans(me);
    }

    /** 上报我的最新进度（可附一句感想）；双方都到终点即读完。 */
    public List<ReadPlanVO> reportReadProgress(String me, String planId, int unit, String note) {
        CoupleSpace space = requireSpace(me);
        CoupleReadPlan plan = readPlanMapper.selectById(planId);
        if (plan == null || !plan.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个共读计划哦");
        }
        if (CoupleReadPlan.STATUS_FINISHED.equals(plan.getStatus())) {
            throw new BusinessException(400, "这本书已经一起读完啦，开一本新的吧");
        }
        if (unit < 0 || unit > plan.getTotalUnits()) {
            throw new BusinessException(400, "进度要在 0 ~ " + plan.getTotalUnits() + " 之间哦");
        }
        readProgressMapper.insert(CoupleReadProgress.of(plan.getId(), me, unit,
                note == null || note.isBlank() ? null : note.trim()));
        if (unit >= plan.getTotalUnits()) {
            // 判断双方是否都到终点
            Map<String, Integer> latest = readProgressMapper.findByPlan(plan.getId()).stream()
                    .collect(Collectors.toMap(CoupleReadProgress::getFromUser, CoupleReadProgress::getUnit,
                            (a, b) -> a)); // findByPlan 已按时间倒序，先出现的即最新
            int partnerLatest = latest.getOrDefault(space.partnerOf(me), 0);
            if (partnerLatest >= plan.getTotalUnits()) {
                plan.setStatus(CoupleReadPlan.STATUS_FINISHED);
                plan.setFinishedAt(System.currentTimeMillis());
                readPlanMapper.updateById(plan);
                push.pushCoupleEventBoth("read-finished", me, space.getUserA(), space.getUserB(),
                        "🎉 共读达成：「" + plan.getTitle() + "」你们一起读到了最后一" + plan.getUnitLabel() + "！");
            } else {
                push.pushCoupleEvent("read-progress", me, space.partnerOf(me),
                        "📖 TA 已读到最后一" + plan.getUnitLabel() + "，等你一起抵达结局");
            }
        } else {
            push.pushCoupleEvent("read-progress", me, space.partnerOf(me),
                    "📖 TA 的「" + plan.getTitle() + "」进度更新：第 " + unit + " " + plan.getUnitLabel());
        }
        return readPlans(me);
    }

    // ========== F76 追剧清单 ==========

    public List<WatchVO> watchlist(String me) {
        CoupleSpace space = requireSpace(me);
        return watchlistMapper.findBySpace(space.getId()).stream()
                .map(w -> new WatchVO(w.getId(), w.getTitle(), w.getCurrentUnit(), w.getTotalUnit(), w.getUpdatedBy(),
                        me.equals(w.getUpdatedBy()), w.getStatus(), w.getCreated()))
                .toList();
    }

    /** 加一部一起追的剧（总集数未知可不填）。 */
    public List<WatchVO> addWatch(String me, String title, Integer totalUnit) {
        if (title == null || title.isBlank() || title.length() > CoupleWatchlist.TITLE_MAX) {
            throw new BusinessException(400, "剧名要写 " + CoupleWatchlist.TITLE_MAX + " 字以内哦");
        }
        if (totalUnit != null && totalUnit <= 0) {
            throw new BusinessException(400, "总集数要大于 0（不知道就先不填）");
        }
        CoupleSpace space = requireSpace(me);
        CoupleWatchlist row = CoupleWatchlist.of(space.getId(), title.trim(), totalUnit);
        row.setUpdatedBy(me);
        watchlistMapper.insert(row);
        push.pushCoupleEvent("watch-added", me, space.partnerOf(me),
                "📺 TA 拉你入伙追剧：「" + title.trim() + "」，遥控器一人一天");
        return watchlist(me);
    }

    /** 更新共同进度（双方都可更新）；追到总集数自动完结。 */
    public List<WatchVO> updateWatch(String me, String id, int currentUnit) {
        CoupleSpace space = requireSpace(me);
        CoupleWatchlist row = watchlistMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这部剧哦");
        }
        if (CoupleWatchlist.STATUS_DONE.equals(row.getStatus())) {
            throw new BusinessException(400, "这部剧已经一起看完啦");
        }
        if (currentUnit < 0 || (row.getTotalUnit() != null && currentUnit > row.getTotalUnit())) {
            throw new BusinessException(400, "进度要在 0 ~ " + row.getTotalUnit() + " 集之间哦");
        }
        row.setCurrentUnit(currentUnit);
        row.setUpdatedBy(me);
        if (row.getTotalUnit() != null && currentUnit >= row.getTotalUnit()) {
            row.setStatus(CoupleWatchlist.STATUS_DONE);
            row.setFinishedAt(System.currentTimeMillis());
            watchlistMapper.updateById(row);
            push.pushCoupleEventBoth("watch-finished", me, space.getUserA(), space.getUserB(),
                    "🎬 「" + row.getTitle() + "」一起追完啦！接下来看哪部？");
        } else {
            watchlistMapper.updateById(row);
            push.pushCoupleEvent("watch-updated", me, space.partnerOf(me),
                    "📺 「" + row.getTitle() + "」进度更新：看到第 " + currentUnit + " 集（更新人：" + me + "）");
        }
        return watchlist(me);
    }

    // ========== F78 恋爱词典 ==========

    public List<DictVO> dictWords(String me) {
        CoupleSpace space = requireSpace(me);
        return dictMapper.findBySpace(space.getId()).stream()
                .map(w -> new DictVO(w.getId(), w.getFromUser(), w.getWord(), w.getMeaning(), w.getCreated()))
                .toList();
    }

    /** 收录一个专属词汇 + 释义。 */
    public List<DictVO> addWord(String me, String word, String meaning) {
        if (word == null || word.isBlank() || word.length() > CoupleDictWord.WORD_MAX) {
            throw new BusinessException(400, "词汇要写 " + CoupleDictWord.WORD_MAX + " 字以内哦");
        }
        if (meaning == null || meaning.isBlank() || meaning.length() > CoupleDictWord.MEANING_MAX) {
            throw new BusinessException(400, "释义要写 " + CoupleDictWord.MEANING_MAX + " 字以内哦");
        }
        CoupleSpace space = requireSpace(me);
        dictMapper.insert(CoupleDictWord.of(space.getId(), me, word.trim(), meaning.trim()));
        push.pushCoupleEvent("dict-added", me, space.partnerOf(me),
                "📖 恋爱词典新增词条：「" + word.trim() + "」——只有你们懂");
        return dictWords(me);
    }

    /** 删除词条（双方都可删，词典保持干净）。 */
    public List<DictVO> removeWord(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleDictWord row = dictMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个词条哦");
        }
        dictMapper.deleteById(id);
        return dictWords(me);
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
