package cn.aiedge.export.controller;

import cn.aiedge.export.service.BatchImportService;
import cn.aiedge.export.service.impl.BatchImportServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 批量导入控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Tag(name = "批量导入", description = "Excel/CSV批量导入、进度追踪、模板下载")
public class BatchImportController {

    private final BatchImportService batchImportService;
    private final BatchImportServiceImpl batchImportServiceImpl;

    @PostMapping("/upload")
    @Operation(summary = "上传导入文件")
    public ResponseEntity<Map<String, Object>> uploadImportFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataType") String dataType) throws IOException {
        
        // 创建任务
        String taskId = batchImportService.createImportTask(dataType, file.getOriginalFilename());
        
        // 异步执行导入
        batchImportServiceImpl.executeImport(taskId, dataType, file.getInputStream(), null);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "taskId", taskId,
            "message", "导入任务已创建"
        ));
    }

    @GetMapping("/progress/{taskId}")
    @Operation(summary = "获取导入进度")
    public ResponseEntity<BatchImportService.ImportProgress> getProgress(@PathVariable String taskId) {
        BatchImportService.ImportProgress progress = batchImportService.getProgress(taskId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }

    @PostMapping("/cancel/{taskId}")
    @Operation(summary = "取消导入任务")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        boolean success = batchImportService.cancelTask(taskId);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "任务已取消" : "无法取消任务"
        ));
    }

    @GetMapping("/template/{dataType}")
    @Operation(summary = "下载导入模板")
    public void downloadTemplate(@PathVariable String dataType, HttpServletResponse response) throws IOException {
        BatchImportService.ImportTemplate template = batchImportService.getTemplate(dataType);
        
        byte[] data = batchImportServiceImpl.downloadTemplate(dataType);
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", 
            "attachment; filename=" + encodeFilename(template.templateName()) + ".xlsx");
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    @GetMapping("/template/{dataType}/config")
    @Operation(summary = "获取模板配置")
    public ResponseEntity<BatchImportService.ImportTemplate> getTemplateConfig(@PathVariable String dataType) {
        return ResponseEntity.ok(batchImportService.getTemplate(dataType));
    }

    @GetMapping("/datatypes")
    @Operation(summary = "获取支持的数据类型")
    public ResponseEntity<Map<String, String>> getSupportedDataTypes() {
        return ResponseEntity.ok(Map.of(
            "user", "用户数据",
            "customer", "客户数据",
            "product", "产品数据"
        ));
    }

    // ==================== 辅助方法 ====================

    private String encodeFilename(String filename) throws Exception {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
