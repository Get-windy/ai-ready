package com.aiready.dict.service;

import com.aiready.dict.dto.*;
import com.aiready.dict.entity.DictType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 字典类型服务接口
 */
public interface DictTypeService extends IService<DictType> {
    
    /**
     * 根据字典编码获取字典类型
     */
    DictTypeDTO getByDictCode(String dictCode);
    
    /**
     * 保存字典类型
     */
    DictTypeDTO saveDictType(DictTypeSaveRequest request, Long operatorId);
    
    /**
     * 更新字典类型
     */
    DictTypeDTO updateDictType(Long dictTypeId, DictTypeSaveRequest request, Long operatorId);
    
    /**
     * 删除字典类型
     */
    void deleteDictType(Long dictTypeId, Long operatorId);
    
    /**
     * 批量删除字典类型
     */
    void batchDeleteDictTypes(List<Long> dictTypeIds, Long operatorId);
    
    /**
     * 查询字典类型列表
     */
    IPage<DictTypeDTO> queryDictTypes(DictQueryRequest request);
    
    /**
     * 获取所有启用的字典类型
     */
    List<DictTypeDTO> getAllActiveDictTypes();
    
    /**
     * 更新字典类型状态
     */
    DictTypeDTO updateStatus(Long dictTypeId, Integer status, Long operatorId);
    
    /**
     * 检查字典编码是否存在
     */
    boolean existsByDictCode(String dictCode);
    
    /**
     * 检查字典编码是否存在（排除指定ID）
     */
    boolean existsByDictCodeExcludeId(String dictCode, Long excludeId);
}
