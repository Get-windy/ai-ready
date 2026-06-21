package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单明细DTO
 */
@Data
public class SaleOrderItemDTO {

    private Long id;
    private Long orderId;
    private Integer lineNo;
    private Long productId;
    private String productCode;
    private String productName;
    private String barcode;
    private String specification;
    private String location;
    private String unit;
    private String lineAttribute;
    private String batchCode;
    private LocalDateTime productionDate;
    private String shelfLife;
    private LocalDateTime expiryDate;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;
    private BigDecimal quantity;
    private BigDecimal shippedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private BigDecimal unitPriceWithTax;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal amountWithTax;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private Long warehouseId;
    private String remark;
}