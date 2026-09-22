package cn.aiedge.agreement;

import cn.aiedge.agreement.domain.AgreementTemplateVisibility;
import cn.aiedge.agreement.dto.AgreementContentSaveDTO;
import cn.aiedge.agreement.dto.AgreementFromTemplateDTO;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementTemplate;
import cn.aiedge.agreement.entity.AgreementTemplateNarrative;
import cn.aiedge.agreement.entity.AgreementTemplateSetting;
import cn.aiedge.agreement.entity.AgreementTemplateTerm;
import cn.aiedge.agreement.entity.AgreementTerm;
import cn.aiedge.agreement.entity.AgreementTermOption;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateNarrativeMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateSettingMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateTermMapper;
import cn.aiedge.agreement.mapper.AgreementTermMapper;
import cn.aiedge.agreement.mapper.AgreementTermOptionMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.agreement.service.AgreementContentService;
import cn.aiedge.agreement.service.AgreementService;
import cn.aiedge.agreement.service.impl.AgreementTemplateServiceImpl;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 契约模板（§13.9）的两条硬口径：
 *
 * <ol>
 *   <li><b>读写权限不对称</b>：平台可读<b>全部</b>模板（合规抽查读，目的是避免非法交易），
 *       但**不能改**租户模板（能改就变成平台替租户定商业条款）；租户只能读「平台模板 + 自己的」；</li>
 *   <li><b>模板不是默认值</b>：从模板发起只是把内容**预填**到新草稿版本，
 *       并把"基于模板 X"记进版本快照（司法可追溯）；版本**不会被置成生效**，
 *       设定/文字的最终取值仍以双方签署的那一版为准（{@code AgreementRuntime} 不读模板）。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgreementTemplateTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Agreement.class);
        TableInfoHelper.initTableInfo(assistant, AgreementVersion.class);
        TableInfoHelper.initTableInfo(assistant, AgreementTerm.class);
        TableInfoHelper.initTableInfo(assistant, AgreementTermOption.class);
        TableInfoHelper.initTableInfo(assistant, AgreementTemplate.class);
        TableInfoHelper.initTableInfo(assistant, AgreementTemplateTerm.class);
        TableInfoHelper.initTableInfo(assistant, AgreementTemplateSetting.class);
        TableInfoHelper.initTableInfo(assistant, AgreementTemplateNarrative.class);
    }

    private static final Long TENANT_A = 2L;
    private static final Long TENANT_B = 3L;
    private static final Long USER = 9L;

    @Mock
    private AgreementTemplateMapper templateMapper;
    @Mock
    private AgreementTemplateTermMapper templateTermMapper;
    @Mock
    private AgreementTemplateSettingMapper templateSettingMapper;
    @Mock
    private AgreementTemplateNarrativeMapper templateNarrativeMapper;
    @Mock
    private AgreementTermMapper termMapper;
    @Mock
    private AgreementTermOptionMapper termOptionMapper;
    @Mock
    private AgreementMapper agreementMapper;
    @Mock
    private AgreementVersionMapper versionMapper;
    @Mock
    private AgreementService agreementService;
    @Mock
    private AgreementContentService contentService;

    @InjectMocks
    private AgreementTemplateServiceImpl service;

    // ══════════════════════ ① 读 / 写权限不对称 ══════════════════════

    @Test
    @DisplayName("平台可读所有模板（含租户模板）；租户只能读自己的 + 平台模板")
    void platformReadsEverythingTenantReadsItsOwnAndPlatform() {
        AgreementTemplate platform = template(1L, "PLATFORM", 0L);
        AgreementTemplate mine = template(2L, "TENANT", TENANT_A);
        AgreementTemplate others = template(3L, "TENANT", TENANT_B);

        assertTrue(AgreementTemplateVisibility.canSee(platform, TENANT_A, false));
        assertTrue(AgreementTemplateVisibility.canSee(mine, TENANT_A, false));
        assertFalse(AgreementTemplateVisibility.canSee(others, TENANT_A, false),
                "别家租户的模板条款是同业敏感信息，不能看到");

        assertTrue(AgreementTemplateVisibility.canSee(others, TENANT_A, true),
                "平台合规抽查读：可读全部模板（避免非法交易）");
        assertTrue(AgreementTemplateVisibility.canSee(mine, TENANT_A, true));

        // 看不到时按「不存在」回话（403 会泄露"这份模板存在"）
        BusinessException e = assertThrows(BusinessException.class,
                () -> AgreementTemplateVisibility.assertVisible(others, TENANT_A, false));
        assertEquals(404, e.getCode());
    }

    @Test
    @DisplayName("平台能读租户模板但**不能改**；租户不能改平台模板（能改就等于平台替租户定条款）")
    void manageabilityIsAsymmetric() {
        AgreementTemplate platform = template(1L, "PLATFORM", 0L);
        AgreementTemplate mine = template(2L, "TENANT", TENANT_A);
        AgreementTemplate others = template(3L, "TENANT", TENANT_B);

        // 平台：管平台模板
        AgreementTemplateVisibility.assertManageable(platform, TENANT_A, true);
        // 平台：不能改租户模板
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> AgreementTemplateVisibility.assertManageable(mine, TENANT_A, true));
        assertTrue(e1.getMessage().contains("只能由该租户自己维护"), e1.getMessage());
        // 租户：管自己的
        AgreementTemplateVisibility.assertManageable(mine, TENANT_A, false);
        // 租户：不能改平台模板
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> AgreementTemplateVisibility.assertManageable(platform, TENANT_A, false));
        assertTrue(e2.getMessage().contains("平台统一维护"), e2.getMessage());
        // 租户：连别家租户的模板都看不到，更谈不上改
        assertThrows(BusinessException.class,
                () -> AgreementTemplateVisibility.assertManageable(others, TENANT_A, false));
    }

    @Test
    @DisplayName("归属位由服务端决定：平台模板记 0（且只有平台侧能建）；租户模板记本租户")
    void ownerTenantIsDecidedByServer() {
        assertEquals(0L, AgreementTemplateVisibility.resolveOwnerTenantId(
                cn.aiedge.agreement.enums.AgreementTemplateScope.PLATFORM, TENANT_A, true));
        assertEquals(TENANT_A, AgreementTemplateVisibility.resolveOwnerTenantId(
                cn.aiedge.agreement.enums.AgreementTemplateScope.TENANT, TENANT_A, false));
        BusinessException e = assertThrows(BusinessException.class,
                () -> AgreementTemplateVisibility.resolveOwnerTenantId(
                        cn.aiedge.agreement.enums.AgreementTemplateScope.PLATFORM, TENANT_A, false));
        assertTrue(e.getMessage().contains("只有平台侧能新建平台模板"), e.getMessage());
    }

    // ══════════════════════ ② 从模板发起：预填 + 来源留痕，但绝不等于已约定 ══════════════════════

    @Test
    @DisplayName("从模板发起：只建草稿版本、把模板来源记进快照；绝不置生效、也不替双方确认")
    void applyRecordsTemplateSourceButNeverActivates() {
        stubApply();

        AgreementFromTemplateDTO dto = new AgreementFromTemplateDTO();
        dto.setPartyAId(11L);
        dto.setPartyATenantId(TENANT_A);
        dto.setPartyBId(22L);
        dto.setPartyBTenantId(TENANT_B);
        dto.setTitle("代销合作（基于标准模板）");

        Long agreementId = service.apply(200L, dto, TENANT_A, false, USER);
        assertEquals(1L, agreementId);

        // ① 内容只预填到**草稿版本**上
        ArgumentCaptor<AgreementContentSaveDTO> contentCaptor =
                ArgumentCaptor.forClass(AgreementContentSaveDTO.class);
        verify(contentService).saveContent(org.mockito.ArgumentMatchers.eq(100L), contentCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(TENANT_A), org.mockito.ArgumentMatchers.eq(USER));
        assertEquals(1, contentCaptor.getValue().getSettings().size(), "模板里的设定项应被预填（作为起点）");

        // ② 模板来源写进版本快照（"基于模板 X，第 N 版"）—— 司法可追溯
        ArgumentCaptor<AgreementVersion> versionCaptor = ArgumentCaptor.forClass(AgreementVersion.class);
        verify(versionMapper).rewriteDraftSnapshot(versionCaptor.capture());
        String snapshot = versionCaptor.getValue().getSnapshotJson();
        assertTrue(snapshot.contains("templateSource"), "版本快照必须记下模板来源（§13.9）: " + snapshot);
        assertTrue(snapshot.contains("标准代销模板"), snapshot);
        assertTrue(snapshot.contains("第 1 版"), snapshot);

        // ③ 绝不替双方生效，也绝不替双方签署
        verify(agreementService, never()).activate(anyLong(), anyLong(), anyLong());
        verify(agreementService, never()).confirm(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("模板未通过法务审核（判定违法）⇒ 不许用于发起契约（㊲ 违法条款无效）")
    void rejectedTemplateCannotBeApplied() {
        AgreementTemplate t = template(200L, "TENANT", TENANT_A);
        t.setLegalReviewStatus("REJECTED");
        when(templateMapper.selectById(200L)).thenReturn(t);

        AgreementFromTemplateDTO dto = new AgreementFromTemplateDTO();
        dto.setTitle("试试");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.apply(200L, dto, TENANT_A, false, USER));
        assertTrue(e.getMessage().contains("未通过法务审核"), e.getMessage());
        verify(agreementService, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("停用模板不许发起；看不到的模板按「不存在」回话")
    void disabledAndInvisibleTemplatesAreRejected() {
        AgreementTemplate disabled = template(200L, "TENANT", TENANT_A);
        disabled.setStatus(0);
        when(templateMapper.selectById(200L)).thenReturn(disabled);
        AgreementFromTemplateDTO dto = new AgreementFromTemplateDTO();
        dto.setTitle("试试");
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> service.apply(200L, dto, TENANT_A, false, USER));
        assertTrue(e1.getMessage().contains("已停用"), e1.getMessage());

        AgreementTemplate others = template(300L, "TENANT", TENANT_B);
        when(templateMapper.selectById(300L)).thenReturn(others);
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> service.apply(300L, dto, TENANT_A, false, USER));
        assertEquals(404, e2.getCode(), "看不到的模板一律按「不存在」回话");
    }

    // ══════════════════════ 造数据 ══════════════════════

    private void stubApply() {
        when(templateMapper.selectById(200L)).thenReturn(template(200L, "TENANT", TENANT_A));
        when(agreementService.create(any(), any(), any())).thenReturn(1L);

        AgreementVersion draft = new AgreementVersion();
        draft.setId(100L);
        draft.setAgreementId(1L);
        draft.setVersionNo(1);
        draft.setStatus(AgreementVersionStatus.DRAFT.getCode());
        draft.setSnapshotJson("{\"terms\":[]}");
        when(versionMapper.selectList(any())).thenReturn(List.of(draft));

        Agreement agreement = new Agreement();
        agreement.setId(1L);
        agreement.setTenantId(0L);
        agreement.setAgreementNo("XY-20260922-0002");
        agreement.setAgreementType("DISTRIBUTION");
        agreement.setTitle("代销合作（基于标准模板）");
        agreement.setPartyAId(11L);
        agreement.setPartyATenantId(TENANT_A);
        agreement.setPartyBId(22L);
        agreement.setPartyBTenantId(TENANT_B);
        agreement.setStatus(AgreementStatus.DRAFT.getCode());
        when(agreementMapper.selectById(1L)).thenReturn(agreement);

        when(termOptionMapper.selectList(any())).thenReturn(List.of());
        when(templateTermMapper.selectList(any())).thenReturn(List.of());
        when(templateNarrativeMapper.selectList(any())).thenReturn(List.of());
        when(templateSettingMapper.selectList(any())).thenReturn(List.of(templateSetting("AR_CREDIT_DAYS", 30)));
        when(versionMapper.rewriteDraftSnapshot(any())).thenReturn(1);
    }

    private static AgreementTemplate template(Long id, String scope, Long tenantId) {
        AgreementTemplate t = new AgreementTemplate();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setScope(scope);
        t.setTemplateName("PLATFORM".equals(scope) ? "平台标准模板" : "标准代销模板");
        t.setAgreementType("DISTRIBUTION");
        t.setLegalReviewStatus("APPROVED");
        t.setStatus(1);
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        return t;
    }

    private static AgreementTemplateSetting templateSetting(String key, int number) {
        AgreementTemplateSetting s = new AgreementTemplateSetting();
        s.setId(1L);
        s.setTenantId(TENANT_A);
        s.setTemplateId(200L);
        s.setSettingKey(key);
        s.setValueType("NUMBER");
        s.setValueNumber(new BigDecimal(number));
        s.setSort(0);
        return s;
    }

}
