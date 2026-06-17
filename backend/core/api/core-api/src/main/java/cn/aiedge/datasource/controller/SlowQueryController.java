package cn.aiedge.datasource.controller;

import cn.aiedge.datasource.model.SlowQuery;
import cn.aiedge.datasource.service.SlowQueryService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 慢查询控制器
 */
@RestController
@RequestMapping("/api/data-source/slow-query")
@RequiredArgsConstructor
@Tag(name = "慢查询管理", description = "数据库慢查询监控管理")
public class SlowQueryController {

    private final SlowQueryService slowQueryService;

    @GetMapping("/list")
    @SaCheckPermission("datasource:slowquery:list")
    @Operation(summary = "获取慢查询列表")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long dataSourceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<SlowQuery> allQueries = slowQueryService.list(dataSourceId, tenantId);
        int total = allQueries.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<SlowQuery> records = fromIndex >= total ? List.of() : allQueries.subList(fromIndex, toIndex);

        return ResponseEntity.ok(Map.of(
                "records", records,
                "total", total,
                "current", page,
                "size", pageSize,
                "pages", (total + pageSize - 1) / pageSize
        ));
    }

    @GetMapping("/export")
    @SaCheckPermission("datasource:slowquery:export")
    @Operation(summary = "导出慢查询")
    public ResponseEntity<Map<String, Object>> export(
            @RequestParam(required = false) Long dataSourceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<SlowQuery> allQueries = slowQueryService.export(dataSourceId, tenantId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "records", allQueries,
                "total", allQueries.size()
        ));
    }
}
