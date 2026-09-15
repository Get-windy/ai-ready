package cn.aiedge.dms.dispatch.job;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.scheduler.JobHandler;
import cn.aiedge.common.lock.DistributedLock;
import cn.aiedge.common.lock.DistributedLockFactory;
import cn.aiedge.dms.dispatch.service.DispatchService;
import cn.aiedge.dms.task.dto.BatchResultVO;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 配送·超时任务升级扫描（定时作业）
 *
 * <p>《调度任务开发文档》§3.6 工程约束 2 的定时化：把原先「只能人工点『超时升级』」的
 * {@code POST /api/dms/dispatch/escalate-overdue} 挂到「系统管理 → 开发工具 → 定时任务」，
 * 由 Cron 驱动（任务行 {@code job_key = dms.dispatch.escalateOverdue}）。</p>
 *
 * <p><b>三重安全闸门</b>（避免在共享/生产环境误改数据）：</p>
 * <ol>
 *   <li><b>默认关闭</b>：迁移只插入任务行且 {@code enabled = 0}，需人工在页面启用；</li>
 *   <li><b>多实例互斥</b>：Redis 锁（{@value #LOCK_KEY}，租约 {@value #LEASE_MS} ms）——
 *       多副本同时到点只有第一个真正执行，其余返回「跳过」摘要（锁被 E2E 抢占时同样可复现跳过）；</li>
 *   <li><b>演练参数</b>：{@code execute_params = {"dryRun": true}} 只统计「本可重派多少单」，
 *       不改任务归属、不动配送员、不写审计。</li>
 * </ol>
 *
 * <p><b>逐租户执行</b>：定时线程无登录上下文，多租户插件不注入 tenant_id —— 这里先取在途任务涉及的
 * 全部租户，再逐个用临时租户上下文执行，保证「阈值配置按租户生效、调度审计归属正确租户」。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DmsDispatchEscalateJob implements JobHandler {

    /** 处理器键（写入 scheduled_task.job_key） */
    public static final String KEY = "dms.dispatch.escalateOverdue";

    /** 多实例互斥锁键（E2E 可用 redis-cli 抢锁复现「跳过」） */
    public static final String LOCK_KEY = "dms:job:lock:" + KEY;

    /** 锁租约（毫秒）：实例崩溃后自动释放，避免任务被永久锁死 */
    private static final long LEASE_MS = 300_000L;

    private final DispatchService dispatchService;
    private final DmsTaskMapper taskMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String name() {
        return "配送·超时任务升级扫描";
    }

    @Override
    public String execute(String params) throws Exception {
        boolean dryRun = readDryRun(params);

        DistributedLock lock = DistributedLockFactory.createRedisLock(redisTemplate, LOCK_KEY, LEASE_MS);
        if (!lock.tryLock()) {
            // 集群下这是正常现象：不抛异常（否则每轮重叠都在执行日志里留一条红记录），摘要里说明即可
            log.info("[{}] 另一实例正在执行（锁 {} 被占用），本轮跳过", KEY, LOCK_KEY);
            return "跳过：另一实例正在执行（锁 " + LOCK_KEY + " 被占用）";
        }
        try {
            List<Long> tenantIds = taskMapper.selectActiveTenantIds();
            if (tenantIds.isEmpty()) {
                return "无在途任务，跳过扫描";
            }
            int total = 0;
            int success = 0;
            int failed = 0;
            int tenants = 0;
            for (Long tenantId : tenantIds) {
                MyBatisPlusConfig.setTempTenantId(tenantId);
                try {
                    BatchResultVO result = dispatchService.escalateOverdue(dryRun);
                    total += result.getTotal();
                    success += result.getSuccess();
                    failed += result.getFailed();
                    tenants++;
                } catch (Exception e) {
                    // 单租户失败不拖垮整体（逐租户反馈）
                    log.warn("[{}] 租户 {} 超时升级失败", KEY, tenantId, e);
                    failed++;
                } finally {
                    MyBatisPlusConfig.clearTempTenantId();
                }
            }
            String summary = String.format("%s：扫描 %d 个租户，命中 %d 单、可重派/已重派 %d 单、未重派 %d 单",
                    dryRun ? "演练（未落库）" : "执行完成", tenants, total, success, failed);
            log.info("[{}] {}", KEY, summary);
            return summary;
        } finally {
            lock.unlock();
        }
    }

    /** 演练开关：execute_params 约定为 JSON，{@code {"dryRun": true}} */
    private boolean readDryRun(String params) {
        if (params == null || params.isBlank()) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(params);
            return node.path("dryRun").asBoolean(false);
        } catch (Exception e) {
            log.warn("[{}] execute_params 不是合法 JSON，按非演练执行: {}", KEY, params);
            return false;
        }
    }
}
