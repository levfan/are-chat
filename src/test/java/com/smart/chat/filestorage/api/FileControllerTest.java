package com.smart.chat.filestorage.api;

import com.smart.chat.filestorage.application.FileStorageService;
import com.smart.chat.filestorage.domain.file.UploadedFile;
import com.smart.chat.bootstrap.config.FastJsonWebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@Import(FastJsonWebConfig.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService storageService;

    private UploadedFile record;

    @BeforeEach
    void setUp() {
        // 换接线：原来 new 持久化模型再逐列 setter，现在按同值构造领域档案（restore = 不校验的重建）
        record = UploadedFile.restore("file-1", "hello.txt", "aa/" + "a".repeat(64) + ".txt",
                "text/plain", 5, "a".repeat(64), 1700000000000L);
    }

    @Test
    void uploadWithoutLoginIsIntercepted() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadStoresNewFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        when(storageService.store(any())).thenReturn(new FileStorageService.StoreResult(record, false));

        mockMvc.perform(multipart("/api/files").file(file).sessionAttr("CurrentUser", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deduplicated").value(false))
                .andExpect(jsonPath("$.data.originalName").value("hello.txt"))
                .andExpect(jsonPath("$.data.downloadUrl").value("/api/files/file-1/download"));
    }

    @Test
    void uploadDuplicateReportsDeduplicated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "again.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        when(storageService.store(any())).thenReturn(new FileStorageService.StoreResult(record, true));

        mockMvc.perform(multipart("/api/files").file(file).sessionAttr("CurrentUser", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deduplicated").value(true));
    }

    @Test
    void downloadStreamsFileWithUtf8AttachmentName(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("content.txt");
        Files.writeString(target, "hello");
        when(storageService.loadForDownload("file-1")).thenReturn(
                new FileStorageService.DownloadableFile("你好 世界.txt", "text/plain", target, 5));

        mockMvc.perform(get("/api/files/file-1/download").sessionAttr("CurrentUser", "alice"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("filename*=UTF-8''%E4%BD%A0%E5%A5%BD%20%E4%B8%96%E7%95%8C.txt")))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getContentAsByteArray()).asString(StandardCharsets.UTF_8).isEqualTo("hello"));
    }
}
