package cn.aiedge.erp.sale.outbound.service;

import cn.aiedge.erp.sale.outbound.dto.SaleOutboundQueryDTO;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SaleOutboundService extends IService<SaleOutbound> {

    SaleOutbound getByOutboundNo(String outboundNo);

    /** 按单据分页查询（对标文档 40 项查询条件） */
    Page<SaleOutbound> pageList(SaleOutboundQueryDTO query);

    /** 按明细分页查询（对标文档 18 项查询条件，分页口径 = 明细行） */
    Page<Map<String, Object>> pageDetail(SaleOutboundQueryDTO query);

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
     * WMS 发货确认回调：从销售订单创建/定位销售出库单并推进为已发货。
     * 注意：WMS 侧已用 InventoryService.decrease 完成库存扣减，本方法**不再扣减**（避免双扣链路），仅建单+推进状态。
     */
    SaleOutbound confirmFromWms(Long saleOrderId);

    /**
     * 导出售库单列表（按当前查询条件返回全部命中数据，供 Excel 流式导出）
     */
    List<SaleOutbound> exportList(SaleOutboundQueryDTO query);

    /**
     * 批量写入物流备注（列表页「物流备注」功能，真实落库到 logistics_remark）
     *
     * @return 实际更新的单据数
     */
    int batchUpdateLogisticsRemark(List<Long> ids, String logisticsRemark);

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