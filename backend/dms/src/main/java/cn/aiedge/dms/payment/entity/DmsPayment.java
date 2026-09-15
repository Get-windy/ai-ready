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

    /** 收款类型：1-代收货款（负债，需上交） 2-配送费（收入）；与支付方式 payChannel 区分 */
    private Integer paymentType;

    /** 支付方式：1-微信 2-支付宝 3-现金 4-POS 5-银行转账 9-其他 */
    private Integer payChannel;

    /** 支付方式名称快照 */
    private String payChannelName;

    /** 支付平台交易号（回调/对账**幂等键**与凭证） */
    private String tradeNo;

    /** 支付回调到达时间 */
    private LocalDateTime callbackTime;

    /** 配送员ID（交款稽核按人汇总） */
    private Long riderId;

    // ========== 资金上交 / 稽核（资金安全核心） ==========

    /** 交款状态 0-未交 1-部分交 2-已交 */
    private Integer handoverStatus;

    /** 已上交金额 */
    private BigDecimal handoverAmount;

    private LocalDateTime handoverTime;

    private Long handoverBy;

    /** 交款经办人姓名快照 */
    private String handoverByName;

    private String handoverRemark;

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

    // ========== 财务打通（推 ERP 幂等键） ==========

    /** 财务推送状态 0-未推送 1-已推送 */
    private Integer financePushStatus;

    /** 财务推送追踪号（事件发件箱 traceId） */
    private String financeTraceId;

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
