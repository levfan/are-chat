package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F373 陪护代记数据访问（uk(nurse_id,day,kind,by_user) 一天每种只一条）。 */
@Mapper
public interface CoupleQuestCareMarkMapper extends BaseMapperCompat<CoupleQuestCareMark> {

    /** 某单的全部代记（打卡日渐升序，画陪护进度条用）。 */
    default List<CoupleQuestCareMark> findByNurse(String nurseId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestCareMark>()
                .eq(CoupleQuestCareMark::getNurseId, nurseId)
                .orderByAsc(CoupleQuestCareMark::getDay));
    }

    /** 按 uk 定位某单某天那一种代记（写前查重用）。 */
    default CoupleQuestCareMark find(String nurseId, String day, String kind, String byUser) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestCareMark>()
                .eq(CoupleQuestCareMark::getNurseId, nurseId)
                .eq(CoupleQuestCareMark::getDay, day)
                .eq(CoupleQuestCareMark::getKind, kind)
                .eq(CoupleQuestCareMark::getByUser, byUser));
    }
}
