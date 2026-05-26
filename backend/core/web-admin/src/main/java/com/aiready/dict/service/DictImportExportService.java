package com.aiready.dict.service;

import com.aiready.dict.dto.DictImportExportDTO;

import java.util.List;

/**
 * 字典导入导出服务接口
 */
public interface DictImportExportService {
    
    /**
     * 导出字典（包含字典类型和字典项）
     */
    String exportDicts(DictImportExportDTO request);
    
    /**
     * 导入字典
     */
    void importDicts(DictImportExportDTO request, Long operatorId);
    
    /**
     * 导出单个字典类型
     */
    String exportSingleDict(String dictCode, boolean includeItems);
    
    /**
     * 导出多个字典类型
     */
    String exportMultipleDicts(List<String> dictCodes, boolean includeItems);
}
