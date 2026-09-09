package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预付款/定金实体
 * 管理预付给供应商的定金、押金、保证金等。
 * 状态机：draft(草稿) → confirmed(已记账) → offset(已冲抵)/recovered(已收回)/refunded(已退款)
 */
@Data
@Accessors(chain = true)
@TableName("erp_pre_payment")
public class PrePayment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String prePaymentNo;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    /** 源单未结金额 */
    private BigDecimal sourceUnsettledAmount;

    /** 供应商编号（结算单位编号） */
    private String partnerCode;

    private Long supplierId;

    private String supplierName;

    /** 本次预付金额（Σ付款账户明细） */
    private BigDecimal amount;

    private BigDecimal usedAmount;

    private BigDecimal remainingAmount;

    /** 此前预付：记账前预付余额快照（选中结算单位时带出） */
    private BigDecimal prevAmount;

    private String depositType;

    private LocalDate paymentDate;

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

    /** 附件 */
    private String attachment;

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
