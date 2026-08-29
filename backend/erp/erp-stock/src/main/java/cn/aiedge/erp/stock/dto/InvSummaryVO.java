package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 进销存汇总报表行
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class InvSummaryVO {

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

    /** 期初数量（startDate之前的净变动） */
    private BigDecimal openingQty;

    /** 期初金额（按成本价） */
    private BigDecimal openingAmt;

    /** 期间入库数量（采购入库+报溢+调拨入+盘盈） */
    private BigDecimal inQty;

    /** 期间入库金额 */
    private BigDecimal inAmt;

    /** 期间出库数量（销售出库+报损+调拨出+盘亏，正数表示） */
    private BigDecimal outQty;

    /** 期间出库金额 */
    private BigDecimal outAmt;

    /** 结存数量 = 期初 + 入 - 出 */
    private BigDecimal closingQty;

    /** 结存金额 = 期初金额 + 入金额 - 出金额 */
    private BigDecimal closingAmt;

    // ═══ 五类入库分项数量（对标：采购入库/调拨入库/其他入库/借进入库/销退入库） ═══
    private BigDecimal purchaseInQty;
    private BigDecimal transferInQty;
    private BigDecimal otherInQty;
    private BigDecimal borrowInQty;
    private BigDecimal saleReturnInQty;

    // ═══ 五类出库分项数量（对标：销售出库/调拨出库/其他出库/借出出库/采退出库） ═══
    private BigDecimal saleOutQty;
    private BigDecimal transferOutQty;
    private BigDecimal otherOutQty;
    private BigDecimal borrowOutQty;
    private BigDecimal purchaseReturnOutQty;
}
