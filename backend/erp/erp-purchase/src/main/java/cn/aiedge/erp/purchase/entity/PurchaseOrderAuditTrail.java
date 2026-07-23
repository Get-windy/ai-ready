package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 采购订单审核流水 (1:N)
 * 对标 SaleOrderAuditTrail
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order_audit_trail")
public class PurchaseOrderAuditTrail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 采购订单ID */
    private Long orderId;

    /** 动作(submit/approve/reject/cancel) */
    private String action;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 审核级别(1-6) */
    private Integer levelNo;

    /** 审核意见 */
    private String comment;

    /** 操作时间 */
    private LocalDateTime operateTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
