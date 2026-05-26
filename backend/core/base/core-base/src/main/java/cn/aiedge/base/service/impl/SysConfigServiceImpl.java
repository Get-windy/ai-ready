package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.mapper.SysProjectConfigMapper;
import cn.aiedge.base.service.SysConfigService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysProjectConfigMapper configMapper;
    private final StringRedisTemplate redisTemplate;
    
    // 配置缓存
    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    
    // 配置历史表名
    private static final String CONFIG_HISTORY_KEY = "sys:config:history:";
    private static final String CONFIG_CHANGE_CHANNEL = "sys:config:change";
    
    // 当前租户ID（简化实现）
    private static final Long CURRENT_TENANT_ID = 1L;

    @Override
    public Optional<String> getValue(String key) {
        // 先从缓存获取
        String cachedValue = configCache.get(key);
        if (cachedValue != null) {
            return Optional.of(cachedValue);
        }
        
        // 从数据库获取
        String value = configMapper.getConfigValue(CURRENT_TENANT_ID, key);
        if (value != null) {
            configCache.put(key, value);
            return Optional.of(value);
        }
        
        return Optional.empty();
    }

    @Override
    public String getValue(String key, String defaultValue) {
        return getValue(key).orElse(defaultValue);
    }

    @Override
    public Optional<SysProjectConfig> getConfig(String key) {
        LambdaQueryWrapper<SysProjectConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysProjectConfig::getTenantId, CURRENT_TENANT_ID)
               .eq(SysProjectConfig::getConfigKey, key)
               .eq(SysProjectConfig::getDeleted, 0);
        
        return Optional.ofNullable(configMapper.selectOne(wrapper));
    }

    @Override
    public List<SysProjectConfig> getConfigsByGroup(String group) {
        return configMapper.selectByGroup(CURRENT_TENANT_ID, group);
    }

    @Override
    public List<SysProjectConfig> getAllConfigs() {
        return configMapper.selectAllConfigs(CURRENT_TENANT_ID);
    }

    @Override
    public Map<String, String> getConfigMap() {
        Map<String, String> map = new HashMap<>();
        List<SysProjectConfig> configs = getAllConfigs();
        for (SysProjectConfig config : configs) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setValue(String key, String value) {
        setValue(key, value, "string", "default", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setValue(String key, String value, String type, String group, String description) {
        Optional<SysProjectConfig> existing = getConfig(key);
        
        if (existing.isPresent()) {
            // 保存历史记录
            saveHistory(key, existing.get().getConfigValue(), value);
            
            // 更新配置
            LambdaUpdateWrapper<SysProjectConfig> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(SysProjectConfig::getId, existing.get().getId())
                   .set(SysProjectConfig::getConfigValue, value)
                   .set(SysProjectConfig::getUpdateTime, LocalDateTime.now());
            configMapper.update(null, wrapper);
        } else {
            // 新增配置
            SysProjectConfig config = new SysProjectConfig();
            config.setTenantId(CURRENT_TENANT_ID);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType(type);
            config.setConfigGroup(group);
            config.setDescription(description);
            config.setStatus(0);
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            configMapper.insert(config);
        }
        
        // 更新缓存
        configCache.put(key, value);
        
        // 发布变更事件
        publishConfigChange(key, value);
        
        log.info("配置已更新: {} = {}", key, value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setValues(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(String key) {
        Optional<SysProjectConfig> config = getConfig(key);
        if (config.isPresent()) {
            // 保存历史记录
            saveHistory(key, config.get().getConfigValue(), null);
            
            // 删除配置
            LambdaUpdateWrapper<SysProjectConfig> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(SysProjectConfig::getId, config.get().getId())
                   .set(SysProjectConfig::getDeleted, 1)
                   .set(SysProjectConfig::getUpdateTime, LocalDateTime.now());
            configMapper.update(null, wrapper);
            
            // 清除缓存
            configCache.remove(key);
            
            log.info("配置已删除: {}", key);
        }
    }

    @Override
    public void refreshCache() {
        configCache.clear();
        List<SysProjectConfig> configs = getAllConfigs();
        for (SysProjectConfig config : configs) {
            configCache.put(config.getConfigKey(), config.getConfigValue());
        }
        log.info("配置缓存已刷新，共{}项", configCache.size());
    }

    @Override
    public void refreshConfig(String key) {
        String value = configMapper.getConfigValue(CURRENT_TENANT_ID, key);
        if (value != null) {
            configCache.put(key, value);
        } else {
            configCache.remove(key);
        }
        log.info("配置已刷新: {}", key);
    }

    @Override
    public void publishConfigChange(String key, String value) {
        try {
            Map<String, String> message = new HashMap<>();
            message.put("key", key);
            message.put("value", value);
            message.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            redisTemplate.convertAndSend(CONFIG_CHANGE_CHANNEL, JSONUtil.toJsonStr(message));
            log.debug("配置变更已发布: {}", key);
        } catch (Exception e) {
            log.warn("发布配置变更失败: {}", e.getMessage());
        }
    }

    @Override
    public List<ConfigHistory> getConfigHistory(String key) {
        String historyKey = CONFIG_HISTORY_KEY + key;
        List<String> historyList = redisTemplate.opsForList().range(historyKey, 0, 99);
        
        List<ConfigHistory> result = new ArrayList<>();
        if (historyList != null) {
            for (String json : historyList) {
                Map<String, Object> map = JSONUtil.toBean(json, Map.class);
                result.add(new ConfigHistory(
                    Long.valueOf(map.get("id").toString()),
                    key,
                    (String) map.get("oldValue"),
                    (String) map.get("newValue"),
                    (String) map.get("changedBy"),
                    LocalDateTime.parse((String) map.get("changedAt")),
                    (String) map.get("changeReason")
                ));
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackConfig(String key, Long version) {
        List<ConfigHistory> history = getConfigHistory(key);
        
        for (ConfigHistory h : history) {
            if (h.id().equals(version)) {
                setValue(key, h.oldValue());
                log.info("配置已回滚: {} -> {}", key, h.oldValue());
                return;
            }
        }
        
        throw new RuntimeException("未找到配置历史版本: " + version);
    }

    @Override
    public ConfigDiff compareVersions(String key, Long version1, Long version2) {
        List<ConfigHistory> history = getConfigHistory(key);
        
        String value1 = null;
        String value2 = null;
        
        for (ConfigHistory h : history) {
            if (h.id().equals(version1)) {
                value1 = h.newValue();
            }
            if (h.id().equals(version2)) {
                value2 = h.newValue();
            }
        }
        
        return new ConfigDiff(key, value1, value2, 
            Objects.equals(value1, value2) ? "SAME" : "DIFFERENT");
    }

    @Override
    public <T> T getJsonValue(String key, Class<T> clazz) {
        String value = getValue(key, null);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return JSONUtil.toBean(value, clazz);
        } catch (Exception e) {
            log.warn("解析JSON配置失败: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 保存配置历史记录
     */
    private void saveHistory(String key, String oldValue, String newValue) {
        try {
            String historyKey = CONFIG_HISTORY_KEY + key;
            
            Map<String, Object> history = new HashMap<>();
            history.put("id", System.currentTimeMillis());
            history.put("configKey", key);
            history.put("oldValue", oldValue);
            history.put("newValue", newValue);
            history.put("changedBy", "system");
            history.put("changedAt", LocalDateTime.now().toString());
            history.put("changeReason", "配置更新");
            
            redisTemplate.opsForList().leftPush(historyKey, JSONUtil.toJsonStr(history));
            redisTemplate.opsForList().trim(historyKey, 0, 99); // 保留最近100条
            
            log.debug("配置历史已保存: {}", key);
        } catch (Exception e) {
            log.warn("保存配置历史失败: {}", e.getMessage());
        }
    }
}
