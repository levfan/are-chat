package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoupleGardenMapper extends BaseMapperCompat<CoupleGarden> {

    /** 空间的花园（没有则返回 null，由 Service 懒创建）。 */
    default CoupleGarden findBySpace(String spaceId) {
        return selectOne(new LambdaQueryWrapper<CoupleGarden>()
                .eq(CoupleGarden::getSpaceId, spaceId));
    }

    /** 全部花园（定时任务扫描缺水用）。 */
    default java.util.List<CoupleGarden> findAll() {
        return selectList(null);
    }
}
