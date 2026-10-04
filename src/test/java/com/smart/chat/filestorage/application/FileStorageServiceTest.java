package com.smart.chat.filestorage.application;

import com.smart.chat.filestorage.domain.file.ContentAlreadyStored;
import com.smart.chat.filestorage.domain.file.UploadedFile;
import com.smart.chat.filestorage.domain.file.UploadedFileRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.bootstrap.properties.FileStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用例编排的接线：mock 的是领域端口 {@link UploadedFileRepository}（改造前是 UploadedFileMapper），
 * 断言的期望值与改造前一字不改——去重、落盘路径、异常文案都按原口径校验。
 */
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private UploadedFileRepository repository;

    @TempDir
    Path tempDir;

    private FileStorageService service;
    private final Map<String, UploadedFile> db = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new FileStorageService(repository, new FileStorageProperties(tempDir.toString()));
    }

    private MockMultipartFile upload(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes());
    }

    private void stubInMemoryDb() {
        doAnswer(inv -> {
            UploadedFile f = inv.getArgument(0);
            db.put(f.id(), f);
            return null;
        }).when(repository).save(any(UploadedFile.class));
        when(repository.findBySha256(anyString())).thenAnswer(inv ->
                db.values().stream()
                        .filter(f -> f.sha256().equals(inv.getArgument(0, String.class)))
                        .findFirst());
        lenient().when(repository.findById(anyString())).thenAnswer(inv ->
                Optional.ofNullable(db.get(inv.getArgument(0, String.class))));
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
        assertThat(result.file().sha256()).hasSize(64);
        assertThat(result.file().storedPath()).startsWith(result.file().sha256().substring(0, 2) + "/");
        Path stored = service.resolveStored(result.file().storedPath());
        assertThat(Files.readString(stored)).isEqualTo("hello world");
    }

    @Test
    void storeSameContentTwiceIsDeduplicated() {
        stubInMemoryDb();

        FileStorageService.StoreResult first = service.store(upload("a.txt", "same-content"));
        FileStorageService.StoreResult second = service.store(upload("b.txt", "same-content"));

        assertThat(first.deduplicated()).isFalse();
        assertThat(second.deduplicated()).isTrue();
        assertThat(second.file().id()).isEqualTo(first.file().id());
        verify(repository, times(1)).save(any(UploadedFile.class));
    }

    @Test
    void storeUniqueIndexRaceFallsBackToExisting() {
        // 首查为空触发落盘与 save，save 撞唯一索引（并发写入），此时库里已有那条记录
        UploadedFile concurrent = UploadedFile.restore("winner", "winner.txt", "aa/hash.txt", null, 5,
                FileStorageService.sha256Hex("same-content".getBytes(StandardCharsets.UTF_8)),
                System.currentTimeMillis());
        db.put("winner", concurrent);
        when(repository.findBySha256(anyString()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrent));
        doThrow(new ContentAlreadyStored(concurrent.sha256())).when(repository).save(any(UploadedFile.class));

        FileStorageService.StoreResult result = service.store(upload("mine.txt", "same-content"));

        assertThat(result.deduplicated()).isTrue();
        assertThat(result.file().id()).isEqualTo("winner");
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
