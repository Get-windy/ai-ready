package cn.aiedge.statistics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单统计DTO
 */
@Data
public class OrderStatsDTO {
    private Long tenantId;
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer pendingOrders;
    private Integer cancelledOrders;
    private BigDecimal totalAmount;
    private BigDecimal actualAmount;
    private BigDecimal receivedAmount;
    private BigDecimal unReceivedAmount;
    private List<DailyStats> dailyStats;

    @Data
    public static class DailyStats {
        private String date;
        private Integer orderCount;
        private BigDecimal amount;
    }
}
