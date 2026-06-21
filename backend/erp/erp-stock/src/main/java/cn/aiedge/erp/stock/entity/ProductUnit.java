package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品多单位换算
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_unit")
public class ProductUnit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long productId;

    private String unitName;

    private Integer isBaseUnit;

    private BigDecimal conversionRate;

    private String barcode;

    private Integer sortOrder;

    /** 单位类型: SMALL=小单位 MEDIUM=中单位 LARGE=大单位 */
    private String unitType;

    /** 预设进价 */
    private BigDecimal presetPurchasePrice;

    /** 参考成本 */
    private BigDecimal referenceCost;

    /** 最近进价 */
    private BigDecimal recentPurchasePrice;

    /** 批发价 */
    private BigDecimal wholesalePrice;

    /** 零售价 */
    private BigDecimal retailPrice;

    /** 最低售价 */
    private BigDecimal minSalePrice;

    /** 最低折扣(%) */
    private BigDecimal minDiscount;

    /** 餐饮店价格 */
    private BigDecimal restaurantPrice;

    /** 食堂团餐价格 */
    private BigDecimal canteenPrice;

    /** 外围餐饮店价格 */
    private BigDecimal outerRestaurantPrice;

    /** 自助vip价格 */
    private BigDecimal selfVipPrice;

    /** 大团餐价格 */
    private BigDecimal groupMealPrice;

    /** 重点vip价格 */
    private BigDecimal keyVipPrice;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
