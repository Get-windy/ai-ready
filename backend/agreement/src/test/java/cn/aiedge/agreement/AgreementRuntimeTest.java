package cn.aiedge.agreement;

import cn.aiedge.agreement.domain.AgreementResolvedSettings;
import cn.aiedge.agreement.domain.AgreementRuntime;
import cn.aiedge.agreement.domain.AgreementSettingValue;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.enums.FulfillmentMode;
import cn.aiedge.agreement.mapper.AgreementFulfillmentModeMapper;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementSettingDefMapper;
import cn.aiedge.agreement.mapper.AgreementSettingMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateNarrativeMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateSettingMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateTermMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code AgreementRuntime} —— 生效设定的唯一读取入口（DOMAIN-MODEL §13.3）的三条硬口径。
 *
 * <ol>
 *   <li><b>必须带业务时点</b>：两个版本区间，不同时点取到不同版本；没有生效版本要给出
 *       **明确结果**而不是 NPE；业务时点为空直接拒绝（不许"读当前版本"）；</li>
 *   <li><b>未约定 ⇒ 返回"未约定"，绝不回落默认值</b>：三态可区分；{@code require} 未约定时抛中文异常；</li>
 *   <li><b>模板不参与 resolve</b>：结构性断言（运行时不可能持有模板表）+ 行为断言（模板里有、
 *       本版没约定 ⇒ 仍是"未约定"）。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgreementRuntimeTest {

    /** 纯单元测试没有 Spring/MyBatis 环境，先手工注册用到的实体映射（否则 Lambda 条件解析不了列名）。 */
    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Agreement.class);
        TableInfoHelper.initTableInfo(assistant, AgreementVersion.class);
        TableInfoHelper.initTableInfo(assistant, AgreementSetting.class);
        TableInfoHelper.initTableInfo(assistant, AgreementSettingDef.class);
        TableInfoHelper.initTableInfo(assistant, AgreementFulfillmentMode.class);
    }

    private static final Long TENANT_A = 2L;
    private static final Long TENANT_B = 3L;

    @Mock
    private AgreementMapper agreementMapper;
    @Mock
    private AgreementVersionMapper versionMapper;
    @Mock
    private AgreementSettingMapper settingMapper;
    @Mock
    private AgreementSettingDefMapper settingDefMapper;
    @Mock
    private AgreementFulfillmentModeMapper fulfillmentModeMapper;

    /**
     * 模板三张表的 Mapper —— <b>刻意只是声明出来做"零查询"断言用的</b>：
     * {@code AgreementRuntime} 的构造参数里根本没有它们（见
     * {@link #runtimeStructurallyCannotReadTemplates()}），因此它们不参与注入。
     */
    @Mock
    private AgreementTemplateSettingMapper templateSettingMapper;
    @Mock
    private AgreementTemplateTermMapper templateTermMapper;
    @Mock
    private AgreementTemplateNarrativeMapper templateNarrativeMapper;

    @InjectMocks
    private AgreementRuntime runtime;

    // ══════════════════════ ① 业务时点决定取哪一版 ══════════════════════

    @Test
    @DisplayName("两版本区间：按不同业务时点取到不同版本（不许读「当前版本」）")
    void picksVersionByBusinessTime() {
        stubAgreement();
        // v1：2026-01-01 ~ 2026-07-01（已被取代，但在自己的区间里确实生效过）
        // v2：2026-07-01 起（当前生效）
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.SUPERSEDED.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0)),
                version(102L, 2, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 7, 1, 0, 0), null)));
        stubDefs();
        when(settingMapper.selectList(any())).thenReturn(List.of(numberSetting("AR_CREDIT_DAYS", 30)));
        when(fulfillmentModeMapper.selectList(any())).thenReturn(List.of(mode("DROP_SHIP")));

        AgreementResolvedSettings inJune = runtime.resolve(11L, TENANT_A, 22L, TENANT_B,
                LocalDateTime.of(2026, 6, 15, 10, 0));
        AgreementResolvedSettings inAugust = runtime.resolve(11L, TENANT_A, 22L, TENANT_B,
                LocalDateTime.of(2026, 8, 15, 10, 0));

        assertTrue(inJune.isVersionFound());
        assertTrue(inAugust.isVersionFound());
        assertEquals(1, inJune.getVersionNo(), "6 月那一刻生效的是第 1 版（历史口径不许被新版篡改）");
        assertEquals(101L, inJune.getVersionId());
        assertEquals(2, inAugust.getVersionNo(), "8 月那一刻生效的是第 2 版");
        assertEquals(102L, inAugust.getVersionId());
    }

    @Test
    @DisplayName("该时点没有生效版本 ⇒ 给出明确结果（不是 null、不是 NPE），require 时明确报错")
    void noEffectiveVersionGivesExplicitResult() {
        stubAgreement();
        // 只有草稿：从未生效，任何时点都不该按它执行
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.DRAFT.getCode(), null, null)));
        stubDefs();
        when(settingMapper.selectList(any())).thenReturn(List.of());

        AgreementResolvedSettings resolved = runtime.resolve(11L, TENANT_A, 22L, TENANT_B, LocalDateTime.now());

        assertFalse(resolved.isVersionFound());
        assertTrue(resolved.getAbsenceReason() != null && resolved.getAbsenceReason().contains("没有生效版本"),
                resolved.getAbsenceReason());
        assertTrue(resolved.getValues().isEmpty(), "没有生效版本时不应有设定值");

        BusinessException e = assertThrows(BusinessException.class,
                () -> resolved.require("AR_CREDIT_DAYS", "生成应收到期日"));
        assertTrue(e.getMessage().contains("没有生效版本"), e.getMessage());
    }

    @Test
    @DisplayName("业务时点为空 ⇒ 直接拒绝（不许用「当前版本」凑，那会篡改已发生交易的口径）")
    void businessTimeIsMandatory() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> runtime.resolve(11L, TENANT_A, 22L, TENANT_B, null));
        assertTrue(e.getMessage().contains("必须提供业务时点"), e.getMessage());
    }

    @Test
    @DisplayName("没有覆盖该租户对的生效协议 ⇒ 明确结果；同租户对有多份生效协议 ⇒ 明确报错，不猜一份")
    void tenantPairMatching() {
        stubDefs();
        when(versionMapper.selectList(any())).thenReturn(List.of());
        when(settingMapper.selectList(any())).thenReturn(List.of());

        when(agreementMapper.selectList(any())).thenReturn(List.of());
        AgreementResolvedSettings none = runtime.resolve(11L, TENANT_A, 22L, TENANT_B, LocalDateTime.now());
        assertFalse(none.isVersionFound());
        assertTrue(none.getAbsenceReason().contains("没有找到覆盖"), none.getAbsenceReason());

        when(agreementMapper.selectList(any())).thenReturn(List.of(
                agreement("XY-1"), agreement("XY-2")));
        BusinessException e = assertThrows(BusinessException.class,
                () -> runtime.resolve(11L, TENANT_A, 22L, TENANT_B, LocalDateTime.now()));
        assertTrue(e.getMessage().contains("无法判断按哪一份执行"), e.getMessage());
    }

    // ══════════════════════ ② 三态：已约定 / 未约定 / 未定义 ══════════════════════

    @Test
    @DisplayName("三态可区分：已约定有值、未约定不是默认值、字段未定义单独一态")
    void threeStatesAreDistinguishable() {
        AgreementResolvedSettings resolved = resolvedWith(
                List.of(numberSetting("AR_CREDIT_DAYS", 30), textSetting("LEGACY_KEY", "旧字段")),
                List.of(mode("DROP_SHIP")));

        AgreementSettingValue agreed = resolved.get("AR_CREDIT_DAYS");
        assertEquals(AgreementSettingValue.State.AGREED, agreed.getState());
        assertEquals("30", agreed.displayValue());

        AgreementSettingValue undeclared = resolved.get("AR_CREDIT_PROVIDER");
        assertEquals(AgreementSettingValue.State.UNDECLARED, undeclared.getState());
        assertEquals("未约定", undeclared.displayValue());
        assertEquals(null, undeclared.rawValue(), "未约定**不许**回落到任何默认值（连 0 都不许）");
        assertFalse(undeclared.isAgreed());

        AgreementSettingValue undefined = resolved.get("LEGACY_KEY");
        assertEquals(AgreementSettingValue.State.UNDEFINED, undefined.getState(),
                "字段不在平台字典里 = 未定义（配置问题），不能与「双方未约定」混为一谈");
    }

    @Test
    @DisplayName("require() 未约定 ⇒ 抛中文业务异常，文案点明字段与用途，并说明系统不会自动执行")
    void requireThrowsOnUndeclared() {
        AgreementResolvedSettings resolved = resolvedWith(
                List.of(numberSetting("COMMISSION_RATE", 5)), List.of(mode("DROP_SHIP")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> resolved.require("AR_CREDIT_DAYS", "生成应收到期日"));
        assertTrue(e.getMessage().contains("未约定"), e.getMessage());
        assertTrue(e.getMessage().contains("账期天数"), e.getMessage());
        assertTrue(e.getMessage().contains("生成应收到期日"), e.getMessage());
        assertTrue(e.getMessage().contains("不会自动执行"), e.getMessage());

        // 已约定的字段照常取值
        assertEquals(0, resolved.require("COMMISSION_RATE", "结算分账").requireNumber().compareTo(new java.math.BigDecimal("5")));
    }

    @Test
    @DisplayName("require() 取到未定义字段 ⇒ 说清是「平台字典里没这个字段」，与「未约定」措辞不同")
    void requireThrowsOnUndefined() {
        AgreementResolvedSettings resolved = resolvedWith(
                List.of(textSetting("LEGACY_KEY", "旧字段")), List.of());
        BusinessException e = assertThrows(BusinessException.class,
                () -> resolved.require("LEGACY_KEY", "计算什么"));
        assertTrue(e.getMessage().contains("未在平台字段字典中定义"), e.getMessage());
        assertFalse(e.getMessage().contains("未约定"), "未定义与未约定必须分开说，否则排查方向会跑偏");
    }

    // ══════════════════════ ③ 履约方式集合 ══════════════════════

    @Test
    @DisplayName("履约方式是**集合**：同城直发 + 异地中转同时存在，两者都能取到")
    void fulfillmentModesAreASet() {
        AgreementResolvedSettings resolved = resolvedWith(
                List.of(numberSetting("AR_CREDIT_DAYS", 30)),
                List.of(mode("DROP_SHIP"), mode("TRANSIT_STOCK")));

        assertEquals(2, resolved.getFulfillmentModes().size());
        assertTrue(resolved.allows(FulfillmentMode.DROP_SHIP));
        assertTrue(resolved.allows(FulfillmentMode.TRANSIT_STOCK));
        assertFalse(resolved.allows(FulfillmentMode.PICKUP));
        // 「履约方式集合」这一项也能在三态视图里看到：值来自履约方式表（唯一事实来源）
        assertEquals(AgreementSettingValue.State.AGREED, resolved.get("FULFILLMENT_MODES").getState());
        assertEquals("DROP_SHIP,TRANSIT_STOCK", resolved.get("FULFILLMENT_MODES").displayValue());
    }

    @Test
    @DisplayName("未约定履约方式 ⇒ 空集 + 该项为「未约定」（不等于「随便用哪种」）")
    void emptyFulfillmentModesMeansUndeclared() {
        AgreementResolvedSettings resolved = resolvedWith(List.of(), List.of());
        assertTrue(resolved.getFulfillmentModes().isEmpty());
        assertEquals(AgreementSettingValue.State.UNDECLARED, resolved.get("FULFILLMENT_MODES").getState());
    }

    // ══════════════════════ ④ 模板不是默认值 ══════════════════════

    @Test
    @DisplayName("模板不参与 resolve：结构上做不到 + 行为上模板值绝不漏进设定集 + 全路径零模板查询")
    void runtimeStructurallyCannotReadTemplates() {
        // 结构性断言：本类的构造参数 / 字段 / 方法签名里**不出现任何模板类型**。
        // 若哪天有人为了"顺手支持一下模板"把 AgreementTemplateSettingMapper 注入进来，这条会立刻失败。
        for (Constructor<?> c : AgreementRuntime.class.getDeclaredConstructors()) {
            for (Class<?> p : c.getParameterTypes()) {
                assertFalse(looksLikeTemplate(p),
                        "AgreementRuntime 不允许依赖模板相关类型：" + p.getName());
            }
        }
        for (Field f : AgreementRuntime.class.getDeclaredFields()) {
            assertFalse(looksLikeTemplate(f.getType()),
                    "AgreementRuntime 不允许持有模板相关字段：" + f.getName());
        }
        for (Method m : AgreementRuntime.class.getDeclaredMethods()) {
            assertFalse(looksLikeTemplate(m.getReturnType()),
                    "AgreementRuntime 的方法不允许返回模板相关类型：" + m.getName());
            for (Class<?> p : m.getParameterTypes()) {
                assertFalse(looksLikeTemplate(p),
                        "AgreementRuntime 的方法不允许接收模板相关类型：" + m.getName());
            }
        }

        // 行为断言：模板里有「账期 60 天」这一项，但本版没约定 ⇒ 结果必须是"未约定"，绝不能取到 60
        AgreementResolvedSettings resolved = resolvedWith(List.of(), List.of(mode("DROP_SHIP")));
        AgreementSettingValue creditDays = resolved.get("AR_CREDIT_DAYS");
        assertEquals(AgreementSettingValue.State.UNDECLARED, creditDays.getState());
        assertEquals(null, creditDays.rawValue());

        // 整条 resolve 调用路径上，模板三张表一次都没被查询
        verify(templateSettingMapper, never()).selectList(any());
        verify(templateTermMapper, never()).selectList(any());
        verify(templateNarrativeMapper, never()).selectList(any());
    }

    // ══════════════════════ 造数据 ══════════════════════

    private static boolean looksLikeTemplate(Class<?> type) {
        return type.getSimpleName().toLowerCase().contains("template");
    }

    private void stubAgreement() {
        when(agreementMapper.selectList(any())).thenReturn(List.of(agreement("XY-20260922-0001")));
    }

    private void stubDefs() {
        when(settingDefMapper.selectList(any())).thenReturn(defs());
    }

    /** 一版协议 + 一个已约定的字段（默认走通路径）。 */
    private AgreementResolvedSettings resolvedWith(List<AgreementSetting> settings,
                                                   List<AgreementFulfillmentMode> modes) {
        stubAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        stubDefs();
        when(settingMapper.selectList(any())).thenReturn(settings);
        when(fulfillmentModeMapper.selectList(any())).thenReturn(modes);
        return runtime.resolve(11L, TENANT_A, 22L, TENANT_B, LocalDateTime.of(2026, 9, 22, 10, 0));
    }

    private static Agreement agreement(String no) {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setTenantId(0L);
        a.setAgreementNo(no);
        a.setAgreementType("DISTRIBUTION");
        a.setPartyAId(11L);
        a.setPartyATenantId(TENANT_A);
        a.setPartyBId(22L);
        a.setPartyBTenantId(TENANT_B);
        a.setStatus(AgreementStatus.ACTIVE.getCode());
        return a;
    }

    private static AgreementVersion version(Long id, int no, Integer status,
                                            LocalDateTime from, LocalDateTime to) {
        AgreementVersion v = new AgreementVersion();
        v.setId(id);
        v.setAgreementId(1L);
        v.setVersionNo(no);
        v.setStatus(status);
        v.setEffectiveFrom(from);
        v.setEffectiveTo(to);
        v.setSnapshotJson("{\"terms\":[]}");
        return v;
    }

    private static List<AgreementSettingDef> defs() {
        return List.of(
                def("AR_CREDIT_DAYS", "账期天数", "NUMBER", "AR_DUE_DATE"),
                def("AR_CREDIT_PROVIDER", "账期方向（谁给谁）", "ENUM", "AR_DUE_DATE"),
                def("COMMISSION_RATE", "佣金率", "NUMBER", "SETTLEMENT_SPLIT"),
                def("FULFILLMENT_MODES", "履约方式集合", "ENUM", "ORDER_ROUTING"));
    }

    private static AgreementSettingDef def(String key, String label, String type, String consumer) {
        AgreementSettingDef d = new AgreementSettingDef();
        d.setId((long) key.hashCode());
        d.setTenantId(0L);
        d.setSettingKey(key);
        d.setLabel(label);
        d.setValueType(type);
        d.setConsumerPoint(consumer);
        d.setRequired(false);
        d.setStatus(1);
        if ("FULFILLMENT_MODES".equals(key)) {
            d.setOptions("DROP_SHIP,TRANSIT_STOCK,PICKUP,LOCAL_STOCK");
        }
        return d;
    }

    private static AgreementSetting numberSetting(String key, int value) {
        AgreementSetting s = new AgreementSetting();
        s.setTenantId(0L);
        s.setAgreementId(1L);
        s.setVersionId(101L);
        s.setSettingKey(key);
        s.setValueType("NUMBER");
        s.setValueNumber(new java.math.BigDecimal(value));
        return s;
    }

    private static AgreementSetting textSetting(String key, String value) {
        AgreementSetting s = new AgreementSetting();
        s.setTenantId(0L);
        s.setAgreementId(1L);
        s.setVersionId(101L);
        s.setSettingKey(key);
        s.setValueType("TEXT");
        s.setValueText(value);
        return s;
    }

    private static AgreementFulfillmentMode mode(String mode) {
        AgreementFulfillmentMode m = new AgreementFulfillmentMode();
        m.setTenantId(0L);
        m.setAgreementId(1L);
        m.setVersionId(101L);
        m.setMode(mode);
        return m;
    }
}
