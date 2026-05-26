package cn.aiedge.erp.supplier.notification.service;

import cn.aiedge.erp.supplier.notification.entity.SupplierNotificationConfig;
import cn.aiedge.erp.supplier.notification.entity.SupplierNotificationRecord;
import cn.aiedge.erp.supplier.notification.model.BatchNotificationResult;
import cn.aiedge.erp.supplier.notification.model.DateRange;
import cn.aiedge.erp.supplier.notification.model.NotificationResult;
import cn.aiedge.erp.supplier.notification.model.NotificationStatistics;
import cn.aiedge.erp.supplier.notification.model.SupplierNotificationRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 供应商通知服务接口
 */
public interface SupplierNotificationService {
    
    /**
     * 发送通知
     */
    NotificationResult sendNotification(SupplierNotificationRequest request);
    
    /**
     * 批量发送通知
     */
    CompletableFuture<BatchNotificationResult> sendBatchNotifications(List<SupplierNotificationRequest> requests);
    
    /**
     * 获取供应商通知配置
     */
    SupplierNotificationConfig getConfig(Long supplierId);
    
    /**
     * 更新供应商通知配置
     */
    void updateConfig(Long supplierId, SupplierNotificationConfig config);
    
    /**
     * 获取通知历史
     */
    List<SupplierNotificationRecord> getHistory(Long supplierId, DateRange range);
    
    /**
     * 获取通知统计
     */
    NotificationStatistics getStatistics(Long supplierId, DateRange range);
}
