package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSweetLineMapper extends BaseMapperCompat<CoupleSweetLine> {

    /** 某场擂台的参赛句子。 */
    default List<CoupleSweetLine> findByBattle(String battleId) {
        return selectList(new LambdaQueryWrapper<CoupleSweetLine>()
                .eq(CoupleSweetLine::getBattleId, battleId)
                .orderByAsc(CoupleSweetLine::getCreated));
    }
}
