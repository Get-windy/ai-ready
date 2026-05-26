package cn.aiedge.order.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单明细DTO
 */
@Data
public class OrderItemDTO {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productCode;
    private String specification;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal price;
    private BigDecimal discountRate;
    private BigDecimal amount;
    private BigDecimal subtotal;
    private String remark;
}