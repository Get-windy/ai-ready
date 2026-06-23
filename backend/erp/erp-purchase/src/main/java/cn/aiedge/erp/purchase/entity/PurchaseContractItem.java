package cn.aiedge.erp.purchase.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购合同明细实体
 */
@Data
@Accessors(chain = true)
public class PurchaseContractItem {

    private Long id;

    private Long contractId;

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

    private String itemNote;

    private LocalDateTime createdAt;
}