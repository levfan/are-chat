package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleNotifyMapper extends BaseMapperCompat<CoupleNotifyPO> {

    /** 我的最近 50 条通知（新→旧）。 */
    default List<CoupleNotifyPO> findMine(String username) {
        return selectList(new LambdaQueryWrapper<CoupleNotifyPO>()
                .eq(CoupleNotifyPO::getUsername, username)
                .orderByDesc(CoupleNotifyPO::getCreated)
                .last("LIMIT 50"));
    }

    default long countUnread(String username) {
        return selectCount(new LambdaQueryWrapper<CoupleNotifyPO>()
                .eq(CoupleNotifyPO::getUsername, username)
                .eq(CoupleNotifyPO::getReadFlag, 0));
    }

    /** 全部标记已读，返回影响条数。 */
    default int markAllRead(String username) {
        return update(null, new LambdaUpdateWrapper<CoupleNotifyPO>()
                .eq(CoupleNotifyPO::getUsername, username)
                .eq(CoupleNotifyPO::getReadFlag, 0)
                .set(CoupleNotifyPO::getReadFlag, 1));
    }
}
