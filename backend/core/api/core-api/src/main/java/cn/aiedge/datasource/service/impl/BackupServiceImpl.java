package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.mapper.BackupRecordMapper;
import cn.aiedge.datasource.model.BackupRecord;
import cn.aiedge.datasource.service.BackupService;
import cn.aiedge.datasource.service.BackupTaskRunner;
import cn.aiedge.datasource.support.BackupConnectionResolver;
import cn.aiedge.datasource.support.BackupFileGuard;
import cn.aiedge.datasource.support.PgConnectionTarget;
import cn.aiedge.datasource.support.PgProcessResult;
import cn.aiedge.datasource.support.PgProcessRunner;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 备份服务实现（真实 pg_dump / pg_restore）
 *
 * <p>本轮（2026-09-19）把两个动作从空实现改成真实现：
 * <ul>
 *   <li><b>创建备份</b>：调用官方 {@code pg_dump}（自定义格式）落盘到受控备份目录，
 *       并把文件路径/大小/终态写回 {@code sys_backup_record}。整库 dump 可能几分钟到几十分钟，
 *       因此**异步执行**：接口立即返回「已受理」，页面靠回读台账看状态。</li>
 *   <li><b>恢复</b>：调用 {@code pg_restore --clean --if-exists}，**同步执行**并返回真实结论
 *       （恢复是运维显式发起的破坏性动作，需要立刻知道成败；异步会让页面无法核验）。
 *       恢复前的三重校验：① 记录状态为 success；② 文件存在且非空；③ 显式 {@code confirm=true}。</li>
 * </ul>
 *
 * <p>幂等与安全：
 * <ul>
 *   <li><b>幂等闸门</b>：同一数据源已有 pending/running 的备份时拒绝再建 ——
 *       重复点击不会叠加出一堆并行 dump 进程抢占磁盘 IO。</li>
 *   <li><b>路径安全</b>：文件名一律服务端生成（用户填的 backupName 不参与路径）；
 *       所有读取/删除的路径都经 {@link BackupFileGuard} 校验仍在备份根目录内。</li>
 * </ul>
 *
 * <p>⚠️ 本类**刻意不加类级 {@code @Transactional}**：备份是长耗时外部进程，
 * 若把「插入台账」与「提交异步任务」包在一个事务里，任务可能在事务提交前就跑起来
 * （读不到刚插入的行），或事务回滚而进程已经启动。插入即提交，状态由后续 UPDATE 推进。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    /** 在途状态：这两个状态下的记录不允许同数据源再次创建备份 */
    private static final List<String> IN_FLIGHT_STATUS = List.of("pending", "running");

    private final BackupRecordMapper backupRecordMapper;
    private final BackupConnectionResolver connectionResolver;
    private final BackupFileGuard fileGuard;
    private final PgProcessRunner pgProcessRunner;
    private final BackupTaskRunner backupTaskRunner;

    @Override
    public List<BackupRecord> list(Long dataSourceId, Long tenantId) {
        LambdaQueryWrapper<BackupRecord> wrapper = new LambdaQueryWrapper<BackupRecord>()
                .eq(dataSourceId != null, BackupRecord::getDataSourceId, dataSourceId)
                .eq(tenantId != null, BackupRecord::getTenantId, tenantId)
                .orderByDesc(BackupRecord::getCreateTime);
        return backupRecordMapper.selectList(wrapper);
    }

    // ==================== 创建备份 ====================

    @Override
    public Map<String, Object> create(Long dataSourceId, String backupName, String backupType,
                                      Long tenantId, String createBy) {
        // ① 解析目标连接：解析失败（数据源不存在/非 PostgreSQL）就是明确失败，不落台账、不启动进程
        PgConnectionTarget target;
        try {
            target = connectionResolver.resolve(dataSourceId);
        } catch (IllegalArgumentException e) {
            return fail(e.getMessage());
        }

        // ② 环境前置校验：工具与磁盘。放在插台账之前 —— 不产生「注定失败」的台账垃圾
        String pgDump = pgProcessRunner.resolvePgDump();
        if (!StringUtils.hasText(pgDump)) {
            return fail("未找到 pg_dump 可执行文件：请在 app.data-maintenance.backup.pg-bin-dir "
                    + "配置 PostgreSQL 客户端 bin 目录，或将 pg_dump 加入系统 PATH");
        }
        if (!fileGuard.hasFreeSpace()) {
            return fail("备份目录可用空间不足（低于 app.data-maintenance.backup.min-free-space-mb），已拒绝创建备份");
        }

        // ③ 幂等闸门：同一数据源已有在途备份 → 拒绝，避免重复点击叠出并行 dump
        Long existing = findInFlight(dataSourceId, tenantId);
        if (existing != null) {
            return fail("该数据源已有进行中的备份任务（#" + existing + "），请等待其结束后再发起");
        }

        // ④ 落盘路径：服务端生成，不采用前端传来的任何路径/名称片段
        Path outFile;
        try {
            outFile = fileGuard.assertWritable(fileGuard.newBackupFile(tenantId, dataSourceId));
        } catch (RuntimeException e) {
            return fail("备份文件路径校验失败: " + e.getMessage());
        }

        // ⑤ 插入台账（pending），随后由异步线程推进到 running → 终态
        BackupRecord record = new BackupRecord();
        record.setDataSourceId(dataSourceId);
        record.setBackupName(StringUtils.hasText(backupName) ? backupName.trim() : defaultName(target));
        record.setBackupType("incremental".equalsIgnoreCase(backupType) ? "incremental" : "full");
        record.setStatus("pending");
        record.setStartTime(LocalDateTime.now());
        record.setFileSize(0L);
        record.setTenantId(tenantId);
        record.setCreateTime(LocalDateTime.now());
        record.setCreateBy(StringUtils.hasText(createBy) ? createBy : "unknown");
        backupRecordMapper.insert(record);

        // ⑥ 提交异步执行
        backupTaskRunner.runDump(record.getId(), tenantId, target, outFile);

        String note = "incremental".equals(record.getBackupType())
                ? "（pg_dump 逻辑备份不支持增量，本次按全量执行）"
                : "";
        log.info("备份任务已受理: id={}, target={}, file={}", record.getId(), target.safeDescription(), outFile.getFileName());

        Map<String, Object> body = success("备份任务已受理，正在后台执行" + note);
        body.put("data", record);
        body.put("target", target.safeDescription());
        body.put("fileName", outFile.getFileName().toString());
        return body;
    }

    private Long findInFlight(Long dataSourceId, Long tenantId) {
        LambdaQueryWrapper<BackupRecord> wrapper = new LambdaQueryWrapper<BackupRecord>()
                .in(BackupRecord::getStatus, IN_FLIGHT_STATUS)
                .eq(dataSourceId != null, BackupRecord::getDataSourceId, dataSourceId)
                .eq(tenantId != null, BackupRecord::getTenantId, tenantId)
                .orderByDesc(BackupRecord::getCreateTime)
                .last("limit 1");
        BackupRecord record = backupRecordMapper.selectOne(wrapper);
        return record == null ? null : record.getId();
    }

    private String defaultName(PgConnectionTarget target) {
        return "手动备份_" + target.database() + "_" + LocalDateTime.now();
    }

    // ==================== 恢复 ====================

    @Override
    public Map<String, Object> restore(Long id, boolean confirm, Long tenantId, String operator) {
        // ① 二次确认闸门：缺省拒绝。这是恢复动作唯一的「人在现场」证明，不能只靠前端按钮是否置灰
        if (!confirm) {
            return fail("恢复是覆盖目标库数据的高危操作，必须显式二次确认（confirm=true）后才能执行");
        }

        BackupRecord record = loadOwned(id, tenantId);
        if (record == null) {
            return fail("备份记录不存在");
        }
        // ② 只有成功的备份可恢复：failed/pending/running 的记录没有可用产物
        if (!"success".equals(record.getStatus())) {
            return fail("该备份当前状态为「" + record.getStatus() + "」，只有成功（success）的备份才能恢复");
        }

        // ③ 文件三重校验：受控目录内 + 存在 + 非空
        Path dumpFile;
        try {
            dumpFile = fileGuard.resolveReadableFile(record.getFilePath());
        } catch (IllegalArgumentException e) {
            // 校验失败也要留痕：这说明台账与磁盘已经不一致，是必须被看到的问题
            writeRestoreResult(record, tenantId, "failed", e.getMessage());
            return fail(e.getMessage());
        }

        // ④ 解析恢复目标并执行
        PgConnectionTarget target;
        try {
            target = connectionResolver.resolve(record.getDataSourceId());
        } catch (IllegalArgumentException e) {
            writeRestoreResult(record, tenantId, "failed", e.getMessage());
            return fail(e.getMessage());
        }

        PgProcessResult result;
        try {
            result = pgProcessRunner.restore(target, dumpFile);
        } catch (RuntimeException e) {
            log.error("恢复执行异常: backupId={}", id, e);
            writeRestoreResult(record, tenantId, "failed", "恢复执行异常: " + e.getMessage());
            return fail("恢复执行异常: " + e.getMessage());
        }

        if (!result.success()) {
            writeRestoreResult(record, tenantId, "failed", result.message());
            return fail(result.message());
        }

        String summary = "已对 " + target.safeDescription() + " 执行 pg_restore，耗时 "
                + result.durationMs() + "ms，文件 " + dumpFile.getFileName();
        writeRestoreResult(record, tenantId, "success", summary);
        log.warn("恢复完成: backupId={}, operator={}, target={}, duration={}ms",
                id, operator, target.safeDescription(), result.durationMs());

        Map<String, Object> body = success("恢复已完成：" + summary);
        body.put("target", target.safeDescription());
        body.put("durationMs", result.durationMs());
        return body;
    }

    /**
     * 回写恢复结果
     *
     * <p>口径：**不污染** {@code status} —— 那是「这份备份本身是否可用」，
     * 一次失败的恢复不代表备份坏了。恢复的结果写 {@code restore_status} / {@code restore_time} /
     * {@code restore_message}，页面回读即可看到成败与原因（失败绝不谎报成功）。
     */
    private void writeRestoreResult(BackupRecord record, Long tenantId, String status, String message) {
        LambdaUpdateWrapper<BackupRecord> wrapper = new LambdaUpdateWrapper<BackupRecord>()
                .eq(BackupRecord::getId, record.getId())
                .eq(tenantId != null, BackupRecord::getTenantId, tenantId)
                .set(BackupRecord::getRestoreStatus, status)
                .set(BackupRecord::getRestoreTime, LocalDateTime.now())
                .set(BackupRecord::getRestoreMessage, abbreviate(message));
        backupRecordMapper.update(null, wrapper);
    }

    // ==================== 删除 ====================

    @Override
    public Map<String, Object> delete(Long id, Long tenantId) {
        BackupRecord record = loadOwned(id, tenantId);
        if (record == null) {
            return fail("备份记录不存在");
        }
        // 记录删除是主体动作；文件删除尽力而为（越界/已丢失不阻断，见 BackupFileGuard）
        boolean fileDeleted = fileGuard.deleteFileQuietly(record.getFilePath());
        backupRecordMapper.deleteById(id);

        Map<String, Object> body = success(fileDeleted
                ? "备份记录与磁盘文件均已删除"
                : "备份记录已删除（未找到可删除的磁盘文件，可能本就没有产物或已被手工清理）");
        body.put("fileDeleted", fileDeleted);
        return body;
    }

    // ==================== 辅助 ====================

    /** 按 id 读取并校验租户归属（异步/无会话场景下不依赖拦截器） */
    private BackupRecord loadOwned(Long id, Long tenantId) {
        if (id == null) {
            return null;
        }
        BackupRecord record = backupRecordMapper.selectById(id);
        if (record == null) {
            return null;
        }
        if (tenantId != null && record.getTenantId() != null && !tenantId.equals(record.getTenantId())) {
            log.warn("跨租户访问备份记录被拒绝: id={}, recordTenant={}, requestTenant={}",
                    id, record.getTenantId(), tenantId);
            return null;
        }
        return record;
    }

    private String abbreviate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 4000 ? text.substring(0, 4000) : text;
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
