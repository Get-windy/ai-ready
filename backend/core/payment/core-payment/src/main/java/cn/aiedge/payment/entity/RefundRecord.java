package cn.aiedge.payment.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refund_record")
public class RefundRecord extends BaseEntity {

    /** 退款请求ID */
    private Long requestId;

    /** 支付渠道 */
    private String channel;

    /** 第三方退款号 */
    private String channelRefundNo;

    /** 退款金额 */
    private BigDecimal amount;

    /** 退款状态: 0处理中, 1成功, 2失败 */
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