package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存变动流水报表行
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class StockFlowVO {

    /** 变动时间 */
    private LocalDateTime moveTime;

    /** 单据类型（PURCHASE_IN采购入库/SALE_OUT销售出库/TRANSFER_IN调拨入库/TRANSFER_OUT调拨出库/OVERFLOW_IN报溢入库/DAMAGE_OUT报损出库/CHECK_ADJUST盘点调整） */
    private String docType;

    /** 单据类型中文名 */
    private String docTypeName;

    /** 单据编号 */
    private String docNo;

    /** 商品ID */
    private Long productId;

    /** 商品编码 */
    private String productCode;

    /** 商品名称 */
    private String productName;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 变动数量（正=入库，负=出库） */
    private BigDecimal qty;

    /** 单位成本 */
    private BigDecimal unitCost;

    /** 变动金额 = qty * unitCost */
    private BigDecimal amount;

    /** 变动后结存（按单据变动历史累计推算，期初无单据时为相对口径） */
    private BigDecimal balanceAfter;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;
}
