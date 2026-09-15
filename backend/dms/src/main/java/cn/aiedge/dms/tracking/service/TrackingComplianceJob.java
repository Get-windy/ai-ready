package cn.aiedge.dms.tracking.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 轨迹合规清理（《实时跟踪开发文档》§4 隐私与合规 / §6 待完善①）
 *
 * <p>按配置 {@code dms.tracking.retention.days} 保留历史轨迹点；**未配置（0）时不做任何删除**。
 * 清理按租户逐个执行，避免在无租户上下文时因多租户条件缺失而误删全量数据。</p>
 *
 * <p>手动入口见 {@code POST /api/dms/tracking/clean-expired}（请求线程带租户上下文）。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingComplianceJob {

    private final TrackingService trackingService;
    private final JdbcTemplate jdbcTemplate;

    /** 每日 03:40 按保留策略清理（保留天数未配置则跳过，默认安全） */
    @Scheduled(cron = "0 40 3 * * ?")
    public void purgeExpired() {
        List<Long> tenantIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT tenant_id FROM dms_tracking WHERE deleted = 0", Long.class);
        int total = 0;
        for (Long tenantId : tenantIds) {
            try {
                MyBatisPlusConfig.setTempTenantId(tenantId);
                total += trackingService.cleanExpiredByRetention();
            } catch (Exception e) {
                log.warn("[实时跟踪] 租户 {} 轨迹清理失败: {}", tenantId, e.getMessage());
            } finally {
                MyBatisPlusConfig.clearTempTenantId();
            }
        }
        if (total > 0) {
            log.info("[实时跟踪] 合规清理完成，共删除轨迹点 {} 条（涉及 {} 个租户）", total, tenantIds.size());
        }
    }
}
