package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockSplit;
import cn.aiedge.erp.stock.entity.StockSplitItem;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.mapper.StockSplitItemMapper;
import cn.aiedge.erp.stock.mapper.StockSplitMapper;
import cn.aiedge.erp.stock.service.StockSplitService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockSplitServiceImpl extends ServiceImpl<StockSplitMapper, StockSplit> implements StockSplitService {

    @Autowired
    private StockSplitItemMapper stockSplitItemMapper;

    @Autowired
    private StockMapper stockMapper;

    private String generateSplitNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "SP" + dateStr + randomStr;
    }

    @Override
    public Page<StockSplit> pageList(String keyword, Long warehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockSplit> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(StockSplit::getSplitNo, keyword)
                    .or().like(StockSplit::getProductName, keyword));
        }
        wrapper.eq(warehouseId != null, StockSplit::getWarehouseId, warehouseId)
                .eq(status != null, StockSplit::getStatus, status)
                .orderByDesc(StockSplit::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockSplit createSplit(StockSplit split, List<StockSplitItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        split.setTenantId(userId);
        split.setSplitNo(generateSplitNo());
        split.setStatus(0);
        split.setApplicantId(userId);
        split.setApplyTime(LocalDateTime.now());
        split.setCreateBy(userId);
        split.setCreateTime(LocalDateTime.now());
        this.save(split);

        if (items != null && !items.isEmpty()) {
            BigDecimal subTotalCost = BigDecimal.ZERO;
            for (StockSplitItem item : items) {
                item.setSplitId(split.getId());
                if (item.getQuantity() == null) {
                    item.setQuantity(BigDecimal.ONE);
                }
                if (item.getUnitCost() == null) {
                    item.setUnitCost(BigDecimal.ZERO);
                }
                item.setCost(item.getQuantity().multiply(item.getUnitCost()));
                item.setCreateTime(LocalDateTime.now());
                stockSplitItemMapper.insert(item);
                subTotalCost = subTotalCost.add(item.getCost());
            }
            split.setSubTotalCost(subTotalCost);
            split.setTotalItems(items.size());

            // Calculate allocation ratios
            if (split.getOutputTotalCost() != null && split.getOutputTotalCost().compareTo(BigDecimal.ZERO) > 0) {
                for (StockSplitItem item : items) {
                    BigDecimal ratio = item.getCost().divide(split.getOutputTotalCost(), 4, RoundingMode.HALF_UP);
                    item.setAllocationRatio(ratio);
                    stockSplitItemMapper.updateById(item);
                }
            }

            this.updateById(split);
        }

        return split;
    }

    @Override
    public StockSplit getItems(Long splitId) {
        StockSplit split = this.getById(splitId);
        if (split == null) {
            throw BusinessException.notFound("拆分单不存在");
        }
        return split;
    }

    @Override
    public List<StockSplitItem> getItemList(Long splitId) {
        return stockSplitItemMapper.selectList(
                new LambdaQueryWrapper<StockSplitItem>()
                        .eq(StockSplitItem::getSplitId, splitId)
                        .orderByAsc(StockSplitItem::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockSplit submitForApproval(Long id) {
        StockSplit split = this.getById(id);
        if (split == null) {
            throw BusinessException.notFound("拆分单不存在");
        }
        if (split.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的拆分单可以提交审批");
        }
        split.setStatus(1);
        split.setUpdateTime(LocalDateTime.now());
        this.updateById(split);
        return split;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockSplit approve(Long id, Long approverId, String note) {
        StockSplit split = this.getById(id);
        if (split == null) {
            throw BusinessException.notFound("拆分单不存在");
        }
        if (split.getStatus() != 1) {
            throw BusinessException.badRequest("只有待审批状态的拆分单可以审批通过");
        }
        split.setStatus(2);
        split.setApprovedBy(approverId);
        split.setApprovedTime(LocalDateTime.now());
        split.setApprovedNote(note);
        split.setUpdateTime(LocalDateTime.now());
        this.updateById(split);
        return split;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockSplit reject(Long id, String reason) {
        StockSplit split = this.getById(id);
        if (split == null) {
            throw BusinessException.notFound("拆分单不存在");
        }
        if (split.getStatus() != 1) {
            throw BusinessException.badRequest("只有待审批状态的拆分单可以拒绝");
        }
        split.setStatus(4);
        split.setApprovedNote(reason);
        split.setUpdateTime(LocalDateTime.now());
        this.updateById(split);
        return split;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockSplit execute(Long id) {
        StockSplit split = this.getById(id);
        if (split == null) {
            throw BusinessException.notFound("拆分单不存在");
        }
        if (split.getStatus() != 2) {
            throw BusinessException.badRequest("只有已审核状态的拆分单可以执行");
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // Decrease stock for the original product
        Stock originalStock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getWarehouseId, split.getWarehouseId())
                        .eq(Stock::getProductId, split.getProductId())
        );
        if (originalStock == null) {
            throw BusinessException.badRequest("产品「" + split.getProductName() + "」库存不足，无法执行拆分");
        }
        BigDecimal neededQuantity = split.getSplitQuantity();
        if (originalStock.getQuantity().compareTo(neededQuantity) < 0) {
            throw BusinessException.badRequest("产品「" + split.getProductName() + "」库存不足，需要" +
                    neededQuantity + "，当前库存" + originalStock.getQuantity());
        }
        originalStock.setQuantity(originalStock.getQuantity().subtract(neededQuantity));
        originalStock.setAvailableQuantity(originalStock.getAvailableQuantity().subtract(neededQuantity));
        originalStock.setUpdateTime(LocalDateTime.now());
        originalStock.setUpdateBy(userId);
        stockMapper.updateById(originalStock);

        // Increase stock for each sub-item with cost allocation
        List<StockSplitItem> items = getItemList(id);
        BigDecimal totalCost = split.getOutputTotalCost() != null ? split.getOutputTotalCost() : BigDecimal.ZERO;
        BigDecimal totalQuantity = items.stream()
                .map(StockSplitItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (StockSplitItem item : items) {
            Stock subStock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, split.getWarehouseId())
                            .eq(Stock::getProductId, item.getProductId())
            );

            BigDecimal addQuantity = item.getQuantity().multiply(neededQuantity);

            if (subStock == null) {
                subStock = new Stock();
                subStock.setTenantId(split.getTenantId());
                subStock.setWarehouseId(split.getWarehouseId());
                subStock.setWarehouseName(split.getWarehouseName());
                subStock.setProductId(item.getProductId());
                subStock.setProductCode(item.getProductCode());
                subStock.setProductName(item.getProductName());
                subStock.setQuantity(addQuantity);
                subStock.setAvailableQuantity(addQuantity);
                subStock.setFrozenQuantity(BigDecimal.ZERO);
                subStock.setCreateTime(LocalDateTime.now());
                subStock.setCreateBy(userId);
                stockMapper.insert(subStock);
            } else {
                subStock.setQuantity(subStock.getQuantity().add(addQuantity));
                subStock.setAvailableQuantity(subStock.getAvailableQuantity().add(addQuantity));
                subStock.setUpdateTime(LocalDateTime.now());
                subStock.setUpdateBy(userId);
                stockMapper.updateById(subStock);
            }

            // Allocate cost based on allocation ratio or proportionally
            if (totalCost.compareTo(BigDecimal.ZERO) > 0 && totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal allocatedCost;
                if (item.getAllocationRatio() != null && item.getAllocationRatio().compareTo(BigDecimal.ZERO) > 0) {
                    allocatedCost = totalCost.multiply(item.getAllocationRatio());
                } else {
                    allocatedCost = totalCost.multiply(item.getQuantity()).divide(totalQuantity, 2, RoundingMode.HALF_UP);
                }
                item.setAllocationRatio(item.getAllocationRatio() != null ? item.getAllocationRatio()
                        : item.getQuantity().divide(totalQuantity, 4, RoundingMode.HALF_UP));
                item.setCost(allocatedCost);
                stockSplitItemMapper.updateById(item);
            }
        }

        split.setStatus(3);
        split.setExecutedBy(userId);
        split.setExecutedTime(LocalDateTime.now());
        split.setUpdateTime(LocalDateTime.now());
        this.updateById(split);

        return split;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockSplit cancel(Long id, String reason) {
        StockSplit split = this.getById(id);
        if (split == null) {
            throw BusinessException.notFound("拆分单不存在");
        }
        if (split.getStatus() == 3) {
            throw BusinessException.badRequest("已执行的拆分单不能取消");
        }
        split.setStatus(5);
        split.setRemark(reason);
        split.setUpdateTime(LocalDateTime.now());
        this.updateById(split);
        return split;
    }
}
