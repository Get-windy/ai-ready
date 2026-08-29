package cn.aiedge.erp.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 缺货补货行（按商品×仓库一行）
 * <p>
 * 对齐对标系统「缺货补货」页 16 列。口径说明：
 * <ul>
 *   <li>订单数量/价税合计/已发货/待发货：销售订单明细按商品×仓库汇总（仅统计有效销售订单，排除草稿/待审批/已取消）</li>
 *   <li>待收货数量：采购订单明细 (quantity - received_quantity) 汇总（在途采购，status IN (2,3,5) 且未关闭未取消）</li>
 *   <li>账面库存：erp_stock.quantity（当前账面结存）</li>
 *   <li>缺货数量：按前端选择口径计算，非负（不外发缺货为 0）</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "缺货补货行")
public class ShortageReplenishVO {

    @Schema(description = "行主键（productId+warehouseId 组合）")
    private String id;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品图片")
    private String image;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品货号")
    private String productCode;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "订单数量（销售订单明细汇总）")
    private BigDecimal orderQty;

    @Schema(description = "价税合计（销售订单明细含税金额汇总）")
    private BigDecimal amountWithTax;

    @Schema(description = "已发货数量")
    private BigDecimal shippedQty;

    @Schema(description = "待发货数量")
    private BigDecimal unshippedQty;

    @Schema(description = "待收货数量（采购在途）")
    private BigDecimal inTransitQty;

    @Schema(description = "账面库存")
    private BigDecimal bookQty;

    @Schema(description = "缺货数量（按口径计算，非负）")
    private BigDecimal shortageQty;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "最近采购供货商（供应商过滤辅助）")
    private String supplierName;
}
