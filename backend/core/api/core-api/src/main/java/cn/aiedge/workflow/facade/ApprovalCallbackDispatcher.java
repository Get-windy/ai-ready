package cn.aiedge.workflow.facade;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.workflow.facade.ApprovalCallback;
import cn.aiedge.workflow.entity.WorkflowCallbackLogEntity;
import cn.aiedge.workflow.event.ApprovalCompletedEvent;
import cn.aiedge.workflow.mapper.WorkflowCallbackLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批终态回调分发器（第二期：回调联动；第三期：失败补偿 + terminated 联动）
 *
 * 时序：引擎实例翻转 approved/rejected/terminated 落库 → 发布 ApprovalCompletedEvent
 * → 事务提交后（AFTER_COMMIT；当前引擎无声明式事务，经 fallbackExecution 立即触发）
 * → 专用线程池异步执行 → 遍历支持该 bizType 的 ApprovalCallback Bean 分发，
 * 由各业务实现回写单据状态。
 *
 * 第三期补偿（workflow_callback_log，V11.26.0）：
 * 1. 分发前落 pending 日志；分发后按结果落 success / failed（error + next_retry_time）；
 * 2. 任一目标回调抛异常即记 failed，由 {@link #retryFailedCallbacks()} 每分钟扫描，
 *    按指数退避（1m/5m/15m/30m/1h，retry_count 已重试次数取档）自动重发——
 *    重发复用同一 {@link #dispatch} 逻辑，经 ApprovalCallback.supports(bizType) 过滤目标回调；
 *    同一任务另兜底扫描 status=pending 且超过 10 分钟未翻转的死记录
 *    （落 pending 后、dispatch 前应用崩溃所致），按 failed 同一路径重发；
 * 3. 达到 max_retry（默认5）记 final-failed 停止重试，人工经
 *    POST /api/workflow/callback-log/{id}/retry 重置重试周期并立即分发（{@link #manualRetry}）。
 *
 * terminated 联动：实例被管理员终止视同驳回，回调 onRejected（comment 前缀"流程终止"），
 * 单据回退草稿可重新提交；日志 result 记 terminated。
 *
 * 选择 @TransactionalEventListener 而非手工 TransactionSynchronization 的理由：
 * 声明式语义清晰，且 fallbackExecution = true 覆盖引擎当前无事务的现实
 * （此时事件在 publish 后立即进入异步线程，不会丢失）；将来引擎或调用方
 * 纳入事务后自动获得 AFTER_COMMIT 语义，避免引擎事务与单据回写事务互锁。
 *
 * 健壮性：逐个回调 try/catch，单个业务回调失败不影响其他业务，也不反向
 * 影响引擎审批结果（引擎终态已提交，回写失败落 failed 待补偿）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalCallbackDispatcher {

    /** 补偿状态 */
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_FINAL_FAILED = "final-failed";

    /** 指数退避档位（分钟）：第 1~5 次重试分别等待 1m/5m/15m/30m/1h */
    private static final long[] RETRY_BACKOFF_MINUTES = {1, 5, 15, 30, 60};

    private static final int DEFAULT_MAX_RETRY = 5;
    /** 单次补偿扫描批量上限，防止积压时长时间占用调度线程 */
    private static final int RETRY_BATCH_SIZE = 100;
    /** pending 死记录兜底窗口（分钟）：分发前落 pending 后应用崩溃的记录会永远停在 pending，
     *  超过该窗口仍未翻转即按 failed 同一路径重发（正常在途分发为秒级，10 分钟窗口足够，不会误判） */
    private static final long PENDING_STALE_MINUTES = 10;

    private final ObjectProvider<ApprovalCallback> callbackProvider;
    private final WorkflowCallbackLogMapper callbackLogMapper;

    @Async("approvalCallbackExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        if (event.businessType() == null || event.businessId() == null) {
            // 纯流程实例（无业务单据关联），无需回写
            return;
        }
        Long logId = createPendingLog(event);
        dispatch(event, logId);
    }

    /**
     * 分发核心：事件首次分发与补偿重试（自动/人工）共用。
     * 按 ApprovalCallback.supports(bizType) 过滤目标回调；terminated 视同 rejected
     * 回调（comment 前缀"流程终止"）。全部目标回调成功标记 success，
     * 任一失败（全部失败或部分失败）标记 failed 并按退避档位计算 next_retry_time。
     *
     * @param logId 补偿日志ID；为 null（日志落库失败）时仅分发不标记
     */
    public void dispatch(ApprovalCompletedEvent event, Long logId) {
        // 回调线程无 Sa-Token 会话，设置临时租户上下文，
        // 否则 TenantLineInnerInterceptor 会注入字面量 tenant_id=null（永不匹配），导致单据查询不到
        if (event.tenantId() != null) {
            MyBatisPlusConfig.setTempTenantId(event.tenantId());
        }
        List<String> errors = new ArrayList<>();
        try {
            for (ApprovalCallback callback : callbackProvider) {
                if (!callback.supports(event.businessType())) {
                    // 第三期：按 bizType 过滤目标回调（默认实现恒 true，保持广播兼容）
                    continue;
                }
                try {
                    if (event.isApproved()) {
                        callback.onApproved(event.businessType(), event.businessId(),
                                event.operatorId(), event.operatorName());
                    } else {
                        callback.onRejected(event.businessType(), event.businessId(),
                                resolveRejectComment(event), event.operatorId(), event.operatorName());
                    }
                } catch (Exception e) {
                    log.error("审批回调执行失败: callback={}, bizType={}, bizId={}, result={}, error={}",
                            callback.getClass().getSimpleName(), event.businessType(), event.businessId(),
                            event.result(), e.getMessage(), e);
                    errors.add(callback.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        } finally {
            if (event.tenantId() != null) {
                MyBatisPlusConfig.clearTempTenantId();
            }
        }
        if (logId != null) {
            if (errors.isEmpty()) {
                markSuccess(logId);
            } else {
                markFailure(logId, errors);
            }
        }
    }

    /**
     * 补偿重试任务：每分钟扫描到期 failed 记录重发（retry_count<max_retry）。
     * 兜底：status=pending 且 update_time 早于 10 分钟前的记录视为分发途中崩溃的死记录，
     * 按 failed 同一路径重发（正常在途分发为秒级，10 分钟窗口不会与在途分发冲突）。
     * 调度线程无租户上下文——TenantLineInnerInterceptor 取不到租户时不注入条件，
     * 故可扫到全租户的到期记录；逐条重发时由 dispatch 按行内 tenant_id 设置临时上下文。
     */
    @Scheduled(fixedDelay = 60000)
    public void retryFailedCallbacks() {
        List<WorkflowCallbackLogEntity> due = new ArrayList<>();
        try {
            due.addAll(callbackLogMapper.selectList(new LambdaQueryWrapper<WorkflowCallbackLogEntity>()
                    .eq(WorkflowCallbackLogEntity::getStatus, STATUS_FAILED)
                    .apply("retry_count < max_retry")
                    .le(WorkflowCallbackLogEntity::getNextRetryTime, LocalDateTime.now())
                    .orderByAsc(WorkflowCallbackLogEntity::getId)
                    .last("LIMIT " + RETRY_BATCH_SIZE)));
            // 兜底：分发前落 pending 后、dispatch 前应用崩溃的死记录（不会再被事件线程触碰）
            due.addAll(callbackLogMapper.selectList(new LambdaQueryWrapper<WorkflowCallbackLogEntity>()
                    .eq(WorkflowCallbackLogEntity::getStatus, STATUS_PENDING)
                    .apply("retry_count < max_retry")
                    .lt(WorkflowCallbackLogEntity::getUpdateTime,
                            LocalDateTime.now().minusMinutes(PENDING_STALE_MINUTES))
                    .orderByAsc(WorkflowCallbackLogEntity::getId)
                    .last("LIMIT " + RETRY_BATCH_SIZE)));
        } catch (Exception e) {
            log.error("扫描待补偿回调日志失败: error={}", e.getMessage(), e);
            return;
        }
        for (WorkflowCallbackLogEntity row : due) {
            try {
                int retryCount = row.getRetryCount() != null ? row.getRetryCount() : 0;
                int maxRetry = row.getMaxRetry() != null ? row.getMaxRetry() : DEFAULT_MAX_RETRY;
                if (retryCount >= maxRetry) {
                    // 兜底（查询条件已过滤）：达到上限记 final-failed 停止重试
                    row.setStatus(STATUS_FINAL_FAILED);
                    row.setNextRetryTime(null);
                    row.setUpdateTime(LocalDateTime.now());
                    callbackLogMapper.updateById(row);
                    continue;
                }
                // 先累加已重试次数再分发：分发失败由 markFailure 按新计数排定下次退避
                row.setRetryCount(retryCount + 1);
                row.setUpdateTime(LocalDateTime.now());
                callbackLogMapper.updateById(row);
                log.info("补偿重试审批回调: logId={}, bizType={}, bizId={}, 第{}次重试",
                        row.getId(), row.getBizType(), row.getBizId(), row.getRetryCount());
                dispatch(toEvent(row), row.getId());
            } catch (Exception e) {
                // 单条补偿异常不影响其余记录（分发内部异常已被捕获并落 failed，这里是日志行自身的读写异常）
                log.error("补偿重试异常: logId={}, error={}", row.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 人工重试：重置重试计数与退避（给予全新退避周期），并立即同步分发。
     * success 记录拒绝重试；failed/final-failed/pending 均可人工干预。
     */
    public Map<String, Object> manualRetry(Long logId) {
        Map<String, Object> result = new HashMap<>();
        WorkflowCallbackLogEntity row = callbackLogMapper.selectById(logId);
        if (row == null) {
            result.put("success", false);
            result.put("message", "回调日志不存在: " + logId);
            return result;
        }
        if (STATUS_SUCCESS.equals(row.getStatus())) {
            result.put("success", true);
            result.put("message", "该记录已成功，无需重试");
            result.put("status", row.getStatus());
            return result;
        }
        resetLogForRetry(logId);

        dispatch(toEvent(row), row.getId());

        WorkflowCallbackLogEntity after = callbackLogMapper.selectById(logId);
        boolean success = after != null && STATUS_SUCCESS.equals(after.getStatus());
        result.put("success", success);
        result.put("status", after != null ? after.getStatus() : null);
        result.put("message", success ? "重试成功"
                : "重试仍失败" + (after != null && after.getError() != null ? ": " + after.getError() : ""));
        return result;
    }

    /**
     * 补偿日志分页查询（status 过滤；当前登录租户经租户插件自动过滤）
     */
    public Map<String, Object> pageLogs(String status, int pageNum, int pageSize) {
        LambdaQueryWrapper<WorkflowCallbackLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(WorkflowCallbackLogEntity::getStatus, status);
        }
        wrapper.orderByDesc(WorkflowCallbackLogEntity::getId);
        Page<WorkflowCallbackLogEntity> page = callbackLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("page", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * terminated 视同 rejected 的回调意见：加"流程终止"前缀
     */
    private String resolveRejectComment(ApprovalCompletedEvent event) {
        if (!"terminated".equals(event.result())) {
            return event.comment();
        }
        return "流程终止" + (event.comment() != null && !event.comment().isBlank()
                ? "：" + event.comment() : "");
    }

    /**
     * 分发前落 pending 日志。落库失败（如迁移未执行）仅记 error 并返回 null，
     * 回调照常分发（无补偿能力），不阻断审批回写主链路。
     */
    private Long createPendingLog(ApprovalCompletedEvent event) {
        try {
            WorkflowCallbackLogEntity row = new WorkflowCallbackLogEntity()
                    .setTenantId(event.tenantId())
                    .setInstanceId(parseLongOrNull(event.instanceId()))
                    .setBizType(event.businessType())
                    .setBizId(event.businessId())
                    .setResult(event.result())
                    .setOperatorId(event.operatorId())
                    .setOperatorName(event.operatorName())
                    .setComment(event.comment())
                    .setStatus(STATUS_PENDING)
                    .setRetryCount(0)
                    .setMaxRetry(DEFAULT_MAX_RETRY)
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now());
            callbackLogMapper.insert(row);
            return row.getId();
        } catch (Exception e) {
            log.error("落审批回调补偿日志失败(不影响回调分发): bizType={}, bizId={}, error={}",
                    event.businessType(), event.businessId(), e.getMessage(), e);
            return null;
        }
    }

    private void markSuccess(Long logId) {
        // updateById 跳过 null 字段，用 UpdateWrapper 显式清空 error/next_retry_time
        callbackLogMapper.update(null, new LambdaUpdateWrapper<WorkflowCallbackLogEntity>()
                .eq(WorkflowCallbackLogEntity::getId, logId)
                .set(WorkflowCallbackLogEntity::getStatus, STATUS_SUCCESS)
                .set(WorkflowCallbackLogEntity::getError, null)
                .set(WorkflowCallbackLogEntity::getNextRetryTime, null)
                .set(WorkflowCallbackLogEntity::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 人工重试前置重置：status=pending、清空 error、retry_count=0、清空 next_retry_time
     * （显式 set null，updateById 无法清空字段）
     */
    private void resetLogForRetry(Long logId) {
        callbackLogMapper.update(null, new LambdaUpdateWrapper<WorkflowCallbackLogEntity>()
                .eq(WorkflowCallbackLogEntity::getId, logId)
                .set(WorkflowCallbackLogEntity::getStatus, STATUS_PENDING)
                .set(WorkflowCallbackLogEntity::getError, null)
                .set(WorkflowCallbackLogEntity::getRetryCount, 0)
                .set(WorkflowCallbackLogEntity::getNextRetryTime, null)
                .set(WorkflowCallbackLogEntity::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 标记失败：retry_count 为已重试次数，达到 max_retry 记 final-failed 停止重试，
     * 否则按退避档位（1m/5m/15m/30m/1h）排定 next_retry_time。
     */
    private void markFailure(Long logId, List<String> errors) {
        WorkflowCallbackLogEntity current = callbackLogMapper.selectById(logId);
        if (current == null) {
            return;
        }
        int retryCount = current.getRetryCount() != null ? current.getRetryCount() : 0;
        int maxRetry = current.getMaxRetry() != null ? current.getMaxRetry() : DEFAULT_MAX_RETRY;
        WorkflowCallbackLogEntity row = new WorkflowCallbackLogEntity()
                .setId(logId)
                .setError(String.join("; ", errors))
                .setUpdateTime(LocalDateTime.now());
        if (retryCount >= maxRetry) {
            row.setStatus(STATUS_FINAL_FAILED);
            row.setNextRetryTime(null);
            callbackLogMapper.updateById(row);
            log.error("审批回调最终失败(已达最大重试次数{}次)，转人工处理: logId={}, retryCount={}, error={}",
                    maxRetry, logId, retryCount, row.getError());
        } else {
            long backoffMinutes = RETRY_BACKOFF_MINUTES[Math.min(retryCount, RETRY_BACKOFF_MINUTES.length - 1)];
            row.setStatus(STATUS_FAILED);
            row.setNextRetryTime(LocalDateTime.now().plusMinutes(backoffMinutes));
            callbackLogMapper.updateById(row);
            log.warn("审批回调失败待补偿: logId={}, retryCount={}, {}分钟后重试, error={}",
                    logId, retryCount, backoffMinutes, row.getError());
        }
    }

    private ApprovalCompletedEvent toEvent(WorkflowCallbackLogEntity row) {
        return new ApprovalCompletedEvent(
                row.getInstanceId() != null ? String.valueOf(row.getInstanceId()) : null,
                row.getBizType(), row.getBizId(), row.getResult(),
                row.getOperatorId(), row.getOperatorName(), row.getComment(), row.getTenantId());
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 回调专用线程池：独立命名，避免与应用内其他 @Async 任务争抢默认线程池
     */
    @Configuration
    static class ApprovalCallbackExecutorConfig {

        @Bean("approvalCallbackExecutor")
        public ThreadPoolTaskExecutor approvalCallbackExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(4);
            executor.setQueueCapacity(200);
            executor.setThreadNamePrefix("approval-callback-");
            executor.initialize();
            return executor;
        }
    }
}
