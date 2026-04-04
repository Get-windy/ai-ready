package cn.aiedge.config.service.impl;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.config.model.ConfigChangeLog;
import cn.aiedge.config.model.SystemConfig;
import cn.aiedge.config.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final CacheService cacheService;
    
    private static final String CONFIG_KEY = "sys:config:";
    private static final String LOG_KEY = "sys:config:log:";
    
    // 内置系统配置
    private static final Map<String, SystemConfig> BUILTIN_CONFIGS = new LinkedHashMap<>();
    
    static {
        // 系统基础配置
        addBuiltinConfig("system.name", "AI-Ready系统", "system", "basic", "系统名称");
        addBuiltinConfig("system.version", "1.0.0", "system", "basic", "系统版本");
        addBuiltinConfig("system.logo", "/assets/logo.png", "system", "basic", "系统Logo");
        
        // 安全配置
        addBuiltinConfig("security.login.maxAttempts", "5", "security", "login", "最大登录尝试次数");
        addBuiltinConfig("security.login.lockTime", "30", "security", "login", "锁定时间(分钟)");
        addBuiltinConfig("security.password.minLength", "8", "security", "password", "密码最小长度");
        addBuiltinConfig("security.session.timeout", "7200", "security", "session", "会话超时(秒)");
        
        // 文件上传配置
        addBuiltinConfig("upload.maxSize", "10485760", "system", "upload", "最大上传大小(字节)");
        addBuiltinConfig("upload.allowedTypes", "jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx", "system", "upload", "允许的文件类型");
        
        // 通知配置
        addBuiltinConfig("notification.email.enabled", "true", "notification", "email", "启用邮件通知");
        addBuiltinConfig("notification.sms.enabled", "false", "notification", "sms", "启用短信通知");
    }
    
    private static void addBuiltinConfig(String key, String value, String type, String group, String name) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType(type);
        config.setConfigGroup(group);
        config.setConfigName(name);
        config.setValueType("string");
        config.setDefaultValue(value);
        config.setEnabled(true);
        config.setSystemConfig(true);
        BUILTIN_CONFIGS.put(key, config);
    }

    @Override
    public SystemConfig getConfig(Long id) {
        return null; // 简化实现
    }

    @Override
    public SystemConfig getConfigByKey(String configKey, Long tenantId) {
        SystemConfig builtin = BUILTIN_CONFIGS.get(configKey);
        if (builtin != null) return builtin;
        
        String key = CONFIG_KEY + tenantId + ":" + configKey;
        return cacheService.get(key, SystemConfig.class);
    }

    @Override
    public String getConfigValue(String configKey, Long tenantId) {
        SystemConfig config = getConfigByKey(configKey, tenantId);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public <T> T getConfigValue(String configKey, Class<T> clazz, T defaultValue, Long tenantId) {
        String value = getConfigValue(configKey, tenantId);
        if (value == null) return defaultValue;
        
        try {
            if (clazz == String.class) return (T) value;
            if (clazz == Integer.class || clazz == int.class) return (T) Integer.valueOf(value);
            if (clazz == Long.class || clazz == long.class) return (T) Long.valueOf(value);
            if (clazz == Boolean.class || clazz == boolean.class) return (T) Boolean.valueOf(value);
            if (clazz == Double.class || clazz == double.class) return (T) Double.valueOf(value);
        } catch (Exception e) {
            log.warn("配置值转换失败: key={}, value={}", configKey, value);
        }
        return defaultValue;
    }

    @Override
    public List<SystemConfig> getConfigList(String configType, String configGroup, Long tenantId) {
        List<SystemConfig> result = new ArrayList<>();
        
        for (SystemConfig config : BUILTIN_CONFIGS.values()) {
            if (configType != null && !configType.equals(config.getConfigType())) continue;
            if (configGroup != null && !configGroup.equals(config.getConfigGroup())) continue;
            result.add(config);
        }
        
        return result;
    }

    @Override
    public Map<String, String> getConfigMap(String configGroup, Long tenantId) {
        Map<String, String> result = new HashMap<>();
        
        for (SystemConfig config : BUILTIN_CONFIGS.values()) {
            if (configGroup != null && !configGroup.equals(config.getConfigGroup())) continue;
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        
        return result;
    }

    @Override
    public SystemConfig saveConfig(SystemConfig config, Long tenantId) {
        config.setUpdateTime(LocalDateTime.now());
        if (config.getCreateTime() == null) {
            config.setCreateTime(LocalDateTime.now());
        }
        config.setTenantId(tenantId);
        
        String key = CONFIG_KEY + tenantId + ":" + config.getConfigKey();
        cacheService.set(key, config);
        
        log.info("保存配置: key={}", config.getConfigKey());
        return config;
    }

    @Override
    public void saveConfigValue(String configKey, String configValue, Long tenantId) {
        SystemConfig config = getConfigByKey(configKey, tenantId);
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(configKey);
        }
        
        String oldValue = config.getConfigValue();
        config.setConfigValue(configValue);
        saveConfig(config, tenantId);
        
        logConfigChange(config, oldValue, "update", null, null, null, tenantId);
    }

    @Override
    public void batchSaveConfigs(Map<String, String> configs, Long tenantId) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            saveConfigValue(entry.getKey(), entry.getValue(), tenantId);
        }
        log.info("批量保存配置: count={}", configs.size());
    }

    @Override
    public boolean deleteConfig(Long id, Long tenantId) {
        return true;
    }

    @Override
    public boolean deleteConfigByKey(String configKey, Long tenantId) {
        if (BUILTIN_CONFIGS.containsKey(configKey)) {
            log.warn("不能删除内置配置: {}", configKey);
            return false;
        }
        
        String key = CONFIG_KEY + tenantId + ":" + configKey;
        cacheService.delete(key);
        return true;
    }

    @Override
    public List<ConfigChangeLog> getConfigChangeLogs(String configKey, Long tenantId) {
        String key = LOG_KEY + tenantId + ":" + configKey;
        List<Object> logs = cacheService.lRange(key, 0, 100);
        
        List<ConfigChangeLog> result = new ArrayList<>();
        if (logs != null) {
            for (Object obj : logs) {
                if (obj instanceof ConfigChangeLog) {
                    result.add((ConfigChangeLog) obj);
                }
            }
        }
        return result;
    }

    @Override
    public void logConfigChange(SystemConfig config, String oldValue, String changeType, 
                                Long operatorId, String operatorName, String reason, Long tenantId) {
        ConfigChangeLog logEntry = new ConfigChangeLog();
        logEntry.setConfigId(config.getId());
        logEntry.setConfigKey(config.getConfigKey());
        logEntry.setOldValue(oldValue);
        logEntry.setNewValue(config.getConfigValue());
        logEntry.setChangeType(changeType);
        logEntry.setChangeReason(reason);
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorName(operatorName);
        logEntry.setOperateTime(LocalDateTime.now());
        logEntry.setTenantId(tenantId);
        
        String key = LOG_KEY + tenantId + ":" + config.getConfigKey();
        cacheService.lPush(key, logEntry);
        cacheService.expire(key, 365, TimeUnit.DAYS);
    }

    @Override
    public void refreshCache(Long tenantId) {
        log.info("刷新配置缓存: tenantId={}", tenantId);
    }

    @Override
    public void refreshCache(String configKey, Long tenantId) {
        log.info("刷新配置缓存: key={}", configKey);
    }
}
