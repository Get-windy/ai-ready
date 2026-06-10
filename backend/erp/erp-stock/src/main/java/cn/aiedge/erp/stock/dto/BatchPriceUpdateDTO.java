package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量更新产品价格 DTO
 */
@Data
public class BatchPriceUpdateDTO {

    /**
     * 待更新的价格项
     */
    private List<PriceItem> items;

    @Data
    public static class PriceItem {
        private Long id;
        private BigDecimal costPrice;
        private BigDecimal standardPrice;
        private BigDecimal wholesalePrice;
    }
}
