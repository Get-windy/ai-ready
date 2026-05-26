package cn.aiedge.automation.controller;

import cn.aiedge.automation.dto.*;
import cn.aiedge.automation.entity.AutomationRule;
import cn.aiedge.automation.service.AutomationRuleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "自动化规则管理", description = "Odoo核心特性：自动触发业务动作")
@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
public class AutomationRuleController {

    private final AutomationRuleService ruleService;

    @Operation(summary = "创建自动化规则")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody AutomationRuleCreateRequest request) {
        AutomationRule rule = ruleService.createRule(request);
        return ResponseEntity.ok(success(rule));
    }

    @Operation(summary = "更新自动化规则")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long id, @RequestBody AutomationRuleCreateRequest request) {
        AutomationRule rule = ruleService.updateRule(id, request);
        return ResponseEntity.ok(success(rule));
    }

    @Operation(summary = "获取规则详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRule(@PathVariable Long id) {
        AutomationRule rule = ruleService.getRuleById(id);
        return ResponseEntity.ok(success(rule));
    }

    @Operation(summary = "获取模型的所有规则")
    @GetMapping("/model/{modelName}")
    public ResponseEntity<Map<String, Object>> getRulesByModel(@PathVariable String modelName) {
        List<AutomationRule> rules = ruleService.getRulesByModel(modelName);
        return ResponseEntity.ok(success(rules));
    }

    @Operation(summary = "获取模型特定触发类型的规则")
    @GetMapping("/model/{modelName}/trigger/{triggerType}")
    public ResponseEntity<Map<String, Object>> getRulesByModelAndTrigger(
            @PathVariable String modelName,
            @PathVariable String triggerType) {
        List<AutomationRule> rules = ruleService.getRulesByModelAndTrigger(modelName, triggerType);
        return ResponseEntity.ok(success(rules));
    }

    @Operation(summary = "规则列表查询")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listRules(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String triggerType) {
        Page<AutomationRule> pageResult = ruleService.listRules(page, size, modelName, triggerType);
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

    @Operation(summary = "手动触发创建规则")
    @PostMapping("/trigger/create")
    public ResponseEntity<Map<String, Object>> triggerCreate(
            @RequestParam String modelName,
            @RequestParam Long recordId,
            @RequestBody Map<String, Object> values) {
        ruleService.executeOnCreate(modelName, recordId, values);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "手动触发更新规则")
    @PostMapping("/trigger/write")
    public ResponseEntity<Map<String, Object>> triggerWrite(
            @RequestParam String modelName,
            @RequestParam Long recordId,
            @RequestBody Map<String, Object> oldValues,
            @RequestBody Map<String, Object> newValues) {
        ruleService.executeOnWrite(modelName, recordId, oldValues, newValues);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "手动触发删除规则")
    @PostMapping("/trigger/delete")
    public ResponseEntity<Map<String, Object>> triggerDelete(
            @RequestParam String modelName,
            @RequestParam Long recordId) {
        ruleService.executeOnDelete(modelName, recordId);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "执行定时规则")
    @PostMapping("/trigger/scheduled")
    public ResponseEntity<Map<String, Object>> triggerScheduled() {
        ruleService.executeScheduledRules();
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