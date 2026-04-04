package cn.aiedge.export.controller;

import cn.aiedge.export.template.*;
import cn.aiedge.export.template.ImportTemplateDefinition.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 导入模板管理控制器
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/import-templates")
@RequiredArgsConstructor
@Tag(name = "导入模板管理", description = "模板定义、生成、校验接口")
public class ImportTemplateController {

    private final ImportTemplateRegistry templateRegistry;
    private final ImportTemplateGenerator templateGenerator;
    private final ImportTemplateValidator templateValidator;

    // ==================== 模板管理 ====================

    @GetMapping
    @Operation(summary = "获取所有模板列表")
    public ResponseEntity<List<ImportTemplateDefinition>> getAllTemplates() {
        return ResponseEntity.ok(templateRegistry.getAllTemplates());
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "获取模板详情")
    public ResponseEntity<ImportTemplateDefinition> getTemplate(@PathVariable String templateId) {
        ImportTemplateDefinition template = templateRegistry.getTemplate(templateId);
        return template != null ? ResponseEntity.ok(template) : ResponseEntity.notFound().build();
    }

    @GetMapping("/data-type/{dataType}")
    @Operation(summary = "根据数据类型获取模板")
    public ResponseEntity<ImportTemplateDefinition> getTemplateByDataType(@PathVariable String dataType) {
        ImportTemplateDefinition template = templateRegistry.getTemplateByDataType(dataType);
        return template != null ? ResponseEntity.ok(template) : ResponseEntity.notFound().build();
    }

    @GetMapping("/data-types")
    @Operation(summary = "获取支持的数据类型")
    public ResponseEntity<Set<String>> getSupportedDataTypes() {
        return ResponseEntity.ok(templateRegistry.getSupportedDataTypes());
    }

    @PostMapping
    @Operation(summary = "注册新模板")
    public ResponseEntity<Map<String, Object>> registerTemplate(@RequestBody ImportTemplateDefinition template) {
        try {
            templateRegistry.register(template);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "templateId", template.getTemplateId(),
                "message", "模板注册成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "更新模板")
    public ResponseEntity<Map<String, Object>> updateTemplate(
            @PathVariable String templateId, 
            @RequestBody ImportTemplateDefinition template) {
        try {
            template.setTemplateId(templateId);
            templateRegistry.updateTemplate(template);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "模板更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "删除模板")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable String templateId) {
        boolean removed = templateRegistry.removeTemplate(templateId);
        return ResponseEntity.ok(Map.of(
            "success", removed,
            "message", removed ? "模板删除成功" : "模板不存在"
        ));
    }

    // ==================== 模板下载 ====================

    @GetMapping("/{templateId}/download")
    @Operation(summary = "下载导入模板")
    public void downloadTemplate(@PathVariable String templateId, HttpServletResponse response) throws IOException {
        ImportTemplateDefinition template = templateRegistry.getTemplate(templateId);
        if (template == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "模板不存在");
            return;
        }
        
        byte[] data = templateGenerator.generateTemplate(template);
        
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
            "attachment; filename=" + encodeFilename(template.getTemplateName()) + ".xlsx");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    @GetMapping("/data-type/{dataType}/download")
    @Operation(summary = "根据数据类型下载模板")
    public void downloadTemplateByDataType(@PathVariable String dataType, HttpServletResponse response) throws IOException {
        ImportTemplateDefinition template = templateRegistry.getTemplateByDataType(dataType);
        if (template == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "不支持的数据类型: " + dataType);
            return;
        }
        
        byte[] data = templateGenerator.generateTemplate(template);
        
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
            "attachment; filename=" + encodeFilename(template.getTemplateName()) + ".xlsx");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    // ==================== 数据校验 ====================

    @PostMapping("/{templateId}/validate")
    @Operation(summary = "校验导入数据")
    public ResponseEntity<ImportTemplateValidator.ValidationResult> validateData(
            @PathVariable String templateId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        ImportTemplateDefinition template = templateRegistry.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        // TODO: 解析Excel文件获取数据
        List<Map<String, Object>> data = parseExcelFile(file);
        
        ImportTemplateValidator.ValidationResult result = templateValidator.validate(template, data);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{templateId}/validate-row")
    @Operation(summary = "校验单行数据")
    public ResponseEntity<ImportTemplateValidator.RowValidationResult> validateRow(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> rowData,
            @RequestParam(defaultValue = "1") int rowNum) {
        
        ImportTemplateDefinition template = templateRegistry.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        ImportTemplateValidator.RowValidationResult result = 
            templateValidator.validateRow(template, rowData, rowNum);
        
        return ResponseEntity.ok(result);
    }

    // ==================== 模板预览 ====================

    @GetMapping("/{templateId}/preview")
    @Operation(summary = "预览模板结构")
    public ResponseEntity<TemplatePreview> previewTemplate(@PathVariable String templateId) {
        ImportTemplateDefinition template = templateRegistry.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<FieldPreview> fields = new ArrayList<>();
        for (TemplateField field : template.getFields()) {
            fields.add(new FieldPreview(
                field.getFieldName(),
                field.getFieldTitle(),
                field.getFieldType().name(),
                field.isRequired(),
                field.getMaxLength(),
                field.getDropdownOptions(),
                field.getSampleValue(),
                field.getDescription()
            ));
        }
        
        return ResponseEntity.ok(new TemplatePreview(
            template.getTemplateId(),
            template.getTemplateName(),
            template.getDescription(),
            template.getMaxImportRows(),
            fields
        ));
    }

    // ==================== 字段校验规则 ====================

    @GetMapping("/{templateId}/fields/{fieldName}/validation")
    @Operation(summary = "获取字段校验规则")
    public ResponseEntity<FieldValidationRules> getFieldValidationRules(
            @PathVariable String templateId,
            @PathVariable String fieldName) {
        
        ImportTemplateDefinition template = templateRegistry.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        TemplateField field = template.getFields().stream()
            .filter(f -> f.getFieldName().equals(fieldName))
            .findFirst()
            .orElse(null);
        
        if (field == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(new FieldValidationRules(
            field.getFieldName(),
            field.getFieldType().name(),
            field.isRequired(),
            field.getMaxLength(),
            field.getMinValue(),
            field.getMaxValue(),
            field.getRegexPattern(),
            field.getDropdownOptions(),
            field.isUnique()
        ));
    }

    // ==================== 辅助方法 ====================

    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private List<Map<String, Object>> parseExcelFile(MultipartFile file) throws IOException {
        // TODO: 实现Excel解析
        // 这里简化处理，实际应使用POI读取Excel
        return new ArrayList<>();
    }

    // ==================== DTO ====================

    public record TemplatePreview(
        String templateId,
        String templateName,
        String description,
        int maxImportRows,
        List<FieldPreview> fields
    ) {}

    public record FieldPreview(
        String fieldName,
        String fieldTitle,
        String fieldType,
        boolean required,
        Integer maxLength,
        Map<String, String> dropdownOptions,
        String sampleValue,
        String description
    ) {}

    public record FieldValidationRules(
        String fieldName,
        String fieldType,
        boolean required,
        Integer maxLength,
        Double minValue,
        Double maxValue,
        String regexPattern,
        Map<String, String> dropdownOptions,
        boolean unique
    ) {}
}
