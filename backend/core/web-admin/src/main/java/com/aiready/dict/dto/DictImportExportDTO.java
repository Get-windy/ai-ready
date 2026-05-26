package com.aiready.dict.dto;

import lombok.Data;

import java.util.List;

/**
 * 字典导入导出DTO
 */
@Data
public class DictImportExportDTO {
    
    /**
     * 导出内容（导入时使用）
     */
    private String content;
    
    /**
     * 字典类型编码（用于按类型导出）
     */
    private String dictCode;
    
    /**
     * 字典类型编码列表（用于批量导出）
     */
    private List<String> dictCodes;
    
    /**
     * 是否覆盖（导入时使用）
     */
    private Boolean overwrite;
    
    /**
     * 是否包含字典项
     */
    private Boolean includeItems;
}
