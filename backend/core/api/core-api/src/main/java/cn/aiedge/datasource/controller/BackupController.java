package cn.aiedge.datasource.controller;

import cn.aiedge.datasource.model.BackupRecord;
import cn.aiedge.datasource.service.BackupService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据备份控制器
 */
@RestController
@RequestMapping("/api/data-source/backup")
@RequiredArgsConstructor
@Tag(name = "数据备份管理", description = "数据库备份与恢复管理")
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/list")
    @SaCheckPermission("datasource:backup:list")
    @Operation(summary = "获取备份记录列表")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long dataSourceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<BackupRecord> allRecords = backupService.list(dataSourceId, tenantId);
        int total = allRecords.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<BackupRecord> records = fromIndex >= total ? List.of() : allRecords.subList(fromIndex, toIndex);

        return ResponseEntity.ok(Map.of(
                "records", records,
                "total", total,
                "current", page,
                "size", pageSize,
                "pages", (total + pageSize - 1) / pageSize
        ));
    }

    @PostMapping("/create")
    @SaCheckPermission("datasource:backup:create")
    @Operation(summary = "创建备份")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-User-Id", required = false) String createBy) {

        Object dataSourceIdObj = request.get("dataSourceId");
        Object backupNameObj = request.get("backupName");
        Object backupTypeObj = request.get("backupType");

        if (dataSourceIdObj == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "请提供数据源ID"));
        }

        Long dataSourceId = Long.valueOf(dataSourceIdObj.toString());
        String backupName = backupNameObj != null ? backupNameObj.toString() : "手动备份_" + System.currentTimeMillis();
        String backupType = backupTypeObj != null ? backupTypeObj.toString() : "full";
        String creator = createBy != null ? createBy : "unknown";

        BackupRecord record = backupService.create(dataSourceId, backupName, backupType, tenantId, creator);
        return ResponseEntity.ok(Map.of("success", true, "data", record, "message", "备份任务已创建"));
    }

    @PostMapping("/{id}/restore")
    @SaCheckPermission("datasource:backup:restore")
    @Operation(summary = "从备份恢复")
    public ResponseEntity<Map<String, Object>> restore(@PathVariable Long id) {
        boolean success = backupService.restore(id);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "恢复操作已启动" : "备份记录不存在或状态无效"
        ));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("datasource:backup:delete")
    @Operation(summary = "删除备份记录")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = backupService.delete(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "备份记录不存在"));
    }
}
