package cn.aiedge.erp.sale.outbound.service;

import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SaleOutboundService extends IService<SaleOutbound> {

    SaleOutbound getByOutboundNo(String outboundNo);

    Page<SaleOutbound> pageList(String keyword, Long customerId, Long orderId, Long warehouseId, Integer status,
                                String outboundNo, Long salesPersonId, String settlementStatus,
                                String settlementMethod, String sourceOrder, String receiverName,
                                String dateStart, String dateEnd, int pageNum, int pageSize);

    Page<Map<String, Object>> pageDetail(String keyword, Long customerId, Long warehouseId, Integer status,
                                          String outboundNo, Long productId, Long salesPersonId,
                                          String settlementStatus, String sourceOrder,
                                          String dateStart, String dateEnd, int pageNum, int pageSize);

    List<SaleOutbound> listByCustomerId(Long customerId);

    List<SaleOutbound> listByOrderId(Long orderId);

    String generateOutboundNo();

    SaleOutbound createOutbound(SaleOutbound outbound, List<SaleOutboundItem> items);

    SaleOutbound createFromOrder(Long orderId);

    SaleOutbound updateOutbound(Long outboundId, SaleOutbound outbound, List<SaleOutboundItem> items);

    SaleOutbound submitForApproval(Long outboundId);

    SaleOutbound approve(Long outboundId, Long approverId, String note);

    SaleOutbound reject(Long outboundId, String reason);

    SaleOutbound startPicking(Long outboundId, Long pickerId);

    SaleOutboundItem pickItem(Long itemId, BigDecimal outboundQuantity, String batchNo);

    SaleOutbound completePicking(Long outboundId);

    SaleOutbound startPacking(Long outboundId, Long packerId);

    SaleOutboundItem packItem(Long itemId);

    SaleOutbound completePacking(Long outboundId);

    SaleOutbound ship(Long outboundId, Long shipperId, String trackingNumber, String logisticsCompany);

    SaleOutbound complete(Long outboundId);

    SaleOutbound cancel(Long outboundId, String reason);

    void calculateTotals(Long outboundId);

    List<SaleOutboundItem> getItems(Long outboundId);

    SaleOutboundItem addItem(Long outboundId, SaleOutboundItem item);

    SaleOutboundItem updateItem(Long itemId, SaleOutboundItem item);

    void removeItem(Long itemId);

    void updateStock(Long outboundId);

    /**
     * 导出出库单列表
     */
    List<SaleOutbound> exportList(String keyword, Integer status);

    /**
     * 复制出库单（从已有出库单复制为草稿）
     */
    SaleOutbound copyOutbound(Long sourceId);

    /**
     * 批量导入出库单
     */
    int importOutbound(org.springframework.web.multipart.MultipartFile file);

    /**
     * 计算商品价格（前端选品时调用，对标Odoo pricelist / SAP条件定价）
     *
     * @param customerId 客户ID
     * @param productId 商品ID
     * @param quantity 数量
     * @param unitPrice 手动输入单价（可选，null则自动计算）
     * @return 计算结果（含最终价、各价格等级、折扣信息等）
     */
    Map<String, Object> calculateItemPrice(Long customerId, Long productId, BigDecimal quantity, BigDecimal unitPrice);
}