package cn.aiedge.statistics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售统计DTO
 */
@Data
public class SalesStatsDTO {
    private Long tenantId;
    private BigDecimal totalSales;
    private BigDecimal targetSales;
    private BigDecimal achievementRate;
    private Integer totalDeals;
    private BigDecimal avgDealAmount;
    private List<SalesByOwner> salesByOwner;
    private List<ProductSales> topProducts;

    @Data
    public static class SalesByOwner {
        private Long ownerId;
        private String ownerName;
        private BigDecimal sales;
        private Integer dealCount;
    }

    @Data
    public static class ProductSales {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal amount;
    }
}
