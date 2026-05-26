package cn.aiedge.base.service;

import cn.aiedge.base.entity.WorkflowDefinition;
import cn.aiedge.base.entity.WorkflowInstance;
import cn.aiedge.base.entity.WorkflowTask;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 工作流服务接口
 * 审批流程管理
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface WorkflowService {

    /**
     * 创建工作流定义
     */
    Long createDefinition(WorkflowDefinition definition);

    /**
     * 更新工作流定义
     */
    void updateDefinition(WorkflowDefinition definition);

    /**
     * 发布工作流定义
     */
    void publishDefinition(Long definitionId);

    /**
     * 停用工作流定义
     */
    void disableDefinition(Long definitionId);

    /**
     * 获取工作流定义
     */
    WorkflowDefinition getDefinition(Long definitionId);

    /**
     * 根据流程编码获取默认流程定义
     */
    WorkflowDefinition getDefaultDefinition(String processCode, Long tenantId);

    /**
     * 分页查询工作流定义
     */
    Page<WorkflowDefinition> pageDefinitions(Page<WorkflowDefinition> page, 
                                               Long tenantId, 
                                               String processName, 
                                               Integer status);

    /**
     * 启动流程实例
     */
    Long startInstance(Long definitionId, Long businessId, String businessType, 
                       String title, Long applicantId, String applicantName);

    /**
     * 审批通过
     */
    void approve(Long taskId, Long userId, String comment);

    /**
     * 审批驳回
     */
    void reject(Long taskId, Long userId, String comment);

    /**
     * 转交任务
     */
    void transfer(Long taskId, Long fromUserId, Long toUserId, String comment);

    /**
     * 撤回流程
     */
    void withdraw(Long instanceId, Long userId);

    /**
     * 获取流程实例
     */
    WorkflowInstance getInstance(Long instanceId);

    /**
     * 获取待办任务列表
     */
    List<WorkflowTask> getTodoTasks(Long userId, Long tenantId);

    /**
     * 获取已办任务列表
     */
    List<WorkflowTask> getDoneTasks(Long userId, Long tenantId);

    /**
     * 获取流程审批历史
     */
    List<WorkflowTask> getInstanceHistory(Long instanceId);
}
