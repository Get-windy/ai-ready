package cn.aiedge.wms.borrow.service.impl;

import cn.aiedge.wms.borrow.dto.BorrowReturnRequest;
import cn.aiedge.wms.borrow.dto.WmsBorrowOrderVO;
import cn.aiedge.wms.borrow.mapper.WmsBorrowOrderItemMapper;
import cn.aiedge.wms.borrow.mapper.WmsBorrowOrderMapper;
import cn.aiedge.wms.borrow.mapper.WmsBorrowReturnItemMapper;
import cn.aiedge.wms.borrow.mapper.WmsBorrowReturnMapper;
import cn.aiedge.wms.borrow.service.BorrowService;
import cn.aiedge.wms.entity.WmsBorrowOrder;
import cn.aiedge.wms.entity.WmsBorrowOrderItem;
import cn.aiedge.wms.entity.WmsBorrowReturn;
import cn.aiedge.wms.entity.WmsBorrowReturnItem;
import cn.aiedge.wms.entity.WmsInventory;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.inventory.mapper.WmsInventoryMapper;
import cn.aiedge.wms.inventory.service.InventoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    /** 库存异动来源类型 */
    private static final String SRC_BORROW_IN = "WMS_BORROW_IN";
    private static final String SRC_BORROW_OUT = "WMS_BORROW_OUT";
    private static final String SRC_BORROW_IN_RETURN = "WMS_BORROW_IN_RETURN";
    private static final String SRC_BORROW_OUT_RETURN = "WMS_BORROW_OUT_RETURN";

    private final WmsBorrowOrderMapper orderMapper;
    private final WmsBorrowOrderItemMapper itemMapper;
    private final WmsBorrowReturnMapper returnMapper;
    private final WmsBorrowReturnItemMapper returnItemMapper;
    private final InventoryService inventoryService;
    private final WmsInventoryMapper inventoryMapper;

    @Override
    public Page<WmsBorrowOrder> pageOrder(Page<WmsBorrowOrder> page, WmsBorrowOrder query) {
        LambdaQueryWrapper<WmsBorrowOrder> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsBorrowOrder::getId, query.getId());
            }
            if (query.getOrderNo() != null) {
                wrapper.like(WmsBorrowOrder::getOrderNo, query.getOrderNo());
            }
            if (query.getDirection() != null) {
                wrapper.eq(WmsBorrowOrder::getDirection, query.getDirection());
            }
            if (query.getPartnerId() != null) {
                wrapper.eq(WmsBorrowOrder::getPartnerId, query.getPartnerId());
            }
            if (query.getPartnerName() != null) {
                wrapper.like(WmsBorrowOrder::getPartnerName, query.getPartnerName());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsBorrowOrder::getWarehouseId, query.getWarehouseId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsBorrowOrder::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(WmsBorrowOrder::getId);
        return orderMapper.selectPage(page, wrapper);
    }

    @Override
    public WmsBorrowOrderVO getOrderDetail(Long id) {
        WmsBorrowOrder order = orderMapper.selectById(id);
        if (order == null) return null;
        WmsBorrowOrderVO vo = new WmsBorrowOrderVO();
        org.springframework.beans.BeanUtils.copyProperties(order, vo);
        vo.setItems(listItems(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsBorrowOrder createOrder(WmsBorrowOrderVO order) {
        validateDirection(order.getDirection());
        order.setId(null);
        order.setOrderNo(generateOrderNo(order.getDirection()));
        order.setStatus(STATUS_DRAFT);
        order.setReturnedQuantity(BigDecimal.ZERO);
        order.setTotalQuantity(sumQuantity(order.getItems()));
        orderMapper.insert(order);
        saveItems(order.getId(), order.getItems());
        log.info("新建借进借出单: id={}, orderNo={}, direction={}", order.getId(), order.getOrderNo(), order.getDirection());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(WmsBorrowOrderVO order) {
        WmsBorrowOrder existing = orderMapper.selectById(order.getId());
        if (existing == null) throw new WmsBusinessException("借进借出单不存在: " + order.getId());
        if (existing.getStatus() != STATUS_DRAFT) {
            throw new WmsBusinessException(String.format("单据[%s]仅草稿状态可修改", existing.getOrderNo()));
        }
        validateDirection(order.getDirection());
        // 单号/状态/已归还数量不允许改
        order.setOrderNo(existing.getOrderNo());
        order.setStatus(STATUS_DRAFT);
        order.setReturnedQuantity(existing.getReturnedQuantity());
        order.setTotalQuantity(sumQuantity(order.getItems()));
        orderMapper.updateById(order);
        // 明细整体替换（先删后插）
        LambdaQueryWrapper<WmsBorrowOrderItem> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsBorrowOrderItem::getOrderId, existing.getId());
        itemMapper.delete(delWrapper);
        saveItems(existing.getId(), order.getItems());
        log.info("更新借进借出单: id={}, orderNo={}", existing.getId(), existing.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrder(Long id) {
        WmsBorrowOrder existing = orderMapper.selectById(id);
        if (existing == null) throw new WmsBusinessException("借进借出单不存在: " + id);
        if (existing.getStatus() != STATUS_DRAFT && existing.getStatus() != STATUS_CANCELLED) {
            throw new WmsBusinessException(String.format("单据[%s]仅草稿/已取消状态可删除", existing.getOrderNo()));
        }
        orderMapper.deleteById(id);
        LambdaQueryWrapper<WmsBorrowOrderItem> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsBorrowOrderItem::getOrderId, id);
        itemMapper.delete(delWrapper);
        log.info("删除借进借出单: id={}, orderNo={}", id, existing.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        WmsBorrowOrder order = getOrThrow(id);
        if (order.getStatus() != STATUS_DRAFT) {
            throw new WmsBusinessException(String.format("单据[%s]仅草稿状态可提交审批", order.getOrderNo()));
        }
        order.setStatus(STATUS_PENDING_APPROVAL);
        orderMapper.updateById(order);
        log.info("提交审批: id={}, orderNo={}", id, order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long operatorId, String operatorName) {
        WmsBorrowOrder order = getOrThrow(id);
        if (order.getStatus() != STATUS_PENDING_APPROVAL) {
            throw new WmsBusinessException(String.format("单据[%s]仅待审批状态可审批", order.getOrderNo()));
        }
        List<WmsBorrowOrderItem> items = listItems(id);
        if (items.isEmpty()) {
            throw new WmsBusinessException(String.format("单据[%s]无明细，不能审批", order.getOrderNo()));
        }
        String traceId = UUID.randomUUID().toString();
        boolean borrowIn = order.getDirection() == DIRECTION_IN;
        for (WmsBorrowOrderItem item : items) {
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            if (borrowIn) {
                // 借进：库存增加
                ensureInventoryRow(item, order);
                inventoryService.increase(item.getProductId(), order.getWarehouseId(), null, null,
                        item.getQuantity(), traceId, SRC_BORROW_IN, order.getId(), order.getOrderNo(),
                        operatorId, operatorName);
            } else {
                // 借出：库存扣减
                inventoryService.decrease(item.getProductId(), order.getWarehouseId(), null, null,
                        item.getQuantity(), traceId, SRC_BORROW_OUT, order.getId(), order.getOrderNo(),
                        operatorId, operatorName);
            }
        }
        order.setStatus(STATUS_APPROVED);
        orderMapper.updateById(order);
        log.info("审批通过: id={}, orderNo={}, direction={}, traceId={}", id, order.getOrderNo(), order.getDirection(), traceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        WmsBorrowOrder order = getOrThrow(id);
        if (order.getStatus() != STATUS_DRAFT && order.getStatus() != STATUS_PENDING_APPROVAL) {
            throw new WmsBusinessException(String.format("单据[%s]仅草稿/待审批状态可取消", order.getOrderNo()));
        }
        order.setStatus(STATUS_CANCELLED);
        orderMapper.updateById(order);
        log.info("取消单据: id={}, orderNo={}", id, order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsBorrowReturn returnOrder(BorrowReturnRequest request) {
        WmsBorrowOrder order = getOrThrow(request.getOrderId());
        if (order.getStatus() != STATUS_APPROVED && order.getStatus() != STATUS_PARTIAL_RETURNED) {
            throw new WmsBusinessException(String.format("单据[%s]当前状态不允许归还", order.getOrderNo()));
        }
        boolean borrowIn = order.getDirection() == DIRECTION_IN;
        String traceId = UUID.randomUUID().toString();
        BigDecimal orderReturned = nvl(order.getReturnedQuantity());

        // 归还记录头
        WmsBorrowReturn ret = new WmsBorrowReturn();
        ret.setOrderId(order.getId());
        ret.setReturnDate(request.getReturnDate() != null ? request.getReturnDate() : LocalDate.now());
        ret.setOperatorId(request.getOperatorId());
        ret.setOperatorName(request.getOperatorName());
        ret.setRemark(request.getRemark());
        returnMapper.insert(ret);

        for (WmsBorrowReturnItem ri : request.getItems()) {
            if (ri.getQuantity() == null || ri.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new WmsBusinessException("归还数量必须大于0");
            }
            WmsBorrowOrderItem item = itemMapper.selectById(ri.getOrderItemId());
            if (item == null || !item.getOrderId().equals(order.getId())) {
                throw new WmsBusinessException("归还明细不属于本单据: orderItemId=" + ri.getOrderItemId());
            }
            BigDecimal remaining = nvl(item.getQuantity()).subtract(nvl(item.getReturnedQuantity()));
            if (ri.getQuantity().compareTo(remaining) > 0) {
                throw new WmsBusinessException(String.format("商品[%s]归还数量超出未归还数量: 本次=%s, 剩余=%s",
                        item.getProductCode(), ri.getQuantity(), remaining));
            }
            // 累计明细已归还数量（UpdateWrapper 更新，绕开乐观锁，明细表无 version 列）
            LambdaUpdateWrapper<WmsBorrowOrderItem> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WmsBorrowOrderItem::getId, item.getId())
                    .set(WmsBorrowOrderItem::getReturnedQuantity, nvl(item.getReturnedQuantity()).add(ri.getQuantity()));
            itemMapper.update(null, updateWrapper);

            // 库存反向回冲：借进归还=库存扣减（还给对方），借出归还=库存增加（收回）
            if (borrowIn) {
                inventoryService.decrease(item.getProductId(), order.getWarehouseId(), null, null,
                        ri.getQuantity(), traceId, SRC_BORROW_IN_RETURN, ret.getId(), order.getOrderNo(),
                        request.getOperatorId(), request.getOperatorName());
            } else {
                ensureInventoryRow(item, order);
                inventoryService.increase(item.getProductId(), order.getWarehouseId(), null, null,
                        ri.getQuantity(), traceId, SRC_BORROW_OUT_RETURN, ret.getId(), order.getOrderNo(),
                        request.getOperatorId(), request.getOperatorName());
            }

            // 归还记录明细
            ri.setId(null);
            ri.setReturnId(ret.getId());
            ri.setProductId(item.getProductId());
            ri.setProductCode(item.getProductCode());
            ri.setProductName(item.getProductName());
            returnItemMapper.insert(ri);

            orderReturned = orderReturned.add(ri.getQuantity());
        }

        // 回写头表累计与状态
        order.setReturnedQuantity(orderReturned);
        boolean fullyReturned = orderReturned.compareTo(nvl(order.getTotalQuantity())) >= 0;
        order.setStatus(fullyReturned ? STATUS_RETURNED : STATUS_PARTIAL_RETURNED);
        orderMapper.updateById(order);
        log.info("归还登记: orderId={}, orderNo={}, returnId={}, returned={}, status={}",
                order.getId(), order.getOrderNo(), ret.getId(), orderReturned, order.getStatus());
        return ret;
    }

    @Override
    public Page<WmsBorrowReturn> pageReturn(Page<WmsBorrowReturn> page, Long orderId) {
        LambdaQueryWrapper<WmsBorrowReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(orderId != null, WmsBorrowReturn::getOrderId, orderId);
        wrapper.orderByDesc(WmsBorrowReturn::getId);
        return returnMapper.selectPage(page, wrapper);
    }

    @Override
    public List<WmsBorrowReturnItem> listReturnItems(Long returnId) {
        LambdaQueryWrapper<WmsBorrowReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsBorrowReturnItem::getReturnId, returnId);
        return returnItemMapper.selectList(wrapper);
    }

    @Override
    public String generateOrderNo(Integer direction) {
        String prefix = (direction != null && direction == DIRECTION_OUT) ? "JC" : "JJ";
        String head = prefix + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<WmsBorrowOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(WmsBorrowOrder::getOrderNo, head);
        wrapper.orderByDesc(WmsBorrowOrder::getOrderNo);
        wrapper.last("LIMIT 1");
        WmsBorrowOrder last = orderMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null) {
            seq = Integer.parseInt(last.getOrderNo().substring(head.length())) + 1;
        }
        return head + String.format("%03d", seq);
    }

    // ==================== 私有方法 ====================

    private WmsBorrowOrder getOrThrow(Long id) {
        WmsBorrowOrder order = orderMapper.selectById(id);
        if (order == null) throw new WmsBusinessException("借进借出单不存在: " + id);
        return order;
    }

    private List<WmsBorrowOrderItem> listItems(Long orderId) {
        LambdaQueryWrapper<WmsBorrowOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsBorrowOrderItem::getOrderId, orderId);
        wrapper.orderByAsc(WmsBorrowOrderItem::getLineNo);
        return itemMapper.selectList(wrapper);
    }

    private void saveItems(Long orderId, List<WmsBorrowOrderItem> items) {
        if (items == null) return;
        int lineNo = 1;
        for (WmsBorrowOrderItem item : items) {
            item.setId(null);
            item.setOrderId(orderId);
            item.setLineNo(lineNo++);
            if (item.getReturnedQuantity() == null) item.setReturnedQuantity(BigDecimal.ZERO);
            if (item.getAmount() == null && item.getQuantity() != null && item.getPrice() != null) {
                item.setAmount(item.getQuantity().multiply(item.getPrice()));
            }
            itemMapper.insert(item);
        }
    }

    /**
     * 库存增加前确保存在仓库级库存行（无库位/无批次），补齐商品信息.
     */
    private void ensureInventoryRow(WmsBorrowOrderItem item, WmsBorrowOrder order) {
        WmsInventory existing = inventoryService.getByUniqueKey(item.getProductId(), order.getWarehouseId(), null, null);
        if (existing != null) return;
        WmsInventory inventory = new WmsInventory();
        inventory.setProductId(item.getProductId());
        inventory.setProductCode(item.getProductCode());
        inventory.setProductName(item.getProductName());
        inventory.setProductSpec(item.getProductSpec());
        inventory.setProductUnit(item.getUnit());
        inventory.setWarehouseId(order.getWarehouseId());
        inventory.setWarehouseName(order.getWarehouseName());
        inventory.setQuantity(BigDecimal.ZERO);
        inventory.setAvailableQuantity(BigDecimal.ZERO);
        inventory.setFrozenQuantity(BigDecimal.ZERO);
        inventoryMapper.insert(inventory);
    }

    private BigDecimal sumQuantity(List<WmsBorrowOrderItem> items) {
        if (items == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (WmsBorrowOrderItem item : items) {
            if (item.getQuantity() != null) total = total.add(item.getQuantity());
        }
        return total;
    }

    private void validateDirection(Integer direction) {
        if (direction == null || (direction != DIRECTION_IN && direction != DIRECTION_OUT)) {
            throw new WmsBusinessException("方向不合法: 1-借进 2-借出");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
