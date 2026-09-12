package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品价格批量修改入参（对标「批量修改」弹窗）
 * <p>
 * 一次可对选中的商品单位行修改多个价格项；每个价格项支持「直接改价」或「按规则改价」
 * （基础价 ×|+|-|÷ 数值，如「零售价*0.9」）。
 */
@Data
public class BatchModifyPriceDTO {

    /** 选中的商品单位行 ID */
    private List<Long> unitIds;

    /** 价格修改项 */
    private List<PriceModifyItem> items;

    @Data
    public static class PriceModifyItem {

        /** 价格字段：wholesalePrice/retailPrice/minSalePrice/minDiscount/presetPurchasePrice/referenceCost/gradePrice1..8 */
        private String field;

        /** 修改方式：FIXED=直接改价，RULE=按规则改价 */
        private String mode;

        /** 直接改价时的目标值 */
        private BigDecimal value;

        /** 规则模式：基础价字段（同 field 取值域） */
        private String basePriceField;

        /** 规则模式：计算符 * + - / */
        private String calcOperator;

        /** 规则模式：计算数 */
        private BigDecimal calcValue;
    }
}
