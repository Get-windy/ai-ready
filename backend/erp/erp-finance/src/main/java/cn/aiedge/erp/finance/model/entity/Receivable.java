package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款实体
 * 管理企业因销售商品或提供劳务等应向客户收取的款项
 */
@Data
@TableName("finance_receivable")
@EqualsAndHashCode(callSuper = true)
public class Receivable extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 来源业务类型
     * sale_order-销售订单 invoice-发票
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 来源业务ID
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 来源业务编号
     */
    @TableField("source_no")
    private String sourceNo;

    /**
     * 客户ID
     */
    @TableField("customer_id")
    private String customerId;

    /**
     * 客户名称
     */
    @TableField("customer_name")
    private String customerName;

    /**
     * 应收总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 已收金额
     */
    @TableField("paid_amount")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * 未收金额
     */
    @TableField("remaining_amount")
    private BigDecimal remainingAmount;

    /**
     * 到期日
     */
    @TableField("due_date")
    private LocalDate dueDate;

    /**
     * 开票日期
     */
    @TableField("invoice_date")
    private LocalDate invoiceDate;

    /**
     * 发票号码
     */
    @TableField("invoice_no")
    private String invoiceNo;

    /**
     * 状态
     * normal-正常 overdue-逾期 written_off-已核销 bad_debt-坏账
     */
    @TableField("status")
    private String status;
}
