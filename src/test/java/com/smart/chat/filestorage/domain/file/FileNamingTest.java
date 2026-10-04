package com.smart.chat.filestorage.domain.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件名清洗裁决（从 FileStorageService 下沉到领域）。
 * 期望值与改造前 {@code FileStorageServiceTest} 里的同名断言一字不改，只是判定的归属变了。
 */
class FileNamingTest {

    @Test
    void sanitizeStripsPathAndIllegalChars() {
        assertThat(FileNaming.sanitize("a/b/../../evil?.txt")).doesNotContain("/").doesNotContain("..");
        assertThat(FileNaming.sanitize("..\\..\\etc\\passwd")).isEqualTo("passwd");
        assertThat(FileNaming.sanitize("x*?|<>y.txt")).isEqualTo("x_____y.txt");
        assertThat(FileNaming.sanitize("")).isEqualTo("unnamed");
        assertThat(FileNaming.sanitize("中文名.png")).isEqualTo("中文名.png");
    }

    @Test
    void longNamesAreTruncatedKeepingExtension() {
        String longName = "很长的名字".repeat(40) + ".txt";
        assertThat(FileNaming.sanitize(longName)).hasSize(100).endsWith(".txt");
    }

    @Test
    void extensionOfFollowsTheSameRulesAsBefore() {
        assertThat(FileNaming.extensionOf("a.txt")).isEqualTo(".txt");
        assertThat(FileNaming.extensionOf("a.PNG")).isEqualTo(".PNG");
        assertThat(FileNaming.extensionOf("noext")).isEmpty();
        assertThat(FileNaming.extensionOf(".hidden")).isEmpty();
        assertThat(FileNaming.extensionOf("trailing.")).isEmpty();
        assertThat(FileNaming.extensionOf("a." + "x".repeat(16))).isEmpty();
    }
}
