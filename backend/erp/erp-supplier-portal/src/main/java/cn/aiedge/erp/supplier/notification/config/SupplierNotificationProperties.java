package cn.aiedge.erp.supplier.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 供应商通知配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "aiedge.supplier.notification")
public class SupplierNotificationProperties {
    
    private String defaultChannels = "in-app,email";
    private int maxRetryCount = 3;
    private long retryDelayMs = 5000;
    private boolean asyncEnabled = true;
}
