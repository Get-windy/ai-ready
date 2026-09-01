package cn.aiedge.erp.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预警查询行（触发上下限预警的商品清单）
 * <p>
 * 对齐对标系统「仓储 → 库存预警 → 预警查询」页（默认 9 列，全量可配置列 15 列）。
 * 数据来源：以 erp_stock_alert_config（预警阈值配置）为主，联商品主数据 erp_product 取
 * 商品属性，联 erp_stock 汇总当前账面库存，按配置的上下限阈值判断是否触发预警。
 * <ul>
 *   <li>下限预警（库存不足）：enable_low_stock_alert = 1 且 账面库存 &lt; 库存下限</li>
 *   <li>上限预警（库存积压）：enable_over_stock_alert = 1 且 账面库存 &gt; 库存上限</li>
 *   <li>comparisonQty（账面库存口径）默认取 erp_stock.quantity，预警设置页可配置比较口径</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "预警查询行")
public class StockAlertQueryVO {

    @Schema(description = "预警配置ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "货号（商品编码，product_code_alias 优先）")
    private String productCode;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "口味")
    private String taste;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "小单位")
    private String smallUnit;

    @Schema(description = "条码")
    private String barcode;

    @Schema(description = "规格")
    private String spec;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "预警仓库ID")
    private Long warehouseId;

    @Schema(description = "预警仓库")
    private String warehouseName;

    @Schema(description = "库存上限")
    private BigDecimal maxStock;

    @Schema(description = "库存下限")
    private BigDecimal minStock;

    @Schema(description = "安全库存")
    private BigDecimal safetyStock;

    @Schema(description = "账面库存")
    private BigDecimal bookQty;

    @Schema(description = "差异数量（下限预警=最小值-账面；上限预警=账面-最大值，可正可负）")
    private BigDecimal diffQty;

    @Schema(description = "预警类型：LOW_STOCK=下限预警，OVER_STOCK=上限预警")
    private String alertType;

    @Schema(description = "说明")
    private String remark;
}
