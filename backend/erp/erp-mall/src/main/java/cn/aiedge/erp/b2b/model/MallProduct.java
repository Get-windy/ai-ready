package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商城商品（映射 mall_product 表）
 * 与 erp_product 通过 product_id 关联
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_product")
public class MallProduct extends BaseEntity {

    /** 商品编码，关联 erp_product.product_code */
    @TableField("product_id")
    private String productId;

    @TableField("product_code")
    private String productCode;

    @TableField("product_name")
    private String productName;

    @TableField("image_url")
    private String imageUrl;

    @TableField("sale_price")
    private BigDecimal salePrice;

    @TableField("market_price")
    private BigDecimal marketPrice;

    @TableField("stock_quantity")
    private Integer stockQuantity;

    @TableField("category_id")
    private String categoryId;

    @TableField("category_name")
    private String categoryName;

    @TableField("status")
    private String status;

    @TableField("description")
    private String description;

    @TableField("sales_count")
    private Integer salesCount = 0;
}
