package com.smart.chat.upload;

import com.smart.chat.common.ApiResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件上传/下载：为 IM 图片消息提供存储能力（SHA-256 内容寻址去重）。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    public record FileInfo(String id, String originalName, String contentType, long size,
                           String sha256, boolean deduplicated, String downloadUrl, Long uploadedAt) {

        static FileInfo of(UploadedFile f, boolean deduplicated) {
            return new FileInfo(f.getId(), f.getOriginalName(), f.getContentType(), f.getSize(),
                    f.getSha256(), deduplicated, "/api/files/" + f.getId() + "/download", f.getUploadedAt());
        }
    }

    private final FileStorageService storageService;

    public FileController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileInfo> upload(@RequestParam("file") MultipartFile file) {
        FileStorageService.StoreResult result = storageService.store(file);
        return ApiResponse.ok(FileInfo.of(result.file(), result.deduplicated()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        FileStorageService.DownloadableFile file = storageService.loadForDownload(id);
        String encodedName = URLEncoder.encode(file.originalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .body(new FileSystemResource(file.path()));
    }
}
