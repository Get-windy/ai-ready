package cn.aiedge.payment.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付请求实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_request")
public class PaymentRequest extends BaseEntity {

    /** 业务类型: SALE_ORDER, TRADE_ORDER, DMS_DELIVERY */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 业务单号 */
    private String bizNo;

    /** 支付金额 */
    private BigDecimal amount;

    /** 支付渠道: ALIPAY, WECHAT, UNIONPAY, BANK, CASH */
    private String channel;

    /** 支付状态: 0待支付, 1支付中, 2已支付, 3已取消, 4已失败 */
    private Integer status;

    /** 第三方订单号 */
    private String channelOrderNo;

    /** 第三方交易号 */
    private String channelTradeNo;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 支付完成时间 */
    private LocalDateTime paidTime;

    /** 备注 */
    private String remark;

    /** 支付人ID */
    private Long payerId;

    /** 支付人姓名 */
    private String payerName;
}