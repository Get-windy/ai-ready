package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartDTO {
    private Long id;
    private String productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private Boolean checked;
}
