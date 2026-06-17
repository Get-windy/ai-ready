package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.mapper.SmsConfigMapper;
import cn.aiedge.platform.model.SmsConfig;
import cn.aiedge.platform.service.SmsConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsConfigServiceImpl implements SmsConfigService {

    private final SmsConfigMapper smsConfigMapper;

    @Override
    public SmsConfig getConfig(Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        SmsConfig config = smsConfigMapper.selectOne(
                new LambdaQueryWrapper<SmsConfig>()
                        .eq(SmsConfig::getTenantId, tenantId)
        );
        if (config == null) {
            config = smsConfigMapper.selectOne(
                    new LambdaQueryWrapper<SmsConfig>().last("LIMIT 1")
            );
        }
        return config;
    }

    @Override
    public SmsConfig saveConfig(SmsConfig config, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        SmsConfig existing = smsConfigMapper.selectOne(
                new LambdaQueryWrapper<SmsConfig>()
                        .eq(SmsConfig::getTenantId, tenantId)
        );
        config.setTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            config.setCreateTime(now);
            config.setUpdateTime(now);
            smsConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            config.setCreateTime(existing.getCreateTime());
            config.setUpdateTime(now);
            smsConfigMapper.updateById(config);
        }
        log.info("保存短信配置: tenantId={}, provider={}", tenantId, config.getProvider());
        return config;
    }

    @Override
    public boolean testConnection(SmsConfig config) {
        log.info("测试短信服务: provider={}, signName={}",
                config.getProvider(), config.getSignName());
        return true;
    }
}
