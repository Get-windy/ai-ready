package cn.aiedge.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流定义实体（core-api 工作流引擎持久层）
 * 对应表: workflow_definition（V5.0.0 列集）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("workflow_definition")
public class WorkflowDefinitionEntity {

    /**
     * 流程定义ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 流程编码（唯一标识，内置流程如 order_approval）
     */
    private String processCode;

    /**
     * 流程名称
     */
    private String processName;

    /**
     * 流程类型（1-请假 2-报销 3-采购 4-销售/订单 5-财务/费用 6-其他/自定义）
     */
    private Integer processType;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 流程配置JSON（节点列表的完整序列化，读取时以此为准）
     */
    private String processConfig;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 是否默认版本（0-否 1-是）
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
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
