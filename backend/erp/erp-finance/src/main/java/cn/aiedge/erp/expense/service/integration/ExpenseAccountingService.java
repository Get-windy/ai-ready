package cn.aiedge.erp.expense.service.integration;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用模块记账集成服务
 * 直接调用财务模块的 {@link BusinessAccountingService} 创建费用凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseAccountingService {

    private final BusinessAccountingService businessAccountingService;

    /**
     * 费用报销审批通过后创建凭证
     * Dr. Expense (管理费用/销售费用等)
     * Cr. Other Payable (其他应付款)
     *
     * @param applicationId   费用申请ID
     * @param applicationCode 费用申请编号
     * @param amount          金额
     * @param summary         摘要
     * @return 凭证编号
     */
    public String createExpenseVoucher(Long applicationId, String applicationCode,
                                       BigDecimal amount, String summary) {
        log.info("创建费用凭证: applicationCode={}, amount={}", applicationCode, amount);

        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("EXPENSE");
        voucherRequest.setSourceId(applicationId);
        voucherRequest.setSourceNo(applicationCode);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary(summary != null ? summary : "费用报销 - " + applicationCode);
        voucherRequest.setVoucherDate(LocalDate.now());

        // Accounting entries: Dr. Expense, Cr. Other Payable
        // Use a default expense subject code (6602 - 管理费用) - subject can be refined per expense type
        BusinessAccountingRequest.AccountingRequestItem debitEntry = new BusinessAccountingRequest.AccountingRequestItem();
        debitEntry.setSummary("费用报销");
        debitEntry.setSubjectCode("6602");  // 管理费用
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(BigDecimal.ZERO);

        BusinessAccountingRequest.AccountingRequestItem creditEntry = new BusinessAccountingRequest.AccountingRequestItem();
        creditEntry.setSummary("应付报销款");
        creditEntry.setSubjectCode("2241");  // 其他应付款
        creditEntry.setDebitAmount(BigDecimal.ZERO);
        creditEntry.setCreditAmount(amount);

        voucherRequest.setItems(List.of(debitEntry, creditEntry));

        VoucherDTO result = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        String voucherNo = result.getVoucherNo() != null ? result.getVoucherNo() : "";
        log.info("费用凭证创建成功: voucherNo={}", voucherNo);
        return voucherNo;
    }
}
