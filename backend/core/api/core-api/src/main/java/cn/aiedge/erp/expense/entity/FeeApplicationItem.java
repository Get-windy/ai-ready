package cn.aiedge.erp.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用申请明细表
 */
@Data
@TableName("fee_application_item")
public class FeeApplicationItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 费用申请ID */
    private Long applicationId;

    /** 费用项目名称 */
    private String itemName;

    /** 费用说明 */
    private String description;

    /** 费用发生日期 */
    private LocalDate expenseDate;

    /** 费用类型 */
    private String expenseType;

    /** 费用类型描述 */
    private String expenseTypeDesc;

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

    /** 税率(%) */
    private BigDecimal taxRate;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 含税总金额 */
    private BigDecimal totalAmountWithTax;

    /** 是否有发票(0=否 1=是) */
    private Integer hasInvoice;

    /** 发票号码 */
    private String invoiceNumber;

    /** 发票日期 */
    private LocalDate invoiceDate;

    /** 会计科目编码 */
    private String accountCode;

    /** 预算科目编码 */
    private String budgetCode;

    /** 项目编码 */
    private String projectCode;

    /** 成本中心 */
    private String costCenter;

    /** 是否个人垫付(0=否 1=是) */
    private Integer isPersonal;

    /** 是否可报销(0=否 1=是) */
    private Integer isReimbursable;

    /** 是否需要收据(0=否 1=是) */
    private Integer receiptRequired;

    /** 收据已附(0=否 1=是) */
    private Integer receiptAttached;

    /** 是否已核验(0=否 1=是) */
    private Integer isVerified;

    /** 核验人ID */
    private Long verifiedBy;

    /** 核验日期 */
    private LocalDate verifiedDate;

    /** 核验意见 */
    private String verificationComment;

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
