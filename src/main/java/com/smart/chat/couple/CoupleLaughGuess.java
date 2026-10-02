package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F395 笑点默契考：同一条冷笑话，两人各自预判「对方会不会笑」，两份预判一致就算默契。
 * 一行一票：uk(space_id,joke_id,from_user) 保证一条梗每人只有一票，改票走 find() 回填 update。
 * 没有 day 列——预判挂在被考的 joke 上，要按天/按年统计得读 created 毫秒或回查 couple_laugh_joke.day。
 */
@Data
@TableName("couple_laugh_guess")
public class CoupleLaughGuess {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 考的是哪条冷笑话（couple_laugh_joke.id） */
    private String jokeId;
    /** 做预判的人（区分大小写） */
    private String fromUser;
    /** 预判对方会不会笑：1=会笑，0=不会笑 */
    private Integer predict;
    private Long created;
    private Long updatedAt;

    public static CoupleLaughGuess of(String spaceId, String jokeId, String fromUser, int predict) {
        CoupleLaughGuess row = new CoupleLaughGuess();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.jokeId = jokeId;
        row.fromUser = fromUser;
        row.predict = predict;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 这一票投的是「TA 会笑」。 */
    public boolean predictsLaugh() {
        return predict != null && predict == 1;
    }
}
