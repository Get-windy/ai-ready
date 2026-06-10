package cn.aiedge.erp.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用报销明细表
 */
@Data
@TableName("fee_reimbursement_item")
public class FeeReimbursementItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 报销单ID */
    private Long reimbursementId;

    /** 关联申请明细ID */
    private Long applicationItemId;

    /** 费用项目名称 */
    private String itemName;

    /** 费用说明 */
    private String description;

    /** 费用发生日期 */
    private LocalDate expenseDate;

    /** 金额(不含税) */
    private BigDecimal amount;

    /** 数量 */
    private BigDecimal quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 单位 */
    private String unit;

    /** 供应商/收款方 */
    private String vendorName;

    /** 是否有发票(0=否 1=是) */
    private Integer hasInvoice;

    /** 发票号码 */
    private String invoiceNumber;

    /** 发票日期 */
    private LocalDate invoiceDate;

    /** 税率(%) */
    private BigDecimal taxRate;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 含税总金额 */
    private BigDecimal totalAmountWithTax;

    /** 会计科目编码 */
    private String accountCode;

    /** 预算科目编码 */
    private String budgetCode;

    /** 项目编码 */
    private String projectCode;

    /** 成本中心 */
    private String costCenter;

    /** 序号 */
    private Integer sequenceNumber;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
