package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F335 进城接待方案：TA 来访城市的行程/交通/陪同小包共建手册。 */
@Data
@TableName("couple_world_city_plan")
public class CoupleWorldCityPlan {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String city;
    private String arriveDay;
    private String fromUser;
    private String itinerary;
    private String transport;
    private String packList;
    private Long created;
    private Long updatedAt;

    public static CoupleWorldCityPlan of(String spaceId, String city, String arriveDay, String fromUser) {
        CoupleWorldCityPlan row = new CoupleWorldCityPlan();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.city = city;
        row.arriveDay = arriveDay == null ? "" : arriveDay;
        row.fromUser = fromUser;
        row.itinerary = "";
        row.transport = "";
        row.packList = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
