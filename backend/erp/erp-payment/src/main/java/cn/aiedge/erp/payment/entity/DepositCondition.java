package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定金/押金条件实体
 * 记录定金/押金的约定条件、违约条款、到期日期等
 */
@Data
@Accessors(chain = true)
@TableName("erp_deposit_condition")
public class DepositCondition {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 关联预收款/预付款 */
    private Long preReceiptId;
    private Long prePaymentId;

    /** 类型: deposit/deposit_guarantee/bid_bond */
    private String depositType;

    /** 方向: receipt(我方收款)/payment(我方付款) */
    private String direction;

    /** 来源业务单据 */
    private String sourceType;
    private Long sourceId;
    private String sourceNo;

    /** 条件描述 */
    private String conditionDesc;
    private String breachClause;

    /** 日期 */
    private LocalDate agreedDate;
    private LocalDate expiredDate;

    /** 金额 */
    private BigDecimal amount;
    private BigDecimal forfeitAmount;

    /** 联系人 */
    private String contactPerson;
    private String contactPhone;

    /** 状态: frozen/converted/refunded/forfeited/deducted */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer versionNo;
}
