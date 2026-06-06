package cn.aiedge.base.log;

import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 错误报告控制器 — 接收前端错误上报并提供查询接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/error-report")
@RequiredArgsConstructor
@Tag(name = "错误报告", description = "前端错误上报及错误日志查询")
public class ErrorReportController {

    private final ErrorLogService errorLogService;

    @PostMapping
    @Operation(summary = "接收前端错误上报")
    public Result<Void> reportError(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> errors = (List<Map<String, Object>>) body.getOrDefault("errors", Collections.emptyList());
            @SuppressWarnings("unchecked")
            Map<String, Object> environment = (Map<String, Object>) body.getOrDefault("environment", Collections.emptyMap());

            String userAgent = environment != null ? (String) environment.getOrDefault("userAgent", "") : "";
            String pageUrl = environment != null ? (String) environment.getOrDefault("url", "") : "";

            for (Map<String, Object> err : errors) {
                String type = (String) err.getOrDefault("type", "unknown");
                String message = extractErrorMessage(err);
                String stack = (String) err.getOrDefault("stack", "");
                String url = (String) err.getOrDefault("url", pageUrl);
                String ua = (String) err.getOrDefault("userAgent", userAgent);
                String ts = (String) err.getOrDefault("timestamp", "");

                @SuppressWarnings("unchecked")
                Map<String, Object> extra = (Map<String, Object>) err.getOrDefault("extra", null);

                errorLogService.logFrontendError(type, message, stack, url, ua, extra);
            }

            log.debug("[ErrorReport] 接收前端错误上报: {} 条", errors.size());
            return Result.success();
        } catch (Exception e) {
            log.warn("[ErrorReport] 处理前端错误上报失败: {}", e.getMessage());
            return Result.fail(400, "无效的错误报告数据");
        }
    }

    /**
     * 从错误 Map 中提取错误消息
     */
    private String extractErrorMessage(Map<String, Object> err) {
        Object error = err.get("error");
        if (error instanceof String) {
            return (String) error;
        }
        if (error instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> errMap = (Map<String, Object>) error;
            String msg = (String) errMap.get("message");
            if (msg != null) return msg;
            msg = (String) errMap.get("error");
            if (msg != null) return msg;
            return errMap.toString();
        }
        if (error != null) {
            return error.toString();
        }
        return (String) err.getOrDefault("message", "未知错误");
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近错误列表")
    public Result<List<ErrorLogEntry>> getRecentErrors(
            @Parameter(description = "条数限制") @RequestParam(defaultValue = "50") int limit) {
        return Result.success(errorLogService.getRecentErrors(limit));
    }

    @GetMapping("/query")
    @Operation(summary = "按日期范围查询错误日志")
    public Result<List<ErrorLogEntry>> queryErrors(
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "级别过滤") @RequestParam(required = false) String level,
            @Parameter(description = "来源过滤") @RequestParam(required = false) String source,
            @Parameter(description = "条数限制") @RequestParam(defaultValue = "200") int limit) {
        return Result.success(errorLogService.queryErrors(startDate, endDate, level, source, limit));
    }

    @GetMapping("/statistics")
    @Operation(summary = "错误日志统计信息")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(errorLogService.getStatistics());
    }
}
