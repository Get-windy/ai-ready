package cn.aiedge.erp.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款记录表
 */
@Data
@TableName("fee_payment_record")
public class FeePaymentRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 业务类型: APPLICATION/REIMBURSEMENT */
    private String businessType;

    /** 业务单据ID */
    private Long businessId;

    /** 业务单据编号 */
    private String businessNo;

    /** 付款单号 */
    private String paymentNo;

    /** 付款金额 */
    private BigDecimal paymentAmount;

    /** 币种 */
    private String currency;

    /** 支付方式 */
    private String paymentMethod;

    /** 付款账户 */
    private String paymentAccount;

    /** 付款账户名称 */
    private String accountName;

    /** 收款人名称 */
    private String payeeName;

    /** 收款人账户 */
    private String payeeAccount;

    /** 付款日期 */
    private LocalDate paymentDate;

    /** 凭证号 */
    private String voucherNo;

    /** 付款人ID */
    private Long payerId;

    /** 付款人姓名 */
    private String payerName;

    /** 状态: PENDING/COMPLETED/FAILED/CANCELLED */
    private String status;

    /** 失败原因 */
    private String failReason;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 确认人ID */
    private Long confirmUserId;

    /** 确认人姓名 */
    private String confirmUserName;

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
