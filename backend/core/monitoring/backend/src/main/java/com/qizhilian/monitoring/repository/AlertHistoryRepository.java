package com.qizhilian.monitoring.repository;

import com.qizhilian.monitoring.entity.AlertHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistoryEntity, Long> {

    List<AlertHistoryEntity> findByRuleId(Long ruleId);

    List<AlertHistoryEntity> findByAlertStatus(String alertStatus);

    Page<AlertHistoryEntity> findByAlertStatus(String alertStatus, Pageable pageable);

    List<AlertHistoryEntity> findBySeverity(String severity);

    @Query("SELECT a FROM AlertHistoryEntity a WHERE a.triggerTime BETWEEN :start AND :end ORDER BY a.triggerTime DESC")
    List<AlertHistoryEntity> findByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT a FROM AlertHistoryEntity a WHERE a.alertStatus = 'firing' ORDER BY a.triggerTime DESC")
    List<AlertHistoryEntity> findActiveAlerts();

    @Query("SELECT COUNT(a) FROM AlertHistoryEntity a WHERE a.alertStatus = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT a.severity, COUNT(a) FROM AlertHistoryEntity a WHERE a.triggerTime >= :since GROUP BY a.severity")
    List<Object[]> countBySeveritySince(@Param("since") LocalDateTime since);
}
