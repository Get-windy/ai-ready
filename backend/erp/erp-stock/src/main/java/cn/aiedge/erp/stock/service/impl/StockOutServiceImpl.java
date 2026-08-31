package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockOutItemVO;
import cn.aiedge.erp.stock.dto.StockOutQuery;
import cn.aiedge.erp.stock.entity.StockOut;
import cn.aiedge.erp.stock.entity.StockOutItem;
import cn.aiedge.erp.stock.mapper.StockOutItemMapper;
import cn.aiedge.erp.stock.mapper.StockOutMapper;
import cn.aiedge.erp.stock.service.StockOutService;
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
public class StockOutServiceImpl extends ServiceImpl<StockOutMapper, StockOut> implements StockOutService {

    private final StockOutItemMapper stockOutItemMapper;

    private static final String NO_PREFIX = "QTCKD-";

    @Override
    public String generateNo() {
        return NO_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /** 从查询条件构建单据级过滤 wrapper（按单据/按明细共用） */
    private LambdaQueryWrapper<StockOut> buildDocWrapper(StockOutQuery q) {
        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<StockOut>()
                .like(StringUtils.hasText(q.getStockOutNo()), StockOut::getStockOutNo, q.getStockOutNo())
                .like(StringUtils.hasText(q.getPartnerName()), StockOut::getPartnerName, q.getPartnerName())
                .like(StringUtils.hasText(q.getKeyword()), StockOut::getPartnerName, q.getKeyword())
                .like(StringUtils.hasText(q.getHandlerName()), StockOut::getHandlerName, q.getHandlerName())
                .like(StringUtils.hasText(q.getDeptName()), StockOut::getDeptName, q.getDeptName())
                .like(StringUtils.hasText(q.getCreatorName()), StockOut::getCreatorName, q.getCreatorName())
                .like(StringUtils.hasText(q.getBookkeeperName()), StockOut::getBookkeeperName, q.getBookkeeperName())
                .like(StringUtils.hasText(q.getRemark()), StockOut::getRemark, q.getRemark())
                .like(StringUtils.hasText(q.getWarehouseName()), StockOut::getWarehouseName, q.getWarehouseName())
                .eq(q.getWarehouseId() != null, StockOut::getWarehouseId, q.getWarehouseId())
                .eq(q.getStatus() != null, StockOut::getStatus, q.getStatus())
                .orderByDesc(StockOut::getStockOutDate)
                .orderByDesc(StockOut::getCreateTime);
        if (StringUtils.hasText(q.getDateStart())) {
            wrapper.ge(StockOut::getStockOutDate, LocalDate.parse(q.getDateStart()));
        }
        if (StringUtils.hasText(q.getDateEnd())) {
            wrapper.le(StockOut::getStockOutDate, LocalDate.parse(q.getDateEnd()));
        }
        return wrapper;
    }

    @Override
    public Page<StockOut> pageList(StockOutQuery query) {
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), buildDocWrapper(query));
    }

    @Override
    public Page<StockOutItemVO> pageDetail(StockOutQuery query) {
        // 1. 先取满足单据级过滤的单据ID集合
        LambdaQueryWrapper<StockOut> docWrapper = buildDocWrapper(query);
        docWrapper.select(StockOut::getId);
        List<StockOut> docs = this.list(docWrapper);
        List<Long> docIds = docs.stream().map(StockOut::getId).collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize(), 0);
        }

        // 2. 明细过滤
        LambdaQueryWrapper<StockOutItem> itemWrapper = new LambdaQueryWrapper<StockOutItem>()
                .in(StockOutItem::getStockOutId, docIds)
                .like(StringUtils.hasText(query.getProductName()), StockOutItem::getProductName, query.getProductName())
                .like(StringUtils.hasText(query.getItemRemark()), StockOutItem::getRemark, query.getItemRemark())
                .orderByDesc(StockOutItem::getCreateTime);
        Page<StockOutItem> itemPage = stockOutItemMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), itemWrapper);

        // 3. 批量补齐单据级字段
        List<Long> pageDocIds = itemPage.getRecords().stream()
                .map(StockOutItem::getStockOutId).distinct().collect(Collectors.toList());
        Map<Long, StockOut> docMap = pageDocIds.isEmpty() ? Map.of() :
                this.listByIds(pageDocIds).stream().collect(Collectors.toMap(StockOut::getId, Function.identity()));

        List<StockOutItemVO> voList = new ArrayList<>();
        for (StockOutItem item : itemPage.getRecords()) {
            StockOutItemVO vo = new StockOutItemVO();
            BeanUtils.copyProperties(item, vo);
            StockOut doc = docMap.get(item.getStockOutId());
            if (doc != null) {
                vo.setStockOutDate(doc.getStockOutDate());
                vo.setStockOutNo(doc.getStockOutNo());
                vo.setStatus(doc.getStatus());
                vo.setWarehouseId(doc.getWarehouseId());
                vo.setWarehouseName(doc.getWarehouseName());
                vo.setPartnerCode(doc.getPartnerCode());
                vo.setPartnerName(doc.getPartnerName());
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
        Page<StockOutItemVO> voPage = new Page<>(query.getPageNum(), query.getPageSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public StockOut getDetail(Long id) {
        StockOut d = this.getById(id);
        if (d == null) throw BusinessException.notFound("出库单不存在");
        d.setItems(getItems(id));
        return d;
    }

    @Override
    public List<StockOutItem> getItems(Long stockOutId) {
        return stockOutItemMapper.selectList(
                new LambdaQueryWrapper<StockOutItem>().eq(StockOutItem::getStockOutId, stockOutId)
                        .orderByAsc(StockOutItem::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOut createStockOut(StockOut stockOut, List<StockOutItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        stockOut.setId(null);
        stockOut.setStockOutNo(generateNo());
        stockOut.setTenantId(1L);
        if (stockOut.getStatus() == null) stockOut.setStatus(0);
        stockOut.setCreateBy(userId);
        stockOut.setCreatorName(String.valueOf(userId));
        stockOut.setCreateTime(LocalDateTime.now());
        fillTotals(stockOut, items);
        this.save(stockOut);
        saveItems(stockOut.getId(), items);
        return stockOut;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOut updateStockOut(Long id, StockOut stockOut, List<StockOutItem> items) {
        StockOut exist = this.getById(id);
        if (exist == null) throw BusinessException.notFound("出库单不存在");
        if (exist.getStatus() != 0) throw BusinessException.badRequest("只有草稿状态的出库单可以修改");
        stockOut.setId(id);
        stockOut.setTenantId(exist.getTenantId());
        if (stockOut.getStockOutNo() == null) stockOut.setStockOutNo(exist.getStockOutNo());
        stockOut.setStatus(0);
        stockOut.setCreateBy(exist.getCreateBy());
        stockOut.setCreateTime(exist.getCreateTime());
        stockOut.setUpdateBy(StpUtil.getLoginIdAsLong());
        fillTotals(stockOut, items);
        this.updateById(stockOut);
        // 重建明细
        stockOutItemMapper.delete(new LambdaQueryWrapper<StockOutItem>().eq(StockOutItem::getStockOutId, id));
        saveItems(id, items);
        return stockOut;
    }

    private void fillTotals(StockOut stockOut, List<StockOutItem> items) {
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        if (items != null) {
            for (StockOutItem item : items) {
                if (item.getAmount() == null && item.getQuantity() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO));
                }
                totalQty = totalQty.add(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
                totalAmt = totalAmt.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                totalWeight = totalWeight.add(item.getWeight() != null ? item.getWeight() : BigDecimal.ZERO);
                totalVolume = totalVolume.add(item.getVolume() != null ? item.getVolume() : BigDecimal.ZERO);
            }
        }
        stockOut.setTotalQuantity(totalQty);
        stockOut.setTotalAmount(totalAmt);
        stockOut.setTotalWeight(totalWeight);
        stockOut.setTotalVolume(totalVolume);
        stockOut.setTotalItems(items != null ? items.size() : 0);
    }

    private void saveItems(Long stockOutId, List<StockOutItem> items) {
        if (items == null) return;
        for (StockOutItem item : items) {
            item.setId(null);
            item.setStockOutId(stockOutId);
            item.setCreateTime(LocalDateTime.now());
            stockOutItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOut submitForApproval(Long id) {
        StockOut d = getAndCheck(id, 0, "只有草稿状态的出库单可以提交审批");
        d.setStatus(1);
        d.setApplicantId(StpUtil.getLoginIdAsLong());
        d.setApplicantName(StpUtil.getLoginId().toString());
        d.setApplyTime(LocalDateTime.now());
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOut approve(Long id, Long approverId, String note) {
        StockOut d = getAndCheck(id, 1, "只有待审批状态的出库单可以审批");
        d.setStatus(2);
        d.setApprovedBy(approverId);
        d.setApprovedTime(LocalDateTime.now());
        d.setApprovedNote(note);
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOut reject(Long id, String reason) {
        StockOut d = getAndCheck(id, 1, "只有待审批状态的出库单可以拒绝");
        d.setStatus(4);
        d.setApprovedNote(reason);
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOut execute(Long id) {
        // 执行出库：记账，扣减库存
        StockOut d = getAndCheck(id, 2, "只有已审核状态的出库单可以出库");
        d.setStatus(3);
        d.setExecutedBy(StpUtil.getLoginIdAsLong());
        d.setExecutedTime(LocalDateTime.now());
        d.setBookkeeperId(StpUtil.getLoginIdAsLong());
        d.setBookkeeperName(StpUtil.getLoginId().toString());
        d.setBookkeepingTime(LocalDateTime.now());
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOut cancel(Long id, String reason) {
        StockOut d = this.getById(id);
        if (d == null) throw BusinessException.notFound("出库单不存在");
        if (d.getStatus() >= 3) throw BusinessException.badRequest("已出库的出库单不能取消");
        d.setStatus(5);
        d.setCancelReason(reason);
        this.updateById(d);
        return d;
    }

    private StockOut getAndCheck(Long id, int expectedStatus, String msg) {
        StockOut d = this.getById(id);
        if (d == null) throw BusinessException.notFound("出库单不存在");
        if (d.getStatus() != expectedStatus) throw BusinessException.badRequest(msg);
        return d;
    }
}
