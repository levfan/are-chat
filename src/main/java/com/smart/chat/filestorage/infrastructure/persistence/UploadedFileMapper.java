package com.smart.chat.filestorage.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * 上传文件 Mapper（MyBatis-Plus BaseMapper）。不感知领域类型，只服务 {@link UploadedFilePO}。
 */
@Mapper
public interface UploadedFileMapper extends BaseMapper<UploadedFilePO> {

    default Optional<UploadedFilePO> findBySha256(String sha256) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<UploadedFilePO>()
                .eq(UploadedFilePO::getSha256, sha256)));
    }
}
