package cn.aiedge.erp.supplier.notification.repository;

import cn.aiedge.erp.supplier.notification.entity.SupplierNotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 供应商通知配置仓储接口
 */
@Repository
public interface SupplierNotificationConfigRepository extends JpaRepository<SupplierNotificationConfig, Long> {
    
    Optional<SupplierNotificationConfig> findBySupplierId(Long supplierId);
    
    boolean existsBySupplierId(Long supplierId);
}
