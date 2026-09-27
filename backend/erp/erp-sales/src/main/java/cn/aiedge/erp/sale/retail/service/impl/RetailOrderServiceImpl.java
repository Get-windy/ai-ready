package cn.aiedge.erp.sale.retail.service.impl;

import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyMapper;
import cn.aiedge.erp.sale.retail.entity.RetailOrder;
import cn.aiedge.erp.sale.retail.entity.RetailOrderItem;
import cn.aiedge.erp.sale.retail.entity.RetailOrderPayment;
import cn.aiedge.erp.sale.retail.mapper.RetailOrderItemMapper;
import cn.aiedge.erp.sale.retail.mapper.RetailOrderMapper;
import cn.aiedge.erp.sale.retail.mapper.RetailOrderPaymentMapper;
import cn.aiedge.erp.sale.retail.service.IRetailOrderService;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.StockService;
import cn.aiedge.common.event.InventoryChangeEvent;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetailOrderServiceImpl extends ServiceImpl<RetailOrderMapper, RetailOrder> implements IRetailOrderService {

    private final RetailOrderItemMapper retailOrderItemMapper;
    private final RetailOrderPaymentMapper retailOrderPaymentMapper;
    private final PartyMapper partyMapper;
    /**
     * 积分台账（**余额的权威载体**）。
     * erp-sales 本来就依赖并已 import erp-marketing（`SaleOrderServiceImpl` 用了它的促销引擎），
     * 所以这里直接注入，没有新增模块依赖、也不构成循环。
     */
    private final cn.aiedge.erp.marketing.service.PointsLedgerService pointsLedgerService;
    private final ProductMapper productMapper;
    private final StockMapper stockMapper;
    private final StockService stockService;
    private final BizNumberGeneratorService bizNumberGeneratorService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public IPage<RetailOrder> pageByDoc(Page<RetailOrder> page, RetailQueryDTO query) {
        // 商品行属性是明细字段，先按明细命中反查单据ID
        if (StringUtils.hasText(query.getProductAttribute())) {
            List<Long> orderIds = findOrderIdsByProductAttribute(query.getProductAttribute());
            if (orderIds.isEmpty()) {
                return page.setRecords(Collections.emptyList()).setTotal(0);
            }
            query.setMatchedOrderIds(orderIds);
        }
        LambdaQueryWrapper<RetailOrder> wrapper = buildDocWrapper(query)
                .orderByDesc(RetailOrder::getCreateTime);
        IPage<RetailOrder> result = page(page, wrapper);
        // 计算非持久化字段：折后金额、优惠后金额、优惠金额总计
        for (RetailOrder o : result.getRecords()) {
            BigDecimal baseAmt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
            BigDecimal direct = o.getDirectDiscount() != null ? o.getDirectDiscount() : BigDecimal.ZERO;
            BigDecimal coupon = o.getCouponDiscount() != null ? o.getCouponDiscount() : BigDecimal.ZERO;
            BigDecimal promo = o.getPromoDiscount() != null ? o.getPromoDiscount() : BigDecimal.ZERO;
            o.setDiscountedAmount(baseAmt.subtract(direct));
            o.setFavorableAmount(baseAmt.subtract(direct).subtract(coupon).subtract(promo));
            o.setTotalDiscount(direct.add(coupon).add(promo));
        }
        return result;
    }

    @Override
    public IPage<Map<String, Object>> pageByDetail(Page<Map<String, Object>> page, RetailDetailQueryDTO query) {
        // 1) 先按主表条件命中单据ID（日期/单号/客户/经手人/部门/仓库/状态/红冲），
        //    保证明细分页的 total 与过滤结果一致（原实现先分页明细再过滤，total 会虚高）
        List<Long> matchedOrderIds = list(buildDocWrapper(query).select(RetailOrder::getId))
                .stream().map(RetailOrder::getId).collect(Collectors.toList());
        if (matchedOrderIds.isEmpty()) {
            Page<Map<String, Object>> emptyPage = new Page<>(page.getCurrent(), page.getSize(), 0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 2) 明细条件 + 主表命中集合，一次性分页
        LambdaQueryWrapper<RetailOrderItem> itemWrapper = new LambdaQueryWrapper<RetailOrderItem>()
                .eq(RetailOrderItem::getDeleted, 0)
                .in(RetailOrderItem::getOrderId, matchedOrderIds)
                .like(StringUtils.hasText(query.getProductName()), RetailOrderItem::getProductName, query.getProductName())
                .like(StringUtils.hasText(query.getBarcode()), RetailOrderItem::getBarcode, query.getBarcode())
                .like(StringUtils.hasText(query.getRemark()), RetailOrderItem::getRemark, query.getRemark())
                .orderByDesc(RetailOrderItem::getCreateTime);

        IPage<RetailOrderItem> itemPage = new Page<>(page.getCurrent(), page.getSize());
        itemPage = retailOrderItemMapper.selectPage(itemPage, itemWrapper);
        if (itemPage.getRecords().isEmpty()) {
            Page<Map<String, Object>> emptyPage = new Page<>(page.getCurrent(), page.getSize(), itemPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        Set<Long> orderIds = itemPage.getRecords().stream()
                .map(RetailOrderItem::getOrderId)
                .collect(Collectors.toSet());

        // 3) 取本次页内单据（同一批过滤条件，直接按ID取回）
        Map<Long, RetailOrder> orderMap = listByIds(orderIds).stream()
                .filter(o -> o.getDeleted() == null || o.getDeleted() == 0)
                .collect(Collectors.toMap(RetailOrder::getId, o -> o));

        // 组装结果
        List<Map<String, Object>> records = itemPage.getRecords().stream()
                .filter(item -> orderMap.containsKey(item.getOrderId()))
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    RetailOrder order = orderMap.get(item.getOrderId());
                    // 主表字段
                    map.put("orderId", order.getId());
                    map.put("retailNo", order.getRetailNo());
                    map.put("orderDate", order.getOrderDate());
                    map.put("status", order.getStatus());
                    map.put("warehouseId", order.getWarehouseId());
                    map.put("warehouseName", order.getWarehouseName());
                    map.put("customerId", order.getCustomerId());
                    map.put("customerName", order.getCustomerName());
                    map.put("customerCode", order.getCustomerCode());
                    map.put("handlerId", order.getHandlerId());
                    map.put("handlerName", order.getHandlerName());
                    map.put("departmentId", order.getDepartmentId());
                    map.put("departmentName", order.getDepartmentName());
                    map.put("remark", order.getRemark());
                    map.put("summary", order.getSummary());
                    map.put("attachment", order.getAttachment());
                    map.put("creatorName", order.getCreatorName());
                    map.put("bookkeeperName", order.getBookkeeperName());
                    map.put("bookkeepingTime", order.getBookkeepingTime());
                    map.put("createTime", order.getCreateTime());
                    map.put("printCount", order.getPrintCount());
                    // 明细字段
                    map.put("itemId", item.getId());
                    map.put("productId", item.getProductId());
                    map.put("productName", item.getProductName());
                    map.put("itemCode", item.getProductCode());
                    map.put("barcode", item.getBarcode());
                    map.put("specification", item.getSpecification());
                    map.put("model", item.getModel());
                    map.put("origin", item.getOrigin());
                    map.put("brand", item.getBrand());
                    map.put("unit", item.getUnit());
                    map.put("quantity", item.getQuantity());
                    map.put("unitPrice", item.getUnitPrice());
                    map.put("amount", item.getAmount());
                    map.put("discountRate", item.getDiscountRate());
                    map.put("discountedPrice", item.getDiscountedPrice());
                    map.put("discountedAmount", item.getDiscountedAmount());
                    map.put("favorableDiscountRate", item.getFavorableDiscountRate());
                    map.put("favorableUnitPrice", item.getFavorableUnitPrice());
                    map.put("favorableAmount", item.getFavorableAmount());
                    map.put("itemRemark", item.getRemark());
                    map.put("conversionRelation", item.getConversionRelation());
                    map.put("conversionResult", item.getConversionResult());
                    map.put("bigPack", item.getBigPack());
                    map.put("midPack", item.getMidPack());
                    map.put("smallPack", item.getSmallPack());
                    map.put("smallUnit", item.getSmallUnit());
                    map.put("smallUnitQuantity", item.getSmallUnitQuantity());
                    map.put("smallUnitPrice", item.getSmallUnitPrice());
                    map.put("availableStock", item.getAvailableStock());
                    map.put("bookStock", item.getBookStock());
                    map.put("costPrice", item.getCostPrice());
                    map.put("costAmount", item.getCostAmount());
                    map.put("retailPrice", item.getRetailPrice());
                    map.put("wholesalePrice", item.getWholesalePrice());
                    map.put("minSalePrice", item.getMinSalePrice());
                    map.put("lastSalePrice", item.getLastSalePrice());
                    map.put("lastSaleDate", item.getLastSaleDate());
                    map.put("priceRestaurant", item.getPriceRestaurant());
                    map.put("priceCanteen", item.getPriceCanteen());
                    map.put("priceVipSelf", item.getPriceVipSelf());
                    map.put("priceLargeGroup", item.getPriceLargeGroup());
                    map.put("priceSpecialCustomer", item.getPriceSpecialCustomer());
                    map.put("priceOutRestaurant", item.getPriceOutRestaurant());
                    map.put("priceVipLevel1", item.getPriceVipLevel1());
                    map.put("priceVipLevel2", item.getPriceVipLevel2());
                    map.put("gift", item.getGift());
                    map.put("exchangePoints", item.getExchangePoints());
                    map.put("usedPoints", item.getUsedPoints());
                    map.put("generatedPoints", item.getGeneratedPoints());
                    map.put("productAttribute", item.getProductAttribute());
                    map.put("batchNo", item.getBatchNo());
                    map.put("batchCode", item.getBatchCode());
                    map.put("productionDate", item.getProductionDate());
                    map.put("shelfLife", item.getShelfLife());
                    map.put("expiryDate", item.getExpiryDate());
                    map.put("imageUrl", item.getImageUrl());
                    map.put("extNum1", item.getExtNum1());
                    map.put("extNum2", item.getExtNum2());
                    map.put("extNum3", item.getExtNum3());
                    map.put("extNum4", item.getExtNum4());
                    map.put("extNum5", item.getExtNum5());
                    map.put("extNum6", item.getExtNum6());
                    map.put("extNum7", item.getExtNum7());
                    map.put("extText1", item.getExtText1());
                    map.put("extText2", item.getExtText2());
                    map.put("extPartner", item.getExtPartner());
                    map.put("extStaff", item.getExtStaff());
                    map.put("extDept", item.getExtDept());
                    return map;
                })
                .collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(itemPage.getCurrent(), itemPage.getSize(), itemPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetailOrder saveWithItems(RetailOrder order, List<RetailOrderItem> items) {
        // 生成零售单号（LS+日期+4位流水）
        if (!StringUtils.hasText(order.getRetailNo())) {
            order.setRetailNo(generateRetailNo());
        }
        // 填充快照字段
        buildSnapshots(order, items);
        // 计算商品总金额
        calculateAmounts(order, items);
        // 设置制单人
        if (!StringUtils.hasText(order.getCreatorName())) {
            order.setCreatorName("系统");
        }
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        save(order);
        if (items != null) {
            int lineNo = 1;
            for (RetailOrderItem item : items) {
                item.setOrderId(order.getId());
                item.setLineNo(lineNo++);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                retailOrderItemMapper.insert(item);
            }
        }
        return order;
    }

    /**
     * 生成零售单号：走系统统一号段（biz_number_sequence: LSD → LS-yyyyMMdd-NNNN）
     * 数据库行锁保证跨进程唯一，前端 /next-no 与保存时未传单号均走此处。
     */
    @Override
    public String generateRetailNo() {
        return bizNumberGeneratorService.nextNumber("LSD");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetailOrder updateWithItems(RetailOrder order, List<RetailOrderItem> items) {
        // 重新计算金额
        calculateAmounts(order, items);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        // 软删除旧明细
        LambdaUpdateWrapper<RetailOrderItem> delWrapper = new LambdaUpdateWrapper<>();
        delWrapper.eq(RetailOrderItem::getOrderId, order.getId())
                  .set(RetailOrderItem::getDeleted, 1);
        retailOrderItemMapper.update(null, delWrapper);
        // 插入新明细
        if (items != null) {
            int lineNo = 1;
            for (RetailOrderItem item : items) {
                item.setId(null);
                item.setOrderId(order.getId());
                item.setLineNo(lineNo++);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                retailOrderItemMapper.insert(item);
            }
        }
        return order;
    }

    @Override
    public List<RetailOrderItem> listItemsByOrderId(Long orderId) {
        LambdaQueryWrapper<RetailOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RetailOrderItem::getOrderId, orderId)
               .eq(RetailOrderItem::getDeleted, 0)
               .orderByAsc(RetailOrderItem::getLineNo);
        return retailOrderItemMapper.selectList(wrapper);
    }

    @Override
    public RetailOrderDetailVO getDetailById(Long id) {
        RetailOrder order = getById(id);
        if (order == null) return null;
        List<RetailOrderItem> items = listItemsByOrderId(id);
        RetailOrderDetailVO vo = new RetailOrderDetailVO(order, items);
        // 查询支付明细
        LambdaQueryWrapper<RetailOrderPayment> payWrapper = new LambdaQueryWrapper<>();
        payWrapper.eq(RetailOrderPayment::getOrderId, id)
                  .eq(RetailOrderPayment::getDeleted, 0);
        vo.setPayments(retailOrderPaymentMapper.selectList(payWrapper));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetailOrder settle(Long orderId, List<RetailOrderPayment> payments) {
        RetailOrder order = getById(orderId);
        if (order == null) throw BusinessException.notFound("零售单不存在");
        Integer currentStatus = order.getStatus();
        if (currentStatus != null && currentStatus == 1) throw BusinessException.badRequest("零售单已完成结算");
        if (currentStatus != null && currentStatus == 3) throw BusinessException.badRequest("零售单已作废");

        BigDecimal payable = order.getPayableAmount() != null ? order.getPayableAmount() : BigDecimal.ZERO;

        // 收款明细不能为空（应收为 0 的免单/全额优惠单据除外）
        if ((payments == null || payments.isEmpty()) && payable.compareTo(BigDecimal.ZERO) > 0) {
            throw BusinessException.badRequest("请录入收款方式及金额");
        }

        // 实收合计（找零前）
        BigDecimal totalPayment = payments == null ? BigDecimal.ZERO
                : payments.stream()
                    .map(p -> p.getPaymentAmount() != null ? p.getPaymentAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 收款不得少于应收；多收部分由后端统一计算为找零（不信任前端传入的找零）
        if (totalPayment.compareTo(payable) < 0) {
            throw BusinessException.badRequest(String.format("收款不足: 应收%.2f，实收%.2f", payable, totalPayment));
        }
        BigDecimal changeAmount = totalPayment.subtract(payable);

        // 落库收款明细，并回写主表各支付方式汇总
        if (payments != null) {
            for (RetailOrderPayment payment : payments) {
                if (payment.getPaymentAmount() == null) payment.setPaymentAmount(BigDecimal.ZERO);
                payment.setId(null);
                payment.setOrderId(orderId);
                payment.setPaymentTime(LocalDateTime.now());
                payment.setCreateTime(LocalDateTime.now());
                payment.setUpdateTime(LocalDateTime.now());
                retailOrderPaymentMapper.insert(payment);

                // 更新主表收款汇总
                switch (payment.getPaymentMethod() == null ? "" : payment.getPaymentMethod()) {
                    case "CASH": order.setCashAmount(payment.getPaymentAmount()); break;
                    case "CARD": order.setCardAmount(payment.getPaymentAmount()); break;
                    case "ALIPAY": order.setAlipayAmount(payment.getPaymentAmount()); break;
                    case "WECHAT": order.setWechatAmount(payment.getPaymentAmount()); break;
                    case "AGGREGATE": order.setAggregateAmount(payment.getPaymentAmount()); break;
                    case "ABC": order.setAbcAmount(payment.getPaymentAmount()); break;
                    case "CCB": order.setCcbAmount(payment.getPaymentAmount()); break;
                    case "JD": order.setJdAmount(payment.getPaymentAmount()); break;
                    case "PREPAID": order.setPrepaidAmount(payment.getPaymentAmount()); break;
                    case "TRANSFER": order.setTransferAmount(payment.getPaymentAmount()); break;
                    default: break;
                }
            }
        }

        // 更新状态（结算即记账：写入记账人/记账时间）
        order.setStatus(1); // 已完成
        order.setTotalReceived(totalPayment);
        order.setChangeAmount(changeAmount);
        order.setCombinedPayment(payments != null && payments.size() > 1);
        order.setPaymentMethod(resolvePaymentMethod(payments));
        order.setSettlementStatus("SETTLED");
        order.setSettledAmount(payable);
        order.setCompletedTime(LocalDateTime.now());
        order.setCompletedBy(order.getCashierId() != null ? order.getCashierId() : order.getHandlerId());
        order.setBookkeepingTime(LocalDateTime.now());
        if (!StringUtils.hasText(order.getBookkeeperName())) {
            order.setBookkeeperName(order.getCashierName() != null ? order.getCashierName() : order.getCreatorName());
        }

        // 会员积分闭环：产生积分（1 元 = 1 分，向下取整）/ 使用积分，写回会员档案
        applyMemberPoints(order);
        updateById(order);

        // 库存处理（同事务，异常时整体回滚）
        // NORMAL 正常销售：扣减库存（库存不足抛异常）；RETURN 退货：回补库存（数量取绝对值口径）
        if (order.getWarehouseId() != null) {
            boolean isReturn = "RETURN".equals(order.getSaleType());
            List<RetailOrderItem> items = listItemsByOrderId(orderId);
            for (RetailOrderItem item : items) {
                if (item.getProductId() == null || item.getQuantity() == null
                        || item.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                BigDecimal quantity = item.getQuantity().abs();
                // 统一走 WMS 唯一写入口（发事件），不再直写 erp_stock：此前只写 ERP 轨、不动 wms_inventory，
                // 会造成两轨单向漂移（2026-09-20 修复）。库存不足由 WMS 侧抛异常并回滚本事务。
                InventoryChangeEvent.ChangeType changeType = isReturn
                        ? InventoryChangeEvent.ChangeType.INCREASE
                        : InventoryChangeEvent.ChangeType.DECREASE;
                eventPublisher.publishEvent(new InventoryChangeEvent(
                        changeType, item.getProductId(), order.getWarehouseId(), null,
                        null, quantity, "RETAIL_ORDER", orderId, order.getRetailNo(), null, null));
            }
        } else {
            log.warn("零售单 {} 未指定仓库，跳过库存处理", orderId);
        }

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hold(Long orderId) {
        RetailOrder order = getById(orderId);
        if (order == null) throw BusinessException.notFound("零售单不存在");
        if (order.getStatus() != 0) throw BusinessException.badRequest("只有草稿状态才能挂单");
        order.setStatus(2); // 挂单
        order.setHoldOrderFlag(true);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unhold(Long orderId) {
        RetailOrder order = getById(orderId);
        if (order == null) throw BusinessException.notFound("零售单不存在");
        if (order.getStatus() != 2) throw BusinessException.badRequest("只有挂单状态才能取单");
        order.setStatus(0); // 恢复草稿
        order.setHoldOrderFlag(false);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidOrder(Long orderId, String reason) {
        RetailOrder order = getById(orderId);
        if (order == null) throw BusinessException.notFound("零售单不存在");
        if (order.getStatus() == 3) throw BusinessException.badRequest("零售单已作废");
        order.setStatus(3); // 已作废
        if (StringUtils.hasText(reason)) {
            order.setInternalNote((order.getInternalNote() != null ? order.getInternalNote() + "; " : "") + "作废原因: " + reason);
        }
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetailOrder copyOrder(Long orderId) {
        RetailOrder source = getById(orderId);
        if (source == null) throw BusinessException.notFound("零售单不存在");

        // 复制主表
        RetailOrder newOrder = new RetailOrder();
        org.springframework.beans.BeanUtils.copyProperties(source, newOrder, "id", "retailNo", "status", "createTime", "updateTime", "createBy", "updateBy", "deleted", "versionNo", "printCount", "holdOrderFlag");
        newOrder.setStatus(0); // 草稿
        newOrder.setGenerationMethod("复制");
        newOrder.setSourceBillNo(source.getRetailNo());
        newOrder.setOrderDate(LocalDate.now());
        newOrder.setPrintCount(0);
        newOrder.setHoldOrderFlag(false);

        // 复制明细
        List<RetailOrderItem> sourceItems = listItemsByOrderId(orderId);
        return saveWithItems(newOrder, sourceItems.stream().map(si -> {
            RetailOrderItem ni = new RetailOrderItem();
            org.springframework.beans.BeanUtils.copyProperties(si, ni, "id", "orderId", "createTime", "updateTime", "createBy", "updateBy", "deleted");
            return ni;
        }).collect(Collectors.toList()));
    }

    @Override
    public List<RetailOrder> listHoldOrders(Long warehouseId) {
        LambdaQueryWrapper<RetailOrder> wrapper = new LambdaQueryWrapper<RetailOrder>()
                .eq(RetailOrder::getDeleted, 0)
                .eq(RetailOrder::getStatus, 2)
                .eq(RetailOrder::getHoldOrderFlag, true)
                .eq(warehouseId != null, RetailOrder::getWarehouseId, warehouseId)
                .orderByDesc(RetailOrder::getCreateTime);
        return list(wrapper);
    }

    @Override
    public void incrementPrintCount(Long orderId) {
        LambdaUpdateWrapper<RetailOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RetailOrder::getId, orderId)
               .setSql("print_count = COALESCE(print_count, 0) + 1")
               .set(RetailOrder::getPrintTime, LocalDateTime.now());
        update(wrapper);
    }

    @Override
    public List<Map<String, Object>> quickSearchProducts(String keyword, Long warehouseId) {
        // 查询产品表
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getDeleted, 0)
                .and(w -> w.like(Product::getProductName, keyword)
                        .or().like(Product::getProductCode, keyword)
                        .or().like(Product::getBarcode, keyword)
                        .or().like(Product::getSku, keyword))
                .last("LIMIT 20");
        List<Product> products = productMapper.selectList(wrapper);
        return products.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("productName", p.getProductName());
            map.put("productCode", p.getProductCode());
            map.put("barcode", p.getBarcode());
            map.put("specification", p.getSpec());
            map.put("model", p.getModel());
            map.put("origin", p.getOrigin());
            map.put("brand", p.getBrand());
            map.put("unit", p.getUnit());
            map.put("unitPrice", p.getStandardPrice());
            map.put("retailPrice", p.getRetailPrice());
            map.put("wholesalePrice", p.getWholesalePrice());
            map.put("costPrice", p.getCostPrice());
            map.put("imageUrl", p.getImageUrl());
            if (warehouseId != null) {
                map.put("availableStock", getAvailableStock(p.getId(), warehouseId));
            } else {
                map.put("availableStock", BigDecimal.ZERO);
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteOrder(Long id) {
        RetailOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("零售单不存在");
        if (order.getStatus() != 0) throw BusinessException.badRequest("只有草稿状态才能删除");
        // 软删除主表
        order.setDeleted(1);
        updateById(order);
        // 软删除明细
        LambdaUpdateWrapper<RetailOrderItem> itemDelWrapper = new LambdaUpdateWrapper<>();
        itemDelWrapper.eq(RetailOrderItem::getOrderId, id)
                      .set(RetailOrderItem::getDeleted, 1);
        retailOrderItemMapper.update(null, itemDelWrapper);
        // 软删除支付记录
        LambdaUpdateWrapper<RetailOrderPayment> payDelWrapper = new LambdaUpdateWrapper<>();
        payDelWrapper.eq(RetailOrderPayment::getOrderId, id)
                     .set(RetailOrderPayment::getDeleted, 1);
        retailOrderPaymentMapper.update(null, payDelWrapper);
    }

    // ── 内部工具方法 ──

    /**
     * 主表公共查询条件（按单据 / 按明细共用，保证两 Tab 口径一致）
     * 经手人/部门/仓库支持按名称模糊（列表搜索框为文本输入，落库字段即名称快照）
     */
    private LambdaQueryWrapper<RetailOrder> buildDocWrapper(RetailQueryDTO query) {
        LambdaQueryWrapper<RetailOrder> wrapper = new LambdaQueryWrapper<RetailOrder>()
                .eq(RetailOrder::getDeleted, 0)
                .ge(StringUtils.hasText(query.getDateStart()), RetailOrder::getOrderDate, parseSqlDate(query.getDateStart()))
                .le(StringUtils.hasText(query.getDateEnd()), RetailOrder::getOrderDate, parseSqlDate(query.getDateEnd()))
                .like(StringUtils.hasText(query.getRetailNo()), RetailOrder::getRetailNo, query.getRetailNo())
                .like(StringUtils.hasText(query.getCustomerName()), RetailOrder::getCustomerName, query.getCustomerName())
                .eq(query.getCustomerId() != null, RetailOrder::getCustomerId, query.getCustomerId())
                .eq(query.getHandlerId() != null, RetailOrder::getHandlerId, query.getHandlerId())
                .eq(query.getDepartmentId() != null, RetailOrder::getDepartmentId, query.getDepartmentId())
                .eq(query.getWarehouseId() != null, RetailOrder::getWarehouseId, query.getWarehouseId())
                .eq(query.getStatus() != null, RetailOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getSaleType()), RetailOrder::getSaleType, query.getSaleType())
                .like(StringUtils.hasText(query.getRemark()), RetailOrder::getRemark, query.getRemark())
                .like(StringUtils.hasText(query.getCreatorName()), RetailOrder::getCreatorName, query.getCreatorName())
                .like(StringUtils.hasText(query.getBookkeeperName()), RetailOrder::getBookkeeperName, query.getBookkeeperName())
                .like(StringUtils.hasText(query.getMemberCardNo()), RetailOrder::getMemberCardNo, query.getMemberCardNo())
                // 默认不显示已作废（红冲），除非显式要求
                .ne(!Boolean.TRUE.equals(query.getShowRedFlush()), RetailOrder::getStatus, 3)
                .in(query.getMatchedOrderIds() != null, RetailOrder::getId,
                        query.getMatchedOrderIds() != null ? query.getMatchedOrderIds() : Collections.emptyList());
        if (query.getPrintCount() != null) {
            wrapper.eq(RetailOrder::getPrintCount, query.getPrintCount());
        }
        if (query.getExtNum1() != null) {
            wrapper.eq(RetailOrder::getExtNum1, query.getExtNum1());
        }
        if (query.getExtNum2() != null) {
            wrapper.eq(RetailOrder::getExtNum2, query.getExtNum2());
        }
        if (StringUtils.hasText(query.getExtText1())) {
            wrapper.like(RetailOrder::getExtText1, query.getExtText1());
        }
        if (StringUtils.hasText(query.getExtText2())) {
            wrapper.like(RetailOrder::getExtText2, query.getExtText2());
        }
        if (StringUtils.hasText(query.getExtText3())) {
            wrapper.like(RetailOrder::getExtText3, query.getExtText3());
        }
        return wrapper;
    }

    /**
     * 主表公共查询条件（按明细 Tab 口径：无下单员/记账人/会员卡号/自定义字段）
     */
    private LambdaQueryWrapper<RetailOrder> buildDocWrapper(RetailDetailQueryDTO query) {
        return new LambdaQueryWrapper<RetailOrder>()
                .eq(RetailOrder::getDeleted, 0)
                .ge(StringUtils.hasText(query.getDateStart()), RetailOrder::getOrderDate, parseSqlDate(query.getDateStart()))
                .le(StringUtils.hasText(query.getDateEnd()), RetailOrder::getOrderDate, parseSqlDate(query.getDateEnd()))
                .like(StringUtils.hasText(query.getRetailNo()), RetailOrder::getRetailNo, query.getRetailNo())
                .like(StringUtils.hasText(query.getCustomerName()), RetailOrder::getCustomerName, query.getCustomerName())
                .eq(query.getHandlerId() != null, RetailOrder::getHandlerId, query.getHandlerId())
                .like(StringUtils.hasText(query.getHandlerName()), RetailOrder::getHandlerName, query.getHandlerName())
                .eq(query.getDepartmentId() != null, RetailOrder::getDepartmentId, query.getDepartmentId())
                .like(StringUtils.hasText(query.getDepartmentName()), RetailOrder::getDepartmentName, query.getDepartmentName())
                .eq(query.getWarehouseId() != null, RetailOrder::getWarehouseId, query.getWarehouseId())
                .like(StringUtils.hasText(query.getWarehouseName()), RetailOrder::getWarehouseName, query.getWarehouseName())
                .eq(query.getStatus() != null, RetailOrder::getStatus, query.getStatus())
                .ne(!Boolean.TRUE.equals(query.getShowRedFlush()), RetailOrder::getStatus, 3);
    }

    /**
     * 按商品行属性反查命中单据ID（商品行属性为明细字段）
     */
    private List<Long> findOrderIdsByProductAttribute(String productAttribute) {
        return retailOrderItemMapper.selectList(new LambdaQueryWrapper<RetailOrderItem>()
                        .select(RetailOrderItem::getOrderId)
                        .eq(RetailOrderItem::getDeleted, 0)
                        .like(RetailOrderItem::getProductAttribute, productAttribute))
                .stream().map(RetailOrderItem::getOrderId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    /**
     * 由收款明细推导主表支付方式：单方式原样存，多方式记 MIXED
     */
    private String resolvePaymentMethod(List<RetailOrderPayment> payments) {
        if (payments == null || payments.isEmpty()) return "CASH";
        Set<String> methods = payments.stream()
                .filter(p -> p.getPaymentAmount() != null && p.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(RetailOrderPayment::getPaymentMethod)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (methods.isEmpty()) return "CASH";
        if (methods.size() == 1) return methods.iterator().next();
        return "MIXED";
    }

    /**
     * 会员积分闭环：
     * 产生积分 = 应收金额（1 元 = 1 分，向下取整；单据显式传入时以单据为准）
     * 使用积分 = 单据传入值（不能超过会员当前可用积分）
     * 结果写回 erp_retail_order（此前/当前积分）。
     *
     * <p>⚠️ **2026-09-27 改口径：余额的权威载体是积分台账，不再是 `biz_party.points`。**</p>
     * 行业口径（多方一致）：**绝不把可变余额存在主档上** —— 那样会同时坏在四处：
     * 重试重复发放、并发丢更新、"我的积分去哪了"答不出来、出错没法安全更正。
     * 正确形态是「**只追加台账 + 派生余额**」；本仓台账（`mkt_points_batch` 批次 +
     * `mkt_points_journal` 流水，FIFO + 到期）**早就实现了，只是主链路一直绕过它**。
     *
     * <p>两处细节不是随手写的：</p>
     * <ol>
     *   <li><b>按主体（`partner_id`）而不是按卡号</b>：本方法手里只有 `order.getCustomerId()`
     *       （= party id），**没有卡号**；而旧数据里存在"有积分没卡"的主体 ⇒ 台账的卡号版 API
     *       （`earn` 第一行就 `memberCardNo == null → return null`）在这里用不了。</li>
     *   <li><b>先扣后赚</b>：先用掉本单要抵扣的分，再发放本单产生的分 —— 免得出现
     *       "用掉刚产生的那一笔"这种说不清的边界；可用额度校验也在扣减之前做。</li>
     * </ol>
     * <p>幂等：同一张零售单（`retailNo`）重复调用不会重复入账（台账侧按业务事件判重、
     * 数据库唯一索引兜底），所以这里不需要额外的状态判断。</p>
     */
    private void applyMemberPoints(RetailOrder order) {
        if (order.getCustomerId() == null) return;
        Party member = partyMapper.selectById(order.getCustomerId());
        if (member == null) return;

        BigDecimal payable = order.getPayableAmount() != null ? order.getPayableAmount() : BigDecimal.ZERO;
        BigDecimal generated = order.getMemberGeneratedPoints();
        if (generated == null || generated.compareTo(BigDecimal.ZERO) <= 0) {
            generated = payable.setScale(0, java.math.RoundingMode.DOWN);
        }
        BigDecimal used = order.getMemberUsedPoints() != null ? order.getMemberUsedPoints() : BigDecimal.ZERO;
        if (used.compareTo(BigDecimal.ZERO) < 0) used = BigDecimal.ZERO;

        // 可用积分以**台账**为准（不再读 biz_party.points）
        BigDecimal available = pointsLedgerService.availableByPartner(member.getId());
        if (used.compareTo(available) > 0) {
            throw BusinessException.badRequest(String.format("会员积分不足: 可用%s，本次使用%s",
                    available.stripTrailingZeros().toPlainString(), used.stripTrailingZeros().toPlainString()));
        }

        String billNo = order.getRetailNo();
        if (used.compareTo(BigDecimal.ZERO) > 0) {
            pointsLedgerService.useByPartner(member.getId(), used, billNo);
        }
        if (generated.compareTo(BigDecimal.ZERO) > 0) {
            pointsLedgerService.earnByPartner(member.getId(), generated, "RETAIL", billNo);
        }
        BigDecimal current = pointsLedgerService.availableByPartner(member.getId());

        order.setPrevPoints(available.intValue());
        order.setMemberGeneratedPoints(generated);
        order.setMemberUsedPoints(used);
        order.setCurrentPoints(current);
    }

    /**
     * 将日期字符串转为 java.sql.Date，确保 MyBatis-Plus 参数绑定时使用正确的 JDBC 类型
     */
    private java.sql.Date parseSqlDate(String dateString) {
        if (!StringUtils.hasText(dateString)) return null;
        try {
            return java.sql.Date.valueOf(LocalDate.parse(dateString));
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateString);
            return null;
        }
    }

    /**
     * 填充快照字段（查询 biz_party 和 product 表）
     */
    private void buildSnapshots(RetailOrder order, List<RetailOrderItem> items) {
        // 主表快照：从 biz_party 获取客户信息
        if (order.getCustomerId() != null) {
            Party customer = partyMapper.selectById(order.getCustomerId());
            if (customer != null) {
                order.setCustomerName(customer.getPartyName());
                order.setCustomerCode(customer.getPartyCode());
                order.setCustomerLevel(customer.getPartyLevel());
                order.setBankName(customer.getBankName());
                order.setBankAccount(customer.getBankAccount());
                order.setTaxNo(customer.getTaxNumber());
                order.setMemberCardNo(customer.getMemberCardNo());
            }
        }
        // 明细快照：从 product 获取商品信息
        if (items != null) {
            for (RetailOrderItem item : items) {
                if (item.getProductId() != null) {
                    Product product = productMapper.selectById(item.getProductId());
                    if (product != null) {
                        item.setProductName(product.getProductName());
                        item.setProductCode(product.getProductCode());
                        item.setBarcode(product.getBarcode());
                        item.setSpecification(product.getSpec());
                        item.setModel(product.getModel());
                        item.setOrigin(product.getOrigin());
                        item.setBrand(product.getBrand());
                        item.setUnit(product.getUnit());
                        item.setRetailPrice(product.getRetailPrice());
                        item.setWholesalePrice(product.getWholesalePrice());
                        item.setCostPrice(product.getCostPrice());
                        item.setImageUrl(product.getImageUrl());
                        // 8个价格等级使用统一的 retailPrice
                        item.setPriceRestaurant(product.getRetailPrice());
                        item.setPriceCanteen(product.getRetailPrice());
                        item.setPriceVipSelf(product.getRetailPrice());
                        item.setPriceLargeGroup(product.getRetailPrice());
                        item.setPriceSpecialCustomer(product.getRetailPrice());
                        item.setPriceOutRestaurant(product.getRetailPrice());
                        item.setPriceVipLevel1(product.getRetailPrice());
                        item.setPriceVipLevel2(product.getRetailPrice());
                    }
                }
                // 计算金额（如果未传入）
                if (item.getAmount() == null && item.getQuantity() != null && item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                }
                if (item.getLineAmount() == null) {
                    item.setLineAmount(item.getAmount());
                }
            }
        }
    }

    /**
     * 查询可用库存
     */
    private BigDecimal getAvailableStock(Long productId, Long warehouseId) {
        if (productId == null) return BigDecimal.ZERO;
        try {
            Stock stock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getProductId, productId)
                            .eq(warehouseId != null, Stock::getWarehouseId, warehouseId)
                            .last("LIMIT 1")
            );
            if (stock != null && stock.getQuantity() != null) {
                BigDecimal frozen = stock.getFrozenQuantity() != null ? stock.getFrozenQuantity() : BigDecimal.ZERO;
                return stock.getQuantity().subtract(frozen);
            }
        } catch (Exception e) {
            log.warn("查询库存失败: productId={}, warehouseId={}, error={}", productId, warehouseId, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /**
     * 计算金额
     */
    private void calculateAmounts(RetailOrder order, List<RetailOrderItem> items) {
        // 计算商品总金额
        BigDecimal totalAmount = items.stream()
                .map(i -> {
                    BigDecimal qty = i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO;
                    BigDecimal price = i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal amt = qty.multiply(price);
                    i.setAmount(amt);
                    if (i.getLineAmount() == null) i.setLineAmount(amt);
                    return amt;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setAmount(totalAmount);
        order.setTotalQuantity(items.stream()
                .map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        order.setBoxCount(items.size());

        // 计算应付金额
        BigDecimal discount = (order.getDirectDiscount() != null ? order.getDirectDiscount() : BigDecimal.ZERO)
                .add(order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO)
                .add(order.getPromoDiscount() != null ? order.getPromoDiscount() : BigDecimal.ZERO);
        BigDecimal otherFee = order.getOtherFee() != null ? order.getOtherFee() : BigDecimal.ZERO;
        BigDecimal roundingAmount = order.getRoundingAmount() != null ? order.getRoundingAmount() : BigDecimal.ZERO;
        BigDecimal payableAmount = totalAmount.subtract(discount).add(otherFee).subtract(roundingAmount);
        if (order.getPayableAmount() == null || order.getPayableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            order.setPayableAmount(payableAmount);
        }
    }
}
