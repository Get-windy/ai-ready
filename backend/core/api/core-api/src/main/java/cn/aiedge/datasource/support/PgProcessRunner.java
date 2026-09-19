package cn.aiedge.datasource.support;

import cn.aiedge.datasource.config.DataMaintenanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * PostgreSQL 官方客户端工具（{@code pg_dump} / {@code pg_restore}）调用器
 *
 * <p>为什么用官方命令行而不是 JDBC 自己实现：逻辑备份/恢复的正确性边界（序列、外键顺序、
 * 扩展、大对象、权限、并行）由官方工具保证；自己写等于重新发明一个更容易出错的备份器。
 *
 * <p>安全与健壮性约定：
 * <ul>
 *   <li>口令**只**经 {@code PGPASSWORD} 环境变量传递，不进命令行（不进进程列表/审计日志）；</li>
 *   <li>所有参数以 {@code List<String>} 形式传给 {@link ProcessBuilder}，**不经过 shell**，
 *       因此库名/用户名中的特殊字符不构成命令注入；</li>
 *   <li>stdout/stderr 由**独立线程**持续读取 —— 若只 waitFor 不读，子进程会因管道缓冲区写满而假死；</li>
 *   <li>超时即 {@code destroyForcibly()} 并返回失败，绝不留下「永远 running」的台账。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PgProcessRunner {

    private static final String PG_DUMP = "pg_dump";
    private static final String PG_RESTORE = "pg_restore";

    /** Windows 下 PostgreSQL 的常规安装根目录；用于未配置 pgBinDir 且 PATH 中找不到工具时兜底 */
    private static final String WIN_PG_ROOT = "C:/Program Files/PostgreSQL";

    private final DataMaintenanceProperties properties;

    // ==================== 工具定位 ====================

    /**
     * 定位 pg_dump 可执行文件
     *
     * @return 可执行文件绝对路径；找不到返回 null（调用方需给出「未安装客户端工具」的明确失败）
     */
    public String resolvePgDump() {
        return resolveTool(PG_DUMP);
    }

    /**
     * 定位 pg_restore 可执行文件
     *
     * @return 可执行文件绝对路径；找不到返回 null
     */
    public String resolvePgRestore() {
        return resolveTool(PG_RESTORE);
    }

    /**
     * 工具定位顺序：显式配置 → 系统 PATH → Windows 常见安装目录（取最高版本）
     *
     * <p>注意：这里**不做缓存** —— 运维补装客户端后无需重启应用即可生效，代价只是几次文件存在性判断。
     */
    private String resolveTool(String toolName) {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        String exeName = windows ? toolName + ".exe" : toolName;

        // ① 显式配置的工具目录
        String configuredDir = properties.getBackup().getPgBinDir();
        if (StringUtils.hasText(configuredDir)) {
            Path candidate = Paths.get(configuredDir.trim()).resolve(exeName);
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize().toString();
            }
            log.warn("配置的 pg 工具目录下找不到 {}: {}", exeName, candidate);
        }

        // ② 系统 PATH（拿一个必然存在的目录去 resolve，避免依赖 PATH 解析语义）
        if (isOnPath(toolName)) {
            return toolName;
        }

        // ③ Windows 常见安装目录：取版本号最高的那个（与 DBA 本机安装的最新版一致）
        if (windows) {
            Path root = Paths.get(WIN_PG_ROOT);
            if (Files.isDirectory(root)) {
                try (Stream<Path> versions = Files.list(root)) {
                    List<Path> candidates = versions
                            .filter(Files::isDirectory)
                            .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                            .toList();
                    for (Path versionDir : candidates) {
                        Path candidate = versionDir.resolve("bin").resolve(exeName);
                        if (Files.isRegularFile(candidate)) {
                            return candidate.toAbsolutePath().normalize().toString();
                        }
                    }
                } catch (IOException e) {
                    log.warn("扫描 {} 失败: {}", WIN_PG_ROOT, e.getMessage());
                }
            }
        }
        return null;
    }

    /** 用 {@code <tool> --version} 探测 PATH 中是否存在该工具（PATH 解析交给操作系统） */
    private boolean isOnPath(String toolName) {
        try {
            Process p = new ProcessBuilder(toolName, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /** 供页面/日志回显的工具版本（拿不到返回「未找到」） */
    public String toolVersion(String executable) {
        if (!StringUtils.hasText(executable)) {
            return "未找到";
        }
        try {
            Process p = new ProcessBuilder(executable, "--version").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = br.readLine();
                p.waitFor(5, TimeUnit.SECONDS);
                return StringUtils.hasText(line) ? line.trim() : "未知版本";
            }
        } catch (IOException e) {
            return "不可执行: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "探测被中断";
        }
    }

    // ==================== 备份 / 恢复 ====================

    /**
     * 执行逻辑备份：{@code pg_dump --format=custom --no-owner --no-privileges --file=<out>}
     *
     * <p>用自定义格式（{@code -Fc}）而不是纯 SQL：① 体积更小；② 支持 {@code pg_restore} 的选择性/
     * 并行恢复与 {@code --clean}；③ 纯 SQL 无法表达「先删后建」的安全恢复语义。
     *
     * @param target  目标库连接
     * @param outFile 落盘文件（必须已由 {@link BackupFileGuard#assertWritable} 校验过）
     * @return 进程执行结果（失败时 output 已截断，可直接作为 error_message）
     */
    public PgProcessResult dump(PgConnectionTarget target, Path outFile) {
        String pgDump = resolvePgDump();
        if (!StringUtils.hasText(pgDump)) {
            return PgProcessResult.fail(
                    "未找到 pg_dump 可执行文件：请在 app.data-maintenance.backup.pg-bin-dir 配置 PostgreSQL 客户端 bin 目录，"
                            + "或将 pg_dump 加入系统 PATH", 0L);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(pgDump);
        cmd.add("--format=custom");
        // 不导出属主与授权：恢复目标库通常与原库的账号体系不同，带上会在恢复时报权限错误
        cmd.add("--no-owner");
        cmd.add("--no-privileges");
        cmd.add("--file=" + outFile.toAbsolutePath());
        addConnectionArgs(cmd, target);

        log.info("开始执行 pg_dump: target={}, file={}, timeout={}s",
                target.safeDescription(), outFile.getFileName(), properties.getBackup().getDumpTimeoutSeconds());
        return run(cmd, target, properties.getBackup().getDumpTimeoutSeconds(), "备份");
    }

    /**
     * 执行恢复：{@code pg_restore --clean --if-exists --no-owner --no-privileges}
     *
     * <p>⚠️ {@code --clean --if-exists} 会先 DROP 备份中包含的对象再重建 —— 这正是「恢复」的语义，
     * 但也就是它为什么必须走「权限码 + 显式 confirm + 审计」三件套。
     *
     * @param target   目标库连接（恢复的目标）
     * @param dumpFile 备份文件（必须已由 {@link BackupFileGuard#resolveReadableFile} 校验过）
     */
    public PgProcessResult restore(PgConnectionTarget target, Path dumpFile) {
        String pgRestore = resolvePgRestore();
        if (!StringUtils.hasText(pgRestore)) {
            return PgProcessResult.fail(
                    "未找到 pg_restore 可执行文件：请在 app.data-maintenance.backup.pg-bin-dir 配置 PostgreSQL 客户端 bin 目录，"
                            + "或将 pg_restore 加入系统 PATH", 0L);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(pgRestore);
        cmd.add("--clean");
        cmd.add("--if-exists");
        cmd.add("--no-owner");
        cmd.add("--no-privileges");
        addConnectionArgs(cmd, target);
        // 位置参数（备份文件）必须放最后
        cmd.add(dumpFile.toAbsolutePath().toString());

        log.warn("开始执行 pg_restore（破坏性操作）: target={}, file={}, timeout={}s",
                target.safeDescription(), dumpFile.getFileName(), properties.getBackup().getRestoreTimeoutSeconds());
        return run(cmd, target, properties.getBackup().getRestoreTimeoutSeconds(), "恢复");
    }

    /** 连接参数一律用长选项；口令由环境变量提供，不进命令行 */
    private void addConnectionArgs(List<String> cmd, PgConnectionTarget target) {
        cmd.add("--host=" + target.host());
        cmd.add("--port=" + target.port());
        cmd.add("--username=" + target.username());
        cmd.add("--dbname=" + target.database());
    }

    /**
     * 启动子进程、并发读取输出、按超时等待
     *
     * @param actionName 动作名（用于拼错误信息，如「备份」「恢复」）
     */
    private PgProcessResult run(List<String> cmd, PgConnectionTarget target, int timeoutSeconds, String actionName) {
        long start = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        if (StringUtils.hasText(target.password())) {
            pb.environment().put("PGPASSWORD", target.password());
        }
        // 客户端提示本地化会影响错误信息可读性；强制中文环境无必要，保持默认（英文错误码更利于检索）
        pb.directory(null);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            log.error("{}进程启动失败: cmd={}, error={}", actionName, cmd.get(0), e.getMessage());
            return PgProcessResult.fail(actionName + "进程启动失败: " + e.getMessage(), System.currentTimeMillis() - start);
        }

        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> {
            int limit = properties.getBackup().getMaxOutputChars();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (output.length() < limit) {
                        output.append(line).append('\n');
                    }
                }
            } catch (IOException ignored) {
                // 进程被强杀时流会关闭 —— 属预期情况，不影响结论
            }
        }, "pg-output-reader");
        reader.setDaemon(true);
        reader.start();

        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return PgProcessResult.fail(actionName + "被中断", System.currentTimeMillis() - start);
        }

        if (!finished) {
            process.destroyForcibly();
            long duration = System.currentTimeMillis() - start;
            log.error("{}超时（{}s），已强制终止进程: target={}", actionName, timeoutSeconds, target.safeDescription());
            return new PgProcessResult(false, -1, output.toString(), duration,
                    actionName + "超时（超过 " + timeoutSeconds + " 秒），已强制终止");
        }

        try {
            reader.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long duration = System.currentTimeMillis() - start;
        int exitCode = process.exitValue();
        String text = output.toString();
        if (exitCode == 0) {
            return new PgProcessResult(true, 0, text, duration, actionName + "成功");
        }
        // pg_restore 的「非致命告警」也会走非 0 退出码（如已存在对象、权限不足的注释）
        // —— 不美化：只要退出码非 0 就判失败，宁可让运维复核，也不谎报成功
        String reason = StringUtils.hasText(text) ? text.trim() : ("退出码 " + exitCode);
        log.error("{}失败: target={}, exitCode={}, output={}", actionName, target.safeDescription(), exitCode, reason);
        return new PgProcessResult(false, exitCode, text, duration,
                actionName + "失败（退出码 " + exitCode + "）: " + firstLine(reason));
    }

    private String firstLine(String text) {
        int idx = text.indexOf('\n');
        String first = idx > 0 ? text.substring(0, idx) : text;
        return first.length() > 300 ? first.substring(0, 300) + "..." : first;
    }

    /** 供诊断：备份目录所在盘的可用空间（MB） */
    public long usableSpaceMb(Path dir) {
        File f = dir.toFile();
        return f.getUsableSpace() / 1024 / 1024;
    }
}
