package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockSplitQuery;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockSplit;
import cn.aiedge.erp.stock.entity.StockSplitItem;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.mapper.StockSplitItemMapper;
import cn.aiedge.erp.stock.mapper.StockSplitMapper;
import cn.aiedge.erp.stock.service.StockSplitService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockSplitServiceImpl extends ServiceImpl<StockSplitMapper, StockSplit> implements StockSplitService {

    @Autowired
    private StockSplitItemMapper stockSplitItemMapper;

    @Autowired
    private StockMapper stockMapper;

    @Override
    public String generateNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "CXD-" + dateStr + randomStr;
    }

    @Override
    public Page<StockSplit> pageList(StockSplitQuery query) {
        if (query == null) query = new StockSplitQuery();
        LambdaQueryWrapper<StockSplit> wrapper = new LambdaQueryWrapper<>();
        String keyword = StrUtil.trimToNull(query.getKeyword());
        if (keyword != null) {
            wrapper.and(w -> w.like(StockSplit::getSplitNo, keyword)
                    .or().like(StockSplit::getProductName, keyword)
                    .or().like(StockSplit::getHandlerName, keyword));
        }
        wrapper.like(StrUtil.isNotBlank(query.getSplitNo()), StockSplit::getSplitNo, StrUtil.trimToNull(query.getSplitNo()))
                .like(StrUtil.isNotBlank(query.getHandlerName()), StockSplit::getHandlerName, StrUtil.trimToNull(query.getHandlerName()))
                .like(StrUtil.isNotBlank(query.getDeptName()), StockSplit::getDeptName, StrUtil.trimToNull(query.getDeptName()))
                .like(StrUtil.isNotBlank(query.getCreatorName()), StockSplit::getCreatorName, StrUtil.trimToNull(query.getCreatorName()))
                .like(StrUtil.isNotBlank(query.getBookkeeperName()), StockSplit::getBookkeeperName, StrUtil.trimToNull(query.getBookkeeperName()))
                .like(StrUtil.isNotBlank(query.getRemark()), StockSplit::getRemark, StrUtil.trimToNull(query.getRemark()))
                .like(StrUtil.isNotBlank(query.getSummary()), StockSplit::getSummary, StrUtil.trimToNull(query.getSummary()))
                .eq(query.getInWarehouseId() != null, StockSplit::getInWarehouseId, query.getInWarehouseId())
                .eq(query.getOutWarehouseId() != null, StockSplit::getOutWarehouseId, query.getOutWarehouseId())
                .eq(query.getStatus() != null, StockSplit::getStatus, query.getStatus());
        if (StrUtil.isNotBlank(query.getDateStart())) {
            wrapper.ge(StockSplit::getSplitDate, LocalDate.parse(query.getDateStart()).atStartOfDay());
        }
        if (StrUtil.isNotBlank(query.getDateEnd())) {
            wrapper.le(StockSplit::getSplitDate, LocalDate.parse(query.getDateEnd()).atTime(LocalTime.MAX));
        }
        wrapper.orderByDesc(StockSplit::getCreateTime);
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockSplit createSplit(StockSplit split, List<StockSplitItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        split.setTenantId(1L);
        split.setSplitNo(generateNo());
        split.setStatus(0);
        split.setApplicantId(userId);
        String loginName = getLoginName();
        split.setApplicantName(loginName);
        if (StrUtil.isBlank(split.getCreatorName())) {
            split.setCreatorName(loginName);
        }
        split.setApplyTime(LocalDateTime.now());
        split.setCreateBy(userId);
        split.setCreateTime(LocalDateTime.now());
        if (split.getSplitDate() == null) {
            split.setSplitDate(LocalDateTime.now());
        }
        if (split.getInWarehouseId() == null) {
            split.setInWarehouseId(split.getWarehouseId());
        }
        if (split.getOutWarehouseId() == null) {
            split.setOutWarehouseId(split.getWarehouseId());
        }
        this.save(split);

        BigDecimal subTotalCost = saveItems(split.getId(), items);
        BigDecimal totalCost = split.getTotalCost() != null ? split.getTotalCost() : subTotalCost;
        if (split.getOutputTotalCost() == null) {
            split.setOutputTotalCost(totalCost);
        }
        split.setSubTotalCost(subTotalCost);
        split.setTotalCost(totalCost);
        split.setTotalItems(items == null ? 0 : items.size());
        this.updateById(split);
        applyAllocationRatio(items, totalCost);

        return split;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockSplit updateSplit(Long id, StockSplit split, List<StockSplitItem> items) {
        StockSplit existing = this.getById(id);
        if (existing == null) {
            throw BusinessException.notFound("拆分单不存在");
        }
        if (existing.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的拆分单可以修改");
        }
        split.setId(id);
        split.setTenantId(1L);
        split.setSplitNo(existing.getSplitNo());
        split.setStatus(0);
        split.setApplicantId(existing.getApplicantId());
        split.setApplicantName(existing.getApplicantName());
        split.setApplyTime(existing.getApplyTime());
        split.setCreateBy(existing.getCreateBy());
        split.setCreateTime(existing.getCreateTime());
        if (split.getSplitDate() == null) {
            split.setSplitDate(existing.getSplitDate());
        }
        if (split.getInWarehouseId() == null) {
            split.setInWarehouseId(existing.getInWarehouseId() != null ? existing.getInWarehouseId() : existing.getWarehouseId());
        }
        if (split.getOutWarehouseId() == null) {
            split.setOutWarehouseId(existing.getOutWarehouseId() != null ? existing.getOutWarehouseId() : existing.getWarehouseId());
        }
        this.updateById(split);

        // 重建明细
        stockSplitItemMapper.delete(
                new LambdaQueryWrapper<StockSplitItem>().eq(StockSplitItem::getSplitId, id)
        );
        BigDecimal subTotalCost = saveItems(id, items);
        BigDecimal totalCost = split.getTotalCost() != null ? split.getTotalCost() : subTotalCost;
        if (split.getOutputTotalCost() == null) {
            split.setOutputTotalCost(totalCost);
        }
        split.setSubTotalCost(subTotalCost);
        split.setTotalCost(totalCost);
        split.setTotalItems(items == null ? 0 : items.size());
        this.updateById(split);
        applyAllocationRatio(items, totalCost);

        return split;
    }

    /**
     * 保存拆分明细（入库原料），并计算子件总成本
     */
    private BigDecimal saveItems(Long splitId, List<StockSplitItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal subTotalCost = BigDecimal.ZERO;
        for (StockSplitItem item : items) {
            item.setId(null);
            item.setSplitId(splitId);
            item.setTenantId(1L);
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
        return subTotalCost;
    }

    /**
     * 按成本占比回填拆分产出原料的分配比例
     */
    private void applyAllocationRatio(List<StockSplitItem> items, BigDecimal totalCost) {
        if (items == null || items.isEmpty() || totalCost == null || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        for (StockSplitItem item : items) {
            if (item.getId() == null) {
                continue;
            }
            BigDecimal ratio = item.getCost().divide(totalCost, 4, RoundingMode.HALF_UP);
            item.setAllocationRatio(ratio);
            stockSplitItemMapper.updateById(item);
        }
    }

    private String getLoginName() {
        try {
            Object name = StpUtil.getSession().get("name");
            return name != null ? name.toString() : "";
        } catch (Exception e) {
            return "";
        }
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

        // 成品出库仓库（兼容旧数据：无双仓库时回退到 warehouse_id）
        Long productWarehouseId = split.getOutWarehouseId() != null
                ? split.getOutWarehouseId()
                : split.getWarehouseId();
        // 原料入库仓库
        Long materialWarehouseId = split.getInWarehouseId() != null
                ? split.getInWarehouseId()
                : split.getWarehouseId();

        // Decrease stock for the original product from 成品仓库
        Stock originalStock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getWarehouseId, productWarehouseId)
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

        // Increase stock for each sub-item in 原料仓库 with cost allocation
        List<StockSplitItem> items = getItemList(id);
        BigDecimal totalCost = split.getOutputTotalCost() != null ? split.getOutputTotalCost() : BigDecimal.ZERO;
        BigDecimal totalQuantity = items.stream()
                .map(StockSplitItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (StockSplitItem item : items) {
            Stock subStock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, materialWarehouseId)
                            .eq(Stock::getProductId, item.getProductId())
            );

            BigDecimal addQuantity = item.getQuantity().multiply(neededQuantity);

            if (subStock == null) {
                subStock = new Stock();
                subStock.setTenantId(split.getTenantId());
                subStock.setWarehouseId(materialWarehouseId);
                subStock.setWarehouseName(split.getInWarehouseName() != null
                        ? split.getInWarehouseName() : split.getWarehouseName());
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
        split.setBookkeeperId(userId);
        split.setBookkeeperName(getLoginName());
        split.setBookkeepingTime(LocalDateTime.now());
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
        split.setCancelReason(reason);
        split.setUpdateTime(LocalDateTime.now());
        this.updateById(split);
        return split;
    }
}
