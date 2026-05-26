package cn.aiedge.erp.supplier.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量通知结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchNotificationResult {
    
    private int successCount;
    private int failedCount;
    private List<NotificationResult> results;
}
