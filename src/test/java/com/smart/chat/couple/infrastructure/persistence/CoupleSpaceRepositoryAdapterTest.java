package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.space.CoupleSpace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 仓储适配器的写回口径：聚合只维护自己那几列，存储里聚合不认识的列必须原样留着。
 * 这条断言防的是「用聚合重建整行、把没建模到的列静默清空」——这类 bug 编译与业务测试都照不出来。
 * 2026-10-20 三轮裁剪后，爱称/宣言/主题三列也归进「聚合不认识」那一侧：功能下线了，
 * 老值留在库里但谁也不读写，适配器要是拿 null 去覆盖就等于顺手抹掉别人的历史。
 */
@ExtendWith(MockitoExtension.class)
class CoupleSpaceRepositoryAdapterTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;

    @InjectMocks
    private CoupleSpaceRepositoryAdapter repository;

    @Test
    void updateKeepsColumnsTheAggregateDoesNotOwn() {
        CoupleSpacePO existing = CoupleSpacePO.of("alice", "bob");
        existing.setId("s1");
        existing.setCityA("杭州");
        existing.setCityB("厦门");
        existing.setNickA("宝宝");
        existing.setSlogan("宣言");
        existing.setTheme("ocean");
        when(spaceMapper.selectById("s1")).thenReturn(existing);

        CoupleSpace space = CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 1L,
                "2026-10-01", null);
        repository.save(space);

        ArgumentCaptor<CoupleSpacePO> captor = ArgumentCaptor.forClass(CoupleSpacePO.class);
        verify(spaceMapper).updateById(captor.capture());
        CoupleSpacePO written = captor.getValue();
        assertThat(written.getCityA()).as("聚合没建模的列不能被动").isEqualTo("杭州");
        assertThat(written.getCityB()).isEqualTo("厦门");
        assertThat(written.getNickA()).isEqualTo("宝宝");
        assertThat(written.getSlogan()).isEqualTo("宣言");
        assertThat(written.getTheme()).isEqualTo("ocean");
        assertThat(written.getAnniversary()).isEqualTo("2026-10-01");
        verify(spaceMapper, never()).insert(any(CoupleSpacePO.class));
    }

    @Test
    void newSpaceIsInsertedWithBothMembersAndOwnership() {
        when(spaceMapper.selectById("s2")).thenReturn(null);
        CoupleSpace space = CoupleSpace.restore("s2", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 123L, null, null);

        repository.save(space);

        ArgumentCaptor<CoupleSpacePO> captor = ArgumentCaptor.forClass(CoupleSpacePO.class);
        verify(spaceMapper).insert(captor.capture());
        CoupleSpacePO po = captor.getValue();
        assertThat(po.getId()).isEqualTo("s2");
        assertThat(po.getUserA()).isEqualTo("alice");
        assertThat(po.getUserB()).isEqualTo("bob");
        assertThat(po.getCreated()).isEqualTo(123L);
        verify(spaceMapper, never()).updateById(any(CoupleSpacePO.class));
    }

    @Test
    void findActiveByMemberMapsStorageBackIntoTheAggregate() {
        CoupleSpacePO po = CoupleSpacePO.of("alice", "bob");
        po.setId("s3");
        po.setAnniversary("2026-06-01");
        when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(po));

        CoupleSpace space = repository.findActiveByMember("bob").orElseThrow();

        assertThat(space.id()).isEqualTo("s3");
        assertThat(space.partnerOf("bob")).isEqualTo("alice");
        assertThat(space.anniversary()).isEqualTo("2026-06-01");
    }

    @Test
    void batchReadsMapEveryRowIntoAnAggregate() {
        CoupleSpacePO active = CoupleSpacePO.of("alice", "bob");
        active.setId("s4");
        CoupleSpacePO dissolved = CoupleSpacePO.of("carol", "dave");
        dissolved.setId("s5");
        dissolved.setStatus(CoupleSpacePO.STATUS_DISSOLVED);
        when(spaceMapper.findAllActive()).thenReturn(List.of(active));
        when(spaceMapper.selectById("s5")).thenReturn(dissolved);

        assertThat(repository.findAllActive()).extracting(CoupleSpace::id).containsExactly("s4");
        assertThat(repository.findById("s5").orElseThrow().status()).isEqualTo(CoupleSpace.STATUS_DISSOLVED);
    }
}
