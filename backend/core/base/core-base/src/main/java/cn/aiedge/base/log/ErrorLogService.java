package cn.aiedge.base.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 错误日志服务 — 将错误记录到 JSONL 格式文件
 *
 * <p>设计参考 AtCode 的 errorLogSink 机制：</p>
 * <ul>
 *   <li>按日期分文件: logs/errors/YYYY-MM-DD.jsonl</li>
 *   <li>缓冲写入: 1s 刷新间隔 + 50 条批量</li>
 *   <li>内存环形缓冲区: 保留最近 200 条</li>
 *   <li>自动清理: 超过 30 天的日志</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@Primary
public class ErrorLogService {

    private static final String LOG_DIR = "logs/errors";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_IN_MEMORY_ERRORS = 200;
    private static final int BUFFER_FLUSH_SIZE = 50;
    private static final long BUFFER_FLUSH_INTERVAL_MS = 1000;
    private static final long CLEANUP_OLDER_THAN_DAYS = 30;

    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<ErrorLogEntry> buffer = new ConcurrentLinkedQueue<>();
    private final LinkedList<ErrorLogEntry> inMemoryRing = new LinkedList<>();
    private final AtomicInteger writeErrorCount = new AtomicInteger(0);

    private ScheduledExecutorService scheduler;
    private Path logDirPath;
    private PrintWriter currentWriter;
    private String currentDateStr;

    @Value("${app.error-log.enabled:true}")
    private boolean enabled;

    @Value("${app.error-log.cleanup-days:30}")
    private int cleanupDays;

    public ErrorLogService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("[ErrorLogService] 错误日志记录已禁用");
            return;
        }

        try {
            logDirPath = Paths.get(LOG_DIR);
            Files.createDirectories(logDirPath);
            log.info("[ErrorLogService] 错误日志目录: {}", logDirPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[ErrorLogService] 创建日志目录失败: {}", e.getMessage());
            enabled = false;
            return;
        }

        // 打开今天的日志文件
        openDailyFile(LocalDate.now());

        // 启动定时刷新线程
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "error-log-flusher");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flushBuffer, BUFFER_FLUSH_INTERVAL_MS, BUFFER_FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // 启动每日清理（每天凌晨 3 点执行，首次延迟 1 小时）
        scheduler.scheduleWithFixedDelay(this::cleanupOldLogs, 1, 24, TimeUnit.HOURS);

        log.info("[ErrorLogService] 错误日志服务已初始化");
    }

    @PreDestroy
    public void destroy() {
        flushBuffer();
        closeCurrentWriter();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    // ==================== 公共 API ====================

    /**
     * 记录一条错误日志
     */
    public void logError(ErrorLogEntry entry) {
        if (!enabled) return;

        entry.setTimestamp(entry.getTimestamp() != null ? entry.getTimestamp() : new Date());
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }

        // 加入内存环形缓冲区
        synchronized (inMemoryRing) {
            inMemoryRing.addLast(entry);
            if (inMemoryRing.size() > MAX_IN_MEMORY_ERRORS) {
                inMemoryRing.removeFirst();
            }
        }

        // 加入写入缓冲区
        buffer.add(entry);

        // 缓冲区达到阈值立即刷新
        if (buffer.size() >= BUFFER_FLUSH_SIZE) {
            flushBuffer();
        }
    }

    /**
     * 记录异常到错误日志
     */
    public void logException(Throwable throwable, String source, Map<String, Object> context) {
        ErrorLogEntry entry = new ErrorLogEntry();
        entry.setLevel("ERROR");
        entry.setSource(source);
        entry.setType(throwable.getClass().getName());
        entry.setMessage(throwable.getMessage());
        entry.setStackTrace(getStackTraceAsString(throwable));
        entry.setContext(context);
        logError(entry);
    }

    /**
     * 记录前端上报的错误
     */
    public void logFrontendError(String type, String message, String stack,
                                  String url, String userAgent,
                                  Map<String, Object> extra) {
        ErrorLogEntry entry = new ErrorLogEntry();
        entry.setLevel("ERROR");
        entry.setSource("frontend");
        entry.setType(type);
        entry.setMessage(message);
        entry.setStackTrace(stack);
        entry.setUrl(url);
        entry.setUserAgent(userAgent);
        entry.setContext(extra);
        logError(entry);
    }

    /**
     * 获取最近的内存错误列表
     */
    public List<ErrorLogEntry> getRecentErrors(int limit) {
        synchronized (inMemoryRing) {
            return inMemoryRing.stream()
                    .skip(Math.max(0, inMemoryRing.size() - limit))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 按日期范围查询错误日志文件
     */
    public List<ErrorLogEntry> queryErrors(LocalDate startDate, LocalDate endDate,
                                            String level, String source, int limit) {
        List<ErrorLogEntry> results = new ArrayList<>();
        if (!enabled || logDirPath == null) return results;

        LocalDate current = startDate;
        while (!current.isAfter(endDate) && results.size() < limit) {
            try {
                Path filePath = logDirPath.resolve(current.format(DATE_FMT) + ".jsonl");
                if (Files.exists(filePath)) {
                    List<String> lines = Files.readAllLines(filePath);
                    for (int i = Math.max(0, lines.size() - limit); i < lines.size(); i++) {
                        try {
                            ErrorLogEntry entry = objectMapper.readValue(lines.get(i), ErrorLogEntry.class);
                            if (matchesFilter(entry, level, source)) {
                                results.add(entry);
                                if (results.size() >= limit) break;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (IOException ignored) {
            }
            current = current.plusDays(1);
        }
        return results;
    }

    /**
     * 获取错误统计
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", enabled);
        stats.put("logDirectory", logDirPath != null ? logDirPath.toAbsolutePath().toString() : null);
        stats.put("inMemoryCount", inMemoryRing.size());
        stats.put("writeErrorCount", writeErrorCount.get());

        // 统计当天文件大小
        if (logDirPath != null) {
            try {
                Path todayFile = logDirPath.resolve(LocalDate.now().format(DATE_FMT) + ".jsonl");
                if (Files.exists(todayFile)) {
                    stats.put("todayFileSize", Files.size(todayFile));
                    stats.put("todayEntryCount", Files.readAllLines(todayFile).size());
                } else {
                    stats.put("todayFileSize", 0);
                    stats.put("todayEntryCount", 0);
                }
            } catch (IOException e) {
                stats.put("todayFileSize", -1);
            }
        }

        // 列出所有日志文件
        if (logDirPath != null) {
            try (Stream<Path> paths = Files.list(logDirPath)) {
                List<String> files = paths
                        .filter(p -> p.toString().endsWith(".jsonl"))
                        .map(p -> p.getFileName().toString())
                        .sorted(Comparator.reverseOrder())
                        .limit(30)
                        .collect(Collectors.toList());
                stats.put("logFiles", files);
            } catch (IOException ignored) {
            }
        }

        return stats;
    }

    // ==================== 内部方法 ====================

    private synchronized void openDailyFile(LocalDate date) {
        String dateStr = date.format(DATE_FMT);
        if (dateStr.equals(currentDateStr) && currentWriter != null) {
            return; // 已打开当天文件
        }

        closeCurrentWriter();

        try {
            Path filePath = logDirPath.resolve(dateStr + ".jsonl");
            boolean isNew = !Files.exists(filePath);
            currentWriter = new PrintWriter(new FileWriter(filePath.toFile(), true));
            currentDateStr = dateStr;
            log.debug("[ErrorLogService] 打开日志文件: {} (新文件: {})", filePath, isNew);
        } catch (IOException e) {
            log.error("[ErrorLogService] 打开日志文件失败: {}", e.getMessage());
        }
    }

    private synchronized void flushBuffer() {
        if (buffer.isEmpty() || currentWriter == null) return;

        // 检查是否需要切换日期文件
        String today = LocalDate.now().format(DATE_FMT);
        if (!today.equals(currentDateStr)) {
            openDailyFile(LocalDate.now());
        }

        List<ErrorLogEntry> batch = new ArrayList<>();
        ErrorLogEntry entry;
        while ((entry = buffer.poll()) != null && batch.size() < BUFFER_FLUSH_SIZE) {
            batch.add(entry);
        }

        if (batch.isEmpty()) return;

        try {
            for (ErrorLogEntry e : batch) {
                String json = objectMapper.writeValueAsString(e);
                currentWriter.println(json);
            }
            currentWriter.flush();
        } catch (IOException e) {
            writeErrorCount.incrementAndGet();
            log.error("[ErrorLogService] 写入错误日志失败: {}", e.getMessage());
        }
    }

    private synchronized void closeCurrentWriter() {
        if (currentWriter != null) {
            currentWriter.flush();
            currentWriter.close();
            currentWriter = null;
            currentDateStr = null;
        }
    }

    /**
     * 清理超过指定天数的旧日志
     */
    private void cleanupOldLogs() {
        if (logDirPath == null) return;

        LocalDate cutoff = LocalDate.now().minusDays(cleanupDays);
        log.debug("[ErrorLogService] 清理 {} 天前的旧日志...", cleanupDays);

        try (Stream<Path> paths = Files.list(logDirPath)) {
            paths.filter(p -> p.toString().endsWith(".jsonl"))
                    .forEach(p -> {
                        try {
                            String fileName = p.getFileName().toString();
                            String dateStr = fileName.replace(".jsonl", "");
                            LocalDate fileDate = LocalDate.parse(dateStr, DATE_FMT);
                            if (fileDate.isBefore(cutoff)) {
                                Files.delete(p);
                                log.info("[ErrorLogService] 删除旧日志: {}", fileName);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        } catch (IOException e) {
            log.warn("[ErrorLogService] 清理旧日志失败: {}", e.getMessage());
        }
    }

    private boolean matchesFilter(ErrorLogEntry entry, String level, String source) {
        if (level != null && !level.isEmpty() && !level.equalsIgnoreCase(entry.getLevel())) {
            return false;
        }
        if (source != null && !source.isEmpty() && !source.equalsIgnoreCase(entry.getSource())) {
            return false;
        }
        return true;
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        if (throwable.getCause() != null) {
            sb.append("Caused by: ").append(getStackTraceAsString(throwable.getCause()));
        }
        return sb.toString();
    }
}
