package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流定义实体
 * 审批流程模板定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("workflow_definition")
public class WorkflowDefinition {

    /**
     * 流程定义ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 流程编码（唯一）
     */
    private String processCode;

    /**
     * 流程名称
     */
    private String processName;

    /**
     * 流程类型（1-请假 2-报销 3-采购 4-其他）
     */
    private Integer processType;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 流程配置JSON
     */
    private String processConfig;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 是否默认版本
     */
    private Integer isDefault;

    /**
     * 状态（0-草稿 1-已发布 2-已停用）
     */
    private Integer status;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
