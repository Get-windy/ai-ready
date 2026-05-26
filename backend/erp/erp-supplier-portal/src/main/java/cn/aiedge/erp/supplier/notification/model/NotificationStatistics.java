package cn.aiedge.erp.supplier.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 通知统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatistics {
    
    private long totalCount;
    private long successCount;
    private long readCount;
    private double successRate;
    private double readRate;
    private Map<String, Long> templateStatistics;
    private Map<String, Long> channelStatistics;
}
