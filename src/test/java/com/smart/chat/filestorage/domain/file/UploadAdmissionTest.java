package com.smart.chat.filestorage.domain.file;

import com.smart.chat.filestorage.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 上传准入裁决（96 加固口径）：文案与拒绝范围都是对外契约，逐条锁住。 */
class UploadAdmissionTest {

    @Test
    void emptyContentIsRejected() {
        assertThatThrownBy(() -> UploadAdmission.admitContent(0))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("不能上传空文件");
        assertThatCode(() -> UploadAdmission.admitContent(1)).doesNotThrowAnyException();
    }

    @Test
    void dangerousExtensionsAreRejected() {
        for (String ext : new String[]{".exe", ".HTML", ".svg", ".sh", ".jar", ".ps1", ".dll"}) {
            assertThatThrownBy(() -> UploadAdmission.admitExtension(ext))
                    .as("黑名单里的 " + ext + " 必须拒收")
                    .isInstanceOf(RuleViolation.class)
                    .hasMessage("不允许上传 " + ext.toLowerCase() + " 类型的文件");
        }
    }

    @Test
    void ordinaryExtensionsPassAndComeBackLowercased() {
        // 落盘路径用的是小写扩展名，这个口径从改造前就带着，测试锁住
        assertThat(UploadAdmission.admitExtension(".PNG")).isEqualTo(".png");
        assertThat(UploadAdmission.admitExtension("")).isEmpty();
        assertThat(UploadAdmission.admitExtension(null)).isEmpty();
        assertThatCode(() -> UploadAdmission.admitExtension(".txt")).doesNotThrowAnyException();
    }

    @Test
    void blacklistIsTheDocumentedSet() {
        assertThat(UploadAdmission.BLOCKED_EXTENSIONS).containsExactlyInAnyOrder(
                ".exe", ".msi", ".bat", ".cmd", ".com", ".scr", ".ps1", ".vbs", ".vbe", ".js", ".jse",
                ".wsf", ".wsh", ".hta", ".cpl", ".jar", ".sh", ".apk", ".html", ".htm", ".svg", ".dll");
    }
}
