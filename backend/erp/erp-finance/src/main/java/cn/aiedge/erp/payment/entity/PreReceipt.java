package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预收款/定金实体
 * 管理客户预付的定金、押金、保证金等
 */
@Data
@Accessors(chain = true)
@TableName("erp_pre_receipt")
public class PreReceipt {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String preReceiptNo;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    /** 源单未结金额 */
    private BigDecimal sourceUnsettledAmount;

    /** 结算单位编号（往来单位/客户编号，对标「客户编号」） */
    private String partnerCode;

    private Long customerId;

    private String customerName;

    private BigDecimal amount;

    private BigDecimal usedAmount;

    private BigDecimal remainingAmount;

    /** 本单赠送金额：充值赠送计入预收余额但不计资金账户 */
    private BigDecimal giftAmount;

    /** 总金额 = 本次预收 + 赠送金额 */
    private BigDecimal totalAmount;

    /** 此前预收：记账前预收余额快照（选中结算单位时带出） */
    private BigDecimal prevAmount;

    private String depositType;

    private LocalDate receiptDate;

    private String status;

    private String remark;

    /** 摘要 */
    private String summary;

    /** 经手人 */
    private Long handlerId;

    private String handlerName;

    /** 部门 */
    private Long deptId;

    private String deptName;

    /** 制单人（Audit 填充 createBy，此列冗余展示） */
    private String creatorName;

    /** 记账人 */
    private Long bookkeeperId;

    private String bookkeeperName;

    private LocalDateTime bookkeepingTime;

    /** 审核人 */
    private Long auditorId;

    private String auditorName;

    private LocalDateTime auditorTime;

    /** 打印次数 */
    private Integer printCount;

    private Integer paymentMethod;

    private String bankAccount;

    private String bankName;

    private String transactionNo;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}
