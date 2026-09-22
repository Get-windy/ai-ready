package cn.aiedge.agreement;

import cn.aiedge.agreement.domain.AgreementRuntime;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.mapper.AgreementFulfillmentModeMapper;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementSettingDefMapper;
import cn.aiedge.agreement.mapper.AgreementSettingMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.agreement.provider.AgreementCreditTermProvider;
import cn.aiedge.base.credit.CreditTermQuery;
import cn.aiedge.base.credit.CreditTermResult;
import cn.aiedge.base.credit.SettlementType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@code AgreementCreditTermProvider} —— 「协议结算口径 → 应收/应付到期日」的协议侧实现。
 *
 * <p>钉死<b>两层判定</b>与四态分流（DOMAIN-MODEL §13.3 补充口径 3）：</p>
 * <ol>
 *   <li><b>第 1 层先取「结算方式」</b>：没约定 / 取值认不出 ⇒ {@code NO_AGREEMENT}
 *       （走无协议无特定客户模式 = 现款现结）；</li>
 *   <li><b>第 2 层才决定要不要问账期天数</b>：现款现货（三种）与滚结 ⇒ {@code NO_CREDIT_TERM}
 *       （<b>压根不问天数</b>）；只有「账期结算」才去取天数与方向 ——
 *       这是用户第 4 轮点名的毛病："约定现金也照样报未约定账期"；</li>
 *   <li><b>账期结算缺天数 / 方向 ⇒ {@code UNDECLARED}</b>（条件必填缺失，消费方据此拒单）；</li>
 *   <li><b>未约定绝不回落默认值</b>：{@code getCreditDays()} 恒为 null，也不是 0；</li>
 *   <li><b>按业务日期取版本</b>：业务时点落不到任何版本区间 ⇒ 无适用协议
 *       （证明"读的是业务时点那一版"，不是"当前版本"）；</li>
 *   <li><b>账期有向</b>：方向与债权方对不上时归"未约定"（挂人工），不归"无适用协议"
 *       （否则会静默丢掉双方表达过的方向意思）。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgreementCreditTermProviderTest {

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

    /** 卖方（债权方）租户与主体：本笔交易的收款方。 */
    private static final Long SELLER_TENANT = 2L;
    private static final Long SELLER_PARTY = 11L;

    /** 买方（债务方）租户与主体：本笔交易的付款方（应收里的客户）。 */
    private static final Long BUYER_TENANT = 3L;
    private static final Long BUYER_PARTY = 22L;

    /** 业务日期：刻意取一个"过去"的日子，用来证明取数用的是业务时点而非"现在"。 */
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 1, 31);

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

    @InjectMocks
    private AgreementRuntime runtime;

    private AgreementCreditTermProvider provider() {
        return new AgreementCreditTermProvider(runtime, agreementMapper);
    }

    // ══════════════════════ ① 账期结算 + 天数 + 方向 ⇒ AGREED ══════════════════════

    @Test
    @DisplayName("约定账期结算 45 天 ⇒ AGREED 45，到期日 = 业务日期 + 45（跨月：1/31 + 45 = 3/17）")
    void agreedReturnsDaysAndDueDate() {
        stubActiveAgreement();
        // 版本区间刻意只覆盖业务日期那一天附近、**不覆盖"现在"**：
        // 若实现偷偷用了"当前时间"取版本，这里会取不到版本 ⇒ 断言就会红。
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        when(settingMapper.selectList(any())).thenReturn(List.of(
                settlement("CREDIT"),
                numberSetting(AgreementCreditTermProvider.KEY_CREDIT_DAYS, new BigDecimal("45")),
                textSetting(AgreementCreditTermProvider.KEY_CREDIT_PROVIDER, "PARTY_A")));

        CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isAgreed(), "有约定应返回 AGREED：" + result);
        assertEquals(45, result.getCreditDays());
        assertEquals(LocalDate.of(2026, 3, 17), result.dueDateFrom(BUSINESS_DATE),
                "到期日 = 业务日期 + 45 天（自然日历进位，跨月）");
        assertEquals(1L, result.getAgreementId(), "结果必须带上「是哪一份协议」");
        assertEquals(101L, result.getVersionId(), "结果必须带上「是哪一版」");
        assertEquals(1, result.getVersionNo());
    }

    @Test
    @DisplayName("月末 + 跨年：12/31 + 30 = 次年 1/30")
    void dueDateCrossesYearBoundary() {
        stubActiveAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        when(settingMapper.selectList(any())).thenReturn(List.of(
                settlement("CREDIT"),
                numberSetting(AgreementCreditTermProvider.KEY_CREDIT_DAYS, new BigDecimal("30")),
                textSetting(AgreementCreditTermProvider.KEY_CREDIT_PROVIDER, "PARTY_A")));

        CreditTermResult result = provider().resolve(
                query(SELLER_PARTY, BUYER_PARTY, LocalDate.of(2026, 12, 31)));

        assertTrue(result.isAgreed());
        assertEquals(LocalDate.of(2027, 1, 30), result.dueDateFrom(LocalDate.of(2026, 12, 31)));
    }

    @Test
    @DisplayName("只拿得到债务方（应收=客户）时：账期提供方在协议另一端 ⇒ 仍判定为已约定")
    void agreedWhenOnlyDebtorSideKnown() {
        stubActiveAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        when(settingMapper.selectList(any())).thenReturn(List.of(
                settlement("CREDIT"),
                numberSetting(AgreementCreditTermProvider.KEY_CREDIT_DAYS, new BigDecimal("45")),
                // 甲方(11) 是卖方/债权方 ⇒ 账期由甲方提供
                textSetting(AgreementCreditTermProvider.KEY_CREDIT_PROVIDER, "PARTY_A")));

        // 债权方（卖方主体）未知 —— 本仓现状就是如此（没有"本租户主体"登记）
        CreditTermResult result = provider().resolve(query(null, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isAgreed(), "债务方在乙方 ⇒ 债权方在甲方 ⇒ 与 PARTY_A 一致：" + result);
        assertEquals(45, result.getCreditDays());
    }

    // ══════════════════════ ② 第 1 层：结算方式未约定 / 认不出 ⇒ NO_AGREEMENT ══════════════════════

    @Test
    @DisplayName("没约定「结算方式」⇒ NO_AGREEMENT（走无协议无特定客户模式），即使天数与方向都约定了")
    void undeclaredSettlementTypeFallsBackToNoAgreement() {
        stubActiveAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        // 刻意只给天数与方向、**不给结算方式**
        when(settingMapper.selectList(any())).thenReturn(List.of(
                numberSetting(AgreementCreditTermProvider.KEY_CREDIT_DAYS, new BigDecimal("45")),
                textSetting(AgreementCreditTermProvider.KEY_CREDIT_PROVIDER, "PARTY_A")));

        CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isNoAgreement(), "没有结算方式就判不了第 2 层 ⇒ 走无协议模式：" + result);
        assertNull(result.getCreditDays(), "无适用协议不得带出任何天数");
        assertTrue(result.getReason().contains("结算方式"), "原因必须点出缺的是「结算方式」：" + result.getReason());
    }

    @Test
    @DisplayName("「结算方式」取值认不出 ⇒ NO_AGREEMENT（绝不就此给双方记一笔赊账）")
    void unknownSettlementCodeFallsBackToNoAgreement() {
        stubActiveAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        when(settingMapper.selectList(any())).thenReturn(List.of(
                settlement("BY_CRYPTO"),
                numberSetting(AgreementCreditTermProvider.KEY_CREDIT_DAYS, new BigDecimal("45"))));

        CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isNoAgreement(), "认不出的取值是数据问题，只能落到最低风险的现款现结：" + result);
        assertNull(result.getCreditDays());
    }

    @Test
    @DisplayName("有协议但业务时点没有生效版本 ⇒ NO_AGREEMENT（含「协议已到期」）")
    void noVersionInBusinessRange() {
        stubActiveAgreement();
        // 版本 2026-01-01 ~ 2026-02-01：业务日期 6/30 落在区间外（= 协议已到期）
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0))));
        when(settingMapper.selectList(any())).thenReturn(List.of());

        CreditTermResult result = provider().resolve(
                query(SELLER_PARTY, BUYER_PARTY, LocalDate.of(2026, 6, 30)));

        assertTrue(result.isNoAgreement(), "取不到「当时生效的那一版」应与「无适用协议」同处理：" + result);
        assertNull(result.getCreditDays(), "无适用协议不得带出任何天数");
    }

    @Test
    @DisplayName("这对主体/租户之间根本没有生效协议 ⇒ NO_AGREEMENT")
    void noAgreementForTenantPairAtAll() {
        when(agreementMapper.selectList(any())).thenReturn(List.of());

        CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isNoAgreement(), result.toString());
        assertNull(result.getCreditDays());
    }

    // ══════════════════════ ③ 第 2 层：现款 / 滚结 ⇒ NO_CREDIT_TERM（压根不问天数） ══════════════════════

    @Test
    @DisplayName("结算方式 = 现款现结 且**没填账期天数** ⇒ NO_CREDIT_TERM，绝不报「未约定账期」")
    void cashSettlementDoesNotAskForCreditDays() {
        stubActiveAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        // 只约定了结算方式，**没有** AR_CREDIT_DAYS —— 旧实现到这一步会报"未约定账期"
        when(settingMapper.selectList(any())).thenReturn(List.of(settlement("CASH_SPOT")));

        CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isNoCreditTerm(), "约定现金 ⇒ 本笔没有账期这回事（正常结论）：" + result);
        assertEquals(SettlementType.CASH_SPOT, result.getSettlementType());
        assertNull(result.getCreditDays(), "无账期不得带出天数");
        assertTrue(result.getReason().contains("现款"), "原因要带上具体是哪一种现款：" + result.getReason());
        assertEquals(BUSINESS_DATE, result.dueDateFrom(BUSINESS_DATE), "无账期 ⇒ 到期日 = 业务日（当天结清）");
    }

    @Test
    @DisplayName("四种无账期的结算方式（先款后货 / 现款现结 / 货到付款 / 滚结）都返回 NO_CREDIT_TERM，并保留各自是哪一种")
    void allNonCreditSettlementsReturnNoCreditTerm() {
        for (String code : List.of("CASH_PREPAY", "CASH_SPOT", "CASH_ON_DELIVERY", "ROLLING")) {
            stubActiveAgreement();
            when(versionMapper.selectList(any())).thenReturn(List.of(
                    version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                            LocalDateTime.of(2026, 1, 1, 0, 0), null)));
            when(settingMapper.selectList(any())).thenReturn(List.of(settlement(code)));

            CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

            assertTrue(result.isNoCreditTerm(), code + " 应当无账期：" + result);
            assertEquals(SettlementType.parseOrNull(code), result.getSettlementType(),
                    code + " 必须原样带出，不许压成一个笼统的「现金」（压了会丢掉资金与货物先后的区别）");
        }
    }

    // ══════════════════════ ④ 账期结算缺项 ⇒ UNDECLARED（消费方据此拒单） ══════════════════════

    @Test
    @DisplayName("结算方式 = 账期结算，但没填账期天数 ⇒ UNDECLARED（条件必填缺失）")
    void undeclaredWhenDaysMissing() {
        stubActiveAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        // 约定了账期结算与方向，没约定天数
        when(settingMapper.selectList(any())).thenReturn(List.of(
                settlement("CREDIT"),
                textSetting(AgreementCreditTermProvider.KEY_CREDIT_PROVIDER, "PARTY_A")));

        CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isUndeclared(), result.toString());
        assertNull(result.getCreditDays(), "未约定时不得回落到 30 天这类默认值");
        assertTrue(result.getReason().contains("未约定"), "原因必须说清是「未约定」：" + result.getReason());
        assertTrue(result.getReason().contains("账期天数"), "原因必须指出缺的是哪一个字段：" + result.getReason());
        assertThrows(IllegalStateException.class, () -> result.dueDateFrom(BUSINESS_DATE),
                "缺项状态下推算到期日是编程错误，必须直接报错 —— 正确处置是拒单");
    }

    @Test
    @DisplayName("结算方式 = 账期结算，约定了天数但没约定方向 ⇒ UNDECLARED（账期有向，方向不明不能执行）")
    void undeclaredWhenDirectionMissing() {
        stubActiveAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        when(settingMapper.selectList(any())).thenReturn(List.of(
                settlement("CREDIT"),
                numberSetting(AgreementCreditTermProvider.KEY_CREDIT_DAYS, new BigDecimal("45"))));

        CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isUndeclared(), result.toString());
        assertTrue(result.getReason().contains("账期方向"), result.getReason());
    }

    @Test
    @DisplayName("方向对不上（协议约定由买方给账期）⇒ UNDECLARED，而不是「无适用协议」")
    void undeclaredWhenDirectionMismatch() {
        stubActiveAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        when(settingMapper.selectList(any())).thenReturn(List.of(
                settlement("CREDIT"),
                numberSetting(AgreementCreditTermProvider.KEY_CREDIT_DAYS, new BigDecimal("45")),
                // 乙方(22) 才是买方；协议却约定由乙方提供账期 ⇒ 与"卖方给买方账期"方向相反
                textSetting(AgreementCreditTermProvider.KEY_CREDIT_PROVIDER, "PARTY_B")));

        CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isUndeclared(), "方向对不上必须归「未约定」（挂人工/拒单）："
                + "归「无适用协议」会静默丢掉双方表达过的方向意思：" + result);
        assertNull(result.getCreditDays());
        assertTrue(result.getReason().contains("方向"), result.getReason());
    }

    @Test
    @DisplayName("天数不是有效的非负整数（45.5）⇒ UNDECLARED，不四舍五入")
    void undeclaredWhenDaysNotWholeNumber() {
        stubActiveAgreement();
        when(versionMapper.selectList(any())).thenReturn(List.of(
                version(101L, 1, AgreementVersionStatus.ACTIVE.getCode(),
                        LocalDateTime.of(2026, 1, 1, 0, 0), null)));
        when(settingMapper.selectList(any())).thenReturn(List.of(
                settlement("CREDIT"),
                numberSetting(AgreementCreditTermProvider.KEY_CREDIT_DAYS, new BigDecimal("45.5")),
                textSetting(AgreementCreditTermProvider.KEY_CREDIT_PROVIDER, "PARTY_A")));

        CreditTermResult result = provider().resolve(query(SELLER_PARTY, BUYER_PARTY, BUSINESS_DATE));

        assertTrue(result.isUndeclared(), result.toString());
        assertNull(result.getCreditDays());
    }

    // ══════════════════════ 辅助 ══════════════════════

    /** 只给已知那一端主体的查询（另一端留空，模拟本仓"拿不到本租户主体"的现状）。 */
    private static CreditTermQuery query(Long sellerPartyId, Long buyerPartyId, LocalDate businessDate) {
        return CreditTermQuery.of(SELLER_TENANT, sellerPartyId, BUYER_TENANT, buyerPartyId,
                businessDate, "生成应收到期日");
    }

    /** 一对生效协议 + 字段字典。 */
    private void stubActiveAgreement() {
        when(agreementMapper.selectList(any())).thenReturn(List.of(agreement()));
        when(agreementMapper.selectById(1L)).thenReturn(agreement());
        when(settingDefMapper.selectList(any())).thenReturn(defs());
        when(fulfillmentModeMapper.selectList(any())).thenReturn(List.of());
    }

    private static Agreement agreement() {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setTenantId(0L);
        a.setAgreementNo("AG-E2E-UNIT");
        a.setAgreementType("DISTRIBUTION");
        a.setPartyAId(SELLER_PARTY);
        a.setPartyATenantId(SELLER_TENANT);
        a.setPartyBId(BUYER_PARTY);
        a.setPartyBTenantId(BUYER_TENANT);
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
                def(AgreementCreditTermProvider.KEY_SETTLEMENT_TYPE, "结算方式", "ENUM"),
                def(AgreementCreditTermProvider.KEY_CREDIT_DAYS, "账期天数", "NUMBER"),
                def(AgreementCreditTermProvider.KEY_CREDIT_PROVIDER, "账期方向（谁给谁）", "ENUM"));
    }

    private static AgreementSettingDef def(String key, String label, String type) {
        AgreementSettingDef d = new AgreementSettingDef();
        d.setId((long) key.hashCode());
        d.setTenantId(0L);
        d.setSettingKey(key);
        d.setLabel(label);
        d.setValueType(type);
        d.setConsumerPoint("AR_DUE_DATE");
        d.setRequired(false);
        d.setStatus(1);
        if (AgreementCreditTermProvider.KEY_CREDIT_PROVIDER.equals(key)) {
            d.setOptions("PARTY_A,PARTY_B");
        }
        if (AgreementCreditTermProvider.KEY_SETTLEMENT_TYPE.equals(key)) {
            d.setOptions("CASH_PREPAY,CASH_SPOT,CASH_ON_DELIVERY,CREDIT,ROLLING");
        }
        return d;
    }

    /** 本版约定的结算方式（存的是文本列，取值就是 SettlementType 的编码）。 */
    private static AgreementSetting settlement(String code) {
        return textSetting(AgreementCreditTermProvider.KEY_SETTLEMENT_TYPE, code);
    }

    private static AgreementSetting numberSetting(String key, BigDecimal value) {
        AgreementSetting s = new AgreementSetting();
        s.setTenantId(0L);
        s.setAgreementId(1L);
        s.setVersionId(101L);
        s.setSettingKey(key);
        s.setValueType("NUMBER");
        s.setValueNumber(value);
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
}
