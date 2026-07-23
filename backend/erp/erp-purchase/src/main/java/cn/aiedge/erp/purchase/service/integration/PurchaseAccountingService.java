package cn.aiedge.erp.purchase.service.integration;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.PayableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购模块记账集成服务
 * 直接调用财务模块的 {@link BusinessAccountingService} 创建应付与会计凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseAccountingService {

    private final BusinessAccountingService businessAccountingService;
    private final PayableService payableService;

    /**
     * 收货时创建应付和库存凭证
     * Dr. Inventory (存货)
     * Cr. Accounts Payable (应付账款)
     *
     * @param receiptId   收货单ID
     * @param receiptNo   收货单编号
     * @param supplierId  供应商ID
     * @param supplierName 供应商名称
     * @param amount      金额
     */
    public void createPayableOnReceipt(Long receiptId, String receiptNo, String supplierId,
                                       String supplierName, BigDecimal amount) {
        // 防重复记账：同一收货单已产生应付则整体跳过（应付与凭证一并跳过）
        if (payableService.existsBySource("PURCHASE_RECEIPT", receiptId)) {
            log.warn("收货单已存在应付记录，跳过重复记账: receiptId={}, receiptNo={}", receiptId, receiptNo);
            return;
        }
        log.info("创建采购应付及凭证: receiptNo={}, supplierId={}, amount={}", receiptNo, supplierId, amount);

        // 1. Create payable via finance integration service
        BusinessAccountingRequest payableRequest = new BusinessAccountingRequest();
        payableRequest.setSourceType("PURCHASE_RECEIPT");
        payableRequest.setSourceId(receiptId);
        payableRequest.setSourceNo(receiptNo);
        payableRequest.setSupplierId(supplierId);
        payableRequest.setSupplierName(supplierName);
        payableRequest.setAmount(amount);
        payableRequest.setDueDate(LocalDate.now().plusDays(30));
        payableRequest.setSummary("采购收货 - " + receiptNo);

        PayableDTO payableResult = businessAccountingService.createPayableFromBusiness(payableRequest);
        log.info("应付创建成功: payableId={}", payableResult.getId());

        // 2. Create voucher via finance integration service
        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("PURCHASE_RECEIPT");
        voucherRequest.setSourceId(receiptId);
        voucherRequest.setSourceNo(receiptNo);
        voucherRequest.setSupplierId(supplierId);
        voucherRequest.setSupplierName(supplierName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary("采购入库凭证 - " + receiptNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        // Accounting entries: Dr. Inventory, Cr. AP
        BusinessAccountingRequest.AccountingRequestItem debitEntry = new BusinessAccountingRequest.AccountingRequestItem();
        debitEntry.setSummary("采购入库");
        debitEntry.setSubjectCode("1403");  // 原材料
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(BigDecimal.ZERO);

        BusinessAccountingRequest.AccountingRequestItem creditEntry = new BusinessAccountingRequest.AccountingRequestItem();
        creditEntry.setSummary("采购入库");
        creditEntry.setSubjectCode("2202");  // 应付账款
        creditEntry.setDebitAmount(BigDecimal.ZERO);
        creditEntry.setCreditAmount(amount);

        voucherRequest.setItems(List.of(debitEntry, creditEntry));

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        log.info("凭证创建成功: voucherNo={}", voucherResult.getVoucherNo());
    }
}
