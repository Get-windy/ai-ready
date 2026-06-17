package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.model.SmsConfig;
import cn.aiedge.platform.service.SmsConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信配置服务实现（内存模式）
 */
@Slf4j
@Service
public class SmsConfigServiceImpl implements SmsConfigService {

    private final Map<Long, SmsConfig> configStore = new ConcurrentHashMap<>();

    @Override
    public SmsConfig getConfig(Long tenantId) {
        SmsConfig config = configStore.get(tenantId);
        if (config == null) {
            config = createDefaultConfig(tenantId);
            configStore.put(tenantId, config);
        }
        return config;
    }

    @Override
    public SmsConfig saveConfig(SmsConfig config, Long tenantId) {
        SmsConfig existing = configStore.get(tenantId);
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
        log.info("保存短信配置: tenantId={}, provider={}", tenantId, config.getProvider());
        return config;
    }

    @Override
    public boolean testConnection(SmsConfig config) {
        log.info("测试短信服务: provider={}, signName={}",
                config.getProvider(), config.getSignName());
        // 模拟短信服务测试成功
        return true;
    }

    private SmsConfig createDefaultConfig(Long tenantId) {
        SmsConfig config = new SmsConfig();
        config.setId(System.currentTimeMillis());
        config.setProvider("aliyun");
        config.setAccessKey("");
        config.setAccessSecret("");
        config.setSignName("");
        config.setEnabled(false);
        config.setTenantId(tenantId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return config;
    }
}
