package com.smart.chat.filestorage.domain.file;

/**
 * 文件命名的清洗裁决：把客户端传来的原始名字变成一个能安全落盘、能安全回显的名字。
 * <p>
 * 三步都是安全判定，不是显示技巧，所以归领域：
 * <ol>
 *   <li>剥掉路径部分（{@code a/b/../../etc/passwd} 只留最后一段）——上传侧的目录穿越入口；</li>
 *   <li>删控制字符、把 {@code \ / : * ? " &lt; &gt; |} 换成下划线——避免写出逃逸名字；</li>
 *   <li>空名兜底为 {@code unnamed}，超过 {@link #NAME_MAX} 时保留扩展名再截断。</li>
 * </ol>
 * 逐字符口径与改造前的 Service 实现一致（含「扩展名长于 16 个字符视为无扩展名」这条）。
 */
public final class FileNaming {

    /** 清洗后的文件名长度上限 */
    public static final int NAME_MAX = 100;
    /** 超过这个长度的「扩展名」不当扩展名看待 */
    public static final int EXTENSION_MAX = 16;

    private FileNaming() {
    }

    /** 清洗原始文件名（null 视作空，最终一定得到非空名字）。 */
    public static String sanitize(String original) {
        String name = original == null ? "" : original;
        int lastSep = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSep >= 0) {
            name = name.substring(lastSep + 1);
        }
        name = name.replaceAll("\\p{Cntrl}", "")
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .trim();
        if (name.isEmpty()) {
            name = "unnamed";
        }
        if (name.length() > NAME_MAX) {
            String ext = extensionOf(name);
            name = name.substring(0, NAME_MAX - ext.length()) + ext;
        }
        return name;
    }

    /** 取扩展名（含前导点、保留原始大小写）；无扩展名或过长返回空串。 */
    public static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        String ext = name.substring(dot);
        return ext.length() > EXTENSION_MAX ? "" : ext;
    }
}
