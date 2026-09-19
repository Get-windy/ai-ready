package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.config.DataMaintenanceProperties;
import cn.aiedge.datasource.mapper.CleanupRuleMapper;
import cn.aiedge.datasource.model.CleanupRule;
import cn.aiedge.datasource.service.CleanupRuleService;
import cn.aiedge.datasource.support.CleanupExecutor;
import cn.aiedge.datasource.support.CleanupGuard;
import cn.aiedge.datasource.support.CleanupOutcome;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 数据清理规则服务实现（真实 DELETE 执行器）
 *
 * <p>本轮（2026-09-19）把 {@code execute} 从「只置状态」改成真执行：
 * 完整读取 {@code targetTable} / {@code conditionColumn} / {@code retentionDays} 三个此前
 * **只存不用**的字段，经四重安全校验后分批 DELETE，并把预统计行数、实删行数、是否截断、
 * 失败原因写回台账（{@code last_run_*}）。
 *
 * <p>🔴 状态语义（本页最容易被搞错的一点）：
 * {@code sys_data_cleanup_rule.status} 是**启停开关**（前端开关直接写 running/paused/stopped），
 * **不是执行状态**。旧实现的 bug 正是把开关当执行态用 —— {@code execute} 把 status 置成 running，
 * 于是「执行一次」= 「把规则打开」，且 running 永不复位。现在 execute **完全不写 status**，
 * 执行结论写 {@code last_run_status} / {@code last_run_time} / {@code last_deleted_count}。
 *
 * <p>执行是**同步**的：单次删除受 {@code maxRowsPerRun} 上限约束，工作量有界，
 * 因此可以立即把真实结果返回给调用方 —— 页面无需轮询即可拿到「删了多少行」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupRuleServiceImpl implements CleanupRuleService {

    private final CleanupRuleMapper cleanupRuleMapper;
    private final CleanupGuard cleanupGuard;
    private final CleanupExecutor cleanupExecutor;
    private final DataMaintenanceProperties properties;

    @Override
    public List<CleanupRule> list(Long tenantId) {
        LambdaQueryWrapper<CleanupRule> wrapper = new LambdaQueryWrapper<CleanupRule>()
                .eq(CleanupRule::getDeleted, 0)
                .eq(tenantId != null, CleanupRule::getTenantId, tenantId)
                .orderByDesc(CleanupRule::getCreateTime);
        return cleanupRuleMapper.selectList(wrapper);
    }

    @Override
    public CleanupRule create(CleanupRule rule, Long tenantId, String createBy) {
        LocalDateTime now = LocalDateTime.now();
        rule.setTenantId(tenantId);
        rule.setCreateBy(createBy);
        rule.setUpdateBy(createBy);
        rule.setCreateTime(now);
        rule.setUpdateTime(now);
        rule.setDeleted(0);
        if (rule.getStatus() == null) rule.setStatus("stopped");
        cleanupRuleMapper.insert(rule);
        return rule;
    }

    @Override
    public CleanupRule update(Long id, CleanupRule rule, Long tenantId, String updateBy) {
        CleanupRule existing = cleanupRuleMapper.selectById(id);
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) return null;
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
        cleanupRuleMapper.updateById(existing);
        return existing;
    }

    @Override
    public boolean delete(Long id) {
        CleanupRule existing = cleanupRuleMapper.selectById(id);
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) return false;
        existing.setDeleted(1);
        existing.setUpdateTime(LocalDateTime.now());
        return cleanupRuleMapper.updateById(existing) > 0;
    }

    // ==================== 立即执行 ====================

    @Override
    public Map<String, Object> execute(Long id, boolean confirm, boolean dryRun, Long tenantId, String operator) {
        // ① 二次确认闸门（缺省拒绝）
        if (!confirm) {
            return fail("清理是删除数据的破坏性操作，必须显式二次确认（confirm=true）后才能执行");
        }

        CleanupRule rule = loadOwned(id, tenantId);
        if (rule == null) {
            return fail("清理规则不存在");
        }

        // ② 四重安全校验：白名单 → 标识符正则 → 存在性 → 条件列必须是时间类型
        String table;
        String column;
        int retentionDays;
        try {
            table = cleanupGuard.validateTable(rule.getTargetTable());
            column = cleanupGuard.validateConditionColumn(table, rule.getConditionColumn());
            retentionDays = cleanupGuard.validateRetentionDays(rule.getRetentionDays());
        } catch (IllegalArgumentException e) {
            // 被拒也要留痕：last_run_status=rejected 让人一眼看出「规则配错了」而不是「没有执行过」
            writeResult(rule, tenantId, "rejected", 0L, "前置校验拒绝: " + e.getMessage());
            log.warn("清理执行被拒绝: ruleId={}, reason={}", id, e.getMessage());
            return fail("清理前置校验未通过: " + e.getMessage());
        }

        // ③ 干跑：只预统计，不删除（用于执行前确认影响面）
        if (dryRun) {
            long planned = cleanupExecutor.countMatched(table, column, retentionDays);
            boolean partitioned = cleanupGuard.isPartitioned(table);
            Map<String, Object> body = success("干跑完成：将删除 " + planned + " 行（本次未删除任何数据）");
            body.put("dryRun", true);
            body.put("targetTable", table);
            body.put("conditionColumn", column);
            body.put("retentionDays", retentionDays);
            body.put("plannedRows", planned);
            body.put("partitioned", partitioned);
            return body;
        }

        // ④ 真实执行：分批 DELETE + 单次上限保护
        CleanupOutcome outcome;
        try {
            outcome = cleanupExecutor.execute(table, column, retentionDays);
        } catch (RuntimeException e) {
            log.error("清理执行失败: ruleId={}, table={}", id, table, e);
            writeResult(rule, tenantId, "failed", 0L,
                    "执行失败: " + e.getMessage() + "；目标表 " + table + "，条件列 " + column + "，保留 " + retentionDays + " 天");
            return fail("清理执行失败: " + e.getMessage());
        }

        writeResult(rule, tenantId, "success", outcome.deletedRows(), outcome.message());
        log.warn("清理规则执行完成: ruleId={}, operator={}, table={}, deleted={}, truncated={}",
                id, operator, outcome.table(), outcome.deletedRows(), outcome.truncated());

        Map<String, Object> body = success("清理执行完成：实际删除 " + outcome.deletedRows() + " 行"
                + (outcome.truncated() ? "（已达单次上限，剩余数据未删除）" : ""));
        body.put("dryRun", false);
        body.put("targetTable", outcome.table());
        body.put("conditionColumn", outcome.column());
        body.put("retentionDays", outcome.retentionDays());
        body.put("plannedRows", outcome.plannedRows());
        body.put("deletedRows", outcome.deletedRows());
        body.put("remainingRows", outcome.remainingRows());
        body.put("truncated", outcome.truncated());
        body.put("partitioned", outcome.partitioned());
        body.put("durationMs", outcome.durationMs());
        body.put("maxRowsPerRun", properties.getCleanup().getMaxRowsPerRun());
        return body;
    }

    @Override
    public List<Map<String, Object>> allowedTables() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> allowed = properties.getCleanup().getAllowedTables();
        if (allowed == null) {
            return result;
        }
        for (String raw : allowed) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("table", raw.trim().toLowerCase(Locale.ROOT));
            // 是否真实存在：白名单可能因表已下线而过期，暴露出来便于运维清理配置
            try {
                item.put("exists", cleanupGuard.validateTable(raw.trim()) != null);
            } catch (IllegalArgumentException e) {
                item.put("exists", false);
            }
            result.add(item);
        }
        return result;
    }

    // ==================== 辅助 ====================

    private CleanupRule loadOwned(Long id, Long tenantId) {
        if (id == null) {
            return null;
        }
        CleanupRule rule = cleanupRuleMapper.selectById(id);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted())) {
            return null;
        }
        if (tenantId != null && rule.getTenantId() != null && !tenantId.equals(rule.getTenantId())) {
            log.warn("跨租户访问清理规则被拒绝: id={}, ruleTenant={}, requestTenant={}",
                    id, rule.getTenantId(), tenantId);
            return null;
        }
        return rule;
    }

    /**
     * 回写执行结果
     *
     * <p>刻意**不写 status**（那是启停开关）；执行结论落在 last_run_* 三列 + 明细文本上。
     */
    private void writeResult(CleanupRule rule, Long tenantId, String status, Long deletedCount, String detail) {
        String truncated = detail != null && detail.length() > 4000 ? detail.substring(0, 4000) : detail;
        LambdaUpdateWrapper<CleanupRule> wrapper = new LambdaUpdateWrapper<CleanupRule>()
                .eq(CleanupRule::getId, rule.getId())
                .eq(tenantId != null, CleanupRule::getTenantId, tenantId)
                .set(CleanupRule::getLastRunStatus, status)
                .set(CleanupRule::getLastRunTime, LocalDateTime.now())
                .set(CleanupRule::getLastDeletedCount, deletedCount)
                .set(CleanupRule::getLastRunResult, truncated);
        cleanupRuleMapper.update(null, wrapper);
    }

    private Map<String, Object> success(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        return body;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        return body;
    }
}
