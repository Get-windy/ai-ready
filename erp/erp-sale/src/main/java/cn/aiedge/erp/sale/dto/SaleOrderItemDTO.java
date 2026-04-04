package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;

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
    private String specification;
    private String unit;
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