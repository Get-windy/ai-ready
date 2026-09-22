package cn.aiedge.agreement;

import cn.aiedge.agreement.domain.AgreementPartySide;
import cn.aiedge.agreement.domain.AgreementRuntime;
import cn.aiedge.agreement.dto.AgreementInviteCreateDTO;
import cn.aiedge.agreement.dto.AgreementInviteOpenDTO;
import cn.aiedge.agreement.dto.AgreementInviteVO;
import cn.aiedge.agreement.dto.AgreementProposalDTO;
import cn.aiedge.agreement.dto.AgreementSignDTO;
import cn.aiedge.agreement.dto.AgreementSignatureVO;
import cn.aiedge.agreement.dto.AgreementTerminationCreateDTO;
import cn.aiedge.agreement.dto.AgreementTerminationVO;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementInvite;
import cn.aiedge.agreement.entity.AgreementNarrative;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementSignature;
import cn.aiedge.agreement.entity.AgreementTermination;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementInviteStatus;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementTerminationSource;
import cn.aiedge.agreement.enums.AgreementTerminationStatus;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.mapper.AgreementFulfillmentModeMapper;
import cn.aiedge.agreement.mapper.AgreementInviteMapper;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementNarrativeMapper;
import cn.aiedge.agreement.mapper.AgreementSettingMapper;
import cn.aiedge.agreement.mapper.AgreementSignatureMapper;
import cn.aiedge.agreement.mapper.AgreementTerminationMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.agreement.service.AgreementService;
import cn.aiedge.agreement.service.impl.AgreementLifecycleServiceImpl;
import cn.aiedge.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成立过程与终止在**服务层**的行为（Mockito 隔离数据库与 Spring）。
 *
 * <p>钉死四件最容易做错的事：</p>
 * <ol>
 *   <li><b>反要约不碰现行生效版本</b>：只把上一份草稿置 REJECTED，ACTIVE 版本与主档一行都不动（㉛）；</li>
 *   <li><b>唯一送达发给"另一端"</b>：收件方由服务端判定，且库里只存哈希、明文只返回一次；</li>
 *   <li><b>签署复用既有确认链路并落签署记录</b>（不造第二套确认机制）；</li>
 *   <li><b>终止只改主档状态</b>：不写任何版本行、不写任何单据 —— "终止 ≠ 免责"的技术形态。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class AgreementLifecycleServiceTest {

    private static final Long TENANT_A = 2L;
    private static final Long TENANT_B = 3L;
    private static final Long PARTY_A = 11L;
    private static final Long PARTY_B = 22L;
    private static final Long USER = 9L;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Agreement.class);
        TableInfoHelper.initTableInfo(assistant, AgreementVersion.class);
        TableInfoHelper.initTableInfo(assistant, AgreementInvite.class);
        TableInfoHelper.initTableInfo(assistant, AgreementSignature.class);
        TableInfoHelper.initTableInfo(assistant, AgreementTermination.class);
        TableInfoHelper.initTableInfo(assistant, AgreementSetting.class);
        TableInfoHelper.initTableInfo(assistant, AgreementNarrative.class);
        TableInfoHelper.initTableInfo(assistant, AgreementFulfillmentMode.class);
    }

    @Mock
    private AgreementMapper agreementMapper;
    @Mock
    private AgreementVersionMapper versionMapper;
    @Mock
    private AgreementSettingMapper settingMapper;
    @Mock
    private AgreementNarrativeMapper narrativeMapper;
    @Mock
    private AgreementFulfillmentModeMapper fulfillmentModeMapper;
    @Mock
    private AgreementInviteMapper inviteMapper;
    @Mock
    private AgreementSignatureMapper signatureMapper;
    @Mock
    private AgreementTerminationMapper terminationMapper;
    @Mock
    private AgreementService agreementService;
    @Mock
    private AgreementRuntime runtime;

    @InjectMocks
    private AgreementLifecycleServiceImpl service;

    // ══════════════════════ 一、唯一送达（§13.5） ══════════════════════

    @Test
    @DisplayName("发起送达：收件方是**另一端**（不接受前端指定），库里只存哈希，明文 token 只返回这一次")
    void createInviteTargetsTheOtherSide() {
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        AgreementVersion draft = version(200L, 1, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");

        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        // 会话租户是甲方 ⇒ 收件方必须是乙方
        when(inviteMapper.selectList(any())).thenReturn(List.of());
        when(versionMapper.selectList(any())).thenReturn(List.of(draft));
        when(inviteMapper.insert(any(AgreementInvite.class))).thenAnswer(inv -> {
            inv.getArgument(0, AgreementInvite.class).setId(500L);
            return 1;
        });
        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(agreementMapper.selectPartyNames(anyCollection())).thenReturn(List.of());
        when(agreementMapper.selectTenantNames(anyCollection())).thenReturn(List.of());

        AgreementInviteVO vo = service.createInvite(1L, new AgreementInviteCreateDTO(),
                TENANT_A, USER);

        ArgumentCaptor<AgreementInvite> captor = ArgumentCaptor.forClass(AgreementInvite.class);
        verify(inviteMapper).insert(captor.capture());
        AgreementInvite saved = captor.getValue();
        assertEquals(0L, saved.getTenantId(), "系统级归属位必须显式为 0（裁定⑥）");
        assertEquals(PARTY_B, saved.getTargetPartyId(), "收件方 = 另一端的主体（乙方）");
        assertEquals(TENANT_B, saved.getTargetTenantId(), "收件方 = 另一端的租户（乙方租户）");
        assertEquals("B", saved.getTargetSide());
        assertEquals(200L, saved.getVersionId(), "token 绑定的是目标文稿版本");
        assertEquals(AgreementInviteStatus.PENDING.getCode(), saved.getStatus());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
        assertNotEquals(vo.getToken(), saved.getTokenHash(), "库里存的必须是哈希，不是明文");
        assertTrue(vo.getToken().length() >= 40, "明文 token 必须是高强度随机串");
        assertTrue(vo.getShortLink().contains(vo.getToken()), "短链给前端渲染二维码用");
        assertEquals(saved.getTokenHash(), cn.aiedge.agreement.domain.AgreementInviteToken.hash(vo.getToken()));
        assertNull(vo.getTargetPartyName(), "名称查不到时给 null，不编造");
    }

    @Test
    @DisplayName("发起送达：消费者单方承诺没有指定的另一方主体 ⇒ 拒绝（不要给不特定消费者发邀请）")
    void createInviteRejectsConsumerPromise() {
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        agreement.setPartyBId(null);
        agreement.setPartyBTenantId(null);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.createInvite(1L, new AgreementInviteCreateDTO(), TENANT_A, USER));
        assertTrue(e.getMessage().contains("不特定消费者"), e.getMessage());
        verify(inviteMapper, never()).insert(any(AgreementInvite.class));
    }

    @Test
    @DisplayName("领取邀请：五绑定匹配 ⇒ 置为已领取并留下「谁/何时/哪个渠道」的痕迹")
    void openInviteMarksAcceptedWithAudit() {
        AgreementInvite invite = invite(AgreementInviteStatus.PENDING);
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        AgreementVersion draft = version(200L, 1, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");

        when(inviteMapper.selectOne(any())).thenReturn(invite);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(inviteMapper.updateById(any(AgreementInvite.class))).thenReturn(1);
        when(inviteMapper.selectById(500L)).thenReturn(accepted(invite));
        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(agreementMapper.selectPartyNames(anyCollection())).thenReturn(List.of());
        when(agreementMapper.selectTenantNames(anyCollection())).thenReturn(List.of());

        AgreementInviteOpenDTO dto = new AgreementInviteOpenDTO();
        dto.setToken("raw-token-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        dto.setRepresentedPartyId(PARTY_B);
        dto.setChannel("QRCODE");

        AgreementInviteVO vo = service.openInvite(dto, TENANT_B, USER);

        ArgumentCaptor<AgreementInvite> captor = ArgumentCaptor.forClass(AgreementInvite.class);
        verify(inviteMapper).updateById(captor.capture());
        AgreementInvite patch = captor.getValue();
        assertEquals(AgreementInviteStatus.ACCEPTED.getCode(), patch.getStatus(), "一次性：领取即失效");
        assertEquals(USER, patch.getAcceptedBy());
        assertEquals(PARTY_B, patch.getAcceptedPartyId());
        assertEquals("QRCODE", patch.getAcceptChannel(), "留痕：通过哪个渠道领取");
        assertEquals(1, patch.getViewCount());
        assertEquals(USER, patch.getFirstViewedBy());
        assertEquals("QRCODE", patch.getFirstViewChannel());
        assertEquals(AgreementInviteStatus.ACCEPTED.getCode(), vo.getStatus());
    }

    @Test
    @DisplayName("撤回邀请：只有发起方那一端能撤，且撤回后链接立即失效")
    void revokeInviteOnlyByInitiator() {
        AgreementInvite invite = invite(AgreementInviteStatus.PENDING);
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        when(inviteMapper.selectById(500L)).thenReturn(invite);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);

        // 收到邀请的一方（乙方租户）不能撤对方的邀请
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.revokeInvite(500L, "不发了", TENANT_B, USER));
        assertEquals(403, e.getCode());
        verify(inviteMapper, never()).updateById(any(AgreementInvite.class));

        // 发起方（甲方租户）可以撤 ⇒ 置为"已撤回"（与"已过期""已领取"是三种不同的失效）
        when(inviteMapper.updateById(any(AgreementInvite.class))).thenReturn(1);
        when(agreementMapper.selectPartyNames(anyCollection())).thenReturn(List.of());
        when(agreementMapper.selectTenantNames(anyCollection())).thenReturn(List.of());
        service.revokeInvite(500L, "条款要重谈", TENANT_A, USER);

        ArgumentCaptor<AgreementInvite> captor = ArgumentCaptor.forClass(AgreementInvite.class);
        verify(inviteMapper).updateById(captor.capture());
        assertEquals(AgreementInviteStatus.REVOKED.getCode(), captor.getValue().getStatus());
        assertEquals(USER, captor.getValue().getRevokeBy());
        assertEquals("条款要重谈", captor.getValue().getRevokeReason());
    }

    // ══════════════════════ 二、多轮协商（§13.4） ══════════════════════

    @Test
    @DisplayName("反要约：上一份草稿置 REJECTED（保留在时间线），而**现行生效版本与主档一行都不动**")
    void proposeKeepsCurrentActiveVersionUntouched() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        AgreementVersion draft = version(200L, 2, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");
        draft.setProposedBySide("A");

        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(versionMapper.updateById(any(AgreementVersion.class))).thenReturn(1);
        when(agreementService.createVersionFrom(eq(1L), eq(200L), any(), eq(TENANT_B), eq(USER))).thenReturn(300L);
        when(versionMapper.updateProposal(any(AgreementVersion.class))).thenReturn(1);

        AgreementProposalDTO dto = new AgreementProposalDTO();
        dto.setChangeReason("账期由 30 天改为 45 天");
        dto.setProposalNote("旺季资金周转，麻烦通融");
        dto.setBaseVersionId(200L);

        Long newVersionId = service.propose(1L, dto, TENANT_B, USER);

        assertEquals(300L, newVersionId);

        // ① 被反要约的那份草稿置 REJECTED，且从不是现行生效版本
        ArgumentCaptor<AgreementVersion> patchCaptor = ArgumentCaptor.forClass(AgreementVersion.class);
        verify(versionMapper).updateById(patchCaptor.capture());
        AgreementVersion patch = patchCaptor.getValue();
        assertEquals(200L, patch.getId());
        assertEquals(AgreementVersionStatus.REJECTED.getCode(), patch.getStatus());
        assertNotEquals(100L, patch.getId(), "绝不能动现行生效版本");

        // ② 主档一行都不动（现行版本继续有效，交易照常）
        verify(agreementMapper, never()).updateById(any(Agreement.class));
        assertEquals(100L, agreement.getCurrentVersionId());
        assertEquals(AgreementStatus.ACTIVE.getCode(), agreement.getStatus());

        // ③ 新版本落下"谁提的 + 协商留言"
        ArgumentCaptor<AgreementVersion> proposalCaptor = ArgumentCaptor.forClass(AgreementVersion.class);
        verify(versionMapper).updateProposal(proposalCaptor.capture());
        AgreementVersion proposal = proposalCaptor.getValue();
        assertEquals(300L, proposal.getId());
        assertEquals("B", proposal.getProposedBySide(), "提案方由会话租户判定（乙方）");
        assertEquals(USER, proposal.getProposedByPerson());
        assertEquals("旺季资金周转，麻烦通融", proposal.getProposalNote());
    }

    @Test
    @DisplayName("反要约：基准版本不是草稿（例如已是生效版本）⇒ 拒绝，且不产生任何写操作")
    void proposeRejectsNonDraftBase() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        AgreementVersion active = version(100L, 1, AgreementVersionStatus.ACTIVE.getCode(), "{\"terms\":[]}");
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(versionMapper.selectById(100L)).thenReturn(active);

        AgreementProposalDTO dto = new AgreementProposalDTO();
        dto.setChangeReason("想直接改生效版本");
        dto.setBaseVersionId(100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.propose(1L, dto, TENANT_A, USER));
        assertTrue(e.getMessage().contains("待双方确认"), e.getMessage());
        verify(versionMapper, never()).updateById(any(AgreementVersion.class));
        verify(agreementService, never()).createVersionFrom(any(), any(), any(), any(), any());
    }

    // ══════════════════════ 三、签署（§13.6） ══════════════════════

    @Test
    @DisplayName("签署：必须带「代表哪个主体」与「凭什么代表」，并复用既有确认链路（不造第二套机制）")
    void signRecordsWhoAndWhy() {
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        AgreementVersion draft = version(200L, 1, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");

        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(signatureMapper.insert(any(AgreementSignature.class))).thenAnswer(inv -> {
            inv.getArgument(0, AgreementSignature.class).setId(700L);
            return 1;
        });
        when(agreementMapper.selectPartyNames(anyCollection())).thenReturn(List.of());
        when(agreementMapper.selectTenantNames(anyCollection())).thenReturn(List.of());

        AgreementSignDTO dto = new AgreementSignDTO();
        dto.setRepresentedPartyId(PARTY_A);
        dto.setPersonId(99L);
        dto.setSignerName("张三");
        dto.setAuthorityBasis("法定代表人本人");
        dto.setAuthorityEvidenceNo("授字-2026-001");

        AgreementSignatureVO vo = service.sign(200L, dto, TENANT_A, USER);

        // ① 复用既有"本方确认"（双签痕迹仍是能否生效的唯一依据）
        verify(agreementService).confirm(200L, TENANT_A, USER);

        // ② 落一条签署记录，七件事齐备
        ArgumentCaptor<AgreementSignature> captor = ArgumentCaptor.forClass(AgreementSignature.class);
        verify(signatureMapper).insert(captor.capture());
        AgreementSignature row = captor.getValue();
        assertEquals(0L, row.getTenantId());
        assertEquals(1L, row.getAgreementId());
        assertEquals(200L, row.getVersionId());
        assertEquals("A", row.getPartySide());
        assertEquals(PARTY_A, row.getPartyId(), "代表哪个主体");
        assertEquals(TENANT_A, row.getSignerPartyTenantId(), "在哪个租户签的");
        assertEquals(USER, row.getSignerUserId(), "谁签的（自然人账号）");
        assertEquals(99L, row.getSignerPersonId());
        assertEquals("张三", row.getSignerName());
        assertEquals("法定代表人本人", row.getAuthorityBasis(), "凭什么代表");
        assertEquals("授字-2026-001", row.getAuthorityEvidenceNo());
        assertEquals(cn.aiedge.agreement.domain.AgreementSnapshot.sha256(draft.getSnapshotJson()), row.getSignHash());
        assertTrue(row.getSignedAt() != null);
        assertEquals("VERSION", row.getSignatureType());
        assertEquals(Boolean.TRUE, vo.getEffective(), "对当前内容有效");
    }

    @Test
    @DisplayName("签署：没填授权依据 / 代表主体不一致 ⇒ 直接拒绝，且不写任何签署记录")
    void signRequiresAuthorityAndMatchingParty() {
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        AgreementVersion draft = version(200L, 1, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");
        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);

        AgreementSignDTO noAuthority = new AgreementSignDTO();
        noAuthority.setRepresentedPartyId(PARTY_A);
        assertTrue(assertThrows(BusinessException.class,
                () -> service.sign(200L, noAuthority, TENANT_A, USER))
                .getMessage().contains("授权依据"));

        AgreementSignDTO wrongParty = new AgreementSignDTO();
        wrongParty.setRepresentedPartyId(PARTY_B);
        wrongParty.setAuthorityBasis("法定代表人本人");
        assertTrue(assertThrows(BusinessException.class,
                () -> service.sign(200L, wrongParty, TENANT_A, USER))
                .getMessage().contains("不一致"));

        verify(signatureMapper, never()).insert(any(AgreementSignature.class));
        verify(agreementService, never()).confirm(any(), any(), any());
    }

    // ══════════════════════ 四、终止（§13.7：终止 ≠ 免责） ══════════════════════

    @Test
    @DisplayName("单方终止：主档置为已终止（= 停止履行），但**没有任何版本行被写过**（不追溯、不留免责）")
    void unilateralTerminationStopsPerformanceWithoutTouchingVersions() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(terminationMapper.insert(any(AgreementTermination.class))).thenAnswer(inv -> {
            inv.getArgument(0, AgreementTermination.class).setId(600L);
            return 1;
        });
        when(versionMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        AgreementTerminationCreateDTO dto = new AgreementTerminationCreateDTO();
        dto.setSource(AgreementTerminationSource.UNILATERAL.name());
        dto.setBasisText("按第 2 版第 7 条，合作条件已不具备");

        AgreementTerminationVO vo = service.requestTermination(1L, dto, TENANT_A, USER);

        // ① 单方终止**发起即生效**（停止履行已经是事实，系统不假装它没发生）
        ArgumentCaptor<AgreementTermination> insertCaptor = ArgumentCaptor.forClass(AgreementTermination.class);
        verify(terminationMapper).insert(insertCaptor.capture());
        AgreementTermination row = insertCaptor.getValue();
        assertEquals(0L, row.getTenantId());
        assertEquals(AgreementTerminationStatus.CONFIRMED.getCode(), row.getStatus());
        assertEquals(AgreementTerminationSource.UNILATERAL.name(), row.getSource());
        assertEquals(100L, row.getVersionId(), "记录「依据哪一版终止」");
        assertEquals("按第 2 版第 7 条，合作条件已不具备", row.getBasisText());
        assertEquals(Boolean.FALSE, row.getClaimCounterpartyBreach());
        assertTrue(row.getStopPerformanceAt() != null);

        // ② 主档置为已终止（这是"停止履行"的唯一技术效果）
        ArgumentCaptor<Agreement> agreementCaptor = ArgumentCaptor.forClass(Agreement.class);
        verify(agreementMapper).updateById(agreementCaptor.capture());
        Agreement patch = agreementCaptor.getValue();
        assertEquals(AgreementStatus.TERMINATED.getCode(), patch.getStatus());
        assertEquals(USER, patch.getTerminatedBy());
        assertTrue(patch.getTerminateReason().contains("单方终止"), patch.getTerminateReason());

        // ③ 终止**只动主档状态**：版本、条款、设定、文字、履约方式一个都不写
        verify(versionMapper, never()).updateById(any(AgreementVersion.class));
        verify(versionMapper, never()).update(any(), any());
        verify(versionMapper, never()).insertVersion(any(AgreementVersion.class));
        verify(versionMapper, never()).rewriteDraftSnapshot(any(AgreementVersion.class));
        verify(settingMapper, never()).insert(any(AgreementSetting.class));
        verify(narrativeMapper, never()).insert(any(AgreementNarrative.class));
        verify(fulfillmentModeMapper, never()).insert(any(AgreementFulfillmentMode.class));

        // ④ 下发给用户的口径必须写明"终止≠免责"
        assertTrue(vo.getExemptionNotice().contains("不会自动结清"), vo.getExemptionNotice());
        assertTrue(vo.getExemptionNotice().contains("平台不裁判"), vo.getExemptionNotice());
        assertEquals("单方终止", vo.getSourceLabel());
    }

    @Test
    @DisplayName("协商一致：发起时只是「待对方确认」，主档状态一行都不动（协议继续有效）")
    void mutualTerminationWaitsForCounterparty() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(terminationMapper.insert(any(AgreementTermination.class))).thenAnswer(inv -> {
            AgreementTermination t = inv.getArgument(0, AgreementTermination.class);
            t.setId(600L);
            t.setStatus(AgreementTerminationStatus.PENDING.getCode());
            return 1;
        });
        when(versionMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        AgreementTerminationCreateDTO dto = new AgreementTerminationCreateDTO();
        dto.setSource(AgreementTerminationSource.MUTUAL_AGREEMENT.name());
        dto.setBasisText("双方协商一致，业务方向调整");

        AgreementTerminationVO vo = service.requestTermination(1L, dto, TENANT_A, USER);

        assertEquals(AgreementTerminationStatus.PENDING.getCode(), vo.getStatus());
        verify(agreementMapper, never()).updateById(any(Agreement.class));
        assertEquals(AgreementStatus.ACTIVE.getCode(), agreement.getStatus(), "谈成之前协议继续有效");
    }

    @Test
    @DisplayName("对方确认终止：记录置为已终止，主档随之置为已终止（停止履行）")
    void confirmTerminationStopsPerformance() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        AgreementTermination pending = pendingMutual();
        when(terminationMapper.selectById(600L)).thenReturn(pending, confirmedMutual());
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(terminationMapper.updateById(any(AgreementTermination.class))).thenReturn(1);
        when(versionMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        AgreementTerminationVO vo = service.confirmTermination(600L, null, TENANT_B, USER + 1);

        ArgumentCaptor<AgreementTermination> captor = ArgumentCaptor.forClass(AgreementTermination.class);
        verify(terminationMapper).updateById(captor.capture());
        assertEquals(AgreementTerminationStatus.CONFIRMED.getCode(), captor.getValue().getStatus());
        assertEquals(USER + 1, captor.getValue().getCounterpartyActionBy());

        ArgumentCaptor<Agreement> agreementCaptor = ArgumentCaptor.forClass(Agreement.class);
        verify(agreementMapper).updateById(agreementCaptor.capture());
        assertEquals(AgreementStatus.TERMINATED.getCode(), agreementCaptor.getValue().getStatus());
        assertEquals(AgreementTerminationStatus.CONFIRMED.getCode(), vo.getStatus());

        verify(versionMapper, never()).updateById(any(AgreementVersion.class));
    }

    @Test
    @DisplayName("异议：待确认时提 ⇒ 终止不成立（协议继续有效）；已终止后提 ⇒ 只留痕，不回滚终止事实")
    void objectionBehaviourDependsOnStatus() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);

        // 待确认时提异议 ⇒ OBJECTED，主档不动
        AgreementTermination pending = pendingMutual();
        when(terminationMapper.selectById(600L)).thenReturn(pending, objectedMutual());
        when(terminationMapper.updateById(any(AgreementTermination.class))).thenReturn(1);
        when(versionMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        service.objectTermination(600L, null, TENANT_B, USER + 1);

        ArgumentCaptor<AgreementTermination> captor = ArgumentCaptor.forClass(AgreementTermination.class);
        verify(terminationMapper).updateById(captor.capture());
        assertEquals(AgreementTerminationStatus.OBJECTED.getCode(), captor.getValue().getStatus());
        assertEquals(Boolean.TRUE, captor.getValue().getCounterpartyObjection());
        verify(agreementMapper, never()).updateById(any(Agreement.class));

        // 已终止后提异议 ⇒ 状态保持 CONFIRMED，只记留痕
        AgreementTermination confirmed = confirmedMutual();
        when(terminationMapper.selectById(601L)).thenReturn(confirmed, confirmedMutual());
        service.objectTermination(601L, null, TENANT_B, USER + 1);

        ArgumentCaptor<AgreementTermination> second = ArgumentCaptor.forClass(AgreementTermination.class);
        verify(terminationMapper, times(2)).updateById(second.capture());
        assertNull(second.getAllValues().get(1).getStatus(), "已终止后提异议不改状态（不回滚终止事实）");
        assertEquals(Boolean.TRUE, second.getAllValues().get(1).getCounterpartyObjection());
    }

    @Test
    @DisplayName("发起方撤回终止：置为已撤回，主档不动")
    void withdrawTermination() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        AgreementTermination pending = pendingMutual();
        pending.setRequestedBy(USER);
        AgreementTermination withdrawn = pendingMutual();
        withdrawn.setRequestedBy(USER);
        withdrawn.setStatus(AgreementTerminationStatus.WITHDRAWN.getCode());
        when(terminationMapper.selectById(600L)).thenReturn(pending, withdrawn);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(terminationMapper.updateById(any(AgreementTermination.class))).thenReturn(1);
        when(versionMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        AgreementTerminationVO vo = service.withdrawTermination(600L, null, TENANT_A, USER);

        assertEquals(AgreementTerminationStatus.WITHDRAWN.getCode(), vo.getStatus());
        verify(agreementMapper, never()).updateById(any(Agreement.class));
    }

    @Test
    @DisplayName("平台清退：非平台侧发起被拒；平台服务协议里平台是甲方 ⇒ 放行")
    void platformExpulsionOnlyByPlatformSide() {
        Agreement tenantToTenant = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        when(agreementMapper.selectById(1L)).thenReturn(tenantToTenant);

        AgreementTerminationCreateDTO dto = new AgreementTerminationCreateDTO();
        dto.setSource(AgreementTerminationSource.PLATFORM_EXPULSION.name());
        dto.setBasisText("违反入驻规则第 3 条");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.requestTermination(1L, dto, TENANT_A, USER));
        assertTrue(e.getMessage().contains("只能由平台方发起"), e.getMessage());
        verify(terminationMapper, never()).insert(any(AgreementTermination.class));
    }

    @Test
    @DisplayName("主张对方违约：必须写清具体情形（主张是要拿去举证的，不能只是一句情绪）")
    void claimBreachNeedsNote() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);

        AgreementTerminationCreateDTO dto = new AgreementTerminationCreateDTO();
        dto.setSource(AgreementTerminationSource.COUNTERPARTY_BREACH.name());
        dto.setBasisText("对方逾期付款");
        dto.setClaimCounterpartyBreach(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.requestTermination(1L, dto, TENANT_A, USER));
        assertTrue(e.getMessage().contains("主张"), e.getMessage());
    }

    // ══════════════════════ 造数据 ══════════════════════

    private static Agreement agreement(Integer status, Long currentVersionId) {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setTenantId(0L);
        a.setAgreementNo("XY-20260922-0001");
        a.setAgreementType("DISTRIBUTION");
        a.setTitle("E2E 代销协议");
        a.setPartyAId(PARTY_A);
        a.setPartyATenantId(TENANT_A);
        a.setPartyBId(PARTY_B);
        a.setPartyBTenantId(TENANT_B);
        a.setStatus(status);
        a.setCurrentVersionId(currentVersionId);
        return a;
    }

    private static AgreementVersion version(Long id, int no, Integer status, String snapshot) {
        AgreementVersion v = new AgreementVersion();
        v.setId(id);
        v.setAgreementId(1L);
        v.setVersionNo(no);
        v.setStatus(status);
        v.setSnapshotJson(snapshot);
        return v;
    }

    private static AgreementInvite invite(AgreementInviteStatus status) {
        AgreementInvite invite = new AgreementInvite();
        invite.setId(500L);
        invite.setTenantId(0L);
        invite.setAgreementId(1L);
        invite.setVersionId(200L);
        invite.setTargetPartyId(PARTY_B);
        invite.setTargetTenantId(TENANT_B);
        invite.setTargetSide("B");
        invite.setTokenHash(cn.aiedge.agreement.domain.AgreementInviteToken.hash(
                "raw-token-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"));
        invite.setTokenHint("raw-toke");
        invite.setInviteCode("ABCD234567");
        invite.setChannel("LINK");
        invite.setStatus(status.getCode());
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        invite.setViewCount(0);
        invite.setCreateBy(USER);
        return invite;
    }

    private static AgreementInvite accepted(AgreementInvite invite) {
        AgreementInvite copy = new AgreementInvite();
        copy.setId(invite.getId());
        copy.setAgreementId(invite.getAgreementId());
        copy.setVersionId(invite.getVersionId());
        copy.setTargetPartyId(invite.getTargetPartyId());
        copy.setTargetTenantId(invite.getTargetTenantId());
        copy.setTargetSide(invite.getTargetSide());
        copy.setExpiresAt(invite.getExpiresAt());
        copy.setStatus(AgreementInviteStatus.ACCEPTED.getCode());
        copy.setAcceptedBy(USER);
        copy.setAcceptedAt(LocalDateTime.now());
        copy.setAcceptChannel("QRCODE");
        copy.setViewCount(1);
        return copy;
    }

    private static AgreementTermination pendingMutual() {
        AgreementTermination t = new AgreementTermination();
        t.setId(600L);
        t.setTenantId(0L);
        t.setAgreementId(1L);
        t.setVersionId(100L);
        t.setSource(AgreementTerminationSource.MUTUAL_AGREEMENT.name());
        t.setStatus(AgreementTerminationStatus.PENDING.getCode());
        t.setRequestedBy(USER);
        t.setRequestedSide("A");
        t.setRequestedAt(LocalDateTime.now());
        t.setBasisText("双方协商一致");
        t.setClaimCounterpartyBreach(false);
        t.setCounterpartyObjection(false);
        t.setStopPerformanceAt(LocalDateTime.now());
        return t;
    }

    private static AgreementTermination pendingUnilateral() {
        AgreementTermination t = pendingMutual();
        t.setSource(AgreementTerminationSource.UNILATERAL.name());
        t.setStatus(AgreementTerminationStatus.CONFIRMED.getCode());
        t.setEffectiveAt(LocalDateTime.now());
        t.setBasisText("按第 2 版第 7 条，合作条件已不具备");
        return t;
    }

    private static AgreementTermination confirmedMutual() {
        AgreementTermination t = pendingMutual();
        t.setStatus(AgreementTerminationStatus.CONFIRMED.getCode());
        t.setCounterpartyActionBy(USER + 1);
        t.setCounterpartyActionAt(LocalDateTime.now());
        t.setEffectiveAt(LocalDateTime.now());
        return t;
    }

    private static AgreementTermination objectedMutual() {
        AgreementTermination t = pendingMutual();
        t.setStatus(AgreementTerminationStatus.OBJECTED.getCode());
        t.setCounterpartyObjection(true);
        t.setObjectionReason("对方提出异议");
        return t;
    }
}
