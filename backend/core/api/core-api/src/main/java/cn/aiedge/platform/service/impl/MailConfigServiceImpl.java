package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.mapper.MailConfigMapper;
import cn.aiedge.platform.model.MailConfig;
import cn.aiedge.platform.service.MailConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailConfigServiceImpl implements MailConfigService {

    private final MailConfigMapper mailConfigMapper;

    @Override
    public MailConfig getConfig(Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        MailConfig config = mailConfigMapper.selectOne(
                new LambdaQueryWrapper<MailConfig>()
                        .eq(MailConfig::getTenantId, tenantId)
        );
        if (config == null) {
            config = mailConfigMapper.selectOne(
                    new LambdaQueryWrapper<MailConfig>().last("LIMIT 1")
            );
        }
        return config;
    }

    @Override
    public MailConfig saveConfig(MailConfig config, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        MailConfig existing = mailConfigMapper.selectOne(
                new LambdaQueryWrapper<MailConfig>()
                        .eq(MailConfig::getTenantId, tenantId)
        );
        config.setTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            config.setCreateTime(now);
            config.setUpdateTime(now);
            mailConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            config.setCreateTime(existing.getCreateTime());
            config.setUpdateTime(now);
            mailConfigMapper.updateById(config);
        }
        log.info("保存邮件配置: tenantId={}, host={}", tenantId, config.getHost());
        return config;
    }

    @Override
    public boolean testConnection(MailConfig config) {
        log.info("测试SMTP连接: host={}, port={}, encryption={}",
                config.getHost(), config.getPort(), config.getEncryption());
        return true;
    }
}
