package cn.aiedge.workflow.facade;

import cn.aiedge.base.workflow.facade.ApprovalFacade;
import cn.aiedge.workflow.model.WorkflowInstance;
import cn.aiedge.workflow.service.WorkflowService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 审批门面实现：桥接业务单据与 core-api 工作流引擎（WorkflowService）。
 *
 * 第二期职责：
 * 1. 发起实例前按 businessType+businessId 取消在途实例（防重），再发起新实例——
 *    选择"取消旧的+新建"而非复用：单据驳回后内容可能已修改，旧实例的 formData
 *    （金额等业务数据）与节点进度均已失效，复用会导致条件节点按陈旧数据求值；
 * 2. 引擎实例到达终态后的单据回写由 ApprovalCallbackDispatcher 异步分发
 *    （见 workflow 包下 ApprovalCompletedEvent / ApprovalCallbackDispatcher）。
 *
 * 第三期新增（影子转正式）：findInFlightInstance 探测单据在途实例，
 * driveApproval 以任务级语义驱动引擎审批，供单据审批端点优先走引擎。
 *
 * 所有方法对内捕获异常并降级返回 null——门面调用方（单据服务）与单据
 * 主事务同库同事务，绝不允许引擎侧故障阻断/污染单据业务。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowApprovalFacadeImpl implements ApprovalFacade {

    private final WorkflowService workflowService;

    @Override
    public String startApproval(String processCode, String bizType, Long bizId, String bizNo,
                                Map<String, Object> businessData, Long applicantId, String applicantName) {
        try {
            // 防重：同一单据重新提交时，先取消在途（审批中/已挂起）旧实例再发起新实例。
            // 与单据主事务同事务执行，旧实例取消与新实例创建要么同时生效要么同时回滚。
            int cancelled = workflowService.cancelInFlightByBusiness(bizType, bizId, applicantId,
                    "重新提交审批，自动取消旧实例");
            if (cancelled > 0) {
                log.info("发起审批前取消在途旧实例: bizType={}, bizId={}, cancelled={}", bizType, bizId, cancelled);
            }
            Map<String, Object> formData = businessData == null ? new HashMap<>() : new HashMap<>(businessData);
            if (bizNo != null) {
                formData.put("bizNo", bizNo);
            }
            if (applicantName != null) {
                formData.put("applicantName", applicantName);
            }
            WorkflowInstance instance = workflowService.startWorkflow(
                    processCode, bizType, bizId == null ? null : String.valueOf(bizId),
                    formData, applicantId, resolveTenantId());
            String instanceId = instance == null ? null : instance.getInstanceId();
            log.info("审批实例已发起: processCode={}, bizType={}, bizId={}, instanceId={}",
                    processCode, bizType, bizId, instanceId);
            return instanceId;
        } catch (Exception e) {
            // 流程定义不存在/未发布等场景降级为 null，由调用方记 warn，不阻断业务
            log.warn("发起审批实例失败(降级为无引擎): processCode={}, bizType={}, bizId={}, error={}",
                    processCode, bizType, bizId, e.getMessage());
            return null;
        }
    }

    @Override
    public String getInstanceStatus(String instanceId) {
        try {
            Map<String, Object> status = workflowService.getWorkflowStatus(instanceId);
            if (!Boolean.TRUE.equals(status.get("exists"))) {
                return null;
            }
            Object s = status.get("status");
            return s == null ? null : s.toString();
        } catch (Exception e) {
            log.warn("查询影子审批实例状态失败: instanceId={}, error={}", instanceId, e.getMessage());
            return null;
        }
    }

    @Override
    public InFlightInstance findInFlightInstance(String bizType, Long bizId) {
        try {
            Map<String, Object> row = workflowService.findInFlightByBusiness(bizType, bizId);
            if (row == null || row.get("instanceId") == null) {
                return null;
            }
            Object taskId = row.get("currentTaskId");
            return new InFlightInstance(row.get("instanceId").toString(),
                    taskId != null ? taskId.toString() : null);
        } catch (Exception e) {
            log.warn("查询在途审批实例失败(降级为无引擎): bizType={}, bizId={}, error={}",
                    bizType, bizId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean driveApproval(InFlightInstance instance, String action, Long operatorId, String comment) {
        if (instance == null || instance.getInstanceId() == null || action == null) {
            return false;
        }
        try {
            if (instance.getCurrentTaskId() != null) {
                // 任务级审批（approveTask 语义）：任务翻转 → 实例推进 → 终态发布事件回调
                return workflowService.approveTask(instance.getCurrentTaskId(), operatorId, action, comment, null);
            }
            // 无待办任务的兜底（如角色未解析出的占位任务）：退化为实例级审批
            return switch (action) {
                case "approve" -> workflowService.approve(instance.getInstanceId(), operatorId, comment);
                case "reject" -> workflowService.reject(instance.getInstanceId(), operatorId, comment);
                default -> {
                    log.warn("驱动在途审批实例失败，未知动作: action={}", action);
                    yield false;
                }
            };
        } catch (Exception e) {
            log.warn("驱动在途审批实例失败: instanceId={}, action={}, error={}",
                    instance.getInstanceId(), action, e.getMessage());
            return false;
        }
    }

    /**
     * 解析当前租户：优先 Sa-Token 会话中的 tenantId，无登录上下文时返回 null
     * （引擎允许 tenantId 为空，内置流程定义挂在 tenant_id=0）。
     */
    private Long resolveTenantId() {
        try {
            if (StpUtil.isLogin()) {
                Object tenantId = StpUtil.getSession().get("tenantId");
                if (tenantId != null) {
                    return Long.valueOf(tenantId.toString());
                }
            }
        } catch (Exception ignored) {
            // 无会话/无租户信息时按 null 处理
        }
        return null;
    }
}
