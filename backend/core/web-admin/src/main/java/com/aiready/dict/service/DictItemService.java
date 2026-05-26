package com.aiready.dict.service;

import com.aiready.dict.dto.*;
import com.aiready.dict.entity.DictItem;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 字典项服务接口
 */
public interface DictItemService extends IService<DictItem> {
    
    /**
     * 根据字典编码获取字典项列表
     */
    List<DictItemDTO> getItemsByDictCode(String dictCode);
    
    /**
     * 根据字典类型ID获取字典项列表
     */
    List<DictItemDTO> getItemsByDictTypeId(Long dictTypeId);
    
    /**
     * 获取树形字典项
     */
    List<DictItemDTO> getTreeItems(String dictCode);
    
    /**
     * 根据ID获取字典项详情
     */
    DictItemDTO getItemById(Long itemId);
    
    /**
     * 根据字典编码和字典项编码获取字典项
     */
    DictItemDTO getItemByCode(String dictCode, String itemCode);
    
    /**
     * 根据字典编码和字典项值获取字典项名称
     */
    String getItemNameByValue(String dictCode, String itemValue);
    
    /**
     * 保存字典项
     */
    DictItemDTO saveDictItem(DictItemSaveRequest request, Long operatorId);
    
    /**
     * 更新字典项
     */
    DictItemDTO updateDictItem(Long itemId, DictItemSaveRequest request, Long operatorId);
    
    /**
     * 删除字典项
     */
    void deleteDictItem(Long itemId, Long operatorId);
    
    /**
     * 批量删除字典项
     */
    void batchDeleteDictItems(List<Long> itemIds, Long operatorId);
    
    /**
     * 批量保存字典项
     */
    void batchSaveDictItems(DictItemBatchRequest request, Long operatorId);
    
    /**
     * 查询字典项列表
     */
    IPage<DictItemDTO> queryDictItems(DictQueryRequest request);
    
    /**
     * 更新字典项状态
     */
    DictItemDTO updateItemStatus(Long itemId, Integer status, Long operatorId);
    
    /**
     * 获取默认选项
     */
    DictItemDTO getDefaultItem(String dictCode);
    
    /**
     * 获取字典项Map（itemCode -> itemName）
     */
    Map<String, String> getItemMap(String dictCode);
    
    /**
     * 获取字典项Map（itemValue -> itemName）
     */
    Map<String, String> getValueNameMap(String dictCode);
}
