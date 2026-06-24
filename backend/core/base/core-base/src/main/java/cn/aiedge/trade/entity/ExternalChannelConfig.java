package cn.aiedge.trade.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 外部平台渠道配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("external_channel_config")
public class ExternalChannelConfig extends BaseEntity {

    /** 渠道编码 */
    private String channelCode;

    /** 渠道名称 */
    private String channelName;

    /** 渠道类型: ECOMMERCE, SOCIAL, SELF, ERP */
    private String channelType;

    /** API端点 */
    private String apiEndpoint;

    /** 应用ID */
    private String appId;

    /** 应用密钥(加密存储) */
    private String appSecret;

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** Token过期时间 */
    private LocalDateTime tokenExpireTime;

    /** 同步启用 */
    private Integer syncEnabled;

    /** 同步间隔(分钟) */
    private Integer syncInterval;

    /** 最后同步时间 */
    private LocalDateTime lastSyncTime;

    /** 平台特定配置(JSON) */
    private String configJson;

    /** 状态 */
    private Integer status;
}