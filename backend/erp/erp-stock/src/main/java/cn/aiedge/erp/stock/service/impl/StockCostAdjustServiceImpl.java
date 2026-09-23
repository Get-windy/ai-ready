package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockCostAdjustItemVO;
import cn.aiedge.erp.stock.dto.StockCostAdjustQuery;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockCostAdjust;
import cn.aiedge.erp.stock.entity.StockCostAdjustItem;
import cn.aiedge.erp.stock.mapper.StockCostAdjustItemMapper;
import cn.aiedge.erp.stock.mapper.StockCostAdjustMapper;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.StockCostAdjustService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockCostAdjustServiceImpl extends ServiceImpl<StockCostAdjustMapper, StockCostAdjust> implements StockCostAdjustService {

    private final StockCostAdjustItemMapper adjustItemMapper;
    private final StockMapper stockMapper;

    private static final String NO_PREFIX = "CBTJD-";

    @Override
    public String generateNo() {
        return NO_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /** 从查询条件构建单据级过滤 wrapper（按单据/按明细共用） */
    private LambdaQueryWrapper<StockCostAdjust> buildDocWrapper(StockCostAdjustQuery q) {
        LambdaQueryWrapper<StockCostAdjust> wrapper = new LambdaQueryWrapper<StockCostAdjust>()
                .like(StringUtils.hasText(q.getAdjustNo()), StockCostAdjust::getAdjustNo, q.getAdjustNo())
                .like(StringUtils.hasText(q.getKeyword()), StockCostAdjust::getAdjustNo, q.getKeyword())
                .like(StringUtils.hasText(q.getHandlerName()), StockCostAdjust::getHandlerName, q.getHandlerName())
                .like(StringUtils.hasText(q.getDeptName()), StockCostAdjust::getDeptName, q.getDeptName())
                .like(StringUtils.hasText(q.getCreatorName()), StockCostAdjust::getCreatorName, q.getCreatorName())
                .like(StringUtils.hasText(q.getBookkeeperName()), StockCostAdjust::getBookkeeperName, q.getBookkeeperName())
                .like(StringUtils.hasText(q.getRemark()), StockCostAdjust::getRemark, q.getRemark())
                .like(StringUtils.hasText(q.getWarehouseName()), StockCostAdjust::getWarehouseName, q.getWarehouseName())
                .eq(StringUtils.hasText(q.getReasonType()), StockCostAdjust::getReasonType, q.getReasonType())
                .eq(q.getWarehouseId() != null, StockCostAdjust::getWarehouseId, q.getWarehouseId())
                .eq(q.getStatus() != null, StockCostAdjust::getStatus, q.getStatus())
                .orderByDesc(StockCostAdjust::getAdjustDate)
                .orderByDesc(StockCostAdjust::getCreateTime);
        if (StringUtils.hasText(q.getDateStart())) {
            wrapper.ge(StockCostAdjust::getAdjustDate, LocalDate.parse(q.getDateStart()));
        }
        if (StringUtils.hasText(q.getDateEnd())) {
            wrapper.le(StockCostAdjust::getAdjustDate, LocalDate.parse(q.getDateEnd()));
        }
        return wrapper;
    }

    @Override
    public Page<StockCostAdjust> pageList(StockCostAdjustQuery query) {
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), buildDocWrapper(query));
    }

    @Override
    public Page<StockCostAdjustItemVO> pageDetail(StockCostAdjustQuery query) {
        // 1. 先取满足单据级过滤的单据ID集合
        LambdaQueryWrapper<StockCostAdjust> docWrapper = buildDocWrapper(query);
        docWrapper.select(StockCostAdjust::getId);
        List<StockCostAdjust> docs = this.list(docWrapper);
        List<Long> docIds = docs.stream().map(StockCostAdjust::getId).collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize(), 0);
        }

        // 2. 明细过滤
        LambdaQueryWrapper<StockCostAdjustItem> itemWrapper = new LambdaQueryWrapper<StockCostAdjustItem>()
                .in(StockCostAdjustItem::getAdjustId, docIds)
                .like(StringUtils.hasText(query.getProductName()), StockCostAdjustItem::getProductName, query.getProductName())
                .like(StringUtils.hasText(query.getItemRemark()), StockCostAdjustItem::getRemark, query.getItemRemark())
                .orderByDesc(StockCostAdjustItem::getCreateTime);
        Page<StockCostAdjustItem> itemPage = adjustItemMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), itemWrapper);

        // 3. 批量补齐单据级字段
        List<Long> pageDocIds = itemPage.getRecords().stream()
                .map(StockCostAdjustItem::getAdjustId).distinct().collect(Collectors.toList());
        Map<Long, StockCostAdjust> docMap = pageDocIds.isEmpty() ? Map.of() :
                this.listByIds(pageDocIds).stream().collect(Collectors.toMap(StockCostAdjust::getId, Function.identity()));

        List<StockCostAdjustItemVO> voList = new ArrayList<>();
        for (StockCostAdjustItem item : itemPage.getRecords()) {
            StockCostAdjustItemVO vo = new StockCostAdjustItemVO();
            BeanUtils.copyProperties(item, vo);
            StockCostAdjust doc = docMap.get(item.getAdjustId());
            if (doc != null) {
                vo.setAdjustDate(doc.getAdjustDate());
                vo.setAdjustNo(doc.getAdjustNo());
                vo.setStatus(doc.getStatus());
                vo.setWarehouseId(doc.getWarehouseId());
                vo.setWarehouseName(doc.getWarehouseName());
                vo.setHandlerName(doc.getHandlerName());
                vo.setDeptName(doc.getDeptName());
                vo.setDocRemark(doc.getRemark());
                vo.setSummary(doc.getSummary());
                vo.setAttachment(doc.getAttachment());
                vo.setBookkeeperName(doc.getBookkeeperName());
                vo.setCreatorName(doc.getCreatorName());
                vo.setBookkeepingTime(doc.getBookkeepingTime());
                vo.setCreateTime(doc.getCreateTime());
                vo.setPrintCount(doc.getPrintCount());
            }
            voList.add(vo);
        }
        Page<StockCostAdjustItemVO> voPage = new Page<>(query.getPageNum(), query.getPageSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public StockCostAdjust getDetail(Long id) {
        StockCostAdjust adjust = this.getById(id);
        if (adjust == null) throw BusinessException.notFound("成本调价单不存在");
        adjust.setItems(getItems(id));
        return adjust;
    }

    @Override
    public List<StockCostAdjustItem> getItems(Long adjustId) {
        return adjustItemMapper.selectList(
                new LambdaQueryWrapper<StockCostAdjustItem>().eq(StockCostAdjustItem::getAdjustId, adjustId)
                        .orderByAsc(StockCostAdjustItem::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust createAdjust(StockCostAdjust adjust, List<StockCostAdjustItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        adjust.setId(null);
        adjust.setAdjustNo(generateNo());
        adjust.setTenantId(1L);
        if (adjust.getStatus() == null) adjust.setStatus(0);
        adjust.setCreateBy(userId);
        if (adjust.getCreatorName() == null) adjust.setCreatorName(String.valueOf(userId));
        adjust.setCreateTime(LocalDateTime.now());
        fillTotals(adjust, items);
        this.save(adjust);
        saveItems(adjust.getId(), adjust.getWarehouseId(), items);
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust updateAdjust(Long id, StockCostAdjust adjust, List<StockCostAdjustItem> items) {
        StockCostAdjust exist = this.getById(id);
        if (exist == null) throw BusinessException.notFound("成本调价单不存在");
        if (exist.getStatus() != 0) throw BusinessException.badRequest("只有草稿状态的成本调价单可以修改");
        adjust.setId(id);
        adjust.setTenantId(exist.getTenantId());
        if (adjust.getAdjustNo() == null) adjust.setAdjustNo(exist.getAdjustNo());
        adjust.setStatus(0);
        adjust.setCreateBy(exist.getCreateBy());
        adjust.setCreateTime(exist.getCreateTime());
        adjust.setUpdateBy(StpUtil.getLoginIdAsLong());
        fillTotals(adjust, items);
        this.updateById(adjust);
        // 重建明细
        adjustItemMapper.delete(new LambdaQueryWrapper<StockCostAdjustItem>().eq(StockCostAdjustItem::getAdjustId, id));
        saveItems(id, adjust.getWarehouseId(), items);
        return adjust;
    }

    /** 计算调前/调后成本金额与调整金额，汇总合计金额 */
    private void fillTotals(StockCostAdjust adjust, List<StockCostAdjustItem> items) {
        BigDecimal totalAdjustAmount = BigDecimal.ZERO;
        if (items != null) {
            for (StockCostAdjustItem item : items) {
                BigDecimal qty = item.getCurrentQuantity() != null ? item.getCurrentQuantity() : BigDecimal.ZERO;
                BigDecimal oldCost = item.getOldCost() != null ? item.getOldCost() : BigDecimal.ZERO;
                BigDecimal newCost = item.getNewCost() != null ? item.getNewCost() : BigDecimal.ZERO;
                BigDecimal oldAmount = qty.multiply(oldCost);
                BigDecimal newAmount = qty.multiply(newCost);
                BigDecimal diff = newAmount.subtract(oldAmount);
                item.setOldAmount(oldAmount);
                item.setNewAmount(newAmount);
                item.setDiffAmount(diff);
                totalAdjustAmount = totalAdjustAmount.add(diff);
            }
        }
        adjust.setTotalAdjustAmount(totalAdjustAmount);
        adjust.setTotalItems(items != null ? items.size() : 0);
    }

    private void saveItems(Long adjustId, Long warehouseId, List<StockCostAdjustItem> items) {
        if (items == null) return;
        for (StockCostAdjustItem item : items) {
            item.setId(null);
            item.setAdjustId(adjustId);
            item.setTenantId(1L);
            item.setWarehouseId(warehouseId);
            item.setCreateTime(LocalDateTime.now());
            adjustItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust submitForApproval(Long id) {
        StockCostAdjust adjust = getAndCheck(id, 0, "只有草稿状态的成本调价单可以提交审批");
        adjust.setStatus(1);
        adjust.setApplicantId(StpUtil.getLoginIdAsLong());
        adjust.setApplicantName(StpUtil.getLoginId().toString());
        adjust.setApplyTime(LocalDateTime.now());
        this.updateById(adjust);
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust approve(Long id, Long approverId, String note) {
        StockCostAdjust adjust = getAndCheck(id, 1, "只有待审批状态的成本调价单可以审批");
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
        StockCostAdjust adjust = getAndCheck(id, 1, "只有待审批状态的成本调价单可以拒绝");
        adjust.setStatus(4);
        adjust.setApprovedNote(reason);
        this.updateById(adjust);
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust execute(Long id) {
        // 记账执行：仅调整成本单价不动库存数量
        StockCostAdjust adjust = getAndCheck(id, 2, "只有已审核状态的成本调价单可以记账执行");
        List<StockCostAdjustItem> items = getItems(id);
        BigDecimal totalAdjustAmount = BigDecimal.ZERO;
        for (StockCostAdjustItem item : items) {
            BigDecimal qty = item.getCurrentQuantity() != null ? item.getCurrentQuantity() : BigDecimal.ZERO;
            BigDecimal oldCost = item.getOldCost() != null ? item.getOldCost() : BigDecimal.ZERO;
            BigDecimal newCost = item.getNewCost() != null ? item.getNewCost() : BigDecimal.ZERO;
            BigDecimal oldAmount = qty.multiply(oldCost);
            BigDecimal newAmount = qty.multiply(newCost);
            BigDecimal diff = newAmount.subtract(oldAmount);
            item.setOldAmount(oldAmount);
            item.setNewAmount(newAmount);
            item.setDiffAmount(diff);
            totalAdjustAmount = totalAdjustAmount.add(diff);
            adjustItemMapper.updateById(item);
            // 记账回写：把调后成本价落到 ERP 轨库存成本（出库成本与毛利的取数口径）。
            // 本单只改成本、不动数量，故不经 InventoryService（那是数量变动的唯一写入口）。
            applyCostToStock(item, newCost, adjust.getWarehouseId());
        }
        Long userId = StpUtil.getLoginIdAsLong();
        adjust.setStatus(3);
        adjust.setExecutedBy(userId);
        adjust.setExecutedTime(LocalDateTime.now());
        adjust.setBookkeeperId(userId);
        adjust.setBookkeeperName(StpUtil.getLoginId().toString());
        adjust.setBookkeepingTime(LocalDateTime.now());
        adjust.setTotalAdjustAmount(totalAdjustAmount);
        this.updateById(adjust);
        return adjust;
    }

    /**
     * 把调后成本价回写到 ERP 轨库存（{@code erp_stock.unit_price}）。
     *
     * <p>修复点：改动前 {@code execute} 只更新单据与明细的状态与金额，库存成本单价原封不动，
     * 导致调价后出库成本、毛利仍按旧价计算。</p>
     *
     * <p>定位口径为「商品 × 仓库」—— {@code erp_stock} 是仓库级汇总账，不含库位/批次维度。
     * WMS 库位级成本 {@code wms_inventory.unit_cost} 不在本处同步，属跨轨口径待收敛项。</p>
     */
    private void applyCostToStock(StockCostAdjustItem item, BigDecimal newCost, Long fallbackWarehouseId) {
        if (item.getProductId() == null || newCost == null) {
            return;
        }
        Long warehouseId = item.getWarehouseId() != null ? item.getWarehouseId() : fallbackWarehouseId;
        if (warehouseId == null) {
            return;
        }
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getProductId, item.getProductId())
                .eq(Stock::getWarehouseId, warehouseId)
                .last("LIMIT 1"));
        if (stock == null) {
            // 该仓库尚无库存行：成本无从落地，保持原值（不凭空建行）
            return;
        }
        stock.setUnitPrice(newCost);
        stockMapper.updateById(stock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockCostAdjust cancel(Long id, String reason) {
        StockCostAdjust adjust = this.getById(id);
        if (adjust == null) throw BusinessException.notFound("成本调价单不存在");
        if (adjust.getStatus() >= 3) throw BusinessException.badRequest("已记账执行的成本调价单不能取消");
        if (adjust.getStatus() == 5) throw BusinessException.badRequest("成本调价单已取消");
        adjust.setStatus(5);
        adjust.setCancelReason(reason);
        this.updateById(adjust);
        return adjust;
    }

    private StockCostAdjust getAndCheck(Long id, int expectedStatus, String msg) {
        StockCostAdjust adjust = this.getById(id);
        if (adjust == null) throw BusinessException.notFound("成本调价单不存在");
        if (adjust.getStatus() != expectedStatus) throw BusinessException.badRequest(msg);
        return adjust;
    }
}
