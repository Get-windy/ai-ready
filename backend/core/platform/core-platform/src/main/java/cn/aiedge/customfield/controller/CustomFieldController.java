package cn.aiedge.customfield.controller;

import cn.aiedge.customfield.dto.*;
import cn.aiedge.customfield.entity.CustomField;
import cn.aiedge.customfield.entity.CustomFieldGroup;
import cn.aiedge.customfield.service.CustomFieldService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "动态字段管理", description = "Odoo核心特性：运行时动态添加自定义字段")
@RestController
@RequestMapping("/api/custom-field")
@RequiredArgsConstructor
public class CustomFieldController {

    private final CustomFieldService fieldService;

    @Operation(summary = "创建自定义字段")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createField(@RequestBody CustomFieldCreateRequest request) {
        CustomField field = fieldService.createField(request);
        return ResponseEntity.ok(success(field));
    }

    @Operation(summary = "更新自定义字段")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateField(@PathVariable Long id, @RequestBody CustomFieldCreateRequest request) {
        CustomField field = fieldService.updateField(id, request);
        return ResponseEntity.ok(success(field));
    }

    @Operation(summary = "获取字段详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getField(@PathVariable Long id) {
        CustomField field = fieldService.getFieldById(id);
        return ResponseEntity.ok(success(field));
    }

    @Operation(summary = "获取模型的所有字段")
    @GetMapping("/model/{modelName}")
    public ResponseEntity<Map<String, Object>> getFieldsByModel(@PathVariable String modelName) {
        List<CustomField> fields = fieldService.getFieldsByModel(modelName);
        return ResponseEntity.ok(success(fields));
    }

    @Operation(summary = "获取模型分组的字段")
    @GetMapping("/model/{modelName}/group/{groupCode}")
    public ResponseEntity<Map<String, Object>> getFieldsByModelAndGroup(
            @PathVariable String modelName,
            @PathVariable String groupCode) {
        List<CustomField> fields = fieldService.getFieldsByModelAndGroup(modelName, groupCode);
        return ResponseEntity.ok(success(fields));
    }

    @Operation(summary = "字段列表查询")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFields(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String fieldType) {
        Page<CustomField> pageResult = fieldService.listFields(page, size, modelName, fieldType);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "删除字段")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteField(@PathVariable Long id) {
        fieldService.deleteField(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "激活字段")
    @PostMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateField(@PathVariable Long id) {
        fieldService.activateField(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "停用字段")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateField(@PathVariable Long id) {
        fieldService.deactivateField(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "获取可搜索字段")
    @GetMapping("/model/{modelName}/searchable")
    public ResponseEntity<Map<String, Object>> getSearchableFields(@PathVariable String modelName) {
        List<CustomField> fields = fieldService.getSearchableFields(modelName);
        return ResponseEntity.ok(success(fields));
    }

    @Operation(summary = "获取字段分组")
    @GetMapping("/group/model/{modelName}")
    public ResponseEntity<Map<String, Object>> getGroupsByModel(@PathVariable String modelName) {
        List<CustomFieldGroup> groups = fieldService.getGroupsByModel(modelName);
        return ResponseEntity.ok(success(groups));
    }

    @Operation(summary = "创建字段分组")
    @PostMapping("/group")
    public ResponseEntity<Map<String, Object>> createGroup(
            @RequestParam String modelName,
            @RequestParam String groupName,
            @RequestParam(required = false) String description) {
        CustomFieldGroup group = fieldService.createGroup(modelName, groupName, description);
        return ResponseEntity.ok(success(group));
    }

    @Operation(summary = "删除字段分组")
    @DeleteMapping("/group/{id}")
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable Long id) {
        fieldService.deleteGroup(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "验证字段名称")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateFieldName(
            @RequestParam String modelName,
            @RequestParam String fieldName) {
        boolean valid = fieldService.validateFieldName(modelName, fieldName);
        return ResponseEntity.ok(success(Map.of("valid", valid)));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}