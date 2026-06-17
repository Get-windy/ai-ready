package cn.aiedge.datasource.controller;

import cn.aiedge.datasource.model.CleanupRule;
import cn.aiedge.datasource.service.CleanupRuleService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据清理规则控制器
 */
@RestController
@RequestMapping("/api/data-source/cleanup")
@RequiredArgsConstructor
@Tag(name = "数据清理管理", description = "数据清理规则管理")
public class CleanupRuleController {

    private final CleanupRuleService cleanupRuleService;

    @GetMapping("/list")
    @SaCheckPermission("datasource:cleanup:list")
    @Operation(summary = "获取清理规则列表")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<CleanupRule> allRules = cleanupRuleService.list(tenantId);
        int total = allRules.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<CleanupRule> records = fromIndex >= total ? List.of() : allRules.subList(fromIndex, toIndex);

        return ResponseEntity.ok(Map.of(
                "records", records,
                "total", total,
                "current", page,
                "size", pageSize,
                "pages", (total + pageSize - 1) / pageSize
        ));
    }

    @PostMapping("/")
    @SaCheckPermission("datasource:cleanup:create")
    @Operation(summary = "创建清理规则")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody CleanupRule rule,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-User-Id", required = false) String createBy) {

        CleanupRule saved = cleanupRuleService.create(rule, tenantId, createBy != null ? createBy : "unknown");
        return ResponseEntity.ok(Map.of("success", true, "data", saved, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("datasource:cleanup:update")
    @Operation(summary = "更新清理规则")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody CleanupRule rule,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-User-Id", required = false) String updateBy) {

        CleanupRule updated = cleanupRuleService.update(id, rule, tenantId, updateBy != null ? updateBy : "unknown");
        if (updated == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "清理规则不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("datasource:cleanup:delete")
    @Operation(summary = "删除清理规则")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = cleanupRuleService.delete(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "清理规则不存在"));
    }

    @PostMapping("/{id}/execute")
    @SaCheckPermission("datasource:cleanup:execute")
    @Operation(summary = "立即执行清理")
    public ResponseEntity<Map<String, Object>> execute(@PathVariable Long id) {
        boolean success = cleanupRuleService.execute(id);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "清理任务已触发执行" : "清理规则不存在"
        ));
    }
}
