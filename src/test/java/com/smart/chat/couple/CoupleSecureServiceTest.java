package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
 * 确定感·安全感（F120-F129）核心逻辑单测：安全感账户收下规则、十年之约凑齐、
 * 愿景同词共鸣、承诺双章展出、信任币日限额、双人契约计数、守护兽心情惰性衰减、
 * 恋爱年轮聚合、恋爱体检打分。
 */
@ExtendWith(MockitoExtension.class)
class CoupleSecureServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleSecurityBankMapper securityMapper;
    @Mock
    private CoupleDecadePactMapper decadeMapper;
    @Mock
    private CoupleVisionCardMapper visionMapper;
    @Mock
    private CoupleOathMapper oathMapper;
    @Mock
    private CoupleTrustCoinMapper trustMapper;
    @Mock
    private CoupleSelfContractMapper contractMapper;
    @Mock
    private CouplePetMapper petMapper;
    @Mock
    private CoupleCheckinMapper checkinMapper;
    @Mock
    private CoupleMoodMapper moodMapper;
    @Mock
    private CouplePromiseMapper promiseMapper;
    @Mock
    private CoupleActionMapper actionMapper;
    @Mock
    private CoupleLetterMapper letterMapper;
    @Mock
    private CoupleAnniversaryMapper anniversaryMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleSecureService secureService;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setCreated(System.currentTimeMillis() - 400L * 24 * 3600 * 1000);
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    // ========== F120 安全感账户 ==========

    @Test
    void acceptSecurityRejectsOwnDeposit() {
        stubSpace("alice");
        CoupleSecurityBank row = CoupleSecurityBank.of("s1", "alice", "有我在，别怕");
        when(securityMapper.selectById(row.getId())).thenReturn(row);
        assertThatThrownBy(() -> secureService.acceptSecurity("alice", row.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己存的");
    }

    @Test
    void acceptSecurityIncreasesBalanceAndPushes() {
        stubSpace("alice");
        CoupleSecurityBank row = CoupleSecurityBank.of("s1", "bob", "有我在，别怕");
        when(securityMapper.selectById(row.getId())).thenReturn(row);
        when(securityMapper.findBySpace("s1")).thenReturn(List.of(row));
        when(securityMapper.countAccepted("s1")).thenReturn(1L);

        CoupleSecureService.SecurityBoardVO vo = secureService.acceptSecurity("alice", row.getId());

        assertThat(vo.balance()).isEqualTo(1);
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ========== F122 十年之约 ==========

    @Test
    void decadeCompletesWhenBothWritten() {
        stubSpace("alice");
        CoupleDecadePact mine = CoupleDecadePact.of("s1", "bob", "十年后我们还在海边");
        CoupleDecadePact alicePact = CoupleDecadePact.of("s1", "alice", "十年后我们养一只猫");
        when(decadeMapper.findByUser("s1", "alice")).thenReturn(null);
        // 写入后空间里应该有两条（bob 的 + alice 刚写的）
        when(decadeMapper.findBySpace("s1")).thenReturn(List.of(mine, alicePact));

        secureService.saveDecadePact("alice", "十年后我们养一只猫");

        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), detail.capture());
        assertThat(detail.getValue()).contains("凑齐");
    }

    // ========== F123 愿景板 ==========

    @Test
    void visionResonatesOnSameWord() {
        stubSpace("alice");
        CoupleVisionCard partnerCard = CoupleVisionCard.of("s1", "bob", "海边小屋", null);
        when(visionMapper.findBySpace("s1")).thenReturn(List.of(partnerCard));

        secureService.addVision("alice", "海边小屋", "带院子");

        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), detail.capture());
        assertThat(detail.getValue()).contains("共鸣").contains("海边小屋");
    }

    // ========== F124 承诺博物馆 ==========

    @Test
    void doubleStampExhibitsOath() {
        stubSpace("alice");
        CoupleOath oath = CoupleOath.of("s1", "bob", "吵架不过夜");
        // bob 已盖章（userB），alice（userA）盖下第二个章即展出
        oath.setStampB(1);
        when(oathMapper.selectById(oath.getId())).thenReturn(oath);
        when(oathMapper.findBySpace("s1")).thenReturn(List.of(oath));

        secureService.stampOath("alice", oath.getId());

        assertThat(oath.fullyStamped()).isTrue();
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ========== F125 信任存折 ==========

    @Test
    void trustCoinDailyLimitEnforced() {
        stubSpace("alice");
        when(trustMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> secureService.depositTrust("alice", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("明天");
    }

    @Test
    void trustCoinDepositsToPartner() {
        stubSpace("alice");
        when(trustMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(trustMapper.findBySpace("s1")).thenReturn(List.of());
        when(trustMapper.countByTo("s1", "alice")).thenReturn(3L);
        when(trustMapper.countByTo("s1", "bob")).thenReturn(5L);

        CoupleSecureService.TrustBoardVO vo = secureService.depositTrust("alice", "说到做到");

        assertThat(vo.mineBalance()).isEqualTo(3);
        assertThat(vo.partnerBalance()).isEqualTo(5);
    }

    // ========== F126 恋爱年轮 ==========

    @Test
    void ringsAggregateByYear() {
        stubSpace("alice");
        CoupleAnniversary ann = CoupleAnniversary.of("s1", "领证", LocalDate.now().minusYears(1).toString(), true, "alice");
        when(anniversaryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(ann));

        CoupleSecureService.RingBoardVO vo = secureService.rings("alice");

        assertThat(vo.years()).isEqualTo(2);
        assertThat(vo.rings()).hasSize(2);
        assertThat(vo.rings().get(0).events()).isEqualTo(1);
        assertThat(vo.rings().get(1).events()).isZero();
    }

    // ========== F121 恋爱体检 ==========

    @SuppressWarnings("unchecked")
    @Test
    void checkupScoresFiveItems() {
        stubSpace("alice");
        when(checkinMapper.selectCount(any(Wrapper.class))).thenReturn(30L);
        when(moodMapper.selectCount(any(Wrapper.class))).thenReturn(20L);
        when(promiseMapper.selectCount(any(Wrapper.class))).thenReturn(10L);
        when(actionMapper.selectCount(any(Wrapper.class))).thenReturn(30L);
        when(letterMapper.selectCount(any(Wrapper.class))).thenReturn(10L);

        CoupleSecureService.CheckupVO vo = secureService.checkup("alice");

        assertThat(vo.items()).hasSize(5);
        assertThat(vo.total()).isEqualTo(100);
        assertThat(vo.level()).contains("稳固");
    }

    // ========== F128 双人契约 ==========

    @Test
    void contractCheckinIncrementsMyCountOnly() {
        stubSpace("alice");
        CoupleSelfContract contract = CoupleSelfContract.of("s1", "每天说晚安", null);
        contract.setCountB(2);
        when(contractMapper.selectById(contract.getId())).thenReturn(contract);
        when(contractMapper.findBySpace("s1")).thenReturn(List.of(contract));

        List<CoupleSecureService.ContractVO> list = secureService.checkContract("alice", contract.getId());

        assertThat(contract.getCountA()).isEqualTo(1);
        assertThat(contract.getCountB()).isEqualTo(2);
        assertThat(list.get(0).myCount()).isEqualTo(1);
        assertThat(list.get(0).partnerCount()).isEqualTo(2);
    }

    // ========== F129 守护兽 ==========

    @Test
    void adoptTwiceRejected() {
        stubSpace("alice");
        when(petMapper.findBySpace("s1")).thenReturn(CouplePet.of("s1", "团子", CouplePet.KIND_CAT));
        assertThatThrownBy(() -> secureService.adoptPet("alice", "毛球", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("吃醋");
    }

    @Test
    void petMoodDecaysByDaysSinceCare() {
        stubSpace("alice");
        CouplePet pet = CouplePet.of("s1", "团子", CouplePet.KIND_CAT);
        pet.setLastCareAt(System.currentTimeMillis() - 4L * 24 * 3600 * 1000);
        pet.setCareCount(9);
        when(petMapper.findBySpace("s1")).thenReturn(pet);

        CoupleSecureService.PetVO vo = secureService.pet("alice");

        assertThat(vo.mood()).isEqualTo("MISS");
        assertThat(vo.careCount()).isEqualTo(9);
    }

    @Test
    void carePetRefreshesMood() {
        stubSpace("alice");
        CouplePet pet = CouplePet.of("s1", "团子", CouplePet.KIND_CAT);
        pet.setLastCareAt(System.currentTimeMillis() - 9L * 24 * 3600 * 1000);
        pet.setCareCount(1);
        when(petMapper.findBySpace("s1")).thenReturn(pet);

        CoupleSecureService.PetVO vo = secureService.carePet("alice");

        assertThat(pet.getCareCount()).isEqualTo(2);
        assertThat(vo.mood()).isEqualTo("HAPPY");
    }

    @Test
    void neverAdoptedPetReturnsNull() {
        stubSpace("alice");
        when(petMapper.findBySpace("s1")).thenReturn(null);
        assertThat(secureService.pet("alice")).isNull();
        verify(push, never()).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void invalidKindFallsBackToFox() {
        stubSpace("alice");
        when(petMapper.findBySpace("s1")).thenReturn(null);
        lenient().doNothing().when(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());

        CoupleSecureService.PetVO vo = secureService.adoptPet("alice", "团子", "DRAGON");

        assertThat(vo.kind()).isEqualTo(CouplePet.KIND_FOX);
    }
}
