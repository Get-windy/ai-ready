package cn.aiedge.erp.purchase.service.integration;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.PayableService;
import cn.aiedge.erp.purchase.mapper.CounterpartyTenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 「采购收货 → 应付」接线层：收货日期与供应商所属租户<b>必须</b>被送进财务记账请求。
 *
 * <p>本测试与 {@code SalesAccountingServiceTest} 完全对称（口径见 DOMAIN-MODEL §7.7 附 / §13.3）：</p>
 * <ol>
 *   <li><b>回退到期日 = 业务日期本身</b>（现款现结、当天结清）——
 *       ⚠️ 曾经是「业务日期 + 30 天」，那是平台硬编码的默认账期，已按裁定移除；</li>
 *   <li>单据日期缺失时才退到「今天」（口径仍是当天结清，不再多加 30 天）；</li>
 *   <li>两个身份字段被正确装载（业务日期 = 收货/入库日期；对方租户 = 供应商档案所属租户）；</li>
 *   <li>取租户失败 / 财务侧回空到期日都<b>不阻断</b>收货确认；</li>
 *   <li><b>但「协议约定账期结算却缺天数/方向」必须拒单</b> —— 走
 *       {@link PurchaseAccountingService#assertSettlementResolvable}。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseAccountingServiceTest {

    /** 供应商 {@code biz_party.id}。 */
    private static final String SUPPLIER_ID = "3";
    private static final Long SUPPLIER_TENANT = 1L;

    /** 收货（入库）日期刻意是「过去」且与「今天」不同，用来说明回退基准不是今天。 */
    private static final LocalDate INBOUND_DATE = LocalDate.of(2026, 1, 31);

    @Mock
    private BusinessAccountingService businessAccountingService;
    @Mock
    private PayableService payableService;
    @Mock
    private CounterpartyTenantMapper counterpartyTenantMapper;

    private PurchaseAccountingService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseAccountingService(businessAccountingService, payableService,
                counterpartyTenantMapper);

        when(payableService.existsBySource("PURCHASE_RECEIPT", 9001L)).thenReturn(false);
        when(counterpartyTenantMapper.selectTenantIdByPartyId(3L)).thenReturn(SUPPLIER_TENANT);

        PayableDTO payable = new PayableDTO();
        payable.setId(6001L);
        payable.setDueDate(INBOUND_DATE);
        when(businessAccountingService.createPayableFromBusiness(any())).thenReturn(payable);

        VoucherDTO voucher = new VoucherDTO();
        voucher.setVoucherNo("PZ-9001");
        when(businessAccountingService.createVoucherFromBusiness(any())).thenReturn(voucher);
    }

    // ══════════════════════ ① 回退到期日 = 现款现结（业务日当天） ══════════════════════

    @Test
    @DisplayName("回退到期日 = 收货日期本身（现款现结、当天结清），不再是「收货日期 + 30」")
    void fallbackDueDateIsCashOnSpotNotThirtyDays() {
        service.createPayableOnReceipt(9001L, "PI-9001", SUPPLIER_ID, "供应商甲",
                new BigDecimal("100.00"), INBOUND_DATE);

        BusinessAccountingRequest request = capturedRequest();

        assertEquals(INBOUND_DATE, request.getDueDate(),
                "无协议 / 协议到期 / 未约定结算方式一律走「无协议无特定客户模式」= 现款现结，"
                        + "到期日 = 业务日；凭空的 30 天账期已移除");
        assertEquals(INBOUND_DATE, request.getBusinessDate(), "业务日期应原样透传为收货/入库日期");
        assertEquals(SUPPLIER_TENANT, request.getCounterpartyTenantId(),
                "对方租户应取供应商档案(biz_party)的归属租户 —— 它决定财务侧走协议口径还是档案口径");
    }

    @Test
    @DisplayName("单据日期缺失 ⇒ 到期日退到「今天」（当天结清），不再多加 30 天")
    void missingBusinessDateFallsBackToToday() {
        LocalDate today = LocalDate.now();

        service.createPayableOnReceipt(9001L, "PI-9001", SUPPLIER_ID, "供应商甲",
                new BigDecimal("100.00"), null);

        BusinessAccountingRequest request = capturedRequest();

        assertEquals(today, request.getDueDate(),
                "收货日期缺失时退回「今天」—— 口径仍是现款现结当天结清，不许加默认账期");
        assertNull(request.getBusinessDate(), "业务日期缺失时如实留空（财务侧据此按身份不足走现款现结）");
    }

    // ══════════════════════ ② 身份字段取不到 ⇒ 不猜、不阻断 ══════════════════════

    @Test
    @DisplayName("取供应商所属租户失败 ⇒ 对方租户留空、不抛异常、到期日照常回退")
    void counterpartyTenantLookupFailureDoesNotBlock() {
        when(counterpartyTenantMapper.selectTenantIdByPartyId(3L))
                .thenThrow(new IllegalStateException("模拟主数据取数异常"));

        assertDoesNotThrow(() -> service.createPayableOnReceipt(9001L, "PI-9001", SUPPLIER_ID,
                "供应商甲", new BigDecimal("100.00"), INBOUND_DATE));

        BusinessAccountingRequest request = capturedRequest();
        assertNull(request.getCounterpartyTenantId(),
                "取不到所属租户就如实留空（财务侧按「身份不足」走现款现结），不许拿本租户顶替");
        assertEquals(INBOUND_DATE, request.getDueDate(), "取数失败不得影响到期日生成");
    }

    @Test
    @DisplayName("供应商 ID 不是数字（只有名字快照）⇒ 不查租户、不阻断")
    void nonNumericSupplierIdIsTreatedAsMissingIdentity() {
        assertDoesNotThrow(() -> service.createPayableOnReceipt(9001L, "PI-9001", "供应商甲",
                "供应商甲", new BigDecimal("100.00"), INBOUND_DATE));

        assertNull(capturedRequest().getCounterpartyTenantId(),
                "非数字 ID 无法定位 biz_party 主体，不许用名字去猜");
    }

    // ══════════════════════ ③ 财务侧回空到期日不阻断（正常结论，不是缺项） ══════════════════════

    @Test
    @DisplayName("财务返回「到期日为空」⇒ 收货确认照常走完、凭证照常生成")
    void emptyDueDateFromFinanceDoesNotBlock() {
        PayableDTO noDueDate = new PayableDTO();
        noDueDate.setId(6002L);
        noDueDate.setDueDate(null);
        when(businessAccountingService.createPayableFromBusiness(any())).thenReturn(noDueDate);

        assertDoesNotThrow(() -> service.createPayableOnReceipt(9001L, "PI-9001", SUPPLIER_ID,
                "供应商甲", new BigDecimal("100.00"), INBOUND_DATE),
                "「本笔无账期」是正常业务结论、「无适用协议」走现款现结 —— 两种都不许阻断收货");

        verify(businessAccountingService, times(1)).createVoucherFromBusiness(any());
    }

    // ══════════════════════ ④ 唯一的拒单出口：提交前预检 ══════════════════════

    @Test
    @DisplayName("协议约定「账期结算」却缺天数/方向 ⇒ 预检抛业务异常（拒单）")
    void precheckRejectsWhenAgreementCreditTermIncomplete() {
        when(businessAccountingService.precheckPayableDueDate(any()))
                .thenThrow(BusinessException.badRequest("本笔单据不能提交 —— 协议约定的结算方式是「账期结算」，"
                        + "但账期天数或账期方向没有约定完整"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertSettlementResolvable(SUPPLIER_ID, INBOUND_DATE),
                "账期结算缺天数/方向是条件必填缺失 ⇒ 必须拒单（§13.3 铁律②）");

        assertTrue(ex.getMessage().contains("不能提交"), "提示要说清是「提交不了」：" + ex.getMessage());
        verify(businessAccountingService, times(1)).precheckPayableDueDate(any());
    }

    @Test
    @DisplayName("预检不落库、不记账：只调预检接口，不碰应付与凭证")
    void precheckDoesNotBookAnything() {
        service.assertSettlementResolvable(SUPPLIER_ID, INBOUND_DATE);

        verify(businessAccountingService, times(0)).createPayableFromBusiness(any());
        verify(businessAccountingService, times(0)).createVoucherFromBusiness(any());
    }

    @Test
    @DisplayName("预检入参带着两个身份字段（业务日期 + 对方租户），否则财务侧判不了走哪一层口径")
    void precheckCarriesIdentityFields() {
        service.assertSettlementResolvable(SUPPLIER_ID, INBOUND_DATE);

        ArgumentCaptor<BusinessAccountingRequest> captor =
                ArgumentCaptor.forClass(BusinessAccountingRequest.class);
        verify(businessAccountingService, times(1)).precheckPayableDueDate(captor.capture());

        assertEquals(INBOUND_DATE, captor.getValue().getBusinessDate());
        assertEquals(SUPPLIER_TENANT, captor.getValue().getCounterpartyTenantId());
        assertEquals(SUPPLIER_ID, captor.getValue().getSupplierId());
    }

    // ══════════════════════ 辅助 ══════════════════════

    private BusinessAccountingRequest capturedRequest() {
        ArgumentCaptor<BusinessAccountingRequest> captor =
                ArgumentCaptor.forClass(BusinessAccountingRequest.class);
        verify(businessAccountingService, times(1)).createPayableFromBusiness(captor.capture());
        return captor.getValue();
    }
}
