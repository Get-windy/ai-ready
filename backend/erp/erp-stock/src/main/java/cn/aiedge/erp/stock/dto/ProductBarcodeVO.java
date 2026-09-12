package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商品条码行 VO（一行 = 商品 × 单位）
 * <p>
 * 数据主体为 erp_product_unit（商品多单位行），条码优先取单位行 barcode，
 * 缺失时回退 erp_product_barcode 的默认条码，保证与既有条码表口径一致（不另建重复条码表）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class ProductBarcodeVO {

    /** 单位行ID（行级修改的定位键） */
    private Long unitId;

    private Long productId;

    private String imageUrl;

    private String productName;

    /** 货号 */
    private String productCode;

    /** 单位名称 */
    private String unitName;

    /** 换算率（相对基础单位） */
    private BigDecimal conversionRate;

    /** 是否基础单位 1=是 */
    private Integer isBaseUnit;

    /** 基础单位名称（用于生成「1箱=12瓶」展示） */
    private String baseUnitName;

    /** 换算关系展示文案（如「1箱=12瓶」，基础单位行为空） */
    private String conversionRelation;

    /** 商品上架状态 1=已上架 0=未上架 */
    private Integer shelfStatus;

    /** 上架状态展示文案（已上架/未上架） */
    private String shelfStatusText;

    private String barcode;

    private String barcodeType;

    private String spec;

    private String model;

    private String origin;

    /** 商品新增时间 */
    private LocalDateTime createTime;

    /** 商品显示状态 ENABLED/DISABLED */
    private String status;

    /** 最近采购日期（已入库/已完成的采购入库单最大单据日期） */
    private LocalDate lastPurchaseDate;
}
