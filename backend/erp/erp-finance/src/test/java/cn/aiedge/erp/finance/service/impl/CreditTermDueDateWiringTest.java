package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.base.credit.CreditTermQuery;
import cn.aiedge.base.credit.CreditTermResult;
import cn.aiedge.base.credit.SettlementType;
import cn.aiedge.base.credit.TradeCreditTermProvider;
import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PartySettlementProfile;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.PartySettlementProfileMapper;
import cn.aiedge.erp.finance.service.PayableService;
import cn.aiedge.erp.finance.service.ReceivableService;
import cn.aiedge.erp.finance.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 「结算口径 → 应收/应付到期日」在<b>财务侧接线</b>上的行为口径（DOMAIN-MODEL §7.7 附 + §13.3）。
 *
 * <p>纯单元测试（不起 Spring、不连库），钉死三层优先级链与唯一的拒单出口：</p>
 * <ol>
 *   <li><b>无实现时零行为变化</b>：协议模块没部署时，到期日与请求里带来的值<b>逐字一致</b>；</li>
 *   <li><b>身份不足 ⇒ 现款现结</b>（到期日 = 业务日），不编主体、不编租户、不加默认账期；</li>
 *   <li><b>无适用协议 ⇒ 现款现结</b>（不是"沿用调用方带来的 30 天"——那已按裁定移除）；</li>
 *   <li><b>跨租户</b>：账期约定 ⇒ 业务日 + N；现款 / 滚结 ⇒ 业务日（<b>且不报"未约定账期"</b>）；</li>
 *   <li><b>租户内</b>：走 {@code biz_party} 结算档案（现结 ⇒ 业务日；有账期 ⇒ 业务日 + 档案天数）；</li>
 *   <li><b>账期结算缺天数/方向 ⇒ 拒单</b>（唯一会拦单据的情形）；其余异常一律降级放行；</li>
 *   <li><b>起算基准是业务日期</b>（含跨月/月末/跨年），不是"当前时间"。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditTermDueDateWiringTest {

    private static final Long SELF_TENANT = 1L;
    private static final Long COUNTERPARTY_TENANT = 2L;
    private static final String CUSTOMER_PARTY = "22";
    private static final String SUPPLIER_PARTY = "11";

    /** 业务日期刻意是"过去"，用来说明起算基准不是今天。 */
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 1, 31);

    /** 调用方带来的到期日：刻意用一个一眼能认出的哨兵值（协议不介入时才应当看到它）。 */
    private static final LocalDate LEGACY_DUE_DATE = LocalDate.of(2030, 12, 31);

    @Mock
    private VoucherService voucherService;
    @Mock
    private ReceivableService receivableService;
    @Mock
    private PayableService payableService;
    @Mock
    private AccountSubjectMapper accountSubjectMapper;
    @Mock
    private ObjectProvider<TradeCreditTermProvider> providerHolder;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private PartySettlementProfileMapper partySettlementProfileMapper;
    @Mock
    private TradeCreditTermProvider provider;

    private BusinessAccountingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BusinessAccountingServiceImpl(voucherService, receivableService, payableService,
                accountSubjectMapper, providerHolder, securityContext, partySettlementProfileMapper);
        when(securityContext.getCurrentTenantId()).thenReturn(SELF_TENANT);
        when(receivableService.create(any())).thenAnswer(inv -> inv.getArgument(0));
        when(payableService.create(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ══════════════════════ ① 无实现 ⇒ 零行为变化 ══════════════════════

    @Test
    @DisplayName("协议模块未部署（取不到实现）⇒ 应收/应付到期日与接入前逐字一致")
    void noImplementationKeepsBehaviourUnchanged() {
        when(providerHolder.getIfAvailable()).thenReturn(null);

        assertEquals(LEGACY_DUE_DATE, createReceivable(sentinelRequest()).getDueDate(),
                "无实现时应收到期日必须原样透传");
        assertEquals(LEGACY_DUE_DATE, createPayable(sentinelRequest()).getDueDate(),
                "无实现时应付到期日必须原样透传");

        // 请求里本来就没给到期日 ⇒ 仍然为空（不许凭空补一个默认值）
        BusinessAccountingRequest blank = sentinelRequest();
        blank.setDueDate(null);
        assertNull(createReceivable(blank).getDueDate());

        // 无实现时连档案也不该去读（协议模块都没部署，财务侧不做额外取数）
        org.mockito.Mockito.verify(partySettlementProfileMapper, times(0)).selectByPartyId(anyLong(), anyLong());
    }

    // ══════════════════════ ② 身份不足 ⇒ 现款现结 ══════════════════════

    @Test
    @DisplayName("对方租户取不到 ⇒ 现款现结（到期日 = 业务日），不沿用调用方带来的哨兵值")
    void missingCounterpartyTenantFallsBackToCashOnSpot() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);

        BusinessAccountingRequest noTenant = sentinelRequest();
        noTenant.setCounterpartyTenantId(null);

        assertEquals(BUSINESS_DATE, createReceivable(noTenant).getDueDate(),
                "身份不足 ⇒ 判不了跨/内租户、也读不到档案 ⇒ 落到最低风险的现款现结（业务日）");
    }

    @Test
    @DisplayName("业务日期缺失 ⇒ 无处起算，只能沿用调用方带来的既有值（不凭空造日期）")
    void missingBusinessDateKeepsLegacyDueDate() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);

        BusinessAccountingRequest noBizDate = sentinelRequest();
        noBizDate.setBusinessDate(null);

        assertEquals(LEGACY_DUE_DATE, createReceivable(noBizDate).getDueDate(),
                "连业务日期都没有，现款现结也算不出到期日 ⇒ 如实沿用既有值");
    }

    // ══════════════════════ ③ 无适用协议 ⇒ 现款现结 ══════════════════════

    @Test
    @DisplayName("无适用协议（无协议 / 已到期 / 未约定结算方式）⇒ 现款现结，到期日 = 业务日")
    void noAgreementFallsBackToCashOnSpot() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenReturn(CreditTermResult.noAgreement("该时点没有生效协议"));

        assertEquals(BUSINESS_DATE, createReceivable(sentinelRequest()).getDueDate(),
                "无协议无特定客户模式的默认口径是现款现结 ⇒ 到期日 = 业务日");
        assertEquals(BUSINESS_DATE, createPayable(sentinelRequest()).getDueDate());
    }

    // ══════════════════════ ④ 跨租户 + 约定账期 ⇒ 业务日 + N ══════════════════════

    @Test
    @DisplayName("约定账期 45 天 ⇒ 应收到期日 = 业务日期 + 45（跨月：1/31 + 45 = 3/17），且不采用传入的既有值")
    void agreedOverridesDueDateByBusinessDate() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenReturn(CreditTermResult.agreed(45, 1L, 101L, 1));

        ReceivableDTO created = createReceivable(sentinelRequest());

        assertEquals(LocalDate.of(2026, 3, 17), created.getDueDate(),
                "到期日 = 业务日期(1/31) + 45 天 = 3/17；不得使用传入的 2030-12-31，也不得用「今天」");
    }

    @Test
    @DisplayName("月末 + 跨年：12/31 + 30 = 次年 1/30")
    void agreedDueDateCrossesYearBoundary() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenReturn(CreditTermResult.agreed(30, 1L, 101L, 1));

        BusinessAccountingRequest request = sentinelRequest();
        request.setBusinessDate(LocalDate.of(2026, 12, 31));

        assertEquals(LocalDate.of(2027, 1, 30), createReceivable(request).getDueDate());
    }

    // ══════════════════════ ⑤ 跨租户 + 现款 / 滚结 ⇒ 本笔无账期（不报"未约定"） ══════════════════════

    @Test
    @DisplayName("协议约定结算方式 = 现款现结 ⇒ 到期日 = 业务日，且**不**被当成「未约定账期」拦下")
    void cashSettlementMeansNoCreditTerm() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenReturn(CreditTermResult.noCreditTerm(
                SettlementType.CASH_SPOT, "本协议约定的结算方式是「现款现货 · 现款现结」，本笔没有账期这回事",
                1L, 101L, 1));

        ReceivableDTO receivable = assertDoesNotThrow(() -> createReceivable(sentinelRequest()),
                "约定现金 ⇒ 压根没有账期这回事，绝不许报「未约定账期」更不许拒单");
        assertEquals(BUSINESS_DATE, receivable.getDueDate(), "无账期 ⇒ 到期日 = 业务日（当天结清）");
    }

    @Test
    @DisplayName("协议约定结算方式 = 滚结 ⇒ 到期日 = 业务日（同样无账期）")
    void rollingSettlementMeansNoCreditTerm() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenReturn(CreditTermResult.noCreditTerm(
                SettlementType.ROLLING, "本协议约定的结算方式是「滚结」", 1L, 101L, 1));

        assertEquals(BUSINESS_DATE, createPayable(sentinelRequest()).getDueDate());
    }

    // ══════════════════════ ⑥ 账期结算缺项 ⇒ 拒单（唯一的拦单情形） ══════════════════════

    @Test
    @DisplayName("协议约定「账期结算」却缺天数/方向 ⇒ **拒单**（抛业务异常），不是留空挂人工")
    void undeclaredRejectsTheDocument() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenReturn(CreditTermResult.undeclared(
                "本协议未约定「账期天数」，无法生成应收到期日（结算方式为「账期结算」时账期天数是必填项）",
                1L, 101L, 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> createReceivable(sentinelRequest()),
                "§13.3 铁律②：账期天数是条件必填，缺 ⇒ 提交不了（不是留空挂人工）");
        assertTrue(ex.getMessage().contains("不能提交"), ex.getMessage());
        assertTrue(ex.getMessage().contains("账期天数") || ex.getMessage().contains("账期方向"),
                "提示要说清缺的是哪一项：" + ex.getMessage());

        assertThrows(BusinessException.class, () -> createPayable(sentinelRequest()),
                "应付侧口径完全对称");
    }

    // ══════════════════════ ⑦ 租户内 ⇒ 走往来单位结算档案 ══════════════════════

    @Test
    @DisplayName("租户内交易：档案为「现结」⇒ 到期日 = 业务日，且**不查协议**")
    void intraTenantCashProfileMeansDueOnBusinessDate() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(partySettlementProfileMapper.selectByPartyId(22L, SELF_TENANT))
                .thenReturn(profile(0, 0, 30));

        BusinessAccountingRequest request = sentinelRequest();
        request.setCounterpartyTenantId(SELF_TENANT);

        assertEquals(BUSINESS_DATE, createReceivable(request).getDueDate(),
                "档案是「现结」⇒ 无账期、到期日 = 业务日");
        verify(provider, times(0)).resolve(any());
    }

    @Test
    @DisplayName("租户内交易：档案有账期 ⇒ 应收看 credit_days、到期日 = 业务日 + 30")
    void intraTenantCreditDaysAppliesToReceivable() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(partySettlementProfileMapper.selectByPartyId(22L, SELF_TENANT))
                .thenReturn(profile(1, 30, 45));

        BusinessAccountingRequest request = sentinelRequest();
        request.setCounterpartyTenantId(SELF_TENANT);

        assertEquals(LocalDate.of(2026, 3, 2), createReceivable(request).getDueDate(),
                "应收方向用 credit_days(30)：1/31 + 30 = 3/2");
    }

    @Test
    @DisplayName("租户内交易：档案有账期 ⇒ 应付看 payment_days（与应收各用各的天数）")
    void intraTenantPaymentDaysAppliesToPayable() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(partySettlementProfileMapper.selectByPartyId(11L, SELF_TENANT))
                .thenReturn(profile(1, 30, 45));

        BusinessAccountingRequest request = sentinelRequest();
        request.setCounterpartyTenantId(SELF_TENANT);

        assertEquals(LocalDate.of(2026, 3, 17), createPayable(request).getDueDate(),
                "应付方向用 payment_days(45)：1/31 + 45 = 3/17");
    }

    @Test
    @DisplayName("租户内交易：档案写了「非现结」却没填有效天数 ⇒ 现款现结，不编一个默认天数")
    void intraTenantMissingDaysFallsBackToCashOnSpot() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(partySettlementProfileMapper.selectByPartyId(22L, SELF_TENANT))
                .thenReturn(profile(1, 0, 0));

        BusinessAccountingRequest request = sentinelRequest();
        request.setCounterpartyTenantId(SELF_TENANT);

        assertEquals(BUSINESS_DATE, createReceivable(request).getDueDate(),
                "档案没填天数不许编默认值（平台不替租户定信用政策）⇒ 落到最低风险的现款现结");
    }

    @Test
    @DisplayName("租户内交易：档案查不到 ⇒ 现款现结（主数据缺失不许阻断单据）")
    void intraTenantProfileMissingFallsBackToCashOnSpot() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(partySettlementProfileMapper.selectByPartyId(22L, SELF_TENANT)).thenReturn(null);

        BusinessAccountingRequest request = sentinelRequest();
        request.setCounterpartyTenantId(SELF_TENANT);

        assertEquals(BUSINESS_DATE,
                assertDoesNotThrow(() -> createReceivable(request)).getDueDate());
    }

    @Test
    @DisplayName("租户内交易：读档案抛异常 ⇒ 现款现结、不阻断单据")
    void intraTenantProfileFailureDoesNotBlock() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(partySettlementProfileMapper.selectByPartyId(22L, SELF_TENANT))
                .thenThrow(new IllegalStateException("模拟主数据取数异常"));

        BusinessAccountingRequest request = sentinelRequest();
        request.setCounterpartyTenantId(SELF_TENANT);

        assertEquals(BUSINESS_DATE,
                assertDoesNotThrow(() -> createReceivable(request)).getDueDate());
    }

    // ══════════════════════ ⑧ 协议侧异常 ⇒ 降级放行 ══════════════════════

    @Test
    @DisplayName("协议侧取数抛异常（例如同一对主体并存两份生效协议）⇒ 降级为现款现结，不阻断单据")
    void providerFailureDoesNotBlock() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenThrow(new IllegalStateException("在租户对之间找到了 2 份生效协议"));

        assertEquals(BUSINESS_DATE,
                assertDoesNotThrow(() -> createReceivable(sentinelRequest())).getDueDate(),
                "取数异常是「接不上」，不是「双方没约好」 ⇒ 按现款现结降级，绝不拦单");
    }

    // ══════════════════════ ⑨ 查询入参的买卖两端 ══════════════════════

    @Test
    @DisplayName("查询入参用的是「本端租户 + 对方主体/租户 + 业务日期」——应收的对方是买方（债务方）、应付的对方是卖方")
    void queryCarriesCorrectSides() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenReturn(CreditTermResult.noAgreement("检查入参"));

        ArgumentCaptor<CreditTermQuery> captor = ArgumentCaptor.forClass(CreditTermQuery.class);

        createReceivable(sentinelRequest());
        createPayable(sentinelRequest());

        org.mockito.Mockito.verify(provider, times(2)).resolve(captor.capture());
        CreditTermQuery ar = captor.getAllValues().get(0);
        CreditTermQuery ap = captor.getAllValues().get(1);

        // 应收：本端(1) 是卖方/债权方，客户(22) 是买方/债务方
        assertEquals(SELF_TENANT, ar.getSellerTenantId());
        assertNull(ar.getSellerPartyId(), "本仓没有「本租户主体」登记，不得硬编一个");
        assertEquals(COUNTERPARTY_TENANT, ar.getBuyerTenantId());
        assertEquals(22L, ar.getBuyerPartyId(), "customer_id 就是 biz_party.id");
        assertEquals(BUSINESS_DATE, ar.getBusinessDate());

        // 应付：供应商(11) 是卖方/债权方，本端(1) 是买方/债务方
        assertEquals(COUNTERPARTY_TENANT, ap.getSellerTenantId());
        assertEquals(11L, ap.getSellerPartyId(), "supplier_id 就是 biz_party.id");
        assertEquals(SELF_TENANT, ap.getBuyerTenantId());
        assertNull(ap.getBuyerPartyId(), "本仓没有「本租户主体」登记，不得硬编一个");
    }

    // ══════════════════════ ⑩ 提交前预检 ══════════════════════

    @Test
    @DisplayName("预检只读：算出到期日但**不落库**（不创建应收/应付）")
    void precheckIsReadOnly() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenReturn(CreditTermResult.agreed(45, 1L, 101L, 1));

        assertEquals(LocalDate.of(2026, 3, 17), service.precheckReceivableDueDate(sentinelRequest()));

        verify(receivableService, times(0)).create(any());
        verify(payableService, times(0)).create(any());
    }

    @Test
    @DisplayName("预检与记账用同一套口径：缺项时预检也抛（这样单据流程才能在记账 try 之外拦住提交）")
    void precheckUsesTheSameChainAsBooking() {
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        when(provider.resolve(any())).thenReturn(
                CreditTermResult.undeclared("本协议未约定「账期天数」", 1L, 101L, 1));

        assertThrows(BusinessException.class, () -> service.precheckReceivableDueDate(sentinelRequest()));
        assertThrows(BusinessException.class, () -> service.precheckPayableDueDate(sentinelRequest()));
    }

    // ══════════════════════ 辅助 ══════════════════════

    /** 调用方（销售/采购集成层）当前会传的东西 + 本次新加的两个身份字段。 */
    private static BusinessAccountingRequest sentinelRequest() {
        BusinessAccountingRequest request = new BusinessAccountingRequest();
        request.setSourceType("SALE_SHIPMENT");
        request.setSourceId(9001L);
        request.setSourceNo("E2ECT-AR-9001");
        request.setCustomerId(CUSTOMER_PARTY);
        request.setSupplierId(SUPPLIER_PARTY);
        request.setAmount(new BigDecimal("100.00"));
        request.setDueDate(LEGACY_DUE_DATE);
        request.setBusinessDate(BUSINESS_DATE);
        request.setCounterpartyTenantId(COUNTERPARTY_TENANT);
        return request;
    }

    /** {@code biz_party} 结算档案（历史两值编码：0 = 现结，非 0 = 有账期）。 */
    private static PartySettlementProfile profile(int settlementType, Integer creditDays, Integer paymentDays) {
        PartySettlementProfile p = new PartySettlementProfile();
        p.setSettlementType(settlementType);
        p.setCreditDays(creditDays);
        p.setPaymentDays(paymentDays);
        return p;
    }

    private ReceivableDTO createReceivable(BusinessAccountingRequest request) {
        return service.createReceivableFromBusiness(request);
    }

    private PayableDTO createPayable(BusinessAccountingRequest request) {
        return service.createPayableFromBusiness(request);
    }
}
