package cn.aiedge.dict.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.aiedge.dict.dto.DictTypeDTO;
import cn.aiedge.dict.model.DictType;
import cn.aiedge.dict.service.DictTypeService;
import cn.aiedge.dict.vo.DictTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典类型控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/dict/type")
@RequiredArgsConstructor
@Tag(name = "字典类型", description = "字典类型管理接口")
public class DictTypeController {

    private final DictTypeService dictTypeService;

    @PostMapping
    @SaCheckPermission("system:dict:create")
    @Operation(summary = "创建字典类型")
    public Map<String, Object> create(@Valid @RequestBody DictTypeDTO dictTypeDTO) {
        Long id = dictTypeService.create(dictTypeDTO);
        return Map.of("success", true, "id", id);
    }

    @PutMapping
    @SaCheckPermission("system:dict:update")
    @Operation(summary = "更新字典类型")
    public Map<String, Object> update(@Valid @RequestBody DictTypeDTO dictTypeDTO) {
        boolean result = dictTypeService.update(dictTypeDTO);
        return Map.of("success", result);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:dict:delete")
    @Operation(summary = "删除字典类型")
    public Map<String, Object> delete(
            @Parameter(description = "字典类型ID") @PathVariable Long id) {
        boolean result = dictTypeService.delete(id);
        return Map.of("success", result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取字典类型详情")
    public DictTypeVO getById(
            @Parameter(description = "字典类型ID") @PathVariable Long id) {
        return dictTypeService.getById(id);
    }

    @GetMapping("/code/{dictCode}")
    @Operation(summary = "根据编码获取字典类型")
    public DictTypeVO getByDictCode(
            @Parameter(description = "字典类型编码") @PathVariable String dictCode) {
        return dictTypeService.getByDictCode(dictCode);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询字典类型")
    public Map<String, Object> page(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "字典类型编码") @RequestParam(required = false) String dictCode,
            @Parameter(description = "字典类型名称") @RequestParam(required = false) String dictName,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {
        return list(tenantId, dictCode, dictName, status, page, pageSize);
    }

    @GetMapping("/list")
    @Operation(summary = "查询字典类型列表")
    public Map<String, Object> list(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "字典类型编码") @RequestParam(required = false) String dictCode,
            @Parameter(description = "字典类型名称") @RequestParam(required = false) String dictName,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {

        Map<String, Object> params = Map.of(
            "tenantId", tenantId != null ? tenantId : 1L,
            "dictCode", dictCode != null ? dictCode : "",
            "dictName", dictName != null ? dictName : "",
            "status", status != null ? status : "",
            "page", page,
            "pageSize", pageSize
        );
        return dictTypeService.list(params);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取字典类型树形结构")
    public List<DictTypeVO> getTree(
            @Parameter(description = "父ID") @RequestParam(required = false, defaultValue = "0") Long parentId) {
        return dictTypeService.getTree(parentId);
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的字典类型")
    public List<DictTypeVO> getEnabled() {
        return dictTypeService.getEnabled();
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("system:dict:update")
    @Operation(summary = "修改字典类型状态")
    public Map<String, Object> updateStatus(
            @Parameter(description = "字典类型ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {
        boolean result = dictTypeService.updateStatus(id, status);
        return Map.of("success", result);
    }

    @GetMapping("/export")
    @SaCheckPermission("system:dict:export")
    @Operation(summary = "导出字典类型")
    public List<DictType> export(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "字典类型编码") @RequestParam(required = false) String dictCode,
            @Parameter(description = "字典类型名称") @RequestParam(required = false) String dictName,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {

        Map<String, Object> params = Map.of(
            "tenantId", tenantId != null ? tenantId : 1L,
            "dictCode", dictCode != null ? dictCode : "",
            "dictName", dictName != null ? dictName : "",
            "status", status != null ? status : ""
        );
        return dictTypeService.export(params);
    }

    @DeleteMapping("/cache/{dictCode}")
    @SaCheckPermission("system:dict:update")
    @Operation(summary = "清理字典类型缓存")
    public Map<String, Object> clearCache(
            @Parameter(description = "字典类型编码") @PathVariable String dictCode) {
        dictTypeService.clearCache(dictCode);
        return Map.of("success", true);
    }

    @DeleteMapping("/cache")
    @SaCheckPermission("system:dict:update")
    @Operation(summary = "清理所有字典缓存")
    public Map<String, Object> clearAllCache() {
        dictTypeService.clearAllCache();
        return Map.of("success", true);
    }
}
