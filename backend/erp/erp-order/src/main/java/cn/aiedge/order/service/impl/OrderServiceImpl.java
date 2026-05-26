package cn.aiedge.order.service.impl;

import cn.aiedge.order.dto.OrderDTO;
import cn.aiedge.order.dto.OrderItemDTO;
import cn.aiedge.order.entity.Order;
import cn.aiedge.order.entity.OrderItem;
import cn.aiedge.order.mapper.OrderMapper;
import cn.aiedge.order.mapper.OrderItemMapper;
import cn.aiedge.order.service.IOrderService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public Page<OrderDTO> pageOrders(Page<Order> page, Long tenantId, String orderNo, String customerName,
                                      Integer orderType, Integer status, Long saleId,
                                      LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getTenantId, tenantId)
               .like(orderNo != null, Order::getOrderNo, orderNo)
               .like(customerName != null, Order::getCustomerName, customerName)
               .eq(orderType != null, Order::getOrderType, orderType)
               .eq(status != null, Order::getStatus, status)
               .eq(saleId != null, Order::getSaleId, saleId)
               .ge(startDate != null, Order::getOrderDate, startDate)
               .le(endDate != null, Order::getOrderDate, endDate)
               .orderByDesc(Order::getCreateTime);

        Page<Order> result = page(page, wrapper);
        Page<OrderDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        return dtoPage;
    }

    @Override
    public OrderDTO getOrderDetail(Long id) {
        Order order = getById(id);
        if (order == null) return null;
        OrderDTO dto = convertToDTO(order);
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, id);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        dto.setItems(items.stream().map(this::convertItemToDTO).collect(Collectors.toList()));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderDTO dto) {
        if (dto.getOrderNo() == null || dto.getOrderNo().isEmpty()) {
            dto.setOrderNo(generateOrderNo());
        }
        Order order = new Order();
        BeanUtils.copyProperties(dto, order);
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());

        // 计算金额
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItemDTO item : dto.getItems()) {
                BigDecimal subtotal = item.getPrice().multiply(item.getQuantity());
                if (item.getDiscountRate() != null) {
                    subtotal = subtotal.multiply(item.getDiscountRate());
                }
                item.setSubtotal(subtotal);
                total = total.add(subtotal);
            }
            order.setTotalAmount(total);
            order.setActualAmount(total.subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO));
        }

        save(order);

        // 保存明细
        if (dto.getItems() != null) {
            for (OrderItemDTO itemDTO : dto.getItems()) {
                OrderItem item = new OrderItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setOrderId(order.getId());
                item.setTenantId(order.getTenantId());
                item.setCreateTime(LocalDateTime.now());
                orderItemMapper.insert(item);
            }
        }

        log.info("创建订单: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(OrderDTO dto) {
        Order order = getById(dto.getId());
        if (order == null) throw new RuntimeException("订单不存在");
        BeanUtils.copyProperties(dto, order);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        // 更新明细：先删后增
        LambdaQueryWrapper<OrderItem> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(OrderItem::getOrderId, dto.getId());
        orderItemMapper.delete(delWrapper);

        if (dto.getItems() != null) {
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItemDTO itemDTO : dto.getItems()) {
                BigDecimal subtotal = itemDTO.getPrice().multiply(itemDTO.getQuantity());
                if (itemDTO.getDiscountRate() != null) {
                    subtotal = subtotal.multiply(itemDTO.getDiscountRate());
                }
                itemDTO.setSubtotal(subtotal);
                total = total.add(subtotal);

                OrderItem item = new OrderItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setOrderId(dto.getId());
                item.setTenantId(order.getTenantId());
                item.setCreateTime(LocalDateTime.now());
                orderItemMapper.insert(item);
            }
            order.setTotalAmount(total);
            order.setActualAmount(total.subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO));
            updateById(order);
        }

        log.info("更新订单: orderId={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        removeById(id);
        log.info("删除订单: orderId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        orderMapper.updateStatus(id, status);
        log.info("更新订单状态: orderId={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOrder(Long id, Integer status, String auditRemark) {
        Order order = getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        order.setStatus(status);
        order.setAuditId(StpUtil.getLoginIdAsLong());
        order.setAuditName(StpUtil.getLoginIdAsString());
        order.setAuditTime(LocalDateTime.now());
        order.setAuditRemark(auditRemark);
        updateById(order);
        log.info("审核订单: orderId={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addReceivedAmount(Long id, BigDecimal amount) {
        orderMapper.addReceivedAmount(id, amount);
        log.info("订单收款: orderId={}, amount={}", id, amount);
    }

    @Override
    public String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "ORD" + dateStr + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    private OrderDTO convertToDTO(Order order) {
        if (order == null) return null;
        OrderDTO dto = new OrderDTO();
        BeanUtils.copyProperties(order, dto);
        dto.setOrderTypeName(getOrderTypeName(order.getOrderType()));
        dto.setStatusName(getStatusName(order.getStatus()));
        return dto;
    }

    private OrderItemDTO convertItemToDTO(OrderItem item) {
        if (item == null) return null;
        OrderItemDTO dto = new OrderItemDTO();
        BeanUtils.copyProperties(item, dto);
        return dto;
    }

    private String getOrderTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "销售订单";
            case 2: return "采购订单";
            case 3: return "退货订单";
            default: return "未知";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "草稿";
        switch (status) {
            case 0: return "草稿";
            case 1: return "待审核";
            case 2: return "已审核";
            case 3: return "已发货";
            case 4: return "已完成";
            case 5: return "已取消";
            case 6: return "已退货";
            default: return "草稿";
        }
    }
}
