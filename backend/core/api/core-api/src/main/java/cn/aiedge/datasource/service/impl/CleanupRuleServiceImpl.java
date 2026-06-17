package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.model.CleanupRule;
import cn.aiedge.datasource.service.CleanupRuleService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 数据清理规则服务实现
 */
@Service
public class CleanupRuleServiceImpl implements CleanupRuleService {

    private final List<CleanupRule> ruleList = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @PostConstruct
    public void init() {
        CleanupRule cr1 = new CleanupRule();
        cr1.setId(idCounter.getAndIncrement());
        cr1.setRuleName("日志表清理");
        cr1.setTargetTable("sys_log");
        cr1.setConditionColumn("create_time");
        cr1.setRetentionDays(90);
        cr1.setCronExpression("0 0 4 * * ?");
        cr1.setStatus("running");
        cr1.setDescription("保留90天内的系统日志，每天凌晨4点清理");
        cr1.setTenantId(1L);
        cr1.setCreateTime(LocalDateTime.now().minusDays(60));
        cr1.setUpdateTime(LocalDateTime.now().minusDays(1));
        cr1.setCreateBy("admin");
        cr1.setUpdateBy("admin");
        cr1.setDeleted(false);
        ruleList.add(cr1);

        CleanupRule cr2 = new CleanupRule();
        cr2.setId(idCounter.getAndIncrement());
        cr2.setRuleName("审计日志清理");
        cr2.setTargetTable("sys_audit_log");
        cr2.setConditionColumn("create_time");
        cr2.setRetentionDays(180);
        cr2.setCronExpression("0 30 4 * * ?");
        cr2.setStatus("running");
        cr2.setDescription("保留180天内的审计日志，每天凌晨4:30清理");
        cr2.setTenantId(1L);
        cr2.setCreateTime(LocalDateTime.now().minusDays(45));
        cr2.setUpdateTime(LocalDateTime.now().minusDays(1));
        cr2.setCreateBy("admin");
        cr2.setUpdateBy("admin");
        cr2.setDeleted(false);
        ruleList.add(cr2);

        CleanupRule cr3 = new CleanupRule();
        cr3.setId(idCounter.getAndIncrement());
        cr3.setRuleName("临时表清理");
        cr3.setTargetTable("sys_temp_data");
        cr3.setConditionColumn("expire_time");
        cr3.setRetentionDays(7);
        cr3.setCronExpression("0 0 */6 * * ?");
        cr3.setStatus("paused");
        cr3.setDescription("保留7天内的临时数据，每6小时清理（已暂停）");
        cr3.setTenantId(1L);
        cr3.setCreateTime(LocalDateTime.now().minusDays(30));
        cr3.setUpdateTime(LocalDateTime.now().minusDays(10));
        cr3.setCreateBy("admin");
        cr3.setUpdateBy("admin");
        cr3.setDeleted(false);
        ruleList.add(cr3);

        CleanupRule cr4 = new CleanupRule();
        cr4.setId(idCounter.getAndIncrement());
        cr4.setRuleName("历史订单归档清理");
        cr4.setTargetTable("sys_order_history");
        cr4.setConditionColumn("archive_time");
        cr4.setRetentionDays(365);
        cr4.setCronExpression("0 0 5 1 * ?");
        cr4.setStatus("stopped");
        cr4.setDescription("保留1年内的历史订单数据，每月1日凌晨5点清理（已停止）");
        cr4.setTenantId(2L);
        cr4.setCreateTime(LocalDateTime.now().minusDays(15));
        cr4.setUpdateTime(LocalDateTime.now().minusDays(5));
        cr4.setCreateBy("analyst");
        cr4.setUpdateBy("analyst");
        cr4.setDeleted(false);
        ruleList.add(cr4);
    }

    @Override
    public List<CleanupRule> list(Long tenantId) {
        return ruleList.stream()
                .filter(r -> !r.getDeleted())
                .filter(r -> tenantId == null || tenantId.equals(r.getTenantId()))
                .collect(Collectors.toList());
    }

    @Override
    public CleanupRule create(CleanupRule rule, Long tenantId, String createBy) {
        rule.setId(idCounter.getAndIncrement());
        rule.setTenantId(tenantId);
        rule.setCreateBy(createBy);
        rule.setUpdateBy(createBy);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setDeleted(false);
        if (rule.getStatus() == null) {
            rule.setStatus("stopped");
        }
        ruleList.add(rule);
        return rule;
    }

    @Override
    public CleanupRule update(Long id, CleanupRule rule, Long tenantId, String updateBy) {
        CleanupRule existing = ruleList.stream()
                .filter(r -> id.equals(r.getId()) && !r.getDeleted())
                .findFirst()
                .orElse(null);
        if (existing == null) {
            return null;
        }
        if (rule.getRuleName() != null) existing.setRuleName(rule.getRuleName());
        if (rule.getTargetTable() != null) existing.setTargetTable(rule.getTargetTable());
        if (rule.getConditionColumn() != null) existing.setConditionColumn(rule.getConditionColumn());
        if (rule.getRetentionDays() != null) existing.setRetentionDays(rule.getRetentionDays());
        if (rule.getCronExpression() != null) existing.setCronExpression(rule.getCronExpression());
        if (rule.getStatus() != null) existing.setStatus(rule.getStatus());
        if (rule.getDescription() != null) existing.setDescription(rule.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setUpdateBy(updateBy);
        if (tenantId != null) existing.setTenantId(tenantId);
        return existing;
    }

    @Override
    public boolean delete(Long id) {
        CleanupRule existing = ruleList.stream()
                .filter(r -> id.equals(r.getId()) && !r.getDeleted())
                .findFirst()
                .orElse(null);
        if (existing == null) {
            return false;
        }
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean execute(Long id) {
        CleanupRule rule = ruleList.stream()
                .filter(r -> id.equals(r.getId()) && !r.getDeleted())
                .findFirst()
                .orElse(null);
        if (rule == null) {
            return false;
        }
        rule.setStatus("running");
        rule.setUpdateTime(LocalDateTime.now());
        return true;
    }
}
