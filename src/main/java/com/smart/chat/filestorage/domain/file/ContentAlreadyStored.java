package com.smart.chat.filestorage.domain.file;

/**
 * 同一份内容已被并发写入（{@code uq_uploaded_file_sha256} 唯一索引兜底）。
 * <p>
 * 这是「秒传」的正常结果而不是故障：仓储端口把它作为一个领域事实抛出来，
 * application 收到后改查已有记录并按「已去重」返回，对外 JSON 只表现 {@code deduplicated=true}。
 * 这样 Spring 的 {@code DuplicateKeyException} 就被封在 infrastructure 内，不再渗进用例层。
 */
public class ContentAlreadyStored extends RuntimeException {

    private final String sha256;

    public ContentAlreadyStored(String sha256) {
        super("同一内容已在库中：" + sha256);
        this.sha256 = sha256;
    }

    /** 撞车的指纹（并发的另一条写入就是它） */
    public String sha256() {
        return sha256;
    }
}
