package com.smart.chat.filestorage.domain.file;

import com.smart.chat.filestorage.domain.RuleViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 内容寻址的落盘路径与目录安全闸门：防目录穿越必须在领域层判，这里逐条按住。 */
class StoragePathTest {

    private static final String SHA = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @TempDir
    Path tempDir;

    @Test
    void storedPathIsShardedByFingerprintPrefix() {
        assertThat(StoragePath.storedPathFor(SHA, ".txt")).isEqualTo(SHA.substring(0, 2) + "/" + SHA + ".txt");
        assertThat(StoragePath.storedPathFor(SHA, "")).isEqualTo(SHA.substring(0, 2) + "/" + SHA);
    }

    @Test
    void resolveUnderKeepsRelativePathInsideTheBase() {
        Path resolved = StoragePath.resolveUnder(tempDir, SHA.substring(0, 2) + "/" + SHA + ".txt");

        assertThat(resolved.startsWith(tempDir.toAbsolutePath().normalize())).isTrue();
    }

    @Test
    void resolveUnderRejectsTraversal() {
        assertThatThrownBy(() -> StoragePath.resolveUnder(tempDir, "../outside.txt"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("非法的文件路径：../outside.txt");
        assertThatThrownBy(() -> StoragePath.resolveUnder(tempDir, "aa/../../escape.txt"))
                .isInstanceOf(RuleViolation.class);
    }

    @Test
    void resolveUnderRejectsAbsolutePathOutsideTheBase() {
        // 绝对路径写进 stored_path 列（人为改库）时也一样出不了圈
        assertThatThrownBy(() -> StoragePath.resolveUnder(tempDir, "/etc/passwd"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("非法的文件路径：/etc/passwd");
    }
}
