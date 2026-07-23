package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购准备分析汇总
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchasePrepAnalysisVO {

    /** 预警中商品数（现存量 <= 安全库存，安全库存取预警配置优先于库存档案） */
    private Long alertProductCount;

    /** 缺货SKU数（现存量 <= 0） */
    private Long outOfStockSkuCount;

    /** 在途采购单数（已审批/已下达/履行中且未完全入库） */
    private Long inTransitOrderCount;

    /** 在途采购数量（订单数量 - 已入库数量的合计） */
    private BigDecimal inTransitQuantity;

    /** 在途采购金额（在途数量 * 成本的合计） */
    private BigDecimal inTransitAmount;

    /** 建议补货金额（安全库存缺口 * 成本价的合计） */
    private BigDecimal suggestReplenishAmount;
}
