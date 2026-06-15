package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.WorkflowDefinition;
import cn.aiedge.base.entity.WorkflowInstance;
import cn.aiedge.base.entity.WorkflowTask;
import cn.aiedge.base.mapper.WorkflowDefinitionMapper;
import cn.aiedge.base.mapper.WorkflowInstanceMapper;
import cn.aiedge.base.mapper.WorkflowTaskMapper;
import cn.aiedge.base.service.WorkflowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流服务实现类
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowInstanceMapper instanceMapper;
    private final WorkflowTaskMapper taskMapper;

    // ── 流程定义管理 ────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDefinition(WorkflowDefinition definition) {
        definition.setVersion(1);
        definition.setIsDefault(0);
        definition.setStatus(0);
        definition.setCreateTime(LocalDateTime.now());
        definition.setUpdateTime(LocalDateTime.now());
        definitionMapper.insert(definition);
        log.info("创建工作流定义: processCode={}, id={}", definition.getProcessCode(), definition.getId());
        return definition.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDefinition(WorkflowDefinition definition) {
        definition.setUpdateTime(LocalDateTime.now());
        definitionMapper.updateById(definition);
        log.info("更新工作流定义: definitionId={}", definition.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDefinition(Long definitionId) {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(definitionId);
        def.setStatus(1);
        def.setUpdateTime(LocalDateTime.now());
        definitionMapper.updateById(def);
        log.info("发布工作流定义: definitionId={}", definitionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableDefinition(Long definitionId) {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(definitionId);
        def.setStatus(2);
        def.setUpdateTime(LocalDateTime.now());
        definitionMapper.updateById(def);
        log.info("停用工作流定义: definitionId={}", definitionId);
    }

    @Override
    public WorkflowDefinition getDefinition(Long definitionId) {
        return definitionMapper.selectById(definitionId);
    }

    @Override
    public WorkflowDefinition getDefaultDefinition(String processCode, Long tenantId) {
        return definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinition>()
            .eq(WorkflowDefinition::getProcessCode, processCode)
            .eq(WorkflowDefinition::getTenantId, tenantId)
            .eq(WorkflowDefinition::getIsDefault, 1)
            .eq(WorkflowDefinition::getStatus, 1));
    }

    @Override
    public Page<WorkflowDefinition> pageDefinitions(Page<WorkflowDefinition> page,
                                                     Long tenantId,
                                                     String processName,
                                                     Integer status) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = new LambdaQueryWrapper<WorkflowDefinition>()
            .eq(tenantId != null, WorkflowDefinition::getTenantId, tenantId)
            .like(processName != null, WorkflowDefinition::getProcessName, processName)
            .eq(status != null, WorkflowDefinition::getStatus, status)
            .orderByDesc(WorkflowDefinition::getUpdateTime);
        return definitionMapper.selectPage(page, wrapper);
    }

    // ── 流程实例管理 ────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startInstance(Long definitionId, Long businessId, String businessType,
                              String title, Long applicantId, String applicantName) {
        WorkflowDefinition definition = definitionMapper.selectById(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("流程定义不存在: " + definitionId);
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setDefinitionId(definitionId);
        instance.setBusinessId(businessId);
        instance.setBusinessType(businessType);
        instance.setTitle(title);
        instance.setApplicantId(applicantId);
        instance.setApplicantName(applicantName);
        instance.setCurrentNodeName("提交申请");
        instance.setStatus(0);
        instance.setTenantId(definition.getTenantId());
        instance.setCreateTime(LocalDateTime.now());
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 创建首节点"提交申请"任务（设为已完成）
        WorkflowTask startTask = new WorkflowTask();
        startTask.setInstanceId(instance.getId());
        startTask.setNodeName("提交申请");
        startTask.setTaskType(1);
        startTask.setAssigneeId(applicantId);
        startTask.setAssigneeName(applicantName);
        startTask.setStatus(1);
        startTask.setAction(1);
        startTask.setHandleTime(LocalDateTime.now());
        startTask.setTenantId(definition.getTenantId());
        startTask.setCreateTime(LocalDateTime.now());
        taskMapper.insert(startTask);

        // 创建第一个审批节点任务
        WorkflowTask approvalTask = new WorkflowTask();
        approvalTask.setInstanceId(instance.getId());
        approvalTask.setNodeName("一级审批");
        approvalTask.setTaskType(1);
        approvalTask.setStatus(0);
        approvalTask.setTenantId(definition.getTenantId());
        approvalTask.setCreateTime(LocalDateTime.now());
        taskMapper.insert(approvalTask);

        // 更新实例当前节点
        instance.setCurrentNodeName("一级审批");
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);

        log.info("启动流程实例: definitionId={}, instanceId={}, applicantId={}",
            definitionId, instance.getId(), applicantId);
        return instance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long taskId, Long userId, String comment) {
        WorkflowTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 0) {
            throw new IllegalStateException("任务不存在或已处理: " + taskId);
        }

        // 完成当前任务
        task.setStatus(1);
        task.setAction(1);
        task.setComment(comment);
        task.setHandleTime(LocalDateTime.now());
        taskMapper.updateById(task);

        WorkflowInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            throw new IllegalStateException("流程实例不存在: " + task.getInstanceId());
        }

        // 检查是否还有后续节点（简化：审批通过后完成流程）
        // 完整实现中应从processConfig解析后续节点，此处按单节点审批处理
        long pendingTasks = taskMapper.selectCount(new LambdaQueryWrapper<WorkflowTask>()
            .eq(WorkflowTask::getInstanceId, instance.getId())
            .eq(WorkflowTask::getStatus, 0));

        if (pendingTasks == 0) {
            // 无待处理任务 → 流程完成
            instance.setStatus(1);
            instance.setResult(0);
            instance.setComment(comment);
            instance.setCurrentNodeName("已完成");
            instance.setFinishTime(LocalDateTime.now());
            instance.setUpdateTime(LocalDateTime.now());
            log.info("流程审批完成: instanceId={}, result=通过", instance.getId());
        } else {
            instance.setUpdateTime(LocalDateTime.now());
            log.info("审批通过，进入下一节点: taskId={}, instanceId={}", taskId, instance.getId());
        }
        instanceMapper.updateById(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, Long userId, String comment) {
        WorkflowTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 0) {
            throw new IllegalStateException("任务不存在或已处理: " + taskId);
        }

        // 完成当前任务（驳回）
        task.setStatus(1);
        task.setAction(2);
        task.setComment(comment);
        task.setHandleTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 结束流程实例
        WorkflowInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            throw new IllegalStateException("流程实例不存在: " + task.getInstanceId());
        }
        instance.setStatus(2);
        instance.setResult(1);
        instance.setComment(comment);
        instance.setCurrentNodeName("已驳回");
        instance.setFinishTime(LocalDateTime.now());
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);

        // 取消实例下其他待处理任务
        List<WorkflowTask> pendingTasks = taskMapper.selectList(new LambdaQueryWrapper<WorkflowTask>()
            .eq(WorkflowTask::getInstanceId, instance.getId())
            .eq(WorkflowTask::getStatus, 0));
        for (WorkflowTask pt : pendingTasks) {
            pt.setStatus(1);
            pt.setAction(2);
            pt.setHandleTime(LocalDateTime.now());
            pt.setComment("流程已驳回");
            taskMapper.updateById(pt);
        }

        log.info("审批驳回: taskId={}, instanceId={}, userId={}", taskId, instance.getId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long taskId, Long fromUserId, Long toUserId, String comment) {
        WorkflowTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 0) {
            throw new IllegalStateException("任务不存在或已处理: " + taskId);
        }

        // 记录原任务为已转交
        task.setStatus(2);
        task.setAction(3);
        task.setComment(comment);
        task.setHandleTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 创建新任务给转交目标人
        WorkflowTask newTask = new WorkflowTask();
        newTask.setInstanceId(task.getInstanceId());
        newTask.setNodeName(task.getNodeName());
        newTask.setTaskType(task.getTaskType());
        newTask.setAssigneeId(toUserId);
        newTask.setStatus(0);
        newTask.setTenantId(task.getTenantId());
        newTask.setCreateTime(LocalDateTime.now());
        taskMapper.insert(newTask);

        log.info("转交任务: taskId={}, fromUserId={}, toUserId={}, newTaskId={}",
            taskId, fromUserId, toUserId, newTask.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long instanceId, Long userId) {
        WorkflowInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在: " + instanceId);
        }
        if (!instance.getApplicantId().equals(userId)) {
            throw new IllegalStateException("只能由申请人撤回流程");
        }
        if (instance.getStatus() != 0) {
            throw new IllegalStateException("运行的流程才可以撤回");
        }

        // 删除未处理任务
        List<WorkflowTask> pendingTasks = taskMapper.selectList(new LambdaQueryWrapper<WorkflowTask>()
            .eq(WorkflowTask::getInstanceId, instanceId)
            .eq(WorkflowTask::getStatus, 0));
        for (WorkflowTask pt : pendingTasks) {
            pt.setDeleted(1);
            taskMapper.updateById(pt);
        }

        instance.setStatus(3);
        instance.setUpdateTime(LocalDateTime.now());
        instance.setFinishTime(LocalDateTime.now());
        instanceMapper.updateById(instance);

        log.info("撤回流程: instanceId={}, userId={}", instanceId, userId);
    }

    // ── 查询 ────────────────────────────────────────────

    @Override
    public WorkflowInstance getInstance(Long instanceId) {
        return instanceMapper.selectById(instanceId);
    }

    @Override
    public List<WorkflowTask> getTodoTasks(Long userId, Long tenantId) {
        return taskMapper.selectList(new LambdaQueryWrapper<WorkflowTask>()
            .eq(WorkflowTask::getAssigneeId, userId)
            .eq(tenantId != null, WorkflowTask::getTenantId, tenantId)
            .eq(WorkflowTask::getStatus, 0)
            .orderByDesc(WorkflowTask::getCreateTime));
    }

    @Override
    public List<WorkflowTask> getDoneTasks(Long userId, Long tenantId) {
        return taskMapper.selectList(new LambdaQueryWrapper<WorkflowTask>()
            .eq(WorkflowTask::getAssigneeId, userId)
            .eq(tenantId != null, WorkflowTask::getTenantId, tenantId)
            .eq(WorkflowTask::getStatus, 1)
            .orderByDesc(WorkflowTask::getHandleTime));
    }

    @Override
    public List<WorkflowTask> getInstanceHistory(Long instanceId) {
        return taskMapper.selectList(new LambdaQueryWrapper<WorkflowTask>()
            .eq(WorkflowTask::getInstanceId, instanceId)
            .orderByAsc(WorkflowTask::getCreateTime));
    }
}
