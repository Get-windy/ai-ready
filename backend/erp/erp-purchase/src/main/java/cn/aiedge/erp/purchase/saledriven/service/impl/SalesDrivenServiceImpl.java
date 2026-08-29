package cn.aiedge.erp.purchase.saledriven.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.dto.PurchaseOrderDTO;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import cn.aiedge.erp.purchase.entity.PurchaseOrderPartnerSnapshot;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenItemDTO;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenPurchaseResult;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenQueryDTO;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenRowDTO;
import cn.aiedge.erp.purchase.saledriven.mapper.SalesDrivenMapper;
import cn.aiedge.erp.purchase.saledriven.service.SalesDrivenService;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 以销定购 Service 实现
 * <p>
 * 列表数据源为销售订单表（跨模块只读），采购成品/采购原料复用 {@link PurchaseOrderService} 创建采购订单
 * 并按商品默认供应商（最近采购单供应商）分组、自动提交审批，实现「以销定购」生产级闭环。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SalesDrivenServiceImpl implements SalesDrivenService {

    private final SalesDrivenMapper salesDrivenMapper;
    private final PurchaseOrderService purchaseOrderService;

    // ══════════════════════════════════════════════════════════════════
    // 查询
    // ══════════════════════════════════════════════════════════════════

    @Override
    public Page<SalesDrivenRowDTO> page(SalesDrivenQueryDTO query) {
        long current = query.getCurrent() != null ? query.getCurrent() : 1L;
        long size = query.getSize() != null ? query.getSize() : 20L;
        Page<SalesDrivenRowDTO> page = new Page<>(current, size);

        QueryWrapper<SalesDrivenRowDTO> wrapper = new QueryWrapper<>();
        wrapper.eq("o.deleted", 0);

        // 默认隐藏已取消（除非显示已取消勾选）
        boolean showCancelled = Boolean.TRUE.equals(query.getShowCancelled());
        if (!showCancelled) {
            wrapper.ne("o.status", 6);
        }

        // 单据日期
        wrapper.ge(notBlank(query.getDateStart()), "o.order_date", parseDateTimeStart(query.getDateStart()))
               .le(notBlank(query.getDateEnd()), "o.order_date", parseDateTimeEnd(query.getDateEnd()));

        // 单据编号
        wrapper.like(notBlank(query.getOrderNo()), "o.order_no", query.getOrderNo());

        // 销售类型
        wrapper.eq(query.getSaleType() != null, "o.sale_type", query.getSaleType());

        // 录入方式
        wrapper.like(notBlank(query.getGenerationMethod()), "o.generation_method", query.getGenerationMethod());

        // 客户
        wrapper.like(notBlank(query.getCustomerName()), "o.customer_name", query.getCustomerName());

        // 经手人
        wrapper.like(notBlank(query.getSalesmanName()), "o.salesman_name", query.getSalesmanName());

        // 部门
        wrapper.like(notBlank(query.getDeptName()), "o.dept_name", query.getDeptName());

        // 制单人
        wrapper.like(notBlank(query.getCreatorName()), "o.creator_name", query.getCreatorName());

        // 提交人
        wrapper.like(notBlank(query.getSubmitterName()), "o.submitter_name", query.getSubmitterName());

        // 审核人
        wrapper.like(notBlank(query.getAuditorName()), "o.auditor_name", query.getAuditorName());

        // 仓库
        wrapper.like(notBlank(query.getWarehouseName()), "o.warehouse_name", query.getWarehouseName());

        // 商品（通过明细表匹配）
        if (notBlank(query.getProductName())) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_sale_order_item soi " +
                    "WHERE soi.order_id = o.id AND soi.product_name LIKE {0})",
                    "%" + query.getProductName() + "%");
        }

        // 单据状态
        wrapper.eq(query.getStatus() != null, "o.status", query.getStatus());

        // 记账状态
        if (query.getBookkeepingStatus() != null) {
            if (query.getBookkeepingStatus() == 1) {
                wrapper.isNotNull("o.bookkeeping_time");
            } else {
                wrapper.isNull("o.bookkeeping_time");
            }
        }

        // 支付状态
        wrapper.eq(query.getPaymentStatus() != null, "o.payment_status", query.getPaymentStatus());

        // 发货日期
        wrapper.ge(notBlank(query.getShipDateStart()), "o.expected_ship_time", parseDateTimeStart(query.getShipDateStart()))
               .le(notBlank(query.getShipDateEnd()), "o.expected_ship_time", parseDateTimeEnd(query.getShipDateEnd()));

        // 收货人/电话/地址
        wrapper.like(notBlank(query.getReceiverName()), "o.receiver_name", query.getReceiverName());
        wrapper.like(notBlank(query.getReceiverPhone()), "o.receiver_phone", query.getReceiverPhone());
        wrapper.like(notBlank(query.getShippingAddress()), "o.shipping_address", query.getShippingAddress());

        // 物流公司/运单号
        wrapper.like(notBlank(query.getLogisticsCompany()), "o.logistics_company", query.getLogisticsCompany());
        wrapper.like(notBlank(query.getWaybillNo()), "o.waybill_no", query.getWaybillNo());

        // 优惠券
        wrapper.eq(query.getCouponAmount() != null, "o.coupon_amount", query.getCouponAmount());

        // 订金账户
        wrapper.like(notBlank(query.getDepositAccount1()), "o.deposit_account1", query.getDepositAccount1());
        wrapper.like(notBlank(query.getDepositAccount2()), "o.deposit_account2", query.getDepositAccount2());

        // 卖家备注/买家备注
        wrapper.like(notBlank(query.getSellerRemark()), "o.order_remark", query.getSellerRemark());
        wrapper.like(notBlank(query.getBuyerRemark()), "o.buyer_remark", query.getBuyerRemark());

        // 只查看用了优惠券的订单
        if (Boolean.TRUE.equals(query.getOnlyCoupon())) {
            wrapper.gt("o.coupon_amount", 0);
        }

        // 排序
        wrapper.orderByDesc("o.order_date").orderByDesc("o.id");

        return salesDrivenMapper.selectPageWithDetails(page, wrapper);
    }

    // ══════════════════════════════════════════════════════════════════
    // 采购成品 / 采购原料
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesDrivenPurchaseResult purchase(Long orderId, String mode) {
        String orderNo = salesDrivenMapper.selectOrderNoById(orderId);
        if (orderNo == null) {
            throw BusinessException.notFound("销售订单不存在");
        }
        String finalMode = MODE_MATERIAL.equals(mode) ? MODE_MATERIAL : MODE_FINISHED;

        // 重复采购校验：同一销售单已存在在途采购单则拦截
        if (salesDrivenMapper.countActivePurchaseBySourceBillNo(orderNo) > 0) {
            throw BusinessException.badRequest("该销售订单已生成采购单，请勿重复采购");
        }

        Long saleWarehouseId = salesDrivenMapper.selectWarehouseIdById(orderId);

        // 1. 采集采购目标商品
        List<SalesDrivenItemDTO> targets = collectTargets(orderId, finalMode);

        // 2. 按商品默认供应商分组
        Map<Long, List<SalesDrivenItemDTO>> grouped = new LinkedHashMap<>();
        for (SalesDrivenItemDTO target : targets) {
            Long supplierId = salesDrivenMapper.selectDefaultSupplierId(target.getProductId());
            if (supplierId == null) {
                throw BusinessException.badRequest(
                        "商品[" + target.getProductName() + "]无默认供应商，请先补充该商品的历史采购关系");
            }
            grouped.computeIfAbsent(supplierId, k -> new ArrayList<>()).add(target);
        }
        if (grouped.isEmpty()) {
            throw BusinessException.badRequest("无可采购商品");
        }

        // 3. 按供应商分组创建采购订单并提交
        SalesDrivenPurchaseResult result = new SalesDrivenPurchaseResult();
        result.setOrderNo(orderNo);
        result.setMode(finalMode);

        for (Map.Entry<Long, List<SalesDrivenItemDTO>> entry : grouped.entrySet()) {
            Long supplierId = entry.getKey();
            List<SalesDrivenItemDTO> items = entry.getValue();
            PurchaseOrderDTO dto = buildPurchaseDTO(supplierId, saleWarehouseId, orderNo, finalMode, items);

            Long createdOrderId = purchaseOrderService.createOrder(dto);
            purchaseOrderService.submitForApproval(createdOrderId);
            result.getPurchaseOrderNos().add(dto.getOrder().getOrderNo());
        }
        result.setOrderCount(result.getPurchaseOrderNos().size());

        return result;
    }

    /** 采集采购目标商品：采购成品=销售明细商品；采购原料=按BOM拆解的原料 */
    private List<SalesDrivenItemDTO> collectTargets(Long orderId, String mode) {
        List<SalesDrivenItemDTO> saleItems = salesDrivenMapper.selectItemsByOrderId(orderId);
        if (saleItems == null || saleItems.isEmpty()) {
            throw BusinessException.badRequest("该销售订单无明细，无法采购");
        }

        List<SalesDrivenItemDTO> targets = new ArrayList<>();
        if (MODE_FINISHED.equals(mode)) {
            targets.addAll(saleItems);
        } else {
            for (SalesDrivenItemDTO saleItem : saleItems) {
                Long kitId = salesDrivenMapper.selectActiveKitIdByProductId(saleItem.getProductId());
                if (kitId == null) {
                    throw BusinessException.badRequest(
                            "商品[" + saleItem.getProductName() + "]无物料清单，无法采购原料");
                }
                List<SalesDrivenItemDTO> components = salesDrivenMapper.selectKitComponents(kitId);
                if (components == null || components.isEmpty()) {
                    throw BusinessException.badRequest(
                            "商品[" + saleItem.getProductName() + "]的物料清单为空，无法采购原料");
                }
                BigDecimal saleQty = nvl(saleItem.getQuantity());
                for (SalesDrivenItemDTO comp : components) {
                    SalesDrivenItemDTO target = new SalesDrivenItemDTO();
                    target.setItemId(comp.getItemId());
                    target.setProductId(comp.getProductId());
                    target.setProductCode(comp.getProductCode());
                    target.setProductName(comp.getProductName());
                    target.setSpecification(comp.getSpecification());
                    target.setUnit(comp.getUnit());
                    // 原料采购数量 = 成品销售数量 × BOM单耗
                    target.setQuantity(saleQty.multiply(nvl(comp.getQuantity())));
                    target.setUnitPrice(comp.getUnitPrice());
                    target.setWarehouseId(saleItem.getWarehouseId());
                    targets.add(target);
                }
            }
        }
        return targets;
    }

    /** 构建按供应商分组的采购订单 DTO（主表+供应商快照+明细） */
    private PurchaseOrderDTO buildPurchaseDTO(Long supplierId, Long warehouseId,
                                              String sourceBillNo, String mode,
                                              List<SalesDrivenItemDTO> items) {
        PurchaseOrderDTO dto = new PurchaseOrderDTO();

        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(supplierId);
        order.setWarehouseId(warehouseId);
        order.setOrderDate(LocalDateTime.now());
        order.setPurchaseType(1);
        order.setSourceBillNo(sourceBillNo);
        order.setRemark("以销定购-" + (MODE_FINISHED.equals(mode) ? "采购成品" : "采购原料") + " 自动生成");
        dto.setOrder(order);

        // 供应商快照
        String supplierName = salesDrivenMapper.selectDefaultSupplierName(items.get(0).getProductId());
        PurchaseOrderPartnerSnapshot snapshot = new PurchaseOrderPartnerSnapshot();
        snapshot.setSupplierName(supplierName);
        dto.setPartnerSnapshot(snapshot);

        // 明细：同一商品合并数量
        Map<Long, PurchaseOrderItem> itemMap = new LinkedHashMap<>();
        for (SalesDrivenItemDTO it : items) {
            PurchaseOrderItem existing = itemMap.get(it.getProductId());
            if (existing == null) {
                PurchaseOrderItem poi = new PurchaseOrderItem();
                poi.setProductId(it.getProductId());
                poi.setProductCode(it.getProductCode());
                poi.setProductName(it.getProductName());
                poi.setSpecification(it.getSpecification());
                poi.setUnit(it.getUnit());
                poi.setQuantity(nvl(it.getQuantity()));
                poi.setWarehouseId(it.getWarehouseId() != null ? it.getWarehouseId() : warehouseId);
                poi.setUnitPrice(fetchPurchasePrice(it.getProductId()));
                itemMap.put(it.getProductId(), poi);
            } else {
                existing.setQuantity(existing.getQuantity().add(nvl(it.getQuantity())));
            }
        }
        dto.setItems(new ArrayList<>(itemMap.values()));

        return dto;
    }

    /** 商品采购价（erp_product.purchase_price），缺失回退销售单价 */
    private BigDecimal fetchPurchasePrice(Long productId) {
        BigDecimal price = salesDrivenMapper.selectProductPurchasePrice(productId);
        return price != null ? price : BigDecimal.ZERO;
    }

    // ══════════════════════════════════════════════════════════════════
    // 工具
    // ══════════════════════════════════════════════════════════════════

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private LocalDateTime parseDateTimeStart(String dateStr) {
        if (notBlank(dateStr)) {
            try { return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(); }
            catch (Exception e) { return null; }
        }
        return null;
    }

    private LocalDateTime parseDateTimeEnd(String dateStr) {
        if (notBlank(dateStr)) {
            try { return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59); }
            catch (Exception e) { return null; }
        }
        return null;
    }
}
