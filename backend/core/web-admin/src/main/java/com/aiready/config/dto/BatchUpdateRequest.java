package com.aiready.config.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 批量更新配置请求
 */
@Data
public class BatchUpdateRequest {
    
    /**
     * 配置键值对
     */
    private Map<String, String> configs;
    
    /**
     * 配置ID列表（用于批量更新）
     */
    private List<Long> configIds;
    
    /**
     * 批量更新的值
     */
    private String value;
    
    /**
     * 变更原因
     */
    private String changeReason;
}
