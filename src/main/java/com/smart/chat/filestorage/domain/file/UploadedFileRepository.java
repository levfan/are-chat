package com.smart.chat.filestorage.domain.file;

import java.util.Optional;

/**
 * 文件档案的仓储端口：用例只说「按指纹找有没有存过」和「登记一条」，
 * 走哪个 Mapper、唯一索引冲突长什么样，都是 infrastructure 的事。
 */
public interface UploadedFileRepository {

    /** 按内容指纹查已登记的档案（秒传去重的入口读法） */
    Optional<UploadedFile> findBySha256(String sha256);

    /** 按档案 id 查 */
    Optional<UploadedFile> findById(String id);

    /**
     * 登记档案。同一内容被并发写入时抛 {@link ContentAlreadyStored}（唯一索引兜底），
     * 调用方据此改走「已去重」分支。
     */
    void save(UploadedFile file);
}
