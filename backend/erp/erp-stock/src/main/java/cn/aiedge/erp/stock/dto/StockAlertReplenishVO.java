package cn.aiedge.erp.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存预警补货行（按商品×仓库一行）
 * <p>
 * 对齐对标系统「库存预警补货」页 25 列。口径说明：
 * <ul>
 *   <li>账面库存：erp_stock.quantity（当前账面结存）</li>
 *   <li>待发货数量：销售出库单明细 pending_quantity 汇总（已销售未出库，占用库存）</li>
 *   <li>待收货数量：采购订单明细 (quantity - received_quantity) 汇总（在途采购，status IN (2,3,5) 且未关闭未取消）</li>
 *   <li>缺货数量 = 库存上限 + 待发货 - 账面库存 - 待收货</li>
 *   <li>最近采购日期/供货商/单价：该商品+仓库最近一条采购订单明细</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "库存预警补货行")
public class StockAlertReplenishVO {

    @Schema(description = "行主键（productId+warehouseId 组合）")
    private String id;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库编号")
    private String warehouseCode;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "货号（优先 product_code_alias，无则回退 product_code）")
    private String productCode;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "重量（kg）")
    private BigDecimal weight;

    @Schema(description = "体积（m³）")
    private BigDecimal volume;

    @Schema(description = "口味")
    private String taste;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "条码")
    private String barcode;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "预警类型（缺货/下限预警/超储/正常）")
    private String alertType;

    @Schema(description = "缺货数量 = 库存上限 + 待发货 - 账面库存 - 待收货")
    private BigDecimal shortageQty;

    @Schema(description = "库存上限")
    private BigDecimal maxStock;

    @Schema(description = "库存下限")
    private BigDecimal minStock;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "待发货数量")
    private BigDecimal pendingQty;

    @Schema(description = "账面库存")
    private BigDecimal bookQty;

    @Schema(description = "待收货数量")
    private BigDecimal inTransitQty;

    @Schema(description = "最近采购日期")
    private LocalDate lastPurchaseDate;

    @Schema(description = "最近采购供货商")
    private String lastSupplierName;

    @Schema(description = "最近采购价")
    private BigDecimal lastPurchasePrice;
}
