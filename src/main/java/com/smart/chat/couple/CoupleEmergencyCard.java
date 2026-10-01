package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 家庭应急卡（F184）：联系人/钥匙/药品清单文字版，各填一份互见。 */
@Data
@TableName("couple_emergency_card")
public class CoupleEmergencyCard {

    public static final int CONTACTS_MAX = 300;
    public static final int KEYS_MAX = 200;
    public static final int MEDICINE_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** 紧急联系人清单 */
    private String contacts;
    /** 备用钥匙/重要物品存放 */
    private String keysPlace;
    /** 常备药与过敏信息 */
    private String medicine;
    private Long updatedAt;
    private Long created;

    public static CoupleEmergencyCard of(String spaceId, String fromUser, String contacts, String keysPlace, String medicine) {
        CoupleEmergencyCard row = new CoupleEmergencyCard();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.contacts = contacts == null ? "" : contacts;
        row.keysPlace = keysPlace == null ? "" : keysPlace;
        row.medicine = medicine == null ? "" : medicine;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
