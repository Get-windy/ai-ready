package cn.aiedge.dms.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付平台流水（日终导入，与 {@link DmsPayment} 逐笔对账）
 *
 * <p>对应 `dms_payment_flow`（迁移 `V11.322.0`）。同一「租户 + 渠道 + 平台交易号」唯一，
 * 重复导入幂等跳过；对账结果落在 {@code matchStatus} / {@code paymentId}。</p>
 */
@Data
@TableName("dms_payment_flow")
public class DmsPaymentFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 渠道编码（WECHAT / ALIPAY / POS / BANK …） */
    private String channelCode;

    /** 渠道名称快照 */
    private String channelName;

    /** 平台交易号（对账幂等键） */
    private String tradeNo;

    /** 商户单号 / 外部单号 */
    private String outTradeNo;

    /** 流水金额 */
    private BigDecimal amount;

    /** 平台成交时间 */
    private LocalDateTime tradeTime;

    /** 付款人 */
    private String payer;

    /** 导入批次号 */
    private String batchNo;

    /** 匹配状态 0-未匹配 1-已匹配 2-差异 3-已忽略 */
    private Integer matchStatus;

    /** 匹配到的收款记录 ID */
    private Long paymentId;

    /** 匹配方式 1-自动 2-人工 */
    private Integer matchType;

    private LocalDateTime matchTime;

    private String remark;

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
