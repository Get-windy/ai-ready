package cn.aiedge.export.controller;

import cn.aiedge.export.handler.ExportConfig;
import cn.aiedge.export.service.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 数据导入导出控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "数据导入导出", description = "Excel/CSV导入导出接口")
public class DataExportController {

    private final DataExportService dataExportService;

    @PostMapping("/excel/export")
    @Operation(summary = "导出Excel")
    public void exportExcel(
            @RequestBody ExportRequest request,
            HttpServletResponse response) throws Exception {

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", 
                "attachment; filename=" + encodeFilename(request.getFilename()) + ".xlsx");

        // 导出（实际应用中从数据库查询）
        // dataExportService.exportExcel(data, request.getHeaders(), response.getOutputStream());
    }

    @PostMapping("/csv/export")
    @Operation(summary = "导出CSV")
    public void exportCsv(
            @RequestBody ExportRequest request,
            HttpServletResponse response) throws Exception {

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", 
                "attachment; filename=" + encodeFilename(request.getFilename()) + ".csv");

        // dataExportService.exportCsv(data, request.getHeaders(), response.getOutputStream());
    }

    @PostMapping("/excel/import")
    @Operation(summary = "导入Excel")
    public DataExportService.ImportResult importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataType") String dataType) throws Exception {

        // 根据dataType确定对应的实体类和表头
        Map<String, String> headers = getHeadersForType(dataType);
        Class<?> rowClass = getClassForType(dataType);

        try (InputStream in = file.getInputStream()) {
            List<?> data = dataExportService.importExcel(in, headers, rowClass);
            return new DataExportService.ImportResult(data.size(), data.size(), 0, List.of());
        }
    }

    @PostMapping("/csv/import")
    @Operation(summary = "导入CSV")
    public DataExportService.ImportResult importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataType") String dataType) throws Exception {

        Map<String, String> headers = getHeadersForType(dataType);
        Class<?> rowClass = getClassForType(dataType);

        try (InputStream in = file.getInputStream()) {
            List<?> data = dataExportService.importCsv(in, headers, rowClass);
            return new DataExportService.ImportResult(data.size(), data.size(), 0, List.of());
        }
    }

    @PostMapping("/template/download")
    @Operation(summary = "下载导入模板")
    public void downloadTemplate(
            @RequestParam("dataType") String dataType,
            HttpServletResponse response) throws Exception {

        Map<String, String> headers = getHeadersForType(dataType);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", 
                "attachment; filename=" + dataType + "_template.xlsx");

        // 创建空Excel（只有表头）
        dataExportService.exportExcel(List.of(), headers, response.getOutputStream());
    }

    // ==================== 辅助方法 ====================

    private String encodeFilename(String filename) throws Exception {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private Map<String, String> getHeadersForType(String dataType) {
        // 实际应用中应从配置或数据库读取
        return switch (dataType) {
            case "user" -> Map.of("username", "用户名", "email", "邮箱", "phone", "手机号");
            case "product" -> Map.of("productCode", "产品编码", "productName", "产品名称", "price", "价格");
            case "customer" -> Map.of("customerName", "客户名称", "contact", "联系人", "phone", "联系电话");
            default -> Map.of();
        };
    }

    private Class<?> getClassForType(String dataType) {
        // 实际应用中应返回对应的DTO类
        return Map.class;
    }

    // ==================== 请求DTO ====================

    @lombok.Data
    public static class ExportRequest {
        private String filename;
        private Map<String, String> headers;
        private String dataType;
        private Map<String, Object> filters;
    }
}
