package cn.aiedge.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流实例实体（core-api 工作流引擎持久层）
 * 对应表: workflow_instance（V5.0.0 列集）
 * 说明: form_data 为 V9.1.0 列，当前仅用于保存业务数据JSON（详情页展示/条件路由上下文），
 * 不依赖其余 V9.1.0 死列。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("workflow_instance")
public class WorkflowInstanceEntity {

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
     * 业务单据ID（数值型；非数值业务键仅存于 form_data）
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
     * 当前节点行ID（workflow_node.id，可经 node_code 还原模型层节点ID）
     */
    private Long currentNodeId;

    /**
     * 当前节点名称
     */
    private String currentNodeName;

    /**
     * 状态（0-审批中 1-已通过 2-已驳回 3-已撤回 4-已取消 5-已挂起 6-已终止）
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
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 业务数据JSON（V9.1.0 列，当前唯一使用的 V9 列）
     */
    private String formData;

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
     * 创建时间（即申请时间）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
