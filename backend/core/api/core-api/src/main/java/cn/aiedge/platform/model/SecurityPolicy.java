package cn.aiedge.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 安全策略配置
 */
@Schema(description = "安全策略配置")
@TableName("sys_security_policy")
public class SecurityPolicy implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "策略ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "锁定阈值（登录失败次数）")
    private Integer lockThreshold;

    @Schema(description = "锁定持续时间（分钟）")
    private Integer lockDuration;

    @Schema(description = "是否启用验证码")
    private boolean captchaEnabled;

    @Schema(description = "是否启用双因素认证")
    private boolean twoFactorEnabled;

    @Schema(description = "密码最小长度")
    private Integer passwordMinLength;

    @Schema(description = "是否需要大写字母")
    private boolean requireUpper;

    @Schema(description = "是否需要小写字母")
    private boolean requireLower;

    @Schema(description = "是否需要数字")
    private boolean requireDigit;

    @Schema(description = "是否需要特殊字符")
    private boolean requireSpecial;

    @Schema(description = "密码过期天数（0表示永不过期）")
    private Integer passwordExpireDays;

    @Schema(description = "会话超时时间（秒）")
    private Integer sessionTimeout;

    @Schema(description = "是否单设备登录")
    private boolean singleDevice;

    @Schema(description = "IP白名单（逗号分隔）")
    private String ipWhitelist;

    @Schema(description = "是否限制请求频率")
    private boolean rateLimit;

    @Schema(description = "审计日志保留天数")
    private Integer auditRetentionDays;

    @Schema(description = "是否记录敏感操作")
    private boolean logSensitiveOps;

    @Schema(description = "是否记录登录日志")
    private boolean logLogin;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建者")
    private String createBy;

    @Schema(description = "更新者")
    private String updateBy;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getLockThreshold() { return lockThreshold; }
    public void setLockThreshold(Integer lockThreshold) { this.lockThreshold = lockThreshold; }
    public Integer getLockDuration() { return lockDuration; }
    public void setLockDuration(Integer lockDuration) { this.lockDuration = lockDuration; }
    public boolean isCaptchaEnabled() { return captchaEnabled; }
    public void setCaptchaEnabled(boolean captchaEnabled) { this.captchaEnabled = captchaEnabled; }
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
    public Integer getPasswordMinLength() { return passwordMinLength; }
    public void setPasswordMinLength(Integer passwordMinLength) { this.passwordMinLength = passwordMinLength; }
    public boolean isRequireUpper() { return requireUpper; }
    public void setRequireUpper(boolean requireUpper) { this.requireUpper = requireUpper; }
    public boolean isRequireLower() { return requireLower; }
    public void setRequireLower(boolean requireLower) { this.requireLower = requireLower; }
    public boolean isRequireDigit() { return requireDigit; }
    public void setRequireDigit(boolean requireDigit) { this.requireDigit = requireDigit; }
    public boolean isRequireSpecial() { return requireSpecial; }
    public void setRequireSpecial(boolean requireSpecial) { this.requireSpecial = requireSpecial; }
    public Integer getPasswordExpireDays() { return passwordExpireDays; }
    public void setPasswordExpireDays(Integer passwordExpireDays) { this.passwordExpireDays = passwordExpireDays; }
    public Integer getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(Integer sessionTimeout) { this.sessionTimeout = sessionTimeout; }
    public boolean isSingleDevice() { return singleDevice; }
    public void setSingleDevice(boolean singleDevice) { this.singleDevice = singleDevice; }
    public String getIpWhitelist() { return ipWhitelist; }
    public void setIpWhitelist(String ipWhitelist) { this.ipWhitelist = ipWhitelist; }
    public boolean isRateLimit() { return rateLimit; }
    public void setRateLimit(boolean rateLimit) { this.rateLimit = rateLimit; }
    public Integer getAuditRetentionDays() { return auditRetentionDays; }
    public void setAuditRetentionDays(Integer auditRetentionDays) { this.auditRetentionDays = auditRetentionDays; }
    public boolean isLogSensitiveOps() { return logSensitiveOps; }
    public void setLogSensitiveOps(boolean logSensitiveOps) { this.logSensitiveOps = logSensitiveOps; }
    public boolean isLogLogin() { return logLogin; }
    public void setLogLogin(boolean logLogin) { this.logLogin = logLogin; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
