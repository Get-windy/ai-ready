package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductListDTO {
    private Long id;
    private String productId;
    private String productName;
    private String imageUrl;
    private BigDecimal salePrice;
    private BigDecimal marketPrice;
    private Integer stockQuantity;
    private Integer salesCount;
}
