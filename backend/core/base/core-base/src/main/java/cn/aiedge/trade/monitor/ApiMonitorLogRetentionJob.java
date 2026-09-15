package cn.aiedge.trade.monitor;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.trade.monitor.mapper.ApiAccessLogMapper;
import cn.aiedge.trade.monitor.service.ApiMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 调用日志留存清理（《API监控开发文档》§3.4 保留 30–90 天 / 配置落位 `monitor.log.retention-days`）
 *
 * <p>按租户逐个执行，避免无租户上下文时多租户条件缺失而**误删全量数据**；
 * 保留天数未配置或为 0 时**不做任何删除**（默认安全）。手动入口见
 * `POST /api/trade/api-monitor/clean-expired`。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiMonitorLogRetentionJob {

    private final ApiMonitorService apiMonitorService;
    private final ApiAccessLogMapper apiAccessLogMapper;

    /** 每日 03:50 按保留策略清理 */
    @Scheduled(cron = "0 50 3 * * ?")
    public void purgeExpired() {
        List<Long> tenantIds = apiAccessLogMapper.selectDistinctTenantIds();
        if (tenantIds.isEmpty()) {
            return;
        }
        int total = 0;
        for (Long tenantId : tenantIds) {
            try {
                MyBatisPlusConfig.setTempTenantId(tenantId);
                total += apiMonitorService.cleanExpired();
            } catch (Exception e) {
                log.warn("[API监控] 租户 {} 调用日志清理失败: {}", tenantId, e.getMessage());
            } finally {
                MyBatisPlusConfig.clearTempTenantId();
            }
        }
        if (total > 0) {
            log.info("[API监控] 调用日志留存清理完成，共删除 {} 条（涉及 {} 个租户）", total, tenantIds.size());
        }
    }
}
