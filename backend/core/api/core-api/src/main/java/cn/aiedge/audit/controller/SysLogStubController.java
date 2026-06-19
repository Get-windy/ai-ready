package cn.aiedge.audit.controller;

import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.mapper.SysOperLogMapper;
import cn.aiedge.base.vo.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统日志 Controller
 * 提供操作日志的分页查询、详情、模块/操作类型列表、清理、导出等功能
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "系统日志", description = "操作日志查询、导出和管理")
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class SysLogStubController {

    private final SysOperLogMapper operLogMapper;

    private static final Long DEFAULT_TENANT_ID = 1L;

    @Operation(summary = "日志分页查询")
    @GetMapping("/page")
    public Result<Page<SysOperLog>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @RequestParam(required = false) Integer status) {

        Page<SysOperLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);

        if (module != null && !module.isEmpty()) {
            wrapper.eq(SysOperLog::getModule, module);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(SysOperLog::getAction, operationType);
        }
        if (operatorName != null && !operatorName.isEmpty()) {
            wrapper.like(SysOperLog::getUsername, operatorName);
        }
        if (status != null) {
            wrapper.eq(SysOperLog::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(SysOperLog::getOperTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(SysOperLog::getOperTime, endDate);
        }

        wrapper.orderByDesc(SysOperLog::getOperTime);

        Page<SysOperLog> result = operLogMapper.selectPage(page, wrapper);
        return Result.ok(result);
    }

    @Operation(summary = "获取日志详情")
    @GetMapping("/{id}")
    public Result<SysOperLog> getById(@PathVariable Long id) {
        SysOperLog log = operLogMapper.selectById(id);
        return Result.ok(log);
    }

    @Operation(summary = "获取模块列表")
    @GetMapping("/modules")
    public Result<List<String>> modules() {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID)
               .isNotNull(SysOperLog::getModule)
               .ne(SysOperLog::getModule, "")
               .select(SysOperLog::getModule)
               .groupBy(SysOperLog::getModule)
               .orderByAsc(SysOperLog::getModule);

        List<String> modules = operLogMapper.selectList(wrapper)
                .stream()
                .map(SysOperLog::getModule)
                .collect(Collectors.toList());
        return Result.ok(modules);
    }

    @Operation(summary = "获取操作类型列表")
    @GetMapping("/operation-types")
    public Result<List<String>> operationTypes() {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID)
               .isNotNull(SysOperLog::getAction)
               .ne(SysOperLog::getAction, "")
               .select(SysOperLog::getAction)
               .groupBy(SysOperLog::getAction)
               .orderByAsc(SysOperLog::getAction);

        List<String> types = operLogMapper.selectList(wrapper)
                .stream()
                .map(SysOperLog::getAction)
                .collect(Collectors.toList());
        return Result.ok(types);
    }

    @Operation(summary = "清空旧日志")
    @DeleteMapping("/clear")
    public Result<Integer> clearLogs(
            @RequestParam(defaultValue = "90") int days) {
        if (days < 7) {
            return Result.fail("日志保留天数不能少于7天");
        }
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID)
               .lt(SysOperLog::getOperTime, threshold);
        int deleted = operLogMapper.delete(wrapper);
        return Result.ok(deleted);
    }

    @Operation(summary = "导出日志")
    @GetMapping("/export")
    public void exportLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            HttpServletResponse response) throws Exception {

        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);

        if (module != null && !module.isEmpty()) {
            wrapper.eq(SysOperLog::getModule, module);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(SysOperLog::getAction, operationType);
        }
        if (operatorName != null && !operatorName.isEmpty()) {
            wrapper.like(SysOperLog::getUsername, operatorName);
        }
        if (startDate != null) {
            wrapper.ge(SysOperLog::getOperTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(SysOperLog::getOperTime, endDate);
        }

        wrapper.orderByDesc(SysOperLog::getOperTime);

        // 限制导出量，最多10000条
        Page<SysOperLog> page = new Page<>(1, 10000);
        Page<SysOperLog> result = operLogMapper.selectPage(page, wrapper);
        List<SysOperLog> logs = result.getRecords();

        response.setContentType("text/csv;charset=UTF-8");
        String filename = URLEncoder.encode("oper_log.csv", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        OutputStream out = response.getOutputStream();

        // BOM头（用于Excel正确识别UTF-8编码）
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);

        // 表头
        StringBuilder sb = new StringBuilder();
        sb.append("ID,模块,操作类型,请求URL,请求方法,操作人,IP地址,操作时间,耗时(ms),状态,请求参数,响应结果\n");

        // 数据行
        for (SysOperLog log : logs) {
            sb.append(log.getId()).append(",");
            sb.append(escapeCsv(log.getModule())).append(",");
            sb.append(escapeCsv(log.getAction())).append(",");
            sb.append(escapeCsv(log.getRequestUrl())).append(",");
            sb.append(escapeCsv(log.getRequestMethod())).append(",");
            sb.append(escapeCsv(log.getUsername())).append(",");
            sb.append(escapeCsv(log.getOperIp())).append(",");
            sb.append(log.getOperTime() != null ? log.getOperTime() : "").append(",");
            sb.append(log.getCostTime() != null ? log.getCostTime() : "").append(",");
            sb.append(log.getStatus() != null && log.getStatus() == 0 ? "成功" : "失败").append(",");
            sb.append(escapeCsv(log.getRequestParams())).append(",");
            sb.append(escapeCsv(log.getResponseResult())).append("\n");
        }

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // ==================== 辅助方法 ====================

    /**
     * CSV字段转义：包含逗号、双引号或换行符时用双引号包裹
     */
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
