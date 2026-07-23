package cn.aiedge.erp.sale.service.integration;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.ReceivableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售模块记账集成服务
 * 直接调用财务模块的 {@link BusinessAccountingService} 创建应收与会计凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAccountingService {

    private final BusinessAccountingService businessAccountingService;
    private final ReceivableService receivableService;

    /**
     * 发货时创建应收和收入凭证
     * Dr. Accounts Receivable (应收账款)
     * Cr. Revenue (主营业务收入)
     *
     * @param shipmentId   发货单ID
     * @param shipmentNo   发货单编号
     * @param customerId   客户ID
     * @param customerName 客户名称
     * @param amount       金额
     */
    public void createReceivableOnShipment(Long shipmentId, String shipmentNo, String customerId,
                                           String customerName, BigDecimal amount) {
        // 防重复记账：同一发货单已产生应收则整体跳过（应收与凭证一并跳过）
        if (receivableService.existsBySource("SALE_SHIPMENT", shipmentId)) {
            log.warn("发货单已存在应收记录，跳过重复记账: shipmentId={}, shipmentNo={}", shipmentId, shipmentNo);
            return;
        }
        log.info("创建销售应收及凭证: shipmentNo={}, customerId={}, amount={}", shipmentNo, customerId, amount);

        // 1. Create receivable via finance integration service
        BusinessAccountingRequest receivableRequest = new BusinessAccountingRequest();
        receivableRequest.setSourceType("SALE_SHIPMENT");
        receivableRequest.setSourceId(shipmentId);
        receivableRequest.setSourceNo(shipmentNo);
        receivableRequest.setCustomerId(customerId);
        receivableRequest.setCustomerName(customerName);
        receivableRequest.setAmount(amount);
        receivableRequest.setDueDate(LocalDate.now().plusDays(30));
        receivableRequest.setSummary("销售发货 - " + shipmentNo);

        ReceivableDTO receivableResult = businessAccountingService.createReceivableFromBusiness(receivableRequest);
        log.info("应收创建成功: receivableId={}", receivableResult.getId());

        // 2. Create voucher via finance integration service
        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("SALE_SHIPMENT");
        voucherRequest.setSourceId(shipmentId);
        voucherRequest.setSourceNo(shipmentNo);
        voucherRequest.setCustomerId(customerId);
        voucherRequest.setCustomerName(customerName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary("销售出库凭证 - " + shipmentNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        // Accounting entries: Dr. AR, Cr. Revenue
        BusinessAccountingRequest.AccountingRequestItem debitEntry = new BusinessAccountingRequest.AccountingRequestItem();
        debitEntry.setSummary("销售出库");
        debitEntry.setSubjectCode("1122");  // 应收账款
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(BigDecimal.ZERO);

        BusinessAccountingRequest.AccountingRequestItem creditEntry = new BusinessAccountingRequest.AccountingRequestItem();
        creditEntry.setSummary("确认收入");
        creditEntry.setSubjectCode("6001");  // 主营业务收入
        creditEntry.setDebitAmount(BigDecimal.ZERO);
        creditEntry.setCreditAmount(amount);

        voucherRequest.setItems(List.of(debitEntry, creditEntry));

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        log.info("凭证创建成功: voucherNo={}", voucherResult.getVoucherNo());
    }
}
