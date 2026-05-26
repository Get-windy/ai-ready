package cn.aiedge.feedback.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 反馈分类实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "反馈分类")
public class FeedbackCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private String categoryId;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String name;

    /**
     * 分类编码
     */
    @Schema(description = "分类编码")
    private String code;

    /**
     * 父分类ID
     */
    @Schema(description = "父分类ID")
    private String parentId;

    /**
     * 分类描述
     */
    @Schema(description = "分类描述")
    private String description;

    /**
     * 默认处理人ID
     */
    @Schema(description = "默认处理人ID")
    private Long defaultHandlerId;

    /**
     * 默认优先级
     */
    @Schema(description = "默认优先级")
    private String defaultPriority;

    /**
     * 排序号
     */
    @Schema(description = "排序号")
    private Integer sortOrder;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private Boolean enabled;

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

    // Getters and Setters
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getDefaultHandlerId() { return defaultHandlerId; }
    public void setDefaultHandlerId(Long defaultHandlerId) { this.defaultHandlerId = defaultHandlerId; }

    public String getDefaultPriority() { return defaultPriority; }
    public void setDefaultPriority(String defaultPriority) { this.defaultPriority = defaultPriority; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
