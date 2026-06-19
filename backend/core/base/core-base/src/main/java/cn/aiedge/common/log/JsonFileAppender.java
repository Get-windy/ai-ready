package cn.aiedge.common.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

/**
 * 集中式结构化 JSON 日志 Appender
 *
 * <p>拦截所有 WARN 及以上级别的日志事件，写入统一的 JSONL 文件。
 * 所有模块、所有组件的错误/警告/信息日志都汇总到同一位置，便于排查。</p>
 *
 * <h3>输出文件</h3>
 * <ul>
 *   <li>当日文件: {@code {logDir}/app.jsonl}</li>
 *   <li>历史归档: {@code {logDir}/app.yyyy-MM-dd.jsonl.gz}</li>
 * </ul>
 *
 * <h3>JSON 格式</h3>
 * <pre>{@code
 * {
 *   "timestamp": "2026-06-19 10:30:45.123",
 *   "level": "ERROR",
 *   "logger": "cn.aiedge.xxx.ServiceImpl",
 *   "thread": "http-nio-8080-exec-1",
 *   "message": "Something failed",
 *   "module": "erp",              // 从 MDC 提取
 *   "traceId": "abc123",          // 从 MDC 提取
 *   "userId": "1",                // 从 MDC 提取
 *   "tenantId": "1",              // 从 MDC 提取
 *   "error": {                    // 异常信息（如果有）
 *     "type": "java.lang.NullPointerException",
 *     "message": "...",
 *     "stackTrace": "..."
 *   }
 * }
 * }</pre>
 *
 * <h3>logback-spring.xml 配置</h3>
 * <pre>{@code
 * <appender name="JSON_FILE" class="cn.aiedge.common.log.JsonFileAppender">
 *     <logDir>./logs/structured</logDir>
 *     <maxHistory>30</maxHistory>
 *     <maxFileSizeMB>200</maxFileSizeMB>
 * </appender>
 * }</pre>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class JsonFileAppender extends AppenderBase<ILoggingEvent> {

    // ── 可配置参数（logback.xml 注入） ──
    private String logDir = "./logs/structured";
    private int maxHistory = 30;
    private int maxFileSizeMB = 200;
    /** 最低级别，默认 WARN */
    private String threshold = "WARN";

    // ── 内部状态 ──
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BlockingQueue<ILoggingEvent> queue = new LinkedBlockingQueue<>(10_000);
    private final AtomicLong droppedCount = new AtomicLong(0);
    private ScheduledExecutorService scheduler;
    private PrintWriter writer;
    private String currentDateStr;
    private long currentFileSize;
    private Path logDirPath;
    private Level thresholdLevel;

    // ── 生命周期 ──

    @Override
    public void start() {
        thresholdLevel = Level.toLevel(threshold, Level.WARN);
        logDirPath = Paths.get(logDir);
        try {
            Files.createDirectories(logDirPath);
        } catch (IOException e) {
            addError("创建日志目录失败: " + logDir, e);
            return;
        }
        openDailyFile(LocalDate.now());

        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "json-log-writer");
            t.setDaemon(true);
            return t;
        });
        // 每 500ms 刷新一次缓冲区
        scheduler.scheduleAtFixedRate(this::drainQueue, 100, 500, TimeUnit.MILLISECONDS);
        // 每天凌晨 3:10 滚动 + 清理
        scheduler.scheduleAtFixedRate(this::dailyRollAndClean, initialDelayUntil3AM(), 24, TimeUnit.HOURS);

        super.start();
    }

    @Override
    public void stop() {
        drainQueue();
        closeWriter();
        if (scheduler != null) {
            scheduler.shutdown();
        }
        super.stop();
    }

    // ── 核心：追加日志事件 ──

    @Override
    protected void append(ILoggingEvent event) {
        // 过滤低于阈值的事件
        if (!event.getLevel().isGreaterOrEqual(thresholdLevel)) {
            return;
        }
        // 预准备（logback 要求在调用线程内完成）
        event.prepareForDeferredProcessing();

        if (!queue.offer(event)) {
            droppedCount.incrementAndGet();
        }
    }

    // ── 异步写入 ──

    private void drainQueue() {
        try {
            ILoggingEvent event;
            int count = 0;
            while ((event = queue.poll()) != null && count < 500) {
                writeEvent(event);
                count++;
            }
            if (writer != null) {
                writer.flush();
            }
        } catch (Exception e) {
            // 静默，避免递归日志
        }
    }

    private synchronized void writeEvent(ILoggingEvent event) {
        // 检查日期切换
        String today = LocalDate.now().format(DATE_FMT);
        if (!today.equals(currentDateStr)) {
            rollOver(today);
        }
        // 检查文件大小
        if (currentFileSize > (long) maxFileSizeMB * 1024 * 1024) {
            rotateBySize();
        }
        if (writer == null) return;

        try {
            String json = toJson(event);
            writer.println(json);
            currentFileSize += json.getBytes(StandardCharsets.UTF_8).length + 1;
        } catch (Exception e) {
            // 静默
        }
    }

    // ── JSON 序列化 ──

    private String toJson(ILoggingEvent event) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();

        // 基础字段
        map.put("timestamp", TS_FMT.format(Instant.ofEpochMilli(event.getTimeStamp())));
        map.put("level", event.getLevel().toString());
        map.put("logger", abbreviateLogger(event.getLoggerName()));
        map.put("thread", event.getThreadName());
        map.put("message", event.getFormattedMessage());

        // MDC 上下文
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            // 常用字段直接提取到顶层
            putIfPresent(map, mdc, "traceId");
            putIfPresent(map, mdc, "spanId");
            putIfPresent(map, mdc, "userId");
            putIfPresent(map, mdc, "tenantId");
            putIfPresent(map, mdc, "requestId");
            putIfPresent(map, mdc, "module");
            putIfPresent(map, mdc, "action");
            putIfPresent(map, mdc, "clientIp");

            // 剩余 MDC 放入 extra
            Map<String, String> extra = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : mdc.entrySet()) {
                if (!isTopLevelField(e.getKey())) {
                    extra.put(e.getKey(), e.getValue());
                }
            }
            if (!extra.isEmpty()) {
                map.put("extra", extra);
            }
        }

        // 异常信息
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("type", tp.getClassName());
            error.put("message", tp.getMessage());
            error.put("stackTrace", formatStackTrace(tp, 15));
            map.put("error", error);
        }

        return MAPPER.writeValueAsString(map);
    }

    // ── 文件管理 ──

    private synchronized void openDailyFile(LocalDate date) {
        String dateStr = date.format(DATE_FMT);
        if (dateStr.equals(currentDateStr) && writer != null) {
            return;
        }
        closeWriter();
        try {
            Path file = logDirPath.resolve("app.jsonl");
            boolean exists = Files.exists(file);
            writer = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file.toFile(), true), StandardCharsets.UTF_8),
                    8192));
            currentDateStr = dateStr;
            currentFileSize = exists ? Files.size(file) : 0;
        } catch (IOException e) {
            addError("打开日志文件失败", e);
        }
    }

    private synchronized void rollOver(String newDateStr) {
        if (newDateStr.equals(currentDateStr)) return;
        closeWriter();
        // 归档昨天的文件
        try {
            Path current = logDirPath.resolve("app.jsonl");
            if (Files.exists(current) && Files.size(current) > 0) {
                Path archive = logDirPath.resolve("app." + currentDateStr + ".jsonl.gz");
                if (!Files.exists(archive)) {
                    gzip(current, archive);
                }
                // 清空当日文件开始新的一天
                Files.write(current, new byte[0]);
            }
        } catch (IOException e) {
            addError("日志归档失败", e);
        }
        openDailyFile(LocalDate.now());
    }

    private void rotateBySize() {
        // 当前文件超过大小限制时，追加序号归档
        if (writer == null || currentDateStr == null) return;
        closeWriter();
        try {
            Path current = logDirPath.resolve("app.jsonl");
            if (Files.exists(current) && Files.size(current) > 0) {
                // 查找下一个可用序号
                int seq = 1;
                Path target;
                do {
                    target = logDirPath.resolve("app." + currentDateStr + "." + seq + ".jsonl.gz");
                    seq++;
                } while (Files.exists(target));
                gzip(current, target);
                Files.write(current, new byte[0]);
            }
        } catch (IOException e) {
            addError("按大小滚动失败", e);
        }
        openDailyFile(LocalDate.now());
    }

    private void dailyRollAndClean() {
        rollOver(LocalDate.now().format(DATE_FMT));
        cleanOldFiles();
    }

    private void cleanOldFiles() {
        LocalDate cutoff = LocalDate.now().minusDays(maxHistory);
        try (var stream = Files.list(logDirPath)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".gz"))
                    .forEach(p -> {
                        try {
                            String name = p.getFileName().toString();
                            // app.yyyy-MM-dd.jsonl.gz 或 app.yyyy-MM-dd.N.jsonl.gz
                            String datePart = name.replace("app.", "").replaceAll("\\.\\d+\\.jsonl\\.gz$", "").replaceAll("\\.jsonl\\.gz$", "");
                            if (datePart.length() == 10) { // yyyy-MM-dd
                                LocalDate fileDate = LocalDate.parse(datePart, DATE_FMT);
                                if (fileDate.isBefore(cutoff)) {
                                    Files.deleteIfExists(p);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private synchronized void closeWriter() {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
    }

    // ── 工具方法 ──

    private static void gzip(Path source, Path target) throws IOException {
        try (InputStream in = Files.newInputStream(source);
             GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(target))) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private static String formatStackTrace(IThrowableProxy tp, int maxLines) {
        StringBuilder sb = new StringBuilder();
        sb.append(tp.getClassName()).append(": ").append(tp.getMessage()).append("\n");
        StackTraceElementProxy[] elements = tp.getStackTraceElementProxyArray();
        if (elements != null) {
            int limit = Math.min(maxLines, elements.length);
            for (int i = 0; i < limit; i++) {
                sb.append("\tat ").append(elements[i].getSTEAsString()).append("\n");
            }
            if (elements.length > limit) {
                sb.append("\t... ").append(elements.length - limit).append(" more\n");
            }
        }
        if (tp.getCause() != null) {
            sb.append("Caused by: ").append(formatStackTrace(tp.getCause(), 5));
        }
        return sb.toString();
    }

    private static String abbreviateLogger(String loggerName) {
        if (loggerName == null) return "";
        // 保持包名缩写：cn.aiedge.erp.price -> c.a.e.p.price（只保留最后两段完整）
        return loggerName;
    }

    private static void putIfPresent(Map<String, Object> target, Map<String, String> source, String key) {
        String val = source.get(key);
        if (val != null && !val.isEmpty()) {
            target.put(key, val);
        }
    }

    private static boolean isTopLevelField(String key) {
        return "traceId".equals(key) || "spanId".equals(key) ||
               "userId".equals(key) || "tenantId".equals(key) ||
               "requestId".equals(key) || "module".equals(key) ||
               "action".equals(key) || "clientIp".equals(key);
    }

    private long initialDelayUntil3AM() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime next3AM = now.withHour(3).withMinute(10).withSecond(0);
        if (now.isAfter(next3AM)) {
            next3AM = next3AM.plusDays(1);
        }
        return java.time.Duration.between(now, next3AM).toMillis();
    }

    // ── Setter（logback.xml 注入） ──

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public void setMaxFileSizeMB(int maxFileSizeMB) {
        this.maxFileSizeMB = maxFileSizeMB;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }
}
