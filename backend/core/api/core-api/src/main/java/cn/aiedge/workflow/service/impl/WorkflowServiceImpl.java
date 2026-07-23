package cn.aiedge.workflow.service.impl;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysDept;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.entity.SysUserRole;
import cn.aiedge.base.mapper.SysDeptMapper;
import cn.aiedge.base.mapper.SysRoleMapper;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.mapper.SysUserRoleMapper;
import cn.aiedge.workflow.engine.ExecutionContext;
import cn.aiedge.workflow.engine.StateTransitionManager;
import cn.aiedge.workflow.engine.WorkflowEngine;
import cn.aiedge.workflow.event.ApprovalCompletedEvent;
import cn.aiedge.workflow.entity.WorkflowDefinitionEntity;
import cn.aiedge.workflow.entity.WorkflowInstanceEntity;
import cn.aiedge.workflow.entity.WorkflowNodeEntity;
import cn.aiedge.workflow.entity.WorkflowTaskEntity;
import cn.aiedge.workflow.mapper.WorkflowDefinitionMapper;
import cn.aiedge.workflow.mapper.WorkflowInstanceMapper;
import cn.aiedge.workflow.mapper.WorkflowNodeMapper;
import cn.aiedge.workflow.mapper.WorkflowTaskMapper;
import cn.aiedge.workflow.model.*;
import cn.aiedge.workflow.model.WorkflowDefinition.WorkflowNode;
import cn.aiedge.workflow.service.WorkflowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 审批流程服务实现（PostgreSQL 持久化版本）
 *
 * 持久层：workflow_definition / workflow_node / workflow_instance / workflow_task 四张表。
 * 已完全移除 Redis CacheService 依赖，无 Redis 时全部功能可用。
 * 状态流转统一经 StateTransitionManager 校验；条件节点路由经 WorkflowEngine 的 SpEL 求值。
 *
 * 审批人解析：user 直取用户ID；role 经 sys_role.role_code → sys_user_role；
 * leader 取流程发起者所在部门的负责人，dept_leader 取节点配置部门的负责人；
 * dept 取节点配置部门（approverIds 逐项为部门ID）的全体正常成员（sys_user.status=0，仅本部门不递归）。
 * sys_dept.leader（字符串）按 用户ID(纯数字) → username → real_name 顺序解析 sys_user；
 * 部门 leader 为空或解析失败时沿 parent 链向上找，全链无果则落占位任务（assignee_id 为空）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowInstanceMapper instanceMapper;
    private final WorkflowTaskMapper taskMapper;
    private final StateTransitionManager stateTransitionManager;
    private final WorkflowEngine workflowEngine;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 流程定义管理 ====================

    @Override
    public WorkflowDefinition getWorkflowDefinition(String definitionId) {
        WorkflowDefinitionEntity entity = findDefinitionEntity(definitionId);
        return entity == null ? null : WorkflowConverter.toModel(entity);
    }

    @Override
    public List<WorkflowDefinition> getWorkflowDefinitions(String type, Long tenantId) {
        LambdaQueryWrapper<WorkflowDefinitionEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(WorkflowDefinitionEntity::getTenantId, tenantId);
        }
        Integer processType = WorkflowConverter.processTypeToInt(type);
        if (processType != null) {
            wrapper.eq(WorkflowDefinitionEntity::getProcessType, processType);
        }
        wrapper.orderByAsc(WorkflowDefinitionEntity::getId);
        List<WorkflowDefinition> result = new ArrayList<>();
        for (WorkflowDefinitionEntity entity : definitionMapper.selectList(wrapper)) {
            result.add(WorkflowConverter.toModel(entity));
        }
        return result;
    }

    @Override
    public WorkflowDefinition saveWorkflowDefinition(WorkflowDefinition definition, Long tenantId) {
        if (definition.getDefinitionId() == null || definition.getDefinitionId().isBlank()) {
            // 新建
            WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
            entity.setProcessCode(definition.getCode() != null && !definition.getCode().isBlank()
                    ? definition.getCode()
                    : "WF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
            entity.setProcessName(definition.getName());
            entity.setProcessType(WorkflowConverter.processTypeToInt(definition.getType()));
            entity.setDescription(definition.getDescription());
            entity.setProcessConfig(WorkflowConverter.nodesToJson(definition.getNodes()));
            entity.setVersion(Math.max(definition.getVersion(), 1));
            entity.setIsDefault(1);
            entity.setStatus(definition.isEnabled() ? 1 : 0);
            entity.setTenantId(tenantId);
            definitionMapper.insert(entity);
            mirrorNodes(entity.getId(), definition.getNodes(), tenantId);
            log.info("新建流程定义: id={}, code={}", entity.getId(), entity.getProcessCode());
            return WorkflowConverter.toModel(definitionMapper.selectById(entity.getId()));
        }
        // 更新（保持既有契约：POST 带 definitionId 即为更新）
        return updateWorkflowDefinition(definition.getDefinitionId(), definition, tenantId);
    }

    @Override
    public WorkflowDefinition updateWorkflowDefinition(String definitionId, WorkflowDefinition definition, Long tenantId) {
        WorkflowDefinitionEntity entity = findDefinitionEntity(definitionId);
        if (entity == null) {
            throw new RuntimeException("流程定义不存在: " + definitionId);
        }
        if (definition.getName() != null) {
            entity.setProcessName(definition.getName());
        }
        if (definition.getCode() != null && !definition.getCode().isBlank()) {
            entity.setProcessCode(definition.getCode());
        }
        if (definition.getType() != null) {
            entity.setProcessType(WorkflowConverter.processTypeToInt(definition.getType()));
        }
        if (definition.getDescription() != null) {
            entity.setDescription(definition.getDescription());
        }
        if (definition.getNodes() != null) {
            entity.setProcessConfig(WorkflowConverter.nodesToJson(definition.getNodes()));
        }
        entity.setVersion((entity.getVersion() != null ? entity.getVersion() : 1) + 1);
        entity.setStatus(definition.isEnabled() ? 1 : 2);
        entity.setUpdateTime(LocalDateTime.now());
        definitionMapper.updateById(entity);
        if (definition.getNodes() != null) {
            mirrorNodes(entity.getId(), definition.getNodes(), tenantId);
        }
        log.info("更新流程定义: id={}, version={}", entity.getId(), entity.getVersion());
        return WorkflowConverter.toModel(definitionMapper.selectById(entity.getId()));
    }

    @Override
    public boolean publishWorkflowDefinition(String definitionId, Long tenantId) {
        WorkflowDefinitionEntity entity = findDefinitionEntity(definitionId);
        if (entity == null) {
            return false;
        }
        entity.setStatus(1);
        entity.setUpdateTime(LocalDateTime.now());
        definitionMapper.updateById(entity);
        log.info("发布流程定义: id={}", entity.getId());
        return true;
    }

    @Override
    public boolean disableWorkflowDefinition(String definitionId, Long tenantId) {
        WorkflowDefinitionEntity entity = findDefinitionEntity(definitionId);
        if (entity == null) {
            return false;
        }
        entity.setStatus(2);
        entity.setUpdateTime(LocalDateTime.now());
        definitionMapper.updateById(entity);
        log.info("停用流程定义: id={}", entity.getId());
        return true;
    }

    @Override
    public long countDefinitionInstances(String definitionId) {
        WorkflowDefinitionEntity entity = findDefinitionEntity(definitionId);
        if (entity == null) {
            return 0;
        }
        return instanceMapper.selectCount(new LambdaQueryWrapper<WorkflowInstanceEntity>()
                .eq(WorkflowInstanceEntity::getDefinitionId, entity.getId()));
    }

    @Override
    public boolean deleteWorkflowDefinition(String definitionId, Long tenantId) {
        WorkflowDefinitionEntity entity = findDefinitionEntity(definitionId);
        if (entity == null) {
            return false;
        }
        long instanceCount = countDefinitionInstances(definitionId);
        if (instanceCount > 0) {
            log.warn("流程定义存在 {} 个实例引用，禁止删除: {}", instanceCount, definitionId);
            return false;
        }
        definitionMapper.deleteById(entity.getId());
        nodeMapper.delete(new LambdaQueryWrapper<WorkflowNodeEntity>()
                .eq(WorkflowNodeEntity::getDefinitionId, entity.getId()));
        log.info("删除流程定义: id={}", entity.getId());
        return true;
    }

    // ==================== 流程实例管理 ====================

    @Override
    public WorkflowInstance startWorkflow(String definitionId, String businessType,
            String businessId, Map<String, Object> businessData, Long applicantId, Long tenantId) {

        WorkflowDefinition definition = getWorkflowDefinition(definitionId);
        if (definition == null) {
            throw new RuntimeException("流程定义不存在: " + definitionId);
        }
        if (!definition.isEnabled()) {
            throw new RuntimeException("流程定义未发布或已停用: " + definitionId);
        }

        Long defDbId = Long.valueOf(definition.getDefinitionId());
        WorkflowNode firstNode = findFirstApprovalNode(definition);

        WorkflowInstanceEntity entity = new WorkflowInstanceEntity();
        entity.setDefinitionId(defDbId);
        entity.setBusinessId(parseLongOrNull(businessId));
        entity.setBusinessType(businessType);
        entity.setTitle(definition.getName());
        entity.setApplicantId(applicantId);
        entity.setApplicantName(resolveUserName(applicantId));
        entity.setStatus(WorkflowConverter.INST_APPROVING);
        entity.setTenantId(tenantId);
        entity.setFormData(WorkflowConverter.toJson(businessData));

        if (firstNode != null) {
            WorkflowNodeEntity nodeRow = findNodeRow(defDbId, firstNode.getNodeId());
            entity.setCurrentNodeId(nodeRow != null ? nodeRow.getId() : null);
            entity.setCurrentNodeName(firstNode.getNodeName());
        }
        instanceMapper.insert(entity);

        // 提交记录
        insertRecordTask(entity, null, "发起申请", applicantId, entity.getApplicantName(),
                WorkflowConverter.ACTION_SUBMIT, "提交审批申请");

        // 首个审批节点的待办任务
        if (firstNode != null) {
            createPendingTasks(entity, firstNode);
        }

        log.info("发起流程: instanceId={}, definitionId={}", entity.getId(), definitionId);
        return getWorkflowInstance(String.valueOf(entity.getId()));
    }

    @Override
    public WorkflowInstance getWorkflowInstance(String instanceId) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(parseLongOrNull(instanceId));
        return entity == null ? null : toInstanceModel(entity);
    }

    @Override
    public List<WorkflowInstance> getMyPendingApprovals(Long userId, int page, int pageSize, Long tenantId) {
        if (userId == null) {
            return List.of();
        }
        Page<WorkflowTaskEntity> taskPage = taskMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<WorkflowTaskEntity>()
                        .eq(WorkflowTaskEntity::getAssigneeId, userId)
                        .eq(WorkflowTaskEntity::getStatus, WorkflowConverter.TASK_STATUS_PENDING)
                        .eq(WorkflowTaskEntity::getTaskType, WorkflowConverter.TASK_TYPE_APPROVAL)
                        .orderByDesc(WorkflowTaskEntity::getId));
        return loadInstancesPreserveOrder(taskPage.getRecords().stream()
                .map(WorkflowTaskEntity::getInstanceId).toList());
    }

    @Override
    public List<WorkflowInstance> getMyApproved(Long userId, int page, int pageSize, Long tenantId) {
        if (userId == null) {
            return List.of();
        }
        Page<WorkflowTaskEntity> taskPage = taskMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<WorkflowTaskEntity>()
                        .eq(WorkflowTaskEntity::getAssigneeId, userId)
                        .in(WorkflowTaskEntity::getStatus, WorkflowConverter.TASK_STATUS_DONE,
                                WorkflowConverter.TASK_STATUS_TRANSFERRED)
                        .eq(WorkflowTaskEntity::getTaskType, WorkflowConverter.TASK_TYPE_APPROVAL)
                        .orderByDesc(WorkflowTaskEntity::getId));
        return loadInstancesPreserveOrder(taskPage.getRecords().stream()
                .map(WorkflowTaskEntity::getInstanceId).toList());
    }

    @Override
    public List<WorkflowInstance> getMyApplications(Long userId, int page, int pageSize, Long tenantId) {
        if (userId == null) {
            return List.of();
        }
        Page<WorkflowInstanceEntity> entityPage = instanceMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<WorkflowInstanceEntity>()
                        .eq(WorkflowInstanceEntity::getApplicantId, userId)
                        .orderByDesc(WorkflowInstanceEntity::getId));
        List<WorkflowInstance> result = new ArrayList<>();
        for (WorkflowInstanceEntity entity : entityPage.getRecords()) {
            result.add(toInstanceModel(entity));
        }
        return result;
    }

    @Override
    public boolean cancelWorkflow(String instanceId, Long userId, String reason) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(parseLongOrNull(instanceId));
        if (entity == null || !Objects.equals(entity.getApplicantId(), userId)) {
            return false;
        }
        if (!transitionTo(entity, "cancelled", userId, reason)) {
            return false;
        }
        entity.setFinishTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(entity);

        closePendingTasks(entity.getId(), "流程已取消");
        insertRecordTask(entity, entity.getCurrentNodeId(), entity.getCurrentNodeName(),
                userId, resolveUserName(userId), WorkflowConverter.ACTION_CANCEL, reason);

        log.info("取消流程: instanceId={}, userId={}", instanceId, userId);
        return true;
    }

    @Override
    public int cancelInFlightByBusiness(String businessType, Long businessId, Long operatorId, String reason) {
        if (businessType == null || businessId == null) {
            return 0;
        }
        // 在途 = 审批中 + 已挂起（均为非终态，防止同一单据堆积多条进行中实例）
        List<WorkflowInstanceEntity> inFlight = instanceMapper.selectList(
                new LambdaQueryWrapper<WorkflowInstanceEntity>()
                        .eq(WorkflowInstanceEntity::getBusinessType, businessType)
                        .eq(WorkflowInstanceEntity::getBusinessId, businessId)
                        .in(WorkflowInstanceEntity::getStatus,
                                WorkflowConverter.INST_APPROVING, WorkflowConverter.INST_SUSPENDED));
        int cancelled = 0;
        for (WorkflowInstanceEntity entity : inFlight) {
            if (!transitionTo(entity, "cancelled", operatorId, reason)) {
                log.warn("取消在途实例失败(状态机拒绝): instanceId={}, status={}", entity.getId(), entity.getStatus());
                continue;
            }
            entity.setFinishTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            instanceMapper.updateById(entity);

            closePendingTasks(entity.getId(), "流程已取消");
            insertRecordTask(entity, entity.getCurrentNodeId(), entity.getCurrentNodeName(),
                    operatorId, resolveUserName(operatorId), WorkflowConverter.ACTION_CANCEL, reason);
            cancelled++;
        }
        if (cancelled > 0) {
            log.info("取消在途审批实例: bizType={}, bizId={}, count={}, reason={}",
                    businessType, businessId, cancelled, reason);
        }
        return cancelled;
    }

    @Override
    public Map<String, Object> findInFlightByBusiness(String businessType, Long businessId) {
        if (businessType == null || businessId == null) {
            return null;
        }
        // 在途 = 审批中 + 已挂起（与 cancelInFlightByBusiness 口径一致），取最新一条
        WorkflowInstanceEntity entity = instanceMapper.selectOne(
                new LambdaQueryWrapper<WorkflowInstanceEntity>()
                        .eq(WorkflowInstanceEntity::getBusinessType, businessType)
                        .eq(WorkflowInstanceEntity::getBusinessId, businessId)
                        .in(WorkflowInstanceEntity::getStatus,
                                WorkflowConverter.INST_APPROVING, WorkflowConverter.INST_SUSPENDED)
                        .orderByDesc(WorkflowInstanceEntity::getId)
                        .last("LIMIT 1"));
        if (entity == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("instanceId", String.valueOf(entity.getId()));
        WorkflowTaskEntity pending = findPendingTask(entity.getId(), null);
        result.put("currentTaskId", pending != null ? String.valueOf(pending.getId()) : null);
        return result;
    }

    // ==================== 审批操作 ====================

    @Override
    public boolean approve(String instanceId, Long userId, String comment) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(parseLongOrNull(instanceId));
        if (entity == null || !Objects.equals(entity.getStatus(), WorkflowConverter.INST_APPROVING)) {
            return false;
        }
        WorkflowDefinition definition = getWorkflowDefinition(String.valueOf(entity.getDefinitionId()));
        if (definition == null) {
            log.warn("流程定义缺失，无法审批: definitionId={}", entity.getDefinitionId());
            return false;
        }

        // 完成当前用户的待办任务
        completeUserTask(entity, userId, WorkflowConverter.ACTION_APPROVE, comment);

        // 经引擎计算下一节点（线性 nextNodeId 或条件节点的 SpEL 分支求值）
        ExecutionContext context = ExecutionContext.builder()
                .instanceId(instanceId)
                .definitionId(definition.getDefinitionId())
                .businessData(WorkflowConverter.parseMap(entity.getFormData()))
                .applicantId(entity.getApplicantId())
                .approverId(userId)
                .comment(comment)
                .build();

        WorkflowNode nextNode = resolveNextNode(definition, resolveNodeCode(entity.getCurrentNodeId()), context, entity);

        if (nextNode == null || "end".equals(nextNode.getNodeType())) {
            // 流程结束
            if (!transitionTo(entity, "approved", userId, comment)) {
                return false;
            }
            entity.setResult(0);
            entity.setFinishTime(LocalDateTime.now());
        } else {
            // 推进到下一审批节点
            Long defDbId = entity.getDefinitionId();
            WorkflowNodeEntity nodeRow = findNodeRow(defDbId, nextNode.getNodeId());
            entity.setCurrentNodeId(nodeRow != null ? nodeRow.getId() : null);
            entity.setCurrentNodeName(nextNode.getNodeName());
        }
        entity.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(entity);

        if (nextNode != null && !"end".equals(nextNode.getNodeType())) {
            createPendingTasks(entity, nextNode);
        } else {
            // 第二期：实例到达 approved 终态，发布事件（事务提交后异步回调业务模块回写单据）
            publishApprovalCompleted(entity, "approved", userId, comment);
        }

        log.info("审批通过: instanceId={}, userId={}", instanceId, userId);
        return true;
    }

    @Override
    public boolean reject(String instanceId, Long userId, String comment) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(parseLongOrNull(instanceId));
        if (entity == null || !Objects.equals(entity.getStatus(), WorkflowConverter.INST_APPROVING)) {
            return false;
        }

        completeUserTask(entity, userId, WorkflowConverter.ACTION_REJECT, comment);

        if (!transitionTo(entity, "rejected", userId, comment)) {
            return false;
        }
        entity.setResult(1);
        entity.setFinishTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(entity);

        // 第二期：实例到达 rejected 终态，发布事件（事务提交后异步回调业务模块回写单据）
        publishApprovalCompleted(entity, "rejected", userId, comment);

        log.info("审批拒绝: instanceId={}, userId={}", instanceId, userId);
        return true;
    }

    @Override
    public boolean transfer(String instanceId, Long fromUserId, Long toUserId, String comment) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(parseLongOrNull(instanceId));
        if (entity == null || toUserId == null) {
            return false;
        }

        // 定位待办任务：优先转出人本人的，其次该实例任意待办（兼容实例级接口调用）
        WorkflowTaskEntity task = findPendingTask(entity.getId(), fromUserId);
        if (task == null) {
            task = findPendingTask(entity.getId(), null);
        }
        if (task == null) {
            log.warn("转交失败，实例无待办任务: instanceId={}", instanceId);
            return false;
        }

        task.setStatus(WorkflowConverter.TASK_STATUS_TRANSFERRED);
        task.setAction(WorkflowConverter.ACTION_TRANSFER);
        task.setComment(comment);
        task.setHandleTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        WorkflowTaskEntity newTask = new WorkflowTaskEntity();
        newTask.setInstanceId(entity.getId());
        newTask.setNodeId(task.getNodeId());
        newTask.setNodeName(task.getNodeName());
        newTask.setTaskType(WorkflowConverter.TASK_TYPE_APPROVAL);
        newTask.setAssigneeId(toUserId);
        newTask.setAssigneeName(resolveUserName(toUserId));
        newTask.setStatus(WorkflowConverter.TASK_STATUS_PENDING);
        newTask.setTenantId(entity.getTenantId());
        taskMapper.insert(newTask);

        log.info("转交审批: instanceId={}, from={}, to={}", instanceId, fromUserId, toUserId);
        return true;
    }

    @Override
    public boolean withdraw(String instanceId, Long userId, String reason) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(parseLongOrNull(instanceId));
        if (entity == null || !Objects.equals(entity.getApplicantId(), userId)) {
            return false;
        }
        if (!transitionTo(entity, "withdrawn", userId, reason)) {
            return false;
        }
        entity.setFinishTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(entity);

        closePendingTasks(entity.getId(), "流程已撤回");
        insertRecordTask(entity, entity.getCurrentNodeId(), entity.getCurrentNodeName(),
                userId, resolveUserName(userId), WorkflowConverter.ACTION_WITHDRAW, reason);

        log.info("撤回流程: instanceId={}, userId={}", instanceId, userId);
        return true;
    }

    // ==================== 审批记录 ====================

    @Override
    public List<ApprovalRecord> getApprovalRecords(String instanceId) {
        List<WorkflowTaskEntity> tasks = taskMapper.selectList(new LambdaQueryWrapper<WorkflowTaskEntity>()
                .eq(WorkflowTaskEntity::getInstanceId, parseLongOrNull(instanceId))
                .orderByAsc(WorkflowTaskEntity::getCreateTime)
                .orderByAsc(WorkflowTaskEntity::getId));
        List<ApprovalRecord> result = new ArrayList<>();
        for (WorkflowTaskEntity task : tasks) {
            result.add(WorkflowConverter.toRecord(task));
        }
        return result;
    }

    @Override
    public List<ApprovalRecord> getApprovalHistory(String businessType, String businessId) {
        WorkflowInstanceEntity entity = instanceMapper.selectOne(new LambdaQueryWrapper<WorkflowInstanceEntity>()
                .eq(WorkflowInstanceEntity::getBusinessType, businessType)
                .eq(WorkflowInstanceEntity::getBusinessId, parseLongOrNull(businessId))
                .orderByDesc(WorkflowInstanceEntity::getId)
                .last("LIMIT 1"));
        if (entity == null) {
            return new ArrayList<>();
        }
        return getApprovalRecords(String.valueOf(entity.getId()));
    }

    // ==================== 统计查询 ====================

    @Override
    public int getPendingCount(Long userId, Long tenantId) {
        if (userId == null) {
            return 0;
        }
        Long count = taskMapper.selectCount(new LambdaQueryWrapper<WorkflowTaskEntity>()
                .eq(WorkflowTaskEntity::getAssigneeId, userId)
                .eq(WorkflowTaskEntity::getStatus, WorkflowConverter.TASK_STATUS_PENDING)
                .eq(WorkflowTaskEntity::getTaskType, WorkflowConverter.TASK_TYPE_APPROVAL));
        return count != null ? count.intValue() : 0;
    }

    @Override
    public Map<String, Object> getWorkflowStatus(String instanceId) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(parseLongOrNull(instanceId));
        if (entity == null) {
            return Map.of("exists", false);
        }

        Map<String, Object> status = new HashMap<>();
        status.put("exists", true);
        status.put("instanceId", instanceId);
        status.put("status", WorkflowConverter.instanceStatusToString(entity.getStatus()));
        status.put("currentNode", entity.getCurrentNodeName());
        status.put("applicantId", entity.getApplicantId());
        status.put("applyTime", entity.getCreateTime());
        status.put("completeTime", entity.getFinishTime());

        List<ApprovalRecord> records = getApprovalRecords(instanceId);
        status.put("approvalCount", records.size());
        status.put("records", records);

        return status;
    }

    // ==================== 分页查询 ====================

    @Override
    public Map<String, Object> pageInstances(int pageNum, int pageSize, String processName, String status, Long tenantId) {
        LambdaQueryWrapper<WorkflowInstanceEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(WorkflowInstanceEntity::getTenantId, tenantId);
        }
        // 流程名称过滤：先匹配定义再按 definition_id 过滤
        if (processName != null && !processName.isBlank()) {
            List<Long> defIds = definitionMapper.selectList(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                            .like(WorkflowDefinitionEntity::getProcessName, processName))
                    .stream().map(WorkflowDefinitionEntity::getId).toList();
            if (defIds.isEmpty()) {
                return pageResult(List.of(), 0, pageNum, pageSize);
            }
            wrapper.in(WorkflowInstanceEntity::getDefinitionId, defIds);
        }
        // 状态过滤（兼容监控页词汇 running/completed 与规范词汇 approving/approved 等）
        Integer statusInt = WorkflowConverter.instanceStatusToInt(status);
        if (statusInt != null) {
            wrapper.eq(WorkflowInstanceEntity::getStatus, statusInt);
        }
        wrapper.orderByDesc(WorkflowInstanceEntity::getId);

        Page<WorkflowInstanceEntity> entityPage = instanceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (WorkflowInstanceEntity entity : entityPage.getRecords()) {
            records.add(toMonitorRecord(entity));
        }
        return pageResult(records, entityPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<String, Object> pageTasks(String tab, Long userId, int pageNum, int pageSize, Long tenantId) {
        return pageTasks(tab, userId, pageNum, pageSize, tenantId, null, null);
    }

    @Override
    public Map<String, Object> pageTasks(String tab, Long userId, int pageNum, int pageSize, Long tenantId,
                                         String taskName, String processName) {
        if (userId == null) {
            return pageResult(List.of(), 0, pageNum, pageSize);
        }
        LambdaQueryWrapper<WorkflowTaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowTaskEntity::getAssigneeId, userId);
        wrapper.eq(WorkflowTaskEntity::getTaskType, WorkflowConverter.TASK_TYPE_APPROVAL);
        if ("done".equals(tab)) {
            wrapper.in(WorkflowTaskEntity::getStatus, WorkflowConverter.TASK_STATUS_DONE,
                    WorkflowConverter.TASK_STATUS_TRANSFERRED);
        } else {
            wrapper.eq(WorkflowTaskEntity::getStatus, WorkflowConverter.TASK_STATUS_PENDING);
        }
        if (tenantId != null) {
            wrapper.eq(WorkflowTaskEntity::getTenantId, tenantId);
        }
        if (taskName != null && !taskName.isBlank()) {
            wrapper.like(WorkflowTaskEntity::getNodeName, taskName);
        }
        if (processName != null && !processName.isBlank()) {
            List<Long> defIds = definitionMapper.selectList(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                            .like(WorkflowDefinitionEntity::getProcessName, processName))
                    .stream().map(WorkflowDefinitionEntity::getId).toList();
            if (defIds.isEmpty()) {
                return pageResult(List.of(), 0, pageNum, pageSize);
            }
            List<Long> instanceIds = instanceMapper.selectList(new LambdaQueryWrapper<WorkflowInstanceEntity>()
                            .in(WorkflowInstanceEntity::getDefinitionId, defIds))
                    .stream().map(WorkflowInstanceEntity::getId).toList();
            if (instanceIds.isEmpty()) {
                return pageResult(List.of(), 0, pageNum, pageSize);
            }
            wrapper.in(WorkflowTaskEntity::getInstanceId, instanceIds);
        }
        wrapper.orderByDesc(WorkflowTaskEntity::getId);

        Page<WorkflowTaskEntity> taskPage = taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (WorkflowTaskEntity task : taskPage.getRecords()) {
            records.add(toTaskRecord(task));
        }
        return pageResult(records, taskPage.getTotal(), pageNum, pageSize);
    }

    // ==================== 流程监控 ====================

    @Override
    public Map<String, Object> getInstanceDiagram(String instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("svg", "");
            empty.put("imageUrl", "");
            return empty;
        }

        WorkflowDefinition definition = getWorkflowDefinition(instance.getDefinitionId());
        if (definition == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("svg", "");
            empty.put("imageUrl", "");
            return empty;
        }

        // 生成简单 SVG 流程图
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 400 ")
                .append(definition.getNodes().size() * 80 + 40).append("\">");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#fafafa\"/>");
        svg.append("<text x=\"200\" y=\"24\" text-anchor=\"middle\" font-size=\"14\" font-weight=\"bold\">")
                .append(escapeXml(definition.getName())).append("</text>");

        int y = 50;
        for (WorkflowNode node : definition.getNodes()) {
            boolean isCurrent = node.getNodeId().equals(instance.getCurrentNodeId());
            String fillColor = isCurrent ? "#1890ff" : "#ffffff";
            String strokeColor = isCurrent ? "#1890ff" : "#d9d9d9";
            String textColor = isCurrent ? "#ffffff" : "#333333";

            if ("start".equals(node.getNodeType()) || "end".equals(node.getNodeType())) {
                // 椭圆形
                svg.append("<ellipse cx=\"200\" cy=\"").append(y + 20)
                        .append("\" rx=\"60\" ry=\"20\" fill=\"").append(fillColor)
                        .append("\" stroke=\"").append(strokeColor).append("\" stroke-width=\"2\"/>");
            } else {
                // 矩形
                svg.append("<rect x=\"120\" y=\"").append(y)
                        .append("\" width=\"160\" height=\"40\" rx=\"4\" fill=\"").append(fillColor)
                        .append("\" stroke=\"").append(strokeColor).append("\" stroke-width=\"2\"/>");
            }
            svg.append("<text x=\"200\" y=\"").append(y + (isCurrent ? 26 : 24))
                    .append("\" text-anchor=\"middle\" font-size=\"12\" fill=\"").append(textColor).append("\">")
                    .append(escapeXml(node.getNodeName())).append("</text>");

            y += 80;

            // 箭头
            if (node.getNextNodeId() != null && !node.getNextNodeId().isEmpty()) {
                int arrowY = y - 60;
                svg.append("<line x1=\"200\" y1=\"").append(arrowY)
                        .append("\" x2=\"200\" y2=\"").append(arrowY + 20)
                        .append("\" stroke=\"#999\" stroke-width=\"1.5\" marker-end=\"url(#arrow)\"/>");
            }
        }

        svg.append("<defs><marker id=\"arrow\" viewBox=\"0 0 10 10\" refX=\"10\" refY=\"5\"")
                .append(" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto\">")
                .append("<path d=\"M 0 0 L 10 5 L 0 10 z\" fill=\"#999\"/></marker></defs>");
        svg.append("</svg>");

        Map<String, Object> result = new HashMap<>();
        result.put("svg", svg.toString());
        result.put("imageUrl", "");
        return result;
    }

    @Override
    public boolean interveneInstance(String instanceId, String action, Long userId) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(parseLongOrNull(instanceId));
        if (entity == null) {
            log.warn("[workflow] 干预失败，实例不存在: {}", instanceId);
            return false;
        }

        log.info("[workflow] 流程干预: instanceId={}, action={}, userId={}", instanceId, action, userId);

        String targetStatus = switch (action) {
            case "terminate" -> "terminated";
            case "suspend" -> "suspended";
            case "resume" -> "approving";
            default -> null;
        };
        if (targetStatus == null) {
            log.warn("[workflow] 未知干预动作: {}", action);
            return false;
        }
        if (!transitionTo(entity, targetStatus, userId, "流程干预: " + action)) {
            return false;
        }
        entity.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(entity);

        if ("terminate".equals(action)) {
            closePendingTasks(entity.getId(), "流程已终止");
            // 第三期：实例终止视同驳回回写单据——发布终态事件（result=terminated），
            // dispatcher 将其映射为 onRejected（comment 前缀"流程终止"），单据回退草稿可重新提交
            publishApprovalCompleted(entity, "terminated", userId, null);
        }

        // 记录干预操作
        insertRecordTask(entity, entity.getCurrentNodeId(), entity.getCurrentNodeName(),
                userId, resolveUserName(userId), WorkflowConverter.ACTION_INTERVENE, "流程干预: " + action);

        log.info("[workflow] 流程干预成功: instanceId={}, action={}", instanceId, action);
        return true;
    }

    // ==================== 任务级操作（Step 2） ====================

    @Override
    public Map<String, Object> getInstanceDetail(String instanceId) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(parseLongOrNull(instanceId));
        if (entity == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        String canonicalStatus = WorkflowConverter.instanceStatusToString(entity.getStatus());
        detail.put("instanceId", String.valueOf(entity.getId()));
        detail.put("definitionId", String.valueOf(entity.getDefinitionId()));
        detail.put("processName", resolveDefinitionName(entity.getDefinitionId()));
        detail.put("status", WorkflowConverter.toMonitorStatus(canonicalStatus));
        detail.put("canonicalStatus", canonicalStatus);
        detail.put("initiator", entity.getApplicantName() != null
                ? entity.getApplicantName() : String.valueOf(entity.getApplicantId()));
        detail.put("applicantId", entity.getApplicantId());
        detail.put("startTime", formatTime(entity.getCreateTime()));
        detail.put("endTime", formatTime(entity.getFinishTime()));
        detail.put("currentNode", entity.getCurrentNodeName());
        detail.put("businessType", entity.getBusinessType());
        detail.put("businessData", entity.getFormData());
        detail.put("result", entity.getResult());

        List<Map<String, Object>> records = new ArrayList<>();
        for (ApprovalRecord record : getApprovalRecords(instanceId)) {
            Map<String, Object> item = new HashMap<>();
            item.put("nodeName", record.getNodeName());
            item.put("approverName", record.getApproverName());
            item.put("action", record.getAction());
            item.put("comment", record.getComment());
            item.put("time", formatTime(record.getApproveTime()));
            records.add(item);
        }
        detail.put("records", records);
        return detail;
    }

    @Override
    public Map<String, Object> getTaskDetail(String taskId) {
        WorkflowTaskEntity task = taskMapper.selectById(parseLongOrNull(taskId));
        if (task == null) {
            return null;
        }
        WorkflowInstanceEntity instance = instanceMapper.selectById(task.getInstanceId());

        Map<String, Object> detail = new HashMap<>();
        detail.put("taskId", String.valueOf(task.getId()));
        detail.put("taskName", task.getNodeName());
        detail.put("instanceId", String.valueOf(task.getInstanceId()));
        detail.put("processName", instance != null ? resolveDefinitionName(instance.getDefinitionId()) : null);
        detail.put("priority", "medium");
        detail.put("assignee", task.getAssigneeName() != null
                ? task.getAssigneeName()
                : (task.getAssigneeId() != null ? "用户" + task.getAssigneeId() : "未指定"));
        detail.put("currentNode", instance != null ? instance.getCurrentNodeName() : task.getNodeName());
        detail.put("createTime", formatTime(task.getCreateTime()));
        detail.put("dueTime", formatTime(computeDueTime(task)));
        detail.put("status", task.getStatus());
        detail.put("action", WorkflowConverter.taskActionToString(task.getAction()));
        detail.put("comment", task.getComment());
        if (instance != null) {
            WorkflowDefinitionEntity def = definitionMapper.selectById(instance.getDefinitionId());
            detail.put("description", def != null ? def.getDescription() : null);
            detail.put("businessType", instance.getBusinessType());
            detail.put("businessData", instance.getFormData());
        }
        return detail;
    }

    @Override
    public boolean approveTask(String taskId, Long userId, String action, String comment, String returnNode) {
        WorkflowTaskEntity task = taskMapper.selectById(parseLongOrNull(taskId));
        if (task == null || !Objects.equals(task.getStatus(), WorkflowConverter.TASK_STATUS_PENDING)) {
            log.warn("任务审批失败，任务不存在或已处理: taskId={}", taskId);
            return false;
        }
        Long operator = userId != null ? userId : task.getAssigneeId();
        String instanceId = String.valueOf(task.getInstanceId());
        return switch (action != null ? action : "") {
            case "approve" -> approve(instanceId, operator, comment);
            case "reject" -> reject(instanceId, operator, comment);
            case "return" -> {
                String target = "start".equals(returnNode) ? "发起人" : "上一节点";
                yield reject(instanceId, operator, "[退回至" + target + "] " + (comment != null ? comment : ""));
            }
            default -> {
                log.warn("任务审批失败，未知动作: action={}", action);
                yield false;
            }
        };
    }

    @Override
    public boolean transferTask(String taskId, Long userId, Long targetUserId, String comment) {
        WorkflowTaskEntity task = taskMapper.selectById(parseLongOrNull(taskId));
        if (task == null || !Objects.equals(task.getStatus(), WorkflowConverter.TASK_STATUS_PENDING)) {
            log.warn("任务转交失败，任务不存在或已处理: taskId={}", taskId);
            return false;
        }
        Long fromUser = task.getAssigneeId() != null ? task.getAssigneeId() : userId;
        return transfer(String.valueOf(task.getInstanceId()), fromUser, targetUserId, comment);
    }

    // ==================== 流程分析（Step 2） ====================

    @Override
    public Map<String, Object> getAnalysisSummary(Long tenantId) {
        Map<String, Object> stats = instanceMapper.selectOverallStats();
        Long todoTasks = taskMapper.selectCount(new LambdaQueryWrapper<WorkflowTaskEntity>()
                .eq(WorkflowTaskEntity::getStatus, WorkflowConverter.TASK_STATUS_PENDING)
                .eq(WorkflowTaskEntity::getTaskType, WorkflowConverter.TASK_TYPE_APPROVAL));

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalInstances", toLong(stats.get("total_instances")));
        statistics.put("runningInstances", toLong(stats.get("running_instances")));
        statistics.put("todoTasks", todoTasks != null ? todoTasks : 0L);
        statistics.put("avgDuration", round1(toDouble(stats.get("avg_duration_days"))));

        // 按流程分组的耗时
        List<Map<String, Object>> processDuration = new ArrayList<>();
        for (Map<String, Object> row : instanceMapper.selectProcessDurations()) {
            Map<String, Object> item = new HashMap<>();
            item.put("processName", row.get("process_name"));
            item.put("instanceCount", toLong(row.get("instance_count")));
            item.put("avgDuration", formatDays(toDouble(row.get("avg_days"))));
            item.put("maxDuration", formatDays(toDouble(row.get("max_days"))));
            item.put("minDuration", formatDays(toDouble(row.get("min_days"))));
            processDuration.add(item);
        }

        // 按节点分组的耗时与超时
        List<Map<String, Object>> nodeDuration = new ArrayList<>();
        for (Map<String, Object> row : taskMapper.selectNodeDurations()) {
            Map<String, Object> item = new HashMap<>();
            long taskCount = toLong(row.get("task_count"));
            long overdueCount = toLong(row.get("overdue_count"));
            item.put("nodeName", row.get("node_name"));
            item.put("taskCount", taskCount);
            item.put("avgDuration", round1(toDouble(row.get("avg_hours"))) + "h");
            item.put("overdueCount", overdueCount);
            item.put("overdueRate", taskCount > 0 ? round1(overdueCount * 100.0 / taskCount) + "%" : "0%");
            nodeDuration.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", statistics);
        result.put("statistics", statistics);
        result.put("processDuration", processDuration);
        result.put("nodeDuration", nodeDuration);
        result.put("refreshedAt", formatTime(LocalDateTime.now()));
        return result;
    }

    @Override
    public Map<String, Object> getAnalysisReport(String startDate, String endDate, Long tenantId) {
        LocalDate end = parseDateOrDefault(endDate, LocalDate.now());
        LocalDate start = parseDateOrDefault(startDate, end.minusDays(6));

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : taskMapper.selectDailyTaskStats(start.toString(), end.toString())) {
            long total = toLong(row.get("total_tasks"));
            long completed = toLong(row.get("completed_tasks"));
            int completionRate = total > 0 ? (int) Math.round(completed * 100.0 / total) : 0;
            Map<String, Object> item = new HashMap<>();
            item.put("date", row.get("day"));
            item.put("totalTasks", total);
            item.put("completedTasks", completed);
            item.put("completionRate", completionRate);
            item.put("avgDuration", formatDays(toDouble(row.get("avg_days"))));
            item.put("overdueTasks", toLong(row.get("overdue_tasks")));
            item.put("efficiency", round1(completionRate / 20.0));
            records.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", Map.of("records", records));
        result.put("records", records);
        result.put("startDate", start.toString());
        result.put("endDate", end.toString());
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 按 definitionId 查找定义实体：数值按主键，非数值按 process_code（兼容内置流程编码）
     */
    private WorkflowDefinitionEntity findDefinitionEntity(String definitionId) {
        if (definitionId == null || definitionId.isBlank()) {
            return null;
        }
        Long id = parseLongOrNull(definitionId);
        if (id != null) {
            return definitionMapper.selectById(id);
        }
        return definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                .eq(WorkflowDefinitionEntity::getProcessCode, definitionId)
                .orderByDesc(WorkflowDefinitionEntity::getId)
                .last("LIMIT 1"));
    }

    /**
     * 节点明细镜像：逻辑删除旧节点后按当前定义重写（定义更新场景）
     */
    private void mirrorNodes(Long definitionId, List<WorkflowNode> nodes, Long tenantId) {
        nodeMapper.delete(new LambdaQueryWrapper<WorkflowNodeEntity>()
                .eq(WorkflowNodeEntity::getDefinitionId, definitionId));
        if (nodes == null) {
            return;
        }
        int order = 0;
        for (WorkflowNode node : nodes) {
            nodeMapper.insert(WorkflowConverter.nodeToEntity(definitionId, node, order++, tenantId));
        }
    }

    private WorkflowNodeEntity findNodeRow(Long definitionId, String nodeCode) {
        if (definitionId == null || nodeCode == null) {
            return null;
        }
        return nodeMapper.selectOne(new LambdaQueryWrapper<WorkflowNodeEntity>()
                .eq(WorkflowNodeEntity::getDefinitionId, definitionId)
                .eq(WorkflowNodeEntity::getNodeCode, nodeCode)
                .orderByAsc(WorkflowNodeEntity::getNodeOrder)
                .last("LIMIT 1"));
    }

    /**
     * 节点行ID → 模型层节点编码（含已逻辑删除的旧版本节点）
     */
    private String resolveNodeCode(Long nodeRowId) {
        if (nodeRowId == null) {
            return null;
        }
        WorkflowNodeEntity row = nodeMapper.selectByIdIncludeDeleted(nodeRowId);
        return row != null ? row.getNodeCode() : null;
    }

    private String resolveDefinitionName(Long definitionId) {
        if (definitionId == null) {
            return null;
        }
        WorkflowDefinitionEntity def = definitionMapper.selectById(definitionId);
        return def != null ? def.getProcessName() : null;
    }

    private WorkflowInstance toInstanceModel(WorkflowInstanceEntity entity) {
        return WorkflowConverter.toModel(entity, resolveDefinitionName(entity.getDefinitionId()),
                this::resolveNodeCode);
    }

    private List<WorkflowInstance> loadInstancesPreserveOrder(List<Long> instanceIds) {
        List<WorkflowInstance> result = new ArrayList<>();
        for (Long id : instanceIds) {
            WorkflowInstanceEntity entity = instanceMapper.selectById(id);
            if (entity != null) {
                result.add(toInstanceModel(entity));
            }
        }
        return result;
    }

    /**
     * 发布审批终态事件：由 ApprovalCallbackDispatcher 在事务提交后（无事务时经
     * fallbackExecution 立即）异步分发给各业务模块的 ApprovalCallback 实现回写单据。
     * 发布失败仅记日志，不影响引擎审批结果（终态已落库）。
     */
    private void publishApprovalCompleted(WorkflowInstanceEntity entity, String result, Long operatorId, String comment) {
        try {
            eventPublisher.publishEvent(new ApprovalCompletedEvent(
                    String.valueOf(entity.getId()), entity.getBusinessType(), entity.getBusinessId(),
                    result, operatorId, resolveUserName(operatorId), comment, entity.getTenantId()));
        } catch (Exception e) {
            log.error("发布审批终态事件失败: instanceId={}, result={}, error={}",
                    entity.getId(), result, e.getMessage(), e);
        }
    }

    /**
     * 经 StateTransitionManager 校验并执行实例状态流转
     */
    private boolean transitionTo(WorkflowInstanceEntity entity, String targetStatus, Long operatorId, String reason) {
        cn.aiedge.workflow.model.WorkflowInstance model = new cn.aiedge.workflow.model.WorkflowInstance();
        model.setInstanceId(String.valueOf(entity.getId()));
        model.setStatus(WorkflowConverter.instanceStatusToString(entity.getStatus()));
        StateTransitionManager.TransitionResult result =
                stateTransitionManager.transition(model, targetStatus, operatorId, reason);
        if (!result.isSuccess()) {
            return false;
        }
        entity.setStatus(WorkflowConverter.instanceStatusToInt(targetStatus));
        if (model.getCompleteTime() != null) {
            entity.setFinishTime(model.getCompleteTime());
        }
        return true;
    }

    private WorkflowTaskEntity findPendingTask(Long instanceId, Long assigneeId) {
        LambdaQueryWrapper<WorkflowTaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowTaskEntity::getInstanceId, instanceId)
                .eq(WorkflowTaskEntity::getStatus, WorkflowConverter.TASK_STATUS_PENDING)
                .eq(WorkflowTaskEntity::getTaskType, WorkflowConverter.TASK_TYPE_APPROVAL);
        if (assigneeId != null) {
            wrapper.eq(WorkflowTaskEntity::getAssigneeId, assigneeId);
        }
        wrapper.orderByDesc(WorkflowTaskEntity::getId).last("LIMIT 1");
        return taskMapper.selectOne(wrapper);
    }

    /**
     * 完成用户在当前实例上的待办任务
     */
    private void completeUserTask(WorkflowInstanceEntity instance, Long userId, int actionCode, String comment) {
        WorkflowTaskEntity task = findPendingTask(instance.getId(), userId);
        if (task == null) {
            log.warn("未找到用户待办任务: instanceId={}, userId={}", instance.getId(), userId);
            return;
        }
        task.setStatus(WorkflowConverter.TASK_STATUS_DONE);
        task.setAction(actionCode);
        task.setComment(comment);
        task.setHandleTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private void closePendingTasks(Long instanceId, String note) {
        List<WorkflowTaskEntity> pending = taskMapper.selectList(new LambdaQueryWrapper<WorkflowTaskEntity>()
                .eq(WorkflowTaskEntity::getInstanceId, instanceId)
                .eq(WorkflowTaskEntity::getStatus, WorkflowConverter.TASK_STATUS_PENDING));
        for (WorkflowTaskEntity task : pending) {
            task.setStatus(WorkflowConverter.TASK_STATUS_DONE);
            task.setComment(note);
            task.setHandleTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    private void insertRecordTask(WorkflowInstanceEntity instance, Long nodeRowId, String nodeName,
                                  Long operatorId, String operatorName, int actionCode, String comment) {
        WorkflowTaskEntity record = new WorkflowTaskEntity();
        record.setInstanceId(instance.getId());
        record.setNodeId(nodeRowId);
        record.setNodeName(nodeName);
        record.setTaskType(WorkflowConverter.TASK_TYPE_RECORD);
        record.setAssigneeId(operatorId);
        record.setAssigneeName(operatorName);
        record.setStatus(WorkflowConverter.TASK_STATUS_DONE);
        record.setAction(actionCode);
        record.setComment(comment);
        record.setHandleTime(LocalDateTime.now());
        record.setTenantId(instance.getTenantId());
        taskMapper.insert(record);
    }

    /**
     * 为审批节点创建待办任务（含审批人解析）
     */
    private void createPendingTasks(WorkflowInstanceEntity instance, WorkflowNode node) {
        List<Long> assigneeIds = resolveAssignees(node, instance.getApplicantId(), instance.getTenantId());
        WorkflowNodeEntity nodeRow = findNodeRow(instance.getDefinitionId(), node.getNodeId());
        Long nodeRowId = nodeRow != null ? nodeRow.getId() : null;

        if (assigneeIds.isEmpty()) {
            // 角色/负责人未能解析出具体用户时的占位任务（见类注释 TODO）
            WorkflowTaskEntity task = new WorkflowTaskEntity();
            task.setInstanceId(instance.getId());
            task.setNodeId(nodeRowId);
            task.setNodeName(node.getNodeName());
            task.setTaskType(WorkflowConverter.TASK_TYPE_APPROVAL);
            task.setStatus(WorkflowConverter.TASK_STATUS_PENDING);
            task.setAssigneeName(unresolvedAssigneeLabel(node));
            task.setTenantId(instance.getTenantId());
            if (node.getTimeoutHours() > 0) {
                // 截止时间由查询侧按 create_time + time_limit 推算，此处不落库
            }
            taskMapper.insert(task);
            return;
        }
        for (Long assigneeId : assigneeIds) {
            WorkflowTaskEntity task = new WorkflowTaskEntity();
            task.setInstanceId(instance.getId());
            task.setNodeId(nodeRowId);
            task.setNodeName(node.getNodeName());
            task.setTaskType(WorkflowConverter.TASK_TYPE_APPROVAL);
            task.setAssigneeId(assigneeId);
            task.setAssigneeName(resolveUserName(assigneeId));
            task.setStatus(WorkflowConverter.TASK_STATUS_PENDING);
            task.setTenantId(instance.getTenantId());
            taskMapper.insert(task);
        }
    }

    private String unresolvedAssigneeLabel(WorkflowNode node) {
        String ids = node.getApproverIds() != null ? String.join(",", node.getApproverIds()) : "";
        return switch (node.getApproverType() != null ? node.getApproverType() : "") {
            case "role" -> "角色:" + ids;
            case "leader", "dept_leader" -> "部门负责人";
            case "dept" -> "部门:" + ids;
            default -> ids.isEmpty() ? "未指定审批人" : ids;
        };
    }

    /**
     * 解析审批节点的具体审批人。
     * user: 直接使用配置的用户ID；role: 经 sys_role.role_code → sys_user_role 解析；
     * applicant_self: 申请人本人；leader: 发起者所在部门的负责人；
     * dept_leader: 节点配置部门（approverIds 为部门ID）的负责人；
     * dept: 节点配置部门（approverIds 逐项为部门ID）的全体正常成员
     * （sys_user.status=0 且未删除，仅本部门不递归子部门）；部门无成员或不存在时返回空，由调用方落占位任务。
     */
    private List<Long> resolveAssignees(WorkflowNode node, Long applicantId, Long tenantId) {
        String approverType = node.getApproverType() != null ? node.getApproverType() : "user";
        List<String> configured = node.getApproverIds() != null ? node.getApproverIds() : List.of();
        Set<Long> userIds = new LinkedHashSet<>();

        switch (approverType) {
            case "user" -> {
                for (String id : configured) {
                    Long uid = parseLongOrNull(id);
                    if (uid != null) {
                        userIds.add(uid);
                    }
                }
            }
            case "role" -> {
                Set<Long> roleIds = new LinkedHashSet<>();
                for (String roleKey : configured) {
                    Long roleId = parseLongOrNull(roleKey);
                    if (roleId != null) {
                        roleIds.add(roleId);
                        continue;
                    }
                    try {
                        SysRole role = sysRoleMapper.selectByRoleCode(roleKey, tenantId);
                        if (role != null) {
                            roleIds.add(role.getId());
                        }
                    } catch (Exception e) {
                        log.warn("角色解析失败: roleCode={}, {}", roleKey, e.getMessage());
                    }
                }
                if (!roleIds.isEmpty()) {
                    List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                            new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, roleIds));
                    for (SysUserRole ur : userRoles) {
                        userIds.add(ur.getUserId());
                    }
                }
                if (userIds.isEmpty()) {
                    log.warn("角色审批人未解析到用户: roles={}", configured);
                }
            }
            case "applicant_self" -> {
                if (applicantId != null) {
                    userIds.add(applicantId);
                }
            }
            case "leader" -> {
                // 流程发起者所在部门的负责人
                Long deptId = null;
                if (applicantId != null) {
                    SysUser applicant = sysUserMapper.selectById(applicantId);
                    if (applicant != null) {
                        deptId = applicant.getDeptId();
                    }
                }
                Long leaderId = resolveDeptLeader(deptId, tenantId);
                if (leaderId != null) {
                    userIds.add(leaderId);
                } else {
                    log.warn("部门负责人未解析到用户: approverType=leader, applicantId={}, deptId={}", applicantId, deptId);
                }
            }
            case "dept_leader" -> {
                // 节点配置的指定部门的负责人（approverIds 为部门ID）
                for (String configuredDept : configured) {
                    Long deptId = parseLongOrNull(configuredDept);
                    if (deptId == null) {
                        log.warn("dept_leader 配置的部门ID无法解析: {}", configuredDept);
                        continue;
                    }
                    Long leaderId = resolveDeptLeader(deptId, tenantId);
                    if (leaderId != null) {
                        userIds.add(leaderId);
                    }
                }
                if (userIds.isEmpty()) {
                    log.warn("部门负责人未解析到用户: approverType=dept_leader, depts={}", configured);
                }
            }
            case "dept" -> {
                // 指定部门全体成员（approverIds 逐项为部门ID）。仅本部门、不递归子部门：
                // 递归会把下级部门员工卷入上级部门审批，越权面大于漏派；需子部门参与时应显式配置多个部门
                Set<Long> deptIds = new LinkedHashSet<>();
                for (String configuredDept : configured) {
                    Long deptId = parseLongOrNull(configuredDept);
                    if (deptId != null) {
                        deptIds.add(deptId);
                    } else {
                        log.warn("dept 配置的部门ID无法解析: {}", configuredDept);
                    }
                }
                if (!deptIds.isEmpty()) {
                    // sys_user.status: 0-正常 1-禁用 2-锁定（见 AuthServiceImpl 登录校验）；
                    // deleted=0 由 @TableLogic 自动过滤
                    List<SysUser> members = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .in(SysUser::getDeptId, deptIds)
                            .eq(SysUser::getStatus, 0)
                            .eq(tenantId != null, SysUser::getTenantId, tenantId));
                    for (SysUser member : members) {
                        userIds.add(member.getId());
                    }
                }
                if (userIds.isEmpty()) {
                    log.warn("部门成员未解析到用户: approverType=dept, depts={}", configured);
                }
            }
            default -> log.warn("TODO: 审批人类型暂未实现解析: {}", approverType);
        }
        return new ArrayList<>(userIds);
    }

    /**
     * 解析部门负责人：sys_dept.leader（字符串）按 用户ID(纯数字) → username → real_name
     * 顺序匹配 sys_user；当前部门 leader 为空或解析不到用户时沿 parent 链向上找，
     * 全链无果返回 null（由调用方落占位任务）。
     */
    private Long resolveDeptLeader(Long deptId, Long tenantId) {
        Set<Long> visited = new HashSet<>();
        Long currentDeptId = deptId;
        while (currentDeptId != null && visited.add(currentDeptId)) {
            SysDept dept;
            try {
                dept = sysDeptMapper.selectById(currentDeptId);
            } catch (Exception e) {
                log.warn("部门查询失败: deptId={}, {}", currentDeptId, e.getMessage());
                return null;
            }
            if (dept == null) {
                return null;
            }
            Long leaderId = resolveLeaderUser(dept.getLeader(), tenantId);
            if (leaderId != null) {
                return leaderId;
            }
            currentDeptId = dept.getParentId();
        }
        return null;
    }

    /**
     * 将 sys_dept.leader 字符串解析为 sys_user 用户ID：
     * 纯数字优先按用户ID解析，否则按 username 再按 real_name 匹配；均无果返回 null。
     */
    private Long resolveLeaderUser(String leader, Long tenantId) {
        if (leader == null || leader.isBlank()) {
            return null;
        }
        String key = leader.trim();
        try {
            Long uid = parseLongOrNull(key);
            if (uid != null) {
                SysUser user = sysUserMapper.selectById(uid);
                if (user != null) {
                    return user.getId();
                }
            }
            LambdaQueryWrapper<SysUser> byUsername = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, key)
                    .eq(tenantId != null, SysUser::getTenantId, tenantId)
                    .last("LIMIT 1");
            SysUser user = sysUserMapper.selectOne(byUsername);
            if (user == null) {
                LambdaQueryWrapper<SysUser> byRealName = new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRealName, key)
                        .eq(tenantId != null, SysUser::getTenantId, tenantId)
                        .last("LIMIT 1");
                user = sysUserMapper.selectOne(byRealName);
            }
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            log.warn("部门负责人解析失败: leader={}, {}", key, e.getMessage());
            return null;
        }
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null) {
                if (user.getNickname() != null && !user.getNickname().isBlank()) {
                    return user.getNickname();
                }
                return user.getUsername();
            }
        } catch (Exception e) {
            log.debug("用户名解析失败: userId={}, {}: {}", userId, e.getClass().getSimpleName(), e.getMessage());
        }
        return "用户" + userId;
    }

    private WorkflowNode findFirstApprovalNode(WorkflowDefinition definition) {
        if (definition.getNodes() == null) {
            return null;
        }
        for (WorkflowNode node : definition.getNodes()) {
            if ("approval".equals(node.getNodeType())) {
                return node;
            }
        }
        return null;
    }

    /**
     * 计算并跳过中间节点：条件节点经引擎 SpEL 求值，抄送节点记录后直通
     */
    private WorkflowNode resolveNextNode(WorkflowDefinition definition, String currentNodeCode,
                                         ExecutionContext context, WorkflowInstanceEntity instance) {
        if (currentNodeCode == null) {
            return null;
        }
        String nextCode = workflowEngine.calculateNextNode(definition, currentNodeCode, context);
        WorkflowNode nextNode = findNode(definition, nextCode);
        int guard = 0;
        while (nextNode != null && guard++ < 20) {
            if ("condition".equals(nextNode.getNodeType())) {
                nextCode = workflowEngine.calculateNextNode(definition, nextNode.getNodeId(), context);
                nextNode = findNode(definition, nextCode);
            } else if ("cc".equals(nextNode.getNodeType())) {
                // 抄送节点：留抄送记录后直通
                WorkflowNodeEntity ccRow = findNodeRow(instance.getDefinitionId(), nextNode.getNodeId());
                insertRecordTask(instance, ccRow != null ? ccRow.getId() : null,
                        nextNode.getNodeName(), null, "抄送", WorkflowConverter.ACTION_SUBMIT, "抄送");
                nextCode = nextNode.getNextNodeId();
                nextNode = findNode(definition, nextCode);
            } else {
                break;
            }
        }
        return nextNode;
    }

    private WorkflowNode findNode(WorkflowDefinition definition, String nodeId) {
        if (nodeId == null || definition.getNodes() == null) {
            return null;
        }
        for (WorkflowNode node : definition.getNodes()) {
            if (nodeId.equals(node.getNodeId())) {
                return node;
            }
        }
        return null;
    }

    // ==================== 记录组装（监控页/任务页） ====================

    /**
     * 实例 → 监控页记录（状态使用监控页词汇 running/completed/...）
     */
    private Map<String, Object> toMonitorRecord(WorkflowInstanceEntity entity) {
        Map<String, Object> record = new HashMap<>();
        String canonicalStatus = WorkflowConverter.instanceStatusToString(entity.getStatus());
        record.put("instanceId", String.valueOf(entity.getId()));
        record.put("definitionId", String.valueOf(entity.getDefinitionId()));
        String defName = resolveDefinitionName(entity.getDefinitionId());
        record.put("processName", defName != null ? defName : entity.getTitle());
        record.put("workflowName", defName != null ? defName : entity.getTitle());
        record.put("status", WorkflowConverter.toMonitorStatus(canonicalStatus));
        record.put("canonicalStatus", canonicalStatus);
        record.put("currentNode", entity.getCurrentNodeName());
        record.put("currentNodeName", entity.getCurrentNodeName());
        record.put("initiator", entity.getApplicantName() != null
                ? entity.getApplicantName() : String.valueOf(entity.getApplicantId()));
        record.put("startTime", formatTime(entity.getCreateTime()));
        record.put("endTime", formatTime(entity.getFinishTime()));
        record.put("duration", formatDuration(entity.getCreateTime(), entity.getFinishTime()));
        record.put("businessType", entity.getBusinessType());
        record.put("tenantId", entity.getTenantId());
        return record;
    }

    /**
     * 任务 → 任务管理页记录
     */
    private Map<String, Object> toTaskRecord(WorkflowTaskEntity task) {
        Map<String, Object> record = new HashMap<>();
        record.put("taskId", String.valueOf(task.getId()));
        record.put("taskName", task.getNodeName());
        record.put("instanceId", String.valueOf(task.getInstanceId()));
        WorkflowInstanceEntity instance = instanceMapper.selectById(task.getInstanceId());
        record.put("processName", instance != null ? resolveDefinitionName(instance.getDefinitionId()) : null);
        record.put("priority", "medium");
        record.put("assignee", task.getAssigneeName() != null
                ? task.getAssigneeName()
                : (task.getAssigneeId() != null ? "用户" + task.getAssigneeId() : "未指定"));
        record.put("createTime", formatTime(task.getCreateTime()));
        record.put("dueTime", formatTime(computeDueTime(task)));
        record.put("status", task.getStatus());
        record.put("action", WorkflowConverter.taskActionToString(task.getAction()));
        record.put("comment", task.getComment());
        return record;
    }

    /**
     * 截止时间 = 任务创建时间 + 节点审批时限（小时）
     */
    private LocalDateTime computeDueTime(WorkflowTaskEntity task) {
        if (task.getNodeId() == null || task.getCreateTime() == null) {
            return null;
        }
        WorkflowNodeEntity node = nodeMapper.selectByIdIncludeDeleted(task.getNodeId());
        if (node == null || node.getTimeLimit() == null) {
            return null;
        }
        return task.getCreateTime().plusHours(node.getTimeLimit());
    }

    // ==================== 工具方法 ====================

    private Map<String, Object> pageResult(List<?> records, long total, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatTime(LocalDateTime time) {
        return time != null ? time.format(DATETIME_FMT) : null;
    }

    private static String formatDuration(LocalDateTime start, LocalDateTime finish) {
        if (start == null) {
            return null;
        }
        Duration duration = Duration.between(start, finish != null ? finish : LocalDateTime.now());
        double hours = duration.toMinutes() / 60.0;
        if (hours < 48) {
            return round1(hours) + "h";
        }
        return round1(hours / 24.0) + "天";
    }

    private static String formatDays(double days) {
        return round1(days) + "天";
    }

    private static double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private static LocalDate parseDateOrDefault(String value, LocalDate defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
