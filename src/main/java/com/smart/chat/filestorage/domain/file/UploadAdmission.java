package com.smart.chat.filestorage.domain.file;

import com.smart.chat.filestorage.domain.RuleViolation;

import java.util.Set;

/**
 * 上传准入裁决（96 上传安全加固的产品口径，全部落在这里，Service 不再自己 if）。
 * <ul>
 *   <li>空文件不收；</li>
 *   <li>危险扩展名黑名单（可执行 / 脚本 / 服务端页面 / 脚本宿主）一律拒绝，防止借下载链接分发或落盘执行。</li>
 * </ul>
 * 注意口径是<b>黑名单</b>而不是白名单：除黑名单外的一律放行，这是现有对外行为，改造期不得收紧。
 * 大小上限（20MB / 25MB）由 {@code spring.servlet.multipart} 在容器层拦，领域里再判一次会变成双重口径，故不搬。
 */
public final class UploadAdmission {

    /** 危险扩展名黑名单（全小写，含前导点）——对外文案直接引用其中的值，不得改名 */
    public static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe", ".msi", ".bat", ".cmd", ".com", ".scr", ".ps1", ".vbs", ".vbe", ".js", ".jse",
            ".wsf", ".wsh", ".hta", ".cpl", ".jar", ".sh", ".apk", ".html", ".htm", ".svg", ".dll");

    private UploadAdmission() {
    }

    /** 空文件不收：文案与 code（400）沿用现有对外口径。 */
    public static void admitContent(long byteSize) {
        if (byteSize <= 0) {
            throw new RuleViolation("不能上传空文件");
        }
    }

    /**
     * 危险扩展名不收。入参是原始大小写的扩展名，返回<b>小写</b>形态——
     * 现有实现里落盘路径用的就是小写扩展名，这里必须保持同一口径。
     */
    public static String admitExtension(String extension) {
        // 刻意沿用原有的 toLowerCase()（默认 Locale）：改造期不改变任何对外行为，
        // Locale 口径问题作为疑似缺陷单独上报，不在本轮顺手改。
        String lowered = extension == null ? "" : extension.toLowerCase();
        if (BLOCKED_EXTENSIONS.contains(lowered)) {
            throw new RuleViolation("不允许上传 " + lowered + " 类型的文件");
        }
        return lowered;
    }
}
