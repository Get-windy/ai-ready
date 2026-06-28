package cn.aiedge.erp.b2b.dao;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * erp_product 表映射（商城模块只读视图）
 * 替代 mall_product 表，消除数据冗余
 */
@Data
@TableName("v_mall_product")
public class ErpProductMall implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 关联 erp_product.product_code */
    private String productId;

    private String productCode;

    private String productName;

    private String imageUrl;

    /** 零售价 → 商城销售价 */
    private BigDecimal salePrice;

    /** 批发价 → 商城市场价 */
    private BigDecimal marketPrice;

    /** 可用库存（来自 erp_stock 实时计算） */
    private Integer stockQuantity;

    private String categoryId;

    private String categoryName;

    /** ON_SHELF / INACTIVE */
    private String status;

    private String description;

    /** 商城累计销量 */
    private Integer salesCount;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
