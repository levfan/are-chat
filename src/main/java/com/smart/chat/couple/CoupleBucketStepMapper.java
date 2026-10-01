package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F291 大事步骤数据访问。 */
@Mapper
public interface CoupleBucketStepMapper extends BaseMapperCompat<CoupleBucketStep> {

    /** 某大事全部步骤（按序）。 */
    default List<CoupleBucketStep> findByBucket(String bucketId) {
        return selectList(new LambdaQueryWrapper<CoupleBucketStep>()
                .eq(CoupleBucketStep::getBucketId, bucketId)
                .orderByAsc(CoupleBucketStep::getSeq));
    }

    /** 某大事最大序号步骤（next seq 计算用）。 */
    default List<CoupleBucketStep> findAllBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBucketStep>()
                .eq(CoupleBucketStep::getSpaceId, spaceId));
    }
}
