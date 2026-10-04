package com.smart.chat.filestorage.domain.file;

import com.smart.chat.filestorage.domain.RuleViolation;

import java.nio.file.Path;

/**
 * 落盘路径规则与目录安全闸门。
 * <p>
 * 两件事都是领域裁决：
 * <ul>
 *   <li><b>内容寻址目录</b>：{@code ab/<sha256><扩展名>}——前两位分片避免单目录爆量，同内容永远同一个路径；</li>
 *   <li><b>防目录穿越</b>：把相对路径拼到存储根目录之前先规范化，一旦逃出根目录就拒绝（历史数据里
 *       任何被人为改成的 {@code ../} 路径都过不去）。根目录本身来自配置，由 application 传入，
 *       「能不能出圈」这件事由这里判。</li>
 * </ul>
 */
public final class StoragePath {

    /** 分片目录取的指纹前缀长度 */
    public static final int SHARD_PREFIX_LENGTH = 2;

    private StoragePath() {
    }

    /** 由内容指纹与（小写）扩展名推出相对落盘路径。 */
    public static String storedPathFor(String sha256, String extension) {
        return sha256.substring(0, SHARD_PREFIX_LENGTH) + "/" + sha256 + (extension == null ? "" : extension);
    }

    /**
     * 把相对路径解析到存储根目录下，并拒绝任何逃出根目录的写法。
     *
     * @param base 已规范化的存储根目录（绝对路径）
     */
    public static Path resolveUnder(Path base, String storedPath) {
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path resolved = normalizedBase.resolve(storedPath).normalize();
        if (!resolved.startsWith(normalizedBase)) {
            throw new RuleViolation("非法的文件路径：" + storedPath);
        }
        return resolved;
    }
}
