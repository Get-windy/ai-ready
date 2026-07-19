package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 销售订单审核流水 (1:N)
 * 记录提交/审核/反审核/取消等操作流水
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_audit_trail")
public class SaleOrderAuditTrail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 操作类型: SUBMIT / APPROVE / REJECT / CANCEL */
    private String action;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作时间 */
    private LocalDateTime actionTime;

    /** 备注 */
    private String remark;
}
