package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import cn.aiedge.erp.stock.dto.StockTakeItemVO;
import cn.aiedge.erp.stock.dto.StockTakeQuery;
import cn.aiedge.erp.stock.dto.StockTakeUncheckedVO;
import cn.aiedge.erp.stock.entity.*;
import cn.aiedge.erp.stock.mapper.StockTakeItemMapper;
import cn.aiedge.erp.stock.mapper.StockTakeMapper;
import cn.aiedge.erp.stock.service.StockDamageService;
import cn.aiedge.erp.stock.service.StockOverflowService;
import cn.aiedge.erp.stock.service.StockService;
import cn.aiedge.erp.stock.service.StockTakeService;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTakeServiceImpl extends ServiceImpl<StockTakeMapper, StockTake> implements StockTakeService {

    private final StockTakeItemMapper stockTakeItemMapper;
    private final BizNumberGeneratorService bizNumberGeneratorService;
    private final StockOverflowService overflowService;
    private final StockDamageService damageService;
    private final StockService stockService;
    private final cn.aiedge.erp.stock.mapper.ProductMapper productMapper;

    private static final String BIZ_TYPE = "PDD";

    @Override
    public String generateNo() {
        return bizNumberGeneratorService.nextNumber(BIZ_TYPE, 1L, "zh_CN");
    }

    /** 从查询条件构建单据级过滤 wrapper（按单据/按明细共用） */
    private LambdaQueryWrapper<StockTake> buildDocWrapper(StockTakeQuery q) {
        LambdaQueryWrapper<StockTake> wrapper = new LambdaQueryWrapper<StockTake>()
                .like(StringUtils.hasText(q.getStockTakeNo()), StockTake::getStockTakeNo, q.getStockTakeNo())
                .like(StringUtils.hasText(q.getKeyword()), StockTake::getRemark, q.getKeyword())
                .like(StringUtils.hasText(q.getHandlerName()), StockTake::getHandlerName, q.getHandlerName())
                .like(StringUtils.hasText(q.getDeptName()), StockTake::getDeptName, q.getDeptName())
                .like(StringUtils.hasText(q.getCreatorName()), StockTake::getCreatorName, q.getCreatorName())
                .like(StringUtils.hasText(q.getBookkeeperName()), StockTake::getBookkeeperName, q.getBookkeeperName())
                .like(StringUtils.hasText(q.getRemark()), StockTake::getRemark, q.getRemark())
                .like(StringUtils.hasText(q.getWarehouseName()), StockTake::getWarehouseName, q.getWarehouseName())
                .like(StringUtils.hasText(q.getLinkedBillNo()), StockTake::getLinkedBillNo, q.getLinkedBillNo())
                .like(StringUtils.hasText(q.getRegionName()), StockTake::getRegionName, q.getRegionName())
                .eq(q.getWarehouseId() != null, StockTake::getWarehouseId, q.getWarehouseId())
                .eq(q.getCheckMethod() != null, StockTake::getCheckMethod, q.getCheckMethod())
                .eq(q.getCheckType() != null, StockTake::getCheckType, q.getCheckType())
                .eq(q.getStatus() != null, StockTake::getStatus, q.getStatus())
                .eq(q.getHandlerId() != null, StockTake::getHandlerId, q.getHandlerId())
                .eq(q.getDeptId() != null, StockTake::getDeptId, q.getDeptId())
                .orderByDesc(StockTake::getStockTakeDate)
                .orderByDesc(StockTake::getCreateTime);
        if (StringUtils.hasText(q.getDateStart())) {
            wrapper.ge(StockTake::getStockTakeDate, LocalDate.parse(q.getDateStart()));
        }
        if (StringUtils.hasText(q.getDateEnd())) {
            wrapper.le(StockTake::getStockTakeDate, LocalDate.parse(q.getDateEnd()));
        }
        return wrapper;
    }

    @Override
    public Page<StockTake> pageList(StockTakeQuery query) {
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), buildDocWrapper(query));
    }

    @Override
    public Page<StockTakeItemVO> pageDetail(StockTakeQuery query) {
        LambdaQueryWrapper<StockTake> docWrapper = buildDocWrapper(query);
        docWrapper.select(StockTake::getId);
        List<StockTake> docs = this.list(docWrapper);
        List<Long> docIds = docs.stream().map(StockTake::getId).collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize(), 0);
        }

        LambdaQueryWrapper<StockTakeItem> itemWrapper = new LambdaQueryWrapper<StockTakeItem>()
                .in(StockTakeItem::getStockTakeId, docIds)
                .like(StringUtils.hasText(query.getProductName()), StockTakeItem::getProductName, query.getProductName())
                .like(StringUtils.hasText(query.getItemRemark()), StockTakeItem::getRemark, query.getItemRemark())
                .eq(StringUtils.hasText(query.getLocation()), StockTakeItem::getLocation, query.getLocation())
                .orderByDesc(StockTakeItem::getCreateTime);
        Page<StockTakeItem> itemPage = stockTakeItemMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), itemWrapper);

        List<Long> pageDocIds = itemPage.getRecords().stream()
                .map(StockTakeItem::getStockTakeId).distinct().collect(Collectors.toList());
        Map<Long, StockTake> docMap = pageDocIds.isEmpty() ? Map.of() :
                this.listByIds(pageDocIds).stream().collect(Collectors.toMap(StockTake::getId, Function.identity()));

        List<StockTakeItemVO> voList = new ArrayList<>();
        for (StockTakeItem item : itemPage.getRecords()) {
            StockTakeItemVO vo = new StockTakeItemVO();
            BeanUtils.copyProperties(item, vo);
            StockTake doc = docMap.get(item.getStockTakeId());
            if (doc != null) {
                vo.setStockTakeDate(doc.getStockTakeDate());
                vo.setStockTakeNo(doc.getStockTakeNo());
                vo.setStatus(doc.getStatus());
                vo.setCheckMethod(doc.getCheckMethod());
                vo.setCheckType(doc.getCheckType());
                vo.setWarehouseId(doc.getWarehouseId());
                vo.setWarehouseName(doc.getWarehouseName());
                vo.setRegionName(doc.getRegionName());
                vo.setHandlerId(doc.getHandlerId());
                vo.setHandlerName(doc.getHandlerName());
                vo.setDeptName(doc.getDeptName());
                vo.setLinkedBillNo(doc.getLinkedBillNo());
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
        Page<StockTakeItemVO> voPage = new Page<>(query.getPageNum(), query.getPageSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public StockTake getDetail(Long id) {
        StockTake d = this.getById(id);
        if (d == null) throw BusinessException.notFound("盘点单不存在");
        d.setItems(getItems(id));
        return d;
    }

    @Override
    public List<StockTakeItem> getItems(Long stockTakeId) {
        return stockTakeItemMapper.selectList(
                new LambdaQueryWrapper<StockTakeItem>().eq(StockTakeItem::getStockTakeId, stockTakeId)
                        .orderByAsc(StockTakeItem::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTake createStockTake(StockTake stockTake, List<StockTakeItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        stockTake.setId(null);
        stockTake.setStockTakeNo(generateNo());
        stockTake.setTenantId(1L);
        // 保存即生效：状态 1=已保存（未盘点），盘点处理后置为 2
        if (stockTake.getStatus() == null) stockTake.setStatus(1);
        stockTake.setCreateBy(userId);
        stockTake.setCreatorName(StpUtil.getLoginId().toString());
        stockTake.setCreateTime(LocalDateTime.now());
        fillTotals(stockTake, items);
        this.save(stockTake);
        saveItems(stockTake.getId(), items);
        return stockTake;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTake updateStockTake(Long id, StockTake stockTake, List<StockTakeItem> items) {
        StockTake exist = this.getById(id);
        if (exist == null) throw BusinessException.notFound("盘点单不存在");
        if (exist.getStatus() != 0 && exist.getStatus() != 1) {
            throw BusinessException.badRequest("只有草稿或已保存状态的盘点单可以修改");
        }
        stockTake.setId(id);
        stockTake.setTenantId(exist.getTenantId());
        if (stockTake.getStockTakeNo() == null) stockTake.setStockTakeNo(exist.getStockTakeNo());
        stockTake.setStatus(exist.getStatus());
        stockTake.setCreateBy(exist.getCreateBy());
        stockTake.setCreateTime(exist.getCreateTime());
        stockTake.setUpdateBy(StpUtil.getLoginIdAsLong());
        fillTotals(stockTake, items);
        this.updateById(stockTake);
        stockTakeItemMapper.delete(new LambdaQueryWrapper<StockTakeItem>().eq(StockTakeItem::getStockTakeId, id));
        saveItems(id, items);
        return stockTake;
    }

    private void fillTotals(StockTake stockTake, List<StockTakeItem> items) {
        BigDecimal totalDiffQty = BigDecimal.ZERO;
        BigDecimal totalDiffAmount = BigDecimal.ZERO;
        if (items != null) {
            for (StockTakeItem item : items) {
                item.setDiffQuantity(calcDiff(item));
                if (item.getDiffAmount() == null && item.getCostPrice() != null && item.getDiffQuantity() != null) {
                    item.setDiffAmount(item.getDiffQuantity().multiply(item.getCostPrice()));
                }
                if (item.getDiffAmount() == null) item.setDiffAmount(BigDecimal.ZERO);
                totalDiffQty = totalDiffQty.add(item.getDiffQuantity() != null ? item.getDiffQuantity() : BigDecimal.ZERO);
                totalDiffAmount = totalDiffAmount.add(item.getDiffAmount() != null ? item.getDiffAmount() : BigDecimal.ZERO);
                autoCheckStatus(item);
            }
        }
        stockTake.setTotalDiffQuantity(totalDiffQty);
        stockTake.setTotalDiffAmount(totalDiffAmount);
        stockTake.setTotalItems(items != null ? items.size() : 0);
    }

    private BigDecimal calcDiff(StockTakeItem item) {
        BigDecimal check = item.getCheckQuantity() != null ? item.getCheckQuantity() : BigDecimal.ZERO;
        BigDecimal stock = item.getStockQuantity() != null ? item.getStockQuantity() : BigDecimal.ZERO;
        return check.subtract(stock);
    }

    private void autoCheckStatus(StockTakeItem item) {
        if (item.getCheckStatus() == null) {
            item.setCheckStatus(1);
        }
    }

    private void saveItems(Long stockTakeId, List<StockTakeItem> items) {
        if (items == null) return;
        for (StockTakeItem item : items) {
            item.setId(null);
            item.setStockTakeId(stockTakeId);
            item.setTenantId(1L);
            item.setCreateTime(LocalDateTime.now());
            stockTakeItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTake process(Long id) {
        StockTake d = getAndCheck(id, "盘点单不存在");
        if (d.getStatus() == 2) throw BusinessException.badRequest("盘点单已处理，不能重复盘点处理");
        List<StockTakeItem> items = getItems(id);
        if (items.isEmpty()) throw BusinessException.badRequest("盘点单没有明细，无法盘点处理");

        List<String> created = new ArrayList<>();
        List<StockTakeItem> positive = items.stream().filter(i -> i.getDiffQuantity() != null && i.getDiffQuantity().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
        List<StockTakeItem> negative = items.stream().filter(i -> i.getDiffQuantity() != null && i.getDiffQuantity().compareTo(BigDecimal.ZERO) < 0).collect(Collectors.toList());

        // 盘盈 -> 报溢单
        if (!positive.isEmpty()) {
            StockOverflow overflow = new StockOverflow();
            overflow.setTenantId(d.getTenantId());
            overflow.setOverflowDate(d.getStockTakeDate());
            overflow.setWarehouseId(d.getWarehouseId());
            overflow.setWarehouseName(d.getWarehouseName());
            overflow.setHandlerId(d.getHandlerId());
            overflow.setHandlerName(d.getHandlerName());
            overflow.setDeptId(d.getDeptId());
            overflow.setDeptName(d.getDeptName());
            overflow.setSourceType("stocktake");
            overflow.setSummary("由盘点单" + d.getStockTakeNo() + "盘盈生成");
            overflow.setRemark("由盘点单" + d.getStockTakeNo() + "盘盈生成");
            overflow.setStatus(0);
            List<StockOverflowItem> ofItems = positive.stream().map(i -> toOverflowItem(i)).collect(Collectors.toList());
            StockOverflow saved = overflowService.createOverflow(overflow, ofItems);
            created.add("报溢单" + saved.getOverflowNo());
        }

        // 盘亏 -> 报损单
        if (!negative.isEmpty()) {
            StockDamage damage = new StockDamage();
            damage.setTenantId(d.getTenantId());
            damage.setDamageDate(d.getStockTakeDate());
            damage.setWarehouseId(d.getWarehouseId());
            damage.setWarehouseName(d.getWarehouseName());
            damage.setHandlerId(d.getHandlerId());
            damage.setHandlerName(d.getHandlerName());
            damage.setDeptId(d.getDeptId());
            damage.setDeptName(d.getDeptName());
            damage.setSummary("由盘点单" + d.getStockTakeNo() + "盘亏生成");
            damage.setRemark("由盘点单" + d.getStockTakeNo() + "盘亏生成");
            damage.setStatus(0);
            List<StockDamageItem> dmItems = negative.stream().map(i -> toDamageItem(i)).collect(Collectors.toList());
            StockDamage saved = damageService.createStockDamage(damage, dmItems);
            created.add("报损单" + saved.getDamageNo());
        }

        // 回写库存：按实盘数调整
        for (StockTakeItem item : items) {
            if (item.getProductId() != null && item.getCheckQuantity() != null && d.getWarehouseId() != null) {
                stockService.checkStock(item.getProductId(), d.getWarehouseId(), item.getCheckQuantity());
            }
        }

        d.setStatus(2);
        d.setExecutedBy(StpUtil.getLoginIdAsLong());
        d.setExecutedTime(LocalDateTime.now());
        d.setBookkeeperId(StpUtil.getLoginIdAsLong());
        d.setBookkeeperName(StpUtil.getLoginId().toString());
        d.setBookkeepingTime(LocalDateTime.now());
        d.setProcessResult(created.isEmpty() ? "无盈亏差异" : String.join("、", created));
        d.setUpdateBy(StpUtil.getLoginIdAsLong());
        this.updateById(d);
        return d;
    }

    private StockOverflowItem toOverflowItem(StockTakeItem i) {
        StockOverflowItem oi = new StockOverflowItem();
        oi.setTenantId(i.getTenantId());
        oi.setProductId(i.getProductId());
        oi.setProductCode(i.getProductCode());
        oi.setProductName(i.getProductName());
        oi.setProductSpec(i.getProductSpec());
        oi.setProductUnit(i.getProductUnit());
        oi.setBarcode(i.getBarcode());
        oi.setModel(i.getModel());
        oi.setOrigin(i.getOrigin());
        oi.setBrand(i.getBrand());
        oi.setRegion(i.getRegion());
        oi.setLocation(i.getLocation());
        oi.setBatchCode(i.getBatchCode());
        oi.setProductionDate(i.getProductionDate());
        oi.setShelfLife(i.getShelfLife());
        oi.setExpiryDate(i.getExpiryDate());
        oi.setQuantity(i.getDiffQuantity().abs());
        oi.setConversionRelation(i.getConversionRelation());
        oi.setConversionResult(i.getConversionResult());
        oi.setPieceQuantity(i.getPieceQuantity());
        oi.setBigPack(i.getBigPack());
        oi.setMidPack(i.getMidPack());
        oi.setSmallPack(i.getSmallPack());
        oi.setUnitCost(i.getCostPrice());
        if (i.getDiffAmount() != null) oi.setAmount(i.getDiffAmount().abs());
        oi.setWeight(i.getDiffQuantity() != null ? i.getDiffQuantity().abs() : null);
        oi.setRemark(i.getRemark());
        return oi;
    }

    private StockDamageItem toDamageItem(StockTakeItem i) {
        StockDamageItem di = new StockDamageItem();
        di.setTenantId(i.getTenantId());
        di.setProductId(i.getProductId());
        di.setProductCode(i.getProductCode());
        di.setProductName(i.getProductName());
        di.setProductSpec(i.getProductSpec());
        di.setProductUnit(i.getProductUnit());
        di.setBarcode(i.getBarcode());
        di.setModel(i.getModel());
        di.setOrigin(i.getOrigin());
        di.setBrand(i.getBrand());
        di.setRegion(i.getRegion());
        di.setLocation(i.getLocation());
        di.setBatchCode(i.getBatchCode());
        di.setProductionDate(i.getProductionDate());
        di.setShelfLife(i.getShelfLife());
        di.setExpiryDate(i.getExpiryDate());
        di.setQuantity(i.getDiffQuantity().abs());
        di.setConversionRelation(i.getConversionRelation());
        di.setConversionResult(i.getConversionResult());
        di.setPieceQuantity(i.getPieceQuantity());
        di.setBigPack(i.getBigPack());
        di.setMidPack(i.getMidPack());
        di.setSmallPack(i.getSmallPack());
        di.setUnitCost(i.getCostPrice());
        if (i.getDiffAmount() != null) di.setAmount(i.getDiffAmount().abs());
        di.setWeight(i.getDiffQuantity() != null ? i.getDiffQuantity().abs() : null);
        di.setRemark(i.getRemark());
        return di;
    }

    @Override
    public List<StockTakeUncheckedVO> uncheckedProducts(StockTakeQuery query) {
        Long warehouseId = query.getCheckWarehouseId() != null ? query.getCheckWarehouseId() : query.getWarehouseId();
        if (warehouseId == null) return Collections.emptyList();
        List<cn.aiedge.erp.stock.entity.Stock> stocks = stockService.list(
                new LambdaQueryWrapper<cn.aiedge.erp.stock.entity.Stock>()
                        .eq(cn.aiedge.erp.stock.entity.Stock::getWarehouseId, warehouseId)
                        .orderByDesc(cn.aiedge.erp.stock.entity.Stock::getCreateTime));
        if (stocks.isEmpty()) return Collections.emptyList();

        // 排除已进入盘点单明细的商品
        Set<Long> countedProductIds = Collections.emptySet();
        if (query.getExcludeStockTakeId() != null) {
            countedProductIds = stockTakeItemMapper.selectList(
                            new LambdaQueryWrapper<StockTakeItem>().eq(StockTakeItem::getStockTakeId, query.getExcludeStockTakeId()))
                    .stream().map(StockTakeItem::getProductId).collect(Collectors.toSet());
        }
        if (countedProductIds == null) countedProductIds = Collections.emptySet();
        final Set<Long> excluded = countedProductIds;

        List<Long> productIds = stocks.stream().map(cn.aiedge.erp.stock.entity.Stock::getProductId)
                .filter(pid -> pid != null && !excluded.contains(pid)).collect(Collectors.toList());
        if (productIds.isEmpty()) return Collections.emptyList();
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<StockTakeUncheckedVO> list = new ArrayList<>();
        for (cn.aiedge.erp.stock.entity.Stock s : stocks) {
            if (countedProductIds.contains(s.getProductId())) continue;
            Product p = productMap.get(s.getProductId());
            if (p == null) continue;
            // 关键字过滤：商品编码/名称
            if (StringUtils.hasText(query.getProductKeyword())) {
                String kw = query.getProductKeyword();
                boolean match = (p.getProductCode() != null && p.getProductCode().contains(kw))
                        || (p.getProductName() != null && p.getProductName().contains(kw));
                if (!match) continue;
            }
            // 无货过滤
            if ("有货".equals(query.getShowStatus()) && (s.getQuantity() == null || s.getQuantity().compareTo(BigDecimal.ZERO) <= 0)) continue;
            if ("无货".equals(query.getShowStatus()) && s.getQuantity() != null && s.getQuantity().compareTo(BigDecimal.ZERO) > 0) continue;
            // 可用库存/账面库存下限过滤
            if (query.getAvailableStockMin() != null && (s.getAvailableQuantity() == null || s.getAvailableQuantity().compareTo(query.getAvailableStockMin()) < 0)) continue;
            if (query.getBookStockMin() != null && (s.getQuantity() == null || s.getQuantity().compareTo(query.getBookStockMin()) < 0)) continue;

            StockTakeUncheckedVO vo = new StockTakeUncheckedVO();
            vo.setProductId(p.getId());
            vo.setProductCode(p.getProductCode());
            vo.setProductName(p.getProductName());
            vo.setProductUnit(p.getUnit());
            vo.setProductSpec(p.getSpec());
            vo.setModel(p.getModel());
            vo.setOrigin(p.getOrigin());
            vo.setBrand(p.getBrand());
            vo.setBarcode(p.getBarcode());
            vo.setWeight(p.getWeight());
            vo.setVolume(p.getVolume());
            vo.setStockQuantity(s.getQuantity());
            vo.setAvailableStock(s.getAvailableQuantity());
            vo.setWarehouseId(s.getWarehouseId());
            vo.setWarehouseName(s.getWarehouseName());
            vo.setLocation(query.getLocation());
            vo.setRegion(null);
            list.add(vo);
        }
        return list;
    }

    private StockTake getAndCheck(Long id, String msg) {
        StockTake d = this.getById(id);
        if (d == null) throw BusinessException.notFound(msg);
        return d;
    }
}
