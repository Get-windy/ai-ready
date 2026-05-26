package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockCheck;
import cn.aiedge.erp.stock.entity.StockCheckItem;
import cn.aiedge.erp.stock.enums.StockCheckStatus;
import cn.aiedge.erp.stock.mapper.StockCheckItemMapper;
import cn.aiedge.erp.stock.mapper.StockCheckMapper;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.StockCheckService;
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
public class StockCheckServiceImpl extends ServiceImpl<StockCheckMapper, StockCheck> implements StockCheckService {

    @Autowired
    private StockCheckItemMapper stockCheckItemMapper;

    @Autowired
    private StockMapper stockMapper;

    @Override
    public StockCheck getByCheckNo(String checkNo) {
        return this.lambdaQuery()
                .eq(StockCheck::getCheckNo, checkNo)
                .one();
    }

    @Override
    public Page<StockCheck> pageList(String keyword, Long warehouseId, Integer status, Integer checkType, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockCheck> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, StockCheck::getCheckNo, keyword)
                .eq(warehouseId != null, StockCheck::getWarehouseId, warehouseId)
                .eq(status != null, StockCheck::getStatus, status)
                .eq(checkType != null, StockCheck::getCheckType, checkType)
                .orderByDesc(StockCheck::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<StockCheck> listByWarehouseId(Long warehouseId) {
        return this.lambdaQuery()
                .eq(StockCheck::getWarehouseId, warehouseId)
                .orderByDesc(StockCheck::getCreateTime)
                .list();
    }

    @Override
    public String generateCheckNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "SC" + dateStr + randomStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck createCheck(StockCheck check) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        check.setTenantId(tenantId);
        check.setCheckNo(generateCheckNo());
        check.setStatus(StockCheckStatus.DRAFT.getCode());
        check.setCreateTime(LocalDateTime.now());
        check.setCreateBy(tenantId);
        this.save(check);
        return check;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck createCheckWithItems(Long warehouseId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        
        StockCheck check = new StockCheck();
        check.setTenantId(tenantId);
        check.setWarehouseId(warehouseId);
        check.setCheckNo(generateCheckNo());
        check.setCheckType(1);
        check.setStatus(StockCheckStatus.DRAFT.getCode());
        check.setCheckDate(LocalDateTime.now());
        check.setCreateTime(LocalDateTime.now());
        check.setCreateBy(tenantId);
        this.save(check);

        List<Stock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getWarehouseId, warehouseId)
                        .eq(Stock::getTenantId, tenantId)
        );

        for (Stock stock : stocks) {
            StockCheckItem item = new StockCheckItem();
            item.setTenantId(tenantId);
            item.setCheckId(check.getId());
            item.setProductId(stock.getProductId());
            item.setProductName(stock.getProductName());
            item.setProductCode(stock.getProductCode());
            item.setBookQuantity(stock.getQuantity());
            item.setActualQuantity(BigDecimal.ZERO);
            item.setDiffQuantity(BigDecimal.ZERO);
            item.setStatus(0);
            item.setCreateTime(LocalDateTime.now());
            stockCheckItemMapper.insert(item);
        }

        check.setTotalItems(stocks.size());
        check.setCheckedItems(0);
        check.setDiffItems(0);
        this.updateById(check);

        return check;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck updateCheck(Long checkId, StockCheck check) {
        StockCheck existing = this.getById(checkId);
        if (existing == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (existing.getStatus() != StockCheckStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的盘点单可以修改");
        }
        
        existing.setWarehouseId(check.getWarehouseId());
        existing.setWarehouseName(check.getWarehouseName());
        existing.setCheckType(check.getCheckType());
        existing.setRemark(check.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        this.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck submitForApproval(Long checkId) {
        StockCheck check = this.getById(checkId);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (check.getStatus() != StockCheckStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的盘点单可以提交审批");
        }
        
        check.setStatus(StockCheckStatus.PENDING_APPROVAL.getCode());
        check.setUpdateTime(LocalDateTime.now());
        this.updateById(check);
        return check;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck approve(Long checkId, Long approverId, String note) {
        StockCheck check = this.getById(checkId);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (check.getStatus() != StockCheckStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的盘点单可以审批");
        }
        
        check.setStatus(StockCheckStatus.APPROVED.getCode());
        check.setApprovedBy(approverId);
        check.setApprovedTime(LocalDateTime.now());
        check.setApprovedNote(note);
        check.setUpdateTime(LocalDateTime.now());
        this.updateById(check);
        return check;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck reject(Long checkId, String reason) {
        StockCheck check = this.getById(checkId);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (check.getStatus() != StockCheckStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的盘点单可以拒绝");
        }
        
        check.setStatus(StockCheckStatus.REJECTED.getCode());
        check.setApprovedNote(reason);
        check.setUpdateTime(LocalDateTime.now());
        this.updateById(check);
        return check;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck startCheck(Long checkId) {
        StockCheck check = this.getById(checkId);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (check.getStatus() != StockCheckStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的盘点单可以开始盘点");
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        check.setStatus(StockCheckStatus.IN_PROGRESS.getCode());
        check.setCheckerId(userId);
        check.setUpdateTime(LocalDateTime.now());
        this.updateById(check);
        return check;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheckItem checkItem(Long itemId, BigDecimal actualQuantity, String note) {
        StockCheckItem item = stockCheckItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("盘点明细不存在");
        }
        
        StockCheck check = this.getById(item.getCheckId());
        if (check.getStatus() != StockCheckStatus.IN_PROGRESS.getCode()) {
            throw new RuntimeException("盘点单不在进行中状态");
        }
        
        item.setActualQuantity(actualQuantity);
        item.setDiffQuantity(actualQuantity.subtract(item.getBookQuantity()));
        item.setNote(note);
        item.setStatus(1);
        item.setUpdateTime(LocalDateTime.now());
        stockCheckItemMapper.updateById(item);

        calculateTotals(item.getCheckId());
        
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck completeCheck(Long checkId) {
        StockCheck check = this.getById(checkId);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (check.getStatus() != StockCheckStatus.IN_PROGRESS.getCode()) {
            throw new RuntimeException("只有进行中状态的盘点单可以完成");
        }
        
        calculateTotals(checkId);
        
        check.setStatus(StockCheckStatus.COMPLETED.getCode());
        check.setUpdateTime(LocalDateTime.now());
        this.updateById(check);
        return check;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck adjust(Long checkId) {
        StockCheck check = this.getById(checkId);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (check.getStatus() != StockCheckStatus.COMPLETED.getCode()) {
            throw new RuntimeException("只有已完成状态的盘点单可以调整库存");
        }
        
        List<StockCheckItem> diffItems = getDiffItems(checkId);
        Long userId = StpUtil.getLoginIdAsLong();
        
        for (StockCheckItem item : diffItems) {
            Stock stock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, check.getWarehouseId())
                            .eq(Stock::getProductId, item.getProductId())
            );
            
            if (stock != null) {
                BigDecimal newQuantity = stock.getQuantity().add(item.getDiffQuantity());
                stock.setQuantity(newQuantity);
                stock.setUpdateTime(LocalDateTime.now());
                stock.setUpdateBy(userId);
                stockMapper.updateById(stock);
            }
            
            item.setStatus(2);
            item.setUpdateTime(LocalDateTime.now());
            stockCheckItemMapper.updateById(item);
        }
        
        check.setStatus(StockCheckStatus.ADJUSTED.getCode());
        check.setAdjustedBy(userId);
        check.setAdjustedTime(LocalDateTime.now());
        check.setUpdateTime(LocalDateTime.now());
        this.updateById(check);
        
        return check;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCheck cancel(Long checkId, String reason) {
        StockCheck check = this.getById(checkId);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (check.getStatus() == StockCheckStatus.ADJUSTED.getCode()) {
            throw new RuntimeException("已调整库存的盘点单不能取消");
        }
        
        check.setStatus(StockCheckStatus.CANCELLED.getCode());
        check.setRemark(reason);
        check.setUpdateTime(LocalDateTime.now());
        this.updateById(check);
        return check;
    }

    @Override
    public List<StockCheckItem> getItems(Long checkId) {
        return stockCheckItemMapper.selectList(
                new LambdaQueryWrapper<StockCheckItem>()
                        .eq(StockCheckItem::getCheckId, checkId)
                        .orderByAsc(StockCheckItem::getId)
        );
    }

    @Override
    public List<StockCheckItem> getDiffItems(Long checkId) {
        return stockCheckItemMapper.selectList(
                new LambdaQueryWrapper<StockCheckItem>()
                        .eq(StockCheckItem::getCheckId, checkId)
                        .ne(StockCheckItem::getDiffQuantity, BigDecimal.ZERO)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long checkId) {
        List<StockCheckItem> items = getItems(checkId);
        
        int totalItems = items.size();
        int checkedItems = (int) items.stream().filter(i -> i.getStatus() == 1).count();
        int diffItems = (int) items.stream().filter(i -> i.getDiffQuantity() != null && !i.getDiffQuantity().equals(BigDecimal.ZERO)).count();
        
        BigDecimal totalBookQuantity = items.stream()
                .map(StockCheckItem::getBookQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalActualQuantity = items.stream()
                .filter(i -> i.getActualQuantity() != null)
                .map(StockCheckItem::getActualQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDiffQuantity = totalActualQuantity.subtract(totalBookQuantity);
        
        StockCheck check = this.getById(checkId);
        check.setTotalItems(totalItems);
        check.setCheckedItems(checkedItems);
        check.setDiffItems(diffItems);
        check.setTotalBookQuantity(totalBookQuantity);
        check.setTotalActualQuantity(totalActualQuantity);
        check.setTotalDiffQuantity(totalDiffQuantity);
        check.setUpdateTime(LocalDateTime.now());
        this.updateById(check);
    }

    @Override
    public Integer getDiffItemCount(Long checkId) {
        return stockCheckItemMapper.selectCount(
                new LambdaQueryWrapper<StockCheckItem>()
                        .eq(StockCheckItem::getCheckId, checkId)
                        .ne(StockCheckItem::getDiffQuantity, BigDecimal.ZERO)
        ).intValue();
    }
}