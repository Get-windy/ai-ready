package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.*;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * 分页查询采购订单（「按单据」39 列视图，含供应商快照与已收/未收数量聚合）。
     *
     * <p>端点 {@code GET /api/erp/purchase/order/page} 是前端长期约定入口
     * （入库/退货表单选源单、订单中心「采购」tab），但 Controller 一直漏写实现，
     * 请求会被 {@code @GetMapping("/{id}")} 捕获并以 400 返回。</p>
     *
     * @param current      页码（从 1 起）
     * @param size         每页条数
     * @param status       单据状态，可空
     * @param orderNo      单据编号模糊，可空
     * @param supplierName 供应商名称模糊（走快照表），可空
     * @param keyword      通用关键字，同时匹配单据编号与供应商名称，可空
     * @param startDate    单据日期起（yyyy-MM-dd），可空
     * @param endDate      单据日期止（yyyy-MM-dd，含当天），可空
     */
    Page<PurchaseOrderListDTO> pageOrders(Integer current, Integer size, Integer status,
                                          String orderNo, String supplierName, String keyword,
                                          String startDate, String endDate);

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
     * 采购订单统计：区间内的单数、总额，以及按状态 / 采购类型的分布。
     *
     * <p>从旧模块 {@code cn.aiedge.erp.order} 迁移而来（原 {@code IPurchaseOrderService.getPurchaseStatistics}），
     * 口径保持一致：总额取 {@code erp_purchase_order.total_amount} 列。
     * 消费方：首页看板 KPI（DashboardController）、采购分析页。</p>
     */
    PurchaseOrderStatisticsDTO getPurchaseStatistics(Long tenantId, LocalDateTime startDate, LocalDateTime endDate);

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
