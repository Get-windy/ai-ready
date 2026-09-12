package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户指定价设置（商品价格管理 → 子标签「客户指定价设置」）
 * <p>
 * 复用既有表 {@code erp_customer_product_price}（客户 × 商品 价格单一口径），
 * 迁移 V11.158.0 补充商品/单位快照与价格规则列，避免另建重复价格表。
 */
@Data
@Accessors(chain = true)
@TableName("erp_customer_product_price")
public class CustomerProductPrice {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long customerId;

    private Long productId;

    /** 价格类型：SPECIFIED=指定价 / GRADE=按等级折扣 */
    private String priceType;

    /** 指定价格 */
    private BigDecimal price;

    private Integer minOrderQty;

    private Integer isActive;

    private LocalDateTime effectiveDate;

    private LocalDateTime expireDate;

    // ── V11.158.0 补充列 ──
    private String productCode;

    private String productName;

    private Long categoryId;

    private String categoryName;

    private Long unitId;

    private String unitName;

    /** 基础价类型：零售价 / 批发价 / 价格等级名 */
    private String basePriceType;

    /** 计算符：+ - * / */
    private String calcOperator;

    private BigDecimal calcValue;

    /** 价格规则展示文本 */
    private String priceRule;

    private String remark;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
