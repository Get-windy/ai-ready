package cn.aiedge.trade.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.entity.ExternalChannelConfig;
import cn.aiedge.trade.mapper.ExternalChannelConfigMapper;
import cn.aiedge.trade.service.ChannelConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    public PageResult<ExternalChannelConfig> pageChannels(Integer pageNum, Integer pageSize, String keyword,
                                                          String channelType, Integer syncEnabled, Integer status) {
        LambdaQueryWrapper<ExternalChannelConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ExternalChannelConfig::getChannelCode, kw)
                    .or().like(ExternalChannelConfig::getChannelName, kw));
        }
        wrapper.eq(StringUtils.hasText(channelType), ExternalChannelConfig::getChannelType, channelType);
        wrapper.eq(syncEnabled != null, ExternalChannelConfig::getSyncEnabled, syncEnabled);
        wrapper.eq(status != null, ExternalChannelConfig::getStatus, status);
        // 渠道编码升序（与 CHANNEL_CODE_MAP 枚举顺序一致），末尾用 id 兜底保证分页稳定
        wrapper.orderByAsc(ExternalChannelConfig::getChannelCode).orderByAsc(ExternalChannelConfig::getId);

        Page<ExternalChannelConfig> page = mapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(),
                pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
    }

    @Override
    public Map<String, Object> statChannels() {
        // 单条聚合 SQL（COUNT + FILTER），tenant_id 由租户插件注入；无行时返回全 0，不回退当前页口径
        Map<String, Object> row = mapper.statChannels();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", longOf(row, "total"));
        out.put("enabledCount", longOf(row, "enabledCount"));
        out.put("syncEnabledCount", longOf(row, "syncEnabledCount"));
        out.put("abnormalCount", longOf(row, "abnormalCount"));
        return out;
    }

    /** 聚合列取值：PostgreSQL COUNT 返回 bigint，统一转为 long，缺失记 0 */
    private static long longOf(Map<String, Object> row, String key) {
        if (row == null) {
            return 0L;
        }
        Object value = row.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
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
