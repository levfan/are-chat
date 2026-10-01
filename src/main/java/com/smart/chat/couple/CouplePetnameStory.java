package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F283 外号考据：每个爱称的诞生故事，存入百科。 */
@Data
@TableName("couple_petname_story")
public class CouplePetnameStory {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String nickname;
    private String givenBy;
    private String occasion;
    private String story;
    private String firstUsedDay;
    private String fromUser;
    private Long created;
    private Long updatedAt;

    public static CouplePetnameStory of(String spaceId, String nickname, String givenBy, String occasion,
                                       String story, String firstUsedDay, String fromUser) {
        CouplePetnameStory row = new CouplePetnameStory();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.nickname = nickname;
        row.givenBy = givenBy == null ? "" : givenBy;
        row.occasion = occasion == null ? "" : occasion;
        row.story = story == null ? "" : story;
        row.firstUsedDay = firstUsedDay == null ? "" : firstUsedDay;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
