package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F224 冷暖互报：自报城市+气温体感（纯文字），对方一键叮嘱添衣。 */
@Data
@TableName("couple_cozy_weather")
public class CoupleCozyWeather {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String city;
    private String feel;
    private String tempText;
    private String advisedBy;
    private Long created;

    public static CoupleCozyWeather of(String spaceId, String day, String fromUser, String city, String feel, String tempText) {
        CoupleCozyWeather row = new CoupleCozyWeather();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.city = city;
        row.feel = feel;
        row.tempText = tempText == null ? "" : tempText;
        row.advisedBy = "";
        row.created = System.currentTimeMillis();
        return row;
    }
}
