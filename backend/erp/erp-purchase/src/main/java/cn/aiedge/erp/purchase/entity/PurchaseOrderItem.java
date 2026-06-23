package cn.aiedge.erp.purchase.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单明细实体
 */
@Data
@Accessors(chain = true)
public class PurchaseOrderItem {

    private Long id;

    private Long orderId;

    private String materialName;

    private String specification;

    private String unit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private String brand;

    private String model;

    private String qualityLevel;

    private String originCountry;

    private Integer leadTime;

    private String deliveryLocation;

    private BigDecimal receivedQuantity;

    private BigDecimal fulfillmentPercent;

    private String itemNote;

    private LocalDateTime createdAt;
}