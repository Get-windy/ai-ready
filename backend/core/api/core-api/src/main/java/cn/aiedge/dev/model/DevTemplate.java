package cn.aiedge.dev.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 开发模板
 */
@Schema(description = "开发模板")
@TableName("dev_template")
public class DevTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板编码")
    private String code;

    @Schema(description = "模板类型: entity/controller/service/mapper/frontend")
    private String type;

    /**
     * 模板类别: codegen=代码生成模板(遗留种子) / import=导入模板(模板管理页 62402)
     * <p>由迁移 V11.415.0 新增；已有 7 行取 DEFAULT 'codegen'。
     * 用途：dev_template.type 一列同时承载两套值域（codegen 用 entity/controller/…，
     * import 用 user/customer/…），靠本列区分，避免 7 行代码生成种子混进 /api/import-templates。
     */
    @Schema(description = "模板类别: codegen/import")
    private String templateKind;

    @Schema(description = "模板内容")
    private String content;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "更新人")
    private String updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTemplateKind() { return templateKind; }
    public void setTemplateKind(String templateKind) { this.templateKind = templateKind; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
