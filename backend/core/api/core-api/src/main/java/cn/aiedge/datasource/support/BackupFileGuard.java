package cn.aiedge.datasource.support;

import cn.aiedge.datasource.config.DataMaintenanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 备份文件路径安全守卫
 *
 * <p>🔴 本类存在的唯一目的：**备份目录与文件名一律由服务端生成，绝不接受前端传路径**；
 * 对所有来自 DB（历史数据可能被篡改）与子进程回填的路径，都必须
 * {@code toAbsolutePath().normalize()} 后校验仍位于备份根目录内，否则拒绝。
 * 这是防目录穿越（{@code ../../}）写/删任意位置的最后一道闸门。
 *
 * <p>根目录解析顺序（先绝对路径再 normalize，避免 JVM 工作目录漂移导致多实例各写各的）：
 * <ol>
 *   <li>{@code app.data-maintenance.backup.dir}（运维显式配置，推荐）</li>
 *   <li>{@code storage.local.base-path} 的**同级** backups 目录（与图片/附件同盘，便于统一备份策略）</li>
 *   <li>{@code ${user.dir}/backups}（最后兜底，仅用于无任何配置的本地调试）</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupFileGuard {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DataMaintenanceProperties properties;

    /** 与文件存储根目录同源代码配置，用于推导默认备份目录 */
    @Value("${storage.local.base-path:}")
    private String storageBasePath;

    /**
     * 解析并确保备份根目录存在
     *
     * @return 绝对化 + normalize 后的备份根目录
     */
    public Path backupRoot() {
        String configured = properties.getBackup().getDir();
        Path root;
        if (StringUtils.hasText(configured)) {
            root = Paths.get(configured.trim());
        } else if (StringUtils.hasText(storageBasePath)) {
            root = Paths.get(storageBasePath.trim()).resolveSibling("backups");
        } else {
            root = Paths.get(System.getProperty("user.dir", "."), "backups");
        }
        root = root.toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("备份目录不可创建: " + root + "（" + e.getMessage() + "）", e);
        }
        return root;
    }

    /**
     * 生成一个新的备份文件绝对路径（**文件名完全由服务端生成**）
     *
     * <p>刻意不使用用户填写的 {@code backupName} 参与文件名 —— 该字段是自由文本，
     * 一旦参与路径拼接就等于把「路径注入」交回给前端。
     *
     * @param tenantId     租户
     * @param dataSourceId 数据源（可为空）
     */
    public Path newBackupFile(Long tenantId, Long dataSourceId) {
        Path root = backupRoot();
        String name = String.format("bkp_t%s_ds%s_%s_%s.dump",
                tenantId == null ? "0" : tenantId,
                dataSourceId == null ? "0" : dataSourceId,
                LocalDateTime.now().format(FILE_TS),
                UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        return root.resolve(name).toAbsolutePath().normalize();
    }

    /**
     * 校验一个「将要写入」的路径确实位于备份根目录内
     *
     * @throws IllegalArgumentException 越界时抛出（调用方应转成业务错误，绝不静默继续）
     */
    public Path assertWritable(Path candidate) {
        Path root = backupRoot();
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IllegalArgumentException("备份文件必须落在受控备份目录内，已拒绝: " + normalized);
        }
        return normalized;
    }

    /**
     * 校验一个「将要读取/恢复/删除」的历史路径：① 在根目录内；② 是普通文件；③ 非空
     *
     * <p>恢复前的三重校验之一（另两重是「记录状态为 success」与「显式 confirm」）。
     * 空文件必须拒绝 —— 用一个空 dump 去 restore 只会得到一个「成功但什么都没恢复」的假象。
     *
     * @throws IllegalArgumentException 任一校验不通过
     */
    public Path resolveReadableFile(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            throw new IllegalArgumentException("备份记录没有文件路径，无法执行该操作");
        }
        Path root = backupRoot();
        Path candidate = Paths.get(rawPath.trim()).toAbsolutePath().normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("备份文件不在受控备份目录内，已拒绝（疑似路径穿越）: " + candidate);
        }
        if (!Files.isRegularFile(candidate)) {
            throw new IllegalArgumentException("备份文件不存在或不是普通文件: " + candidate);
        }
        try {
            long size = Files.size(candidate);
            if (size <= 0) {
                throw new IllegalArgumentException("备份文件为空（0 字节），拒绝执行: " + candidate);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("备份文件不可读: " + candidate + "（" + e.getMessage() + "）", e);
        }
        return candidate;
    }

    /**
     * 尽力删除备份文件（用于「删除记录 + 删文件」）
     *
     * <p>路径越界/文件不存在都不抛异常：记录删除是主体操作，磁盘清理失败只记日志，
     * 避免「文件已经被手工移走 → 记录永远删不掉」。
     *
     * @return 真正删除了文件返回 true；文件本就不存在或路径越界返回 false
     */
    public boolean deleteFileQuietly(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return false;
        }
        Path root;
        try {
            root = backupRoot();
        } catch (RuntimeException e) {
            log.warn("解析备份根目录失败，跳过磁盘文件清理: {}", e.getMessage());
            return false;
        }
        Path candidate = Paths.get(rawPath.trim()).toAbsolutePath().normalize();
        if (!candidate.startsWith(root)) {
            // 不删、不抛：历史脏数据（被篡改的 file_path）不应阻断记录删除，但必须留下痕迹
            log.warn("拒绝删除备份根目录之外的文件（疑似路径穿越），记录本身仍会被删除: {}", candidate);
            return false;
        }
        try {
            boolean deleted = Files.deleteIfExists(candidate);
            if (deleted) {
                log.info("已删除备份文件: {}", candidate);
            }
            return deleted;
        } catch (IOException e) {
            log.warn("删除备份文件失败（记录仍会删除）: path={}, error={}", candidate, e.getMessage());
            return false;
        }
    }

    /**
     * 备份目录可用空间校验
     *
     * @return 可用空间不足返回 false
     */
    public boolean hasFreeSpace() {
        Path root = backupRoot();
        long usable = root.toFile().getUsableSpace();
        long required = properties.getBackup().getMinFreeSpaceMb() * 1024L * 1024L;
        if (usable < required) {
            log.warn("备份目录可用空间不足: dir={}, usable={}MB, required={}MB",
                    root, usable / 1024 / 1024, properties.getBackup().getMinFreeSpaceMb());
            return false;
        }
        return true;
    }
}
