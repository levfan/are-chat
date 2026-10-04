package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.pin.UserPin;
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
 * 收藏卡仓储适配器：验证「只回写聚合纳管的列」这条纪律。
 * 归属人、空间、创建时刻都不归聚合改，用聚合重建整行会把它们静默改写——
 * 这类偏差编译和业务用例都照不出来。
 */
@ExtendWith(MockitoExtension.class)
class UserPinRepositoryAdapterTest {

    @Mock
    private CoupleUserPinMapper pinMapper;

    @InjectMocks
    private UserPinRepositoryAdapter adapter;

    @Test
    void newBoardIsInsertedWithAllColumns() {
        when(pinMapper.selectById(any())).thenReturn(null);
        UserPin pin = UserPin.blank("s1", "alice");
        pin.replaceWith(List.of("dining", "cozy"));

        adapter.save(pin);

        ArgumentCaptor<CoupleUserPinPO> captor = ArgumentCaptor.forClass(CoupleUserPinPO.class);
        verify(pinMapper).insert(captor.capture());
        CoupleUserPinPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(pin.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getFromUser()).isEqualTo("alice");
        assertThat(po.getPins()).isEqualTo("dining,cozy");
        assertThat(po.getCreated()).isNotNull();
        assertThat(po.getUpdatedAt()).isEqualTo(po.getCreated());
        verify(pinMapper, never()).updateById(any(CoupleUserPinPO.class));
    }

    @Test
    void updateOnlyWritesPinsAndUpdatedAt() {
        CoupleUserPinPO stored = new CoupleUserPinPO();
        stored.setId("p1");
        stored.setSpaceId("s1");
        stored.setFromUser("alice");
        stored.setPins("dining");
        stored.setCreated(111L);
        stored.setUpdatedAt(111L);
        when(pinMapper.selectById("p1")).thenReturn(stored);
        UserPin pin = UserPin.restore("p1", "s1", "alice", "dining", 111L, 111L);
        pin.replaceWith(List.of("dining", "cozy"));

        adapter.save(pin);

        ArgumentCaptor<CoupleUserPinPO> captor = ArgumentCaptor.forClass(CoupleUserPinPO.class);
        verify(pinMapper).updateById(captor.capture());
        CoupleUserPinPO written = captor.getValue();
        assertThat(written.getPins()).as("卡集是聚合唯一真的改动的业务列").isEqualTo("dining,cozy");
        assertThat(written.getUpdatedAt()).isNotEqualTo(111L);
        assertThat(written.getFromUser()).isEqualTo("alice");
        assertThat(written.getSpaceId()).isEqualTo("s1");
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(pinMapper, never()).insert(any(CoupleUserPinPO.class));
    }

    @Test
    void findMapsRowBackIntoUserPin() {
        CoupleUserPinPO row = new CoupleUserPinPO();
        row.setId("p2");
        row.setSpaceId("s1");
        row.setFromUser("bob");
        row.setPins("meeting, host");
        row.setCreated(9L);
        when(pinMapper.find("s1", "bob")).thenReturn(row);
        when(pinMapper.find("s1", "ghost")).thenReturn(null);

        Optional<UserPin> found = adapter.findBySpaceAndUser("s1", "bob");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().keys()).containsExactly("meeting", "host");
        assertThat(found.orElseThrow().updatedAt()).isNull();
        assertThat(adapter.findBySpaceAndUser("s1", "ghost")).isEmpty();
    }
}
