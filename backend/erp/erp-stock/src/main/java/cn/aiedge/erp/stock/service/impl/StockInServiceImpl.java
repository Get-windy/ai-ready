package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.event.InventoryChangeEvent;
import cn.aiedge.common.exception.BusinessException;
import org.springframework.context.ApplicationEventPublisher;
import cn.aiedge.erp.stock.dto.StockInItemVO;
import cn.aiedge.erp.stock.dto.StockInQuery;
import cn.aiedge.erp.stock.entity.StockIn;
import cn.aiedge.erp.stock.entity.StockInItem;
import cn.aiedge.erp.stock.mapper.StockInItemMapper;
import cn.aiedge.erp.stock.mapper.StockInMapper;
import cn.aiedge.erp.stock.service.StockInService;
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
public class StockInServiceImpl extends ServiceImpl<StockInMapper, StockIn> implements StockInService {

    private final StockInItemMapper stockInItemMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String NO_PREFIX = "QTRKD-";

    @Override
    public String generateNo() {
        return NO_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /** 从查询条件构建单据级过滤 wrapper（按单据/按明细共用） */
    private LambdaQueryWrapper<StockIn> buildDocWrapper(StockInQuery q) {
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<StockIn>()
                .like(StringUtils.hasText(q.getStockInNo()), StockIn::getStockInNo, q.getStockInNo())
                .like(StringUtils.hasText(q.getPartnerName()), StockIn::getPartnerName, q.getPartnerName())
                .like(StringUtils.hasText(q.getKeyword()), StockIn::getPartnerName, q.getKeyword())
                .like(StringUtils.hasText(q.getHandlerName()), StockIn::getHandlerName, q.getHandlerName())
                .like(StringUtils.hasText(q.getDeptName()), StockIn::getDeptName, q.getDeptName())
                .like(StringUtils.hasText(q.getCreatorName()), StockIn::getCreatorName, q.getCreatorName())
                .like(StringUtils.hasText(q.getBookkeeperName()), StockIn::getBookkeeperName, q.getBookkeeperName())
                .like(StringUtils.hasText(q.getRemark()), StockIn::getRemark, q.getRemark())
                .like(StringUtils.hasText(q.getWarehouseName()), StockIn::getWarehouseName, q.getWarehouseName())
                .eq(q.getWarehouseId() != null, StockIn::getWarehouseId, q.getWarehouseId())
                .eq(q.getStatus() != null, StockIn::getStatus, q.getStatus())
                .orderByDesc(StockIn::getStockInDate)
                .orderByDesc(StockIn::getCreateTime);
        if (StringUtils.hasText(q.getDateStart())) {
            wrapper.ge(StockIn::getStockInDate, LocalDate.parse(q.getDateStart()));
        }
        if (StringUtils.hasText(q.getDateEnd())) {
            wrapper.le(StockIn::getStockInDate, LocalDate.parse(q.getDateEnd()));
        }
        return wrapper;
    }

    @Override
    public Page<StockIn> pageList(StockInQuery query) {
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), buildDocWrapper(query));
    }

    @Override
    public Page<StockInItemVO> pageDetail(StockInQuery query) {
        // 1. 先取满足单据级过滤的单据ID集合
        LambdaQueryWrapper<StockIn> docWrapper = buildDocWrapper(query);
        docWrapper.select(StockIn::getId);
        List<StockIn> docs = this.list(docWrapper);
        List<Long> docIds = docs.stream().map(StockIn::getId).collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize(), 0);
        }

        // 2. 明细过滤
        LambdaQueryWrapper<StockInItem> itemWrapper = new LambdaQueryWrapper<StockInItem>()
                .in(StockInItem::getStockInId, docIds)
                .like(StringUtils.hasText(query.getProductName()), StockInItem::getProductName, query.getProductName())
                .like(StringUtils.hasText(query.getItemRemark()), StockInItem::getRemark, query.getItemRemark())
                .orderByDesc(StockInItem::getCreateTime);
        Page<StockInItem> itemPage = stockInItemMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), itemWrapper);

        // 3. 批量补齐单据级字段
        List<Long> pageDocIds = itemPage.getRecords().stream()
                .map(StockInItem::getStockInId).distinct().collect(Collectors.toList());
        Map<Long, StockIn> docMap = pageDocIds.isEmpty() ? Map.of() :
                this.listByIds(pageDocIds).stream().collect(Collectors.toMap(StockIn::getId, Function.identity()));

        List<StockInItemVO> voList = new ArrayList<>();
        for (StockInItem item : itemPage.getRecords()) {
            StockInItemVO vo = new StockInItemVO();
            BeanUtils.copyProperties(item, vo);
            StockIn doc = docMap.get(item.getStockInId());
            if (doc != null) {
                vo.setStockInDate(doc.getStockInDate());
                vo.setStockInNo(doc.getStockInNo());
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
        Page<StockInItemVO> voPage = new Page<>(query.getPageNum(), query.getPageSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public StockIn getDetail(Long id) {
        StockIn d = this.getById(id);
        if (d == null) throw BusinessException.notFound("入库单不存在");
        d.setItems(getItems(id));
        return d;
    }

    @Override
    public List<StockInItem> getItems(Long stockInId) {
        return stockInItemMapper.selectList(
                new LambdaQueryWrapper<StockInItem>().eq(StockInItem::getStockInId, stockInId)
                        .orderByAsc(StockInItem::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockIn createStockIn(StockIn stockIn, List<StockInItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        stockIn.setId(null);
        stockIn.setStockInNo(generateNo());
        stockIn.setTenantId(1L);
        if (stockIn.getStatus() == null) stockIn.setStatus(0);
        stockIn.setCreateBy(userId);
        stockIn.setCreatorName(String.valueOf(userId));
        stockIn.setCreateTime(LocalDateTime.now());
        fillTotals(stockIn, items);
        this.save(stockIn);
        saveItems(stockIn.getId(), items);
        return stockIn;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockIn updateStockIn(Long id, StockIn stockIn, List<StockInItem> items) {
        StockIn exist = this.getById(id);
        if (exist == null) throw BusinessException.notFound("入库单不存在");
        if (exist.getStatus() != 0) throw BusinessException.badRequest("只有草稿状态的入库单可以修改");
        stockIn.setId(id);
        stockIn.setTenantId(exist.getTenantId());
        if (stockIn.getStockInNo() == null) stockIn.setStockInNo(exist.getStockInNo());
        stockIn.setStatus(0);
        stockIn.setCreateBy(exist.getCreateBy());
        stockIn.setCreateTime(exist.getCreateTime());
        stockIn.setUpdateBy(StpUtil.getLoginIdAsLong());
        fillTotals(stockIn, items);
        this.updateById(stockIn);
        // 重建明细
        stockInItemMapper.delete(new LambdaQueryWrapper<StockInItem>().eq(StockInItem::getStockInId, id));
        saveItems(id, items);
        return stockIn;
    }

    private void fillTotals(StockIn stockIn, List<StockInItem> items) {
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        if (items != null) {
            for (StockInItem item : items) {
                if (item.getAmount() == null && item.getQuantity() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO));
                }
                totalQty = totalQty.add(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
                totalAmt = totalAmt.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                totalWeight = totalWeight.add(item.getWeight() != null ? item.getWeight() : BigDecimal.ZERO);
                totalVolume = totalVolume.add(item.getVolume() != null ? item.getVolume() : BigDecimal.ZERO);
            }
        }
        stockIn.setTotalQuantity(totalQty);
        stockIn.setTotalAmount(totalAmt);
        stockIn.setTotalWeight(totalWeight);
        stockIn.setTotalVolume(totalVolume);
        stockIn.setTotalItems(items != null ? items.size() : 0);
    }

    private void saveItems(Long stockInId, List<StockInItem> items) {
        if (items == null) return;
        for (StockInItem item : items) {
            item.setId(null);
            item.setStockInId(stockInId);
            item.setCreateTime(LocalDateTime.now());
            stockInItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockIn submitForApproval(Long id) {
        StockIn d = getAndCheck(id, 0, "只有草稿状态的入库单可以提交审批");
        d.setStatus(1);
        d.setApplicantId(StpUtil.getLoginIdAsLong());
        d.setApplicantName(StpUtil.getLoginId().toString());
        d.setApplyTime(LocalDateTime.now());
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockIn approve(Long id, Long approverId, String note) {
        StockIn d = getAndCheck(id, 1, "只有待审批状态的入库单可以审批");
        d.setStatus(2);
        d.setApprovedBy(approverId);
        d.setApprovedTime(LocalDateTime.now());
        d.setApprovedNote(note);
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockIn reject(Long id, String reason) {
        StockIn d = getAndCheck(id, 1, "只有待审批状态的入库单可以拒绝");
        d.setStatus(4);
        d.setApprovedNote(reason);
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockIn execute(Long id) {
        // 执行入库：记账，增加库存
        StockIn d = getAndCheck(id, 2, "只有已审核状态的入库单可以入库");
        d.setStatus(3);
        d.setExecutedBy(StpUtil.getLoginIdAsLong());
        d.setExecutedTime(LocalDateTime.now());
        d.setBookkeeperId(StpUtil.getLoginIdAsLong());
        d.setBookkeeperName(StpUtil.getLoginId().toString());
        d.setBookkeepingTime(LocalDateTime.now());
        this.updateById(d);
        // TODO-P0 库存收敛：发布库存变动请求，由 WMS InventoryService 统一过账（唯一写入口，不再直写 erp_stock）
        List<StockInItem> items = stockInItemMapper.selectList(new LambdaQueryWrapper<StockInItem>().eq(StockInItem::getStockInId, id));
        if (items != null && d.getWarehouseId() != null) {
            for (StockInItem it : items) {
                if (it.getProductId() == null || it.getQuantity() == null || it.getQuantity().signum() <= 0) continue;
                applicationEventPublisher.publishEvent(new InventoryChangeEvent(
                        InventoryChangeEvent.ChangeType.INCREASE, it.getProductId(), d.getWarehouseId(), null,
                        it.getBatchCode(), it.getQuantity(), "STOCK_IN", id, d.getStockInNo(), d.getHandlerId(), d.getHandlerName()));
            }
        }
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockIn cancel(Long id, String reason) {
        StockIn d = this.getById(id);
        if (d == null) throw BusinessException.notFound("入库单不存在");
        if (d.getStatus() >= 3) throw BusinessException.badRequest("已入库的入库单不能取消");
        d.setStatus(5);
        d.setCancelReason(reason);
        this.updateById(d);
        return d;
    }

    private StockIn getAndCheck(Long id, int expectedStatus, String msg) {
        StockIn d = this.getById(id);
        if (d == null) throw BusinessException.notFound("入库单不存在");
        if (d.getStatus() != expectedStatus) throw BusinessException.badRequest(msg);
        return d;
    }
}
