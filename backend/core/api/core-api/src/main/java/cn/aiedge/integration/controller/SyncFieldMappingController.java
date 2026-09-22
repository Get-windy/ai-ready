package cn.aiedge.integration.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.integration.model.SyncFieldMapping;
import cn.aiedge.integration.service.SyncFieldMappingService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 同步字段映射控制器
 *
 * 管理外部系统与本地系统的字段对应关系。
 * 每个同步配置可以按单据类型（601=客户、604=供应商、504=商品、801=库存）配置独立的映射规则。
 */
@RestController
@RequestMapping("/api/v1/sync-config")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "同步字段映射", description = "配置外部系统字段与本地字段的映射规则")
public class SyncFieldMappingController {

    private final SyncFieldMappingService fieldMappingService;

    // ==================== 查询 ====================

    @SaCheckPermission("system:dataimport:list")
    @GetMapping("/{configId}/field-mappings")
    @Operation(summary = "查询指定配置的所有字段映射")
    public ResponseEntity<ApiResponse<List<SyncFieldMapping>>> listFieldMappings(
            @PathVariable Long configId,
            @RequestParam(required = false) String billType) {
        List<SyncFieldMapping> mappings;
        if (billType != null && !billType.isEmpty()) {
            mappings = fieldMappingService.listByConfigAndBillType(configId, billType);
        } else {
            mappings = fieldMappingService.listByConfigId(configId);
        }
        return ResponseEntity.ok(ApiResponse.success(mappings));
    }

    @SaCheckPermission("system:dataimport:list")
    @GetMapping("/{configId}/field-mappings/{billType}")
    @Operation(summary = "按单据类型查询字段映射")
    public ResponseEntity<ApiResponse<List<SyncFieldMapping>>> getMappingsByBillType(
            @PathVariable Long configId,
            @PathVariable String billType) {
        return ResponseEntity.ok(ApiResponse.success(fieldMappingService.listByConfigAndBillType(configId, billType)));
    }

    // ==================== 增删改 ====================

    @SaCheckPermission("system:dataimport:create")
    @PostMapping("/{configId}/field-mappings")
    @Operation(summary = "创建字段映射")
    public ResponseEntity<ApiResponse<SyncFieldMapping>> createFieldMapping(
            @PathVariable Long configId,
            @RequestBody SyncFieldMapping mapping) {
        mapping.setSourceConfigId(configId);
        return ResponseEntity.ok(ApiResponse.success(fieldMappingService.create(mapping)));
    }

    @SaCheckPermission("system:dataimport:update")
    @PutMapping("/{configId}/field-mappings/{id}")
    @Operation(summary = "更新字段映射")
    public ResponseEntity<ApiResponse<SyncFieldMapping>> updateFieldMapping(
            @PathVariable Long configId,
            @PathVariable Long id,
            @RequestBody SyncFieldMapping mapping) {
        return ResponseEntity.ok(ApiResponse.success(fieldMappingService.update(id, mapping)));
    }

    @SaCheckPermission("system:dataimport:delete")
    @DeleteMapping("/{configId}/field-mappings/{id}")
    @Operation(summary = "删除字段映射")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteFieldMapping(
            @PathVariable Long configId,
            @PathVariable Long id) {
        boolean deleted = fieldMappingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("success", deleted)));
    }

    /**
     * 批量保存：前端传入完整的映射列表，服务端先清空再插入
     * 适用于"一次性保存整个单据类型的映射"场景
     */
    @SaCheckPermission("system:dataimport:create")
    @PostMapping("/{configId}/field-mappings/batch")
    @Operation(summary = "批量保存字段映射（先清空再写入）")
    public ResponseEntity<ApiResponse<List<SyncFieldMapping>>> batchSaveFieldMappings(
            @PathVariable Long configId,
            @RequestBody List<SyncFieldMapping> mappings) {
        return ResponseEntity.ok(ApiResponse.success(fieldMappingService.batchSave(configId, mappings)));
    }

    // ==================== 模板初始化 ====================

    @SaCheckPermission("system:dataimport:create")
    @PostMapping("/{configId}/field-mappings/init-template")
    @Operation(summary = "从预置模板初始化字段映射")
    public ResponseEntity<ApiResponse<List<SyncFieldMapping>>> initFromTemplate(
            @PathVariable Long configId,
            @RequestParam String billType) {
        return ResponseEntity.ok(ApiResponse.success(fieldMappingService.initFromTemplate(configId, billType)));
    }
}
