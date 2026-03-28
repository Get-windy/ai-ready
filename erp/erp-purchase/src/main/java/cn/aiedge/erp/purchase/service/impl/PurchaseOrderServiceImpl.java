package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderMapper;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购订单服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
        implements PurchaseOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(PurchaseOrder order) {
        // 生成订单号
        order.setOrderNo(generateOrderNo());
        order.setStatus(0); // 草稿状态
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setCreateBy(StpUtil.getLoginIdAsLong());
        
        // 计算订单金额
        calculateOrderAmount(order);
        
        save(order);
        log.info("创建采购订单成功: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(PurchaseOrder order) {
        // 检查订单状态
        PurchaseOrder existing = getById(order.getId());
        if (existing == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (existing.getStatus() > 1) {
            throw BusinessException.badRequest("订单已审批，无法修改");
        }
        
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy(StpUtil.getLoginIdAsLong());
        updateById(order);
        log.info("更新采购订单成功: orderId={}", order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long orderId) {
        PurchaseOrder existing = getById(orderId);
        if (existing == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (existing.getStatus() > 1) {
            throw BusinessException.badRequest("订单已审批，无法删除");
        }
        
        removeById(orderId);
        log.info("删除采购订单成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的订单才能提交审批");
        }
        
        order.setStatus(1); // 待审批
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("提交采购订单审批: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw BusinessException.badRequest("订单不在待审批状态");
        }
        
        order.setStatus(2); // 已审批
        order.setApprovedBy(StpUtil.getLoginIdAsLong());
        order.setApprovedTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("审批通过采购订单: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long orderId, String reason) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        order.setStatus(0); // 退回草稿
        order.setRemark(order.getRemark() + " [审批拒绝: " + reason + "]");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("审批拒绝采购订单: orderId={}, reason={}", orderId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() >= 3) {
            throw BusinessException.badRequest("订单已开始入库，无法取消");
        }
        
        order.setStatus(5); // 已取消
        order.setRemark(order.getRemark() + " [取消原因: " + reason + "]");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("取消采购订单: orderId={}, reason={}", orderId, reason);
    }

    @Override
    public Page<PurchaseOrder> pageOrders(Page<PurchaseOrder> page, Long tenantId,
                                          String orderNo, Long supplierId, Integer status) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getTenantId, tenantId)
                .like(orderNo != null, PurchaseOrder::getOrderNo, orderNo)
                .eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
                .eq(status != null, PurchaseOrder::getStatus, status)
                .orderByDesc(PurchaseOrder::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public PurchaseOrder getOrderDetail(Long orderId) {
        return getById(orderId);
    }

    @Override
    public List<Object> getOrderItems(Long orderId) {
        // TODO: 实现订单明细查询
        return List.of();
    }

    // ==================== 私有方法 ====================

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "PO" + System.currentTimeMillis();
    }

    /**
     * 计算订单金额
     */
    private void calculateOrderAmount(PurchaseOrder order) {
        // TODO: 根据订单明细计算金额
        if (order.getTotalAmount() == null) {
            order.setTotalAmount(BigDecimal.ZERO);
            order.setTaxAmount(BigDecimal.ZERO);
            order.setTotalAmountWithTax(BigDecimal.ZERO);
        }
    }
}