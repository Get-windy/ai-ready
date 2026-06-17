package cn.aiedge.datasource.controller;

import cn.aiedge.datasource.model.SyncTask;
import cn.aiedge.datasource.service.SyncTaskService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 同步任务控制器
 */
@RestController
@RequestMapping("/api/data-source/sync")
@RequiredArgsConstructor
@Tag(name = "数据同步管理", description = "数据同步任务管理")
public class SyncTaskController {

    private final SyncTaskService syncTaskService;

    @GetMapping("/list")
    @SaCheckPermission("datasource:sync:list")
    @Operation(summary = "获取同步任务列表")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<SyncTask> allTasks = syncTaskService.list(tenantId);
        int total = allTasks.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<SyncTask> records = fromIndex >= total ? List.of() : allTasks.subList(fromIndex, toIndex);

        return ResponseEntity.ok(Map.of(
                "records", records,
                "total", total,
                "current", page,
                "size", pageSize,
                "pages", (total + pageSize - 1) / pageSize
        ));
    }

    @PostMapping("/")
    @SaCheckPermission("datasource:sync:create")
    @Operation(summary = "创建同步任务")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody SyncTask syncTask,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-User-Id", required = false) String createBy) {

        SyncTask saved = syncTaskService.create(syncTask, tenantId, createBy != null ? createBy : "unknown");
        return ResponseEntity.ok(Map.of("success", true, "data", saved, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("datasource:sync:update")
    @Operation(summary = "更新同步任务")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody SyncTask syncTask,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-User-Id", required = false) String updateBy) {

        SyncTask updated = syncTaskService.update(id, syncTask, tenantId, updateBy != null ? updateBy : "unknown");
        if (updated == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "同步任务不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("datasource:sync:delete")
    @Operation(summary = "删除同步任务")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = syncTaskService.delete(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "同步任务不存在"));
    }

    @PostMapping("/{id}/execute")
    @SaCheckPermission("datasource:sync:execute")
    @Operation(summary = "立即执行同步任务")
    public ResponseEntity<Map<String, Object>> execute(@PathVariable Long id) {
        boolean success = syncTaskService.execute(id);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "同步任务已触发执行" : "同步任务不存在"
        ));
    }
}
