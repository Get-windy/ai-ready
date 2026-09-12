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

    /**
     * 销售退货申请审核通过时创建红字冲销凭证
     * <p>收入段：Dr. 主营业务收入(6001) / Cr. 应收账款(1122)——冲回已确认的收入与应收；</p>
     * <p>成本段：Dr. 库存商品(1403) / Cr. 主营业务成本(6401)——退货入库冲回已结转成本（成本为 0 时省略）。</p>
     *
     * @param returnId     退货申请单ID
     * @param returnNo     退货申请单编号
     * @param customerId   客户ID
     * @param customerName 客户名称
     * @param amount       退货金额（含税，冲减收入与应收）
     * @param costAmount   退货成本金额（参考成本合计，可为 null/0）
     * @return 凭证编号；未生成时返回空串
     */
    public String createSaleReturnVoucher(Long returnId, String returnNo, Long customerId, String customerName,
                                          BigDecimal amount, BigDecimal costAmount) {
        return postSaleReturnVoucher(returnId, returnNo, customerId, customerName, amount, costAmount, false);
    }

    /**
     * 销售退货申请取消（已记账后）生成反向冲回凭证
     * <p>收入段：Dr. 应收账款(1122) / Cr. 主营业务收入(6001)；</p>
     * <p>成本段：Dr. 主营业务成本(6401) / Cr. 库存商品(1403)。</p>
     *
     * @return 凭证编号；未生成时返回空串
     */
    public String createSaleReturnReverseVoucher(Long returnId, String returnNo, Long customerId, String customerName,
                                                 BigDecimal amount, BigDecimal costAmount) {
        return postSaleReturnVoucher(returnId, returnNo, customerId, customerName, amount, costAmount, true);
    }

    /**
     * 销售退货单（实际退货入库单）审核通过时生成红字冲销凭证
     * <p>口径与退货申请一致：收入段 Dr. 主营业务收入(6001) / Cr. 应收账款(1122)；
     * 成本段 Dr. 库存商品(1403) / Cr. 主营业务成本(6401)（成本为 0 时省略）。</p>
     *
     * @param returnDocId  退货单ID
     * @param returnDocNo  退货单编号
     * @return 凭证编号；未生成时返回空串
     */
    public String createSaleReturnDocVoucher(Long returnDocId, String returnDocNo, Long customerId, String customerName,
                                             BigDecimal amount, BigDecimal costAmount) {
        return postReturnDocVoucher(returnDocId, returnDocNo, customerId, customerName, amount, costAmount, false);
    }

    /**
     * 销售退货单取消（已记账后）生成反向冲回凭证
     *
     * @return 凭证编号；未生成时返回空串
     */
    public String createSaleReturnDocReverseVoucher(Long returnDocId, String returnDocNo, Long customerId,
                                                    String customerName, BigDecimal amount, BigDecimal costAmount) {
        return postReturnDocVoucher(returnDocId, returnDocNo, customerId, customerName, amount, costAmount, true);
    }

    private String postReturnDocVoucher(Long returnDocId, String returnDocNo, Long customerId, String customerName,
                                        BigDecimal amount, BigDecimal costAmount, boolean reversed) {
        if (amount == null || amount.signum() <= 0) {
            log.warn("销售退货单金额为0，跳过记账: returnDocNo={}", returnDocNo);
            return "";
        }
        String sourceType = reversed ? "SALE_RETURN_DOC_CANCEL" : "SALE_RETURN_DOC";
        String prefix = reversed ? "销售退货单取消冲回" : "销售退货单冲销";
        log.info("创建销售退货单凭证: returnDocNo={}, customerId={}, amount={}, costAmount={}, reversed={}",
                returnDocNo, customerId, amount, costAmount, reversed);

        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType(sourceType);
        voucherRequest.setSourceId(returnDocId);
        voucherRequest.setSourceNo(returnDocNo);
        voucherRequest.setCustomerId(customerId != null ? String.valueOf(customerId) : null);
        voucherRequest.setCustomerName(customerName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary(prefix + " - " + returnDocNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        List<BusinessAccountingRequest.AccountingRequestItem> entries = new java.util.ArrayList<>();
        entries.add(buildEntry(reversed ? "销售退货单取消回补收入" : "销售退货单冲减收入", "6001",
                reversed ? BigDecimal.ZERO : amount, reversed ? amount : BigDecimal.ZERO, null));
        entries.add(buildEntry(reversed ? "销售退货单取消回补应收" : "销售退货单冲减应收", "1122",
                reversed ? amount : BigDecimal.ZERO, reversed ? BigDecimal.ZERO : amount, customerName));

        BigDecimal cost = costAmount != null ? costAmount : BigDecimal.ZERO;
        if (cost.signum() > 0) {
            entries.add(buildEntry(reversed ? "销售退货单取消转回成本" : "退货入库", "1403",
                    reversed ? BigDecimal.ZERO : cost, reversed ? cost : BigDecimal.ZERO, null));
            entries.add(buildEntry(reversed ? "销售退货单取消转回库存" : "冲回主营业务成本", "6401",
                    reversed ? cost : BigDecimal.ZERO, reversed ? BigDecimal.ZERO : cost, null));
        }
        voucherRequest.setItems(entries);

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        String voucherNo = voucherResult.getVoucherNo() != null ? voucherResult.getVoucherNo() : "";
        log.info("销售退货单凭证创建成功: returnDocNo={}, voucherNo={}", returnDocNo, voucherNo);
        return voucherNo;
    }

    private String postSaleReturnVoucher(Long returnId, String returnNo, Long customerId, String customerName,
                                         BigDecimal amount, BigDecimal costAmount, boolean reversed) {
        if (amount == null || amount.signum() <= 0) {
            log.warn("退货申请单金额为0，跳过记账: returnNo={}", returnNo);
            return "";
        }
        String sourceType = reversed ? "SALE_RETURN_APPLY_CANCEL" : "SALE_RETURN_APPLY";
        String prefix = reversed ? "销售退货取消冲回" : "销售退货冲销";
        log.info("创建销售退货凭证: returnNo={}, customerId={}, amount={}, costAmount={}, reversed={}",
                returnNo, customerId, amount, costAmount, reversed);

        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType(sourceType);
        voucherRequest.setSourceId(returnId);
        voucherRequest.setSourceNo(returnNo);
        voucherRequest.setCustomerId(customerId != null ? String.valueOf(customerId) : null);
        voucherRequest.setCustomerName(customerName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary(prefix + " - " + returnNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        List<BusinessAccountingRequest.AccountingRequestItem> entries = new java.util.ArrayList<>();

        // 收入段：正向冲减收入与应收；反向回补（往来科目行挂客户核算项，供辅助核算余额表归集）
        entries.add(buildEntry(reversed ? "销售退货取消回补收入" : "销售退货冲减收入", "6001",
                reversed ? BigDecimal.ZERO : amount, reversed ? amount : BigDecimal.ZERO, null));
        entries.add(buildEntry(reversed ? "销售退货取消回补应收" : "销售退货冲减应收", "1122",
                reversed ? amount : BigDecimal.ZERO, reversed ? BigDecimal.ZERO : amount, customerName));

        // 成本段：正向退货入库冲回已结转成本；反向退回出库恢复成本
        BigDecimal cost = costAmount != null ? costAmount : BigDecimal.ZERO;
        if (cost.signum() > 0) {
            entries.add(buildEntry(reversed ? "销售退货取消转回成本" : "退货入库", "1403",
                    reversed ? BigDecimal.ZERO : cost, reversed ? cost : BigDecimal.ZERO, null));
            entries.add(buildEntry(reversed ? "销售退货取消转回库存" : "冲回主营业务成本", "6401",
                    reversed ? cost : BigDecimal.ZERO, reversed ? BigDecimal.ZERO : cost, null));
        }

        voucherRequest.setItems(entries);

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        String voucherNo = voucherResult.getVoucherNo() != null ? voucherResult.getVoucherNo() : "";
        log.info("销售退货凭证创建成功: returnNo={}, voucherNo={}", returnNo, voucherNo);
        return voucherNo;
    }

    private BusinessAccountingRequest.AccountingRequestItem buildEntry(String summary, String subjectCode,
                                                                      BigDecimal debit, BigDecimal credit,
                                                                      String auxUnit) {
        BusinessAccountingRequest.AccountingRequestItem entry = new BusinessAccountingRequest.AccountingRequestItem();
        entry.setSummary(summary);
        entry.setSubjectCode(subjectCode);
        entry.setDebitAmount(debit);
        entry.setCreditAmount(credit);
        entry.setAuxUnit(auxUnit);
        return entry;
    }
}
