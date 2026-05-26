package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysProjectConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统配置服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysConfigService {

    // ==================== 配置查询 ====================

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    Optional<String> getValue(String key);

    /**
     * 获取配置值（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getValue(String key, String defaultValue);

    /**
     * 获取配置对象
     *
     * @param key 配置键
     * @return 配置对象
     */
    Optional<SysProjectConfig> getConfig(String key);

    /**
     * 获取配置分组
     *
     * @param group 分组名
     * @return 配置列表
     */
    List<SysProjectConfig> getConfigsByGroup(String group);

    /**
     * 获取所有配置
     *
     * @return 配置列表
     */
    List<SysProjectConfig> getAllConfigs();

    /**
     * 获取配置Map
     *
     * @return 配置键值对
     */
    Map<String, String> getConfigMap();

    // ==================== 配置更新 ====================

    /**
     * 设置配置
     *
     * @param key   配置键
     * @param value 配置值
     */
    void setValue(String key, String value);

    /**
     * 设置配置（带描述）
     *
     * @param key         配置键
     * @param value       配置值
     * @param type        配置类型
     * @param group       配置分组
     * @param description 描述
     */
    void setValue(String key, String value, String type, String group, String description);

    /**
     * 批量设置配置
     *
     * @param configs 配置Map
     */
    void setValues(Map<String, String> configs);

    /**
     * 删除配置
     *
     * @param key 配置键
     */
    void deleteConfig(String key);

    // ==================== 配置热更新 ====================

    /**
     * 刷新配置缓存
     */
    void refreshCache();

    /**
     * 刷新指定配置
     *
     * @param key 配置键
     */
    void refreshConfig(String key);

    /**
     * 发布配置变更事件
     *
     * @param key   配置键
     * @param value 新值
     */
    void publishConfigChange(String key, String value);

    // ==================== 配置版本管理 ====================

    /**
     * 获取配置历史版本
     *
     * @param key 配置键
     * @return 历史记录
     */
    List<ConfigHistory> getConfigHistory(String key);

    /**
     * 回滚配置到指定版本
     *
     * @param key      配置键
     * @param version  版本号
     */
    void rollbackConfig(String key, Long version);

    /**
     * 比较配置版本差异
     *
     * @param key        配置键
     * @param version1   版本1
     * @param version2   版本2
     * @return 差异信息
     */
    ConfigDiff compareVersions(String key, Long version1, Long version2);

    // ==================== 类型转换 ====================

    /**
     * 获取整数配置
     */
    default Integer getIntValue(String key, Integer defaultValue) {
        String value = getValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取长整数配置
     */
    default Long getLongValue(String key, Long defaultValue) {
        String value = getValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取布尔配置
     */
    default Boolean getBooleanValue(String key, Boolean defaultValue) {
        String value = getValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 获取JSON配置
     */
    <T> T getJsonValue(String key, Class<T> clazz);

    // ==================== 内部类型 ====================

    /**
     * 配置历史记录
     */
    record ConfigHistory(
        Long id,
        String configKey,
        String oldValue,
        String newValue,
        String changedBy,
        java.time.LocalDateTime changedAt,
        String changeReason
    ) {}

    /**
     * 配置差异
     */
    record ConfigDiff(
        String configKey,
        String value1,
        String value2,
        String diffType
    ) {}
}
