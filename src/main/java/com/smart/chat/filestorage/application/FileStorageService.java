package com.smart.chat.filestorage.application;

import com.smart.chat.filestorage.domain.RuleViolation;
import com.smart.chat.filestorage.domain.file.ContentAlreadyStored;
import com.smart.chat.filestorage.domain.file.ContentFingerprint;
import com.smart.chat.filestorage.domain.file.FileNaming;
import com.smart.chat.filestorage.domain.file.StoragePath;
import com.smart.chat.filestorage.domain.file.UploadAdmission;
import com.smart.chat.filestorage.domain.file.UploadedFile;
import com.smart.chat.filestorage.domain.file.UploadedFileRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.bootstrap.properties.FileStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 文件存储用例编排：SHA-256 内容去重 + 路径清洗 + 目录防穿越。
 * 96 上传安全加固：危险扩展名黑名单（可执行/脚本/网页服务端文件）一律拒绝，防止借下载链接分发或落盘执行。
 * <p>
 * 取数只经 {@link UploadedFileRepository} 端口；四条裁决（空文件、扩展名、清洗文件名、内容寻址路径与防穿越）
 * 全在 {@code filestorage.domain.file} 里，这里只按顺序把它们串起来，并把结果投影成 VO。
 */
@Service
public class FileStorageService {

    public record StoreResult(UploadedFile file, boolean deduplicated) {
    }

    public record DownloadableFile(String originalName, String contentType, Path path, long size) {
    }

    private final UploadedFileRepository repository;
    private final FileStorageProperties properties;

    public FileStorageService(UploadedFileRepository repository, FileStorageProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    // 注意：这里刻意不加 @Transactional —— 并发去重兜底依赖捕获唯一索引冲突，
    // 若在同一事务内捕获会导致提交时 UnexpectedRollbackException。
    public StoreResult store(MultipartFile upload) {
        long declaredSize = upload == null ? 0L : upload.getSize();
        DomainRules.guard(() -> UploadAdmission.admitContent(declaredSize));
        String originalName = FileNaming.sanitize(upload == null ? null : upload.getOriginalFilename());
        // 96 扩展名黑名单：下载时保留原始 Content-Disposition，可执行文件不允许上传
        String extension = DomainRules.rule(() -> UploadAdmission.admitExtension(FileNaming.extensionOf(originalName)));

        byte[] bytes;
        try {
            bytes = upload.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("读取上传内容失败", e);
        }
        String sha256 = ContentFingerprint.sha256Hex(bytes);
        Optional<UploadedFile> alreadyStored = repository.findBySha256(sha256);
        if (alreadyStored.isPresent()) {
            return new StoreResult(alreadyStored.get(), true);
        }

        String storedPath = StoragePath.storedPathFor(sha256, extension);
        Path target = resolveStored(storedPath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("文件落盘失败", e);
        }

        UploadedFile file = DomainRules.rule(() -> UploadedFile.register(originalName, storedPath,
                upload.getContentType(), bytes.length, sha256));
        try {
            repository.save(file);
            return new StoreResult(file, false);
        } catch (ContentAlreadyStored raced) {
            // 并发上传同一内容：唯一索引兜底，改为返回已有记录
            UploadedFile winner = repository.findBySha256(raced.sha256()).orElseThrow(() -> raced);
            return new StoreResult(winner, true);
        }
    }

    public UploadedFile get(String id) {
        return DomainRules.rule(() -> repository.findById(id)
                .orElseThrow(() -> new RuleViolation("文件不存在：" + id, true)));
    }

    public DownloadableFile loadForDownload(String id) {
        UploadedFile file = get(id);
        Path path = resolveStored(file.storedPath());
        // 磁盘上的文件是否还在是环境事实（不是业务规则），所以这一句留在用例层；文案仍是对外契约
        if (!Files.exists(path)) {
            throw new BusinessException(404, "文件已丢失：" + file.originalName());
        }
        return new DownloadableFile(file.originalName(), file.contentTypeOrBinary(), path, file.size());
    }

    /** 将存储相对路径解析为绝对路径，并拒绝逃出根目录（防目录穿越）。 */
    public Path resolveStored(String storedPath) {
        return DomainRules.rule(() -> StoragePath.resolveUnder(Path.of(properties.baseDir()), storedPath));
    }

    /** 内容指纹算法已归领域（{@link ContentFingerprint}），这里保留既有的用例入口。 */
    static String sha256Hex(byte[] content) {
        return ContentFingerprint.sha256Hex(content);
    }

    /** 文件名清洗已归领域（{@link FileNaming}），这里保留既有的用例入口。 */
    static String sanitizeName(String original) {
        return FileNaming.sanitize(original);
    }
}
