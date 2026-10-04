package com.smart.chat.filestorage.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 上传文件档案的**持久化模型**（ADR-0002：业务名让给 domain，PO 只做表映射）。
 * id / uploadedAt 由领域聚合 {@code filestorage.domain.file.UploadedFile} 在创建时赋值，这里不加业务方法。
 * 其余字段 camelCase 自动映射 snake_case 列。
 */
@Data
@TableName("uploaded_file")
public class UploadedFilePO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    private String originalName;

    private String storedPath;

    private String contentType;

    private long size;

    private String sha256;

    private Long uploadedAt;
}
