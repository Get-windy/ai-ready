package cn.aiedge.erp.sale.outbound.service.impl;

import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import cn.aiedge.erp.sale.outbound.enums.OutboundStatus;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundItemMapper;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundMapper;
import cn.aiedge.erp.sale.outbound.service.SaleOutboundService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleOutboundServiceImpl extends ServiceImpl<SaleOutboundMapper, SaleOutbound> implements SaleOutboundService {

    private final SaleOutboundItemMapper outboundItemMapper;

    @Override
    public SaleOutbound getByOutboundNo(String outboundNo) {
        return lambdaQuery()
                .eq(SaleOutbound::getOutboundNo, outboundNo)
                .eq(SaleOutbound::getDeleted, 0)
                .one();
    }

    @Override
    public Page<SaleOutbound> pageList(String keyword, Long customerId, Long orderId, Long warehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<SaleOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOutbound::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SaleOutbound::getOutboundNo, keyword)
                    .or().like(SaleOutbound::getOrderNo, keyword)
                    .or().like(SaleOutbound::getCustomerName, keyword));
        }
        if (customerId != null) {
            wrapper.eq(SaleOutbound::getCustomerId, customerId);
        }
        if (orderId != null) {
            wrapper.eq(SaleOutbound::getOrderId, orderId);
        }
        if (warehouseId != null) {
            wrapper.eq(SaleOutbound::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(SaleOutbound::getStatus, status);
        }
        wrapper.orderByDesc(SaleOutbound::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<SaleOutbound> exportList(String keyword, Integer status) {
        LambdaQueryWrapper<SaleOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOutbound::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SaleOutbound::getOutboundNo, keyword)
                    .or().like(SaleOutbound::getOrderNo, keyword)
                    .or().like(SaleOutbound::getCustomerName, keyword));
        }
        if (status != null) {
            wrapper.eq(SaleOutbound::getStatus, status);
        }
        wrapper.orderByDesc(SaleOutbound::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<SaleOutbound> listByCustomerId(Long customerId) {
        return baseMapper.selectByCustomerId(customerId);
    }

    @Override
    public List<SaleOutbound> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public String generateOutboundNo() {
        String prefix = "SO";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<SaleOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SaleOutbound::getOutboundNo, prefix + dateStr)
                .eq(SaleOutbound::getDeleted, 0)
                .orderByDesc(SaleOutbound::getOutboundNo)
                .last("LIMIT 1");
        SaleOutbound lastOutbound = getOne(wrapper);
        int seq = 1;
        if (lastOutbound != null) {
            String lastNo = lastOutbound.getOutboundNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound createOutbound(SaleOutbound outbound, List<SaleOutboundItem> items) {
        outbound.setOutboundNo(generateOutboundNo());
        outbound.setStatus(OutboundStatus.DRAFT.getCode());
        outbound.setOutboundDate(LocalDate.now());
        outbound.setOutboundType(1);
        outbound.setTotalQuantity(BigDecimal.ZERO);
        outbound.setTotalAmount(BigDecimal.ZERO);
        save(outbound);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                SaleOutboundItem item = items.get(i);
                item.setOutboundId(outbound.getId());
                item.setLineNo(i + 1);
                item.setTenantId(outbound.getTenantId());
                item.setPendingQuantity(item.getOrderQuantity());
                item.setOutboundQuantity(BigDecimal.ZERO);
                item.setPickingStatus(0);
                item.setPackingStatus(0);
                calculateItemAmounts(item);
                outboundItemMapper.insert(item);
            }
        }
        calculateTotals(outbound.getId());
        return getById(outbound.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound createFromOrder(Long orderId) {
        SaleOutbound outbound = new SaleOutbound();
        outbound.setOrderId(orderId);
        outbound.setOutboundDate(LocalDate.now());
        outbound.setOutboundType(1);
        outbound.setWarehouseId(1L);
        return createOutbound(outbound, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound updateOutbound(Long outboundId, SaleOutbound outbound, List<SaleOutboundItem> items) {
        SaleOutbound existing = getById(outboundId);
        if (existing == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (existing.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以修改");
        }
        outbound.setId(outboundId);
        updateById(outbound);
        if (items != null) {
            List<SaleOutboundItem> existingItems = getItems(outboundId);
            for (SaleOutboundItem oldItem : existingItems) {
                outboundItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                SaleOutboundItem item = items.get(i);
                item.setOutboundId(outboundId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                item.setPendingQuantity(item.getOrderQuantity());
                item.setOutboundQuantity(BigDecimal.ZERO);
                item.setPickingStatus(0);
                item.setPackingStatus(0);
                calculateItemAmounts(item);
                outboundItemMapper.insert(item);
            }
        }
        calculateTotals(outboundId);
        return getById(outboundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound submitForApproval(Long outboundId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以提交审批");
        }
        outbound.setStatus(OutboundStatus.PENDING_APPROVAL.getCode());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound approve(Long outboundId, Long approverId, String note) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的出库单可以审批");
        }
        outbound.setStatus(OutboundStatus.APPROVED.getCode());
        outbound.setApprovedBy(approverId);
        outbound.setApprovedTime(LocalDateTime.now());
        outbound.setApprovedNote(note);
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound reject(Long outboundId, String reason) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的出库单可以拒绝");
        }
        outbound.setStatus(OutboundStatus.DRAFT.getCode());
        outbound.setRemark(reason);
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound startPicking(Long outboundId, Long pickerId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的出库单可以开始拣货");
        }
        outbound.setStatus(OutboundStatus.PICKING.getCode());
        outbound.setPickingBy(pickerId);
        outbound.setPickingTime(LocalDateTime.now());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutboundItem pickItem(Long itemId, BigDecimal outboundQuantity, String batchNo) {
        SaleOutboundItem item = outboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("出库明细不存在");
        }
        SaleOutbound outbound = getById(item.getOutboundId());
        if (outbound.getStatus() != OutboundStatus.PICKING.getCode()) {
            throw new RuntimeException("只有拣货中状态的出库单可以处理明细");
        }
        item.setOutboundQuantity(outboundQuantity);
        item.setPendingQuantity(item.getOrderQuantity().subtract(outboundQuantity));
        item.setBatchNo(batchNo);
        item.setPickingStatus(1);
        item.setPickingBy(outbound.getPickingBy());
        item.setPickingTime(LocalDateTime.now());
        calculateItemAmounts(item);
        outboundItemMapper.updateById(item);
        calculateTotals(item.getOutboundId());
        return outboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound completePicking(Long outboundId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        outbound.setStatus(OutboundStatus.PICKED.getCode());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound startPacking(Long outboundId, Long packerId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.PICKED.getCode()) {
            throw new RuntimeException("只有已拣货状态的出库单可以开始打包");
        }
        outbound.setStatus(OutboundStatus.PACKING.getCode());
        outbound.setPackingBy(packerId);
        outbound.setPackingTime(LocalDateTime.now());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutboundItem packItem(Long itemId) {
        SaleOutboundItem item = outboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("出库明细不存在");
        }
        SaleOutbound outbound = getById(item.getOutboundId());
        if (outbound.getStatus() != OutboundStatus.PACKING.getCode()) {
            throw new RuntimeException("只有打包中状态的出库单可以处理明细");
        }
        item.setPackingStatus(1);
        item.setPackingBy(outbound.getPackingBy());
        item.setPackingTime(LocalDateTime.now());
        outboundItemMapper.updateById(item);
        return outboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound completePacking(Long outboundId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        outbound.setStatus(OutboundStatus.PACKED.getCode());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound ship(Long outboundId, Long shipperId, String trackingNumber, String logisticsCompany) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.PACKED.getCode()) {
            throw new RuntimeException("只有已打包状态的出库单可以发货");
        }
        outbound.setStatus(OutboundStatus.SHIPPED.getCode());
        outbound.setShippedBy(shipperId);
        outbound.setShippedTime(LocalDateTime.now());
        outbound.setActualShipTime(LocalDateTime.now());
        outbound.setTrackingNumber(trackingNumber);
        outbound.setLogisticsCompany(logisticsCompany);
        updateById(outbound);
        updateStock(outboundId);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound complete(Long outboundId) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.SHIPPED.getCode()) {
            throw new RuntimeException("只有已发货状态的出库单可以完成");
        }
        outbound.setStatus(OutboundStatus.COMPLETED.getCode());
        outbound.setCompletedBy(outbound.getCreateBy());
        outbound.setCompletedTime(LocalDateTime.now());
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutbound cancel(Long outboundId, String reason) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() == OutboundStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的出库单不能取消");
        }
        outbound.setStatus(OutboundStatus.CANCELLED.getCode());
        outbound.setRemark(reason);
        updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long outboundId) {
        BigDecimal totalQuantity = outboundItemMapper.sumOutboundQuantityByOutboundId(outboundId);
        BigDecimal totalAmount = outboundItemMapper.sumLineAmountByOutboundId(outboundId);
        SaleOutbound outbound = getById(outboundId);
        outbound.setTotalQuantity(totalQuantity != null ? totalQuantity : BigDecimal.ZERO);
        outbound.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        updateById(outbound);
    }

    @Override
    public List<SaleOutboundItem> getItems(Long outboundId) {
        return outboundItemMapper.selectByOutboundId(outboundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutboundItem addItem(Long outboundId, SaleOutboundItem item) {
        SaleOutbound outbound = getById(outboundId);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (outbound.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以添加明细");
        }
        List<SaleOutboundItem> existingItems = getItems(outboundId);
        item.setOutboundId(outboundId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(outbound.getTenantId());
        item.setPendingQuantity(item.getOrderQuantity());
        item.setOutboundQuantity(BigDecimal.ZERO);
        item.setPickingStatus(0);
        item.setPackingStatus(0);
        calculateItemAmounts(item);
        outboundItemMapper.insert(item);
        calculateTotals(outboundId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOutboundItem updateItem(Long itemId, SaleOutboundItem item) {
        SaleOutboundItem existing = outboundItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("出库明细不存在");
        }
        SaleOutbound outbound = getById(existing.getOutboundId());
        if (outbound.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以修改明细");
        }
        item.setId(itemId);
        item.setPendingQuantity(item.getOrderQuantity());
        calculateItemAmounts(item);
        outboundItemMapper.updateById(item);
        calculateTotals(existing.getOutboundId());
        return outboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        SaleOutboundItem item = outboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("出库明细不存在");
        }
        SaleOutbound outbound = getById(item.getOutboundId());
        if (outbound.getStatus() != OutboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的出库单可以删除明细");
        }
        outboundItemMapper.deleteById(itemId);
        calculateTotals(item.getOutboundId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long outboundId) {
        List<SaleOutboundItem> items = getItems(outboundId);
        SaleOutbound outbound = getById(outboundId);
        for (SaleOutboundItem item : items) {
            if (item.getOutboundQuantity().compareTo(BigDecimal.ZERO) > 0) {
                log.info("更新库存: 产品ID={}, 仓库ID={}, 出库数量={}", 
                        item.getProductId(), outbound.getWarehouseId(), item.getOutboundQuantity());
            }
        }
    }

    private void calculateItemAmounts(SaleOutboundItem item) {
        BigDecimal quantity = item.getOutboundQuantity() != null ? item.getOutboundQuantity() : BigDecimal.ZERO;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        item.setLineAmount(lineAmount);
    }
}