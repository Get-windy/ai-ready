package cn.aiedge.report.controller;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import cn.aiedge.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Tag(name = "报表服务", description = "报表生成、导出、管理功能")
public class ReportController {

    private final ReportService reportService;

    /**
     * 获取报表列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取报表列表", description = "获取所有可用的报表定义")
    public ResponseEntity<Map<String, Object>> getReportList(
            @Parameter(description = "分类筛选") @RequestParam(required = false) String category,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<ReportDefinition> reports = reportService.getReportList(category, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("reports", reports);
        result.put("total", reports.size());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取报表定义
     */
    @GetMapping("/{reportId}")
    @Operation(summary = "获取报表定义", description = "获取指定报表的详细定义")
    public ResponseEntity<ReportDefinition> getReportDefinition(
            @Parameter(description = "报表ID") @PathVariable String reportId) {
        
        ReportDefinition definition = reportService.getReportDefinition(reportId);
        if (definition == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(definition);
    }

    /**
     * 生成报表
     */
    @PostMapping("/{reportId}/generate")
    @Operation(summary = "生成报表", description = "根据参数生成报表数据")
    public ResponseEntity<ReportData> generateReport(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @RequestBody(required = false) Map<String, Object> parameters,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("生成报表: reportId={}, params={}", reportId, parameters);
        
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        
        ReportData reportData = reportService.generateReport(reportId, parameters, tenantId);
        return ResponseEntity.ok(reportData);
    }

    /**
     * 预览报表
     */
    @PostMapping("/{reportId}/preview")
    @Operation(summary = "预览报表", description = "预览报表前N行数据")
    public ResponseEntity<ReportData> previewReport(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "最大行数") @RequestParam(defaultValue = "10") int maxRows,
            @RequestBody(required = false) Map<String, Object> parameters,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        
        ReportData reportData = reportService.previewReport(reportId, parameters, maxRows, tenantId);
        return ResponseEntity.ok(reportData);
    }

    /**
     * 导出Excel
     */
    @PostMapping("/{reportId}/export/excel")
    @Operation(summary = "导出Excel", description = "将报表导出为Excel文件")
    public void exportToExcel(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @RequestBody(required = false) Map<String, Object> parameters,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            HttpServletResponse response) throws IOException {
        
        log.info("导出Excel: reportId={}", reportId);
        
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        
        ReportDefinition definition = reportService.getReportDefinition(reportId);
        String fileName = definition != null ? definition.getReportName() : "report";
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", 
            "attachment; filename=" + encodeFilename(fileName) + ".xlsx");
        
        reportService.exportToExcel(reportId, parameters, response.getOutputStream(), tenantId);
    }

    /**
     * 导出CSV
     */
    @PostMapping("/{reportId}/export/csv")
    @Operation(summary = "导出CSV", description = "将报表导出为CSV文件")
    public void exportToCsv(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @RequestBody(required = false) Map<String, Object> parameters,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            HttpServletResponse response) throws IOException {
        
        log.info("导出CSV: reportId={}", reportId);
        
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        
        ReportDefinition definition = reportService.getReportDefinition(reportId);
        String fileName = definition != null ? definition.getReportName() : "report";
        
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", 
            "attachment; filename=" + encodeFilename(fileName) + ".csv");
        // 添加BOM以支持Excel正确识别UTF-8
        response.getOutputStream().write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
        
        reportService.exportToCsv(reportId, parameters, response.getOutputStream(), tenantId);
    }

    /**
     * 导出PDF
     */
    @PostMapping("/{reportId}/export/pdf")
    @Operation(summary = "导出PDF", description = "将报表导出为PDF文件")
    public void exportToPdf(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @RequestBody(required = false) Map<String, Object> parameters,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            HttpServletResponse response) throws IOException {
        
        log.info("导出PDF: reportId={}", reportId);
        
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        
        ReportDefinition definition = reportService.getReportDefinition(reportId);
        String fileName = definition != null ? definition.getReportName() : "report";
        
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", 
            "attachment; filename=" + encodeFilename(fileName) + ".pdf");
        
        reportService.exportToPdf(reportId, parameters, response.getOutputStream(), tenantId);
    }

    /**
     * 创建报表定义
     */
    @PostMapping
    @Operation(summary = "创建报表定义", description = "创建新的自定义报表")
    public ResponseEntity<ReportDefinition> createReport(
            @RequestBody ReportDefinition definition,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("创建报表: {}", definition.getReportName());
        
        ReportDefinition saved = reportService.saveReportDefinition(definition, tenantId);
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新报表定义
     */
    @PutMapping("/{reportId}")
    @Operation(summary = "更新报表定义", description = "更新已有的报表定义")
    public ResponseEntity<ReportDefinition> updateReport(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @RequestBody ReportDefinition definition,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("更新报表: {}", reportId);
        
        definition.setReportId(reportId);
        ReportDefinition saved = reportService.saveReportDefinition(definition, tenantId);
        return ResponseEntity.ok(saved);
    }

    /**
     * 删除报表定义
     */
    @DeleteMapping("/{reportId}")
    @Operation(summary = "删除报表定义", description = "删除指定的报表定义")
    public ResponseEntity<Map<String, Object>> deleteReport(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("删除报表: {}", reportId);
        
        boolean success = reportService.deleteReportDefinition(reportId, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败，可能报表不存在或为内置报表");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 复制报表定义
     */
    @PostMapping("/{reportId}/copy")
    @Operation(summary = "复制报表定义", description = "复制现有报表创建新报表")
    public ResponseEntity<ReportDefinition> copyReport(
            @Parameter(description = "源报表ID") @PathVariable String reportId,
            @Parameter(description = "新报表名称") @RequestParam String newName,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("复制报表: {} -> {}", reportId, newName);
        
        ReportDefinition copied = reportService.copyReportDefinition(reportId, newName, tenantId);
        return ResponseEntity.ok(copied);
    }

    /**
     * 获取报表分类列表
     */
    @GetMapping("/categories")
    @Operation(summary = "获取报表分类", description = "获取所有报表分类")
    public ResponseEntity<Map<String, Object>> getCategories() {
        List<Map<String, String>> categories = List.of(
            Map.of("code", "sales", "name", "销售报表"),
            Map.of("code", "customer", "name", "客户报表"),
            Map.of("code", "product", "name", "产品报表"),
            Map.of("code", "order", "name", "订单报表"),
            Map.of("code", "finance", "name", "财务报表"),
            Map.of("code", "inventory", "name", "库存报表")
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("categories", categories);
        result.put("total", categories.size());
        
        return ResponseEntity.ok(result);
    }

    // ==================== 辅助方法 ====================

    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
