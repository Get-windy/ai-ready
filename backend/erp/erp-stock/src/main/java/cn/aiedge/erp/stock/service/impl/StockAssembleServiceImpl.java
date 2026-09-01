package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockAssembleQuery;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockAssemble;
import cn.aiedge.erp.stock.entity.StockAssembleItem;
import cn.aiedge.erp.stock.mapper.StockAssembleItemMapper;
import cn.aiedge.erp.stock.mapper.StockAssembleMapper;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.StockAssembleService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockAssembleServiceImpl extends ServiceImpl<StockAssembleMapper, StockAssemble> implements StockAssembleService {

    @Autowired
    private StockAssembleItemMapper stockAssembleItemMapper;

    @Autowired
    private StockMapper stockMapper;

    private String generateAssembleNo() {
        return generateNo();
    }

    @Override
    public String generateNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "ZZD-" + dateStr + randomStr;
    }

    @Override
    public Page<StockAssemble> pageList(StockAssembleQuery query) {
        if (query == null) query = new StockAssembleQuery();
        LambdaQueryWrapper<StockAssemble> wrapper = new LambdaQueryWrapper<>();
        String keyword = StrUtil.trimToNull(query.getKeyword());
        if (keyword != null) {
            wrapper.and(w -> w.like(StockAssemble::getAssembleNo, keyword)
                    .or().like(StockAssemble::getProductName, keyword)
                    .or().like(StockAssemble::getHandlerName, keyword));
        }
        wrapper.like(StrUtil.isNotBlank(query.getAssembleNo()), StockAssemble::getAssembleNo, StrUtil.trimToNull(query.getAssembleNo()))
                .like(StrUtil.isNotBlank(query.getProduceUnit()), StockAssemble::getProduceUnit, StrUtil.trimToNull(query.getProduceUnit()))
                .like(StrUtil.isNotBlank(query.getHandlerName()), StockAssemble::getHandlerName, StrUtil.trimToNull(query.getHandlerName()))
                .like(StrUtil.isNotBlank(query.getDeptName()), StockAssemble::getDeptName, StrUtil.trimToNull(query.getDeptName()))
                .like(StrUtil.isNotBlank(query.getCreatorName()), StockAssemble::getCreatorName, StrUtil.trimToNull(query.getCreatorName()))
                .like(StrUtil.isNotBlank(query.getBookkeeperName()), StockAssemble::getBookkeeperName, StrUtil.trimToNull(query.getBookkeeperName()))
                .like(StrUtil.isNotBlank(query.getRemark()), StockAssemble::getRemark, StrUtil.trimToNull(query.getRemark()))
                .eq(query.getInWarehouseId() != null, StockAssemble::getInWarehouseId, query.getInWarehouseId())
                .eq(query.getOutWarehouseId() != null, StockAssemble::getOutWarehouseId, query.getOutWarehouseId())
                .eq(query.getStatus() != null, StockAssemble::getStatus, query.getStatus());
        if (StrUtil.isNotBlank(query.getDateStart())) {
            wrapper.ge(StockAssemble::getAssembleDate, LocalDate.parse(query.getDateStart()).atStartOfDay());
        }
        if (StrUtil.isNotBlank(query.getDateEnd())) {
            wrapper.le(StockAssemble::getAssembleDate, LocalDate.parse(query.getDateEnd()).atTime(LocalTime.MAX));
        }
        wrapper.orderByDesc(StockAssemble::getCreateTime);
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAssemble createAssemble(StockAssemble assemble, List<StockAssembleItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        assemble.setTenantId(1L);
        assemble.setAssembleNo(generateAssembleNo());
        assemble.setStatus(0);
        assemble.setApplicantId(userId);
        String loginName = getLoginName();
        assemble.setApplicantName(loginName);
        if (StrUtil.isBlank(assemble.getCreatorName())) {
            assemble.setCreatorName(loginName);
        }
        assemble.setApplyTime(LocalDateTime.now());
        assemble.setCreateBy(userId);
        assemble.setCreateTime(LocalDateTime.now());
        if (assemble.getAssembleDate() == null) {
            assemble.setAssembleDate(LocalDateTime.now());
        }
        if (assemble.getInWarehouseId() == null) {
            assemble.setInWarehouseId(assemble.getWarehouseId());
        }
        if (assemble.getOutWarehouseId() == null) {
            assemble.setOutWarehouseId(assemble.getWarehouseId());
        }
        this.save(assemble);

        BigDecimal subTotalCost = saveItems(assemble.getId(), items);
        BigDecimal fee = assemble.getAssembleFee() == null ? BigDecimal.ZERO : assemble.getAssembleFee();
        assemble.setSubTotalCost(subTotalCost);
        assemble.setTotalCost(subTotalCost.add(fee));
        assemble.setTotalItems(items == null ? 0 : items.size());
        this.updateById(assemble);

        return assemble;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAssemble updateAssemble(Long id, StockAssemble assemble, List<StockAssembleItem> items) {
        StockAssemble existing = this.getById(id);
        if (existing == null) {
            throw BusinessException.notFound("组装单不存在");
        }
        if (existing.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的组装单可以修改");
        }
        assemble.setId(id);
        assemble.setTenantId(1L);
        assemble.setAssembleNo(existing.getAssembleNo());
        assemble.setStatus(0);
        assemble.setApplicantId(existing.getApplicantId());
        assemble.setApplicantName(existing.getApplicantName());
        assemble.setApplyTime(existing.getApplyTime());
        assemble.setCreateBy(existing.getCreateBy());
        assemble.setCreateTime(existing.getCreateTime());
        if (assemble.getAssembleDate() == null) {
            assemble.setAssembleDate(existing.getAssembleDate());
        }
        if (assemble.getInWarehouseId() == null) {
            assemble.setInWarehouseId(existing.getInWarehouseId() != null ? existing.getInWarehouseId() : existing.getWarehouseId());
        }
        if (assemble.getOutWarehouseId() == null) {
            assemble.setOutWarehouseId(existing.getOutWarehouseId() != null ? existing.getOutWarehouseId() : existing.getWarehouseId());
        }
        this.updateById(assemble);

        // 重建明细
        stockAssembleItemMapper.delete(
                new LambdaQueryWrapper<StockAssembleItem>().eq(StockAssembleItem::getAssembleId, id)
        );
        BigDecimal subTotalCost = saveItems(id, items);
        BigDecimal fee = assemble.getAssembleFee() == null ? BigDecimal.ZERO : assemble.getAssembleFee();
        assemble.setSubTotalCost(subTotalCost);
        assemble.setTotalCost(subTotalCost.add(fee));
        assemble.setTotalItems(items == null ? 0 : items.size());
        this.updateById(assemble);

        return assemble;
    }

    /**
     * 保存明细并计算子件总成本
     */
    private BigDecimal saveItems(Long assembleId, List<StockAssembleItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal subTotalCost = BigDecimal.ZERO;
        for (StockAssembleItem item : items) {
            item.setId(null);
            item.setAssembleId(assembleId);
            item.setTenantId(1L);
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
        return subTotalCost;
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

        // 原料出库仓库（兼容旧数据：无双仓库时回退到 warehouse_id）
        Long materialWarehouseId = assemble.getOutWarehouseId() != null
                ? assemble.getOutWarehouseId()
                : assemble.getWarehouseId();
        // 成品入库仓库
        Long productWarehouseId = assemble.getInWarehouseId() != null
                ? assemble.getInWarehouseId()
                : assemble.getWarehouseId();

        // Decrease stock for each sub-item from 原料仓库
        List<StockAssembleItem> items = getItemList(id);
        for (StockAssembleItem item : items) {
            Stock stock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, materialWarehouseId)
                            .eq(Stock::getProductId, item.getProductId())
            );
            if (stock == null) {
                throw BusinessException.badRequest("子件产品「" + item.getProductName() + "」库存不足，无法执行组装");
            }
            BigDecimal neededQuantity = item.getQuantity();
            if (stock.getQuantity().compareTo(neededQuantity) < 0) {
                throw BusinessException.badRequest("子件产品「" + item.getProductName() + "」库存不足，需要" +
                        neededQuantity + "，当前库存" + stock.getQuantity());
            }
            stock.setQuantity(stock.getQuantity().subtract(neededQuantity));
            stock.setAvailableQuantity(stock.getAvailableQuantity().subtract(neededQuantity));
            stock.setUpdateTime(LocalDateTime.now());
            stock.setUpdateBy(userId);
            stockMapper.updateById(stock);
        }

        // Increase stock for the assembled product in 成品仓库
        Stock productStock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getWarehouseId, productWarehouseId)
                        .eq(Stock::getProductId, assemble.getProductId())
        );
        BigDecimal outputQty = assemble.getOutputQuantity() != null
                ? assemble.getOutputQuantity()
                : assemble.getAssembleQuantity();
        if (outputQty == null) {
            outputQty = BigDecimal.ONE;
        }
        if (productStock == null) {
            productStock = new Stock();
            productStock.setTenantId(assemble.getTenantId());
            productStock.setWarehouseId(productWarehouseId);
            productStock.setWarehouseName(assemble.getInWarehouseName() != null
                    ? assemble.getInWarehouseName() : assemble.getWarehouseName());
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

        // 记账信息
        assemble.setStatus(3);
        assemble.setExecutedBy(userId);
        assemble.setExecutedTime(LocalDateTime.now());
        assemble.setBookkeeperId(userId);
        assemble.setBookkeeperName(getLoginName());
        assemble.setBookkeepingTime(LocalDateTime.now());
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
        assemble.setCancelReason(reason);
        assemble.setUpdateTime(LocalDateTime.now());
        this.updateById(assemble);
        return assemble;
    }
}
