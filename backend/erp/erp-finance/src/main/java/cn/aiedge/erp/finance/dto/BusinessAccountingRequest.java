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
     * 业务日期（来源单据的**业务发生日**：销售出库日 / 采购入库日）。
     *
     * <p>它是「协议约定的账期」的起算基准日 —— 到期日 = 业务日期 + 约定天数。
     * 刻意<b>不用</b> {@code voucherDate} 代替：凭证日期是记账口径的日期，两者可以不同
     * （补录、跨期调整都是常见情形），拿它当起算基准会把账期算到错的月份去。</p>
     *
     * <p><b>为空表示调用方未提供</b>：此时不消费协议账期，到期日按下面 {@code dueDate}
     * 的既有逻辑走（与接入协议账期之前逐字一致）。</p>
     */
    private LocalDate businessDate;

    /**
     * 对方主体（客户 / 供应商）**所属租户 ID**。
     *
     * <p>协议是跨租户契约，判定"按哪一份协议执行"必须知道两端租户。
     * 本端租户取当前会话租户，另一端（对方主体）的租户只能由调用方给出 ——
     * 本仓现状：单据上只有对方的 {@code biz_party.id}，没有它所属租户。</p>
     *
     * <p><b>为空表示调用方未提供</b>：此时不消费协议账期（按既有逻辑走），
     * 而<b>不是</b>去猜一个租户 —— 猜错租户会读到别人的商业条款。</p>
     */
    private Long counterpartyTenantId;

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

        /**
         * 核算项-往来单位（应收/应付/预收/预付等往来科目行填写，供辅助核算余额表按往来单位归集）
         */
        private String auxUnit;

        /**
         * 核算项-部门（费用类科目行填写）
         */
        private String auxDept;

        /**
         * 核算项-职员（费用类科目行填写）
         */
        private String auxStaff;
    }
}
