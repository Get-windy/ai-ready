package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.erp.sale.dto.SaleOrderDTO;
import cn.aiedge.erp.sale.dto.SaleOrderItemDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.entity.SaleOrderItem;
import cn.aiedge.erp.sale.mapper.SaleOrderMapper;
import cn.aiedge.erp.sale.mapper.SaleOrderItemMapper;
import cn.aiedge.erp.sale.service.ISaleOrderService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 销售订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleOrderServiceImpl extends ServiceImpl<SaleOrderMapper, SaleOrder> 
        implements ISaleOrderService {

    private final SaleOrderMapper orderMapper;
    private final SaleOrderItemMapper itemMapper;

    @Override
    public Page<SaleOrderDTO> pageOrders(Page<SaleOrder> page, Long tenantId, String orderNo,
                                          Long customerId, Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOrder::getTenantId, tenantId)
               .like(orderNo != null, SaleOrder::getOrderNo, orderNo)
               .eq(customerId != null, SaleOrder::getCustomerId, customerId)
               .eq(status != null, SaleOrder::getStatus, status)
               .ge(startDate != null, SaleOrder::getOrderDate, startDate)
               .le(endDate != null, SaleOrder::getOrderDate, endDate)
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
    public SaleOrderDTO getOrderDetail(Long id) {
        SaleOrder order = getById(id);
        if (order == null) return null;

        SaleOrderDTO dto = convertToDTO(order);
        List<SaleOrderItem> items = itemMapper.selectByOrderId(id);
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

        // 计算金额
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
                SaleOrderItem item = new SaleOrderItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setOrderId(order.getId());
                item.setLineNo(lineNo++);
                item.setShippedQuantity(BigDecimal.ZERO);
                item.setCreateTime(LocalDateTime.now());
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
        if (order == null) throw new RuntimeException("订单不存在");

        if (order.getStatus() > 1) throw new RuntimeException("只有草稿和待审批状态的订单可以修改");

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
                SaleOrderItem item = new SaleOrderItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setOrderId(order.getId());
                item.setLineNo(lineNo++);
                item.setShippedQuantity(BigDecimal.ZERO);
                item.setCreateTime(LocalDateTime.now());
                itemMapper.insert(item);
            }
        }

        log.info("更新销售订单: orderId={}", order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        SaleOrder order = getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() > 1) throw new RuntimeException("只有草稿和待审批状态的订单可以删除");

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
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != 0) throw new RuntimeException("只有草稿状态的订单可以提交审批");

        orderMapper.updateStatus(id, 1); // 待审批
        log.info("提交销售订单审批: orderId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long auditorId) {
        SaleOrder order = getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != 1) throw new RuntimeException("订单不是待审批状态");

        orderMapper.updateStatus(id, 2); // 已审批
        log.info("销售订单审批通过: orderId={}, auditorId={}", id, auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, Long auditorId, String reason) {
        SaleOrder order = getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != 1) throw new RuntimeException("订单不是待审批状态");

        orderMapper.updateStatus(id, 0); // 退回草稿
        order.setRemark(reason);
        updateById(order);
        log.info("销售订单审批拒绝: orderId={}, auditorId={}, reason={}", id, auditorId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, String reason) {
        SaleOrder order = getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() >= 4) throw new RuntimeException("已完成的订单不能取消");

        orderMapper.updateStatus(id, 5); // 取消
        order.setRemark(reason);
        updateById(order);
        log.info("取消销售订单: orderId={}, reason={}", id, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmShipment(Long id, Long warehouseId) {
        SaleOrder order = getById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != 2 && order.getStatus() != 3) 
            throw new RuntimeException("订单状态不允许出库");

        List<SaleOrderItem> items = itemMapper.selectByOrderId(id);
        
        // TODO: 调用库存服务扣减库存
        // inventoryService.outbound(...)

        // 更新明细出库数量
        for (SaleOrderItem item : items) {
            itemMapper.addShippedQuantity(item.getId(), item.getQuantity());
        }

        // 检查是否全部出库
        boolean allShipped = items.stream()
                .allMatch(item -> item.getShippedQuantity().add(item.getQuantity())
                        .compareTo(item.getQuantity()) >= 0);

        orderMapper.updateStatus(id, allShipped ? 4 : 3); // 完成或部分出库
        log.info("确认销售订单出库: orderId={}, warehouseId={}", id, warehouseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPayment(Long id, BigDecimal amount) {
        SaleOrder order = getById(id);
        if (order == null) throw new RuntimeException("订单不存在");

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
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "SO" + dateStr + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    @Override
    public void calculateAmount(SaleOrderDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) return;

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (SaleOrderItemDTO item : dto.getItems()) {
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal taxRate = item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO;

            BigDecimal amount = qty.multiply(price);
            BigDecimal tax = amount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            item.setAmount(amount);
            item.setTaxAmount(tax);
            item.setAmountWithTax(amount.add(tax));

            totalAmount = totalAmount.add(amount);
            totalTax = totalTax.add(tax);
        }

        dto.setTotalAmount(totalAmount);
        dto.setTaxAmount(totalTax);
        dto.setTotalAmountWithTax(totalAmount.add(totalTax));
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