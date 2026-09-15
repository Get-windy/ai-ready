package cn.aiedge.trade.service;

import cn.aiedge.common.result.PageResult;
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
     * 分页查询渠道配置（管理端列表：渠道编码/渠道类型/同步开关/启用状态 + 关键字）
     *
     * <p>原 `listEnabled` 一次返回全部启用渠道、无法带条件，管理端列表改为分页查询。</p>
     *
     * @param keyword     关键字（渠道编码 / 渠道名称，模糊）
     * @param channelType 渠道类型：ECOMMERCE/SOCIAL/SELF/ERP
     * @param syncEnabled 同步开关：1 开 / 0 关
     * @param status      启用状态：1 正常 / 0 禁用
     */
    PageResult<ExternalChannelConfig> pageChannels(Integer pageNum, Integer pageSize, String keyword,
                                                   String channelType, Integer syncEnabled, Integer status);

    /**
     * 渠道台账统计（真实聚合 SQL，非当前页口径）
     *
     * <p>返回：total 渠道总数 / enabledCount 启用数 / syncEnabledCount 同步开启数 / abnormalCount 异常数
     * （禁用或令牌已过期）；口径见 {@link cn.aiedge.trade.mapper.ExternalChannelConfigMapper#statChannels()}。</p>
     */
    Map<String, Object> statChannels();

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

    /**
     * 同步渠道数据（从外部平台拉取订单/商品等）
     */
    Map<String, Object> syncChannelData(Long id);
}