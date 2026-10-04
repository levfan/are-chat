package com.smart.chat.platform.infrastructure.persistence;

import com.smart.chat.platform.domain.announcement.AnnouncementRead;
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

/** 已读记录适配器：判重沿用 Mapper 的 exists（uq_ann_read 口径），追加只走 insert。 */
@ExtendWith(MockitoExtension.class)
class AnnouncementReadRepositoryAdapterTest {

    @Mock
    private AnnouncementReadMapper mapper;

    @InjectMocks
    private AnnouncementReadRepositoryAdapter repository;

    @Test
    void hasReadDelegatesToTheUniquePairQuery() {
        when(mapper.exists("alice", "ann-1")).thenReturn(true);

        assertThat(repository.hasRead("alice", "ann-1")).isTrue();
    }

    @Test
    void newReadIsInsertedWithAllColumns() {
        AnnouncementRead read = AnnouncementRead.restore("r1", "alice", "ann-1", 500L);
        when(mapper.selectById("r1")).thenReturn(null);

        repository.save(read);

        ArgumentCaptor<AnnouncementReadPO> captor = ArgumentCaptor.forClass(AnnouncementReadPO.class);
        verify(mapper).insert(captor.capture());
        AnnouncementReadPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo("r1");
        assertThat(po.getUsername()).isEqualTo("alice");
        assertThat(po.getAnnouncementId()).isEqualTo("ann-1");
        assertThat(po.getReadAt()).isEqualTo(500L);
        verify(mapper, never()).updateById(any(AnnouncementReadPO.class));
    }

    @Test
    void updateOnlyRewritesReadAt() {
        AnnouncementReadPO existing = new AnnouncementReadPO();
        existing.setId("r2");
        existing.setUsername("库里的用户");
        existing.setAnnouncementId("库里的公告");
        existing.setReadAt(1L);
        when(mapper.selectById("r2")).thenReturn(existing);

        repository.save(AnnouncementRead.restore("r2", "聚合的用户", "聚合的公告", 2L));

        ArgumentCaptor<AnnouncementReadPO> captor = ArgumentCaptor.forClass(AnnouncementReadPO.class);
        verify(mapper).updateById(captor.capture());
        AnnouncementReadPO written = captor.getValue();
        assertThat(written.getReadAt()).isEqualTo(2L);
        assertThat(written.getUsername()).as("归属两列由登记时冻结").isEqualTo("库里的用户");
        assertThat(written.getAnnouncementId()).isEqualTo("库里的公告");
        verify(mapper, never()).insert(any(AnnouncementReadPO.class));
    }
}
