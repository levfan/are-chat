package com.smart.chat.sharedkernel;

/**
 * 对外产品名。欢迎语、审批推送、系统消息与日志前缀都从这里取值，
 * 改名只动这一处——散在各 Service 里的字符串会让下一改名的人漏掉某一条。
 */
public final class Brand {

    /** 中文站名：出现在用户看得见的每一句话里 */
    public static final String NAME = "天枢";

    /** 英文代号：只用于标识与文档，不进用户文案 */
    public static final String CODENAME = "DUBHE";

    private Brand() {
    }
}
