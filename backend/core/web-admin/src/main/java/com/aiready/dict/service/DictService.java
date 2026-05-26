package com.aiready.dict.service;

import com.aiready.dict.dto.*;
import com.aiready.dict.entity.DictType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 字典服务接口
 */
public interface DictService extends IService<DictType> {
    
    /**
     * 创建字典类型
     */
    DictTypeDTO createDictType(DictSaveRequest request, Long operatorId);
    
    /**
     * 更新字典类型
     */
    DictTypeDTO updateDictType(Long dictTypeId, DictSaveRequest request, Long operatorId);
    
    /**
     * 删除字典类型
     */
    void deleteDictType(Long dictTypeId, Long operatorId);
    
    /**
     * 批量删除字典类型
     */
    void batchDeleteDictTypes(List<Long> dictTypeIds, Long operatorId);
    
    /**
     * 获取字典类型详情
     */
    DictTypeDTO getDictTypeById(Long dictTypeId);
    
    /**
     * 获取字典类型详情（通过编码）
     */
    DictTypeDTO getDictTypeByCode(String dictTypeCode);
    
    /**
     * 查询字典类型列表
     */
    IPage<DictTypeDTO> queryDictTypes(DictQueryRequest request);
    
    /**
     * 获取所有字典类型
     */
    List<DictTypeDTO> getAllDictTypes();
    
    /**
     * 获取字典项列表（通过类型ID）
     */
    List<DictItemDTO> getDictItemsByTypeId(Long dictTypeId);
    
    /**
     * 获取字典项列表（通过类型编码）
     */
    List<DictItemDTO> getDictItemsByTypeCode(String dictTypeCode);
    
    /**
     * 获取字典项（通过类型编码和值）
     */
    DictItemDTO getDictItemByTypeCodeAndValue(String dictTypeCode, String dictItemValue);
    
    /**
     * 获取字典项（通过类型编码和编码）
     */
    DictItemDTO getDictItemByTypeCodeAndCode(String dictTypeCode, String dictItemCode);
    
    /**
     * 获取字典项（通过类型ID和值）
     */
    DictItemDTO getDictItemByTypeIdAndValue(Long dictTypeId, String dictItemValue);
    
    /**
     * 获取字典项（通过类型ID和编码）
     */
    DictItemDTO getDictItemByTypeIdAndCode(Long dictTypeId, String dictItemCode);
    
    /**
     * 更新字典项状态
     */
    void updateDictItemStatus(Long dictItemId, Integer status, Long operatorId);
    
    /**
     * 更新字典类型状态
     */
    void updateDictTypeStatus(Long dictTypeId, Integer status, Long operatorId);
    
    /**
     * 验证字典类型编码唯一性
     */
    boolean validateDictTypeCode(String dictTypeCode, Long excludeId);
    
    /**
     * 验证字典项编码唯一性
     */
    boolean validateDictItemCode(Long dictTypeId, String dictItemCode, Long excludeId);
    
    /**
     * 导出字典数据
     */
    String exportDictData(DictQueryRequest request);
    
    /**
     * 导入字典数据
     */
    void importDictData(String filePath, Long operatorId);
    
    /**
     * 刷新字典缓存
     */
    void refreshCache();
    
    /**
     * 获取字典缓存统计
     */
    Map<String, Object> getCacheStats();
    
    /**
     * 清理字典缓存
     */
    void clearCache();
}
