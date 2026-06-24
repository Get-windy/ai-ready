package cn.aiedge.trade.service;

import cn.aiedge.trade.entity.ExternalChannelConfig;

import java.util.List;
import java.util.Map;

/**
 * 渠道配置服务
 */
public interface ChannelConfigService {

    /**
     * 创建渠道配置
     */
    ExternalChannelConfig create(ExternalChannelConfig config);

    /**
     * 更新渠道配置
     */
    ExternalChannelConfig update(Long id, ExternalChannelConfig config);

    /**
     * 删除渠道配置
     */
    void delete(Long id);

    /**
     * 查询渠道配置
     */
    ExternalChannelConfig get(Long id);

    /**
     * 查询渠道配置（按编码）
     */
    ExternalChannelConfig getByCode(String channelCode);

    /**
     * 查询所有启用的渠道
     */
    List<ExternalChannelConfig> listEnabled();

    /**
     * 启用/禁用渠道
     */
    void toggleStatus(Long id, boolean enabled);

    /**
     * 更新Token
     */
    void updateToken(Long id, String accessToken, String refreshToken, Long expireTime);

    /**
     * 更新同步时间
     */
    void updateSyncTime(Long id);

    /**
     * 初始化渠道连接
     */
    boolean initializeChannel(Long id);
}