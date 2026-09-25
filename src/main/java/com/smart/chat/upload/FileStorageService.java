package com.smart.chat.upload;

import com.smart.chat.common.BusinessException;
import com.smart.chat.config.FileStorageProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 文件存储服务：SHA-256 内容去重 + 路径清洗 + 目录防穿越。
 */
@Service
public class FileStorageService {

    public record StoreResult(UploadedFile file, boolean deduplicated) {
    }

    public record DownloadableFile(String originalName, String contentType, Path path, long size) {
    }

    private final UploadedFileMapper mapper;
    private final FileStorageProperties properties;

    public FileStorageService(UploadedFileMapper mapper, FileStorageProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    // 注意：这里刻意不加 @Transactional —— 并发去重兜底依赖捕获唯一索引冲突，
    // 若在同一事务内捕获会导致提交时 UnexpectedRollbackException。
    public StoreResult store(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new BusinessException("不能上传空文件");
        }
        String originalName = sanitizeName(upload.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = upload.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("读取上传内容失败", e);
        }
        String sha256 = sha256Hex(bytes);
        Optional<UploadedFile> existing = mapper.findBySha256(sha256);
        if (existing.isPresent()) {
            return new StoreResult(existing.get(), true);
        }

        String extension = extensionOf(originalName);
        String storedPath = sha256.substring(0, 2) + "/" + sha256 + extension;
        Path target = resolveStored(storedPath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("文件落盘失败", e);
        }

        UploadedFile record = UploadedFile.of(originalName, storedPath, upload.getContentType(), bytes.length, sha256);
        try {
            mapper.insert(record);
            return new StoreResult(record, false);
        } catch (DuplicateKeyException e) {
            // 并发上传同一内容：唯一索引兜底，改为返回已有记录
            UploadedFile winner = mapper.findBySha256(sha256)
                    .orElseThrow(() -> e);
            return new StoreResult(winner, true);
        }
    }

    public UploadedFile get(String id) {
        return Optional.ofNullable(mapper.selectById(id))
                .orElseThrow(() -> new BusinessException(404, "文件不存在：" + id));
    }

    public DownloadableFile loadForDownload(String id) {
        UploadedFile record = get(id);
        Path path = resolveStored(record.getStoredPath());
        if (!Files.exists(path)) {
            throw new BusinessException(404, "文件已丢失：" + record.getOriginalName());
        }
        return new DownloadableFile(record.getOriginalName(),
                record.getContentType() == null ? "application/octet-stream" : record.getContentType(),
                path, record.getSize());
    }

    /** 将存储相对路径解析为绝对路径，并拒绝逃出根目录（防路径穿越）。 */
    public Path resolveStored(String storedPath) {
        Path base = Path.of(properties.baseDir()).toAbsolutePath().normalize();
        Path resolved = base.resolve(storedPath).normalize();
        if (!resolved.startsWith(base)) {
            throw new BusinessException("非法的文件路径：" + storedPath);
        }
        return resolved;
    }

    static String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    static String sanitizeName(String original) {
        String name = original == null ? "" : original;
        int lastSep = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSep >= 0) {
            name = name.substring(lastSep + 1);
        }
        name = name.replaceAll("\\p{Cntrl}", "")
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .trim();
        if (name.isEmpty()) {
            name = "unnamed";
        }
        if (name.length() > 100) {
            String ext = extensionOf(name);
            name = name.substring(0, 100 - ext.length()) + ext;
        }
        return name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        String ext = name.substring(dot);
        return ext.length() > 16 ? "" : ext;
    }
}
