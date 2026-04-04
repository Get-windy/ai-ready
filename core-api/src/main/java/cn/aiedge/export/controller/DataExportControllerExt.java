package cn.aiedge.export.controller;

import cn.aiedge.export.handler.ExportConfig;
import cn.aiedge.export.service.DataExportService;
import cn.aiedge.export.service.PdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 数据导出控制器扩展
 * 支持大数据量导出和PDF导出
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "数据导出扩展", description = "大数据量导出和PDF导出接口")
public class DataExportControllerExt {

    private final DataExportService dataExportService;
    private final PdfExportService pdfExportService;

    @PostMapping("/batch/excel")
    @Operation(summary = "批量导出Excel（大数据量）")
    public void exportBatchExcel(
            @RequestBody BatchExportRequest request,
            HttpServletResponse response) throws Exception {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodeFilename(request.getFilename()) + ".xlsx");

        // 使用分批导出
        dataExportService.exportExcelBatch(
                (page, size) -> fetchData(request.getDataType(), request.getFilters(), page, size),
                request.getHeaders(),
                response.getOutputStream(),
                request.getBatchSize()
        );
    }

    @PostMapping("/pdf/export")
    @Operation(summary = "导出PDF")
    public void exportPdf(
            @RequestBody PdfExportRequest request,
            HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodeFilename(request.getFilename()) + ".pdf");

        // 获取数据
        List<?> data = fetchData(request.getDataType(), request.getFilters(), 1, 10000);

        // 导出PDF
        pdfExportService.exportPdf(data, request.getHeaders(), request.getTitle(),
                response.getOutputStream());
    }

    @PostMapping("/pdf/report")
    @Operation(summary = "导出PDF报告")
    public void exportPdfReport(
            @RequestBody PdfReportRequest request,
            HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodeFilename(request.getFilename()) + ".pdf");

        pdfExportService.exportReport(request.getContent(), request.getTitle(),
                response.getOutputStream());
    }

    @GetMapping("/template/{dataType}")
    @Operation(summary = "获取导出模板配置")
    public ExportConfig getExportConfig(
            @Parameter(description = "数据类型") @PathVariable String dataType) {

        ExportConfig config = new ExportConfig();
        config.setDataType(dataType);
        config.setHeaders(getHeadersForType(dataType));
        config.setSupportedFormats(List.of("xlsx", "csv", "pdf"));

        return config;
    }

    // ==================== 辅助方法 ====================

    private String encodeFilename(String filename) throws Exception {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private List<?> fetchData(String dataType, Map<String, Object> filters, int page, int size) {
        // 实际应用中应调用对应的Service进行分页查询
        // 这里返回空列表作为示例
        return List.of();
    }

    private Map<String, String> getHeadersForType(String dataType) {
        return switch (dataType) {
            case "user" -> Map.of(
                    "id", "ID",
                    "username", "用户名",
                    "email", "邮箱",
                    "phone", "手机号",
                    "status", "状态",
                    "createTime", "创建时间"
            );
            case "product" -> Map.of(
                    "id", "ID",
                    "productCode", "产品编码",
                    "productName", "产品名称",
                    "price", "价格",
                    "stock", "库存",
                    "status", "状态"
            );
            case "customer" -> Map.of(
                    "id", "ID",
                    "customerName", "客户名称",
                    "contact", "联系人",
                    "phone", "联系电话",
                    "email", "邮箱",
                    "address", "地址"
            );
            case "order" -> Map.of(
                    "id", "ID",
                    "orderNo", "订单号",
                    "customerName", "客户名称",
                    "totalAmount", "总金额",
                    "status", "状态",
                    "createTime", "创建时间"
            );
            default -> Map.of();
        };
    }

    // ==================== 请求DTO ====================

    @lombok.Data
    public static class BatchExportRequest {
        private String filename;
        private String dataType;
        private Map<String, String> headers;
        private Map<String, Object> filters;
        private int batchSize = 1000;
    }

    @lombok.Data
    public static class PdfExportRequest {
        private String filename;
        private String dataType;
        private String title;
        private Map<String, String> headers;
        private Map<String, Object> filters;
    }

    @lombok.Data
    public static class PdfReportRequest {
        private String filename;
        private String title;
        private String content;
    }
}
