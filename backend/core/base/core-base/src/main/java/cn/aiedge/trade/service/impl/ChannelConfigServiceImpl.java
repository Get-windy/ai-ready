package cn.aiedge.trade.service.impl;

import cn.aiedge.trade.entity.ExternalChannelConfig;
import cn.aiedge.trade.mapper.ExternalChannelConfigMapper;
import cn.aiedge.trade.service.ChannelConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 渠道配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelConfigServiceImpl implements ChannelConfigService {

    private final ExternalChannelConfigMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExternalChannelConfig create(ExternalChannelConfig config) {
        config.setStatus(1);
        config.setSyncEnabled(0);
        mapper.insert(config);
        log.info("创建渠道配置: id={}, code={}", config.getId(), config.getChannelCode());
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExternalChannelConfig update(Long id, ExternalChannelConfig config) {
        ExternalChannelConfig existing = mapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("渠道配置不存在: " + id);
        }
        config.setId(id);
        mapper.updateById(config);
        log.info("更新渠道配置: id={}", id);
        return mapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        mapper.deleteById(id);
        log.info("删除渠道配置: id={}", id);
    }

    @Override
    public ExternalChannelConfig get(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public ExternalChannelConfig getByCode(String channelCode) {
        LambdaQueryWrapper<ExternalChannelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExternalChannelConfig::getChannelCode, channelCode);
        return mapper.selectOne(wrapper);
    }

    @Override
    public List<ExternalChannelConfig> listEnabled() {
        LambdaQueryWrapper<ExternalChannelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExternalChannelConfig::getStatus, 1);
        return mapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, boolean enabled) {
        ExternalChannelConfig config = mapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("渠道配置不存在: " + id);
        }
        config.setStatus(enabled ? 1 : 0);
        mapper.updateById(config);
        log.info("切换渠道状态: id={}, enabled={}", id, enabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateToken(Long id, String accessToken, String refreshToken, Long expireTime) {
        ExternalChannelConfig config = mapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("渠道配置不存在: " + id);
        }
        config.setAccessToken(accessToken);
        config.setRefreshToken(refreshToken);
        if (expireTime != null) {
            config.setTokenExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        }
        mapper.updateById(config);
        log.info("更新渠道Token: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSyncTime(Long id) {
        ExternalChannelConfig config = mapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("渠道配置不存在: " + id);
        }
        config.setLastSyncTime(LocalDateTime.now());
        mapper.updateById(config);
    }

    @Override
    public boolean initializeChannel(Long id) {
        ExternalChannelConfig config = mapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("渠道配置不存在: " + id);
        }
        // 初始化渠道连接（验证API凭证等）
        log.info("初始化渠道连接: id={}, code={}", id, config.getChannelCode());
        // TODO: 实际调用外部API验证连接
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncChannelData(Long id) {
        ExternalChannelConfig config = mapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("渠道配置不存在: " + id);
        }
        if (config.getStatus() != 1) {
            throw new RuntimeException("渠道未启用，无法同步");
        }

        log.info("开始同步渠道数据: id={}, code={}", id, config.getChannelCode());

        // 模拟同步结果（实际应调用外部API拉取数据）
        int syncedOrders = 0;
        int syncedProducts = 0;

        // TODO: 根据渠道类型调用对应的外部API
        // 根据 channelType (ECOMMERCE/SOCIAL/SELF/ERP) 使用不同的适配器
        // 拉取订单、商品等数据并存入本地数据库

        // 更新同步时间
        updateSyncTime(id);

        Map<String, Object> result = new HashMap<>();
        result.put("channelId", id);
        result.put("channelCode", config.getChannelCode());
        result.put("syncTime", LocalDateTime.now());
        result.put("syncedOrders", syncedOrders);
        result.put("syncedProducts", syncedProducts);
        result.put("message", "同步完成");

        log.info("渠道数据同步完成: id={}, orders={}, products={}", id, syncedOrders, syncedProducts);
        return result;
    }
}
