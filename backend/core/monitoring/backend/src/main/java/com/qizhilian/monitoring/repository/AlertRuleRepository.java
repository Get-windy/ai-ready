package com.qizhilian.monitoring.repository;

import com.qizhilian.monitoring.entity.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, Long> {

    List<AlertRuleEntity> findByEnabledTrue();

    List<AlertRuleEntity> findByMetricName(String metricName);

    List<AlertRuleEntity> findByRuleType(String ruleType);

    List<AlertRuleEntity> findBySeverity(String severity);
}
