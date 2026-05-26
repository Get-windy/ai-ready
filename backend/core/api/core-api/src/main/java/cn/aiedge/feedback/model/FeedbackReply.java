package cn.aiedge.feedback.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 反馈回复实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "反馈回复")
public class FeedbackReply implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回复ID
     */
    @Schema(description = "回复ID")
    private String replyId;

    /**
     * 反馈ID
     */
    @Schema(description = "反馈ID")
    private String feedbackId;

    /**
     * 回复内容
     */
    @Schema(description = "回复内容")
    private String content;

    /**
     * 回复人类型: user/system
     */
    @Schema(description = "回复人类型")
    private String replierType;

    /**
     * 回复人ID
     */
    @Schema(description = "回复人ID")
    private Long replierId;

    /**
     * 回复人名称
     */
    @Schema(description = "回复人名称")
    private String replierName;

    /**
     * 是否内部回复
     */
    @Schema(description = "是否内部回复")
    private Boolean internal;

    /**
     * 附件URL
     */
    @Schema(description = "附件URL")
    private String attachmentUrl;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // Getters and Setters
    public String getReplyId() { return replyId; }
    public void setReplyId(String replyId) { this.replyId = replyId; }

    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReplierType() { return replierType; }
    public void setReplierType(String replierType) { this.replierType = replierType; }

    public Long getReplierId() { return replierId; }
    public void setReplierId(Long replierId) { this.replierId = replierId; }

    public String getReplierName() { return replierName; }
    public void setReplierName(String replierName) { this.replierName = replierName; }

    public Boolean getInternal() { return internal; }
    public void setInternal(Boolean internal) { this.internal = internal; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
