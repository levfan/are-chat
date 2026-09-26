package com.smart.chat.upload;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 上传文件档案：按 SHA-256 内容寻址，天然去重（老项目 DeduplicationAction 的现代化版本）。
 * id / uploadedAt 由服务层在入库前显式赋值；其余字段 camelCase 自动映射 snake_case 列。
 */
@Data
@TableName("uploaded_file")
public class UploadedFile {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    private String originalName;

    private String storedPath;

    private String contentType;

    private long size;

    private String sha256;

    private Long uploadedAt;

    public static UploadedFile of(String originalName, String storedPath, String contentType, long size, String sha256) {
        UploadedFile record = new UploadedFile();
        record.setId(UUID.randomUUID().toString());
        record.setOriginalName(originalName);
        record.setStoredPath(storedPath);
        record.setContentType(contentType);
        record.setSize(size);
        record.setSha256(sha256);
        record.setUploadedAt(System.currentTimeMillis());
        return record;
    }
}
