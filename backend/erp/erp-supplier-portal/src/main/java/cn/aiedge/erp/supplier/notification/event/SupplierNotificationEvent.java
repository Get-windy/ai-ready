package cn.aiedge.erp.supplier.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 供应商通知事件
 */
@Getter
public class SupplierNotificationEvent extends ApplicationEvent {
    
    private final Long supplierId;
    private final String templateCode;
    private final String status;
    
    public SupplierNotificationEvent(Long supplierId, String templateCode, String status) {
        super(supplierId);
        this.supplierId = supplierId;
        this.templateCode = templateCode;
        this.status = status;
    }
}
