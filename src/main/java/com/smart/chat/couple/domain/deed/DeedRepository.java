package com.smart.chat.couple.domain.deed;

import java.util.List;
import java.util.Optional;

/**
 * 好事簿的仓储端口：领域只说「按 id 拿一条」「某人记了哪些」「这个内容是不是已经记过」「把它存回去」。
 * <p>
 * 走哪个 Mapper、按什么排序、哪一列归谁写，都是 infrastructure 的事（见 {@code docs/ddd/02-layering.md} 第一节）。
 * 端口里没有 delete：好事簿是关系记录，现役用例没有「删掉一条好事」，不编造行为。
 */
public interface DeedRepository {

    /** 全部空间的好事条数（运营看板 F45 的全站口径）。 */
    long countAll();

    /** 按 id 取一条（不带空间过滤——归属闸门是用例的事，见 {@code CoupleEchoService.starDeed}）。 */
    Optional<Deed> findById(String deedId);

    /** 该空间的全部好事（心动值按条数供数，只要数量不要内容）。 */
    List<Deed> findBySpace(String spaceId);

    /** 某人记下的全部好事，新的在前（列表截断上限属于用例，不在这里）。 */
    List<Deed> listByRecorder(String spaceId, String fromUser);

    /** 同日同人同内容是否已经记过（内容全等才算重复，这是现役查重口径）。 */
    boolean alreadyRecorded(String spaceId, String fromUser, String day, String content);

    /** 存回聚合：新记录整行插入；已存在则<b>只回写聚合纳管的列</b>（加星与更新时间），内容/发生日/记录人发布即冻结。 */
    void save(Deed deed);
}
