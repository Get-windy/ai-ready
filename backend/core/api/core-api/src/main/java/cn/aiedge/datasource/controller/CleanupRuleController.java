package cn.aiedge.datasource.controller;

import cn.aiedge.base.log.annotation.OperationLog;
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
 *
 * <p>契约：写动作返回 {@code {success, message, ...计数}}。
 * {@code /{id}/execute} 支持两个可选开关：
 * <ul>
 *   <li>{@code confirm}（必须为 true 才真正执行；缺省拒绝）—— 破坏性动作的二次确认闸门；</li>
 *   <li>{@code dryRun}（true 时只预统计「将删除 N 行」，不删任何数据）—— 文档 §5.6 ⑧ 的干跑能力。</li>
 * </ul>
 * 执行结果直接返回真实计数（plannedRows / deletedRows / remainingRows / truncated），
 * 页面无需猜测即可展示「实际删了多少」。
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

    @GetMapping("/allowed-tables")
    @SaCheckPermission("datasource:cleanup:list")
    @Operation(summary = "获取清理表白名单")
    public ResponseEntity<List<Map<String, Object>>> allowedTables() {
        return ResponseEntity.ok(cleanupRuleService.allowedTables());
    }

    @PostMapping("/")
    @SaCheckPermission("datasource:cleanup:create")
    @OperationLog(module = "数据管理-清理规则", type = "CREATE", desc = "新增数据清理规则")
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
    @OperationLog(module = "数据管理-清理规则", type = "UPDATE", desc = "修改数据清理规则")
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
    @OperationLog(module = "数据管理-清理规则", type = "DELETE", desc = "删除数据清理规则")
    @Operation(summary = "删除清理规则")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = cleanupRuleService.delete(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "清理规则不存在"));
    }

    @PostMapping("/{id}/execute")
    @SaCheckPermission("datasource:cleanup:execute")
    @OperationLog(module = "数据管理-清理规则", type = "OTHER",
            desc = "执行数据清理（按保留天数删除历史数据）", saveResponse = true)
    @Operation(summary = "立即执行清理")
    public ResponseEntity<Map<String, Object>> execute(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-User-Id", required = false) String operator) {

        boolean confirm = isTrue(request == null ? null : request.get("confirm"));
        boolean dryRun = isTrue(request == null ? null : request.get("dryRun"));
        return ResponseEntity.ok(cleanupRuleService.execute(id, confirm, dryRun, tenantId,
                operator != null ? operator : "unknown"));
    }

    /** 宽松布尔解析：接受 true / "true" / 1 / "1"；其余（含 null）一律视为未开启 */
    private boolean isTrue(Object value) {
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }
}
