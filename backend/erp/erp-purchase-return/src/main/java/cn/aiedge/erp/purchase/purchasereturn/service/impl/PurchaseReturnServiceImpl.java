package cn.aiedge.erp.purchase.purchasereturn.service.impl;

import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturnItem;
import cn.aiedge.erp.purchase.purchasereturn.mapper.PurchaseReturnItemMapper;
import cn.aiedge.erp.purchase.purchasereturn.mapper.PurchaseReturnMapper;
import cn.aiedge.erp.purchase.purchasereturn.service.PurchaseReturnService;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PurchaseReturnServiceImpl extends ServiceImpl<PurchaseReturnMapper, PurchaseReturn> implements PurchaseReturnService {

    @Autowired
    private PurchaseReturnItemMapper purchaseReturnItemMapper;

    @Override
    public PurchaseReturn getByReturnNo(String returnNo) {
        return this.lambdaQuery()
                .eq(PurchaseReturn::getReturnNo, returnNo)
                .one();
    }

    @Override
    public Page<PurchaseReturn> pageList(String keyword, Long supplierId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, PurchaseReturn::getReturnNo, keyword)
                .eq(supplierId != null, PurchaseReturn::getSupplierId, supplierId)
                .eq(status != null, PurchaseReturn::getStatus, status)
                .orderByDesc(PurchaseReturn::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<PurchaseReturn> listBySupplierId(Long supplierId) {
        return this.lambdaQuery()
                .eq(PurchaseReturn::getSupplierId, supplierId)
                .orderByDesc(PurchaseReturn::getCreateTime)
                .list();
    }

    @Override
    public String generateReturnNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "PR" + dateStr + randomStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn createReturn(PurchaseReturn returnOrder, List<PurchaseReturnItem> items) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        
        returnOrder.setTenantId(tenantId);
        returnOrder.setReturnNo(generateReturnNo());
        returnOrder.setStatus(0);
        returnOrder.setCreateTime(LocalDateTime.now());
        returnOrder.setCreateBy(tenantId);
        this.save(returnOrder);

        if (items != null && !items.isEmpty()) {
            for (PurchaseReturnItem item : items) {
                item.setTenantId(tenantId);
                item.setReturnId(returnOrder.getId());
                item.setCreateTime(LocalDateTime.now());
                purchaseReturnItemMapper.insert(item);
            }
        }

        calculateTotals(returnOrder.getId());
        
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn submitForApproval(Long returnId) {
        PurchaseReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以提交审批");
        }
        
        returnOrder.setStatus(1);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn approve(Long returnId, Long approverId, String note) {
        PurchaseReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != 1) {
            throw new RuntimeException("只有待审批状态的退货单可以审批");
        }
        
        returnOrder.setStatus(2);
        returnOrder.setApprovedBy(approverId);
        returnOrder.setApprovedTime(LocalDateTime.now());
        returnOrder.setApprovedNote(note);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn reject(Long returnId, String reason) {
        PurchaseReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != 1) {
            throw new RuntimeException("只有待审批状态的退货单可以拒绝");
        }
        
        returnOrder.setStatus(3);
        returnOrder.setApprovedNote(reason);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn complete(Long returnId) {
        PurchaseReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != 2) {
            throw new RuntimeException("只有已审批状态的退货单可以完成");
        }
        
        returnOrder.setStatus(4);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn cancel(Long returnId, String reason) {
        PurchaseReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() == 4) {
            throw new RuntimeException("已完成的退货单不能取消");
        }
        
        returnOrder.setStatus(5);
        returnOrder.setRemark(reason);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
        return returnOrder;
    }

    @Override
    public List<PurchaseReturnItem> getItems(Long returnId) {
        return purchaseReturnItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseReturnItem>()
                        .eq(PurchaseReturnItem::getReturnId, returnId)
                        .orderByAsc(PurchaseReturnItem::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturnItem addItem(Long returnId, PurchaseReturnItem item) {
        PurchaseReturn returnOrder = this.getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以添加明细");
        }
        
        Long tenantId = StpUtil.getLoginIdAsLong();
        item.setTenantId(tenantId);
        item.setReturnId(returnId);
        item.setCreateTime(LocalDateTime.now());
        purchaseReturnItemMapper.insert(item);
        
        calculateTotals(returnId);
        
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturnItem updateItem(Long itemId, PurchaseReturnItem item) {
        PurchaseReturnItem existing = purchaseReturnItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("退货明细不存在");
        }
        
        PurchaseReturn returnOrder = this.getById(existing.getReturnId());
        if (returnOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以修改明细");
        }
        
        existing.setProductId(item.getProductId());
        existing.setProductName(item.getProductName());
        existing.setProductCode(item.getProductCode());
        existing.setReturnQuantity(item.getReturnQuantity());
        existing.setUnitPrice(item.getUnitPrice());
        existing.setReason(item.getReason());
        existing.setRemark(item.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        purchaseReturnItemMapper.updateById(existing);
        
        calculateTotals(existing.getReturnId());
        
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        PurchaseReturnItem item = purchaseReturnItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("退货明细不存在");
        }
        
        PurchaseReturn returnOrder = this.getById(item.getReturnId());
        if (returnOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以删除明细");
        }
        
        purchaseReturnItemMapper.deleteById(itemId);
        
        calculateTotals(item.getReturnId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long returnId) {
        List<PurchaseReturnItem> items = getItems(returnId);
        
        BigDecimal totalQuantity = items.stream()
                .map(PurchaseReturnItem::getReturnQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAmount = items.stream()
                .map(i -> i.getReturnQuantity().multiply(i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        PurchaseReturn returnOrder = this.getById(returnId);
        returnOrder.setTotalQuantity(totalQuantity);
        returnOrder.setTotalAmount(totalAmount);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
    }
}