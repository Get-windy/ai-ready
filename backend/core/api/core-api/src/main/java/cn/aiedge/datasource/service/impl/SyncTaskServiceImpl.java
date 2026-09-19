package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.config.DataMaintenanceProperties;
import cn.aiedge.datasource.mapper.SyncTaskMapper;
import cn.aiedge.datasource.model.SyncTask;
import cn.aiedge.datasource.service.SyncTaskService;
import cn.aiedge.integration.model.SyncDataSourceConfig;
import cn.aiedge.integration.service.SyncConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步任务服务实现
 *
 * <p>本轮（2026-09-19）对「立即执行」的处置：<b>真实接线 + 如实回执</b>。
 *
 * <h3>先查清了什么</h3>
 * <ol>
 *   <li>{@code backend/sync-engine/} 是一个**独立的 Python 服务**（不是 Java 模块），提供
 *       {@code POST /api/sync/trigger}、{@code /api/sync/test-connection}、{@code /health} 等 HTTP 接口，
 *       由 {@code cn.aiedge.integration.SyncConfigServiceImpl} 消费（「数据导入」页链路）。</li>
 *   <li>sync-engine 自己的 {@code trigger_sync} 端点**并不同步执行同步** ——
 *       它只记录日志并返回「已接收，将在下一次调度周期执行」，真正的搬运由该服务内的
 *       APScheduler（{@code scheduler.py}）按配置表 {@code sync_data_source} 轮询驱动。</li>
 *   <li>{@code sys_sync_task}（本页的表，源/目标指向 {@code sys_data_source}）与
 *       sync-engine 消费的 {@code sync_data_source}（按 tenant + source_type 配置）**不是同一张表**，
 *       {@code sys_sync_task} 也**没有任何指向 {@code sync_data_source.id} 的映射列**。</li>
 * </ol>
 *
 * <h3>因此本实现的口径</h3>
 * <ul>
 *   <li><b>投递目标解析</b>：按任务 {@code tenant_id} 在 sync-engine 的启用配置中唯一匹配
 *       （恰好 1 条才投递）。0 条 → 明确失败「该租户在同步引擎中没有启用的数据源配置」；
 *       多条 → 明确失败「无法自动确定投递目标」。**绝不猜**：宁可报错，也不把 A 租户的任务投到 B 配置上。</li>
 *   <li><b>绝不谎报搬运</b>：投递成功后只回「已投递给同步引擎，由引擎调度周期执行」，
 *       并明确写出「本系统无法确认引擎侧的数据搬运结果」。</li>
 *   <li><b>running 复位</b>：{@code last_run_status} 先置 {@code running} 作为在途标记（幂等闸门），
 *       无论成功失败都立即改写为终态；超过 {@code inflight-timeout-seconds} 的历史 running 视为
 *       中断残留，允许重新执行（不会永久锁死）。</li>
 *   <li><b>不碰 status</b>：{@code status} 是启停开关（前端「启用/暂停」写它），
 *       旧实现把「执行一次」等同于「把任务打开」，是本页最误导的一处，已移除。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncTaskServiceImpl implements SyncTaskService {

    private final SyncTaskMapper syncTaskMapper;
    private final SyncConfigService syncConfigService;
    private final DataMaintenanceProperties properties;

    @Override
    public List<SyncTask> list(Long tenantId) {
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<SyncTask>()
                .eq(SyncTask::getDeleted, 0)
                .eq(tenantId != null, SyncTask::getTenantId, tenantId)
                .orderByDesc(SyncTask::getCreateTime);
        return syncTaskMapper.selectList(wrapper);
    }

    @Override
    public SyncTask create(SyncTask syncTask, Long tenantId, String createBy) {
        LocalDateTime now = LocalDateTime.now();
        syncTask.setTenantId(tenantId);
        syncTask.setCreateBy(createBy);
        syncTask.setUpdateBy(createBy);
        syncTask.setCreateTime(now);
        syncTask.setUpdateTime(now);
        syncTask.setDeleted(0);
        if (syncTask.getStatus() == null) syncTask.setStatus("stopped");
        syncTaskMapper.insert(syncTask);
        return syncTask;
    }

    @Override
    public SyncTask update(Long id, SyncTask syncTask, Long tenantId, String updateBy) {
        SyncTask existing = syncTaskMapper.selectById(id);
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) return null;
        if (syncTask.getSourceId() != null) existing.setSourceId(syncTask.getSourceId());
        if (syncTask.getTargetId() != null) existing.setTargetId(syncTask.getTargetId());
        if (syncTask.getTaskName() != null) existing.setTaskName(syncTask.getTaskName());
        if (syncTask.getSyncType() != null) existing.setSyncType(syncTask.getSyncType());
        if (syncTask.getCronExpression() != null) existing.setCronExpression(syncTask.getCronExpression());
        if (syncTask.getStatus() != null) existing.setStatus(syncTask.getStatus());
        if (syncTask.getDescription() != null) existing.setDescription(syncTask.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setUpdateBy(updateBy);
        if (tenantId != null) existing.setTenantId(tenantId);
        syncTaskMapper.updateById(existing);
        return existing;
    }

    @Override
    public boolean delete(Long id) {
        SyncTask existing = syncTaskMapper.selectById(id);
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) return false;
        existing.setDeleted(1);
        existing.setUpdateTime(LocalDateTime.now());
        return syncTaskMapper.updateById(existing) > 0;
    }

    // ==================== 立即执行 ====================

    @Override
    public Map<String, Object> execute(Long id, Long tenantId, String operator) {
        SyncTask task = loadOwned(id, tenantId);
        if (task == null) {
            return fail("同步任务不存在");
        }

        // ① 幂等闸门：在途窗口内的重复调用直接拒绝（防双击 / 并发重复投递）
        Long inFlightId = checkInFlight(task);
        if (inFlightId != null) {
            return fail("该任务正在执行中（自 " + task.getLastRunTime() + " 起），请等待本次结束或超时（"
                    + properties.getSync().getInflightTimeoutSeconds() + " 秒）后再试");
        }

        // ② 解析投递目标：必须是该租户在同步引擎中**唯一**启用的配置
        List<SyncDataSourceConfig> matched = enabledConfigsForTenant(task.getTenantId());
        if (matched.isEmpty()) {
            String reason = "该租户在同步引擎中没有启用的数据源配置（sync_data_source 中 tenant_id="
                    + task.getTenantId() + " 且 status=1 的记录为 0 条），无法投递。"
                    + "请先到「数据导入」页登记并启用同步配置。";
            writeResult(task, tenantId, "failed", null, reason);
            return fail(reason);
        }
        if (matched.size() > 1) {
            String reason = "该租户在同步引擎中存在 " + matched.size()
                    + " 条启用配置，无法自动确定投递目标（本系统 sys_sync_task 无「引擎配置」映射列）。"
                    + "请仅保留一条启用配置后重试。";
            writeResult(task, tenantId, "failed", null, reason);
            return fail(reason);
        }
        SyncDataSourceConfig config = matched.get(0);

        // ③ 置在途标记（last_run_status，不动 status 启停开关）
        writeResult(task, tenantId, "running", LocalDateTime.now(), "正在向同步引擎投递…");

        // ④ 复用已有集成通道投递（不新建 HTTP 客户端）
        Map<String, Object> engineResult;
        try {
            engineResult = syncConfigService.triggerSync(config.getId(),
                    StringUtils.hasText(task.getSyncType()) ? task.getSyncType() : "incremental");
        } catch (RuntimeException e) {
            log.error("同步投递异常: taskId={}, configId={}", id, config.getId(), e);
            String reason = "投递同步引擎时异常: " + e.getMessage();
            writeResult(task, tenantId, "failed", null, reason);
            return fail(reason);
        }

        boolean engineAccepted = Boolean.TRUE.equals(engineResult.get("success"))
                || Boolean.TRUE.equals(engineResult.get("connected"));
        String engineMessage = engineResult.get("message") == null ? "" : String.valueOf(engineResult.get("message"));

        if (!engineAccepted) {
            String reason = "同步引擎未接受本次投递: " + (StringUtils.hasText(engineMessage) ? engineMessage : "引擎未返回成功状态")
                    + "（本次没有发生任何数据搬运）";
            writeResult(task, tenantId, "failed", null, reason);
            return fail(reason);
        }

        // ⑤ 投递成功 —— 只报「已投递」，绝不报「已同步」
        String detail = "已投递给同步引擎（config_id=" + config.getId() + "，来源系统 " + config.getSourceType() + "）："
                + (StringUtils.hasText(engineMessage) ? engineMessage : "引擎已接收")
                + "。⚠️ 真正的数据搬运由 sync-engine 自身的调度周期执行，本系统无法回读其结果，"
                + "因此「已投递」不等于「数据已同步完成」。";
        writeResult(task, tenantId, "dispatched", LocalDateTime.now(), detail);
        log.info("同步任务已投递: taskId={}, configId={}, operator={}", id, config.getId(), operator);

        Map<String, Object> body = success("同步请求已投递给同步引擎（config_id=" + config.getId()
                + "），由引擎调度周期执行；本系统无法确认数据搬运结果");
        body.put("dispatched", true);
        body.put("configId", config.getId());
        body.put("sourceType", config.getSourceType());
        body.put("engineMessage", engineMessage);
        return body;
    }

    // ==================== 辅助 ====================

    /**
     * 在途判定
     *
     * @return 仍在途返回记录ID；超时残留返回 null（视为中断，允许重新执行）
     */
    private Long checkInFlight(SyncTask task) {
        if (!"running".equals(task.getLastRunStatus()) || task.getLastRunTime() == null) {
            return null;
        }
        long elapsed = Duration.between(task.getLastRunTime(), LocalDateTime.now()).getSeconds();
        if (elapsed >= properties.getSync().getInflightTimeoutSeconds()) {
            log.warn("同步任务 #{} 的上次执行标记已超时（{} 秒），视为中断，允许重新执行",
                    task.getId(), elapsed);
            return null;
        }
        return task.getId();
    }

    /** 该租户在同步引擎中启用的配置（唯一匹配才可投递） */
    private List<SyncDataSourceConfig> enabledConfigsForTenant(Long tenantId) {
        List<SyncDataSourceConfig> all;
        try {
            all = syncConfigService.getAllEnabledConfigs();
        } catch (RuntimeException e) {
            log.error("读取同步引擎启用配置失败", e);
            return List.of();
        }
        if (all == null) {
            return List.of();
        }
        List<SyncDataSourceConfig> matched = new ArrayList<>();
        for (SyncDataSourceConfig config : all) {
            // tenant_id 为空的历史配置按「本任务租户」兜底匹配，避免因脏数据而完全无法投递
            if (tenantId == null || config.getTenantId() == null || tenantId.equals(config.getTenantId())) {
                matched.add(config);
            }
        }
        return matched;
    }

    private SyncTask loadOwned(Long id, Long tenantId) {
        if (id == null) {
            return null;
        }
        SyncTask task = syncTaskMapper.selectById(id);
        if (task == null || Integer.valueOf(1).equals(task.getDeleted())) {
            return null;
        }
        if (tenantId != null && task.getTenantId() != null && !tenantId.equals(task.getTenantId())) {
            log.warn("跨租户访问同步任务被拒绝: id={}, taskTenant={}, requestTenant={}",
                    id, task.getTenantId(), tenantId);
            return null;
        }
        return task;
    }

    /**
     * 回写执行结论（终态必然写入，因此不存在「永停 running」）
     *
     * @param status  running / dispatched / failed
     * @param syncTime 成功投递时写入 last_sync_time；失败/在途传 null 表示不改动该列
     */
    private void writeResult(SyncTask task, Long tenantId, String status, LocalDateTime syncTime, String message) {
        String detail = message != null && message.length() > 4000 ? message.substring(0, 4000) : message;
        LambdaUpdateWrapper<SyncTask> wrapper = new LambdaUpdateWrapper<SyncTask>()
                .eq(SyncTask::getId, task.getId())
                .eq(tenantId != null, SyncTask::getTenantId, tenantId)
                .set(SyncTask::getLastRunStatus, status)
                .set(SyncTask::getLastRunMessage, detail)
                .set(SyncTask::getLastRunTime, LocalDateTime.now())
                .set(SyncTask::getUpdateTime, LocalDateTime.now());
        // last_sync_time 只在「成功投递给引擎」时写入：它表达的是「最后一次发起同步的时间」，
        // 投递失败时保持原值，避免把一次失败的尝试伪装成「同步过了」
        if (syncTime != null) {
            wrapper.set(SyncTask::getLastSyncTime, syncTime);
        }
        syncTaskMapper.update(null, wrapper);
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
