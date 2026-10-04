package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.space.CoupleSpace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 仓储适配器的写回口径：聚合只维护自己那几列，存储里聚合不认识的列（cityA/cityB）必须原样留着。
 * 这条断言防的是「用聚合重建整行、把没建模到的列静默清空」——这类 bug 编译与业务测试都照不出来。
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
        existing.setTheme("ocean");
        when(spaceMapper.selectById("s1")).thenReturn(existing);

        CoupleSpace space = CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 1L,
                "2026-10-01", "宝宝", null, "宣言", "cherry", null, null);
        repository.save(space);

        ArgumentCaptor<CoupleSpacePO> captor = ArgumentCaptor.forClass(CoupleSpacePO.class);
        verify(spaceMapper).updateById(captor.capture());
        CoupleSpacePO written = captor.getValue();
        assertThat(written.getCityA()).as("聚合没建模的列不能被动").isEqualTo("杭州");
        assertThat(written.getCityB()).isEqualTo("厦门");
        assertThat(written.getAnniversary()).isEqualTo("2026-10-01");
        assertThat(written.getNickA()).isEqualTo("宝宝");
        assertThat(written.getSlogan()).isEqualTo("宣言");
        assertThat(written.getTheme()).isEqualTo("cherry");
        verify(spaceMapper, never()).insert(any(CoupleSpacePO.class));
    }

    @Test
    void newSpaceIsInsertedWithBothMembersAndOwnership() {
        when(spaceMapper.selectById("s2")).thenReturn(null);
        CoupleSpace space = CoupleSpace.restore("s2", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 123L,
                null, null, null, null, "classic", null, null);

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
        po.setNickA("宝宝");
        when(spaceMapper.findActiveByUser("bob")).thenReturn(java.util.Optional.of(po));

        CoupleSpace space = repository.findActiveByMember("bob").orElseThrow();

        assertThat(space.id()).isEqualTo("s3");
        assertThat(space.partnerOf("bob")).isEqualTo("alice");
        assertThat(space.nickOf("alice")).isEqualTo("宝宝");
    }
}
