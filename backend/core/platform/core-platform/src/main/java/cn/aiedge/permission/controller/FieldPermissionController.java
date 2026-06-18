package cn.aiedge.permission.controller;

import cn.aiedge.permission.dto.*;
import cn.aiedge.permission.entity.FieldPermission;
import cn.aiedge.permission.service.FieldPermissionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "字段级权限管理", description = "Odoo核心特性：控制字段可见性和可编辑性")
@RestController
@RequestMapping("/api/permission/field")
@RequiredArgsConstructor
public class FieldPermissionController {

    private final FieldPermissionService permissionService;

    @Operation(summary = "创建字段权限")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPermission(@RequestBody FieldPermissionCreateRequest request) {
        FieldPermission permission = permissionService.createPermission(request);
        return ResponseEntity.ok(success(permission));
    }

    @Operation(summary = "更新字段权限")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePermission(@PathVariable Long id, @RequestBody FieldPermissionCreateRequest request) {
        FieldPermission permission = permissionService.updatePermission(id, request);
        return ResponseEntity.ok(success(permission));
    }

    @Operation(summary = "获取权限详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPermission(@PathVariable Long id) {
        FieldPermission permission = permissionService.getPermissionById(id);
        return ResponseEntity.ok(success(permission));
    }

    @Operation(summary = "获取模型的所有字段权限")
    @GetMapping("/model/{modelName}")
    public ResponseEntity<Map<String, Object>> getPermissionsByModel(@PathVariable String modelName) {
        List<FieldPermission> permissions = permissionService.getPermissionsByModel(modelName);
        return ResponseEntity.ok(success(permissions));
    }

    @Operation(summary = "获取用户在模型上的字段权限")
    @GetMapping("/model/{modelName}/user/{userId}")
    public ResponseEntity<Map<String, Object>> getPermissionsByModelAndUser(
            @PathVariable String modelName,
            @PathVariable Long userId) {
        List<FieldPermission> permissions = permissionService.getPermissionsByModelAndUser(modelName, userId);
        return ResponseEntity.ok(success(permissions));
    }

    @Operation(summary = "获取角色在模型上的字段权限")
    @GetMapping("/model/{modelName}/group/{groupId}")
    public ResponseEntity<Map<String, Object>> getPermissionsByModelAndGroup(
            @PathVariable String modelName,
            @PathVariable Long groupId) {
        List<FieldPermission> permissions = permissionService.getPermissionsByModelAndGroup(modelName, groupId);
        return ResponseEntity.ok(success(permissions));
    }

    @Operation(summary = "权限列表查询")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listPermissions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String fieldName) {
        Page<FieldPermission> pageResult = permissionService.listPermissions(page, size, modelName, fieldName);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "删除字段权限")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "获取用户在模型上的所有字段权限")
    @PostMapping("/get-all")
    public ResponseEntity<Map<String, Object>> getAllFieldPermissions(
            @RequestParam String modelName,
            @RequestBody PermissionContext context) {
        Map<String, FieldPermissionResult> permissions = permissionService.getFieldPermissions(modelName, context);
        return ResponseEntity.ok(success(permissions));
    }

    @Operation(summary = "获取单个字段权限")
    @PostMapping("/get-single")
    public ResponseEntity<Map<String, Object>> getSingleFieldPermission(
            @RequestParam String modelName,
            @RequestParam String fieldName,
            @RequestBody PermissionContext context) {
        FieldPermissionResult permission = permissionService.getSingleFieldPermission(modelName, fieldName, context);
        return ResponseEntity.ok(success(permission));
    }

    @Operation(summary = "检查字段是否可读")
    @PostMapping("/check-readable")
    public ResponseEntity<Map<String, Object>> checkReadable(
            @RequestParam String modelName,
            @RequestParam String fieldName,
            @RequestBody PermissionContext context) {
        boolean readable = permissionService.isFieldReadable(modelName, fieldName, context);
        return ResponseEntity.ok(success(Map.of("readable", readable)));
    }

    @Operation(summary = "检查字段是否可写")
    @PostMapping("/check-writable")
    public ResponseEntity<Map<String, Object>> checkWritable(
            @RequestParam String modelName,
            @RequestParam String fieldName,
            @RequestBody PermissionContext context) {
        boolean writable = permissionService.isFieldWritable(modelName, fieldName, context);
        return ResponseEntity.ok(success(Map.of("writable", writable)));
    }

    @Operation(summary = "过滤记录字段")
    @PostMapping("/filter-fields")
    public ResponseEntity<Map<String, Object>> filterFields(
            @RequestParam String modelName,
            @RequestBody Map<String, Object> recordData,
            @RequestBody PermissionContext context) {
        Map<String, Object> filteredData = permissionService.filterRecordFields(modelName, recordData, context);
        return ResponseEntity.ok(success(filteredData));
    }

    @Operation(summary = "应用字段权限")
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyPermissions(
            @RequestParam String modelName,
            @RequestParam Boolean forWrite,
            @RequestBody Map<String, Object> recordData,
            @RequestBody PermissionContext context) {
        permissionService.applyFieldPermissions(modelName, recordData, context, forWrite);
        return ResponseEntity.ok(success(recordData));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}