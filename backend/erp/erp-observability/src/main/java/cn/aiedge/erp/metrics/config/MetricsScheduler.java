package cn.aiedge.erp.metrics.config;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 指标数据采集定时任务
 *
 * <p><b>为什么要逐租户循环（2026-09-20）</b>：指标计算 SQL 一律按 {@code tenant_id} 过滤，
 * 而改造前是**写死成 1**（`MetricsCalculationServiceImpl` 的 `DEFAULT_TENANT_ID`），
 * 定时任务又没有会话上下文 —— 两边叠加的结果是「所有指标只统计平台租户」。
 * 现在采集方与计算方各改一半：调度器负责逐租户设置上下文，计算方按上下文取租户。</p>
 *
 * <p><b>必须 finally clear</b>：{@code TEMP_TENANT_ID} 是 ThreadLocal，而定时任务线程由
 * Spring 线程池复用 —— 不清理会把上一个租户的上下文留给下一次执行（串租户）。
 * 本仓既有 13 处 set 全部配了 finally clear，这里沿用同一纪律。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsScheduler {

    private final MetricsService metricsService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 实时指标采集 - 每5秒执行一次
     * 满足延迟≤3秒的要求
     *
     * <p>逐租户执行：耗时为「租户数 × 单租户采集」，当前租户量级下可接受；
     * 若将来租户显著增多，应改为按租户轮转（每次只采一个租户）。</p>
     */
    @Scheduled(fixedRate = 5000)
    public void collectRealtimeMetrics() {
        List<Long> tenantIds = listActiveTenantIds();
        for (Long tenantId : tenantIds) {
            try {
                MyBatisPlusConfig.setTempTenantId(tenantId);
                metricsService.refreshMetrics();
            } catch (Exception e) {
                // 单租户失败不影响其余租户（与「单个指标失败不影响其余指标」的既有口径一致）
                log.error("租户 {} 实时指标采集失败: {}", tenantId, e.getMessage());
            } finally {
                MyBatisPlusConfig.clearTempTenantId();
            }
        }
    }

    /**
     * 启用中的租户 id 列表。
     *
     * <p>查询失败时返回空集合（fail-closed：本轮不采集，而不是回落去采集平台租户 ——
     * 后者会把「读不到租户」变成「所有租户的看板都显示平台数据」，更难发现）。</p>
     */
    private List<Long> listActiveTenantIds() {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT id FROM sys_tenant WHERE deleted = 0 AND status = 1 ORDER BY id", Long.class);
        } catch (Exception e) {
            log.warn("读取启用租户列表失败，本轮跳过指标采集: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 分钟级指标聚合 - 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * *")
    public void aggregateMinuteMetrics() {
        try {
            log.debug("Aggregating minute metrics...");
            // 聚合逻辑在MetricsAggregationService中实现
        } catch (Exception e) {
            log.error("Error aggregating minute metrics: {}", e.getMessage());
        }
    }

    /**
     * 小时级指标聚合 - 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * *")
    public void aggregateHourMetrics() {
        try {
            log.info("Aggregating hour metrics...");
            // 聚合逻辑在MetricsAggregationService中实现
        } catch (Exception e) {
            log.error("Error aggregating hour metrics: {}", e.getMessage());
        }
    }

    /**
     * 日级指标聚合 - 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void aggregateDayMetrics() {
        try {
            log.info("Aggregating day metrics...");
            // 聚合逻辑在MetricsAggregationService中实现
        } catch (Exception e) {
            log.error("Error aggregating day metrics: {}", e.getMessage());
        }
    }
}
