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

    // ── 以下字段映射 erp_product_unit 表的 grade_price_1~8 列 ──
    // ⚠️ 真实列名是 grade_price_1（数字前有下划线），而 MyBatis-Plus 默认驼峰映射会得到
    //    grade_price1，两者不匹配 → 必须显式 @TableField 指定列名，否则单位查询/保存直接报
    //    "字段 grade_price1 不存在"。此前的实体漏了该注解，是商品单位等级价长期不可用的根因。
    @TableField("grade_price_1")
    private BigDecimal gradePrice1;

    @TableField("grade_price_2")
    private BigDecimal gradePrice2;

    @TableField("grade_price_3")
    private BigDecimal gradePrice3;

    @TableField("grade_price_4")
    private BigDecimal gradePrice4;

    @TableField("grade_price_5")
    private BigDecimal gradePrice5;

    @TableField("grade_price_6")
    private BigDecimal gradePrice6;

    @TableField("grade_price_7")
    private BigDecimal gradePrice7;

    @TableField("grade_price_8")
    private BigDecimal gradePrice8;

    /** 重量（kg，单位级） */
    private BigDecimal weight;

    /** 体积（m³，单位级） */
    private BigDecimal volume;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
