package cn.aiedge.erp.b2b.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "mall_cart")
public class MallCart extends BaseEntity {

    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    @Column(name = "product_id", length = 64, nullable = false)
    private String productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_image", length = 500)
    private String productImage;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "checked")
    private Boolean checked = true;
}
