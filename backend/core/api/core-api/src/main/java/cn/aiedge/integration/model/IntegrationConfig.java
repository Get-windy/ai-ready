package cn.aiedge.integration.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 集成配置实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "集成配置")
public class IntegrationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @Schema(description = "配置ID")
    private String configId;

    /**
     * 集成名称
     */
    @Schema(description = "集成名称")
    private String name;

    /**
     * 系统编码
     */
    @Schema(description = "系统编码")
    private String systemCode;

    /**
     * 系统名称
     */
    @Schema(description = "系统名称")
    private String systemName;

    /**
     * 集成类型: rest/webhook/websocket
     */
    @Schema(description = "集成类型")
    private String type;

    /**
     * 状态: enabled/disabled
     */
    @Schema(description = "状态")
    private String status;

    /**
     * API基础URL
     */
    @Schema(description = "API基础URL")
    private String baseUrl;

    /**
     * API密钥
     */
    @Schema(description = "API密钥")
    private String apiKey;

    /**
     * API密钥Secret
     */
    @Schema(description = "API密钥Secret")
    private String apiSecret;

    /**
     * 访问令牌
     */
    @Schema(description = "访问令牌")
    private String accessToken;

    /**
     * 刷新令牌
     */
    @Schema(description = "刷新令牌")
    private String refreshToken;

    /**
     * 令牌过期时间
     */
    @Schema(description = "令牌过期时间")
    private LocalDateTime tokenExpireTime;

    /**
     * WebHook URL
     */
    @Schema(description = "WebHook URL")
    private String webhookUrl;

    /**
     * WebHook密钥
     */
    @Schema(description = "WebHook密钥")
    private String webhookSecret;

    /**
     * 限流配置: 每秒请求数
     */
    @Schema(description = "限流配置(QPS)")
    private Integer rateLimit;

    /**
     * 每日请求上限
     */
    @Schema(description = "每日请求上限")
    private Integer dailyLimit;

    /**
     * 超时时间(秒)
     */
    @Schema(description = "超时时间(秒)")
    private Integer timeout;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /**
     * 重试间隔(秒)
     */
    @Schema(description = "重试间隔(秒)")
    private Integer retryInterval;

    /**
     * 允许的数据类型
     */
    @Schema(description = "允许的数据类型")
    private List<String> allowedDataTypes;

    /**
     * 允许的操作
     */
    @Schema(description = "允许的操作")
    private List<String> allowedOperations;

    /**
     * IP白名单
     */
    @Schema(description = "IP白名单")
    private List<String> ipWhitelist;

    /**
     * 回调URL
     */
    @Schema(description = "回调URL")
    private String callbackUrl;

    /**
     * 加密方式: none/aes/rsa
     */
    @Schema(description = "加密方式")
    private String encryption;

    /**
     * 加密密钥
     */
    @Schema(description = "加密密钥")
    private String encryptionKey;

    /**
     * 签名方式: none/md5/sha256/hmac
     */
    @Schema(description = "签名方式")
    private String signMethod;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 最后同步时间
     */
    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    // Getters and Setters
    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public LocalDateTime getTokenExpireTime() { return tokenExpireTime; }
    public void setTokenExpireTime(LocalDateTime tokenExpireTime) { this.tokenExpireTime = tokenExpireTime; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public Integer getRateLimit() { return rateLimit; }
    public void setRateLimit(Integer rateLimit) { this.rateLimit = rateLimit; }

    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }

    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getRetryInterval() { return retryInterval; }
    public void setRetryInterval(Integer retryInterval) { this.retryInterval = retryInterval; }

    public List<String> getAllowedDataTypes() { return allowedDataTypes; }
    public void setAllowedDataTypes(List<String> allowedDataTypes) { this.allowedDataTypes = allowedDataTypes; }

    public List<String> getAllowedOperations() { return allowedOperations; }
    public void setAllowedOperations(List<String> allowedOperations) { this.allowedOperations = allowedOperations; }

    public List<String> getIpWhitelist() { return ipWhitelist; }
    public void setIpWhitelist(List<String> ipWhitelist) { this.ipWhitelist = ipWhitelist; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    public String getEncryption() { return encryption; }
    public void setEncryption(String encryption) { this.encryption = encryption; }

    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }

    public String getSignMethod() { return signMethod; }
    public void setSignMethod(String signMethod) { this.signMethod = signMethod; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
}
