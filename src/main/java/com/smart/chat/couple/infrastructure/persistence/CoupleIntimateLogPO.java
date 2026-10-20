package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 亲密记录行：一行 = 一次边缘亲昵或性生活，同一天可以有多行。
 */
@Data
@TableName("couple_intimacy_log")
public class CoupleIntimateLogPO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String kind;
    private String note;
    private String recorderUser;
    private Long created;
}
