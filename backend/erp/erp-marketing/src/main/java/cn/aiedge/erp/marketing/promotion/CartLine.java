package cn.aiedge.erp.marketing.promotion;

import lombok.Data;

import java.math.BigDecimal;

/** 促销引擎输入：一行购物车/单据明细 */
@Data
public class CartLine {

    /** 行号（1 起，与单据明细 line_no 对齐） */
    private Integer lineNo;
    private Long productId;
    private String productName;
    /** 商品分类 id（用于按分类限定促销范围） */
    private Long categoryId;
    private BigDecimal quantity;
    /** 成交单价（服务端已按价格体系算出的 price，供特价比较与金额计算） */
    private BigDecimal unitPrice;

    /** 行金额 = 数量 × 成交单价（引擎内部计算，调用方可留空） */
    public BigDecimal lineAmount() {
        if (quantity == null || unitPrice == null) return BigDecimal.ZERO;
        return quantity.multiply(unitPrice);
    }
}
