package cn.aiedge.erp.sale.service.integration;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.ReceivableService;
import cn.aiedge.erp.sale.mapper.CounterpartyTenantMapper;
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
 * 「销售出库 → 应收」接线层：出库日期与客户所属租户<b>必须</b>被送进财务记账请求。
 *
 * <p>本测试钉死接线层的行为（口径见 DOMAIN-MODEL §7.7 附 / §13.3）：</p>
 * <ol>
 *   <li><b>回退到期日 = 业务日期本身</b>（现款现结、当天结清）——
 *       ⚠️ 曾经是「业务日期 + 30 天」，那是平台硬编码的默认账期，已按裁定移除；</li>
 *   <li>单据日期缺失时才退到「今天」（口径仍是当天结清，不再多加 30 天）；</li>
 *   <li>两个身份字段被正确装载（业务日期 = 出库日期；对方租户 = 客户档案所属租户）——
 *       它们决定财务侧走<b>哪一层</b>结算口径（跨租户→协议 / 租户内→档案）；</li>
 *   <li>取租户失败 / 财务侧回空到期日都<b>不阻断</b>开单；</li>
 *   <li><b>但「协议约定账期结算却缺天数/方向」必须拒单</b> —— 走
 *       {@link SalesAccountingService#assertSettlementResolvable}，
 *       它与记账分开，正是为了不被"记账失败不阻断单据"的 catch 吞掉。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesAccountingServiceTest {

    /** 客户 {@code biz_party.id}；刻意与本租户(1)不同，用来说明「对方租户」被如实透传。 */
    private static final String CUSTOMER_ID = "2";
    private static final Long CUSTOMER_TENANT = 1L;

    /** 出库日期刻意是「过去」且与「今天」不同，用来说明回退基准不是今天。 */
    private static final LocalDate OUTBOUND_DATE = LocalDate.of(2026, 1, 31);

    @Mock
    private BusinessAccountingService businessAccountingService;
    @Mock
    private ReceivableService receivableService;
    @Mock
    private CounterpartyTenantMapper counterpartyTenantMapper;

    private SalesAccountingService service;

    @BeforeEach
    void setUp() {
        service = new SalesAccountingService(businessAccountingService, receivableService,
                counterpartyTenantMapper);

        // 未记账过（否则整个方法会被防重复记账提前返回）
        when(receivableService.existsBySource("SALE_SHIPMENT", 9001L)).thenReturn(false);
        when(counterpartyTenantMapper.selectTenantIdByPartyId(2L)).thenReturn(CUSTOMER_TENANT);

        ReceivableDTO receivable = new ReceivableDTO();
        receivable.setId(5001L);
        receivable.setDueDate(OUTBOUND_DATE);
        when(businessAccountingService.createReceivableFromBusiness(any())).thenReturn(receivable);

        VoucherDTO voucher = new VoucherDTO();
        voucher.setVoucherNo("PZ-9001");
        when(businessAccountingService.createVoucherFromBusiness(any())).thenReturn(voucher);
    }

    // ══════════════════════ ① 回退到期日 = 现款现结（业务日当天） ══════════════════════

    @Test
    @DisplayName("回退到期日 = 出库日期本身（现款现结、当天结清），不再是「出库日期 + 30」")
    void fallbackDueDateIsCashOnSpotNotThirtyDays() {
        service.createReceivableOnShipment(9001L, "XSCK-9001", CUSTOMER_ID, "客户甲",
                new BigDecimal("100.00"), OUTBOUND_DATE);

        BusinessAccountingRequest request = capturedRequest();

        assertEquals(OUTBOUND_DATE, request.getDueDate(),
                "无协议 / 协议到期 / 未约定结算方式一律走「无协议无特定客户模式」= 现款现结，"
                        + "到期日 = 业务日；凭空的 30 天账期是平台替双方定商业条款，已移除");
        assertEquals(OUTBOUND_DATE, request.getBusinessDate(), "业务日期应原样透传给出库日期");
        assertEquals(CUSTOMER_TENANT, request.getCounterpartyTenantId(),
                "对方租户应取客户档案(biz_party)的归属租户 —— 它决定财务侧走协议口径还是档案口径");
    }

    @Test
    @DisplayName("单据日期缺失 ⇒ 到期日退到「今天」（当天结清），不再多加 30 天")
    void missingBusinessDateFallsBackToToday() {
        LocalDate today = LocalDate.now();

        service.createReceivableOnShipment(9001L, "XSCK-9001", CUSTOMER_ID, "客户甲",
                new BigDecimal("100.00"), null);

        BusinessAccountingRequest request = capturedRequest();

        assertEquals(today, request.getDueDate(),
                "出库日期缺失时退回「今天」—— 口径仍是现款现结当天结清，不许凭空造一个业务日期，也不许加默认账期");
        assertNull(request.getBusinessDate(), "业务日期缺失时如实留空（财务侧据此按身份不足走现款现结）");
    }

    // ══════════════════════ ② 身份字段取不到 ⇒ 不猜、不阻断 ══════════════════════

    @Test
    @DisplayName("取客户所属租户失败 ⇒ 对方租户留空、不抛异常、到期日照常回退")
    void counterpartyTenantLookupFailureDoesNotBlock() {
        when(counterpartyTenantMapper.selectTenantIdByPartyId(2L))
                .thenThrow(new IllegalStateException("模拟主数据取数异常"));

        assertDoesNotThrow(() -> service.createReceivableOnShipment(9001L, "XSCK-9001", CUSTOMER_ID,
                "客户甲", new BigDecimal("100.00"), OUTBOUND_DATE));

        BusinessAccountingRequest request = capturedRequest();
        assertNull(request.getCounterpartyTenantId(),
                "取不到所属租户就如实留空（财务侧按「身份不足」走现款现结），不许拿本租户顶替");
        assertEquals(OUTBOUND_DATE, request.getDueDate(), "取数失败不得影响到期日生成");
    }

    @Test
    @DisplayName("客户 ID 不是数字（只有名字快照）⇒ 不查租户、不阻断")
    void nonNumericCustomerIdIsTreatedAsMissingIdentity() {
        assertDoesNotThrow(() -> service.createReceivableOnShipment(9001L, "XSCK-9001", "客户甲",
                "客户甲", new BigDecimal("100.00"), OUTBOUND_DATE));

        assertNull(capturedRequest().getCounterpartyTenantId(),
                "非数字 ID 无法定位 biz_party 主体，不许用名字去猜");
    }

    // ══════════════════════ ③ 财务侧回空到期日不阻断（正常结论，不是缺项） ══════════════════════

    @Test
    @DisplayName("财务返回「到期日为空」⇒ 单据照常走完、凭证照常生成")
    void emptyDueDateFromFinanceDoesNotBlock() {
        ReceivableDTO noDueDate = new ReceivableDTO();
        noDueDate.setId(5002L);
        noDueDate.setDueDate(null);
        when(businessAccountingService.createReceivableFromBusiness(any())).thenReturn(noDueDate);

        assertDoesNotThrow(() -> service.createReceivableOnShipment(9001L, "XSCK-9001", CUSTOMER_ID,
                "客户甲", new BigDecimal("100.00"), OUTBOUND_DATE),
                "「本笔无账期」是正常业务结论、「无适用协议」走现款现结 —— 两种都不许阻断开单");

        verify(businessAccountingService, times(1)).createVoucherFromBusiness(any());
    }

    // ══════════════════════ ④ 唯一的拒单出口：提交前预检 ══════════════════════

    @Test
    @DisplayName("协议约定「账期结算」却缺天数/方向 ⇒ 预检抛业务异常（拒单），且带得出对方租户")
    void precheckRejectsWhenAgreementCreditTermIncomplete() {
        when(businessAccountingService.precheckReceivableDueDate(any()))
                .thenThrow(BusinessException.badRequest("本笔单据不能提交 —— 协议约定的结算方式是「账期结算」，"
                        + "但账期天数或账期方向没有约定完整"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertSettlementResolvable(CUSTOMER_ID, OUTBOUND_DATE),
                "账期结算缺天数/方向是条件必填缺失 ⇒ 必须拒单（§13.3 铁律②）");

        assertTrue(ex.getMessage().contains("不能提交"), "提示要说清是「提交不了」：" + ex.getMessage());
        verify(businessAccountingService, times(1)).precheckReceivableDueDate(any());
    }

    @Test
    @DisplayName("预检不落库、不记账：只调预检接口，不碰应收与凭证")
    void precheckDoesNotBookAnything() {
        service.assertSettlementResolvable(CUSTOMER_ID, OUTBOUND_DATE);

        verify(businessAccountingService, times(0)).createReceivableFromBusiness(any());
        verify(businessAccountingService, times(0)).createVoucherFromBusiness(any());
    }

    @Test
    @DisplayName("预检入参带着两个身份字段（业务日期 + 对方租户），否则财务侧判不了走哪一层口径")
    void precheckCarriesIdentityFields() {
        service.assertSettlementResolvable(CUSTOMER_ID, OUTBOUND_DATE);

        ArgumentCaptor<BusinessAccountingRequest> captor =
                ArgumentCaptor.forClass(BusinessAccountingRequest.class);
        verify(businessAccountingService, times(1)).precheckReceivableDueDate(captor.capture());

        assertEquals(OUTBOUND_DATE, captor.getValue().getBusinessDate());
        assertEquals(CUSTOMER_TENANT, captor.getValue().getCounterpartyTenantId());
        assertEquals(CUSTOMER_ID, captor.getValue().getCustomerId());
    }

    // ══════════════════════ 辅助 ══════════════════════

    private BusinessAccountingRequest capturedRequest() {
        ArgumentCaptor<BusinessAccountingRequest> captor =
                ArgumentCaptor.forClass(BusinessAccountingRequest.class);
        verify(businessAccountingService, times(1)).createReceivableFromBusiness(captor.capture());
        return captor.getValue();
    }
}
