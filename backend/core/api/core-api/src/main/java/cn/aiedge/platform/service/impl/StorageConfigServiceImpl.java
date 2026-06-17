package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.model.StorageConfig;
import cn.aiedge.platform.service.StorageConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储配置服务实现（内存模式）
 */
@Slf4j
@Service
public class StorageConfigServiceImpl implements StorageConfigService {

    private final Map<Long, StorageConfig> configStore = new ConcurrentHashMap<>();

    @Override
    public StorageConfig getConfig(Long tenantId) {
        StorageConfig config = configStore.get(tenantId);
        if (config == null) {
            config = createDefaultConfig(tenantId);
            configStore.put(tenantId, config);
        }
        return config;
    }

    @Override
    public StorageConfig saveConfig(StorageConfig config, Long tenantId) {
        StorageConfig existing = configStore.get(tenantId);
        if (existing == null) {
            config.setId(System.currentTimeMillis());
            config.setCreateTime(LocalDateTime.now());
        } else {
            config.setId(existing.getId());
            config.setCreateTime(existing.getCreateTime());
        }
        config.setTenantId(tenantId);
        config.setUpdateTime(LocalDateTime.now());
        configStore.put(tenantId, config);
        log.info("保存存储配置: tenantId={}, type={}", tenantId, config.getStorageType());
        return config;
    }

    @Override
    public boolean testConnection(StorageConfig config) {
        log.info("测试存储连接: type={}, endpoint={}, bucket={}",
                config.getStorageType(), config.getEndpoint(), config.getBucket());
        // 模拟存储连接测试成功
        return true;
    }

    private StorageConfig createDefaultConfig(Long tenantId) {
        StorageConfig config = new StorageConfig();
        config.setId(System.currentTimeMillis());
        config.setStorageType("local");
        config.setLocalPath("./upload");
        config.setLocalUrlPrefix("/uploads");
        config.setEndpoint("");
        config.setBucket("");
        config.setAccessKey("");
        config.setAccessSecret("");
        config.setEnabled(false);
        config.setTenantId(tenantId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return config;
    }
}
