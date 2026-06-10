package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockAssemble;
import cn.aiedge.erp.stock.entity.StockAssembleItem;
import cn.aiedge.erp.stock.mapper.StockAssembleItemMapper;
import cn.aiedge.erp.stock.mapper.StockAssembleMapper;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.StockAssembleService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockAssembleServiceImpl extends ServiceImpl<StockAssembleMapper, StockAssemble> implements StockAssembleService {

    @Autowired
    private StockAssembleItemMapper stockAssembleItemMapper;

    @Autowired
    private StockMapper stockMapper;

    private String generateAssembleNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "AS" + dateStr + randomStr;
    }

    @Override
    public Page<StockAssemble> pageList(String keyword, Long warehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockAssemble> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, StockAssemble::getAssembleNo, keyword)
                .or(w -> w.like(keyword != null, StockAssemble::getProductName, keyword))
                .eq(warehouseId != null, StockAssemble::getWarehouseId, warehouseId)
                .eq(status != null, StockAssemble::getStatus, status)
                .orderByDesc(StockAssemble::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAssemble createAssemble(StockAssemble assemble, List<StockAssembleItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        assemble.setTenantId(userId);
        assemble.setAssembleNo(generateAssembleNo());
        assemble.setStatus(0);
        assemble.setApplicantId(userId);
        assemble.setApplyTime(LocalDateTime.now());
        assemble.setCreateBy(userId);
        assemble.setCreateTime(LocalDateTime.now());
        this.save(assemble);

        if (items != null && !items.isEmpty()) {
            BigDecimal subTotalCost = BigDecimal.ZERO;
            for (StockAssembleItem item : items) {
                item.setAssembleId(assemble.getId());
                if (item.getQuantity() == null) {
                    item.setQuantity(BigDecimal.ONE);
                }
                if (item.getUnitCost() == null) {
                    item.setUnitCost(BigDecimal.ZERO);
                }
                item.setCost(item.getQuantity().multiply(item.getUnitCost()));
                item.setCreateTime(LocalDateTime.now());
                stockAssembleItemMapper.insert(item);
                subTotalCost = subTotalCost.add(item.getCost());
            }
            assemble.setSubTotalCost(subTotalCost);
            assemble.setTotalItems(items.size());
            if (assemble.getAssembleFee() == null) {
                assemble.setAssembleFee(BigDecimal.ZERO);
            }
            assemble.setTotalCost(subTotalCost.add(assemble.getAssembleFee()));
            this.updateById(assemble);
        }

        return assemble;
    }

    @Override
    public StockAssemble getItems(Long assembleId) {
        StockAssemble assemble = this.getById(assembleId);
        if (assemble == null) {
            throw BusinessException.notFound("组装单不存在");
        }
        return assemble;
    }

    @Override
    public List<StockAssembleItem> getItemList(Long assembleId) {
        return stockAssembleItemMapper.selectList(
                new LambdaQueryWrapper<StockAssembleItem>()
                        .eq(StockAssembleItem::getAssembleId, assembleId)
                        .orderByAsc(StockAssembleItem::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAssemble submitForApproval(Long id) {
        StockAssemble assemble = this.getById(id);
        if (assemble == null) {
            throw BusinessException.notFound("组装单不存在");
        }
        if (assemble.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的组装单可以提交审批");
        }
        assemble.setStatus(1);
        assemble.setUpdateTime(LocalDateTime.now());
        this.updateById(assemble);
        return assemble;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAssemble approve(Long id, Long approverId, String note) {
        StockAssemble assemble = this.getById(id);
        if (assemble == null) {
            throw BusinessException.notFound("组装单不存在");
        }
        if (assemble.getStatus() != 1) {
            throw BusinessException.badRequest("只有待审批状态的组装单可以审批通过");
        }
        assemble.setStatus(2);
        assemble.setApprovedBy(approverId);
        assemble.setApprovedTime(LocalDateTime.now());
        assemble.setApprovedNote(note);
        assemble.setUpdateTime(LocalDateTime.now());
        this.updateById(assemble);
        return assemble;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAssemble reject(Long id, String reason) {
        StockAssemble assemble = this.getById(id);
        if (assemble == null) {
            throw BusinessException.notFound("组装单不存在");
        }
        if (assemble.getStatus() != 1) {
            throw BusinessException.badRequest("只有待审批状态的组装单可以拒绝");
        }
        assemble.setStatus(4);
        assemble.setApprovedNote(reason);
        assemble.setUpdateTime(LocalDateTime.now());
        this.updateById(assemble);
        return assemble;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAssemble execute(Long id) {
        StockAssemble assemble = this.getById(id);
        if (assemble == null) {
            throw BusinessException.notFound("组装单不存在");
        }
        if (assemble.getStatus() != 2) {
            throw BusinessException.badRequest("只有已审核状态的组装单可以执行");
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // Decrease stock for each sub-item
        List<StockAssembleItem> items = getItemList(id);
        for (StockAssembleItem item : items) {
            Stock stock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, assemble.getWarehouseId())
                            .eq(Stock::getProductId, item.getProductId())
            );
            if (stock == null) {
                throw BusinessException.badRequest("子件产品「" + item.getProductName() + "」库存不足，无法执行组装");
            }
            BigDecimal neededQuantity = item.getQuantity().multiply(assemble.getAssembleQuantity());
            if (stock.getQuantity().compareTo(neededQuantity) < 0) {
                throw BusinessException.badRequest("子件产品「" + item.getProductName() + "」库存不足，需要" +
                        neededQuantity + "，当前库存" + stock.getQuantity());
            }
            stock.setQuantity(stock.getQuantity().subtract(neededQuantity));
            stock.setUpdateTime(LocalDateTime.now());
            stock.setUpdateBy(userId);
            stockMapper.updateById(stock);
        }

        // Increase stock for the assembled product
        Stock productStock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getWarehouseId, assemble.getWarehouseId())
                        .eq(Stock::getProductId, assemble.getProductId())
        );
        BigDecimal outputQty = assemble.getOutputQuantity() != null
                ? assemble.getOutputQuantity()
                : assemble.getAssembleQuantity();
        if (productStock == null) {
            productStock = new Stock();
            productStock.setTenantId(assemble.getTenantId());
            productStock.setWarehouseId(assemble.getWarehouseId());
            productStock.setWarehouseName(assemble.getWarehouseName());
            productStock.setProductId(assemble.getProductId());
            productStock.setProductCode(assemble.getProductCode());
            productStock.setProductName(assemble.getProductName());
            productStock.setQuantity(outputQty);
            productStock.setAvailableQuantity(outputQty);
            productStock.setFrozenQuantity(BigDecimal.ZERO);
            productStock.setCreateTime(LocalDateTime.now());
            productStock.setCreateBy(userId);
            stockMapper.insert(productStock);
        } else {
            productStock.setQuantity(productStock.getQuantity().add(outputQty));
            productStock.setAvailableQuantity(productStock.getAvailableQuantity().add(outputQty));
            productStock.setUpdateTime(LocalDateTime.now());
            productStock.setUpdateBy(userId);
            stockMapper.updateById(productStock);
        }

        assemble.setStatus(3);
        assemble.setExecutedBy(userId);
        assemble.setExecutedTime(LocalDateTime.now());
        assemble.setUpdateTime(LocalDateTime.now());
        this.updateById(assemble);

        return assemble;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAssemble cancel(Long id, String reason) {
        StockAssemble assemble = this.getById(id);
        if (assemble == null) {
            throw BusinessException.notFound("组装单不存在");
        }
        if (assemble.getStatus() == 3) {
            throw BusinessException.badRequest("已执行的组装单不能取消");
        }
        assemble.setStatus(5);
        assemble.setRemark(reason);
        assemble.setUpdateTime(LocalDateTime.now());
        this.updateById(assemble);
        return assemble;
    }
}
