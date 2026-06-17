package cn.aiedge.datasource.controller;

import cn.aiedge.datasource.model.DataSource;
import cn.aiedge.datasource.service.DataSourceService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源控制器
 */
@RestController
@RequestMapping("/api/data-source")
@RequiredArgsConstructor
@Tag(name = "数据源管理", description = "数据源连接管理功能")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @GetMapping("/list")
    @SaCheckPermission("datasource:list")
    @Operation(summary = "分页查询数据源列表")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<DataSource> allDataSources = dataSourceService.list(keyword, tenantId);
        int total = allDataSources.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<DataSource> records = fromIndex >= total ? List.of() : allDataSources.subList(fromIndex, toIndex);

        return ResponseEntity.ok(Map.of(
                "records", records,
                "total", total,
                "current", page,
                "size", pageSize,
                "pages", (total + pageSize - 1) / pageSize
        ));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("datasource:list")
    @Operation(summary = "获取单个数据源")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        DataSource dataSource = dataSourceService.getById(id);
        if (dataSource == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "数据源不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", dataSource));
    }

    @PostMapping("/")
    @SaCheckPermission("datasource:create")
    @Operation(summary = "创建数据源")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody DataSource dataSource,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        DataSource saved = dataSourceService.create(dataSource, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "data", saved, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("datasource:update")
    @Operation(summary = "更新数据源")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody DataSource dataSource,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        DataSource updated = dataSourceService.update(id, dataSource, tenantId);
        if (updated == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "数据源不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("datasource:delete")
    @Operation(summary = "删除数据源")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = dataSourceService.delete(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "数据源不存在"));
    }

    @PostMapping("/test")
    @SaCheckPermission("datasource:test")
    @Operation(summary = "测试数据源连接")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestBody Map<String, Object> request) {

        Object idObj = request.get("id");
        if (idObj == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "请提供数据源ID"));
        }
        Long id = Long.valueOf(idObj.toString());
        boolean success = dataSourceService.testConnection(id);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "连接成功" : "连接失败，请检查数据源配置"
        ));
    }
}
