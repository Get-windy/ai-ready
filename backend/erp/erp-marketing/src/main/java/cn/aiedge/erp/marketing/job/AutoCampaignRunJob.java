package cn.aiedge.erp.marketing.job;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.scheduler.JobHandler;
import cn.aiedge.common.lock.DistributedLock;
import cn.aiedge.common.lock.DistributedLockFactory;
import cn.aiedge.erp.marketing.mapper.MarketingTenantMapper;
import cn.aiedge.erp.marketing.service.AutoCampaignService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 营销·自动化规则每日执行（定时作业）
 *
 * <p>把《营销自动化》页的「执行全部启用规则」挂到「系统管理 → 开发工具 → 定时任务」，
 * 由 Cron 驱动（任务行 {@code job_key = mkt.autoCampaign.runAll}）。</p>
 *
 * <p><b>三重安全闸门</b>（与 {@code dms.dispatch.escalateOverdue} 同口径）：</p>
 * <ol>
 *   <li><b>默认关闭</b>：迁移只插任务行且 {@code enabled = 0}，需人工在页面启用；</li>
 *   <li><b>多实例互斥</b>：Redis 锁（租约 10 分钟），多副本同时到点只有第一个真正执行；</li>
 *   <li><b>演练参数</b>：{@code execute_params = {"dryRun": true}} 只统计候选数量，不真发券/短信/积分。</li>
 * </ol>
 *
 * <p><b>逐租户执行</b>：定时线程无登录上下文，先取「有启用规则」的租户，再逐个用临时租户上下文执行。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCampaignRunJob implements JobHandler {

    public static final String KEY = "mkt.autoCampaign.runAll";
    private static final String LOCK_KEY = "mkt:job:lock:" + KEY;
    private static final long LEASE_MS = 600_000L;

    private final AutoCampaignService autoCampaignService;
    private final MarketingTenantMapper marketingTenantMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String name() {
        return "营销·自动化规则每日执行";
    }

    @Override
    public String execute(String params) throws Exception {
        boolean dryRun = readDryRun(params);
        DistributedLock lock = DistributedLockFactory.createRedisLock(redisTemplate, LOCK_KEY, LEASE_MS);
        if (!lock.tryLock()) {
            log.info("[{}] 另一实例正在执行，本轮跳过", KEY);
            return "跳过：另一实例正在执行（锁 " + LOCK_KEY + " 被占用）";
        }
        try {
            List<Long> tenantIds = marketingTenantMapper.selectActiveTenantIds();
            if (tenantIds.isEmpty()) {
                return "无启用中的自动化规则，跳过";
            }
            if (dryRun) {
                int total = 0;
                for (Long tenantId : tenantIds) {
                    MyBatisPlusConfig.setTempTenantId(tenantId);
                    try {
                        total += autoCampaignService.candidatesCount(tenantId);
                    } catch (Exception e) {
                        log.warn("[{}] 租户 {} 演练统计失败", KEY, tenantId, e);
                    } finally {
                        MyBatisPlusConfig.clearTempTenantId();
                    }
                }
                return String.format("演练（未触达）：%d 个租户共 %d 位候选会员", tenantIds.size(), total);
            }

            int tenants = 0;
            int success = 0;
            int failedCampaigns = 0;
            for (Long tenantId : tenantIds) {
                MyBatisPlusConfig.setTempTenantId(tenantId);
                try {
                    Map<String, Object> r = autoCampaignService.runAll(500);
                    success += ((Number) r.getOrDefault("success", 0)).intValue();
                    failedCampaigns += ((Number) r.getOrDefault("failedCampaigns", 0)).intValue();
                    tenants++;
                } catch (Exception e) {
                    // 单租户失败不拖垮整体
                    log.warn("[{}] 租户 {} 自动化执行失败", KEY, tenantId, e);
                    failedCampaigns++;
                } finally {
                    MyBatisPlusConfig.clearTempTenantId();
                }
            }
            String summary = String.format("执行完成：%d 个租户，成功触达 %d 人次，失败规则 %d 条",
                    tenants, success, failedCampaigns);
            log.info("[{}] {}", KEY, summary);
            return summary;
        } finally {
            lock.unlock();
        }
    }

    private boolean readDryRun(String params) {
        if (params == null || params.isBlank()) return false;
        try {
            JsonNode node = objectMapper.readTree(params);
            return node.path("dryRun").asBoolean(false);
        } catch (Exception e) {
            log.warn("[{}] execute_params 不是合法 JSON，按非演练执行: {}", KEY, params);
            return false;
        }
    }
}
