package cn.aiedge.datasource.controller;

import cn.aiedge.base.log.annotation.OperationLog;
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
 *
 * <p>契约：{@code /{id}/execute} 返回 {@code {success, message, dispatched, configId?, engineMessage?}}。
 * <ul>
 *   <li>{@code success=true} + {@code dispatched=true} 表示「请求已投递给同步引擎」，
 *       message 里明确写了「本系统无法确认数据搬运结果」—— 不把「已投递」翻译成「已同步」。</li>
 *   <li>{@code success=false} 表示**根本没有投递出去**（无引擎配置 / 引擎不可达 / 任务正在执行中），
 *       message 是可直接展示的原因。前端 {@code mutate} 会以错误提示弹出该 message。</li>
 * </ul>
 * 注意：本接口**不修改** {@code status}（启停开关），执行结论写 {@code last_run_*} 三列。
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
    @OperationLog(module = "数据管理-同步任务", type = "CREATE", desc = "新增同步任务")
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
    @OperationLog(module = "数据管理-同步任务", type = "UPDATE", desc = "修改同步任务")
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
    @OperationLog(module = "数据管理-同步任务", type = "DELETE", desc = "删除同步任务")
    @Operation(summary = "删除同步任务")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = syncTaskService.delete(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "同步任务不存在"));
    }

    @PostMapping("/{id}/execute")
    @SaCheckPermission("datasource:sync:execute")
    @OperationLog(module = "数据管理-同步任务", type = "OTHER",
            desc = "立即执行同步任务（投递到同步引擎）", saveResponse = true)
    @Operation(summary = "立即执行同步任务")
    public ResponseEntity<Map<String, Object>> execute(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-User-Id", required = false) String operator) {

        return ResponseEntity.ok(syncTaskService.execute(id, tenantId, operator != null ? operator : "unknown"));
    }
}
