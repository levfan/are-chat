package com.smart.chat.filestorage.infrastructure.persistence;

import com.smart.chat.filestorage.domain.file.ContentAlreadyStored;
import com.smart.chat.filestorage.domain.file.UploadedFile;
import com.smart.chat.filestorage.domain.file.UploadedFileRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link UploadedFileRepository} 的 MyBatis-Plus 适配器：PO ↔ 领域双向翻译只发生在这里。
 * <p>
 * 写回口径沿用 {@code CoupleSpaceRepositoryAdapter} 的纪律——<b>只回写聚合纳管的列</b>：
 * uploaded_file 的七列都在聚合里，但内容相关的四列（original_name / stored_path / size / sha256）
 * 与 id、uploaded_at 一样<b>登记即冻结</b>，聚合没有改写它们的方法，所以更新分支只碰 content_type；
 * 真要给档案加改写用例，就在 {@link #applyOwnedFields} 里显式扩列，不靠整行重建。
 * 唯一索引冲突在这里翻译成领域事实 {@link ContentAlreadyStored}，Spring 异常不外泄。
 */
@Component
public class UploadedFileRepositoryAdapter implements UploadedFileRepository {

    private final UploadedFileMapper mapper;

    public UploadedFileRepositoryAdapter(UploadedFileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UploadedFile> findBySha256(String sha256) {
        return mapper.findBySha256(sha256).map(UploadedFileRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<UploadedFile> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(UploadedFileRepositoryAdapter::toDomain);
    }

    @Override
    public void save(UploadedFile file) {
        UploadedFilePO existing = mapper.selectById(file.id());
        if (existing == null) {
            try {
                mapper.insert(toPO(file));
            } catch (DuplicateKeyException e) {
                // 并发上传同一内容：uq_uploaded_file_sha256 兜底，翻译成领域事实交给用例走「已去重」分支
                throw new ContentAlreadyStored(file.sha256());
            }
            return;
        }
        applyOwnedFields(existing, file);
        mapper.updateById(existing);
    }

    /** 聚合负责维护的列（登记即冻结的那些不在这里） */
    private static void applyOwnedFields(UploadedFilePO po, UploadedFile file) {
        po.setContentType(file.contentType());
    }

    private static UploadedFilePO toPO(UploadedFile file) {
        UploadedFilePO po = new UploadedFilePO();
        po.setId(file.id());
        po.setOriginalName(file.originalName());
        po.setStoredPath(file.storedPath());
        po.setContentType(file.contentType());
        po.setSize(file.size());
        po.setSha256(file.sha256());
        po.setUploadedAt(file.uploadedAt());
        return po;
    }

    private static UploadedFile toDomain(UploadedFilePO po) {
        return UploadedFile.restore(po.getId(), po.getOriginalName(), po.getStoredPath(), po.getContentType(),
                po.getSize(), po.getSha256(), po.getUploadedAt());
    }
}
