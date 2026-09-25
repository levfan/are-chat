package com.smart.chat.upload;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * 上传文件 Mapper（MyBatis-Plus BaseMapper）。
 */
@Mapper
public interface UploadedFileMapper extends BaseMapper<UploadedFile> {

    default Optional<UploadedFile> findBySha256(String sha256) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<UploadedFile>()
                .eq(UploadedFile::getSha256, sha256)));
    }
}
