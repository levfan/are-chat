package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleWhisperMapper extends BaseMapperCompat<CoupleWhisper> {

    /** 空间的树洞（新→旧）。 */
    default List<CoupleWhisper> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWhisper>()
                .eq(CoupleWhisper::getSpaceId, spaceId)
                .orderByDesc(CoupleWhisper::getCreated));
    }

    /** 某人未回答的在途提问（每人同时最多一个）。 */
    default CoupleWhisper findPendingByUser(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleWhisper>()
                .eq(CoupleWhisper::getSpaceId, spaceId)
                .eq(CoupleWhisper::getFromUser, fromUser)
                .isNull(CoupleWhisper::getAnswer)
                .last("LIMIT 1"));
    }
}
