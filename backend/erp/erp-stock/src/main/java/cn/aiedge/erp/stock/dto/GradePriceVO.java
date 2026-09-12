package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 级别指定价 / 客户指定价行 VO（子标签 3、4）
 * <p>
 * 规则行只落「客户级别/客户 + 商品(或分类) + 单位 + 价格规则」，商品档案侧字段
 * （条码/规格/型号/品牌/预设进价/零售价/批发价/8 个价格等级）在查询时从
 * {@code erp_product} / {@code erp_product_unit} 实时取，保证与商品档案单一口径。
 */
@Data
public class GradePriceVO {

    private Long id;

    private Long gradeId;

    /** 客户级别（子标签 3） */
    private String gradeName;

    // ── 客户（子标签 4） ──
    private Long customerId;

    private String customerName;

    private String customerCode;

    // ── 商品 / 分类 ──
    private Long productId;

    private String productCode;

    private String productName;

    private Long categoryId;

    private String categoryName;

    /** 商品/分类名称（分类行取分类名） */
    private String targetName;

    private Long unitId;

    private String unitName;

    // ── 规则 ──
    private String priceRule;

    private BigDecimal price;

    private String priceType;

    private String basePriceType;

    private String calcOperator;

    private BigDecimal calcValue;

    // ── 商品档案实时字段 ──
    private String barcode;

    private String spec;

    private String model;

    private String brand;

    private BigDecimal presetPurchasePrice;

    private BigDecimal retailPrice;

    private BigDecimal wholesalePrice;

    private BigDecimal gradePrice1;

    private BigDecimal gradePrice2;

    private BigDecimal gradePrice3;

    private BigDecimal gradePrice4;

    private BigDecimal gradePrice5;

    private BigDecimal gradePrice6;

    private BigDecimal gradePrice7;

    private BigDecimal gradePrice8;

    /** 最后修改人 */
    private Long lastModifierId;

    private String lastModifierName;

    /** 最后修改时间 */
    private LocalDateTime lastModifyTime;
}
