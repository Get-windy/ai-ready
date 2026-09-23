package cn.aiedge.erp.purchase.inbound.service;

import cn.aiedge.erp.purchase.inbound.dto.PurchaseInboundQuery;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PurchaseInboundService extends IService<PurchaseInbound> {

    PurchaseInbound getByInboundNo(String inboundNo);

    Page<PurchaseInbound> pageList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status, int pageNum, int pageSize);

    Page<PurchaseInbound> pageList(PurchaseInboundQuery query);

    List<PurchaseInbound> exportList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status);

    List<PurchaseInbound> listBySupplierId(Long supplierId);

    List<PurchaseInbound> listByOrderId(Long orderId);

    String generateInboundNo();

    /**
     * Excel 批量导入采购入库单。
     *
     * @return {@code {count: 成功行数, failed: 失败行数, errors: [失败原因…]}}
     */
    Map<String, Object> importOrders(org.springframework.web.multipart.MultipartFile file);

    void batchPrint(List<Long> ids, String template);

    PurchaseInbound createInbound(PurchaseInbound inbound, List<PurchaseInboundItem> items);

    PurchaseInbound createFromOrder(Long orderId);

    PurchaseInbound updateInbound(Long inboundId, PurchaseInbound inbound, List<PurchaseInboundItem> items);

    PurchaseInbound submitForApproval(Long inboundId);

    PurchaseInbound approve(Long inboundId, Long approverId, String note);

    PurchaseInbound reject(Long inboundId, String reason);

    PurchaseInbound receive(Long inboundId, Long receiverId);

    PurchaseInboundItem receiveItem(Long itemId, BigDecimal inboundQuantity, String qualityNote);

    PurchaseInbound qualityCheck(Long inboundId, Long checkerId, String result);

    PurchaseInbound confirmWarehouse(Long inboundId, Long confirmerId);

    PurchaseInbound complete(Long inboundId);

    PurchaseInbound cancel(Long inboundId, String reason);

    void calculateTotals(Long inboundId);

    List<PurchaseInboundItem> getItems(Long inboundId);

    PurchaseInboundItem addItem(Long inboundId, PurchaseInboundItem item);

    PurchaseInboundItem updateItem(Long itemId, PurchaseInboundItem item);

    void removeItem(Long itemId);

    void updateStock(Long inboundId);
}