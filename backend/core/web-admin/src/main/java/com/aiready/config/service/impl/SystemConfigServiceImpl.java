package com.aiready.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.aiready.config.dto.*;
import com.aiready.config.entity.SystemConfig;
import com.aiready.config.mapper.SystemConfigMapper;
import com.aiready.config.service.ConfigAuditService;
import com.aiready.config.service.ConfigVersionService;
import com.aiready.config.service.SystemConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> 
        implements SystemConfigService {
    
    private final SystemConfigMapper configMapper;
    private final ConfigVersionService versionService;
    private final ConfigAuditService auditService;
    
    // 本地缓存
    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    
    @Override
    @Cacheable(value = "config", key = "#configKey")
    public String getConfigValue(String configKey) {
        // 先查本地缓存
        String cachedValue = configCache.get(configKey);
        if (cachedValue != null) {
            return cachedValue;
        }
        
        SystemConfig config = configMapper.selectByKey(configKey);
        if (config != null && config.getStatus() == 1) {
            String value = config.getSensitive() == 1 ? decrypt(config.getConfigValue()) : config.getConfigValue();
            configCache.put(configKey, value);
            return value;
        }
        return null;
    }
    
    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }
    
    @Override
    public Integer getIntValue(String configKey, Integer defaultValue) {
        String value = getConfigValue(configKey);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("配置值不是整数: {}", configKey);
            }
        }
        return defaultValue;
    }
    
    @Override
    public Boolean getBooleanValue(String configKey, Boolean defaultValue) {
        String value = getConfigValue(configKey);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    @Override
    public SystemConfigDTO getConfigByKey(String configKey) {
        SystemConfig config = configMapper.selectByKey(configKey);
        return config != null ? convertToDTO(config) : null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "config", key = "#request.configKey")
    public SystemConfigDTO saveConfig(ConfigSaveRequest request, Long operatorId) {
        // 检查配置键是否已存在
        if (configMapper.existsByKey(request.getConfigKey()) > 0) {
            throw new RuntimeException("配置键已存在: " + request.getConfigKey());
        }
        
        SystemConfig config = new SystemConfig();
        BeanUtil.copyProperties(request, config);
        
        // 敏感配置加密
        if (config.getSensitive() != null && config.getSensitive() == 1) {
            config.setConfigValue(encrypt(config.getConfigValue()));
        }
        
        config.setCreateBy(operatorId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateBy(operatorId);
        config.setUpdateTime(LocalDateTime.now());
        
        configMapper.insert(config);
        
        // 创建版本记录
        versionService.createVersion(config.getId(), config.getConfigKey(), null, 
                request.getConfigValue(), "CREATE", request.getChangeReason(), operatorId);
        
        // 审计日志
        auditService.audit(config.getId(), config.getConfigKey(), "CREATE", "创建配置",
                null, JSONUtil.toJsonStr(config), operatorId, null, null, true, null);
        
        // 更新缓存
        configCache.put(config.getConfigKey(), request.getConfigValue());
        
        return convertToDTO(config);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "config", key = "#request.configKey")
    public SystemConfigDTO updateConfig(Long configId, ConfigSaveRequest request, Long operatorId) {
        SystemConfig config = configMapper.selectById(configId);
        if (config == null) {
            throw new RuntimeException("配置不存在");
        }
        
        // 检查是否可编辑
        if (config.getEditable() != null && config.getEditable() == 0) {
            throw new RuntimeException("该配置不可编辑");
        }
        
        String oldValue = config.getConfigValue();
        String oldData = JSONUtil.toJsonStr(config);
        
        BeanUtil.copyProperties(request, config);
        config.setId(configId);
        
        // 敏感配置加密
        if (config.getSensitive() != null && config.getSensitive() == 1) {
            config.setConfigValue(encrypt(config.getConfigValue()));
        }
        
        config.setUpdateBy(operatorId);
        config.setUpdateTime(LocalDateTime.now());
        
        configMapper.updateById(config);
        
        // 创建版本记录
        versionService.createVersion(configId, config.getConfigKey(), oldValue,
                request.getConfigValue(), "UPDATE", request.getChangeReason(), operatorId);
        
        // 审计日志
        auditService.audit(configId, config.getConfigKey(), "UPDATE", "更新配置",
                oldData, JSONUtil.toJsonStr(config), operatorId, null, null, true, null);
        
        // 更新缓存
        configCache.put(config.getConfigKey(), request.getConfigValue());
        
        return convertToDTO(config);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "config", key = "@systemConfigServiceImpl.getConfigKeyById(#configId)")
    public SystemConfigDTO updateConfigValue(Long configId, String value, String changeReason, Long operatorId) {
        SystemConfig config = configMapper.selectById(configId);
        if (config == null) {
            throw new RuntimeException("配置不存在");
        }
        
        if (config.getEditable() != null && config.getEditable() == 0) {
            throw new RuntimeException("该配置不可编辑");
        }
        
        String oldValue = config.getConfigValue();
        String oldData = JSONUtil.toJsonStr(config);
        
        // 敏感配置加密
        if (config.getSensitive() != null && config.getSensitive() == 1) {
            config.setConfigValue(encrypt(value));
        } else {
            config.setConfigValue(value);
        }
        
        config.setUpdateBy(operatorId);
        config.setUpdateTime(LocalDateTime.now());
        
        configMapper.updateById(config);
        
        // 创建版本记录
        versionService.createVersion(configId, config.getConfigKey(), oldValue,
                value, "UPDATE", changeReason, operatorId);
        
        // 审计日志
        auditService.audit(configId, config.getConfigKey(), "UPDATE", "热更新配置值",
                oldData, JSONUtil.toJsonStr(config), operatorId, null, null, true, null);
        
        // 更新缓存
        configCache.put(config.getConfigKey(), value);
        
        return convertToDTO(config);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(BatchUpdateRequest request, Long operatorId) {
        if (request.getConfigs() != null) {
            for (Map.Entry<String, String> entry : request.getConfigs().entrySet()) {
                SystemConfig config = configMapper.selectByKey(entry.getKey());
                if (config != null && (config.getEditable() == null || config.getEditable() == 1)) {
                    updateConfigValue(config.getId(), entry.getValue(), request.getChangeReason(), operatorId);
                }
            }
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long configId, Long operatorId) {
        SystemConfig config = configMapper.selectById(configId);
        if (config == null) {
            return;
        }
        
        if (config.getDeletable() != null && config.getDeletable() == 0) {
            throw new RuntimeException("该配置不可删除");
        }
        
        String oldData = JSONUtil.toJsonStr(config);
        
        configMapper.deleteById(configId);
        
        // 创建版本记录
        versionService.createVersion(configId, config.getConfigKey(), config.getConfigValue(),
                null, "DELETE", null, operatorId);
        
        // 审计日志
        auditService.audit(configId, config.getConfigKey(), "DELETE", "删除配置",
                oldData, null, operatorId, null, null, true, null);
        
        // 清除缓存
        configCache.remove(config.getConfigKey());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> configIds, Long operatorId) {
        if (configIds != null) {
            for (Long configId : configIds) {
                deleteConfig(configId, operatorId);
            }
        }
    }
    
    @Override
    public IPage<SystemConfigDTO> queryConfigs(ConfigQueryRequest request) {
        Page<SystemConfig> pageParam = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (request.getConfigKey() != null && !request.getConfigKey().isEmpty()) {
            wrapper.like(SystemConfig::getConfigKey, request.getConfigKey());
        }
        if (request.getConfigName() != null && !request.getConfigName().isEmpty()) {
            wrapper.like(SystemConfig::getConfigName, request.getConfigName());
        }
        if (request.getGroupId() != null) {
            wrapper.eq(SystemConfig::getGroupId, request.getGroupId());
        }
        if (request.getGroupCode() != null) {
            wrapper.eq(SystemConfig::getGroupCode, request.getGroupCode());
        }
        if (request.getDataType() != null) {
            wrapper.eq(SystemConfig::getDataType, request.getDataType());
        }
        if (request.getSensitive() != null) {
            wrapper.eq(SystemConfig::getSensitive, request.getSensitive());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SystemConfig::getStatus, request.getStatus());
        }
        if (request.getCreateTimeStart() != null) {
            wrapper.ge(SystemConfig::getCreateTime, request.getCreateTimeStart());
        }
        if (request.getCreateTimeEnd() != null) {
            wrapper.le(SystemConfig::getCreateTime, request.getCreateTimeEnd());
        }
        
        wrapper.eq(SystemConfig::getDeleted, 0);
        wrapper.orderByAsc(SystemConfig::getGroupId).orderByAsc(SystemConfig::getSortOrder);
        
        IPage<SystemConfig> configPage = configMapper.selectPage(pageParam, wrapper);
        return configPage.convert(this::convertToDTO);
    }
    
    @Override
    public List<SystemConfigDTO> getConfigsByGroup(String groupCode) {
        List<SystemConfig> configs = configMapper.selectByGroupCode(groupCode);
        return configs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    public Map<String, String> getAllConfigs() {
        List<SystemConfig> configs = configMapper.selectAllActive();
        return configs.stream().collect(Collectors.toMap(
                SystemConfig::getConfigKey,
                config -> config.getSensitive() == 1 ? decrypt(config.getConfigValue()) : config.getConfigValue(),
                (v1, v2) -> v1
        ));
    }
    
    @Override
    public void refreshCache() {
        configCache.clear();
        configCache.putAll(getAllConfigs());
        log.info("配置缓存已刷新，共 {} 条", configCache.size());
    }
    
    @Override
    public String exportConfigs(ConfigImportExportDTO request) {
        List<SystemConfig> configs;
        if (request.getGroupCode() != null) {
            configs = configMapper.selectByGroupCode(request.getGroupCode());
        } else {
            configs = configMapper.selectAllActive();
        }
        
        List<SystemConfigDTO> dtoList = configs.stream().map(this::convertToDTO).collect(Collectors.toList());
        
        return JSONUtil.toJsonPrettyStr(dtoList);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importConfigs(ConfigImportExportDTO request, Long operatorId) {
        // 解析导入数据
        List<SystemConfigDTO> dtoList = JSONUtil.toList(request.getContent(), SystemConfigDTO.class);
        
        for (SystemConfigDTO dto : dtoList) {
            SystemConfig existing = configMapper.selectByKey(dto.getConfigKey());
            if (existing != null) {
                if (Boolean.TRUE.equals(request.getOverwrite())) {
                    // 更新
                    ConfigSaveRequest updateRequest = new ConfigSaveRequest();
                    BeanUtil.copyProperties(dto, updateRequest);
                    updateConfig(existing.getId(), updateRequest, operatorId);
                }
            } else {
                // 新建
                ConfigSaveRequest saveRequest = new ConfigSaveRequest();
                BeanUtil.copyProperties(dto, saveRequest);
                saveConfig(saveRequest, operatorId);
            }
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemConfigDTO rollbackToVersion(Long configId, Integer version, Long operatorId) {
        // 获取指定版本
        com.aiready.config.entity.ConfigVersion ver = versionService.getById(version);
        if (ver == null || !ver.getConfigId().equals(configId)) {
            throw new RuntimeException("版本不存在");
        }
        
        // 更新配置值
        return updateConfigValue(configId, ver.getConfigValue(), "回滚到版本: " + version, operatorId);
    }
    
    private SystemConfigDTO convertToDTO(SystemConfig config) {
        SystemConfigDTO dto = new SystemConfigDTO();
        BeanUtil.copyProperties(config, dto);
        
        // 敏感配置脱敏显示
        if (config.getSensitive() != null && config.getSensitive() == 1) {
            dto.setConfigValue("******");
        }
        
        // 数据类型名称
        String[] typeNames = {"未知", "字符串", "整数", "浮点数", "布尔值", "JSON"};
        if (config.getDataType() != null && config.getDataType() >= 1 && config.getDataType() <= 5) {
            dto.setDataTypeName(typeNames[config.getDataType()]);
        }
        
        return dto;
    }
    
    private String encrypt(String value) {
        // 简单加密，实际应使用更安全的加密方式
        return "ENC(" + value + ")";
    }
    
    private String decrypt(String value) {
        if (value != null && value.startsWith("ENC(") && value.endsWith(")")) {
            return value.substring(4, value.length() - 1);
        }
        return value;
    }
    
    // 用于SpEL表达式获取configKey
    public String getConfigKeyById(Long configId) {
        SystemConfig config = configMapper.selectById(configId);
        return config != null ? config.getConfigKey() : null;
    }
}
