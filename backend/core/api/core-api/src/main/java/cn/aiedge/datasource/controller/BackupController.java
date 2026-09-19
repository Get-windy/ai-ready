package cn.aiedge.datasource.controller;

import cn.aiedge.base.log.annotation.OperationLog;
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
 *
 * <p>契约：所有写动作返回 {@code {success, message, data?}}。
 * <ul>
 *   <li>{@code success=false} 表示**本次没有产生任何副作用**（参数非法 / 前置校验未过 / 执行失败），
 *       message 是可直接展示的原因 —— 前端 {@code mutate} 会在 {@code success!==true} 时抛出该 message，
 *       因此失败一定会以错误提示出现在页面上，不存在「接口回了 200 就是成功」的误读。</li>
 *   <li>{@code create} 的 message 措辞刻意是「已受理，正在后台执行」而非「备份已完成」：
 *       整库 dump 是异步的，接口返回时进程刚启动。</li>
 * </ul>
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
    @OperationLog(module = "数据管理-备份管理", type = "CREATE", desc = "创建数据库备份（pg_dump）")
    @Operation(summary = "创建备份")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-User-Id", required = false) String createBy) {

        Object dataSourceIdObj = request.get("dataSourceId");
        Object backupNameObj = request.get("backupName");
        Object backupTypeObj = request.get("backupType");

        // dataSourceId 允许为空：为空时表示操作本系统数据库（spring.datasource）。
        // 但传了非数字必须明确报错 —— 不能静默当成 null，那会变成「用户以为备份 A 库，实际备份了 B 库」。
        Long dataSourceId = null;
        if (dataSourceIdObj != null && !String.valueOf(dataSourceIdObj).isBlank()) {
            try {
                dataSourceId = Long.valueOf(String.valueOf(dataSourceIdObj).trim());
            } catch (NumberFormatException e) {
                return ResponseEntity.ok(Map.of("success", false, "message", "dataSourceId 必须是数字"));
            }
        }

        String backupName = backupNameObj != null ? backupNameObj.toString() : null;
        String backupType = backupTypeObj != null ? backupTypeObj.toString() : "full";
        String creator = createBy != null ? createBy : "unknown";

        return ResponseEntity.ok(backupService.create(dataSourceId, backupName, backupType, tenantId, creator));
    }

    @PostMapping("/{id}/restore")
    @SaCheckPermission("datasource:backup:restore")
    @OperationLog(module = "数据管理-备份管理", type = "OTHER",
            desc = "从备份恢复数据库（pg_restore，高危）", saveResponse = true)
    @Operation(summary = "从备份恢复")
    public ResponseEntity<Map<String, Object>> restore(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-User-Id", required = false) String operator) {

        // 二次确认闸门：请求体缺 confirm=true 一律拒绝（缺省拒绝，不依赖前端按钮是否置灰）
        boolean confirm = isTrue(request == null ? null : request.get("confirm"));
        return ResponseEntity.ok(backupService.restore(id, confirm, tenantId, operator != null ? operator : "unknown"));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("datasource:backup:delete")
    @OperationLog(module = "数据管理-备份管理", type = "DELETE", desc = "删除备份记录及其磁盘文件")
    @Operation(summary = "删除备份记录")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return ResponseEntity.ok(backupService.delete(id, tenantId));
    }

    /** 宽松布尔解析：接受 true / "true" / 1 / "1"；其余（含 null）一律视为未确认 */
    private boolean isTrue(Object value) {
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }
}
