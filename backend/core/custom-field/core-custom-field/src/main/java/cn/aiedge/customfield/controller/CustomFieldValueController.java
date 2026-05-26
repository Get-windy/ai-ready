package cn.aiedge.customfield.controller;

import cn.aiedge.customfield.dto.*;
import cn.aiedge.customfield.service.CustomFieldValueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "动态字段值管理", description = "业务记录的动态字段值存储和查询")
@RestController
@RequestMapping("/api/custom-field-value")
@RequiredArgsConstructor
public class CustomFieldValueController {

    private final CustomFieldValueService valueService;

    @Operation(summary = "保存单个字段值")
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveFieldValue(
            @RequestParam String modelName,
            @RequestParam Long recordId,
            @RequestParam Long fieldId,
            @RequestBody Object value) {
        valueService.saveFieldValue(modelName, recordId, fieldId, value);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "批量保存字段值（按字段ID）")
    @PostMapping("/save-batch")
    public ResponseEntity<Map<String, Object>> saveFieldValues(
            @RequestParam String modelName,
            @RequestParam Long recordId,
            @RequestBody Map<Long, Object> fieldValues) {
        valueService.saveFieldValues(modelName, recordId, fieldValues);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "批量保存字段值（按字段名）")
    @PostMapping("/save-by-name")
    public ResponseEntity<Map<String, Object>> saveFieldValuesByName(
            @RequestParam String modelName,
            @RequestParam Long recordId,
            @RequestBody Map<String, Object> fieldValues) {
        valueService.saveFieldValuesByName(modelName, recordId, fieldValues);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "获取单个字段值")
    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> getFieldValue(
            @RequestParam String modelName,
            @RequestParam Long recordId,
            @RequestParam Long fieldId) {
        Object value = valueService.getFieldValue(modelName, recordId, fieldId);
        return ResponseEntity.ok(success(value));
    }

    @Operation(summary = "按字段名获取字段值")
    @GetMapping("/get-by-name")
    public ResponseEntity<Map<String, Object>> getFieldValueByName(
            @RequestParam String modelName,
            @RequestParam Long recordId,
            @RequestParam String fieldName) {
        Object value = valueService.getFieldValueByName(modelName, recordId, fieldName);
        return ResponseEntity.ok(success(value));
    }

    @Operation(summary = "获取记录的所有字段值")
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllFieldValues(
            @RequestParam String modelName,
            @RequestParam Long recordId) {
        Map<String, Object> values = valueService.getAllFieldValues(modelName, recordId);
        return ResponseEntity.ok(success(values));
    }

    @Operation(summary = "删除记录的所有字段值")
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFieldValues(
            @RequestParam String modelName,
            @RequestParam Long recordId) {
        valueService.deleteFieldValues(modelName, recordId);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "按字段搜索记录")
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchRecords(
            @RequestParam String modelName,
            @RequestParam String fieldName,
            @RequestParam Object value) {
        List<Long> recordIds = valueService.searchRecords(modelName, fieldName, value);
        return ResponseEntity.ok(success(recordIds));
    }

    @Operation(summary = "复制字段值")
    @PostMapping("/copy")
    public ResponseEntity<Map<String, Object>> copyFieldValues(
            @RequestParam String modelName,
            @RequestParam Long sourceRecordId,
            @RequestParam Long targetRecordId) {
        valueService.copyFieldValues(modelName, sourceRecordId, targetRecordId);
        return ResponseEntity.ok(success(null));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}