package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 业务记账请求DTO
 * 用于接收来自业务系统（采购、销售等）的记账请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAccountingRequest {

    /**
     * 来源业务类型
     */
    private String sourceType;

    /**
     * 来源业务ID
     */
    private Long sourceId;

    /**
     * 来源业务编号
     */
    private String sourceNo;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 凭证日期
     */
    private LocalDate voucherDate;

    /**
     * 客户ID
     */
    private String customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 供应商ID
     */
    private String supplierId;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 到期日
     */
    private LocalDate dueDate;

    /**
     * 记账明细项
     */
    private List<AccountingRequestItem> items;

    /**
     * 记账请求明细项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountingRequestItem {
        /**
         * 摘要
         */
        private String summary;

        /**
         * 科目编码
         */
        private String subjectCode;

        /**
         * 借方金额
         */
        private BigDecimal debitAmount;

        /**
         * 贷方金额
         */
        private BigDecimal creditAmount;
    }
}
