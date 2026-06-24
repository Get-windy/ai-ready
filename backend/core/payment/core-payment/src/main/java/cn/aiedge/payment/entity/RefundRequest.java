package cn.aiedge.payment.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款请求实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refund_request")
public class RefundRequest extends BaseEntity {

    /** 支付请求ID */
    private Long paymentId;

    /** 退款金额 */
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;

    /** 退款状态: 0待处理, 1处理中, 2已退款, 3已拒绝 */
    private Integer status;

    /** 第三方退款号 */
    private String channelRefundNo;

    /** 退款完成时间 */
    private LocalDateTime refundedTime;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 审批人ID */
    private Long approverId;

    /** 审批备注 */
    private String approveRemark;
}