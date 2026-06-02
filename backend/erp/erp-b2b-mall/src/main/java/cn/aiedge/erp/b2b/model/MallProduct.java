package cn.aiedge.erp.b2b.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "mall_product")
public class MallProduct extends BaseEntity {

    @Column(name = "product_id", length = 64, unique = true, nullable = false)
    private String productId;

    @Column(name = "product_code", length = 64)
    private String productCode;

    @Column(name = "product_name", length = 200, nullable = false)
    private String productName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "sale_price", precision = 15, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "market_price", precision = 15, scale = 2)
    private BigDecimal marketPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "category_id", length = 64)
    private String categoryId;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sales_count")
    private Integer salesCount = 0;
}
