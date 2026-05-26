package com.aiready.dict.controller;

import com.aiready.dict.dto.*;
import com.aiready.dict.service.DictService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 字典控制器
 */
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
@Tag(name = "字典管理", description = "字典管理接口")
public class DictController {

    private final DictService dictService;

    /**
     * 创建字典类型
     */
    @PostMapping("/create-type")
    @Operation(summary = "创建字典类型")
    public DictTypeDTO createDictType(@Valid @RequestBody DictSaveRequest request,
                                      @RequestParam Long operatorId) {
        return dictService.createDictType(request, operatorId);
    }

    /**
     * 更新字典类型
     */
    @PutMapping("/update-type/{dictTypeId}")
    @Operation(summary = "更新字典类型")
    public DictTypeDTO updateDictType(@PathVariable Long dictTypeId,
                                      @Valid @RequestBody DictSaveRequest request,
                                      @RequestParam Long operatorId) {
        return dictService.updateDictType(dictTypeId, request, operatorId);
    }

    /**
     * 删除字典类型
     */
    @DeleteMapping("/delete-type/{dictTypeId}")
    @Operation(summary = "删除字典类型")
    public void deleteDictType(@PathVariable Long dictTypeId,
                               @RequestParam Long operatorId) {
        dictService.deleteDictType(dictTypeId, operatorId);
    }

    /**
     * 批量删除字典类型
     */
    @PostMapping("/batch-delete-types")
    @Operation(summary = "批量删除字典类型")
    public void batchDeleteDictTypes(@RequestBody List<Long> dictTypeIds,
                                     @RequestParam Long operatorId) {
        dictService.batchDeleteDictTypes(dictTypeIds, operatorId);
    }

    /**
     * 获取字典类型详情
     */
    @GetMapping("/type/{dictTypeId}")
    @Operation(summary = "获取字典类型详情")
    public DictTypeDTO getDictTypeById(@PathVariable Long dictTypeId) {
        return dictService.getDictTypeById(dictTypeId);
    }

    /**
     * 获取字典类型详情（通过编码）
     */
    @GetMapping("/type/code/{dictTypeCode}")
    @Operation(summary = "获取字典类型详情（通过编码）")
    public DictTypeDTO getDictTypeByCode(@PathVariable String dictTypeCode) {
        return dictService.getDictTypeByCode(dictTypeCode);
    }

    /**
     * 查询字典类型列表
     */
    @PostMapping("/types")
    @Operation(summary = "查询字典类型列表")
    public IPage<DictTypeDTO> queryDictTypes(@RequestBody DictQueryRequest request) {
        return dictService.queryDictTypes(request);
    }

    /**
     * 获取所有字典类型
     */
    @GetMapping("/types/all")
    @Operation(summary = "获取所有字典类型")
    public List<DictTypeDTO> getAllDictTypes() {
        return dictService.getAllDictTypes();
    }

    /**
     * 获取字典项列表（通过类型ID）
     */
    @GetMapping("/items/type-id/{dictTypeId}")
    @Operation(summary = "获取字典项列表（通过类型ID）")
    public List<DictItemDTO> getDictItemsByTypeId(@PathVariable Long dictTypeId) {
        return dictService.getDictItemsByTypeId(dictTypeId);
    }

    /**
     * 获取字典项列表（通过类型编码）
     */
    @GetMapping("/items/type-code/{dictTypeCode}")
    @Operation(summary = "获取字典项列表（通过类型编码）")
    public List<DictItemDTO> getDictItemsByTypeCode(@PathVariable String dictTypeCode) {
        return dictService.getDictItemsByTypeCode(dictTypeCode);
    }

    /**
     * 获取字典项（通过类型编码和值）
     */
    @GetMapping("/item/value/{dictTypeCode}/{dictItemValue}")
    @Operation(summary = "获取字典项（通过类型编码和值）")
    public DictItemDTO getDictItemByTypeCodeAndValue(@PathVariable String dictTypeCode,
                                                    @PathVariable String dictItemValue) {
        return dictService.getDictItemByTypeCodeAndValue(dictTypeCode, dictItemValue);
    }

    /**
     * 获取字典项（通过类型编码和编码）
     */
    @GetMapping("/item/code/{dictTypeCode}/{dictItemCode}")
    @Operation(summary = "获取字典项（通过类型编码和编码）")
    public DictItemDTO getDictItemByTypeCodeAndCode(@PathVariable String dictTypeCode,
                                                   @PathVariable String dictItemCode) {
        return dictService.getDictItemByTypeCodeAndCode(dictTypeCode, dictItemCode);
    }

    /**
     * 更新字典项状态
     */
    @PostMapping("/item/status/{dictItemId}")
    @Operation(summary = "更新字典项状态")
    public void updateDictItemStatus(@PathVariable Long dictItemId,
                                     @RequestParam Integer status,
                                     @RequestParam Long operatorId) {
        dictService.updateDictItemStatus(dictItemId, status, operatorId);
    }

    /**
     * 更新字典类型状态
     */
    @PostMapping("/type/status/{dictTypeId}")
    @Operation(summary = "更新字典类型状态")
    public void updateDictTypeStatus(@PathVariable Long dictTypeId,
                                     @RequestParam Integer status,
                                     @RequestParam Long operatorId) {
        dictService.updateDictTypeStatus(dictTypeId, status, operatorId);
    }

    /**
     * 验证字典类型编码
     */
    @GetMapping("/validate/type-code")
    @Operation(summary = "验证字典类型编码")
    public boolean validateDictTypeCode(@RequestParam String dictTypeCode,
                                        @RequestParam(required = false) Long excludeId) {
        return dictService.validateDictTypeCode(dictTypeCode, excludeId);
    }

    /**
     * 验证字典项编码
     */
    @GetMapping("/validate/item-code")
    @Operation(summary = "验证字典项编码")
    public boolean validateDictItemCode(@RequestParam Long dictTypeId,
                                        @RequestParam String dictItemCode,
                                        @RequestParam(required = false) Long excludeId) {
        return dictService.validateDictItemCode(dictTypeId, dictItemCode, excludeId);
    }

    /**
     * 刷新字典缓存
     */
    @PostMapping("/refresh-cache")
    @Operation(summary = "刷新字典缓存")
    public void refreshCache() {
        dictService.refreshCache();
    }

    /**
     * 清理字典缓存
     */
    @PostMapping("/clear-cache")
    @Operation(summary = "清理字典缓存")
    public void clearCache() {
        dictService.clearCache();
    }

    /**
     * 获取缓存统计
     */
    @GetMapping("/cache-stats")
    @Operation(summary = "获取缓存统计")
    public java.util.Map<String, Object> getCacheStats() {
        return dictService.getCacheStats();
    }
}