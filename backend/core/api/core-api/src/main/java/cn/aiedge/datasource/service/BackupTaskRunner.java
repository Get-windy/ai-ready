package cn.aiedge.datasource.service;

import cn.aiedge.datasource.mapper.BackupRecordMapper;
import cn.aiedge.datasource.model.BackupRecord;
import cn.aiedge.datasource.support.BackupFileGuard;
import cn.aiedge.datasource.support.PgConnectionTarget;
import cn.aiedge.datasource.support.PgProcessResult;
import cn.aiedge.datasource.support.PgProcessRunner;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 备份任务的异步执行器
 *
 * <p>为什么必须独立成 Bean（而不是 {@code BackupServiceImpl} 的私有方法）：
 * {@code @Async} 依赖 Spring 代理生效，**类内自调用会静默退化为同步执行** ——
 * 那样「创建备份」接口会被整库 dump 阻塞几分钟，前端必然超时。
 *
 * <p>状态机（每一步都落库，页面可随时回读）：
 * <pre>
 *   pending（插入台账） → running（本类入口置） → success | failed（终态）
 * </pre>
 * 终态**必然被写入**，包括异常路径（{@code catch Throwable}）—— 这是对旧实现
 * 「status 永停 running、没有任何代码推进」的直接修复。任何一步失败都会写
 * {@code end_time} + {@code error_message}，绝不留下「看起来还在跑」的假象。
 *
 * <p>🔴 租户隔离：本方法运行在 {@code dataMaintenanceExecutor} 线程池中，**没有 Sa-Token 会话**，
 * 多租户拦截器会整体跳过 tenant_id 注入。因此这里的每一次 UPDATE 都显式带
 * {@code tenant_id} 条件，不依赖拦截器兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupTaskRunner {

    private final BackupRecordMapper backupRecordMapper;
    private final PgProcessRunner pgProcessRunner;
    private final BackupFileGuard backupFileGuard;

    /**
     * 异步执行备份
     *
     * @param recordId     台账记录ID
     * @param tenantId     租户（用于 UPDATE 的归属校验）
     * @param target       已解析的目标连接
     * @param outFile      已通过路径安全校验的落盘文件
     */
    @Async("dataMaintenanceExecutor")
    public void runDump(Long recordId, Long tenantId, PgConnectionTarget target, Path outFile) {
        log.info("备份任务开始: id={}, target={}", recordId, target.safeDescription());
        markRunning(recordId, tenantId);

        PgProcessResult result;
        try {
            result = pgProcessRunner.dump(target, outFile);
        } catch (RuntimeException e) {
            // 进程以外的意外（配置缺失、环境变量不可写等）也要落到 failed，不能只打日志
            log.error("备份任务异常: id={}", recordId, e);
            finishFailed(recordId, tenantId, "备份执行异常: " + e.getMessage());
            return;
        }

        if (!result.success()) {
            finishFailed(recordId, tenantId, result.message());
            deletePartialOutput(recordId, outFile);
            return;
        }

        // 进程退出码为 0 不代表文件可用：必须回读磁盘确认产物非空，否则就是「假成功」
        long size;
        try {
            if (!Files.isRegularFile(outFile)) {
                finishFailed(recordId, tenantId, "pg_dump 退出码为 0，但落盘文件不存在: " + outFile);
                return;
            }
            size = Files.size(outFile);
            if (size <= 0) {
                finishFailed(recordId, tenantId, "pg_dump 退出码为 0，但备份文件为 0 字节");
                deletePartialOutput(recordId, outFile);
                return;
            }
        } catch (IOException e) {
            finishFailed(recordId, tenantId, "备份文件校验失败: " + e.getMessage());
            deletePartialOutput(recordId, outFile);
            return;
        }

        finishSuccess(recordId, tenantId, outFile, size, result.durationMs());
    }

    // ==================== 状态回写 ====================

    private void markRunning(Long recordId, Long tenantId) {
        try {
            update(recordId, tenantId, w -> w
                    .set(BackupRecord::getStatus, "running")
                    .set(BackupRecord::getEndTime, null)
                    .set(BackupRecord::getErrorMessage, null));
        } catch (RuntimeException e) {
            // 置 running 失败不致命：终态回写才是关键，继续执行并留日志
            log.warn("置 backup#{} 为 running 失败: {}", recordId, e.getMessage());
        }
    }

    private void finishSuccess(Long recordId, Long tenantId, Path outFile, long size, long durationMs) {
        int rows = update(recordId, tenantId, w -> w
                .set(BackupRecord::getStatus, "success")
                .set(BackupRecord::getFilePath, outFile.toAbsolutePath().toString())
                .set(BackupRecord::getFileSize, size)
                .set(BackupRecord::getEndTime, LocalDateTime.now())
                .set(BackupRecord::getErrorMessage, null));
        log.info("备份任务成功: id={}, file={}, size={}B, duration={}ms, 回写行数={}",
                recordId, outFile.getFileName(), size, durationMs, rows);
    }

    private void finishFailed(Long recordId, Long tenantId, String reason) {
        // 失败时必须清掉 file_path：半截文件不能留在台账里被当成可用备份去恢复
        int rows = update(recordId, tenantId, w -> w
                .set(BackupRecord::getStatus, "failed")
                .set(BackupRecord::getFilePath, null)
                .set(BackupRecord::getFileSize, 0L)
                .set(BackupRecord::getEndTime, LocalDateTime.now())
                .set(BackupRecord::getErrorMessage, abbreviate(reason)));
        log.error("备份任务失败: id={}, reason={}, 回写行数={}", recordId, reason, rows);
    }

    private int update(Long recordId, Long tenantId,
                       java.util.function.Consumer<LambdaUpdateWrapper<BackupRecord>> setter) {
        LambdaUpdateWrapper<BackupRecord> wrapper = new LambdaUpdateWrapper<BackupRecord>()
                .eq(BackupRecord::getId, recordId)
                // 显式租户条件：异步线程无会话，拦截器不会兜底注入
                .eq(tenantId != null, BackupRecord::getTenantId, tenantId);
        setter.accept(wrapper);
        return backupRecordMapper.update(null, wrapper);
    }

    /**
     * 清理失败留下的半成品文件
     *
     * <p>实测（2026-09-19）：{@code pg_dump} 失败时**仍会创建 0 字节的目标文件**
     * （例如口令错误 → 退出码 1 + 空文件）。台账侧已经把 file_path 置空、file_size 归零，
     * 但空文件若留在备份目录里，会让运维误以为「有一份备份」并把目录撑满。
     * 因此失败路径必须显式删掉半成品；路径必须仍在受控备份根目录内（由 guard 校验）。
     */
    private void deletePartialOutput(Long recordId, Path outFile) {
        try {
            boolean deleted = backupFileGuard.deleteFileQuietly(outFile.toAbsolutePath().toString());
            log.info("已清理失败的半成品备份文件: id={}, file={}, deleted={}", recordId, outFile.getFileName(), deleted);
        } catch (RuntimeException e) {
            log.warn("清理半成品备份文件失败（不影响台账已置 failed）: id={}, error={}", recordId, e.getMessage());
        }
    }

    private String abbreviate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 4000 ? text.substring(0, 4000) : text;
    }
}
