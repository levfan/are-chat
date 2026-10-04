package com.smart.chat.filestorage.infrastructure.persistence;

import com.smart.chat.filestorage.domain.file.ContentAlreadyStored;
import com.smart.chat.filestorage.domain.file.UploadedFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件档案仓储适配器的两个口径：
 * ① PO ↔ 领域双向翻译不丢列；② 更新<b>只回写聚合纳管的列</b>（登记即冻结的那几列一律不碰），
 * 以及唯一索引冲突被翻译成领域事实而不是把 Spring 异常递出 infrastructure。
 */
@ExtendWith(MockitoExtension.class)
class UploadedFileRepositoryAdapterTest {

    private static final String SHA = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @Mock
    private UploadedFileMapper mapper;

    @InjectMocks
    private UploadedFileRepositoryAdapter repository;

    @Test
    void findBySha256MapsStorageBackIntoTheDomainRecord() {
        UploadedFilePO po = new UploadedFilePO();
        po.setId("f1");
        po.setOriginalName("hello.txt");
        po.setStoredPath("2c/" + SHA + ".txt");
        po.setContentType("text/plain");
        po.setSize(5);
        po.setSha256(SHA);
        po.setUploadedAt(1700000000000L);
        when(mapper.findBySha256(SHA)).thenReturn(Optional.of(po));

        UploadedFile file = repository.findBySha256(SHA).orElseThrow();

        assertThat(file.id()).isEqualTo("f1");
        assertThat(file.originalName()).isEqualTo("hello.txt");
        assertThat(file.storedPath()).isEqualTo("2c/" + SHA + ".txt");
        assertThat(file.contentType()).isEqualTo("text/plain");
        assertThat(file.size()).isEqualTo(5);
        assertThat(file.sha256()).isEqualTo(SHA);
        assertThat(file.uploadedAt()).isEqualTo(1700000000000L);
    }

    @Test
    void findByIdReturnsEmptyWhenRowIsGone() {
        when(mapper.selectById("nope")).thenReturn(null);

        assertThat(repository.findById("nope")).isEmpty();
    }

    @Test
    void newRecordIsInsertedWithAllColumns() {
        UploadedFile file = UploadedFile.restore("f2", "a.png", "2c/" + SHA + ".png", "image/png", 7, SHA, 42L);
        when(mapper.selectById("f2")).thenReturn(null);

        repository.save(file);

        ArgumentCaptor<UploadedFilePO> captor = ArgumentCaptor.forClass(UploadedFilePO.class);
        verify(mapper).insert(captor.capture());
        UploadedFilePO po = captor.getValue();
        assertThat(po.getId()).isEqualTo("f2");
        assertThat(po.getOriginalName()).isEqualTo("a.png");
        assertThat(po.getStoredPath()).isEqualTo("2c/" + SHA + ".png");
        assertThat(po.getContentType()).isEqualTo("image/png");
        assertThat(po.getSize()).isEqualTo(7);
        assertThat(po.getSha256()).isEqualTo(SHA);
        assertThat(po.getUploadedAt()).isEqualTo(42L);
        verify(mapper, never()).updateById(any(UploadedFilePO.class));
    }

    @Test
    void updateKeepsColumnsTheAggregateDoesNotMaintain() {
        // 库里的行与聚合携带的值故意不同：登记即冻结的列（名字/路径/大小/指纹/上传时刻）必须原样留着
        UploadedFilePO existing = new UploadedFilePO();
        existing.setId("f3");
        existing.setOriginalName("库里登记的名字.txt");
        existing.setStoredPath("2c/库里的路径.txt");
        existing.setSize(9);
        existing.setSha256("b".repeat(64));
        existing.setUploadedAt(111L);
        existing.setContentType(null);
        when(mapper.selectById("f3")).thenReturn(existing);

        repository.save(UploadedFile.restore("f3", "聚合里的名字.txt", "2c/聚合里的路径.txt", "text/plain",
                1234, SHA, 222L));

        ArgumentCaptor<UploadedFilePO> captor = ArgumentCaptor.forClass(UploadedFilePO.class);
        verify(mapper).updateById(captor.capture());
        UploadedFilePO written = captor.getValue();
        assertThat(written.getOriginalName()).as("登记即冻结的列不能被动").isEqualTo("库里登记的名字.txt");
        assertThat(written.getStoredPath()).isEqualTo("2c/库里的路径.txt");
        assertThat(written.getSize()).isEqualTo(9);
        assertThat(written.getSha256()).isEqualTo("b".repeat(64));
        assertThat(written.getUploadedAt()).isEqualTo(111L);
        assertThat(written.getContentType()).as("聚合纳管的列才回写").isEqualTo("text/plain");
        verify(mapper, never()).insert(any(UploadedFilePO.class));
    }

    @Test
    void uniqueIndexConflictBecomesADomainFact() {
        UploadedFile file = UploadedFile.restore("f4", "a.txt", "2c/" + SHA, "text/plain", 3, SHA, 1L);
        when(mapper.selectById("f4")).thenReturn(null);
        when(mapper.insert(any(UploadedFilePO.class))).thenThrow(new DuplicateKeyException("uq_uploaded_file_sha256"));

        assertThatThrownBy(() -> repository.save(file))
                .isInstanceOf(ContentAlreadyStored.class)
                .extracting("sha256").isEqualTo(SHA);
    }
}
