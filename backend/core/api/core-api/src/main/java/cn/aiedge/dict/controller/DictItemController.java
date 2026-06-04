package cn.aiedge.dict.controller;

import cn.aiedge.dict.dto.DictItemDTO;
import cn.aiedge.dict.model.DictItem;
import cn.aiedge.dict.service.DictItemService;
import cn.aiedge.dict.vo.DictItemVO;
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
 * 字典项控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/dict/item")
@RequiredArgsConstructor
@Tag(name = "字典项", description = "字典项管理接口")
public class DictItemController {

    private final DictItemService dictItemService;

    @PostMapping
    @Operation(summary = "创建字典项")
    public Map<String, Object> create(@Valid @RequestBody DictItemDTO dictItemDTO) {
        Long id = dictItemService.create(dictItemDTO);
        return Map.of("success", true, "id", id);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量创建字典项")
    public Map<String, Object> batchCreate(@Valid @RequestBody List<DictItemDTO> dictItemDTOs) {
        int count = dictItemService.batchCreate(dictItemDTOs);
        return Map.of("success", true, "count", count);
    }

    @PutMapping
    @Operation(summary = "更新字典项")
    public Map<String, Object> update(@Valid @RequestBody DictItemDTO dictItemDTO) {
        boolean result = dictItemService.update(dictItemDTO);
        return Map.of("success", result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典项")
    public Map<String, Object> delete(
            @Parameter(description = "字典项ID") @PathVariable Long id) {
        boolean result = dictItemService.delete(id);
        return Map.of("success", result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取字典项详情")
    public DictItemVO getById(
            @Parameter(description = "字典项ID") @PathVariable Long id) {
        return dictItemService.getById(id);
    }

    @GetMapping("/type/{dictTypeId}")
    @Operation(summary = "根据字典类型查询字典项")
    public List<DictItemVO> getByDictTypeId(
            @Parameter(description = "字典类型ID") @PathVariable Long dictTypeId) {
        return dictItemService.getByDictTypeId(dictTypeId);
    }

    @GetMapping("/code/{dictCode}")
    @Operation(summary = "根据字典类型编码查询字典项")
    public List<DictItemVO> getByDictCode(
            @Parameter(description = "字典类型编码") @PathVariable String dictCode) {
        return dictItemService.getByDictCode(dictCode);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取字典项树形结构")
    public List<DictItemVO> getTree(
            @Parameter(description = "字典类型ID") @RequestParam Long dictTypeId,
            @Parameter(description = "父ID") @RequestParam(required = false, defaultValue = "0") Long parentId) {
        return dictItemService.getTree(dictTypeId, parentId);
    }

    @GetMapping("/list")
    @Operation(summary = "查询字典项列表")
    public Map<String, Object> list(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "字典类型ID") @RequestParam(required = false) Long dictTypeId,
            @Parameter(description = "字典项值") @RequestParam(required = false) String itemValue,
            @Parameter(description = "字典项文本") @RequestParam(required = false) String itemText,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {
        
        Map<String, Object> params = Map.of(
            "tenantId", tenantId != null ? tenantId : 1L,
            "dictTypeId", dictTypeId != null ? dictTypeId : "",
            "itemValue", itemValue != null ? itemValue : "",
            "itemText", itemText != null ? itemText : "",
            "status", status != null ? status : "",
            "page", page,
            "pageSize", pageSize
        );
        return dictItemService.list(params);
    }

    @GetMapping("/value")
    @Operation(summary = "根据字典类型和项值获取字典项")
    public DictItemVO getByDictCodeAndValue(
            @Parameter(description = "字典类型编码") @RequestParam String dictCode,
            @Parameter(description = "字典项值") @RequestParam String itemValue) {
        return dictItemService.getByDictCodeAndValue(dictCode, itemValue);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改字典项状态")
    public Map<String, Object> updateStatus(
            @Parameter(description = "字典项ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {
        boolean result = dictItemService.updateStatus(id, status);
        return Map.of("success", result);
    }

    @GetMapping("/export")
    @Operation(summary = "导出字典项")
    public List<DictItem> export(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "字典类型ID") @RequestParam(required = false) Long dictTypeId,
            @Parameter(description = "字典项值") @RequestParam(required = false) String itemValue,
            @Parameter(description = "字典项文本") @RequestParam(required = false) String itemText,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        
        Map<String, Object> params = Map.of(
            "tenantId", tenantId != null ? tenantId : 1L,
            "dictTypeId", dictTypeId != null ? dictTypeId : "",
            "itemValue", itemValue != null ? itemValue : "",
            "itemText", itemText != null ? itemText : "",
            "status", status != null ? status : ""
        );
        return dictItemService.export(params);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除字典项")
    public Map<String, Object> batchDelete(@RequestBody List<Long> ids) {
        boolean result = dictItemService.removeBatchByIds(ids);
        return Map.of("success", result);
    }

    @PutMapping("/cache/refresh/{dictCode}")
    @Operation(summary = "刷新字典项缓存")
    public Map<String, Object> refreshCache(
            @Parameter(description = "字典类型编码") @PathVariable String dictCode) {
        dictItemService.refreshCache(dictCode);
        return Map.of("success", true);
    }
}
