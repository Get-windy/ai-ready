package cn.aiedge.erp.sale.salereturn.service.impl;

import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnItemMapper;
import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnMapper;
import cn.aiedge.erp.sale.salereturn.service.SaleReturnService;
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
public class SaleReturnServiceImpl extends ServiceImpl<SaleReturnMapper, SaleReturn> implements SaleReturnService {

    @Autowired
    private SaleReturnItemMapper saleReturnItemMapper;

    @Override
    public SaleReturn getByReturnNo(String returnNo) {
        return this.lambdaQuery()
                .eq(SaleReturn::getReturnNo, returnNo)
                .one();
    }

    @Override
    public Page<SaleReturn> pageList(String keyword, Long customerId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<SaleReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, SaleReturn::getReturnNo, keyword)
                .eq(customerId != null, SaleReturn::getCustomerId, customerId)
                .eq(status != null, SaleReturn::getStatus, status)
                .orderByDesc(SaleReturn::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<SaleReturn> exportList(String keyword, Long customerId, Integer status) {
        LambdaQueryWrapper<SaleReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, SaleReturn::getReturnNo, keyword)
                .eq(customerId != null, SaleReturn::getCustomerId, customerId)
                .eq(status != null, SaleReturn::getStatus, status)
                .orderByDesc(SaleReturn::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public List<SaleReturn> listByCustomerId(Long customerId) {
        return this.lambdaQuery()
                .eq(SaleReturn::getCustomerId, customerId)
                .orderByDesc(SaleReturn::getCreateTime)
                .list();
    }

    @Override
    public String generateReturnNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "SR" + dateStr + randomStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn createReturn(SaleReturn returnOrder, List<SaleReturnItem> items) {
        Long tenantId = StpUtil.getLoginIdAsLong();

        returnOrder.setTenantId(tenantId);
        returnOrder.setReturnNo(generateReturnNo());
        returnOrder.setStatus(0);
        returnOrder.setCreateTime(LocalDateTime.now());
        returnOrder.setCreateBy(tenantId);
        this.save(returnOrder);

        if (items != null && !items.isEmpty()) {
            for (SaleReturnItem item : items) {
                item.setTenantId(tenantId);
                item.setReturnId(returnOrder.getId());
                item.setCreateTime(LocalDateTime.now());
                saleReturnItemMapper.insert(item);
            }
        }

        calculateTotals(returnOrder.getId());

        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn submitForApproval(Long returnId) {
        SaleReturn returnOrder = this.getById(returnId);
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
    public SaleReturn approve(Long returnId, Long approverId, String note) {
        SaleReturn returnOrder = this.getById(returnId);
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
    public SaleReturn reject(Long returnId, String reason) {
        SaleReturn returnOrder = this.getById(returnId);
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
    public SaleReturn complete(Long returnId) {
        SaleReturn returnOrder = this.getById(returnId);
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
    public SaleReturn cancel(Long returnId, String reason) {
        SaleReturn returnOrder = this.getById(returnId);
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
    public List<SaleReturnItem> getItems(Long returnId) {
        return saleReturnItemMapper.selectList(
                new LambdaQueryWrapper<SaleReturnItem>()
                        .eq(SaleReturnItem::getReturnId, returnId)
                        .orderByAsc(SaleReturnItem::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnItem addItem(Long returnId, SaleReturnItem item) {
        SaleReturn returnOrder = this.getById(returnId);
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
        saleReturnItemMapper.insert(item);

        calculateTotals(returnId);

        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnItem updateItem(Long itemId, SaleReturnItem item) {
        SaleReturnItem existing = saleReturnItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("退货明细不存在");
        }

        SaleReturn returnOrder = this.getById(existing.getReturnId());
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
        saleReturnItemMapper.updateById(existing);

        calculateTotals(existing.getReturnId());

        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        SaleReturnItem item = saleReturnItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("退货明细不存在");
        }

        SaleReturn returnOrder = this.getById(item.getReturnId());
        if (returnOrder.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的退货单可以删除明细");
        }

        saleReturnItemMapper.deleteById(itemId);

        calculateTotals(item.getReturnId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long returnId) {
        List<SaleReturnItem> items = getItems(returnId);

        BigDecimal totalQuantity = items.stream()
                .map(SaleReturnItem::getReturnQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = items.stream()
                .map(i -> i.getReturnQuantity().multiply(i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SaleReturn returnOrder = this.getById(returnId);
        returnOrder.setTotalQuantity(totalQuantity);
        returnOrder.setTotalAmount(totalAmount);
        returnOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(returnOrder);
    }
}
