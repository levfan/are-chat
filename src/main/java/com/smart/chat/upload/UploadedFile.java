package com.smart.chat.upload;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 上传文件档案：按 SHA-256 内容寻址，天然去重（老项目 DeduplicationAction 的现代化版本）。
 * id / uploadedAt 由服务层在入库前显式赋值；其余字段 camelCase 自动映射 snake_case 列。
 */
@TableName("uploaded_file")
public class UploadedFile {

    @TableId(value = "ID", type = IdType.INPUT)
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public void setStoredPath(String storedPath) {
        this.storedPath = storedPath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Long getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Long uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
