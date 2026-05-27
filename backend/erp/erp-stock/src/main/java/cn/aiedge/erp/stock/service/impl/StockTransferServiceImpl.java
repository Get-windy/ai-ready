package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockTransfer;
import cn.aiedge.erp.stock.entity.StockTransferItem;
import cn.aiedge.erp.stock.enums.StockTransferStatus;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.mapper.StockTransferItemMapper;
import cn.aiedge.erp.stock.mapper.StockTransferMapper;
import cn.aiedge.erp.stock.service.StockTransferService;
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
public class StockTransferServiceImpl extends ServiceImpl<StockTransferMapper, StockTransfer> implements StockTransferService {

    @Autowired
    private StockTransferItemMapper stockTransferItemMapper;

    @Autowired
    private StockMapper stockMapper;

    @Override
    public StockTransfer getByTransferNo(String transferNo) {
        return this.lambdaQuery()
                .eq(StockTransfer::getTransferNo, transferNo)
                .one();
    }

    @Override
    public Page<StockTransfer> pageList(String keyword, Long fromWarehouseId, Long toWarehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, StockTransfer::getTransferNo, keyword)
                .eq(fromWarehouseId != null, StockTransfer::getFromWarehouseId, fromWarehouseId)
                .eq(toWarehouseId != null, StockTransfer::getToWarehouseId, toWarehouseId)
                .eq(status != null, StockTransfer::getStatus, status)
                .orderByDesc(StockTransfer::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<StockTransfer> listByFromWarehouseId(Long warehouseId) {
        return this.lambdaQuery()
                .eq(StockTransfer::getFromWarehouseId, warehouseId)
                .orderByDesc(StockTransfer::getCreateTime)
                .list();
    }

    @Override
    public List<StockTransfer> listByToWarehouseId(Long warehouseId) {
        return this.lambdaQuery()
                .eq(StockTransfer::getToWarehouseId, warehouseId)
                .orderByDesc(StockTransfer::getCreateTime)
                .list();
    }

    @Override
    public String generateTransferNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "ST" + dateStr + randomStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer createTransfer(StockTransfer transfer, List<StockTransferItem> items) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        
        transfer.setTenantId(tenantId);
        transfer.setTransferNo(generateTransferNo());
        transfer.setStatus(StockTransferStatus.DRAFT.getCode());
        transfer.setCreateTime(LocalDateTime.now());
        transfer.setCreateBy(tenantId);
        this.save(transfer);

        if (items != null && !items.isEmpty()) {
            for (StockTransferItem item : items) {
                item.setTenantId(tenantId);
                item.setTransferId(transfer.getId());
                item.setStatus(0);
                item.setCreateTime(LocalDateTime.now());
                stockTransferItemMapper.insert(item);
            }
        }

        calculateTotals(transfer.getId());
        
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer submitForApproval(Long transferId) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的调拨单可以提交审批");
        }
        
        transfer.setStatus(StockTransferStatus.PENDING_APPROVAL.getCode());
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer approve(Long transferId, Long approverId, String note) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的调拨单可以审批");
        }
        
        transfer.setStatus(StockTransferStatus.APPROVED.getCode());
        transfer.setApprovedBy(approverId);
        transfer.setApprovedTime(LocalDateTime.now());
        transfer.setApprovedNote(note);
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer reject(Long transferId, String reason) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的调拨单可以拒绝");
        }
        
        transfer.setStatus(StockTransferStatus.REJECTED.getCode());
        transfer.setApprovedNote(reason);
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer execute(Long transferId) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的调拨单可以执行");
        }
        
        List<StockTransferItem> items = getItems(transferId);
        Long userId = StpUtil.getLoginIdAsLong();
        
        for (StockTransferItem item : items) {
            Stock fromStock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, transfer.getFromWarehouseId())
                            .eq(Stock::getProductId, item.getProductId())
            );
            
            if (fromStock == null || fromStock.getQuantity().compareTo(item.getQuantity()) < 0) {
                throw new RuntimeException("源仓库库存不足: " + item.getProductName());
            }
            
            fromStock.setQuantity(fromStock.getQuantity().subtract(item.getQuantity()));
            fromStock.setUpdateTime(LocalDateTime.now());
            fromStock.setUpdateBy(userId);
            stockMapper.updateById(fromStock);
            
            Stock toStock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, transfer.getToWarehouseId())
                            .eq(Stock::getProductId, item.getProductId())
            );
            
            if (toStock == null) {
                toStock = new Stock();
                toStock.setTenantId(transfer.getTenantId());
                toStock.setWarehouseId(transfer.getToWarehouseId());
                toStock.setProductId(item.getProductId());
                toStock.setProductName(item.getProductName());
                toStock.setProductCode(item.getProductCode());
                toStock.setQuantity(item.getQuantity());
                toStock.setCreateTime(LocalDateTime.now());
                toStock.setCreateBy(userId);
                stockMapper.insert(toStock);
            } else {
                toStock.setQuantity(toStock.getQuantity().add(item.getQuantity()));
                toStock.setUpdateTime(LocalDateTime.now());
                toStock.setUpdateBy(userId);
                stockMapper.updateById(toStock);
            }
            
            item.setStatus(1);
            item.setUpdateTime(LocalDateTime.now());
            stockTransferItemMapper.updateById(item);
        }
        
        transfer.setStatus(StockTransferStatus.COMPLETED.getCode());
        transfer.setExecuteBy(userId);
        transfer.setExecuteTime(LocalDateTime.now());
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
        
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer cancel(Long transferId, String reason) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (transfer.getStatus() == StockTransferStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的调拨单不能取消");
        }
        
        transfer.setStatus(StockTransferStatus.CANCELLED.getCode());
        transfer.setRemark(reason);
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
        return transfer;
    }

    @Override
    public List<StockTransferItem> getItems(Long transferId) {
        return stockTransferItemMapper.selectList(
                new LambdaQueryWrapper<StockTransferItem>()
                        .eq(StockTransferItem::getTransferId, transferId)
                        .orderByAsc(StockTransferItem::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransferItem addItem(Long transferId, StockTransferItem item) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的调拨单可以添加明细");
        }
        
        Long tenantId = StpUtil.getLoginIdAsLong();
        item.setTenantId(tenantId);
        item.setTransferId(transferId);
        item.setStatus(0);
        item.setCreateTime(LocalDateTime.now());
        stockTransferItemMapper.insert(item);
        
        calculateTotals(transferId);
        
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransferItem updateItem(Long itemId, StockTransferItem item) {
        StockTransferItem existing = stockTransferItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("调拨明细不存在");
        }
        
        StockTransfer transfer = this.getById(existing.getTransferId());
        if (transfer.getStatus() != StockTransferStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的调拨单可以修改明细");
        }
        
        existing.setProductId(item.getProductId());
        existing.setProductName(item.getProductName());
        existing.setProductCode(item.getProductCode());
        existing.setQuantity(item.getQuantity());
        existing.setUnitPrice(item.getUnitPrice());
        existing.setRemark(item.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        stockTransferItemMapper.updateById(existing);
        
        calculateTotals(existing.getTransferId());
        
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        StockTransferItem item = stockTransferItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("调拨明细不存在");
        }
        
        StockTransfer transfer = this.getById(item.getTransferId());
        if (transfer.getStatus() != StockTransferStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的调拨单可以删除明细");
        }
        
        stockTransferItemMapper.deleteById(itemId);
        
        calculateTotals(item.getTransferId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long transferId) {
        List<StockTransferItem> items = getItems(transferId);
        
        int totalItems = items.size();
        BigDecimal totalQuantity = items.stream()
                .map(StockTransferItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAmount = items.stream()
                .map(i -> i.getQuantity().multiply(i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        StockTransfer transfer = this.getById(transferId);
        transfer.setTotalItems(totalItems);
        transfer.setTotalQuantity(totalQuantity);
        transfer.setTotalAmount(totalAmount);
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
    }
}