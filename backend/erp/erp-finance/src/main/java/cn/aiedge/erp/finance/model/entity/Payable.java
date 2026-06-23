package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应付账款实体
 * 管理企业因采购商品或接受劳务等应支付给供应商的款项
 */
@Data
@TableName("finance_payable")
@EqualsAndHashCode(callSuper = true)
public class Payable extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 来源业务类型
     * purchase_order-采购订单 invoice-发票
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
     * 供应商ID
     */
    @TableField("supplier_id")
    private String supplierId;

    /**
     * 供应商名称
     */
    @TableField("supplier_name")
    private String supplierName;

    /**
     * 应付总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 已付金额
     */
    @TableField("paid_amount")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * 未付金额
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
     * normal-正常 overdue-逾期 written_off-已核销
     */
    @TableField("status")
    private String status;
}
