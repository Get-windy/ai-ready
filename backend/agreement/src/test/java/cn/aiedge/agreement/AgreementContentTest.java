package cn.aiedge.agreement;

import cn.aiedge.agreement.domain.AgreementResolvedSettings;
import cn.aiedge.agreement.domain.AgreementRuntime;
import cn.aiedge.agreement.domain.AgreementSettingValue;
import cn.aiedge.agreement.dto.AgreementContentSaveDTO;
import cn.aiedge.agreement.dto.AgreementContentVO;
import cn.aiedge.agreement.dto.AgreementEffectiveSettingsVO;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementNarrative;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.mapper.AgreementFulfillmentModeMapper;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementNarrativeMapper;
import cn.aiedge.agreement.mapper.AgreementSettingMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.agreement.service.impl.AgreementContentServiceImpl;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 协议内容层的服务行为：设定版 / 文字版 / 履约方式集合。
 *
 * <p>钉死四件最容易做错的事：</p>
 * <ol>
 *   <li><b>已生效版本不可改</b>（㉛）—— 保存内容必须先过 {@code AgreementInvariants}；</li>
 *   <li><b>可见性</b>：第三方租户读内容一律按"协议不存在"（404，不泄露存在性，裁定⑥）；</li>
 *   <li><b>文字条款不自动执行</b>：下发必须带 {@code autoExecutable = false} + 中文说明（§13.2 硬要求）；</li>
 *   <li><b>未约定就是未约定</b>：留空/非法值都不许被写成一个"看起来像约定"的值（㉜）。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgreementContentTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Agreement.class);
        TableInfoHelper.initTableInfo(assistant, AgreementVersion.class);
        TableInfoHelper.initTableInfo(assistant, AgreementSetting.class);
        TableInfoHelper.initTableInfo(assistant, AgreementNarrative.class);
        TableInfoHelper.initTableInfo(assistant, AgreementFulfillmentMode.class);
    }

    private static final Long TENANT_A = 2L;
    private static final Long TENANT_B = 3L;
    private static final Long TENANT_OTHER = 99L;
    private static final Long USER = 9L;

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
    private AgreementRuntime runtime;

    @InjectMocks
    private AgreementContentServiceImpl service;

    // ══════════════════════ ① 只有草稿能改 ══════════════════════

    @Test
    @DisplayName("已生效版本的内容保存被拒（㉛：改内容只能发起变更、双方重新确认）")
    void cannotSaveContentOnActiveVersion() {
        stubAgreementAndVersion(AgreementVersionStatus.ACTIVE.getCode());

        BusinessException e = assertThrows(BusinessException.class, () -> service.saveContent(
                100L, new AgreementContentSaveDTO(), TENANT_A, USER));
        assertTrue(e.getMessage().contains("已生效的协议版本不可修改"), e.getMessage());
        verify(settingMapper, never()).insert(any(AgreementSetting.class));
        verify(versionMapper, never()).rewriteDraftSnapshot(any(AgreementVersion.class));
    }

    @Test
    @DisplayName("被否决的版本也不能改内容（历史快照只读）")
    void cannotSaveContentOnRejectedVersion() {
        stubAgreementAndVersion(AgreementVersionStatus.REJECTED.getCode());
        BusinessException e = assertThrows(BusinessException.class, () -> service.saveContent(
                100L, new AgreementContentSaveDTO(), TENANT_A, USER));
        assertTrue(e.getMessage().contains("已被否决"), e.getMessage());
    }

    // ══════════════════════ ② 可见性 ══════════════════════

    @Test
    @DisplayName("第三方租户读内容按「协议不存在」回话（404，不泄露协议存在性）")
    void thirdPartyCannotReadContent() {
        stubAgreementAndVersion(AgreementVersionStatus.DRAFT.getCode());
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.content(100L, TENANT_OTHER));
        assertEquals(404, e.getCode());
        assertTrue(e.getMessage().contains("协议不存在"), e.getMessage());
    }

    // ══════════════════════ ③ 文字版不自动执行 ══════════════════════

    @Test
    @DisplayName("文字条款下发必须带「不会自动执行」的显式标记与中文说明（§13.2 硬要求）")
    void narrativeIsMarkedAsNotAutoExecutable() {
        stubAgreementAndVersion(AgreementVersionStatus.ACTIVE.getCode());
        when(runtime.definitions()).thenReturn(List.of());
        when(runtime.valuesOf(100L)).thenReturn(java.util.Map.of());
        when(narrativeMapper.selectList(any())).thenReturn(List.of(narrative("DISPUTE", "争议解决与管辖", "提交杭州仲裁委员会仲裁")));

        AgreementContentVO vo = service.content(100L, TENANT_A);

        assertEquals(1, vo.getNarratives().size());
        assertEquals(Boolean.FALSE, vo.getNarratives().get(0).getAutoExecutable(),
                "文字条款永不自动执行 —— 这个标记必须显式下发，不能只写在注释里");
        assertNotNull(vo.getNarratives().get(0).getManualNotice());
        assertTrue(vo.getNarratives().get(0).getManualNotice().contains("不会自动执行"),
                vo.getNarratives().get(0).getManualNotice());
        assertNotNull(vo.getNarratives().get(0).getContentHash(), "文字条款要留哈希以便证明没被改过");
        // 已生效版本的内容不可编辑
        assertEquals(Boolean.FALSE, vo.getEditable());
        assertNotNull(vo.getEditableHint());
    }

    @Test
    @DisplayName("必填字段未约定 ⇒ 内容回执明确列出（不是默认值兜底）")
    void missingRequiredSettingsAreReported() {
        stubAgreementAndVersion(AgreementVersionStatus.DRAFT.getCode());
        AgreementSettingDef required = def("AR_CREDIT_DAYS", "账期天数", "NUMBER", true);
        when(runtime.definitions()).thenReturn(List.of(required));
        when(runtime.valuesOf(100L)).thenReturn(java.util.Map.of(
                "AR_CREDIT_DAYS", AgreementSettingValue.undeclared(required)));
        when(narrativeMapper.selectList(any())).thenReturn(List.of());

        AgreementContentVO vo = service.content(100L, TENANT_A);

        assertEquals(List.of("AR_CREDIT_DAYS"), vo.getMissingRequiredSettings());
        assertEquals(List.of("账期天数"), vo.getMissingRequiredSettingNames());
        assertEquals(AgreementSettingValue.State.UNDECLARED,
                AgreementSettingValue.State.valueOf(vo.getSettings().get(0).getState()));
        assertEquals("未约定", vo.getSettings().get(0).getStateLabel());
        assertTrue(vo.getSettings().get(0).getDisplayValue().contains("未约定"),
                "未约定必须显示成「未约定」，绝不能显示成 0");
    }

    // ══════════════════════ ④ 写入：未约定就是未约定 + 履约方式是集合 ══════════════════════

    @Test
    @DisplayName("设定项留空 ⇒ 撤回该项约定（不写行，回到「未约定」），不产生任何值")
    void blankValueMeansUndeclared() {
        stubAgreementAndVersion(AgreementVersionStatus.DRAFT.getCode());
        when(runtime.definitions()).thenReturn(List.of(def("AR_CREDIT_DAYS", "账期天数", "NUMBER", false)));
        when(runtime.valuesOf(100L)).thenReturn(java.util.Map.of());
        when(settingMapper.selectList(any())).thenReturn(List.of());
        when(narrativeMapper.selectList(any())).thenReturn(List.of());
        when(fulfillmentModeMapper.selectList(any())).thenReturn(List.of());

        AgreementContentSaveDTO dto = new AgreementContentSaveDTO();
        AgreementContentSaveDTO.SettingItem item = new AgreementContentSaveDTO.SettingItem();
        item.setSettingKey("AR_CREDIT_DAYS");
        item.setValue("   ");
        dto.setSettings(List.of(item));

        service.saveContent(100L, dto, TENANT_A, USER);

        verify(settingMapper, never()).insert(any(AgreementSetting.class));
    }

    @Test
    @DisplayName("设定值非法 ⇒ 明确报错，绝不兜底成 0（那会把「填错了」伪装成「已约定」）")
    void invalidValueIsRejectedNotDefaulted() {
        stubAgreementAndVersion(AgreementVersionStatus.DRAFT.getCode());
        when(runtime.definitions()).thenReturn(List.of(def("AR_CREDIT_DAYS", "账期天数", "NUMBER", false)));
        when(settingMapper.selectList(any())).thenReturn(List.of());
        when(narrativeMapper.selectList(any())).thenReturn(List.of());
        when(fulfillmentModeMapper.selectList(any())).thenReturn(List.of());

        AgreementContentSaveDTO dto = new AgreementContentSaveDTO();
        AgreementContentSaveDTO.SettingItem item = new AgreementContentSaveDTO.SettingItem();
        item.setSettingKey("AR_CREDIT_DAYS");
        item.setValue("三十天");
        dto.setSettings(List.of(item));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveContent(100L, dto, TENANT_A, USER));
        assertTrue(e.getMessage().contains("必须填数字"), e.getMessage());
        verify(settingMapper, never()).insert(any(AgreementSetting.class));
    }

    @Test
    @DisplayName("枚举值必须在候选内；履约方式集合可多行并存（直发 + 中转同时约定）")
    void enumValidatedAndModesAreASet() {
        stubAgreementAndVersion(AgreementVersionStatus.DRAFT.getCode());
        AgreementSettingDef modesDef = def("FULFILLMENT_MODES", "履约方式集合", "ENUM", true);
        modesDef.setOptions("DROP_SHIP,TRANSIT_STOCK,PICKUP,LOCAL_STOCK");
        when(runtime.definitions()).thenReturn(List.of(modesDef));
        when(runtime.valuesOf(100L)).thenReturn(java.util.Map.of());
        when(settingMapper.selectList(any())).thenReturn(List.of());
        when(narrativeMapper.selectList(any())).thenReturn(List.of());
        when(fulfillmentModeMapper.selectList(any())).thenReturn(List.of());

        AgreementContentSaveDTO dto = new AgreementContentSaveDTO();
        dto.setFulfillmentModes(List.of("DROP_SHIP", "TRANSIT_STOCK"));

        service.saveContent(100L, dto, TENANT_A, USER);

        ArgumentCaptor<AgreementFulfillmentMode> captor = ArgumentCaptor.forClass(AgreementFulfillmentMode.class);
        verify(fulfillmentModeMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<AgreementFulfillmentMode> rows = captor.getAllValues();
        assertEquals("DROP_SHIP", rows.get(0).getMode());
        assertEquals("TRANSIT_STOCK", rows.get(1).getMode(), "两种履约方式必须并存 —— 一版多行");
        assertEquals(0L, rows.get(0).getTenantId(), "系统级归属位必须显式为 0（裁定⑥）");
    }

    // ══════════════════════ ⑤ 按业务时点取生效设定（只读视图） ══════════════════════

    @Test
    @DisplayName("按业务时点取生效设定：没有生效版本时给出明确原因，不下发空壳让人误读成「没约定」")
    void effectiveSurfacesAbsenceExplicitly() {
        stubAgreementAndVersion(AgreementVersionStatus.DRAFT.getCode());
        when(runtime.resolveByAgreement(any(), any())).thenReturn(
                AgreementResolvedSettings.noEffectiveVersion("协议「XY-1」在该时点没有生效版本", LocalDateTime.of(2026, 9, 1, 0, 0), List.of()));

        AgreementEffectiveSettingsVO vo = service.effective(1L, LocalDateTime.of(2026, 9, 1, 0, 0), TENANT_A);

        assertEquals(Boolean.FALSE, vo.getVersionFound());
        assertTrue(vo.getAbsenceReason().contains("没有生效版本"), vo.getAbsenceReason());
        assertFalse(vo.getAbsenceReason().contains("未约定"), "「没有生效版本」与「未约定」是两件事，措辞必须分开");
    }

    // ══════════════════════ 造数据 ══════════════════════

    private void stubAgreementAndVersion(Integer versionStatus) {
        Agreement agreement = new Agreement();
        agreement.setId(1L);
        agreement.setTenantId(0L);
        agreement.setAgreementNo("XY-20260922-0001");
        agreement.setAgreementType("DISTRIBUTION");
        agreement.setPartyAId(11L);
        agreement.setPartyATenantId(TENANT_A);
        agreement.setPartyBId(22L);
        agreement.setPartyBTenantId(TENANT_B);
        agreement.setStatus(AgreementStatus.DRAFT.getCode());

        AgreementVersion version = new AgreementVersion();
        version.setId(100L);
        version.setAgreementId(1L);
        version.setVersionNo(1);
        version.setStatus(versionStatus);
        version.setSnapshotJson("{\"terms\":[]}");

        when(versionMapper.selectById(100L)).thenReturn(version);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
    }

    private static AgreementSettingDef def(String key, String label, String type, boolean required) {
        AgreementSettingDef d = new AgreementSettingDef();
        d.setId(1L);
        d.setTenantId(0L);
        d.setSettingKey(key);
        d.setLabel(label);
        d.setValueType(type);
        d.setRequired(required);
        d.setConsumerPoint("AR_DUE_DATE");
        d.setStatus(1);
        return d;
    }

    private static AgreementNarrative narrative(String section, String title, String text) {
        AgreementNarrative n = new AgreementNarrative();
        n.setId(1L);
        n.setTenantId(0L);
        n.setAgreementId(1L);
        n.setVersionId(100L);
        n.setSectionCode(section);
        n.setSectionTitle(title);
        n.setContentText(text);
        n.setContentHash("hash");
        return n;
    }
}
