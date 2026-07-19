package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单物流信息 (1:N)
 * 支持配送/快递/自提多种方式，支持多包裹
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_logistics")
public class SaleOrderLogistics {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 物流类型: DELIVERY(配送) / EXPRESS(快递) / PICKUP(自提) */
    private String logisticsType;

    // ═══ 配送信息 ═══
    private String deliveryMethod;
    private String deliveryRoute;
    private Long deliveryRouteId;
    private Long driverId;
    private String driverName;
    private String deliveryVehicle;

    // ═══ 快递信息 ═══
    private String logisticsCompany;
    private String logisticsNo;
    private String waybillNo;

    // ═══ 费用 ═══
    private String freightPayer;
    private BigDecimal shippingFee;
    private BigDecimal codAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
