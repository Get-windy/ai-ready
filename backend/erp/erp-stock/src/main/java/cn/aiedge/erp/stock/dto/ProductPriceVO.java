package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商品价格管理（子标签「商品价格批量修改」）行 VO
 * <p>
 * 一行 = 商品 × 单位；价格口径直接取 {@code erp_product_unit} 的价格列（零售价/批发价/最低售价/
 * 最低折扣/预设进价/参考成本/最近进价 + 8 个价格等级），商品信息取 {@code erp_product}，
 * 账面库存取库存表汇总，最近进货日期取已入库采购单据最大值。与对标 29 列一一对应。
 */
@Data
public class ProductPriceVO {

    private Long unitId;

    private Long productId;

    // ── 基础信息 ──
    private String imageUrl;

    /** 上架状态：1=已上架 0=未上架 */
    private Integer shelfStatus;

    private String productCode;

    private String productName;

    private String unitName;

    private String brand;

    private String conversionRelation;

    private String barcode;

    private String spec;

    private String model;

    private String origin;

    // ── 进价/成本 ──
    private BigDecimal recentPurchasePrice;

    private BigDecimal presetPurchasePrice;

    private BigDecimal referenceCost;

    private BigDecimal costAvgPrice;

    // ── 库存 ──
    private BigDecimal stockQty;

    private LocalDate lastPurchaseDate;

    // ── 售价 ──
    private BigDecimal wholesalePrice;

    private BigDecimal minDiscount;

    private BigDecimal minSalePrice;

    private BigDecimal retailPrice;

    // ── 8 个价格等级（自定义命名，取自 erp_product_grade） ──
    private BigDecimal gradePrice1;

    private BigDecimal gradePrice2;

    private BigDecimal gradePrice3;

    private BigDecimal gradePrice4;

    private BigDecimal gradePrice5;

    private BigDecimal gradePrice6;

    private BigDecimal gradePrice7;

    private BigDecimal gradePrice8;

    /** 上架状态文案 */
    private String shelfStatusText;

    // ── 换算关系（与《商品条码》同口径，服务层拼装） ──
    private BigDecimal conversionRate;

    private Integer isBaseUnit;

    private String baseUnitName;

    private LocalDateTime updateTime;
}
