package cn.aiedge.erp.sale.preorder.service;

import cn.aiedge.erp.sale.preorder.dto.SalePreOrderDetailDTO;
import cn.aiedge.erp.sale.preorder.entity.SalePreOrder;
import cn.aiedge.erp.sale.preorder.entity.SalePreOrderItem;
import cn.aiedge.erp.sale.preorder.mapper.SalePreOrderItemMapper;
import cn.aiedge.erp.sale.preorder.mapper.SalePreOrderMapper;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.entity.SaleOrderItem;
import cn.aiedge.erp.sale.mapper.SaleOrderItemMapper;
import cn.aiedge.erp.sale.mapper.SaleOrderMapper;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.mapper.WarehouseMapper;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class SalePreOrderServiceImpl extends ServiceImpl<SalePreOrderMapper, SalePreOrder>
        implements SalePreOrderService {

    private final SalePreOrderItemMapper itemMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final PartyMapper partyMapper;
    private final BizNumberGeneratorService bizNumberGeneratorService;
    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;

    @Override
    public Page<SalePreOrder> pageList(String keyword, Long customerId, String customerName,
                                       String handlerName, String deptName,
                                       Integer[] status, Integer settlementStatus,
                                       String depositDeadlineStart, String depositDeadlineEnd,
                                       String startDate, String endDate,
                                       String warehouseName, String creatorName,
                                       String auditorName, Integer saleType, String remark,
                                       BigDecimal extNum1, BigDecimal extNum2,
                                       String extText1, String extText2, String extText3,
                                       int pageNum, int pageSize) {
        LambdaQueryWrapper<SalePreOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalePreOrder::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SalePreOrder::getOrderNo, keyword)
                    .or().like(SalePreOrder::getCustomerName, keyword));
        }
        if (customerId != null) wrapper.eq(SalePreOrder::getCustomerId, customerId);
        if (StringUtils.hasText(customerName)) wrapper.like(SalePreOrder::getCustomerName, customerName);
        if (StringUtils.hasText(handlerName)) wrapper.like(SalePreOrder::getHandlerName, handlerName);
        if (StringUtils.hasText(deptName)) wrapper.like(SalePreOrder::getDeptName, deptName);
        if (StringUtils.hasText(warehouseName)) wrapper.like(SalePreOrder::getWarehouseName, warehouseName);
        if (StringUtils.hasText(creatorName)) wrapper.like(SalePreOrder::getCreatorName, creatorName);
        if (StringUtils.hasText(auditorName)) wrapper.like(SalePreOrder::getAuditorName, auditorName);
        if (saleType != null) wrapper.eq(SalePreOrder::getSaleType, saleType);
        if (StringUtils.hasText(remark)) wrapper.like(SalePreOrder::getRemark, remark);
        if (extNum1 != null) wrapper.eq(SalePreOrder::getExtNum1, extNum1);
        if (extNum2 != null) wrapper.eq(SalePreOrder::getExtNum2, extNum2);
        if (StringUtils.hasText(extText1)) wrapper.like(SalePreOrder::getExtText1, extText1);
        if (StringUtils.hasText(extText2)) wrapper.like(SalePreOrder::getExtText2, extText2);
        if (StringUtils.hasText(extText3)) wrapper.like(SalePreOrder::getExtText3, extText3);
        if (status != null && status.length > 0) wrapper.in(SalePreOrder::getStatus, Arrays.asList(status));
        if (settlementStatus != null) wrapper.eq(SalePreOrder::getSettlementStatus, settlementStatus);
        if (StringUtils.hasText(depositDeadlineStart))
            wrapper.ge(SalePreOrder::getDepositDeadline, LocalDate.parse(depositDeadlineStart));
        if (StringUtils.hasText(depositDeadlineEnd))
            wrapper.le(SalePreOrder::getDepositDeadline, LocalDate.parse(depositDeadlineEnd));
        if (StringUtils.hasText(startDate)) wrapper.ge(SalePreOrder::getOrderDate, LocalDate.parse(startDate));
        if (StringUtils.hasText(endDate)) wrapper.le(SalePreOrder::getOrderDate, LocalDate.parse(endDate));

        wrapper.orderByDesc(SalePreOrder::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Page<Map<String, Object>> pageDetail(String keyword, Long customerId, String customerName,
                                                 String handlerName, String deptName,
                                                 String orderNo, Integer[] status,
                                                 Integer settlementStatus, Long categoryId,
                                                 String startDate, String endDate,
                                                 String creatorName, String auditorName,
                                                 Integer saleType, String productAttribute,
                                                 String remark, String itemRemark, Boolean gift,
                                                 int pageNum, int pageSize) {
        // 使用数据库层JOIN分页查询（符合生产级ERP标准）
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        List<Integer> statusList = status != null ? Arrays.asList(status) : null;

        // 调用自定义Mapper的JOIN查询
        // tenant_id 由 MyBatis-Plus TenantLineInnerInterceptor 自动注入
        IPage<SalePreOrderDetailDTO> result = itemMapper.selectDetailPage(
            page,
            keyword,
            customerId,
            customerName,
            handlerName,
            deptName,
            orderNo,
            statusList,
            settlementStatus,
            startDate,
            endDate,
            productAttribute,
            gift,
            creatorName,
            auditorName,
            saleType,
            remark,
            itemRemark
        );

        // 转换为Map格式
        List<Map<String, Object>> records = result.getRecords().stream()
            .map(dto -> {
                Map<String, Object> map = new LinkedHashMap<>();
                // 使用BeanUtils或手动映射
                map.put("id", dto.getId());
                map.put("orderId", dto.getOrderId());
                map.put("lineNo", dto.getLineNo());
                map.put("orderNo", dto.getOrderNo());
                map.put("orderDate", dto.getOrderDate());
                map.put("status", dto.getStatus());
                map.put("customerId", dto.getCustomerId());
                map.put("customerName", dto.getCustomerName());
                map.put("customerCode", dto.getCustomerCode());
                map.put("customerLevel", dto.getCustomerLevel());
                map.put("warehouseName", dto.getWarehouseName());
                map.put("handlerName", dto.getHandlerName());
                map.put("deptName", dto.getDeptName());
                map.put("saleType", dto.getSaleType());
                map.put("receiverName", dto.getReceiverName());
                map.put("receiverPhone", dto.getReceiverPhone());
                map.put("shippingAddress", dto.getShippingAddress());
                map.put("productId", dto.getProductId());
                map.put("productName", dto.getProductName());
                map.put("productCode", dto.getProductCode());
                map.put("barcode", dto.getBarcode());
                map.put("specification", dto.getSpecification());
                map.put("model", dto.getModel());
                map.put("origin", dto.getOrigin());
                map.put("brand", dto.getBrand());
                map.put("unit", dto.getUnit());
                map.put("pricingUnit", dto.getPricingUnit());
                map.put("smallUnit", dto.getSmallUnit());
                map.put("quantity", dto.getQuantity());
                map.put("unitPrice", dto.getUnitPrice());
                map.put("amount", dto.getAmount());
                map.put("discountRate", dto.getDiscountRate());
                map.put("discountedPrice", dto.getDiscountedPrice());
                map.put("discountedAmount", dto.getDiscountedAmount());
                map.put("remark", dto.getRemark());
                map.put("productAttribute", dto.getProductAttribute());
                map.put("gift", dto.getGift());
                map.put("creatorName", dto.getCreatorName());
                map.put("auditorName", dto.getAuditorName());
                map.put("submitTime", dto.getSubmitTime());
                map.put("summary", dto.getSummary());
                map.put("attachment", dto.getAttachment());
                map.put("customerTicket", dto.getCustomerTicket());
                map.put("customerRemark", dto.getCustomerRemark());
                map.put("orderRemark", dto.getOrderRemark());
                // 明细数量字段
                map.put("pieceQuantity", dto.getPieceQuantity());
                map.put("bigPack", dto.getBigPack());
                map.put("midPack", dto.getMidPack());
                map.put("smallPack", dto.getSmallPack());
                map.put("orderedQuantity", dto.getOrderedQuantity());
                map.put("unOrderedQuantity", dto.getUnOrderedQuantity());
                map.put("shippedQuantity", dto.getShippedQuantity());
                map.put("unShippedQuantity", dto.getUnShippedQuantity());
                map.put("terminateQuantity", dto.getTerminateQuantity());
                map.put("terminateAmount", dto.getTerminateAmount());
                // 明细价格字段
                map.put("smallUnitPrice", dto.getSmallUnitPrice());
                map.put("smallUnitQuantity", dto.getSmallUnitQuantity());
                map.put("conversionRelation", dto.getConversionRelation());
                map.put("conversionResult", dto.getConversionResult());
                // 明细库存字段
                map.put("availableStock", dto.getAvailableStock());
                map.put("availableStockConversion", dto.getAvailableStockConversion());
                map.put("bookStock", dto.getBookStock());
                map.put("region", dto.getRegion());
                map.put("location", dto.getLocation());
                // 明细成本毛利
                map.put("costPrice", dto.getCostPrice());
                map.put("costAmount", dto.getCostAmount());
                map.put("grossProfit", dto.getGrossProfit());
                map.put("volume", dto.getVolume());
                map.put("weight", dto.getWeight());
                // 明细价格参考
                map.put("lastSaleDate", dto.getLastSaleDate());
                map.put("retailPrice", dto.getRetailPrice());
                map.put("wholesalePrice", dto.getWholesalePrice());
                map.put("minSalePrice", dto.getMinSalePrice());
                // 明细图片
                map.put("imageUrl", dto.getImageUrl());
                // 明细价格等级
                map.put("priceLevel1", dto.getPriceLevel1());
                map.put("priceLevel2", dto.getPriceLevel2());
                map.put("priceLevel3", dto.getPriceLevel3());
                map.put("priceLevel4", dto.getPriceLevel4());
                map.put("priceLevel5", dto.getPriceLevel5());
                map.put("priceLevel6", dto.getPriceLevel6());
                map.put("priceLevel7", dto.getPriceLevel7());
                map.put("priceLevel8", dto.getPriceLevel8());
                // 明细自定义字段
                map.put("extNum1", dto.getExtNum1());
                map.put("extNum2", dto.getExtNum2());
                map.put("extNum3", dto.getExtNum3());
                map.put("extNum4", dto.getExtNum4());
                map.put("extNum5", dto.getExtNum5());
                map.put("extText1", dto.getExtText1());
                map.put("extText2", dto.getExtText2());
                map.put("extPartner", dto.getExtPartner());
                map.put("extStaff", dto.getExtStaff());
                map.put("extDept", dto.getExtDept());
                // 主表状态别名(前端按明细tab用)
                map.put("orderStatus", dto.getStatus());
                map.put("orderRemark", dto.getSummary());
                return map;
            })
            .collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(pageNum, pageSize, result.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public SalePreOrder getDetail(Long id) {
        SalePreOrder order = getById(id);
        if (order == null) return null;
        return order;
    }

    @Override
    @Transactional
    public void submit(Long id) {
        SalePreOrder order = getById(id);
        if (order == null) throw new RuntimeException("预订货单不存在");
        if (order.getStatus() != 0) throw new RuntimeException("只有草稿状态可以提交");

        order.setStatus(1);
        order.setSubmitBy(StpUtil.getLoginIdAsLong());
        order.setSubmitTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void approve(Long id) {
        SalePreOrder order = getById(id);
        if (order == null) throw new RuntimeException("预订货单不存在");
        if (order.getStatus() != 1) throw new RuntimeException("只有审核中状态可以审批");

        order.setStatus(2);
        order.setApprovedBy(StpUtil.getLoginIdAsLong());
        order.setApprovedTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public SalePreOrder validateAndCreate(SalePreOrder order, List<SalePreOrderItem> items) {
        // 1. 验证客户是否存在
        validateCustomer(order.getCustomerId());

        // 2. 验证仓库是否存在
        if (order.getWarehouseId() != null) {
            validateWarehouse(order.getWarehouseId());
        }

        // 3. 验证明细中的商品是否存在
        if (items != null && !items.isEmpty()) {
            for (SalePreOrderItem item : items) {
                validateProduct(item.getProductId());
            }
        }

        // 4. 生产级ERP: 信用额度校验
        if (order.getOrderAmount() != null && order.getOrderAmount().compareTo(BigDecimal.ZERO) > 0
                && order.getCustomerId() != null) {
            Party party = partyMapper.selectById(order.getCustomerId());
            if (party != null && party.getCreditLimit() != null && party.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentDebt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
                BigDecimal availableCredit = party.getCreditLimit().subtract(currentDebt).max(BigDecimal.ZERO);
                if (order.getOrderAmount().compareTo(availableCredit) > 0) {
                    throw new RuntimeException(String.format(
                            "客户「%s」信用额度不足：额度 %.2f，已欠款 %.2f，可用 %.2f，本单金额 %.2f",
                            party.getPartyName(), party.getCreditLimit(), currentDebt, availableCredit, order.getOrderAmount()));
                }
            }
        }

        // 5. 设置初始状态
        if (order.getStatus() == null) {
            order.setStatus(0); // 草稿状态
        }
        order.setPrintCount(0);

        // 6. 保存主表
        save(order);

        // 7. 保存明细
        if (items != null && !items.isEmpty()) {
            int lineNo = 1;
            for (SalePreOrderItem item : items) {
                item.setOrderId(order.getId());
                item.setLineNo(lineNo++);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                itemMapper.insert(item);
            }
        }

        return order;
    }

    @Override
    @Transactional
    public void validateAndUpdate(Long id, SalePreOrder order, List<SalePreOrderItem> items) {
        // 1. 检查订单是否存在
        SalePreOrder existingOrder = getById(id);
        if (existingOrder == null) {
            throw new RuntimeException("预订货单不存在");
        }

        // 2. 检查状态：只有草稿状态(0)可以修改
        if (existingOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的订单可以修改，当前状态：" + getStatusName(existingOrder.getStatus()));
        }

        // 3. 验证客户
        validateCustomer(order.getCustomerId());

        // 4. 验证仓库
        if (order.getWarehouseId() != null) {
            validateWarehouse(order.getWarehouseId());
        }

        // 5. 验证明细中的商品
        if (items != null && !items.isEmpty()) {
            for (SalePreOrderItem item : items) {
                validateProduct(item.getProductId());
            }
        }

        // 6. 生产级ERP: 信用额度校验
        if (order.getOrderAmount() != null && order.getOrderAmount().compareTo(BigDecimal.ZERO) > 0
                && order.getCustomerId() != null) {
            Party party = partyMapper.selectById(order.getCustomerId());
            if (party != null && party.getCreditLimit() != null && party.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentDebt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
                BigDecimal availableCredit = party.getCreditLimit().subtract(currentDebt).max(BigDecimal.ZERO);
                if (order.getOrderAmount().compareTo(availableCredit) > 0) {
                    throw new RuntimeException(String.format(
                            "客户「%s」信用额度不足：额度 %.2f，已欠款 %.2f，可用 %.2f，本单金额 %.2f",
                            party.getPartyName(), party.getCreditLimit(), currentDebt, availableCredit, order.getOrderAmount()));
                }
            }
        }

        // 7. 更新主表
        order.setId(id);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        // 8. 更新明细：先删除旧的，再插入新的
        LambdaQueryWrapper<SalePreOrderItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SalePreOrderItem::getOrderId, id);
        itemMapper.delete(deleteWrapper);

        if (items != null && !items.isEmpty()) {
            int lineNo = 1;
            for (SalePreOrderItem item : items) {
                item.setId(null); // 让数据库生成新ID
                item.setOrderId(id);
                item.setLineNo(lineNo++);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                itemMapper.insert(item);
            }
        }
    }

    // ─── 私有验证方法（外键验证，确保数据引用完整性） ───

    private void validateCustomer(Long customerId) {
        if (customerId == null) {
            throw new RuntimeException("客户不能为空");
        }
        Party party = partyMapper.selectById(customerId);
        if (party == null) {
            throw new RuntimeException("客户不存在：" + customerId);
        }
    }

    private void validateWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            return; // 仓库可选
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在：" + warehouseId);
        }
    }

    private void validateProduct(Long productId) {
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在：" + productId);
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "审核中";
            case 2: return "待订货";
            case 3: return "部分订货";
            case 4: return "已订货";
            case 5: return "已完成";
            case -1: return "已取消";
            default: return "未知(" + status + ")";
        }
    }

    @Override
    @Transactional
    public void batchOrder(Long id) {
        SalePreOrder order = getById(id);
        if (order == null) throw new RuntimeException("预订货单不存在");
        if (order.getStatus() != 2 && order.getStatus() != 3) {
            throw new RuntimeException("只有待订货或部分订货状态的订单可以执行订货操作，当前状态：" + getStatusName(order.getStatus()));
        }

        // 获取预订单明细
        LambdaQueryWrapper<SalePreOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(SalePreOrderItem::getOrderId, id)
                .eq(SalePreOrderItem::getDeleted, 0)
                .orderByAsc(SalePreOrderItem::getLineNo);
        List<SalePreOrderItem> preOrderItems = itemMapper.selectList(itemWrapper);
        if (preOrderItems == null || preOrderItems.isEmpty()) {
            throw new RuntimeException("预订货单没有明细行，无法执行订货");
        }

        // 生产级ERP: 创建正式销售订单（完整字段映射，确保业务实体间数据贯通）
        SaleOrder saleOrder = new SaleOrder();
        String saleOrderNo = bizNumberGeneratorService.nextSaleOrderNo();
        saleOrder.setOrderNo(saleOrderNo);
        saleOrder.setOrderDate(order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now());
        saleOrder.setStatus(0); // 草稿
        saleOrder.setSaleType(order.getSaleType());
        // 客户信息
        saleOrder.setCustomerId(order.getCustomerId());
        saleOrder.setCustomerName(order.getCustomerName());
        saleOrder.setCustomerCode(order.getCustomerCode());
        saleOrder.setCustomerLevel(order.getCustomerLevel());
        saleOrder.setCustomerRemark(order.getCustomerRemark());
        saleOrder.setCustomerTicket(order.getCustomerTicket());
        // 银行/税务信息
        saleOrder.setBankName(order.getBankName());
        saleOrder.setBankAccount(order.getBankAccount());
        saleOrder.setTaxNo(order.getTaxNo());
        // 仓库
        saleOrder.setWarehouseId(order.getWarehouseId());
        saleOrder.setWarehouseName(order.getWarehouseName());
        // 经手人/部门
        saleOrder.setSalesmanId(order.getHandlerId());
        saleOrder.setSalesmanName(order.getHandlerName());
        saleOrder.setDeptId(order.getDeptId());
        saleOrder.setDeptName(order.getDeptName());
        // 收货信息
        saleOrder.setReceiverName(order.getReceiverName());
        saleOrder.setReceiverPhone(order.getReceiverPhone());
        saleOrder.setShippingAddress(order.getShippingAddress());
        // 源单关联
        saleOrder.setSourceOrder(order.getOrderNo()); // 记录来源预订单号
        saleOrder.setRemark("由预订货单 " + order.getOrderNo() + " 转换生成");
        // 数量汇总
        saleOrder.setTotalQuantity(order.getUnOrderedQuantity());
        // 金额汇总
        saleOrder.setProductAmount(order.getTotalAmount());
        saleOrder.setBillAmount(order.getOrderAmount());
        saleOrder.setDiscountAmount(order.getDiscountedAmount());
        // 订金信息（生产级ERP：预订单订金带到销售单）
        saleOrder.setDepositAccount(order.getDepositAccount1());
        saleOrder.setDepositAmount(order.getDepositAmount());
        saleOrder.setDepositAccount1(order.getDepositAccount1());
        saleOrder.setDepositAccount2(order.getDepositAccount2());
        saleOrder.setDepositAccount3(order.getDepositAccount3());
        saleOrder.setDepositAccount4(order.getDepositAccount4());
        // 信用额度快照
        saleOrder.setCreditLimit(order.getCreditLimit());
        // 重量/体积
        saleOrder.setTotalWeight(order.getTotalWeight());
        saleOrder.setTotalVolume(order.getTotalVolume());
        // 制单人信息
        saleOrder.setCreatorName(order.getCreatorName());
        saleOrder.setPrintCount(0);
        saleOrder.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        saleOrder.setCreateTime(now);
        saleOrder.setUpdateTime(now);
        saleOrderMapper.insert(saleOrder);

        // 创建销售订单明细
        BigDecimal totalConvertedQty = BigDecimal.ZERO;
        int lineNo = 1;
        for (SalePreOrderItem preItem : preOrderItems) {
            // 计算本次可订货数量 = 预订数量 - 已订数量
            BigDecimal qty = preItem.getQuantity() != null ? preItem.getQuantity() : BigDecimal.ZERO;
            BigDecimal orderedQty = preItem.getOrderedQuantity() != null ? preItem.getOrderedQuantity() : BigDecimal.ZERO;
            BigDecimal thisOrderQty = qty.subtract(orderedQty);
            if (thisOrderQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // 已全部订货，跳过
            }

            // 生产级ERP: 完整字段映射，确保商品信息在实体间完全贯通
            SaleOrderItem saleItem = new SaleOrderItem();
            saleItem.setOrderId(saleOrder.getId());
            saleItem.setLineNo(lineNo++);
            // 商品信息
            saleItem.setProductId(preItem.getProductId());
            saleItem.setProductName(preItem.getProductName());
            saleItem.setProductCode(preItem.getProductCode());
            saleItem.setItemCode(preItem.getProductCode());
            saleItem.setBarcode(preItem.getBarcode());
            saleItem.setSpecification(preItem.getSpecification());
            saleItem.setModel(preItem.getModel());
            saleItem.setOrigin(preItem.getOrigin());
            saleItem.setBrand(preItem.getBrand());
            saleItem.setUnit(preItem.getUnit());
            saleItem.setPricingUnit(preItem.getPricingUnit());
            saleItem.setSmallUnit(preItem.getSmallUnit());
            saleItem.setSmallUnitQuantity(preItem.getSmallUnitQuantity());
            saleItem.setConversionRelation(preItem.getConversionRelation());
            // 数量
            saleItem.setQuantity(thisOrderQty);
            saleItem.setBigPack(preItem.getBigPack());
            saleItem.setMidPack(preItem.getMidPack());
            saleItem.setSmallPack(preItem.getSmallPack());
            // 价格
            saleItem.setUnitPrice(preItem.getUnitPrice());
            saleItem.setAmount(preItem.getUnitPrice() != null ? thisOrderQty.multiply(preItem.getUnitPrice()) : BigDecimal.ZERO);
            saleItem.setSmallUnitPrice(preItem.getSmallUnitPrice());
            saleItem.setDiscountRate(preItem.getDiscountRate());
            saleItem.setDiscountedUnitPrice(preItem.getDiscountedPrice());
            saleItem.setCostPrice(preItem.getCostPrice());
            // 物理属性
            saleItem.setVolume(preItem.getVolume());
            saleItem.setWeight(preItem.getWeight());
            // 赠品
            saleItem.setGift(preItem.getGift());
            // 商品行属性
            saleItem.setLineAttribute(preItem.getProductAttribute());
            saleItem.setArea(preItem.getRegion());
            saleItem.setLocation(preItem.getLocation());
            // 库存参考
            saleItem.setAvailableStock(preItem.getAvailableStock());
            saleItem.setBookStock(preItem.getBookStock());
            // 预订单关联
            saleItem.setPreOrderNo(order.getOrderNo());
            // 备注
            saleItem.setRemark(preItem.getRemark());
            // 时间
            saleItem.setCreateTime(now);
            saleItem.setUpdateTime(now);
            saleOrderItemMapper.insert(saleItem);

            totalConvertedQty = totalConvertedQty.add(thisOrderQty);

            // 更新预订单明细的已订数量
            preItem.setOrderedQuantity((orderedQty != null ? orderedQty : BigDecimal.ZERO).add(thisOrderQty));
            preItem.setUnOrderedQuantity(qty.subtract(preItem.getOrderedQuantity()).max(BigDecimal.ZERO));
            preItem.setUpdateTime(now);
            itemMapper.updateById(preItem);
        }

        // 更新预订单主表：已订数量、未订数量、状态
        BigDecimal preOrderQty = order.getPreOrderQuantity() != null ? order.getPreOrderQuantity() : BigDecimal.ZERO;
        BigDecimal totalOrdered = order.getOrderedQuantity() != null ? order.getOrderedQuantity() : BigDecimal.ZERO;
        totalOrdered = totalOrdered.add(totalConvertedQty);
        BigDecimal unOrdered = preOrderQty.subtract(totalOrdered).max(BigDecimal.ZERO);

        order.setOrderedQuantity(totalOrdered);
        order.setUnOrderedQuantity(unOrdered);
        if (unOrdered.compareTo(BigDecimal.ZERO) > 0) {
            order.setStatus(3); // 部分订货
        } else {
            order.setStatus(4); // 已订货
        }
        order.setUpdateTime(now);
        updateById(order);

        log.info("批量订货完成: 预订单号={}, 生成的销售单号={}, 本次订货数量={}, 已订总数={}, 未订={}",
                order.getOrderNo(), saleOrderNo, totalConvertedQty, totalOrdered, unOrdered);
    }

    @Override
    @Transactional
    public void incrementPrintCount(Long id) {
        SalePreOrder order = getById(id);
        if (order == null) throw new RuntimeException("预订货单不存在");
        order.setPrintCount((order.getPrintCount() != null ? order.getPrintCount() : 0) + 1);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
    }
}
