package cn.aiedge.erp.purchase.callback;

import cn.aiedge.base.workflow.facade.ApprovalCallback;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 采购订单审批结果回调（第二期：引擎终态回写单据状态；第三期：失败补偿适配）
 *
 * 由 core-api ApprovalCallbackDispatcher 在流程实例到达 approved/rejected
 * （含 terminated 视同 rejected）终态、事务提交后异步触发。
 * 复用 {@link PurchaseOrderService} 现有 approve/reject 路径，不重复造状态流转逻辑。
 *
 * 幂等：先按当前单据状态预检——已非待审批状态说明单据端点或其他回调已翻转，
 * 直接记 info 返回（视为成功，不进补偿）。
 *
 * 失败传播：单据不存在、DB 异常等真实失败向上抛出，由分发器落 failed 日志
 * 并按 1m/5m/15m/30m/1h 指数退避自动重试（第三期补偿），单个回调失败不影响其他业务。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseApprovalCallback implements ApprovalCallback {

    private static final String BIZ_TYPE = "purchase_order";

    private final PurchaseOrderService purchaseOrderService;

    @Override
    public boolean supports(String bizType) {
        return BIZ_TYPE.equals(bizType);
    }

    @Override
    public void onApproved(String bizType, Long bizId, Long operatorId, String operatorName) {
        if (!supports(bizType) || bizId == null) {
            return;
        }
        PurchaseOrder order = purchaseOrderService.getById(bizId);
        if (order == null) {
            throw new IllegalStateException("工作流回调-采购订单不存在: orderId=" + bizId);
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            log.info("工作流回调-采购订单已非待审批状态，幂等跳过: orderId={}, status={}", bizId, order.getStatus());
            return;
        }
        // 回调线程无会话，显式传入终审人（event.operatorId/operatorName）写入审核流水
        purchaseOrderService.approve(bizId, operatorId, operatorName);
        log.info("工作流回调-采购订单审批通过: orderId={}, operatorId={}, operatorName={}",
                bizId, operatorId, operatorName);
    }

    @Override
    public void onRejected(String bizType, Long bizId, String comment, Long operatorId, String operatorName) {
        if (!supports(bizType) || bizId == null) {
            return;
        }
        PurchaseOrder order = purchaseOrderService.getById(bizId);
        if (order == null) {
            throw new IllegalStateException("工作流回调-采购订单不存在: orderId=" + bizId);
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            log.info("工作流回调-采购订单已非待审批状态，驳回幂等跳过: orderId={}, status={}", bizId, order.getStatus());
            return;
        }
        purchaseOrderService.reject(bizId, comment);
        log.info("工作流回调-采购订单审批拒绝: orderId={}, operatorId={}, operatorName={}",
                bizId, operatorId, operatorName);
    }
}
