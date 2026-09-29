package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleNotifyMapper extends BaseMapperCompat<CoupleNotify> {

    /** 我的最近 50 条通知（新→旧）。 */
    default List<CoupleNotify> findMine(String username) {
        return selectList(new LambdaQueryWrapper<CoupleNotify>()
                .eq(CoupleNotify::getUsername, username)
                .orderByDesc(CoupleNotify::getCreated)
                .last("LIMIT 50"));
    }

    default long countUnread(String username) {
        return selectCount(new LambdaQueryWrapper<CoupleNotify>()
                .eq(CoupleNotify::getUsername, username)
                .eq(CoupleNotify::getReadFlag, 0));
    }

    /** 全部标记已读，返回影响条数。 */
    default int markAllRead(String username) {
        return update(null, new LambdaUpdateWrapper<CoupleNotify>()
                .eq(CoupleNotify::getUsername, username)
                .eq(CoupleNotify::getReadFlag, 0)
                .set(CoupleNotify::getReadFlag, 1));
    }
}
