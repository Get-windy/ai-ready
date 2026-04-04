package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysLoginLog;
import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.service.SysLoginLogService;
import cn.aiedge.base.service.SysOperLogService;
import cn.aiedge.base.vo.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志管理控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
@Tag(name = "日志管理", description = "操作日志、登录日志查询和导出")
public class LogManageController {

    private final SysOperLogService operLogService;
    private final SysLoginLogService loginLogService;
    
    private static final Long DEFAULT_TENANT_ID = 1L;

    // ==================== 操作日志 ====================

    @GetMapping("/oper/page")
    @Operation(summary = "分页查询操作日志")
    public Result<Page<SysOperLog>> pageOperLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        Page<SysOperLog> pageParam = new Page<>(page, pageSize);
        return Result.success(operLogService.pageLogs(pageParam, DEFAULT_TENANT_ID, userId, module, status, startTime, endTime));
    }

    @GetMapping("/oper/{id}")
    @Operation(summary = "获取操作日志详情")
    public Result<SysOperLog> getOperLog(@PathVariable Long id) {
        return Result.success(operLogService.getById(id));
    }

    @GetMapping("/oper/recent/{userId}")
    @Operation(summary = "获取用户最近操作日志")
    public Result<List<SysOperLog>> getRecentOperLogs(@PathVariable Long userId, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(operLogService.getRecentLogs(userId, limit));
    }

    @GetMapping("/oper/stats/module")
    @Operation(summary = "获取模块统计")
    public Result<List<Map<String, Object>>> getModuleStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(operLogService.getModuleStats(DEFAULT_TENANT_ID, startTime, endTime));
    }

    @GetMapping("/oper/stats/user")
    @Operation(summary = "获取用户操作统计")
    public Result<List<Map<String, Object>>> getUserStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(operLogService.getUserStats(DEFAULT_TENANT_ID, startTime, endTime, limit));
    }

    @GetMapping("/oper/export")
    @Operation(summary = "导出操作日志")
    public void exportOperLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) throws IOException {
        
        List<SysOperLog> logs = operLogService.exportLogs(DEFAULT_TENANT_ID, userId, module, startTime, endTime);
        
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + encodeFilename("oper_log") + ".csv");
        
        OutputStream out = response.getOutputStream();
        StringBuilder sb = new StringBuilder();
        
        // BOM头
        out.write('\ufeff');
        
        // 表头
        sb.append("ID,用户名,模块,操作,方法,请求URL,状态,耗时(ms),操作时间,IP\n");
        
        // 数据
        for (SysOperLog log : logs) {
            sb.append(log.getId()).append(",");
            sb.append(escapeCsv(log.getUsername())).append(",");
            sb.append(escapeCsv(log.getModule())).append(",");
            sb.append(escapeCsv(log.getAction())).append(",");
            sb.append(escapeCsv(log.getMethod())).append(",");
            sb.append(escapeCsv(log.getRequestUrl())).append(",");
            sb.append(log.getStatus() == 0 ? "成功" : "失败").append(",");
            sb.append(log.getCostTime()).append(",");
            sb.append(log.getOperTime()).append(",");
            sb.append(escapeCsv(log.getOperIp())).append("\n");
        }
        
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    @DeleteMapping("/oper/clean")
    @Operation(summary = "清理历史操作日志")
    public Result<Integer> cleanOperLogs(@RequestParam(defaultValue = "90") int days) {
        return Result.success(operLogService.cleanLogs(days));
    }

    // ==================== 登录日志 ====================

    @GetMapping("/login/page")
    @Operation(summary = "分页查询登录日志")
    public Result<Page<SysLoginLog>> pageLoginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer loginResult,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        Page<SysLoginLog> pageParam = new Page<>(page, pageSize);
        return Result.success(loginLogService.pageLoginLogs(pageParam, DEFAULT_TENANT_ID, username, loginResult, startTime, endTime));
    }

    @GetMapping("/login/{id}")
    @Operation(summary = "获取登录日志详情")
    public Result<SysLoginLog> getLoginLog(@PathVariable Long id) {
        return Result.success(loginLogService.getById(id));
    }

    @GetMapping("/login/recent/{userId}")
    @Operation(summary = "获取用户最近登录记录")
    public Result<List<SysLoginLog>> getRecentLoginLogs(@PathVariable Long userId, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(loginLogService.getRecentLogins(userId, limit));
    }

    @GetMapping("/login/stats/daily")
    @Operation(summary = "获取每日登录统计")
    public Result<List<Map<String, Object>>> getDailyLoginStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(loginLogService.countByDate(DEFAULT_TENANT_ID, startTime, endTime));
    }

    @GetMapping("/login/abnormal")
    @Operation(summary = "检测异常登录")
    public Result<List<Map<String, Object>>> detectAbnormalLogin(
            @RequestParam(defaultValue = "5") int threshold,
            @RequestParam(defaultValue = "30") int minutes) {
        return Result.success(loginLogService.detectAbnormalLogin(threshold, minutes));
    }

    @GetMapping("/login/export")
    @Operation(summary = "导出登录日志")
    public void exportLoginLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer loginResult,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) throws IOException {
        
        Page<SysLoginLog> pageParam = new Page<>(1, 10000);
        Page<SysLoginLog> logs = loginLogService.pageLoginLogs(pageParam, DEFAULT_TENANT_ID, username, loginResult, startTime, endTime);
        
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + encodeFilename("login_log") + ".csv");
        
        OutputStream out = response.getOutputStream();
        StringBuilder sb = new StringBuilder();
        
        // BOM头
        out.write('\ufeff');
        
        // 表头
        sb.append("ID,用户名,登录类型,登录结果,失败原因,登录IP,登录地点,浏览器,操作系统,登录时间\n");
        
        // 数据
        for (SysLoginLog log : logs.getRecords()) {
            sb.append(log.getId()).append(",");
            sb.append(escapeCsv(log.getUsername())).append(",");
            sb.append(log.getLoginType() == 1 ? "账号密码" : log.getLoginType() == 2 ? "短信验证" : "第三方").append(",");
            sb.append(log.getLoginResult() == 0 ? "成功" : "失败").append(",");
            sb.append(escapeCsv(log.getFailReason())).append(",");
            sb.append(escapeCsv(log.getLoginIp())).append(",");
            sb.append(escapeCsv(log.getLoginLocation())).append(",");
            sb.append(escapeCsv(log.getBrowser())).append(",");
            sb.append(escapeCsv(log.getOs())).append(",");
            sb.append(log.getLoginTime()).append("\n");
        }
        
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // ==================== 辅助方法 ====================

    private String encodeFilename(String filename) throws Exception {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
