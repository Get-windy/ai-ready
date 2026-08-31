package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockDamageItemVO;
import cn.aiedge.erp.stock.dto.StockDamageQuery;
import cn.aiedge.erp.stock.entity.StockDamage;
import cn.aiedge.erp.stock.entity.StockDamageItem;
import cn.aiedge.erp.stock.mapper.StockDamageItemMapper;
import cn.aiedge.erp.stock.mapper.StockDamageMapper;
import cn.aiedge.erp.stock.service.StockDamageService;
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
public class StockDamageServiceImpl extends ServiceImpl<StockDamageMapper, StockDamage> implements StockDamageService {

    private final StockDamageItemMapper damageItemMapper;

    private static final String NO_PREFIX = "BSD-";

    @Override
    public String generateNo() {
        return NO_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /** 从查询条件构建单据级过滤 wrapper（按单据/按明细共用） */
    private LambdaQueryWrapper<StockDamage> buildDocWrapper(StockDamageQuery q) {
        LambdaQueryWrapper<StockDamage> wrapper = new LambdaQueryWrapper<StockDamage>()
                .like(StringUtils.hasText(q.getDamageNo()), StockDamage::getDamageNo, q.getDamageNo())
                .like(StringUtils.hasText(q.getKeyword()), StockDamage::getDamageNo, q.getKeyword())
                .like(StringUtils.hasText(q.getHandlerName()), StockDamage::getHandlerName, q.getHandlerName())
                .like(StringUtils.hasText(q.getDeptName()), StockDamage::getDeptName, q.getDeptName())
                .like(StringUtils.hasText(q.getCreatorName()), StockDamage::getCreatorName, q.getCreatorName())
                .like(StringUtils.hasText(q.getBookkeeperName()), StockDamage::getBookkeeperName, q.getBookkeeperName())
                .like(StringUtils.hasText(q.getRemark()), StockDamage::getRemark, q.getRemark())
                .like(StringUtils.hasText(q.getWarehouseName()), StockDamage::getWarehouseName, q.getWarehouseName())
                .eq(q.getWarehouseId() != null, StockDamage::getWarehouseId, q.getWarehouseId())
                .eq(q.getStatus() != null, StockDamage::getStatus, q.getStatus())
                .eq(q.getDamageCause() != null, StockDamage::getDamageCause, q.getDamageCause())
                .orderByDesc(StockDamage::getDamageDate)
                .orderByDesc(StockDamage::getCreateTime);
        if (StringUtils.hasText(q.getDateStart())) {
            wrapper.ge(StockDamage::getDamageDate, LocalDate.parse(q.getDateStart()));
        }
        if (StringUtils.hasText(q.getDateEnd())) {
            wrapper.le(StockDamage::getDamageDate, LocalDate.parse(q.getDateEnd()));
        }
        return wrapper;
    }

    @Override
    public Page<StockDamage> pageList(StockDamageQuery query) {
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), buildDocWrapper(query));
    }

    @Override
    public Page<StockDamageItemVO> pageDetail(StockDamageQuery query) {
        // 1. 先取满足单据级过滤的单据ID集合
        LambdaQueryWrapper<StockDamage> docWrapper = buildDocWrapper(query);
        docWrapper.select(StockDamage::getId);
        List<StockDamage> docs = this.list(docWrapper);
        List<Long> docIds = docs.stream().map(StockDamage::getId).collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize(), 0);
        }

        // 2. 明细过滤
        LambdaQueryWrapper<StockDamageItem> itemWrapper = new LambdaQueryWrapper<StockDamageItem>()
                .in(StockDamageItem::getDamageId, docIds)
                .like(StringUtils.hasText(query.getProductName()), StockDamageItem::getProductName, query.getProductName())
                .like(StringUtils.hasText(query.getItemRemark()), StockDamageItem::getRemark, query.getItemRemark())
                .orderByDesc(StockDamageItem::getCreateTime);
        Page<StockDamageItem> itemPage = damageItemMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), itemWrapper);

        // 3. 批量补齐单据级字段
        List<Long> pageDocIds = itemPage.getRecords().stream()
                .map(StockDamageItem::getDamageId).distinct().collect(Collectors.toList());
        Map<Long, StockDamage> docMap = pageDocIds.isEmpty() ? Map.of() :
                this.listByIds(pageDocIds).stream().collect(Collectors.toMap(StockDamage::getId, Function.identity()));

        List<StockDamageItemVO> voList = new ArrayList<>();
        for (StockDamageItem item : itemPage.getRecords()) {
            StockDamageItemVO vo = new StockDamageItemVO();
            BeanUtils.copyProperties(item, vo);
            StockDamage doc = docMap.get(item.getDamageId());
            if (doc != null) {
                vo.setDamageDate(doc.getDamageDate());
                vo.setDamageNo(doc.getDamageNo());
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
        Page<StockDamageItemVO> voPage = new Page<>(query.getPageNum(), query.getPageSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public StockDamage getDetail(Long id) {
        StockDamage d = this.getById(id);
        if (d == null) throw BusinessException.notFound("报损单不存在");
        d.setItems(getItems(id));
        return d;
    }

    @Override
    public List<StockDamageItem> getItems(Long damageId) {
        return damageItemMapper.selectList(
                new LambdaQueryWrapper<StockDamageItem>().eq(StockDamageItem::getDamageId, damageId)
                        .orderByAsc(StockDamageItem::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockDamage createStockDamage(StockDamage damage, List<StockDamageItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        damage.setId(null);
        damage.setDamageNo(generateNo());
        damage.setTenantId(1L);
        if (damage.getStatus() == null) damage.setStatus(0);
        damage.setCreateBy(userId);
        if (damage.getCreatorName() == null) damage.setCreatorName(String.valueOf(userId));
        damage.setCreateTime(LocalDateTime.now());
        fillTotals(damage, items);
        this.save(damage);
        saveItems(damage.getId(), items);
        return damage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockDamage updateStockDamage(Long id, StockDamage damage, List<StockDamageItem> items) {
        StockDamage exist = this.getById(id);
        if (exist == null) throw BusinessException.notFound("报损单不存在");
        if (exist.getStatus() != 0) throw BusinessException.badRequest("只有草稿状态的报损单可以修改");
        damage.setId(id);
        damage.setTenantId(exist.getTenantId());
        if (damage.getDamageNo() == null) damage.setDamageNo(exist.getDamageNo());
        damage.setStatus(0);
        damage.setCreateBy(exist.getCreateBy());
        damage.setCreateTime(exist.getCreateTime());
        damage.setUpdateBy(StpUtil.getLoginIdAsLong());
        fillTotals(damage, items);
        this.updateById(damage);
        // 重建明细
        damageItemMapper.delete(new LambdaQueryWrapper<StockDamageItem>().eq(StockDamageItem::getDamageId, id));
        saveItems(id, items);
        return damage;
    }

    private void fillTotals(StockDamage damage, List<StockDamageItem> items) {
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        if (items != null) {
            for (StockDamageItem item : items) {
                if (item.getAmount() == null && item.getQuantity() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO));
                }
                totalQty = totalQty.add(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
                totalAmt = totalAmt.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                totalWeight = totalWeight.add(item.getWeight() != null ? item.getWeight() : BigDecimal.ZERO);
                totalVolume = totalVolume.add(item.getVolume() != null ? item.getVolume() : BigDecimal.ZERO);
            }
        }
        damage.setTotalQuantity(totalQty);
        damage.setTotalAmount(totalAmt);
        damage.setTotalWeight(totalWeight);
        damage.setTotalVolume(totalVolume);
        damage.setTotalItems(items != null ? items.size() : 0);
    }

    private void saveItems(Long damageId, List<StockDamageItem> items) {
        if (items == null) return;
        for (StockDamageItem item : items) {
            item.setId(null);
            item.setDamageId(damageId);
            item.setTenantId(1L);
            item.setCreateTime(LocalDateTime.now());
            damageItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockDamage submitForApproval(Long id) {
        StockDamage d = getAndCheck(id, 0, "只有草稿状态的报损单可以提交审批");
        d.setStatus(1);
        d.setApplicantId(StpUtil.getLoginIdAsLong());
        d.setApplicantName(StpUtil.getLoginId().toString());
        d.setApplyTime(LocalDateTime.now());
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockDamage approve(Long id, Long approverId, String note) {
        StockDamage d = getAndCheck(id, 1, "只有待审批状态的报损单可以审批");
        d.setStatus(2);
        d.setApprovedBy(approverId);
        d.setApprovedTime(LocalDateTime.now());
        d.setApprovedNote(note);
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockDamage reject(Long id, String reason) {
        StockDamage d = getAndCheck(id, 1, "只有待审批状态的报损单可以拒绝");
        d.setStatus(4);
        d.setApprovedNote(reason);
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockDamage execute(Long id) {
        // 执行出库（记账）：报损出库生效，按批次扣减库存
        StockDamage d = getAndCheck(id, 2, "只有已审核状态的报损单可以记账出库");
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
    public StockDamage cancel(Long id, String reason) {
        StockDamage d = this.getById(id);
        if (d == null) throw BusinessException.notFound("报损单不存在");
        if (d.getStatus() >= 3) throw BusinessException.badRequest("已记账出库的报损单不能取消");
        d.setStatus(5);
        d.setCancelReason(reason);
        this.updateById(d);
        return d;
    }

    private StockDamage getAndCheck(Long id, int expectedStatus, String msg) {
        StockDamage d = this.getById(id);
        if (d == null) throw BusinessException.notFound("报损单不存在");
        if (d.getStatus() != expectedStatus) throw BusinessException.badRequest(msg);
        return d;
    }
}
