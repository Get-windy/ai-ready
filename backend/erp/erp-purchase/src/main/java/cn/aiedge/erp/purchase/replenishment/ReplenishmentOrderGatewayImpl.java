package cn.aiedge.erp.purchase.replenishment;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.dto.PurchaseOrderDTO;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import cn.aiedge.erp.purchase.entity.PurchaseOrderPartnerSnapshot;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.aiedge.erp.stock.purchase.ReplenishmentOrderGateway;
import cn.aiedge.erp.stock.purchase.ReplenishmentOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购侧对「补货建议 → 采购订单」出口的实现。
 *
 * <p>替换掉原先「发事件但无人监听」的空转：这里真实建单并提交审批，把**真实订单号**返回给库存模块
 * 写回补货建议的 {@code created_order_no}。</p>
 *
 * <p>事务：调用方 {@code StockReplenishmentServiceImpl.createOrder} 已标
 * {@code @Transactional(rollbackFor = Exception.class)}，本方法默认 REQUIRED 加入同一事务，
 * 建单失败会连同「建议置为已处理」一起回滚，不会留下假成功。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplenishmentOrderGatewayImpl implements ReplenishmentOrderGateway {

    private final PurchaseOrderService purchaseOrderService;
    private final ReplenishmentProductMapper replenishmentProductMapper;

    @Override
    public String createPurchaseOrder(ReplenishmentOrderRequest request) {
        if (request.supplierId() == null) {
            throw BusinessException.badRequest("补货建议未指定供应商，无法生成采购订单");
        }
        Long productId = replenishmentProductMapper.selectProductIdByCode(request.productCode());
        if (productId == null) {
            throw BusinessException.badRequest("商品编码[" + request.productCode()
                    + "]在商品档案中不存在，无法生成采购订单");
        }

        PurchaseOrderDTO dto = new PurchaseOrderDTO();

        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(request.supplierId());
        order.setWarehouseId(request.warehouseId());
        order.setOrderDate(LocalDateTime.now());
        order.setPurchaseType(1);
        order.setRemark("智能补货-补货建议#" + request.suggestionId() + " 自动生成");
        dto.setOrder(order);

        // 供应商快照：建议行上的名称可能为空，回落到往来单位主数据
        String supplierName = isBlank(request.supplierName())
                ? replenishmentProductMapper.selectPartyName(request.supplierId())
                : request.supplierName();
        PurchaseOrderPartnerSnapshot snapshot = new PurchaseOrderPartnerSnapshot();
        snapshot.setSupplierName(supplierName);
        snapshot.setSupplierCode(replenishmentProductMapper.selectPartyCode(request.supplierId()));
        dto.setPartnerSnapshot(snapshot);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setProductId(productId);
        item.setProductCode(request.productCode());
        item.setProductName(request.productName());
        item.setSpecification(request.specification());
        item.setUnit(request.unit());
        item.setQuantity(request.quantity() == null ? BigDecimal.ZERO : request.quantity());
        item.setWarehouseId(request.warehouseId());
        BigDecimal purchasePrice = replenishmentProductMapper.selectPurchasePrice(productId);
        item.setUnitPrice(purchasePrice == null ? BigDecimal.ZERO : purchasePrice);
        dto.setItems(List.of(item));

        Long orderId = purchaseOrderService.createOrder(dto);
        purchaseOrderService.submitForApproval(orderId);

        // createOrder 会把生成的单据号写回同一 PurchaseOrder 实例
        String orderNo = dto.getOrder().getOrderNo();
        log.info("补货建议#{} 已生成采购订单: orderId={}, orderNo={}", request.suggestionId(), orderId, orderNo);
        return orderNo;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
