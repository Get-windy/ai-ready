package cn.aiedge.erp.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 采购订单统计DTO
 */
@Data
public class PurchaseOrderStatisticsDTO {
    
    // 时间范围
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // 采购总览
    private Integer totalOrders;
    private Integer totalItems;
    private BigDecimal totalAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalDiscountAmount;
    private BigDecimal totalFinalAmount;
    
    // 按状态统计
    private Map<String, Integer> ordersByStatus;
    private Map<String, BigDecimal> amountByStatus;
    
    // 按采购类型统计
    private Map<String, Integer> ordersByPurchaseType;
    private Map<String, BigDecimal> amountByPurchaseType;
    
    // 按供应商统计
    private List<SupplierStatistic> topSuppliersByAmount;
    private List<SupplierStatistic> topSuppliersByQuantity;
    
    // 按产品统计
    private List<ProductStatistic> topProductsByAmount;
    private List<ProductStatistic> topProductsByQuantity;
    
    // 按采购员统计
    private List<BuyerStatistic> topBuyersByAmount;
    private List<BuyerStatistic> topBuyersByQuantity;
    
    // 趋势分析
    private List<DailyStatistic> dailyStatistics;
    private List<MonthlyStatistic> monthlyStatistics;
    
    // 审批效率统计
    private BigDecimal averageApprovalTime;
    private Integer pendingApprovalCount;
    private Integer overdueApprovalCount;
    
    // 交付效率统计
    private BigDecimal averageDeliveryTime;
    private Integer onTimeDeliveryRate;
    private Integer delayedDeliveryCount;
    
    // 成本分析
    private BigDecimal averageUnitPrice;
    private BigDecimal priceVariation;
    private List<CostTrend> costTrends;
    
    /**
     * 供应商统计
     */
    @Data
    public static class SupplierStatistic {
        private Long supplierId;
        private String supplierName;
        private Integer orderCount;
        private Integer itemCount;
        private BigDecimal totalAmount;
        private BigDecimal averageAmount;
        private Integer onTimeDeliveryRate;
    }
    
    /**
     * 产品统计
     */
    @Data
    public static class ProductStatistic {
        private Long productId;
        private String productCode;
        private String productName;
        private String productSpec;
        private Integer purchaseCount;
        private Integer totalQuantity;
        private BigDecimal totalAmount;
        private BigDecimal averagePrice;
        private BigDecimal priceVariation;
    }
    
    /**
     * 采购员统计
     */
    @Data
    public static class BuyerStatistic {
        private Long buyerId;
        private String buyerName;
        private Integer orderCount;
        private BigDecimal totalAmount;
        private BigDecimal averageAmount;
        private Integer supplierCount;
        private BigDecimal costSaving;
    }
    
    /**
     * 每日统计
     */
    @Data
    public static class DailyStatistic {
        private String date;
        private Integer orderCount;
        private Integer itemCount;
        private BigDecimal totalAmount;
        private Integer approvedCount;
        private Integer deliveredCount;
    }
    
    /**
     * 月度统计
     */
    @Data
    public static class MonthlyStatistic {
        private String month;
        private Integer orderCount;
        private Integer itemCount;
        private BigDecimal totalAmount;
        private BigDecimal averageAmount;
        private Integer supplierCount;
        private BigDecimal monthOverMonthGrowth;
    }
    
    /**
     * 成本趋势
     */
    @Data
    public static class CostTrend {
        private String period;
        private String productCode;
        private String productName;
        private BigDecimal averagePrice;
        private BigDecimal priceChange;
        private BigDecimal priceChangePercent;
        private Integer purchaseCount;
    }
}