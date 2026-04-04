package cn.aiedge.export.controller;

import cn.aiedge.export.service.DataImportService;
import cn.aiedge.export.service.DataImportService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据导入控制器
 * 
 * 提供Excel和CSV文件导入、预览、校验功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/import/v2")
@RequiredArgsConstructor
@Tag(name = "数据导入", description = "Excel/CSV导入、预览、校验功能")
public class DataImportController {

    private final DataImportService dataImportService;

    /**
     * 导入Excel文件
     */
    @PostMapping("/excel/{dataType}")
    @Operation(summary = "导入Excel文件", description = "上传Excel文件并导入数据")
    public ResponseEntity<Map<String, Object>> importExcel(
            @Parameter(description = "数据类型: customer/product/order/user") @PathVariable String dataType,
            @Parameter(description = "Excel文件") @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) throws IOException {
        
        log.info("Excel导入请求: dataType={}, fileName={}", dataType, file.getOriginalFilename());
        
        // 验证文件格式
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "文件格式错误，仅支持.xlsx和.xls格式"
            ));
        }

        ImportResult result = dataImportService.importExcel(file.getInputStream(), dataType, tenantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("totalCount", result.totalCount());
        response.put("successCount", result.successCount());
        response.put("failureCount", result.failureCount());
        response.put("message", result.message());
        
        if (!result.errors().isEmpty()) {
            response.put("errors", result.errors());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 导入CSV文件
     */
    @PostMapping("/csv/{dataType}")
    @Operation(summary = "导入CSV文件", description = "上传CSV文件并导入数据")
    public ResponseEntity<Map<String, Object>> importCsv(
            @Parameter(description = "数据类型: customer/product/order/user") @PathVariable String dataType,
            @Parameter(description = "CSV文件") @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) throws IOException {
        
        log.info("CSV导入请求: dataType={}, fileName={}", dataType, file.getOriginalFilename());
        
        // 验证文件格式
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".csv")) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "文件格式错误，仅支持.csv格式"
            ));
        }

        ImportResult result = dataImportService.importCsv(file.getInputStream(), dataType, tenantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("totalCount", result.totalCount());
        response.put("successCount", result.successCount());
        response.put("failureCount", result.failureCount());
        response.put("message", result.message());
        
        if (!result.errors().isEmpty()) {
            response.put("errors", result.errors());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 预览导入数据
     */
    @PostMapping("/preview/{dataType}")
    @Operation(summary = "预览导入数据", description = "预览文件前N行数据，不执行导入")
    public ResponseEntity<Map<String, Object>> preview(
            @Parameter(description = "数据类型") @PathVariable String dataType,
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "预览行数") @RequestParam(defaultValue = "10") int maxRows) throws IOException {
        
        log.info("预览数据: dataType={}, maxRows={}", dataType, maxRows);
        
        PreviewResult result = dataImportService.preview(file.getInputStream(), dataType, maxRows);
        
        Map<String, Object> response = new HashMap<>();
        response.put("headers", result.headers());
        response.put("rows", result.rows());
        response.put("totalRows", result.totalRows());
        response.put("previewRows", result.previewRows());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 校验导入数据
     */
    @PostMapping("/validate/{dataType}")
    @Operation(summary = "校验导入数据", description = "校验文件数据是否符合规则")
    public ResponseEntity<Map<String, Object>> validate(
            @Parameter(description = "数据类型") @PathVariable String dataType,
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file) throws IOException {
        
        log.info("校验数据: dataType={}", dataType);
        
        // 先预览获取所有数据
        PreviewResult previewResult = dataImportService.preview(file.getInputStream(), dataType, Integer.MAX_VALUE);
        
        // 执行校验
        ValidateResult result = dataImportService.validate(previewResult.rows(), dataType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.valid());
        response.put("totalRows", result.totalRows());
        response.put("validRows", result.validRows());
        response.put("invalidRows", result.invalidRows());
        
        if (!result.errors().isEmpty()) {
            response.put("errors", result.errors().stream().limit(100).toList());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取字段定义
     */
    @GetMapping("/fields/{dataType}")
    @Operation(summary = "获取字段定义", description = "获取指定数据类型的导入字段定义")
    public ResponseEntity<Map<String, Object>> getFieldDefinitions(
            @Parameter(description = "数据类型") @PathVariable String dataType) {
        
        List<FieldDefinition> fields = dataImportService.getFieldDefinitions(dataType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("dataType", dataType);
        response.put("fields", fields);
        response.put("fieldCount", fields.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取支持的数据类型
     */
    @GetMapping("/datatypes")
    @Operation(summary = "获取支持的数据类型", description = "获取系统支持的所有导入数据类型")
    public ResponseEntity<Map<String, Object>> getSupportedDataTypes() {
        List<Map<String, Object>> dataTypes = List.of(
            Map.of("type", "customer", "name", "客户数据", "description", "导入客户基础信息"),
            Map.of("type", "product", "name", "产品数据", "description", "导入产品基础信息"),
            Map.of("type", "order", "name", "订单数据", "description", "导入订单记录"),
            Map.of("type", "user", "name", "用户数据", "description", "导入系统用户")
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("dataTypes", dataTypes);
        response.put("count", dataTypes.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/template/{dataType}")
    @Operation(summary = "下载导入模板", description = "下载指定数据类型的导入模板文件")
    public void downloadTemplate(
            @Parameter(description = "数据类型") @PathVariable String dataType,
            HttpServletResponse response) throws IOException {
        
        log.info("下载模板: dataType={}", dataType);
        
        List<FieldDefinition> fields = dataImportService.getFieldDefinitions(dataType);
        if (fields.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "不支持的数据类型: " + dataType);
            return;
        }
        
        // 生成模板Excel
        byte[] templateData = generateTemplateExcel(fields, dataType);
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", 
            "attachment; filename=" + encodeFilename(dataType + "_import_template") + ".xlsx");
        response.setHeader("Content-Length", String.valueOf(templateData.length));
        response.getOutputStream().write(templateData);
        response.getOutputStream().flush();
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成模板Excel
     */
    private byte[] generateTemplateExcel(List<FieldDefinition> fields, String dataType) throws IOException {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(dataType + "导入模板");
            
            // 创建表头行
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(workbook.createFont());
            headerStyle.getFont().setBold(true);
            
            // 创建说明行
            org.apache.poi.ss.usermodel.Row descRow = sheet.createRow(1);
            org.apache.poi.ss.usermodel.CellStyle descStyle = workbook.createCellStyle();
            descStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.YELLOW.getIndex());
            descStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            
            // 创建示例数据行
            org.apache.poi.ss.usermodel.Row sampleRow = sheet.createRow(2);
            
            for (int i = 0; i < fields.size(); i++) {
                FieldDefinition field = fields.get(i);
                
                // 表头
                org.apache.poi.ss.usermodel.Cell headerCell = headerRow.createCell(i);
                String headerName = field.required() ? field.label() + "*" : field.label();
                headerCell.setCellValue(headerName);
                headerCell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256);
                
                // 说明
                org.apache.poi.ss.usermodel.Cell descCell = descRow.createCell(i);
                descCell.setCellValue(field.description() != null ? field.description() : "");
                descCell.setCellStyle(descStyle);
                
                // 示例数据
                org.apache.poi.ss.usermodel.Cell sampleCell = sampleRow.createCell(i);
                sampleCell.setCellValue(getSampleValue(field));
            }
            
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 获取字段示例值
     */
    private String getSampleValue(FieldDefinition field) {
        if (field.options() != null && !field.options().isEmpty()) {
            return field.options().get(0).label();
        }
        return switch (field.type()) {
            case "string" -> "示例文本";
            case "integer" -> "100";
            case "decimal" -> "99.99";
            case "date" -> "2026-01-01";
            default -> "";
        };
    }

    /**
     * 编码文件名
     */
    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
