package com.smart.chat.filestorage.domain.file;

import com.smart.chat.filestorage.domain.RuleViolation;

import java.util.UUID;

/**
 * 上传文件档案（filestorage 的业务名，持久化模型是 {@code UploadedFilePO}）。
 * <p>
 * 一条档案 = 一份内容在服务端的唯一登记：内容寻址（{@code sha256}）唯一，落盘路径由指纹推导，
 * 名字与 MIME 只是登记信息。<b>登记之后不改写内容</b>——没有「换内容」的用例，
 * 所以字段全为 final，只暴露读方法。
 */
public final class UploadedFile {

    /** 无 MIME 记录时的对外口径 */
    public static final String BINARY_CONTENT_TYPE = "application/octet-stream";

    private final String id;
    private final String originalName;
    private final String storedPath;
    private final String contentType;
    private final long size;
    private final String sha256;
    private final Long uploadedAt;

    private UploadedFile(String id, String originalName, String storedPath, String contentType,
                         long size, String sha256, Long uploadedAt) {
        this.id = id;
        this.originalName = originalName;
        this.storedPath = storedPath;
        this.contentType = contentType;
        this.size = size;
        this.sha256 = sha256;
        this.uploadedAt = uploadedAt;
    }

    /**
     * 新建一条登记：现场发 id 与上传时刻（沿用改造前 PO 工厂的口径），并守住档案自身的不变式。
     * 名字/指纹/大小这三条闸门在正常管道里都碰不到（上游已清洗与裁决过），留着是为了不让脏数据进库。
     */
    public static UploadedFile register(String originalName, String storedPath, String contentType,
                                        long size, String sha256) {
        if (originalName == null || originalName.isBlank()) {
            throw new RuleViolation("文件必须有名字");
        }
        if (storedPath == null || storedPath.isBlank()) {
            throw new RuleViolation("存储路径不能为空");
        }
        if (size <= 0) {
            throw new RuleViolation("不能上传空文件");
        }
        if (!ContentFingerprint.looksLikeSha256(sha256)) {
            throw new RuleViolation("文件指纹不合法");
        }
        return new UploadedFile(UUID.randomUUID().toString(), originalName, storedPath, contentType,
                size, sha256, System.currentTimeMillis());
    }

    /** 从存储重建：不校验——历史行必须读得出来（哪怕 MIME 为空、扩展名口径已经变过）。 */
    public static UploadedFile restore(String id, String originalName, String storedPath, String contentType,
                                       long size, String sha256, Long uploadedAt) {
        return new UploadedFile(id, originalName, storedPath, contentType, size, sha256, uploadedAt);
    }

    /** 没有 MIME 记录时按二进制流处理——这条口径是对外契约，收在这里守（口径与改造前一致：仅 null 兜底）。 */
    public String contentTypeOrBinary() {
        return contentType == null ? BINARY_CONTENT_TYPE : contentType;
    }

    public String id() {
        return id;
    }

    public String originalName() {
        return originalName;
    }

    public String storedPath() {
        return storedPath;
    }

    public String contentType() {
        return contentType;
    }

    public long size() {
        return size;
    }

    public String sha256() {
        return sha256;
    }

    public Long uploadedAt() {
        return uploadedAt;
    }
}
