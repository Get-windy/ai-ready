package com.aiready.config.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 配置导入导出DTO
 */
@Data
public class ConfigImportExportDTO {
    
    /**
     * 导出格式（json/yaml/properties）
     */
    private String format;
    
    /**
     * 配置组编码（为空则导出所有）
     */
    private String groupCode;
    
    /**
     * 配置数据（导入时使用）
     */
    private String content;
    
    /**
     * 配置列表（导出时使用）
     */
    private List<Map<String, Object>> configs;
    
    /**
     * 是否覆盖已存在配置
     */
    private Boolean overwrite;
}
