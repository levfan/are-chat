package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F395 笑点默契考数据访问（uk(space_id,joke_id,from_user) 一条梗每人一票）。 */
@Mapper
public interface CoupleLaughGuessMapper extends BaseMapperCompat<CoupleLaughGuess> {

    /** 空间全部预判票（旧的在前，一份卷子的两份票按提交顺序读）。 */
    default List<CoupleLaughGuess> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLaughGuess>()
                .eq(CoupleLaughGuess::getSpaceId, spaceId)
                .orderByAsc(CoupleLaughGuess::getCreated));
    }

    /** 某条梗收到的两份票（旧的在前，齐了才比一致）。 */
    default List<CoupleLaughGuess> findByJoke(String spaceId, String jokeId) {
        return selectList(new LambdaQueryWrapper<CoupleLaughGuess>()
                .eq(CoupleLaughGuess::getSpaceId, spaceId)
                .eq(CoupleLaughGuess::getJokeId, jokeId)
                .orderByAsc(CoupleLaughGuess::getCreated));
    }

    /** 按 uk 定位那一票（改票查重用）。 */
    default CoupleLaughGuess find(String spaceId, String jokeId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleLaughGuess>()
                .eq(CoupleLaughGuess::getSpaceId, spaceId)
                .eq(CoupleLaughGuess::getJokeId, jokeId)
                .eq(CoupleLaughGuess::getFromUser, fromUser));
    }
}
