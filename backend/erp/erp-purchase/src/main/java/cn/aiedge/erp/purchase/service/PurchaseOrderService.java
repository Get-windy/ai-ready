package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.*;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 采购订单服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PurchaseOrderService extends IService<PurchaseOrder> {

    /**
     * 创建采购订单（含子表）
     */
    Long createOrder(PurchaseOrderDTO dto);

    /**
     * 更新采购订单（含子表）
     */
    void updateOrder(Long id, PurchaseOrderDTO dto);

    /**
     * 获取采购订单详情（含所有子表）
     */
    PurchaseOrderDTO getOrderDetail(Long id);

    /**
     * 删除采购订单
     */
    void deleteOrder(Long orderId);

    /**
     * 提交审批
     */
    void submitForApproval(Long orderId);

    /**
     * 审批通过
     */
    void approve(Long orderId);

    /**
     * 审批通过（显式指定终审人）：工作流回调等无 Sa-Token 会话的线程使用，
     * 终审人写入审核流水，避免 operator 落空
     */
    void approve(Long orderId, Long auditorId, String auditorName);

    /**
     * 审批拒绝
     */
    void reject(Long orderId, String reason);

    /**
     * 取消订单
     */
    void cancel(Long orderId, String reason);

    /**
     * 批量审批
     */
    void batchApprove(List<Long> orderIds);

    /**
     * 生成下一单据号
     */
    String generateNextOrderNo(LocalDate date);

    /**
     * 导出采购订单
     */
    List<PurchaseOrder> exportOrders(Long tenantId, String orderNo, Long supplierId, Integer status);

    /**
     * 批量导入采购订单
     */
    int importOrders(MultipartFile file);

    /**
     * 批量打印
     */
    void batchPrint(List<Long> orderIds, String template);
}
