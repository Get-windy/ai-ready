package cn.aiedge.permission.controller;

import cn.aiedge.permission.dto.*;
import cn.aiedge.permission.entity.RecordRule;
import cn.aiedge.permission.service.RecordRuleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "记录级权限管理", description = "Odoo核心特性：自动过滤用户可见数据")
@RestController
@RequestMapping("/api/permission/record")
@RequiredArgsConstructor
public class RecordRuleController {

    private final RecordRuleService ruleService;

    @Operation(summary = "创建记录规则")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody RecordRuleCreateRequest request) {
        RecordRule rule = ruleService.createRule(request);
        return ResponseEntity.ok(success(rule));
    }

    @Operation(summary = "更新记录规则")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long id, @RequestBody RecordRuleCreateRequest request) {
        RecordRule rule = ruleService.updateRule(id, request);
        return ResponseEntity.ok(success(rule));
    }

    @Operation(summary = "获取规则详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRule(@PathVariable Long id) {
        RecordRule rule = ruleService.getRuleById(id);
        return ResponseEntity.ok(success(rule));
    }

    @Operation(summary = "获取模型的所有规则")
    @GetMapping("/model/{modelName}")
    public ResponseEntity<Map<String, Object>> getRulesByModel(@PathVariable String modelName) {
        List<RecordRule> rules = ruleService.getRulesByModel(modelName);
        return ResponseEntity.ok(success(rules));
    }

    @Operation(summary = "获取用户的所有规则")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getRulesByUser(@PathVariable Long userId) {
        List<RecordRule> rules = ruleService.getRulesByUser(userId);
        return ResponseEntity.ok(success(rules));
    }

    @Operation(summary = "获取角色的所有规则")
    @GetMapping("/group/{groupId}")
    public ResponseEntity<Map<String, Object>> getRulesByGroup(@PathVariable Long groupId) {
        List<RecordRule> rules = ruleService.getRulesByGroup(groupId);
        return ResponseEntity.ok(success(rules));
    }

    @Operation(summary = "规则列表查询")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listRules(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long groupId) {
        Page<RecordRule> pageResult = ruleService.listRules(page, size, modelName, userId, groupId);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "激活规则")
    @PostMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateRule(@PathVariable Long id) {
        ruleService.activateRule(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "停用规则")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateRule(@PathVariable Long id) {
        ruleService.deactivateRule(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "构建Domain过滤条件")
    @PostMapping("/build-domain")
    public ResponseEntity<Map<String, Object>> buildDomain(
            @RequestParam String modelName,
            @RequestBody PermissionContext context) {
        String domain = ruleService.buildDomainFilter(modelName, context);
        return ResponseEntity.ok(success(Map.of("domain", domain)));
    }

    @Operation(summary = "检查记录访问权限")
    @PostMapping("/check-access")
    public ResponseEntity<Map<String, Object>> checkAccess(
            @RequestParam String modelName,
            @RequestParam Long recordId,
            @RequestParam String operation,
            @RequestBody PermissionContext context) {
        boolean hasAccess = ruleService.checkRecordAccess(modelName, recordId, operation, context);
        return ResponseEntity.ok(success(Map.of("hasAccess", hasAccess)));
    }

    @Operation(summary = "过滤记录列表")
    @PostMapping("/filter-records")
    public ResponseEntity<Map<String, Object>> filterRecords(
            @RequestParam String modelName,
            @RequestParam String operation,
            @RequestBody List<Long> recordIds,
            @RequestBody PermissionContext context) {
        List<Long> filteredIds = ruleService.filterRecords(modelName, recordIds, operation, context);
        return ResponseEntity.ok(success(filteredIds));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}