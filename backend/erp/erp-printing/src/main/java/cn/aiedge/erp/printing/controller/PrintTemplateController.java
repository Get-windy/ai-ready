package cn.aiedge.erp.printing.controller;

import cn.aiedge.erp.printing.entity.PrintTemplate;
import cn.aiedge.erp.printing.service.PrintTemplateService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "打印模板管理", description = "打印模板的增删改查、预览、复制等操作")
@RestController
@RequestMapping("/api/v1/print/templates")
@RequiredArgsConstructor
public class PrintTemplateController {

    private final PrintTemplateService templateService;

    @Operation(summary = "创建打印模板")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody PrintTemplate template) {
        PrintTemplate created = templateService.createTemplate(template);
        return ResponseEntity.ok(success(created));
    }

    @Operation(summary = "更新打印模板")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTemplate(@PathVariable Long id, @RequestBody PrintTemplate template) {
        PrintTemplate updated = templateService.updateTemplate(id, template);
        return ResponseEntity.ok(success(updated));
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTemplate(@PathVariable Long id) {
        PrintTemplate template = templateService.getTemplateById(id);
        return ResponseEntity.ok(success(template));
    }

    @Operation(summary = "模板列表查询")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listTemplates(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) String status) {
        Page<PrintTemplate> pageResult = templateService.listTemplates(page, size, templateType, status);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "删除打印模板")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "复制打印模板")
    @PostMapping("/{id}/copy")
    public ResponseEntity<Map<String, Object>> copyTemplate(
            @PathVariable Long id,
            @RequestParam String newName) {
        PrintTemplate copied = templateService.copyTemplate(id, newName);
        return ResponseEntity.ok(success(copied));
    }

    @Operation(summary = "预览打印模板")
    @PostMapping("/{id}/preview")
    public ResponseEntity<Map<String, Object>> previewTemplate(
            @PathVariable Long id,
            @RequestBody String printData) {
        String preview = templateService.previewTemplate(id, printData);
        return ResponseEntity.ok(success(Map.of("preview", preview)));
    }

    @Operation(summary = "按类型获取模板列表")
    @GetMapping("/type/{templateType}")
    public ResponseEntity<Map<String, Object>> listByType(@PathVariable String templateType) {
        List<PrintTemplate> templates = templateService.listByType(templateType);
        return ResponseEntity.ok(success(templates));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", "200");
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}