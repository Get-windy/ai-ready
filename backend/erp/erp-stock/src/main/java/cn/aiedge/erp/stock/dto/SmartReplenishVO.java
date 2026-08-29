package cn.aiedge.erp.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 智能补货行（每商品一行）
 * <p>
 * 对齐对标系统「智能补货」页 25 列。口径说明：
 * <ul>
 *   <li>销售数量/销售金额/日均销量：销售订单明细在销售日期区间内按商品汇总（仅统计有效销售订单，排除草稿/待审批/已取消）</li>
 *   <li>采购数量/采购金额：采购订单明细在销售日期区间内按商品汇总（仅统计有效采购订单，排除草稿/待审批/已取消/已关闭）</li>
 *   <li>待收货数量：采购订单明细 (quantity - received_quantity) 汇总（在途采购，status IN (2,3,5) 且未关闭未取消，当前口径）</li>
 *   <li>待发货数量：销售出库单明细 pending_quantity 汇总（已售未出库，占用库存，当前口径）</li>
 *   <li>账面库存：erp_stock.quantity 汇总（当前账面结存）</li>
 *   <li>可用库存：erp_stock.available_quantity 汇总（总数量-冻结数量）</li>
 *   <li>账面/可用库存换算结果：按该商品最大非基础单位的 conversion_rate 换算（无多单位时等于原值）</li>
 *   <li>计划采购数量 = 备货天数 × 日均销量 + 待发货数量 - 待收货数量 - 账面库存</li>
 *   <li>最近销售日期/最近进货日期：商品在区间内最近一笔销售/采购订单日期</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "智能补货行")
public class SmartReplenishVO {

    @Schema(description = "行主键（productId）")
    private String id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品图片")
    private String image;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "货号（优先 product_code_alias，无则回退 product_code）")
    private String productCode;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "条码")
    private String barcode;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "销售数量（区间内销售订单明细汇总）")
    private BigDecimal salesQty;

    @Schema(description = "销售金额（区间内销售订单明细含税金额汇总）")
    private BigDecimal salesAmount;

    @Schema(description = "采购金额（区间内采购订单明细含税金额汇总）")
    private BigDecimal purchaseAmount;

    @Schema(description = "日均销量 = 销售数量 / 区间天数")
    private BigDecimal avgDailySales;

    @Schema(description = "备货天数")
    private Integer stockDays;

    @Schema(description = "待收货数量（采购在途）")
    private BigDecimal inTransitQty;

    @Schema(description = "待发货数量（销售出库未发货）")
    private BigDecimal pendingShipQty;

    @Schema(description = "采购数量（区间内采购订单明细汇总）")
    private BigDecimal purchaseQty;

    @Schema(description = "账面库存")
    private BigDecimal bookQty;

    @Schema(description = "账面库存换算结果")
    private BigDecimal bookQtyConverted;

    @Schema(description = "计划采购数量 = 备货天数×日均销量+待发货-待收货-账面库存")
    private BigDecimal planPurchaseQty;

    @Schema(description = "可用库存")
    private BigDecimal availableQty;

    @Schema(description = "可用库存换算结果")
    private BigDecimal availableQtyConverted;

    @Schema(description = "最近销售日期")
    private LocalDate lastSaleDate;

    @Schema(description = "最近进货日期")
    private LocalDate lastPurchaseDate;
}
