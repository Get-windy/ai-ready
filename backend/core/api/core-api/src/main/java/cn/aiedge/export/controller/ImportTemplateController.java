package cn.aiedge.export.controller;

import cn.aiedge.export.template.*;
import cn.aiedge.export.template.ImportTemplateDefinition.*;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 导入模板管理控制器
 *
 * <p><b>数据源（2026-09-19 落库改造）</b>：模板定义真实读写库表 {@code dev_template}
 * （经 {@link ImportTemplateStore}，迁移 {@code V11.415.0}），
 * <b>不再</b>是原先的进程内存注册表 {@code ImportTemplateRegistry}（已删除）。
 * 因此新增/修改/删除都会持久化：重启不复原、多个实例读同一份数据。
 * 本类 13 个端点的路径、入参、返回结构未变。
 *
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/import-templates")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "导入模板管理", description = "模板定义、生成、校验接口")
public class ImportTemplateController {

    private final ImportTemplateStore templateStore;
    private final ImportTemplateGenerator templateGenerator;
    private final ImportTemplateValidator templateValidator;

    // ==================== 模板管理 ====================

    @SaCheckPermission("system:import-template:view")
    @GetMapping
    @Operation(summary = "获取所有模板列表")
    public ResponseEntity<List<ImportTemplateDefinition>> getAllTemplates() {
        return ResponseEntity.ok(templateStore.getAllTemplates());
    }

    @SaCheckPermission("system:import-template:detail")
    @GetMapping("/{templateId}")
    @Operation(summary = "获取模板详情")
    public ResponseEntity<ImportTemplateDefinition> getTemplate(@PathVariable String templateId) {
        ImportTemplateDefinition template = templateStore.getTemplate(templateId);
        return template != null ? ResponseEntity.ok(template) : ResponseEntity.notFound().build();
    }

    @SaCheckPermission("system:import-template:detail")
    @GetMapping("/data-type/{dataType}")
    @Operation(summary = "根据数据类型获取模板")
    public ResponseEntity<ImportTemplateDefinition> getTemplateByDataType(@PathVariable String dataType) {
        ImportTemplateDefinition template = templateStore.getTemplateByDataType(dataType);
        return template != null ? ResponseEntity.ok(template) : ResponseEntity.notFound().build();
    }

    @SaCheckPermission("system:import-template:view")
    @GetMapping("/data-types")
    @Operation(summary = "获取支持的数据类型")
    public ResponseEntity<Set<String>> getSupportedDataTypes() {
        return ResponseEntity.ok(templateStore.getSupportedDataTypes());
    }

    @SaCheckPermission("system:import-template:create")
    @PostMapping
    @Operation(summary = "注册新模板")
    public ResponseEntity<Map<String, Object>> registerTemplate(@RequestBody ImportTemplateDefinition template) {
        try {
            templateStore.register(template);
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

    @SaCheckPermission("system:import-template:update")
    @PutMapping("/{templateId}")
    @Operation(summary = "更新模板")
    public ResponseEntity<Map<String, Object>> updateTemplate(
            @PathVariable String templateId, 
            @RequestBody ImportTemplateDefinition template) {
        try {
            template.setTemplateId(templateId);
            templateStore.updateTemplate(template);
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

    @SaCheckPermission("system:import-template:delete")
    @DeleteMapping("/{templateId}")
    @Operation(summary = "删除模板")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable String templateId) {
        boolean removed = templateStore.removeTemplate(templateId);
        return ResponseEntity.ok(Map.of(
            "success", removed,
            "message", removed ? "模板删除成功" : "模板不存在"
        ));
    }

    // ==================== 模板下载 ====================

    @SaCheckPermission("system:import-template:export")
    @GetMapping("/{templateId}/download")
    @Operation(summary = "下载导入模板")
    public void downloadTemplate(@PathVariable String templateId, HttpServletResponse response) throws IOException {
        ImportTemplateDefinition template = templateStore.getTemplate(templateId);
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

    @SaCheckPermission("system:import-template:export")
    @GetMapping("/data-type/{dataType}/download")
    @Operation(summary = "根据数据类型下载模板")
    public void downloadTemplateByDataType(@PathVariable String dataType, HttpServletResponse response) throws IOException {
        ImportTemplateDefinition template = templateStore.getTemplateByDataType(dataType);
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

    @SaCheckPermission("system:import-template:check")
    @PostMapping("/{templateId}/validate")
    @Operation(summary = "校验导入数据")
    public ResponseEntity<ImportTemplateValidator.ValidationResult> validateData(
            @PathVariable String templateId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        ImportTemplateDefinition template = templateStore.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 解析Excel文件获取数据
        List<Map<String, Object>> data = parseExcelFile(file, template);
        
        ImportTemplateValidator.ValidationResult result = templateValidator.validate(template, data);
        
        return ResponseEntity.ok(result);
    }

    @SaCheckPermission("system:import-template:check")
    @PostMapping("/{templateId}/validate-row")
    @Operation(summary = "校验单行数据")
    public ResponseEntity<ImportTemplateValidator.RowValidationResult> validateRow(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> rowData,
            @RequestParam(defaultValue = "1") int rowNum) {
        
        ImportTemplateDefinition template = templateStore.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        ImportTemplateValidator.RowValidationResult result = 
            templateValidator.validateRow(template, rowData, rowNum);
        
        return ResponseEntity.ok(result);
    }

    // ==================== 模板预览 ====================

    @SaCheckPermission("system:import-template:view")
    @GetMapping("/{templateId}/preview")
    @Operation(summary = "预览模板结构")
    public ResponseEntity<TemplatePreview> previewTemplate(@PathVariable String templateId) {
        ImportTemplateDefinition template = templateStore.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<FieldPreview> fields = new ArrayList<>();
        for (TemplateField field : template.getFields()) {
            fields.add(new FieldPreview(
                field.getFieldName(),
                field.getFieldTitle(),
                // 定义已落库（content 列的 JSON），字段类型可能缺失 → 空值原样返回，不让单行脏数据打成 500
                field.getFieldType() == null ? null : field.getFieldType().name(),
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

    @SaCheckPermission("system:import-template:view")
    @GetMapping("/{templateId}/fields/{fieldName}/validation")
    @Operation(summary = "获取字段校验规则")
    public ResponseEntity<FieldValidationRules> getFieldValidationRules(
            @PathVariable String templateId,
            @PathVariable String fieldName) {
        
        ImportTemplateDefinition template = templateStore.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        TemplateField field = template.getFields().stream()
            .filter(f -> fieldName.equals(f.getFieldName()))
            .findFirst()
            .orElse(null);

        if (field == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new FieldValidationRules(
            field.getFieldName(),
            // 同 previewTemplate：字段类型缺失时返回空值而非 NPE
            field.getFieldType() == null ? null : field.getFieldType().name(),
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

    /**
     * 使用 Apache POI 解析 Excel 文件
     */
    private List<Map<String, Object>> parseExcelFile(MultipartFile file, ImportTemplateDefinition template) throws IOException {
        if (file.isEmpty()) {
            log.warn("上传的文件为空");
            return new ArrayList<>();
        }

        List<TemplateField> fields = template.getFields();
        List<Map<String, Object>> result = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                log.warn("Excel文件中没有工作表");
                return result;
            }

            int lastRowNum = sheet.getLastRowNum();
            log.info("解析Excel文件: fileName={}, rows={}, columns={}", file.getOriginalFilename(), lastRowNum, fields.size());

            // 从第1行开始（跳过表头行）
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 检查是否为空行
                boolean emptyRow = true;
                for (int c = 0; c < fields.size(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        emptyRow = false;
                        break;
                    }
                }
                if (emptyRow) continue;

                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int c = 0; c < fields.size(); c++) {
                    TemplateField field = fields.get(c);
                    Cell cell = row.getCell(c);
                    Object value = getCellValue(cell, field);
                    rowData.put(field.getFieldName(), value);
                }
                result.add(rowData);
            }

            log.info("Excel解析完成: 共解析 {} 条数据", result.size());
        } catch (Exception e) {
            log.error("解析Excel文件失败: fileName={}", file.getOriginalFilename(), e);
            throw new IOException("解析Excel文件失败: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 读取单元格值并根据字段类型转换
     */
    private Object getCellValue(Cell cell, TemplateField field) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> {
                String val = cell.getStringCellValue().trim();
                // 根据目标字段类型转换
                if (field != null && ("number".equals(field.getFieldType().name().toLowerCase())
                        || "decimal".equals(field.getFieldType().name().toLowerCase()))) {
                    try {
                        yield Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        yield val;
                    }
                }
                yield val;
            }
            case NUMERIC -> {
                if (field != null && "string".equals(field.getFieldType().name().toLowerCase())) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue();
                } else {
                    yield cell.getNumericCellValue();
                }
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> {
                try {
                    yield cell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield cell.getCellFormula();
                    }
                }
            }
            case BLANK -> null;
            default -> null;
        };
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
