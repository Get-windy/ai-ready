package cn.aiedge.base.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 集中式结构化 JSON 日志查询控制器
 *
 * <p>提供对 {@code logs/structured/app.jsonl} 文件的查询、过滤和统计接口。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/system/log/structured")
@RequiredArgsConstructor
@Tag(name = "结构化日志查询", description = "集中式 JSON 日志文件的查询、过滤与统计")
public class StructuredLogController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${app.log.structured.dir:./logs/structured}")
    private String structuredLogDir;

    /**
     * 分页查询结构化日志（从 JSONL 文件中读取）
     */
    @GetMapping("/query")
    @Operation(summary = "分页查询结构化日志")
    public StructuredLogQueryResult queryStructuredLogs(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "日志级别过滤: WARN, ERROR") @RequestParam(required = false) String level,
            @Parameter(description = "日志来源过滤（logger名称模糊匹配）") @RequestParam(required = false) String logger,
            @Parameter(description = "关键词搜索（消息内容）") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "50") int pageSize) {

        Path logDir = Paths.get(structuredLogDir);
        List<Map<String, Object>> results = new ArrayList<>();
        long totalEntries = 0;

        if (!Files.exists(logDir)) {
            return new StructuredLogQueryResult(Collections.emptyList(), 0, page, pageSize);
        }

        ObjectMapper mapper = new ObjectMapper();

        // 扫描目录下所有 app*.jsonl 文件（含可能的历史未压缩文件）
        try (Stream<Path> files = Files.list(logDir)) {
            List<Path> logFiles = files
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("app") && name.endsWith(".jsonl") && !name.endsWith(".gz");
                    })
                    .sorted(Comparator.reverseOrder()) // 最新的文件优先
                    .collect(Collectors.toList());

            for (Path logFile : logFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(logFile.toFile()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> entry = mapper.readValue(line, Map.class);

                            // 日期过滤
                            if (!matchesDateRange(entry, startDate, endDate)) continue;

                            if (matchesFilter(entry, level, logger, keyword)) {
                                totalEntries++;
                                results.add(entry);
                            }
                        } catch (Exception e) {
                            // 跳过格式错误的行
                        }
                    }
                } catch (IOException e) {
                    log.warn("读取结构化日志文件失败: {}", logFile, e);
                }
            }
        } catch (IOException e) {
            log.warn("扫描结构化日志目录失败: {}", logDir, e);
        }

        // 按时间倒序
        results.sort((a, b) -> {
            String tsA = (String) a.getOrDefault("timestamp", "");
            String tsB = (String) b.getOrDefault("timestamp", "");
            return tsB.compareTo(tsA);
        });

        // 分页
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, results.size());
        List<Map<String, Object>> pagedResults = fromIndex < results.size()
                ? results.subList(fromIndex, toIndex)
                : Collections.emptyList();

        StructuredLogQueryResult result = new StructuredLogQueryResult(pagedResults, totalEntries, page, pageSize);
        result.setLogDirectory(structuredLogDir);
        return result;
    }

    /**
     * 获取结构化日志统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "结构化日志统计")
    public Map<String, Object> getStructuredLogStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        Path logDir = Paths.get(structuredLogDir);
        stats.put("directory", structuredLogDir);

        if (!Files.exists(logDir)) {
            stats.put("exists", false);
            return stats;
        }
        stats.put("exists", true);

        // 统计文件列表
        try (Stream<Path> files = Files.list(logDir)) {
            List<Map<String, Object>> fileList = files
                    .filter(p -> p.getFileName().toString().startsWith("app."))
                    .map(p -> {
                        Map<String, Object> fileInfo = new LinkedHashMap<>();
                        fileInfo.put("name", p.getFileName().toString());
                        try {
                            fileInfo.put("sizeBytes", Files.size(p));
                            if (p.toString().endsWith(".jsonl")) {
                                long lineCount = Files.lines(p).count();
                                fileInfo.put("lineCount", lineCount);
                            }
                        } catch (IOException e) {
                            fileInfo.put("error", e.getMessage());
                        }
                        return fileInfo;
                    })
                    .sorted((a, b) -> ((String) b.get("name")).compareTo((String) a.get("name")))
                    .collect(Collectors.toList());
            stats.put("files", fileList);

            // 统计总大小
            long totalSize = fileList.stream()
                    .mapToLong(f -> ((Number) f.getOrDefault("sizeBytes", 0)).longValue())
                    .sum();
            stats.put("totalSizeBytes", totalSize);
        } catch (IOException e) {
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * 获取最近 N 条结构化错误日志（快捷接口）
     */
    @GetMapping("/recent")
    @Operation(summary = "获取最近的结构化错误日志")
    public List<Map<String, Object>> getRecentStructuredErrors(
            @Parameter(description = "条数限制") @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "级别: WARN 或 ERROR，默认 ERROR") @RequestParam(defaultValue = "ERROR") String level) {

        List<Map<String, Object>> results = new ArrayList<>();
        Path logFile = Paths.get(structuredLogDir, "app.jsonl");

        if (!Files.exists(logFile)) {
            return results;
        }

        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> entry = mapper.readValue(line, Map.class);
                    String entryLevel = (String) entry.getOrDefault("level", "");
                    if ("ERROR".equals(level)) {
                        if ("ERROR".equals(entryLevel)) {
                            results.add(entry);
                        }
                    } else if ("WARN".equals(level)) {
                        if ("WARN".equals(entryLevel) || "ERROR".equals(entryLevel)) {
                            results.add(entry);
                        }
                    } else {
                        results.add(entry);
                    }
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            log.warn("读取结构化日志失败", e);
        }

        // 倒序取最近 N 条
        Collections.reverse(results);
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    // ── 过滤逻辑 ──

    private boolean matchesFilter(Map<String, Object> entry, String level, String loggerPattern, String keyword) {
        // 级别过滤
        if (level != null && !level.isEmpty()) {
            String entryLevel = (String) entry.getOrDefault("level", "");
            if (!entryLevel.equalsIgnoreCase(level)) {
                return false;
            }
        }

        // Logger 模糊匹配
        if (loggerPattern != null && !loggerPattern.isEmpty()) {
            String entryLogger = (String) entry.getOrDefault("logger", "");
            if (!entryLogger.toLowerCase().contains(loggerPattern.toLowerCase())) {
                return false;
            }
        }

        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            String message = String.valueOf(entry.getOrDefault("message", ""));
            if (!message.toLowerCase().contains(keyword.toLowerCase())) {
                // 也搜索异常信息
                @SuppressWarnings("unchecked")
                Map<String, Object> error = (Map<String, Object>) entry.get("error");
                if (error == null) return false;
                String errorMsg = String.valueOf(error.getOrDefault("message", ""));
                String errorType = String.valueOf(error.getOrDefault("type", ""));
                if (!errorMsg.toLowerCase().contains(keyword.toLowerCase())
                        && !errorType.toLowerCase().contains(keyword.toLowerCase())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 日期范围过滤：检查日志条目的 timestamp 是否在指定范围内
     */
    private boolean matchesDateRange(Map<String, Object> entry, String startDateStr, String endDateStr) {
        if (startDateStr == null && endDateStr == null) return true;

        String timestamp = (String) entry.getOrDefault("timestamp", "");
        if (timestamp.isEmpty() || timestamp.length() < 10) return true; // 无法解析则保留

        String entryDate = timestamp.substring(0, 10); // yyyy-MM-dd

        if (startDateStr != null && entryDate.compareTo(startDateStr) < 0) return false;
        if (endDateStr != null && entryDate.compareTo(endDateStr) > 0) return false;

        return true;
    }

    // ── DTO ──

    @Data
    public static class StructuredLogQueryResult {
        private List<Map<String, Object>> records;
        private long total;
        private int page;
        private int pageSize;
        private String logDirectory;

        public StructuredLogQueryResult(List<Map<String, Object>> records, long total, int page, int pageSize) {
            this.records = records;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
    }
}
