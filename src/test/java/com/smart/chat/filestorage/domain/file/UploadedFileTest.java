package com.smart.chat.filestorage.domain.file;

import com.smart.chat.filestorage.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 文件档案自身的不变式：登记时的闸门、重建时的宽松、以及无 MIME 的对外口径。 */
class UploadedFileTest {

    private static final String SHA = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @Test
    void registerIssuesIdAndTimestampAndKeepsWhatItWasGiven() {
        long before = System.currentTimeMillis();
        UploadedFile file = UploadedFile.register("hello.txt", "2c/" + SHA + ".txt", "text/plain", 5, SHA);

        assertThat(file.id()).isNotBlank();
        assertThat(file.originalName()).isEqualTo("hello.txt");
        assertThat(file.storedPath()).isEqualTo("2c/" + SHA + ".txt");
        assertThat(file.contentType()).isEqualTo("text/plain");
        assertThat(file.size()).isEqualTo(5);
        assertThat(file.sha256()).isEqualTo(SHA);
        assertThat(file.uploadedAt()).isGreaterThanOrEqualTo(before);
    }

    @Test
    void registerRejectsBlankName() {
        assertThatThrownBy(() -> UploadedFile.register("  ", "2c/" + SHA, "text/plain", 5, SHA))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("文件必须有名字");
    }

    @Test
    void registerRejectsEmptyContent() {
        assertThatThrownBy(() -> UploadedFile.register("a.txt", "2c/" + SHA, "text/plain", 0, SHA))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("不能上传空文件");
    }

    @Test
    void registerRejectsFakeFingerprint() {
        assertThatThrownBy(() -> UploadedFile.register("a.txt", "2c/" + SHA, "text/plain", 5, "not-a-sha"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("文件指纹不合法");
        assertThat(ContentFingerprint.looksLikeSha256(SHA)).isTrue();
        assertThat(ContentFingerprint.looksLikeSha256(SHA.substring(1))).isFalse();
    }

    @Test
    void restoreDoesNotValidateLegacyRows() {
        UploadedFile legacy = UploadedFile.restore("old-1", null, null, null, 0, null, null);

        assertThat(legacy.id()).isEqualTo("old-1");
        assertThat(legacy.originalName()).isNull();
        assertThat(legacy.uploadedAt()).isNull();
    }

    @Test
    void missingContentTypeIsServedAsBinaryStream() {
        assertThat(UploadedFile.restore("i", "a.txt", "aa/i.txt", null, 1, SHA, 1L).contentTypeOrBinary())
                .isEqualTo("application/octet-stream");
        assertThat(UploadedFile.restore("i", "a.txt", "aa/i.txt", "image/png", 1, SHA, 1L).contentTypeOrBinary())
                .isEqualTo("image/png");
    }
}
