package com.smart.chat.upload;

import com.smart.chat.common.BusinessException;
import com.smart.chat.config.FileStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private UploadedFileMapper mapper;

    @TempDir
    Path tempDir;

    private FileStorageService service;
    private final Map<String, UploadedFile> db = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new FileStorageService(mapper, new FileStorageProperties(tempDir.toString()));
    }

    private MockMultipartFile upload(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes());
    }

    private void stubInMemoryDb() {
        when(mapper.insert(any(UploadedFile.class))).thenAnswer(inv -> {
            UploadedFile f = inv.getArgument(0);
            db.put(f.getId(), f);
            return 1;
        });
        when(mapper.findBySha256(anyString())).thenAnswer(inv ->
                db.values().stream()
                        .filter(f -> f.getSha256().equals(inv.getArgument(0, String.class)))
                        .findFirst());
        lenient().when(mapper.selectById(anyString())).thenAnswer(inv ->
                db.get(inv.getArgument(0, String.class)));
        lenient().doAnswer(inv -> {
            db.remove(inv.getArgument(0, String.class));
            return 1;
        }).when(mapper).deleteById(anyString());
    }

    @Test
    void sha256MatchesKnownVector() {
        assertThat(FileStorageService.sha256Hex("hello".getBytes()))
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void storeSavesContentAddressedFile() throws IOException {
        stubInMemoryDb();

        FileStorageService.StoreResult result = service.store(upload("照片.png", "hello world"));

        assertThat(result.deduplicated()).isFalse();
        assertThat(result.file().getSha256()).hasSize(64);
        assertThat(result.file().getStoredPath()).startsWith(result.file().getSha256().substring(0, 2) + "/");
        Path stored = service.resolveStored(result.file().getStoredPath());
        assertThat(Files.readString(stored)).isEqualTo("hello world");
    }

    @Test
    void storeSameContentTwiceIsDeduplicated() {
        stubInMemoryDb();

        FileStorageService.StoreResult first = service.store(upload("a.txt", "same-content"));
        FileStorageService.StoreResult second = service.store(upload("b.txt", "same-content"));

        assertThat(first.deduplicated()).isFalse();
        assertThat(second.deduplicated()).isTrue();
        assertThat(second.file().getId()).isEqualTo(first.file().getId());
        verify(mapper, times(1)).insert(any(UploadedFile.class));
    }

    @Test
    void storeUniqueIndexRaceFallsBackToExisting() {
        // 首查为空触发落盘与 save，save 抛唯一索引冲突，此时库里已有并发写入的记录
        UploadedFile concurrent = new UploadedFile();
        concurrent.setId("winner");
        concurrent.setOriginalName("winner.txt");
        concurrent.setStoredPath("aa/hash.txt");
        concurrent.setSize(5);
        concurrent.setSha256(FileStorageService.sha256Hex("same-content".getBytes(StandardCharsets.UTF_8)));
        concurrent.setUploadedAt(System.currentTimeMillis());
        db.put("winner", concurrent);
        when(mapper.findBySha256(anyString()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrent));
        when(mapper.insert(any(UploadedFile.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        FileStorageService.StoreResult result = service.store(upload("mine.txt", "same-content"));

        assertThat(result.deduplicated()).isTrue();
        assertThat(result.file().getId()).isEqualTo("winner");
    }

    @Test
    void storeRejectsEmptyFile() {
        assertThatThrownBy(() -> service.store(upload("empty.txt", "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("空文件");
    }

    @Test
    void sanitizeNameStripsPathAndIllegalChars() {
        assertThat(FileStorageService.sanitizeName("a/b/../../evil?.txt")).doesNotContain("/").doesNotContain("..");
        assertThat(FileStorageService.sanitizeName("..\\..\\etc\\passwd")).isEqualTo("passwd");
        assertThat(FileStorageService.sanitizeName("x*?|<>y.txt")).isEqualTo("x_____y.txt");
        assertThat(FileStorageService.sanitizeName("")).isEqualTo("unnamed");
        assertThat(FileStorageService.sanitizeName("中文名.png")).isEqualTo("中文名.png");
    }

    @Test
    void resolveStoredRejectsTraversal() {
        assertThatThrownBy(() -> service.resolveStored("../outside.txt"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法");
        assertThatThrownBy(() -> service.resolveStored("aa/../../escape.txt"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void longNamesAreTruncated() {
        String longName = "很长的名字".repeat(40) + ".txt";
        assertThat(FileStorageService.sanitizeName(longName).length()).isLessThanOrEqualTo(100);
    }
}
