package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流实例实体
 * 审批流程运行实例
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("workflow_instance")
public class WorkflowInstance {

    /**
     * 实例ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 流程定义ID
     */
    private Long definitionId;

    /**
     * 业务单据ID
     */
    private Long businessId;

    /**
     * 业务单据类型
     */
    private String businessType;

    /**
     * 流程标题
     */
    private String title;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人名称
     */
    private String applicantName;

    /**
     * 当前节点ID
     */
    private Long currentNodeId;

    /**
     * 当前节点名称
     */
    private String currentNodeName;

    /**
     * 状态（0-运行中 1-已完成 2-已驳回 3-已撤回）
     */
    private Integer status;

    /**
     * 审批结果（0-通过 1-驳回）
     */
    private Integer result;

    /**
     * 审批意见
     */
    private String comment;

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
     * 完成时间
     */
    private LocalDateTime finishTime;
}
