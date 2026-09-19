package cn.aiedge.platform.service;

import cn.aiedge.platform.dto.ConnectionTestResult;
import cn.aiedge.platform.model.StorageConfig;

/**
 * 存储配置服务接口
 */
public interface StorageConfigService {

    /**
     * 获取存储配置
     */
    StorageConfig getConfig(Long tenantId);

    /**
     * 保存存储配置
     */
    StorageConfig saveConfig(StorageConfig config, Long tenantId);

    /**
     * 测试存储连接
     */
    ConnectionTestResult testConnection(StorageConfig config);
}
