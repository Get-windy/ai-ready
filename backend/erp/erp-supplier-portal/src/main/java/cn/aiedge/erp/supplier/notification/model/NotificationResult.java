package cn.aiedge.erp.supplier.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {
    
    private boolean success;
    private String message;
    private String notificationId;
    
    public static NotificationResult success() {
        return NotificationResult.builder()
            .success(true)
            .message("发送成功")
            .build();
    }
    
    public static NotificationResult success(String notificationId) {
        return NotificationResult.builder()
            .success(true)
            .message("发送成功")
            .notificationId(notificationId)
            .build();
    }
    
    public static NotificationResult failed(String message) {
        return NotificationResult.builder()
            .success(false)
            .message(message)
            .build();
    }
}
