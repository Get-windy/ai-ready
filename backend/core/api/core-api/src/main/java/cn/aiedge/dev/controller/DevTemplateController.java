package cn.aiedge.dev.controller;

import cn.aiedge.dev.model.DevTemplate;
import cn.aiedge.dev.service.DevTemplateService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 开发模板控制器
 */
@RestController
@RequestMapping("/api/codegen/template")
@RequiredArgsConstructor
@Tag(name = "模板管理", description = "代码模板管理")
public class DevTemplateController {

    private final DevTemplateService devTemplateService;

    @GetMapping("/list")
    @SaCheckPermission("dev:template:list")
    @Operation(summary = "获取模板列表")
    public ResponseEntity<Map<String, Object>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<DevTemplate> all = devTemplateService.getList();
        int total = all.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<DevTemplate> records = from >= total ? List.of() : all.subList(from, to);
        return ResponseEntity.ok(Map.of("records", records, "total", (long) total));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("dev:template:list")
    @Operation(summary = "获取模板详情")
    public ResponseEntity<Map<String, Object>> getDetail(@PathVariable Long id) {
        DevTemplate t = devTemplateService.getById(id);
        if (t == null) {
            return ResponseEntity.ok(Map.of("code", 200, "data", null, "message", "模板不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", t, "message", "ok"));
    }

    @PostMapping("/create")
    @SaCheckPermission("dev:template:create")
    @Operation(summary = "创建模板")
    public ResponseEntity<Map<String, Object>> create(@RequestBody DevTemplate template) {
        DevTemplate created = devTemplateService.create(template);
        return ResponseEntity.ok(Map.of("code", 200, "data", created, "message", "模板创建成功"));
    }

    @PutMapping("/update")
    @SaCheckPermission("dev:template:update")
    @Operation(summary = "更新模板")
    public ResponseEntity<Map<String, Object>> update(@RequestBody DevTemplate template) {
        DevTemplate updated = devTemplateService.update(template);
        if (updated == null) {
            return ResponseEntity.ok(Map.of("code", 200, "data", null, "message", "模板不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", updated, "message", "模板更新成功"));
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckPermission("dev:template:delete")
    @Operation(summary = "删除模板")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = devTemplateService.delete(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", success, "message", success ? "模板删除成功" : "模板删除失败"));
    }

    @PostMapping("/activate/{id}")
    @SaCheckPermission("dev:template:update")
    @Operation(summary = "激活模板")
    public ResponseEntity<Map<String, Object>> activate(@PathVariable Long id) {
        boolean success = devTemplateService.activate(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", success, "message", success ? "模板激活成功" : "模板激活失败"));
    }

    @PostMapping("/deactivate/{id}")
    @SaCheckPermission("dev:template:update")
    @Operation(summary = "停用模板")
    public ResponseEntity<Map<String, Object>> deactivate(@PathVariable Long id) {
        boolean success = devTemplateService.deactivate(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", success, "message", success ? "模板停用成功" : "模板停用失败"));
    }
}
