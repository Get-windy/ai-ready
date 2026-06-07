package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dto.*;
import cn.aiedge.erp.b2b.mapper.MallOrderItemMapper;
import cn.aiedge.erp.b2b.mapper.MallOrderMapper;
import cn.aiedge.erp.b2b.mapper.MallProductMapper;
import cn.aiedge.erp.b2b.model.MallOrder;
import cn.aiedge.erp.b2b.model.MallOrderItem;
import cn.aiedge.erp.b2b.model.MallProduct;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallOrderServiceImpl implements MallOrderService {

    private final MallOrderMapper mallOrderMapper;
    private final MallOrderItemMapper mallOrderItemMapper;
    private final MallProductMapper mallProductMapper;

    private static final AtomicLong ORDER_NO_COUNTER = new AtomicLong(0);

    @Override
    @Transactional
    public OrderDetailDTO createOrder(OrderCreateRequest request) {
        log.info("创建订单");
        String customerId = StpUtil.getLoginIdAsString();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BusinessException.badRequest("订单商品不能为空");
        }

        // Calculate order amounts
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<MallOrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            MallProduct product = mallProductMapper.selectOne(
                    new LambdaQueryWrapper<MallProduct>()
                            .eq(MallProduct::getProductId, itemRequest.getProductId())
                            .eq(MallProduct::getDeleted, false)
            );
            if (product == null) {
                throw BusinessException.notFound("商品不存在: " + itemRequest.getProductId());
            }
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw BusinessException.badRequest("商品库存不足: " + product.getProductName());
            }

            MallOrderItem orderItem = new MallOrderItem();
            orderItem.setProductId(itemRequest.getProductId());
            orderItem.setProductName(product.getProductName());
            orderItem.setProductImage(product.getImageUrl());
            orderItem.setPrice(product.getSalePrice());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setSubtotal(product.getSalePrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
            orderItem.setCreatedAt(LocalDateTime.now());
            orderItem.setUpdatedAt(LocalDateTime.now());
            orderItems.add(orderItem);

            totalAmount = totalAmount.add(orderItem.getSubtotal());

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            mallProductMapper.updateById(product);
        }

        // Create order
        MallOrder order = new MallOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(customerId);
        order.setCustomerName("test_user");
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setOrderStatus("PENDING_PAYMENT");
        order.setPaymentStatus("UNPAID");
        order.setDeliveryStatus("UNSHIPPED");
        order.setRemark(request.getRemark());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        mallOrderMapper.insert(order);

        // Save order items with order reference
        for (MallOrderItem item : orderItems) {
            item.setOrderId(order.getId());
            mallOrderItemMapper.insert(item);
        }

        log.info("订单创建成功: {}", order.getOrderNo());
        return convertToDetailDTO(order, orderItems);
    }

    @Override
    public PageResult<OrderListDTO> listOrders(int page, int size, String orderStatus) {
        log.info("查询订单列表: page={}, size={}, orderStatus={}", page, size, orderStatus);
        String customerId = StpUtil.getLoginIdAsString();

        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallOrder::getCustomerId, customerId);
        wrapper.eq(MallOrder::getDeleted, false);
        if (orderStatus != null && !orderStatus.isEmpty()) {
            wrapper.eq(MallOrder::getOrderStatus, orderStatus);
        }
        wrapper.orderByDesc(MallOrder::getCreatedAt);

        IPage<MallOrder> orderPage = mallOrderMapper.selectPage(new Page<>(page, size), wrapper);

        List<OrderListDTO> records = orderPage.getRecords().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());

        PageResult<OrderListDTO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(orderPage.getTotal());
        result.setPage((int) orderPage.getCurrent());
        result.setSize((int) orderPage.getSize());

        return result;
    }

    @Override
    public OrderDetailDTO getOrderDetail(Long id) {
        log.info("获取订单详情: {}", id);
        MallOrder order = mallOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

        List<MallOrderItem> items = mallOrderItemMapper.selectList(
                new LambdaQueryWrapper<MallOrderItem>()
                        .eq(MallOrderItem::getOrderId, order.getId())
        );

        return convertToDetailDTO(order, items);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        log.info("取消订单: {}", id);
        MallOrder order = mallOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许取消");
        }

        order.setOrderStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        mallOrderMapper.updateById(order);

        // Restore stock
        List<MallOrderItem> items = mallOrderItemMapper.selectList(
                new LambdaQueryWrapper<MallOrderItem>()
                        .eq(MallOrderItem::getOrderId, order.getId())
        );
        for (MallOrderItem item : items) {
            MallProduct product = mallProductMapper.selectOne(
                    new LambdaQueryWrapper<MallProduct>()
                            .eq(MallProduct::getProductId, item.getProductId())
            );
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                mallProductMapper.updateById(product);
            }
        }

        log.info("订单已取消: {}", order.getOrderNo());
    }

    @Override
    @Transactional
    public void confirmOrder(Long id) {
        log.info("确认收货: {}", id);
        MallOrder order = mallOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

        if (!"SHIPPED".equals(order.getOrderStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许确认收货");
        }

        order.setOrderStatus("COMPLETED");
        order.setDeliveryStatus("RECEIVED");
        order.setUpdatedAt(LocalDateTime.now());
        mallOrderMapper.updateById(order);

        log.info("订单已确认收货: {}", order.getOrderNo());
    }

    @Override
    @Transactional
    public void payOrder(Long id) {
        log.info("支付订单: {}", id);
        MallOrder order = mallOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + id);
        }

        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许支付");
        }

        order.setOrderStatus("PAID");
        order.setPaymentStatus("PAID");
        order.setUpdatedAt(LocalDateTime.now());
        mallOrderMapper.updateById(order);

        log.info("订单支付成功: {}", order.getOrderNo());
    }

    @Override
    public List getPaymentMethods() {
        log.info("获取支付方式列表");
        List<Map<String, Object>> methods = new ArrayList<>();

        Map<String, Object> method1 = new HashMap<>();
        method1.put("id", "wechat");
        method1.put("name", "微信支付");
        method1.put("icon", "wechat");
        methods.add(method1);

        Map<String, Object> method2 = new HashMap<>();
        method2.put("id", "alipay");
        method2.put("name", "支付宝");
        method2.put("icon", "alipay");
        methods.add(method2);

        Map<String, Object> method3 = new HashMap<>();
        method3.put("id", "bank_transfer");
        method3.put("name", "银行转账");
        method3.put("icon", "bank");
        methods.add(method3);

        return methods;
    }

    @Override
    public void trackOrder(Long id) {
        log.info("查询物流跟踪: {}", id);
        // TODO: Implement actual logistics tracking
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_MS_PATTERN);
        long seq = ORDER_NO_COUNTER.incrementAndGet() % 10000;
        return "ORD" + dateStr + String.format("%04d", seq);
    }

    private OrderListDTO convertToListDTO(MallOrder order) {
        OrderListDTO dto = new OrderListDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayAmount(order.getPayAmount());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setCreatedAt(order.getCreatedAt());

        // Count items
        Long itemCount = mallOrderItemMapper.selectCount(
                new LambdaQueryWrapper<MallOrderItem>()
                        .eq(MallOrderItem::getOrderId, order.getId())
        );
        dto.setItemCount(itemCount != null ? itemCount.intValue() : 0);

        return dto;
    }

    private OrderDetailDTO convertToDetailDTO(MallOrder order, List<MallOrderItem> items) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayAmount(order.getPayAmount());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCustomerName(order.getCustomerName());
        dto.setConsignee(order.getConsignee());
        dto.setPhone(order.getPhone());
        dto.setAddress(order.getAddress());
        dto.setItemCount(items != null ? items.size() : 0);

        if (items != null) {
            List<OrderDetailDTO.OrderItemDTO> itemDTOs = items.stream().map(item -> {
                OrderDetailDTO.OrderItemDTO itemDTO = new OrderDetailDTO.OrderItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProductId());
                itemDTO.setProductName(item.getProductName());
                itemDTO.setProductImage(item.getProductImage());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setSubtotal(item.getSubtotal());
                return itemDTO;
            }).collect(Collectors.toList());
            dto.setItems(itemDTOs);
        }

        return dto;
    }
}
