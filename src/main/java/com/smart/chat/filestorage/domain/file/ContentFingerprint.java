package com.smart.chat.filestorage.domain.file;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 内容指纹：文件「秒传去重」的身份证（SHA-256 十六进制小写）。
 * <p>
 * 内容寻址是 filestorage 的统一语言——同一个内容全站只存一份，靠的就是这个值，
 * 所以「怎么算指纹」属于领域，不属于 Service。
 */
public final class ContentFingerprint {

    /** SHA-256 十六进制长度 */
    public static final int HEX_LENGTH = 64;

    private ContentFingerprint() {
    }

    /** 算内容指纹（小写十六进制，定长 {@value #HEX_LENGTH}）。 */
    public static String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /** 是否像一个真实指纹（长度与十六进制口径）。 */
    public static boolean looksLikeSha256(String value) {
        if (value == null || value.length() != HEX_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }
}
