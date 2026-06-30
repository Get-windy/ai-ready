package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 购物车
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_cart")
public class MallCart extends BaseEntity {

    @TableField("customer_id")
    private Long customerId;

    @TableField("product_id")
    private String productId;

    @TableField("product_name")
    private String productName;

    @TableField("product_image")
    private String productImage;

    @TableField("price")
    private BigDecimal price;

    @TableField("quantity")
    private Integer quantity;

    @TableField("subtotal")
    private BigDecimal subtotal;

    @TableField("checked")
    private Boolean checked = true;
}
