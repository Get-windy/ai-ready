package com.aiready.config.service;

import com.aiready.config.dto.ConfigVersionDTO;
import com.aiready.config.entity.ConfigVersion;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 配置版本服务接口
 */
public interface ConfigVersionService extends IService<ConfigVersion> {
    
    /**
     * 创建版本记录
     */
    void createVersion(Long configId, String configKey, String oldValue, String newValue, 
                       String changeType, String changeReason, Long operatorId);
    
    /**
     * 获取配置的所有版本
     */
    List<ConfigVersionDTO> getVersionsByConfigId(Long configId);
    
    /**
     * 获取配置的最新版本
     */
    ConfigVersionDTO getLatestVersion(Long configId);
    
    /**
     * 获取指定版本
     */
    ConfigVersionDTO getVersion(Long configId, Integer version);
    
    /**
     * 对比两个版本
     */
    String compareVersions(Long configId, Integer version1, Integer version2);
}
