package cn.aiedge.erp.supplier.notification.repository;

import cn.aiedge.erp.supplier.notification.entity.SupplierNotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 供应商通知记录仓储接口
 */
@Repository
public interface SupplierNotificationRecordRepository extends JpaRepository<SupplierNotificationRecord, Long> {
    
    List<SupplierNotificationRecord> findBySupplierIdAndCreatedAtBetween(
            Long supplierId, LocalDateTime start, LocalDateTime end);
    
    long countBySupplierIdAndCreatedAtBetween(
            Long supplierId, LocalDateTime start, LocalDateTime end);
    
    long countBySupplierIdAndStatusAndCreatedAtBetween(
            Long supplierId, String status, LocalDateTime start, LocalDateTime end);
    
    long countBySupplierIdAndReadTimeIsNotNullAndCreatedAtBetween(
            Long supplierId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT r.templateCode, COUNT(r) FROM SupplierNotificationRecord r " +
           "WHERE r.supplierId = :supplierId AND r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.templateCode")
    List<Object[]> countByTemplateCodeGroup(
            @Param("supplierId") Long supplierId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    
    @Query("SELECT r.channels, COUNT(r) FROM SupplierNotificationRecord r " +
           "WHERE r.supplierId = :supplierId AND r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.channels")
    List<Object[]> countByChannelGroup(
            @Param("supplierId") Long supplierId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    
    default java.util.Map<String, Long> groupByTemplateCode(Long supplierId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = countByTemplateCodeGroup(supplierId, start, end);
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], (Long) row[1]);
        }
        return map;
    }
    
    default java.util.Map<String, Long> groupByChannel(Long supplierId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = countByChannelGroup(supplierId, start, end);
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], (Long) row[1]);
        }
        return map;
    }
}
