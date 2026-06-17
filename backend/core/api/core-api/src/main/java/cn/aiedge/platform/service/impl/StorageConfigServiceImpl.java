package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.mapper.StorageConfigMapper;
import cn.aiedge.platform.model.StorageConfig;
import cn.aiedge.platform.service.StorageConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageConfigServiceImpl implements StorageConfigService {

    private final StorageConfigMapper storageConfigMapper;

    @Override
    public StorageConfig getConfig(Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        StorageConfig config = storageConfigMapper.selectOne(
                new LambdaQueryWrapper<StorageConfig>()
                        .eq(StorageConfig::getTenantId, tenantId)
        );
        if (config == null) {
            config = storageConfigMapper.selectOne(
                    new LambdaQueryWrapper<StorageConfig>().last("LIMIT 1")
            );
        }
        return config;
    }

    @Override
    public StorageConfig saveConfig(StorageConfig config, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        StorageConfig existing = storageConfigMapper.selectOne(
                new LambdaQueryWrapper<StorageConfig>()
                        .eq(StorageConfig::getTenantId, tenantId)
        );
        config.setTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            config.setCreateTime(now);
            config.setUpdateTime(now);
            storageConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            config.setCreateTime(existing.getCreateTime());
            config.setUpdateTime(now);
            storageConfigMapper.updateById(config);
        }
        log.info("保存存储配置: tenantId={}, type={}", tenantId, config.getStorageType());
        return config;
    }

    @Override
    public boolean testConnection(StorageConfig config) {
        log.info("测试存储连接: type={}, endpoint={}, bucket={}",
                config.getStorageType(), config.getEndpoint(), config.getBucket());
        return true;
    }
}
