package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商城订单明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order_item")
public class MallOrderItem extends BaseEntity {

    @TableField("order_id")
    private Long orderId;

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
}
