package cn.aiedge.config.service;

import cn.aiedge.config.model.ConfigChangeLog;
import cn.aiedge.config.model.SystemConfig;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {
    
    // 获取配置
    SystemConfig getConfig(Long id);
    SystemConfig getConfigByKey(String configKey, Long tenantId);
    String getConfigValue(String configKey, Long tenantId);
    <T> T getConfigValue(String configKey, Class<T> clazz, T defaultValue, Long tenantId);
    
    // 获取配置列表
    List<SystemConfig> getConfigList(String configType, String configGroup, Long tenantId);
    Map<String, String> getConfigMap(String configGroup, Long tenantId);
    
    // 保存配置
    SystemConfig saveConfig(SystemConfig config, Long tenantId);
    void saveConfigValue(String configKey, String configValue, Long tenantId);
    void batchSaveConfigs(Map<String, String> configs, Long tenantId);
    
    // 删除配置
    boolean deleteConfig(Long id, Long tenantId);
    boolean deleteConfigByKey(String configKey, Long tenantId);
    
    // 配置变更日志
    List<ConfigChangeLog> getConfigChangeLogs(String configKey, Long tenantId);
    void logConfigChange(SystemConfig config, String oldValue, String changeType, 
                         Long operatorId, String operatorName, String reason, Long tenantId);
    
    // 刷新缓存
    void refreshCache(Long tenantId);
    void refreshCache(String configKey, Long tenantId);
}
