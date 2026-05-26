package cn.aiedge.feedback.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户反馈实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "用户反馈")
public class Feedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 反馈ID
     */
    @Schema(description = "反馈ID")
    private String feedbackId;

    /**
     * 反馈标题
     */
    @Schema(description = "反馈标题")
    private String title;

    /**
     * 反馈内容
     */
    @Schema(description = "反馈内容")
    private String content;

    /**
     * 反馈类型: bug/feature/improvement/complaint/other
     */
    @Schema(description = "反馈类型")
    private String type;

    /**
     * 反馈分类: ui/performance/function/security/experience
     */
    @Schema(description = "反馈分类")
    private String category;

    /**
     * 优先级: urgent/high/medium/low
     */
    @Schema(description = "优先级")
    private String priority;

    /**
     * 状态: pending/review/assigned/processing/resolved/closed/rejected
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 来源渠道: feishu/email/form/api
     */
    @Schema(description = "来源渠道")
    private String source;

    /**
     * 提交人ID
     */
    @Schema(description = "提交人ID")
    private Long submitterId;

    /**
     * 提交人名称
     */
    @Schema(description = "提交人名称")
    private String submitterName;

    /**
     * 提交人联系方式
     */
    @Schema(description = "提交人联系方式")
    private String contactInfo;

    /**
     * 提交人邮箱
     */
    @Schema(description = "提交人邮箱")
    private String email;

    /**
     * 关联用户ID
     */
    @Schema(description = "关联用户ID")
    private Long userId;

    /**
     * 关联租户ID
     */
    @Schema(description = "关联租户ID")
    private Long tenantId;

    /**
     * 关联产品模块
     */
    @Schema(description = "关联产品模块")
    private String module;

    /**
     * 关联版本号
     */
    @Schema(description = "关联版本号")
    private String version;

    /**
     * 截图/附件URL列表
     */
    @Schema(description = "附件URL列表")
    private List<String> attachments;

    /**
     * 处理人ID
     */
    @Schema(description = "处理人ID")
    private Long handlerId;

    /**
     * 处理人名称
     */
    @Schema(description = "处理人名称")
    private String handlerName;

    /**
     * 解决方案
     */
    @Schema(description = "解决方案")
    private String solution;

    /**
     * 回复内容
     */
    @Schema(description = "回复内容")
    private String reply;

    /**
     * 满意度评分: 1-5
     */
    @Schema(description = "满意度评分")
    private Integer satisfaction;

    /**
     * 满意度评价内容
     */
    @Schema(description = "满意度评价内容")
    private String satisfactionComment;

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
     * 处理时间
     */
    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    /**
     * 关闭时间
     */
    @Schema(description = "关闭时间")
    private LocalDateTime closeTime;

    /**
     * 标签列表
     */
    @Schema(description = "标签列表")
    private List<String> tags;

    /**
     * 浏览器信息
     */
    @Schema(description = "浏览器信息")
    private String browserInfo;

    /**
     * 操作系统信息
     */
    @Schema(description = "操作系统信息")
    private String osInfo;

    /**
     * 页面URL
     */
    @Schema(description = "页面URL")
    private String pageUrl;

    /**
     * IP地址
     */
    @Schema(description = "IP地址")
    private String ipAddress;

    // Getters and Setters
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getSubmitterId() { return submitterId; }
    public void setSubmitterId(Long submitterId) { this.submitterId = submitterId; }

    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String submitterName) { this.submitterName = submitterName; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }

    public Long getHandlerId() { return handlerId; }
    public void setHandlerId(Long handlerId) { this.handlerId = handlerId; }

    public String getHandlerName() { return handlerName; }
    public void setHandlerName(String handlerName) { this.handlerName = handlerName; }

    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public Integer getSatisfaction() { return satisfaction; }
    public void setSatisfaction(Integer satisfaction) { this.satisfaction = satisfaction; }

    public String getSatisfactionComment() { return satisfactionComment; }
    public void setSatisfactionComment(String satisfactionComment) { this.satisfactionComment = satisfactionComment; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime handleTime) { this.handleTime = handleTime; }

    public LocalDateTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalDateTime closeTime) { this.closeTime = closeTime; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }

    public String getOsInfo() { return osInfo; }
    public void setOsInfo(String osInfo) { this.osInfo = osInfo; }

    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
