package com.smart.chat.platform.infrastructure.persistence;

import com.smart.chat.platform.domain.announcement.Announcement;
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
 * 公告仓储适配器的两个口径：读法把存储映射回聚合（SQL 口径由 Mapper 的 default 方法守住），
 * 写回<b>只碰聚合纳管的那一列 enabled</b>——正文、发布人、发布时间发布即冻结，
 * 用聚合重建整行等于把「聚合没建模的东西」静默清空，这类 bug 编译与业务测试都照不出来。
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementRepositoryAdapterTest {

    @Mock
    private AnnouncementMapper mapper;

    @InjectMocks
    private AnnouncementRepositoryAdapter repository;

    @Test
    void findActiveMapsStorageBackIntoTheAggregate() {
        AnnouncementPO po = row("a1", "今晚八点维护", "admin", true, 100L);
        when(mapper.findLatestEnabled()).thenReturn(Optional.of(po));

        Announcement announcement = repository.findActive().orElseThrow();

        assertThat(announcement.id()).isEqualTo("a1");
        assertThat(announcement.content()).isEqualTo("今晚八点维护");
        assertThat(announcement.createdBy()).isEqualTo("admin");
        assertThat(announcement.enabled()).isTrue();
        assertThat(announcement.created()).isEqualTo(100L);
    }

    @Test
    void listRecentKeepsOrderAndNullEnabledMeansNotActive() {
        when(mapper.findAllOrdered()).thenReturn(List.of(
                row("a2", "新的", "admin", true, 2L),
                row("a1", "旧的", "admin", null, 1L)));

        List<Announcement> list = repository.listRecent();

        assertThat(list).extracting(Announcement::id).containsExactly("a2", "a1");
        assertThat(list.get(1).enabled()).as("enabled 为空按未生效看待").isFalse();
    }

    @Test
    void findByIdReturnsEmptyWhenRowIsGone() {
        when(mapper.selectById("nope")).thenReturn(null);

        assertThat(repository.findById("nope")).isEmpty();
    }

    @Test
    void newAnnouncementIsInsertedWithAllColumns() {
        Announcement fresh = Announcement.restore("a3", "第一条", "admin", true, 300L);
        when(mapper.selectById("a3")).thenReturn(null);

        repository.save(fresh);

        ArgumentCaptor<AnnouncementPO> captor = ArgumentCaptor.forClass(AnnouncementPO.class);
        verify(mapper).insert(captor.capture());
        AnnouncementPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo("a3");
        assertThat(po.getContent()).isEqualTo("第一条");
        assertThat(po.getCreatedBy()).isEqualTo("admin");
        assertThat(po.getEnabled()).isTrue();
        assertThat(po.getCreated()).isEqualTo(300L);
        verify(mapper, never()).updateById(any(AnnouncementPO.class));
    }

    @Test
    void updateOnlyRewritesTheColumnTheAggregateOwns() {
        AnnouncementPO existing = row("a4", "库里的正文", "someone", true, 400L);
        when(mapper.selectById("a4")).thenReturn(existing);

        // 聚合携带的值故意与库里不同：冻结的列必须原样留着
        Announcement announcement = Announcement.restore("a4", "聚合里的正文", "other", false, 999L);
        repository.save(announcement);

        ArgumentCaptor<AnnouncementPO> captor = ArgumentCaptor.forClass(AnnouncementPO.class);
        verify(mapper).updateById(captor.capture());
        AnnouncementPO written = captor.getValue();
        assertThat(written.getEnabled()).as("聚合纳管的列才回写").isFalse();
        assertThat(written.getContent()).as("发布即冻结的列不能被动").isEqualTo("库里的正文");
        assertThat(written.getCreatedBy()).isEqualTo("someone");
        assertThat(written.getCreated()).isEqualTo(400L);
        verify(mapper, never()).insert(any(AnnouncementPO.class));
    }

    private static AnnouncementPO row(String id, String content, String createdBy, Boolean enabled, Long created) {
        AnnouncementPO po = new AnnouncementPO();
        po.setId(id);
        po.setContent(content);
        po.setCreatedBy(createdBy);
        po.setEnabled(enabled);
        po.setCreated(created);
        return po;
    }
}
