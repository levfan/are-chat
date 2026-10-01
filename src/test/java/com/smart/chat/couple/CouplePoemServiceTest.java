package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文字浪漫系（F160-F169）核心逻辑单测：接龙限一句、三行情书点赞权限、醒来第一条次日送达与已读、
 * 漂流瓶回信流转、密码情书解码权限、灵魂一问双答互见、手账贴纸白名单、语录拼装。
 */
@ExtendWith(MockitoExtension.class)
class CouplePoemServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CouplePoemChainMapper chainMapper;
    @Mock
    private CouplePoem3LineMapper poem3Mapper;
    @Mock
    private CoupleMorningNoteMapper morningMapper;
    @Mock
    private CoupleDriftBottleMapper bottleMapper;
    @Mock
    private CoupleCipherNoteMapper cipherMapper;
    @Mock
    private CoupleSoulAnswerMapper soulMapper;
    @Mock
    private CoupleJournalMapper journalMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CouplePoemService poemService;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setCreated(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000);
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    // ========== F160 情诗接龙 ==========

    @Test
    void addLineLimitedToOnePerDay() {
        stubSpace("alice");
        when(chainMapper.find("s1", LocalDate.now().toString(), "alice"))
                .thenReturn(CouplePoemChain.of("s1", LocalDate.now().toString(), "alice", "你是我窗前的月光"));

        assertThatThrownBy(() -> poemService.addLine("alice", "第二句"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经写啦");
    }

    // ========== F161 三行情书 ==========

    @Test
    void likePoem3RequiresPartnerAndOnce() {
        CouplePoem3Line poem = CouplePoem3Line.of("s1", "alice", "你笑", "我也笑", "我们一起笑");

        stubSpace("alice");
        when(poem3Mapper.selectById(poem.getId())).thenReturn(poem);
        assertThatThrownBy(() -> poemService.likePoem3("alice", poem.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("等 TA 来点赞");

        stubSpace("bob");
        when(poem3Mapper.selectById(poem.getId())).thenReturn(poem);
        when(poem3Mapper.findBySpace("s1")).thenReturn(List.of());
        poemService.likePoem3("bob", poem.getId());
        assertThat(poem.getLikedBy()).isEqualTo("bob");
        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEvent(event.capture(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("poem-liked");

        when(poem3Mapper.selectById(poem.getId())).thenReturn(poem);
        assertThatThrownBy(() -> poemService.likePoem3("bob", poem.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("点过赞");
    }

    // ========== F162 醒来第一条 ==========

    @Test
    void sealMorningNoteDeliversTomorrow() {
        stubSpace("alice");
        when(morningMapper.findByUser("s1", "alice")).thenReturn(List.of());
        when(morningMapper.findDelivered("s1", "alice", LocalDate.now().toString())).thenReturn(List.of());

        poemService.sealMorningNote("alice", "明早要开心呀");

        ArgumentCaptor<CoupleMorningNote> saved = ArgumentCaptor.forClass(CoupleMorningNote.class);
        verify(morningMapper).insert(saved.capture());
        assertThat(saved.getValue().getDeliverDay()).isEqualTo(LocalDate.now().plusDays(1).toString());
    }

    @Test
    void readMorningNoteOnlyByRecipient() {
        CoupleMorningNote note = CoupleMorningNote.of("s1", "alice", "早安", LocalDate.now().toString());

        stubSpace("alice");
        when(morningMapper.selectById(note.getId())).thenReturn(note);
        assertThatThrownBy(() -> poemService.readMorningNote("alice", note.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己写的");

        stubSpace("bob");
        when(morningMapper.selectById(note.getId())).thenReturn(note);
        when(morningMapper.findByUser("s1", "bob")).thenReturn(List.of());
        when(morningMapper.findDelivered("s1", "bob", LocalDate.now().toString())).thenReturn(List.of());
        poemService.readMorningNote("bob", note.getId());
        assertThat(note.getReadAt()).isNotNull();
        verify(push).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
    }

    // ========== F163 心情漂流瓶 ==========

    @Test
    void replyBottleOnlyByPartnerAndOnce() {
        CoupleDriftBottle bottle = CoupleDriftBottle.of("s1", "alice", "委屈", "今天被领导说了");

        stubSpace("alice");
        when(bottleMapper.selectById(bottle.getId())).thenReturn(bottle);
        assertThatThrownBy(() -> poemService.replyBottle("alice", bottle.getId(), "抱抱"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("等 TA 来捡");

        stubSpace("bob");
        when(bottleMapper.selectById(bottle.getId())).thenReturn(bottle);
        when(bottleMapper.findBySpace("s1")).thenReturn(List.of());
        poemService.replyBottle("bob", bottle.getId(), "辛苦啦，晚上煮你爱吃的");
        assertThat(bottle.getStatus()).isEqualTo(CoupleDriftBottle.STATUS_REPLIED);
        assertThat(bottle.getReply()).contains("煮你爱吃的");

        when(bottleMapper.selectById(bottle.getId())).thenReturn(bottle);
        assertThatThrownBy(() -> poemService.replyBottle("bob", bottle.getId(), "再回一次"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("回过信");
    }

    // ========== F164 数字密码情书 ==========

    @Test
    void crackCipherNoteOnlyByPartnerOnce() {
        CoupleCipherNote note = CoupleCipherNote.of("s1", "alice", "20 8 1 14 11 0 25 15 21", "拼音首字母试试");

        stubSpace("bob");
        when(cipherMapper.selectById(note.getId())).thenReturn(note);
        when(cipherMapper.findBySpace("s1")).thenReturn(List.of());
        poemService.crackCipherNote("bob", note.getId());
        assertThat(note.getDecodedBy()).isEqualTo("bob");
        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEvent(event.capture(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("cipher-note-cracked");

        when(cipherMapper.selectById(note.getId())).thenReturn(note);
        assertThatThrownBy(() -> poemService.crackCipherNote("bob", note.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("解开过");
    }

    // ========== F165 灵魂提问盲盒 ==========

    @Test
    void soulAnswerVisibleOnlyWhenBothAnswered() {
        String today = LocalDate.now().toString();
        stubSpace("alice");
        when(soulMapper.find("s1", today, "alice")).thenReturn(null);
        when(soulMapper.find("s1", today, "bob")).thenReturn(CoupleSoulAnswer.of("s1", today, "bob", "答案"));

        // 我没答：即使 TA 答了也看不到
        CouplePoemService.SoulVO before = poemService.soul("alice");
        assertThat(before.partner()).isNull();
        assertThat(before.question()).isNotBlank();

        // 我答了：双答互见 + both 推送
        when(soulMapper.find("s1", today, "alice")).thenReturn(CoupleSoulAnswer.of("s1", today, "alice", "我的答案"));
        when(soulMapper.find("s1", today, "bob")).thenReturn(CoupleSoulAnswer.of("s1", today, "bob", "答案"));
        CouplePoemService.SoulVO after = poemService.answerSoul("alice", "我的答案");
        assertThat(after.partner()).isNotNull();
        assertThat(after.bothAnswered()).isTrue();
        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEventBoth(event.capture(), anyString(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("soul-both");
    }

    // ========== F166 贴纸手账 ==========

    @Test
    void saveJournalValidatesStickerWhitelist() {
        stubSpace("alice");
        assertThatThrownBy(() -> poemService.saveJournal("alice", "🚀", "今天"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("贴纸库");

        when(journalMapper.find("s1", LocalDate.now().toString(), "alice")).thenReturn(null);
        poemService.saveJournal("alice", "🌈", "今天天气和心情一样好");
        verify(journalMapper).insert(any(CoupleJournal.class));
    }

    // ========== F167 恋爱语录机 ==========

    @Test
    void quoteContainsDaysAndPartner() {
        stubSpace("alice");
        String quote = poemService.quote("alice");
        assertThat(quote).contains("11"); // 10 天 + 1
        assertThat(quote).doesNotContain("{days}").doesNotContain("{partner}");
    }

    // ========== F168/F169 静态库 ==========

    @Test
    void letterTemplatesAndStickersPresent() {
        assertThat(poemService.letterTemplates()).hasSize(8);
        assertThat(poemService.stickers()).hasSize(16);
    }

    // ========== 校验兜底 ==========

    @Test
    void addPoem3RequiresAllThreeLines() {
        stubSpace("alice");
        assertThatThrownBy(() -> poemService.addPoem3("alice", "第一行", "  ", "第三行"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("三行都要写");
        verify(poem3Mapper, never()).insert(any(CouplePoem3Line.class));
    }
}
