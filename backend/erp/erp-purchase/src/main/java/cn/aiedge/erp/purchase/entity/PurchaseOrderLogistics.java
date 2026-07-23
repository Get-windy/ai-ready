package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单物流信息 (1:N)
 * 对标 SaleOrderLogistics
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order_logistics")
public class PurchaseOrderLogistics {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 采购订单ID */
    private Long orderId;

    /** 物流公司 */
    private String logisticsCompany;

    /** 运单号 */
    private String trackingNumber;

    /** 运费 */
    private BigDecimal freight;

    /** 预计到达 */
    private LocalDateTime estimatedArrival;

    /** 已发货 */
    private Boolean shipped;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 已收货 */
    private Boolean received;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 收货数量 */
    private BigDecimal receivedQuantity;

    /** 质检结果 */
    private String qualityCheckResult;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
