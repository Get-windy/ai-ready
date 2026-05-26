package com.aiready.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.aiready.config.dto.ConfigVersionDTO;
import com.aiready.config.entity.ConfigVersion;
import com.aiready.config.mapper.ConfigVersionMapper;
import com.aiready.config.service.ConfigVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 配置版本服务实现
 */
@Service
@RequiredArgsConstructor
public class ConfigVersionServiceImpl extends ServiceImpl<ConfigVersionMapper, ConfigVersion> 
        implements ConfigVersionService {
    
    private final ConfigVersionMapper versionMapper;
    
    @Override
    public void createVersion(Long configId, String configKey, String oldValue, String newValue, 
                              String changeType, String changeReason, Long operatorId) {
        Integer maxVersion = versionMapper.getMaxVersion(configId);
        
        ConfigVersion version = new ConfigVersion();
        version.setConfigId(configId);
        version.setConfigKey(configKey);
        version.setVersion(maxVersion + 1);
        version.setConfigValue(newValue);
        version.setChangeType(changeType);
        version.setChangeReason(changeReason);
        version.setOldValue(oldValue);
        version.setNewValue(newValue);
        version.setCreateBy(operatorId);
        version.setCreateTime(LocalDateTime.now());
        
        versionMapper.insert(version);
    }
    
    @Override
    public List<ConfigVersionDTO> getVersionsByConfigId(Long configId) {
        List<ConfigVersion> versions = versionMapper.selectByConfigId(configId);
        return versions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    public ConfigVersionDTO getLatestVersion(Long configId) {
        ConfigVersion version = versionMapper.selectLatestVersion(configId);
        return version != null ? convertToDTO(version) : null;
    }
    
    @Override
    public ConfigVersionDTO getVersion(Long configId, Integer version) {
        ConfigVersion ver = versionMapper.selectByVersion(configId, version);
        return ver != null ? convertToDTO(ver) : null;
    }
    
    @Override
    public String compareVersions(Long configId, Integer version1, Integer version2) {
        ConfigVersion v1 = versionMapper.selectByVersion(configId, version1);
        ConfigVersion v2 = versionMapper.selectByVersion(configId, version2);
        
        if (v1 == null || v2 == null) {
            return "版本不存在";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("版本 ").append(version1).append(" -> ").append(version2).append("\n");
        result.append("旧值: ").append(v1.getConfigValue()).append("\n");
        result.append("新值: ").append(v2.getConfigValue()).append("\n");
        
        return result.toString();
    }
    
    private ConfigVersionDTO convertToDTO(ConfigVersion version) {
        ConfigVersionDTO dto = new ConfigVersionDTO();
        BeanUtil.copyProperties(version, dto);
        
        // 变更类型名称
        String[] typeNames = {"", "创建", "更新", "删除"};
        if ("CREATE".equals(version.getChangeType())) {
            dto.setChangeTypeName("创建");
        } else if ("UPDATE".equals(version.getChangeType())) {
            dto.setChangeTypeName("更新");
        } else if ("DELETE".equals(version.getChangeType())) {
            dto.setChangeTypeName("删除");
        }
        
        return dto;
    }
}
