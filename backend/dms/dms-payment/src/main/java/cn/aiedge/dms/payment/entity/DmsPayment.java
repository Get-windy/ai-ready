package cn.aiedge.dms.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收款记录实体
 *
 * 对应数据库 dms_payment 表，记录现场收款信息，包括支付方式、二维码、外部订单号等。
 *
 * @author AI-Ready Team
 */
@Data
@TableName("dms_payment")
public class DmsPayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联任务ID */
    private Long taskId;

    /** 支付方式：1-微信 2-支付宝 3-现金 4-其他 */
    private Integer paymentType;

    /** 收款金额 */
    private BigDecimal amount;

    /** 收款二维码URL */
    private String qrcodeUrl;

    /** 外部订单号（微信/支付宝支付单号） */
    private String externalOrderNo;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 支付状态：0-待支付 1-已支付 2-未付标记 */
    private Integer status;

    /** 未付备注 */
    private String unpaidRemark;

    // ========== 审核字段 ==========

    /** 审核状态：0-待审核 1-通过 2-驳回 */
    private Integer auditStatus;

    /** 审核人 */
    private Long auditBy;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 逻辑删除 */
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
