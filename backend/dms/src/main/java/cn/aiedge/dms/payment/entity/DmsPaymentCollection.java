package cn.aiedge.dms.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 未付（挂账）催收 / 承诺付款 / 核销流水
 *
 * <p>对应 `dms_payment_collection`（迁移 `V11.322.0`）。挂账单据的每一次催收、客户承诺、
 * 实际收款（核销）都留痕，支撑《收款管理》「未付管理」Tab 与金额自洽校验。</p>
 */
@Data
@TableName("dms_payment_collection")
public class DmsPaymentCollection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 收款记录 ID（dms_payment.id） */
    private Long paymentId;

    /** 配送任务 ID */
    private Long taskId;

    /** 动作 1-催收 2-核销 3-承诺付款 */
    private Integer actionType;

    /** 核销金额（仅 actionType=2） */
    private BigDecimal amount;

    /** 承诺付款日（仅 actionType=3） */
    private LocalDate promiseDate;

    /** 催收/核销说明 */
    private String content;

    private Long operatorId;

    private String operatorName;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @Version
    private Integer version;
}
