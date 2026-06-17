package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.model.MailConfig;
import cn.aiedge.platform.service.MailConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件配置服务实现（内存模式）
 */
@Slf4j
@Service
public class MailConfigServiceImpl implements MailConfigService {

    private final Map<Long, MailConfig> configStore = new ConcurrentHashMap<>();

    @Override
    public MailConfig getConfig(Long tenantId) {
        MailConfig config = configStore.get(tenantId);
        if (config == null) {
            config = createDefaultConfig(tenantId);
            configStore.put(tenantId, config);
        }
        return config;
    }

    @Override
    public MailConfig saveConfig(MailConfig config, Long tenantId) {
        MailConfig existing = configStore.get(tenantId);
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
        log.info("保存邮件配置: tenantId={}, host={}", tenantId, config.getHost());
        return config;
    }

    @Override
    public boolean testConnection(MailConfig config) {
        log.info("测试SMTP连接: host={}, port={}, encryption={}",
                config.getHost(), config.getPort(), config.getEncryption());
        // 模拟SMTP连接测试成功
        return true;
    }

    private MailConfig createDefaultConfig(Long tenantId) {
        MailConfig config = new MailConfig();
        config.setId(System.currentTimeMillis());
        config.setHost("smtp.example.com");
        config.setPort(465);
        config.setEncryption("SSL");
        config.setUsername("");
        config.setPassword("");
        config.setFromAddress("");
        config.setEnabled(false);
        config.setTenantId(tenantId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return config;
    }
}
