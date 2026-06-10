package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockCostAdjust;
import cn.aiedge.erp.stock.entity.StockCostAdjustItem;
import cn.aiedge.erp.stock.mapper.StockCostAdjustItemMapper;
import cn.aiedge.erp.stock.mapper.StockCostAdjustMapper;
import cn.aiedge.erp.stock.service.StockCostAdjustService;
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
public class StockCostAdjustServiceImpl extends ServiceImpl<StockCostAdjustMapper, StockCostAdjust> implements StockCostAdjustService {

    @Autowired
    private StockCostAdjustItemMapper stockCostAdjustItemMapper;

    @Override
    public Page<StockCostAdjust> pageList(String keyword, Long warehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockCostAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, StockCostAdjust::getAdjustNo, keyword)
                .eq(warehouseId != null, StockCostAdjust::getWarehouseId, warehouseId)
                .eq(status != null, StockCostAdjust::getStatus, status)
                .orderByDesc(StockCostAdjust::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    private String generateAdjustNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "CA" + dateStr + randomStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust createAdjust(StockCostAdjust adjust, List<StockCostAdjustItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        adjust.setTenantId(userId);
        adjust.setAdjustNo(generateAdjustNo());
        adjust.setStatus(0);
        adjust.setApplicantId(userId);
        adjust.setApplyTime(LocalDateTime.now());
        if (adjust.getTotalItems() == null) {
            adjust.setTotalItems(items != null ? items.size() : 0);
        }
        this.save(adjust);

        if (items != null && !items.isEmpty()) {
            for (StockCostAdjustItem item : items) {
                item.setAdjustId(adjust.getId());
                item.setWarehouseId(adjust.getWarehouseId());
                stockCostAdjustItemMapper.insert(item);
            }
        }

        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust submitForApproval(Long id) {
        StockCostAdjust adjust = this.getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("成本调价单不存在");
        }
        if (adjust.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的成本调价单可以提交审批");
        }

        adjust.setStatus(1);
        adjust.setApplyTime(LocalDateTime.now());
        this.updateById(adjust);
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust approve(Long id, Long approverId, String note) {
        StockCostAdjust adjust = this.getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("成本调价单不存在");
        }
        if (adjust.getStatus() != 1) {
            throw BusinessException.badRequest("只有待审批状态的成本调价单可以审批通过");
        }

        adjust.setStatus(2);
        adjust.setApprovedBy(approverId);
        adjust.setApprovedTime(LocalDateTime.now());
        adjust.setApprovedNote(note);
        this.updateById(adjust);
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust reject(Long id, String reason) {
        StockCostAdjust adjust = this.getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("成本调价单不存在");
        }
        if (adjust.getStatus() != 1) {
            throw BusinessException.badRequest("只有待审批状态的成本调价单可以拒绝");
        }

        adjust.setStatus(4);
        adjust.setApprovedNote(reason);
        this.updateById(adjust);
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust execute(Long id) {
        StockCostAdjust adjust = this.getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("成本调价单不存在");
        }
        if (adjust.getStatus() != 2) {
            throw BusinessException.badRequest("只有已审批状态的成本调价单可以执行");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        List<StockCostAdjustItem> items = getItems(id);

        BigDecimal totalAdjustAmount = BigDecimal.ZERO;
        for (StockCostAdjustItem item : items) {
            BigDecimal diff = BigDecimal.ZERO;
            if (item.getOldCost() != null && item.getNewCost() != null) {
                diff = item.getNewCost().subtract(item.getOldCost());
            }
            if (item.getCurrentQuantity() != null) {
                diff = diff.multiply(item.getCurrentQuantity());
            }
            item.setDiffAmount(diff);
            totalAdjustAmount = totalAdjustAmount.add(diff);
            stockCostAdjustItemMapper.updateById(item);
        }

        adjust.setStatus(3);
        adjust.setExecutedBy(userId);
        adjust.setExecutedTime(LocalDateTime.now());
        adjust.setTotalAdjustAmount(totalAdjustAmount);
        this.updateById(adjust);

        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust cancel(Long id, String reason) {
        StockCostAdjust adjust = this.getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("成本调价单不存在");
        }
        if (adjust.getStatus() == 3) {
            throw BusinessException.badRequest("已执行的成本调价单不能取消");
        }
        if (adjust.getStatus() == 5) {
            throw BusinessException.badRequest("成本调价单已取消");
        }

        adjust.setStatus(5);
        adjust.setRemark(reason);
        this.updateById(adjust);
        return adjust;
    }

    @Override
    public List<StockCostAdjustItem> getItems(Long adjustId) {
        return stockCostAdjustItemMapper.selectList(
                new LambdaQueryWrapper<StockCostAdjustItem>()
                        .eq(StockCostAdjustItem::getAdjustId, adjustId)
                        .orderByAsc(StockCostAdjustItem::getId)
        );
    }
}
