package cn.aiedge.payment.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体（第三方返回）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_record")
public class PaymentRecord extends BaseEntity {

    /** 支付请求ID */
    private Long requestId;

    /** 支付渠道 */
    private String channel;

    /** 第三方订单号 */
    private String channelOrderNo;

    /** 第三方交易号 */
    private String channelTradeNo;

    /** 支付金额 */
    private BigDecimal amount;

    /** 支付状态: 0待支付, 1支付中, 2成功, 3失败 */
    private Integer status;

    /** 回调时间 */
    private LocalDateTime callbackTime;

    /** 回调原始数据(JSON) */
    private String callbackData;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMsg;
}