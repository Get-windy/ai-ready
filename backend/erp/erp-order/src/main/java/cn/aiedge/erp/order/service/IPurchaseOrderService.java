package cn.aiedge.erp.order.service;

import cn.aiedge.erp.order.dto.PurchaseOrderCreateDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderApproveDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderStatisticsDTO;
import cn.aiedge.erp.order.entity.PurchaseOrder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IPurchaseOrderService extends IService<PurchaseOrder> {

    Page<PurchaseOrder> pagePurchaseOrders(Page<PurchaseOrder> page, Long tenantId, String orderNo, String supplierName,
                                           Integer purchaseType, Integer status, Long buyerId,
                                           LocalDateTime startDate, LocalDateTime endDate);

    PurchaseOrder getPurchaseOrderDetail(Long id);

    Long createPurchaseOrder(PurchaseOrderCreateDTO dto);

    void updatePurchaseOrder(PurchaseOrderCreateDTO dto);

    void deletePurchaseOrder(Long id);

    void submitForApproval(Long orderId, String remark);

    void approvePurchaseOrder(Long orderId, PurchaseOrderApproveDTO approveDTO);

    void rejectPurchaseOrder(Long orderId, String rejectReason);

    List<PurchaseOrder> getPendingApprovalOrders(Long tenantId, Long approverId);

    void confirmBySupplier(Long orderId, String confirmationNo, String remark);

    void shipOrder(Long orderId, String trackingNo, String logisticsCompany, LocalDateTime estimatedArrivalDate);

    void receiveGoods(Long orderId, Integer receivedQuantity, String qualityCheckResult, String remark);

    void submitInvoice(Long orderId, String invoiceNo, BigDecimal invoiceAmount, LocalDateTime invoiceDate);

    void updatePurchaseOrderStatus(Long orderId, Integer status);

    PurchaseOrderStatisticsDTO getPurchaseStatistics(Long tenantId, LocalDateTime startDate, LocalDateTime endDate);

    byte[] generatePurchaseReport(Long tenantId, LocalDateTime startDate, LocalDateTime endDate, String format);

    byte[] exportPurchaseData(Long tenantId, LocalDateTime startDate, LocalDateTime endDate, String format);
}