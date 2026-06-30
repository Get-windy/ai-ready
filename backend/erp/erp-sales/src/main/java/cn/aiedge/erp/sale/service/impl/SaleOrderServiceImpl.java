package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.sale.dto.SaleOrderDTO;
import cn.aiedge.erp.sale.dto.SaleOrderItemDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.entity.SaleOrderItem;
import cn.aiedge.erp.sale.mapper.SaleOrderMapper;
import cn.aiedge.erp.sale.mapper.SaleOrderItemMapper;
import cn.aiedge.erp.sale.service.ISaleOrderService;
import cn.aiedge.erp.stock.service.StockService;
import cn.aiedge.erp.party.service.CustomerGradeService;
import cn.aiedge.erp.party.entity.CustomerGrade;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.service.ProductService;
import cn.aiedge.erp.stock.service.ProductUnitService;
import cn.aiedge.erp.party.service.PartyGradeRelationService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 导入价格引擎相关类
import cn.aiedge.erp.pricing.service.PriceEngineService;
import cn.aiedge.erp.pricing.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.pricing.strategy.entity.PriceCalculationResult;
import cn.aiedge.erp.pricing.strategy.entity.PricingStrategy;
import cn.aiedge.common.serial.BizNumberGeneratorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleOrderServiceImpl extends ServiceImpl<SaleOrderMapper, SaleOrder>
        implements ISaleOrderService {

    private final SaleOrderMapper orderMapper;
    private final SaleOrderItemMapper itemMapper;
    private final StockService stockService;

    // 注入价格引擎服务
    private final PriceEngineService priceEngineService;
    private final CustomerGradeService customerGradeService;
    private final ProductService productService;
    private final ProductUnitService productUnitService;
    private final PartyGradeRelationService partyGradeRelationService;

    // 注入业务编号生成服务
    private final BizNumberGeneratorService bizNumberGeneratorService;

    @Override
    public Page<SaleOrderDTO> pageOrders(Page<SaleOrder> page, Long tenantId, String orderNo,
                                          Long customerId, Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();

        // 转换日期字符串为 LocalDate，避免 PostgreSQL 类型不匹配
        LocalDate startLocalDate = null;
        LocalDate endLocalDate = null;
        if (startDate != null && !startDate.isEmpty()) {
            try {
                startLocalDate = LocalDate.parse(startDate);
            } catch (Exception e) {
                log.warn("无法解析startDate: {}", startDate);
            }
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                endLocalDate = LocalDate.parse(endDate);
            } catch (Exception e) {
                log.warn("无法解析endDate: {}", endDate);
            }
        }

        wrapper.eq(SaleOrder::getTenantId, tenantId)
               .like(orderNo != null && !orderNo.isEmpty(), SaleOrder::getOrderNo, orderNo)
               .eq(customerId != null, SaleOrder::getCustomerId, customerId)
               .eq(status != null, SaleOrder::getStatus, status)
               .ge(startLocalDate != null, SaleOrder::getOrderDate, startLocalDate)
               .le(endLocalDate != null, SaleOrder::getOrderDate, endLocalDate)
               .orderByDesc(SaleOrder::getCreateTime);

        Page<SaleOrder> result = page(page, wrapper);

        Page<SaleOrderDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());

        return dtoPage;
    }

    @Override
    public List<SaleOrder> exportList(String keyword, Long customerId, Integer status) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null && !keyword.isEmpty(), SaleOrder::getOrderNo, keyword)
               .eq(customerId != null, SaleOrder::getCustomerId, customerId)
               .eq(status != null, SaleOrder::getStatus, status)
               .orderByDesc(SaleOrder::getCreateTime);
        return list(wrapper);
    }

    @Override
    public SaleOrderDTO getOrderDetail(Long id) {
        SaleOrder order = getById(id);
        if (order == null) return null;

        SaleOrderDTO dto = convertToDTO(order);
        List<SaleOrderItem> items = itemMapper.selectByOrderId(id);

        // 丰富订单明细，填充JOIN字段
        enrichOrderItems(items);

        dto.setItems(items.stream().map(this::convertItemToDTO).collect(Collectors.toList()));

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(SaleOrderDTO dto) {
        // 生成订单号
        if (dto.getOrderNo() == null || dto.getOrderNo().isEmpty()) {
            dto.setOrderNo(generateOrderNo());
        }

        // 根据客户ID获取客户等级
        String customerGradeCode = getCustomerGradeCode(dto.getCustomerId());
        String customerGradeName = getCustomerGradeName(dto.getCustomerId());

        // 设置客户等级信息到订单
        dto.setCustomerGradeCode(customerGradeCode);
        dto.setCustomerGradeName(customerGradeName);

        // 计算订单总金额（通过明细实时计算）
        calculateAmount(dto);

        SaleOrder order = new SaleOrder();
        BeanUtils.copyProperties(dto, order);
        order.setStatus(0); // 草稿
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setCreateTime(LocalDateTime.now());

        save(order);

        // 保存明细
        if (dto.getItems() != null) {
            int lineNo = 1;
            for (SaleOrderItemDTO itemDTO : dto.getItems()) {
                SaleOrderItem item = createOrderItem(itemDTO, order.getId(), dto.getCustomerId());
                item.setLineNo(lineNo++);
                itemMapper.insert(item);
            }
        }

        log.info("创建销售订单: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(SaleOrderDTO dto) {
        SaleOrder order = getById(dto.getId());
        if (order == null) throw BusinessException.notFound("订单不存在");

        if (order.getStatus() > 1) throw BusinessException.badRequest("只有草稿和待审批状态的订单可以修改");

        // 根据客户ID获取客户等级
        String customerGradeCode = getCustomerGradeCode(dto.getCustomerId());
        String customerGradeName = getCustomerGradeName(dto.getCustomerId());

        // 设置客户等级信息到订单
        dto.setCustomerGradeCode(customerGradeCode);
        dto.setCustomerGradeName(customerGradeName);

        calculateAmount(dto);
        BeanUtils.copyProperties(dto, order);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        // 删除原明细
        List<SaleOrderItem> oldItems = itemMapper.selectByOrderId(dto.getId());
        oldItems.forEach(item -> itemMapper.deleteById(item.getId()));

        // 保存新明细
        if (dto.getItems() != null) {
            int lineNo = 1;
            for (SaleOrderItemDTO itemDTO : dto.getItems()) {
                SaleOrderItem item = createOrderItem(itemDTO, order.getId(), dto.getCustomerId());
                item.setLineNo(lineNo++);
                itemMapper.insert(item);
            }
        }

        log.info("更新销售订单: orderId={}", order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() > 1) throw BusinessException.badRequest("只有草稿和待审批状态的订单可以删除");

        // 删除明细
        List<SaleOrderItem> items = itemMapper.selectByOrderId(id);
        items.forEach(item -> itemMapper.deleteById(item.getId()));

        removeById(id);
        log.info("删除销售订单: orderId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long id) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() != 0) throw BusinessException.badRequest("只有草稿状态的订单可以提交审批");

        orderMapper.updateStatus(id, 1); // 待审批
        log.info("提交销售订单审批: orderId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long auditorId) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() != 1) throw BusinessException.badRequest("订单不是待审批状态");

        orderMapper.updateStatus(id, 2); // 已审批
        log.info("销售订单审批通过: orderId={}, auditorId={}", id, auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, Long auditorId, String reason) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() != 1) throw BusinessException.badRequest("订单不是待审批状态");

        orderMapper.updateStatus(id, 0); // 退回草稿
        order.setRemark(reason);
        updateById(order);
        log.info("销售订单审批拒绝: orderId={}, auditorId={}, reason={}", id, auditorId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, String reason) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() >= 4) throw BusinessException.badRequest("已完成的订单不能取消");

        orderMapper.updateStatus(id, 5); // 取消
        order.setRemark(reason);
        updateById(order);
        log.info("取消销售订单: orderId={}, reason={}", id, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmShipment(Long id, Long warehouseId) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (order.getStatus() != 2 && order.getStatus() != 3)
            throw BusinessException.badRequest("订单状态不允许出库");

        List<SaleOrderItem> items = itemMapper.selectByOrderId(id);

        for (SaleOrderItem item : items) {
            Long productId = item.getProductId();
            BigDecimal quantity = item.getQuantity();

            try {
                boolean success = stockService.decreaseStock(productId, warehouseId, quantity);
                if (!success) {
                    log.warn("库存扣减失败: productId={}, warehouseId={}, quantity={}", productId, warehouseId, quantity);
                }
            } catch (Exception e) {
                log.error("库存扣减异常: productId={}, warehouseId={}, quantity={}", productId, warehouseId, quantity, e);
            }
        }

        for (SaleOrderItem item : items) {
            itemMapper.addShippedQuantity(item.getId(), item.getQuantity());
        }

        boolean allShipped = items.stream()
                .allMatch(item -> item.getShippedQuantity().add(item.getQuantity())
                        .compareTo(item.getQuantity()) >= 0);

        orderMapper.updateStatus(id, allShipped ? 4 : 3);
        log.info("确认销售订单出库: orderId={}, warehouseId={}", id, warehouseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPayment(Long id, BigDecimal amount) {
        SaleOrder order = getById(id);
        if (order == null) throw BusinessException.notFound("订单不存在");

        orderMapper.addReceivedAmount(id, amount);
        log.info("记录销售订单收款: orderId={}, amount={}", id, amount);
    }

    @Override
    public List<SaleOrderDTO> getPendingOrders(Long tenantId) {
        List<SaleOrder> orders = orderMapper.selectPendingOrders(tenantId);
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public String generateOrderNo() {
        // 获取当前用户语言偏好，如果没有则默认使用中文
        String userLocale = getUserLocaleOrDefault();
        if ("en_US".equalsIgnoreCase(userLocale) || "en".equalsIgnoreCase(userLocale)) {
            return bizNumberGeneratorService.nextNumber("SO", 1L, userLocale);
        } else {
            // 默认使用中文拼音前缀
            return bizNumberGeneratorService.nextSaleOrderNo();
        }
    }

    /**
     * 获取当前用户语言偏好，默认为中文
     */
    private String getUserLocaleOrDefault() {
        // 在实际实现中，这里应该获取当前登录用户信息并查询其语言偏好
        // 临时实现：目前返回中文默认值，后续可以根据实际情况完善
        try {
            // 检查用户是否已登录
            if (StpUtil.isLogin()) {
                // 实际应用中，这里应该通过用户服务查询用户语言偏好
                // 例如：userService.getUserLanguagePreference(StpUtil.getLoginIdAsLong())

                // 暂时返回中文作为默认值，实际应用中可从用户配置或系统配置中获取
                return "zh_CN";
            }
        } catch (Exception e) {
            log.warn("获取用户语言偏好失败，使用默认值: {}", e.getMessage());
        }
        return "zh_CN"; // 默认中文
    }

    /**
     * 计算订单总金额（通过明细实时计算）
     */
    @Override
    public void calculateAmount(SaleOrderDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            // 注意：根据重构计划，表头不再存储合计金额字段
            // 这些字段将通过明细实时计算，不存储在表头
            return;
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (SaleOrderItemDTO item : dto.getItems()) {
            // 使用计算得出的价格
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = item.getCalculatedPrice() != null ? item.getCalculatedPrice() : item.getUnitPrice();
            BigDecimal taxRate = item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO;

            BigDecimal amount = qty.multiply(price);
            BigDecimal tax = amount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // 更新项目金额
            item.setAmount(amount);
            item.setTaxAmount(tax);
            item.setAmountWithTax(amount.add(tax));

            totalAmount = totalAmount.add(amount);
            totalTax = totalTax.add(tax);
        }

        // 注意：根据重构计划，表头不再存储合计金额字段
        // totalAmount, taxAmount, totalAmountWithTax 现在只在DTO层面计算用于展示
    }

    @Override
    public Map<String, Object> getOrderStats(Long tenantId) {
        Map<String, Object> stats = new HashMap<>();

        // 待处理订单数量（已审批待出库 + 部分出库）
        int pendingProcessCount = orderMapper.countPendingProcess(tenantId);

        // 待审核订单数量（待审批状态）
        int pendingApprovalCount = orderMapper.countPendingApproval(tenantId);

        // 今日新增订单数量
        int todayOrderCount = orderMapper.countTodayOrders(tenantId);

        // 本月新增订单数量
        int monthOrderCount = orderMapper.countMonthOrders(tenantId);

        stats.put("pendingProcessCount", pendingProcessCount);
        stats.put("pendingApprovalCount", pendingApprovalCount);
        stats.put("todayOrderCount", todayOrderCount);
        stats.put("monthOrderCount", monthOrderCount);

        log.info("获取销售订单统计: tenantId={}, pendingProcess={}, pendingApproval={}, today={}, month={}",
                tenantId, pendingProcessCount, pendingApprovalCount, todayOrderCount, monthOrderCount);
        return stats;
    }

    /**
     * 创建订单明细项（集成价格引擎）
     */
    private SaleOrderItem createOrderItem(SaleOrderItemDTO itemDTO, Long orderId, Long customerId) {
        SaleOrderItem item = new SaleOrderItem();
        BeanUtils.copyProperties(itemDTO, item);

        item.setOrderId(orderId);
        item.setShippedQuantity(BigDecimal.ZERO);
        item.setCreateTime(LocalDateTime.now());

        // 根据客户等级和产品ID，通过价格引擎计算价格
        PriceCalculationRequest priceRequest = new PriceCalculationRequest();
        priceRequest.setCustomerId(customerId.toString());
        priceRequest.setProductId(itemDTO.getProductId().toString());
        priceRequest.setQuantity(itemDTO.getQuantity() != null ? itemDTO.getQuantity().intValue() : 1);
        priceRequest.setCustomerLevel(getCustomerGradeCode(customerId)); // 使用客户等级代码
        priceRequest.setCalculationTime(LocalDateTime.now()); // 当前计算时间

        // 设置产品相关信息以供价格引擎计算
        Product product = productService.getById(itemDTO.getProductId());
        if (product != null) {
            priceRequest.setProductName(product.getProductName());
            priceRequest.setBasePrice(product.getStandardPrice()); // 使用产品标准价
        }

        // 调用价格引擎计算价格
        PriceCalculationResult priceResult = priceEngineService.calculatePrice(priceRequest);

        // 设置价格快照字段
        item.setCalculatedPrice(priceResult.getFinalPrice() != null ? priceResult.getFinalPrice() : item.getUnitPrice());
        item.setPriceSource(priceResult.getCalculationExplanation() != null ? priceResult.getCalculationExplanation() : "Default");
        item.setCustomerGradeCode(getCustomerGradeCode(customerId));
        item.setCustomerGradeName(getCustomerGradeName(customerId));

        // 从计算结果中获取或推导价格等级代码
        // 如果价格策略中包含价格等级信息，可以从AppliedStrategy中获取
        if (priceResult.getAppliedStrategies() != null && !priceResult.getAppliedStrategies().isEmpty()) {
            // 尝试从应用的策略中获取价格等级信息
            for (var strategy : priceResult.getAppliedStrategies()) {
                if (strategy.getStrategyName() != null && strategy.getStrategyName().toLowerCase().contains("grade")) {
                    item.setPriceGradeCode(strategy.getStrategyId()); // 使用策略ID作为价格等级代码
                    break;
                }
            }
        }
        // 如果仍然没有价格等级代码，可以基于客户等级推导
        if (item.getPriceGradeCode() == null || item.getPriceGradeCode().isEmpty()) {
            item.setPriceGradeCode("PG_" + (getCustomerGradeCode(customerId) != null ? getCustomerGradeCode(customerId) : "DEFAULT"));
        }

        // 设置折扣信息
        if (priceResult.getDiscountDetails() != null && !priceResult.getDiscountDetails().isEmpty()) {
            // 将折扣详情转为字符串存储
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < priceResult.getDiscountDetails().size(); i++) {
                if (i > 0) sb.append(", ");
                var detail = priceResult.getDiscountDetails().get(i);
                sb.append(detail.getRuleName()).append(": ").append(detail.getDiscountAmount());
            }
            item.setDiscountApplied(sb.toString());
        } else {
            item.setDiscountApplied("No discounts applied");
        }

        // 使用价格引擎计算出的价格
        item.setUnitPrice(priceResult.getFinalPrice() != null ? priceResult.getFinalPrice() : item.getUnitPrice());

        // 设置成本价（从产品信息或库存成本获取）
        if (product != null) {
            item.setCostPrice(product.getCostPrice());
        }

        item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));

        return item;
    }

    /**
     * 获取客户等级代码
     * 通过 PartyGradeRelationService 从 biz_party_grade_relation 表获取客户等级
     */
    private String getCustomerGradeCode(Long customerId) {
        if (customerId == null) return null;

        try {
            Long gradeId = partyGradeRelationService.getCurrentGradeId(customerId);
            if (gradeId != null) {
                CustomerGrade customerGrade = customerGradeService.getById(gradeId);
                if (customerGrade != null) {
                    return customerGrade.getGradeCode();
                }
            }

            CustomerGrade defaultGrade = customerGradeService.getDefaultGrade();
            return defaultGrade != null ? defaultGrade.getGradeCode() : "DEFAULT";
        } catch (Exception e) {
            log.warn("获取客户等级信息失败，使用默认等级: customerId={}, error={}", customerId, e.getMessage());
            return "DEFAULT";
        }
    }

    /**
     * 获取客户等级名称
     * 通过 PartyGradeRelationService 从 biz_party_grade_relation 表获取客户等级
     */
    private String getCustomerGradeName(Long customerId) {
        if (customerId == null) return null;

        try {
            Long gradeId = partyGradeRelationService.getCurrentGradeId(customerId);
            if (gradeId != null) {
                CustomerGrade customerGrade = customerGradeService.getById(gradeId);
                if (customerGrade != null) {
                    return customerGrade.getGradeName();
                }
            }

            CustomerGrade defaultGrade = customerGradeService.getDefaultGrade();
            return defaultGrade != null ? defaultGrade.getGradeName() : "默认等级";
        } catch (Exception e) {
            log.warn("获取客户等级信息失败，使用默认等级: customerId={}, error={}", customerId, e.getMessage());
            return "默认等级";
        }
    }

    /**
     * 丰富订单明细（填充JOIN字段）
     */
    private void enrichOrderItems(List<SaleOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // 获取所有产品ID
        Set<Long> productIds = items.stream()
            .map(SaleOrderItem::getProductId)
            .collect(Collectors.toSet());

        if (productIds.isEmpty()) {
            return;
        }

        // 批量获取产品信息
        List<Product> productList = productService.listByIds(productIds);

        // 将产品列表转换为Map，便于查找
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : productList) {
            if (product != null && product.getId() != null) {
                productMap.put(product.getId(), product);
            }
        }

        // 填充JOIN字段
        for (SaleOrderItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product != null) {
                item.setImage(product.getImageUrl());
                item.setBarcode(product.getBarcode());
                item.setSmallUnitBarcode(product.getBarcode());
                item.setOrigin(product.getOrigin());
                item.setBrand(product.getBrand());
                item.setShelfLife(product.getShelfLifeDays() != null ? product.getShelfLifeDays().toString() : null);
                item.setRetailPrice(product.getRetailPrice());
                item.setWholesalePrice(product.getWholesalePrice());
                item.setUnit(product.getUnit());
                // 注意：lineAttribute字段在Product实体中不存在，跳过设置
                item.setVolume(product.getVolume());
                item.setWeight(product.getWeight());
            }

            // 计算字段
            item.setCostAmount(item.getQuantity().multiply(item.getCostPrice()));
            item.setGrossProfit(item.getAmount().subtract(item.getCostAmount()));
            item.setTaxAmount(item.getAmount().multiply(item.getTaxRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            item.setDiscountedUnitPrice(item.getUnitPrice().multiply(BigDecimal.ONE.subtract(item.getDiscountRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))));
            item.setDiscountedAmount(item.getAmount().multiply(BigDecimal.ONE.subtract(item.getDiscountRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))));
        }
    }

    private SaleOrderDTO convertToDTO(SaleOrder order) {
        if (order == null) return null;
        SaleOrderDTO dto = new SaleOrderDTO();
        BeanUtils.copyProperties(order, dto);
        dto.setStatusName(getStatusName(order.getStatus()));
        return dto;
    }

    private SaleOrderItemDTO convertItemToDTO(SaleOrderItem item) {
        if (item == null) return null;
        SaleOrderItemDTO dto = new SaleOrderItemDTO();
        BeanUtils.copyProperties(item, dto);
        return dto;
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "待审批";
            case 2: return "已审批";
            case 3: return "部分出库";
            case 4: return "完成";
            case 5: return "取消";
            default: return "未知";
        }
    }
}