package com.smart.chat.platform.application;

import com.smart.chat.messaging.domain.AnnouncementBroadcaster;
import com.smart.chat.platform.domain.announcement.Announcement;
import com.smart.chat.platform.domain.announcement.AnnouncementRead;
import com.smart.chat.platform.domain.announcement.AnnouncementReadRepository;
import com.smart.chat.platform.domain.announcement.AnnouncementRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 公告用例的编排：Service 只依赖端口，所以这里 mock 的是 {@link AnnouncementRepository} 与
 * {@link AnnouncementReadRepository}（改造前是两个 Mapper），断言的对外口径一字不改——
 * VO 字段、异常 code 与文案、WS 推送时机都按原契约校验。
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcements;

    @Mock
    private AnnouncementReadRepository reads;

    @Mock
    private AnnouncementBroadcaster broadcaster;

    @Test
    void publishClosesTheActiveOneInsertsTheNewOneAndBroadcasts() {
        AnnouncementService service = service();
        Announcement active = Announcement.restore("old-1", "旧公告", "admin", true, 1L);
        Announcement closedBefore = Announcement.restore("old-2", "更早的公告", "admin", false, 2L);
        when(announcements.listRecent()).thenReturn(List.of(active, closedBefore));

        AnnouncementService.AnnouncementVO vo = service.publish("admin", "  今晚八点维护  ");

        assertThat(vo.content()).isEqualTo("今晚八点维护");
        assertThat(vo.createdBy()).isEqualTo("admin");
        assertThat(vo.read()).isFalse();
        assertThat(active.enabled()).isFalse();
        // 已经关过的旧公告不该被再写一次（改造前的 if (enabled) 口径）
        verify(announcements, never()).save(closedBefore);
        ArgumentCaptor<Announcement> saved = ArgumentCaptor.forClass(Announcement.class);
        verify(announcements, times(2)).save(saved.capture());
        List<Announcement> written = saved.getAllValues();
        assertThat(written.get(0)).isSameAs(active);
        Announcement fresh = written.get(1);
        assertThat(fresh.enabled()).isTrue();
        assertThat(fresh.content()).isEqualTo("今晚八点维护");
        // 推送时机：先落库再推，且推的是新公告的 id 与清洗后的正文
        verify(broadcaster).publishAnnouncement(fresh.id(), "今晚八点维护");
    }

    @Test
    void publishRejectsBlankContent() {
        AnnouncementService service = service();

        assertThatThrownBy(() -> service.publish("admin", "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("公告内容不能为空");
        verifyNoInteractions(broadcaster, reads);
        verify(announcements, never()).save(any());
    }

    @Test
    void publishRejectsOverLongContent() {
        AnnouncementService service = service();

        assertThatThrownBy(() -> service.publish("admin", "啊".repeat(501)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("公告最长 500 个字");
    }

    @Test
    void closeDisablesTheAnnouncementThroughThePort() {
        AnnouncementService service = service();
        Announcement active = Announcement.restore("a1", "公告", "admin", true, 1L);
        when(announcements.findById("a1")).thenReturn(Optional.of(active));

        service.close("admin", "a1");

        ArgumentCaptor<Announcement> saved = ArgumentCaptor.forClass(Announcement.class);
        verify(announcements).save(saved.capture());
        assertThat(saved.getValue().enabled()).isFalse();
        verifyNoInteractions(broadcaster);
    }

    @Test
    void closeUnknownAnnouncementIs404() {
        AnnouncementService service = service();
        when(announcements.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.close("admin", "nope"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("公告不存在")
                .extracting("code").isEqualTo(404);
        verify(announcements, never()).save(any());
    }

    @Test
    void closingAnAlreadyClosedAnnouncementStillSucceeds() {
        AnnouncementService service = service();
        when(announcements.findById("a2")).thenReturn(Optional.of(
                Announcement.restore("a2", "公告", "admin", false, 1L)));

        service.close("admin", "a2");

        verify(announcements).save(any(Announcement.class));
    }

    @Test
    void currentReturnsActiveAnnouncementWithMyReadFlag() {
        AnnouncementService service = service();
        when(announcements.findActive()).thenReturn(Optional.of(
                Announcement.restore("a3", "生效中", "admin", true, 7L)));
        when(reads.hasRead("alice", "a3")).thenReturn(true);

        assertThat(service.current("alice"))
                .isEqualTo(new AnnouncementService.AnnouncementVO("a3", "生效中", "admin", 7L, true));
    }

    @Test
    void currentIsNullWhenNothingIsActive() {
        AnnouncementService service = service();
        when(announcements.findActive()).thenReturn(Optional.empty());

        assertThat(service.current("alice")).isNull();
        verifyNoInteractions(reads);
    }

    @Test
    void markReadAppendsOnlyOncePerUserAndAnnouncement() {
        AnnouncementService service = service();
        when(announcements.findById("a4")).thenReturn(Optional.of(
                Announcement.restore("a4", "公告", "admin", true, 1L)));
        when(reads.hasRead("alice", "a4")).thenReturn(false);

        service.markRead("alice", "a4");

        ArgumentCaptor<AnnouncementRead> saved = ArgumentCaptor.forClass(AnnouncementRead.class);
        verify(reads).save(saved.capture());
        assertThat(saved.getValue().username()).isEqualTo("alice");
        assertThat(saved.getValue().announcementId()).isEqualTo("a4");
    }

    @Test
    void markReadIsSilentWhenAlreadyRead() {
        AnnouncementService service = service();
        when(announcements.findById("a5")).thenReturn(Optional.of(
                Announcement.restore("a5", "公告", "admin", true, 1L)));
        when(reads.hasRead("bob", "a5")).thenReturn(true);

        service.markRead("bob", "a5");

        verify(reads, never()).save(any());
    }

    @Test
    void markReadOnUnknownAnnouncementIs404() {
        AnnouncementService service = service();
        when(announcements.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead("alice", "ghost"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("公告不存在");
        verifyNoInteractions(reads);
    }

    @Test
    void allProjectsEveryRowIntoTheAdminVO() {
        AnnouncementService service = service();
        when(announcements.listRecent()).thenReturn(List.of(
                Announcement.restore("b2", "新的", "admin", true, 2L),
                Announcement.restore("b1", "旧的", "admin", false, 1L)));

        List<AnnouncementService.AnnouncementAdminVO> vos = service.all();

        assertThat(vos).containsExactly(
                new AnnouncementService.AnnouncementAdminVO("b2", "新的", "admin", true, 2L),
                new AnnouncementService.AnnouncementAdminVO("b1", "旧的", "admin", false, 1L));
    }

    private AnnouncementService service() {
        return new AnnouncementService(announcements, reads, broadcaster);
    }
}
