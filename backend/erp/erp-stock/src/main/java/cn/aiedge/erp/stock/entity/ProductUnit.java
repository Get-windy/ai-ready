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

    /** 价格等级1 */
    private BigDecimal gradePrice1;

    /** 价格等级2 */
    private BigDecimal gradePrice2;

    /** 价格等级3 */
    private BigDecimal gradePrice3;

    /** 价格等级4 */
    private BigDecimal gradePrice4;

    /** 价格等级5 */
    private BigDecimal gradePrice5;

    /** 价格等级6 */
    private BigDecimal gradePrice6;

    /** 价格等级7 */
    private BigDecimal gradePrice7;

    /** 价格等级8 */
    private BigDecimal gradePrice8;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
