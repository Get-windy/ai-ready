package com.aiready.config.service;

import com.aiready.config.dto.*;
import com.aiready.config.entity.SystemConfig;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService extends IService<SystemConfig> {
    
    /**
     * 根据配置键获取配置值
     */
    String getConfigValue(String configKey);
    
    /**
     * 根据配置键获取配置值，不存在返回默认值
     */
    String getConfigValue(String configKey, String defaultValue);
    
    /**
     * 获取整数配置值
     */
    Integer getIntValue(String configKey, Integer defaultValue);
    
    /**
     * 获取布尔配置值
     */
    Boolean getBooleanValue(String configKey, Boolean defaultValue);
    
    /**
     * 获取配置详情
     */
    SystemConfigDTO getConfigByKey(String configKey);
    
    /**
     * 保存配置
     */
    SystemConfigDTO saveConfig(ConfigSaveRequest request, Long operatorId);
    
    /**
     * 更新配置
     */
    SystemConfigDTO updateConfig(Long configId, ConfigSaveRequest request, Long operatorId);
    
    /**
     * 更新配置值（热更新）
     */
    SystemConfigDTO updateConfigValue(Long configId, String value, String changeReason, Long operatorId);
    
    /**
     * 批量更新配置
     */
    void batchUpdate(BatchUpdateRequest request, Long operatorId);
    
    /**
     * 删除配置
     */
    void deleteConfig(Long configId, Long operatorId);
    
    /**
     * 批量删除配置
     */
    void batchDelete(List<Long> configIds, Long operatorId);
    
    /**
     * 查询配置列表
     */
    IPage<SystemConfigDTO> queryConfigs(ConfigQueryRequest request);
    
    /**
     * 根据组查询配置
     */
    List<SystemConfigDTO> getConfigsByGroup(String groupCode);
    
    /**
     * 获取所有配置（用于缓存）
     */
    Map<String, String> getAllConfigs();
    
    /**
     * 刷新配置缓存
     */
    void refreshCache();
    
    /**
     * 导出配置
     */
    String exportConfigs(ConfigImportExportDTO request);
    
    /**
     * 导入配置
     */
    void importConfigs(ConfigImportExportDTO request, Long operatorId);
    
    /**
     * 回滚到指定版本
     */
    SystemConfigDTO rollbackToVersion(Long configId, Integer version, Long operatorId);
}
