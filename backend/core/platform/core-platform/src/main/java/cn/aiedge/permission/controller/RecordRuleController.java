package cn.aiedge.permission.controller;

import cn.aiedge.permission.dto.*;
import cn.aiedge.permission.entity.RecordRule;
import cn.aiedge.permission.service.RecordRuleService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记录级权限管理（对标 Odoo ir.rule）。
 *
 * <p>⚠️ 本组接口原先**没有任何权限校验**（任何登录用户都能创建/修改/删除规则），
 * 而 {@code global = true} 的规则是影响所有角色的全局安全策略，故 2026-09-20 补齐
 * {@code system:record-rule:*} 权限码。新增权限码需同步登记到
 * {@code PermissionInitializationConfig}，否则无人能被授权。</p>
 *
 * <p>权限码分配：读接口与三个计算接口用 {@code list}（计算接口同样只读，但不该对任意登录用户开放）；
 * 增删改用各自的动作码；启用/停用归入 {@code update}。</p>
 */
@Tag(name = "记录级权限管理", description = "Odoo核心特性：自动过滤用户可见数据")
@RestController
@RequestMapping("/api/permission/record")
@RequiredArgsConstructor
public class RecordRuleController {

    private final RecordRuleService ruleService;

    @Operation(summary = "创建记录规则")
    @SaCheckPermission("system:record-rule:create")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody RecordRuleCreateRequest request) {
        RecordRule rule = ruleService.createRule(request);
        return ResponseEntity.ok(success(rule));
    }

    @Operation(summary = "更新记录规则")
    @SaCheckPermission("system:record-rule:update")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long id, @RequestBody RecordRuleCreateRequest request) {
        RecordRule rule = ruleService.updateRule(id, request);
        return ResponseEntity.ok(success(rule));
    }

    @Operation(summary = "获取规则详情")
    @SaCheckPermission("system:record-rule:list")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRule(@PathVariable Long id) {
        RecordRule rule = ruleService.getRuleById(id);
        return ResponseEntity.ok(success(rule));
    }

    @Operation(summary = "获取模型的所有规则")
    @SaCheckPermission("system:record-rule:list")
    @GetMapping("/model/{modelName}")
    public ResponseEntity<Map<String, Object>> getRulesByModel(@PathVariable String modelName) {
        List<RecordRule> rules = ruleService.getRulesByModel(modelName);
        return ResponseEntity.ok(success(rules));
    }

    @Operation(summary = "获取用户的所有规则")
    @SaCheckPermission("system:record-rule:list")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getRulesByUser(@PathVariable Long userId) {
        List<RecordRule> rules = ruleService.getRulesByUser(userId);
        return ResponseEntity.ok(success(rules));
    }

    @Operation(summary = "获取角色的所有规则")
    @SaCheckPermission("system:record-rule:list")
    @GetMapping("/group/{groupId}")
    public ResponseEntity<Map<String, Object>> getRulesByGroup(@PathVariable Long groupId) {
        List<RecordRule> rules = ruleService.getRulesByGroup(groupId);
        return ResponseEntity.ok(success(rules));
    }

    @Operation(summary = "规则列表查询")
    @SaCheckPermission("system:record-rule:list")
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
    @SaCheckPermission("system:record-rule:delete")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "激活规则")
    @SaCheckPermission("system:record-rule:update")
    @PostMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateRule(@PathVariable Long id) {
        ruleService.activateRule(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "停用规则")
    @SaCheckPermission("system:record-rule:update")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateRule(@PathVariable Long id) {
        ruleService.deactivateRule(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "构建Domain过滤条件")
    @SaCheckPermission("system:record-rule:list")
    @PostMapping("/build-domain")
    public ResponseEntity<Map<String, Object>> buildDomain(
            @RequestParam String modelName,
            @RequestBody PermissionContext context) {
        String domain = ruleService.buildDomainFilter(modelName, context);
        return ResponseEntity.ok(success(Map.of("domain", domain)));
    }

    @Operation(summary = "检查记录访问权限")
    @SaCheckPermission("system:record-rule:list")
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
    @SaCheckPermission("system:record-rule:list")
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
