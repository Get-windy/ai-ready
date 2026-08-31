package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockOverflowItemVO;
import cn.aiedge.erp.stock.dto.StockOverflowQuery;
import cn.aiedge.erp.stock.entity.StockOverflow;
import cn.aiedge.erp.stock.entity.StockOverflowItem;
import cn.aiedge.erp.stock.mapper.StockOverflowItemMapper;
import cn.aiedge.erp.stock.mapper.StockOverflowMapper;
import cn.aiedge.erp.stock.service.StockOverflowService;
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
public class StockOverflowServiceImpl extends ServiceImpl<StockOverflowMapper, StockOverflow> implements StockOverflowService {

    private final StockOverflowItemMapper overflowItemMapper;

    private static final String NO_PREFIX = "BYD-";

    @Override
    public String generateNo() {
        return NO_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /** 从查询条件构建单据级过滤 wrapper（按单据/按明细共用） */
    private LambdaQueryWrapper<StockOverflow> buildDocWrapper(StockOverflowQuery q) {
        LambdaQueryWrapper<StockOverflow> wrapper = new LambdaQueryWrapper<StockOverflow>()
                .like(StringUtils.hasText(q.getOverflowNo()), StockOverflow::getOverflowNo, q.getOverflowNo())
                .like(StringUtils.hasText(q.getKeyword()), StockOverflow::getOverflowNo, q.getKeyword())
                .like(StringUtils.hasText(q.getHandlerName()), StockOverflow::getHandlerName, q.getHandlerName())
                .like(StringUtils.hasText(q.getDeptName()), StockOverflow::getDeptName, q.getDeptName())
                .like(StringUtils.hasText(q.getCreatorName()), StockOverflow::getCreatorName, q.getCreatorName())
                .like(StringUtils.hasText(q.getBookkeeperName()), StockOverflow::getBookkeeperName, q.getBookkeeperName())
                .like(StringUtils.hasText(q.getRemark()), StockOverflow::getRemark, q.getRemark())
                .like(StringUtils.hasText(q.getWarehouseName()), StockOverflow::getWarehouseName, q.getWarehouseName())
                .eq(q.getWarehouseId() != null, StockOverflow::getWarehouseId, q.getWarehouseId())
                .eq(q.getStatus() != null, StockOverflow::getStatus, q.getStatus())
                .eq(StringUtils.hasText(q.getSourceType()), StockOverflow::getSourceType, q.getSourceType())
                .orderByDesc(StockOverflow::getOverflowDate)
                .orderByDesc(StockOverflow::getCreateTime);
        if (StringUtils.hasText(q.getDateStart())) {
            wrapper.ge(StockOverflow::getOverflowDate, LocalDate.parse(q.getDateStart()));
        }
        if (StringUtils.hasText(q.getDateEnd())) {
            wrapper.le(StockOverflow::getOverflowDate, LocalDate.parse(q.getDateEnd()));
        }
        return wrapper;
    }

    @Override
    public Page<StockOverflow> pageList(StockOverflowQuery query) {
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), buildDocWrapper(query));
    }

    @Override
    public Page<StockOverflowItemVO> pageDetail(StockOverflowQuery query) {
        // 1. 先取满足单据级过滤的单据ID集合
        LambdaQueryWrapper<StockOverflow> docWrapper = buildDocWrapper(query);
        docWrapper.select(StockOverflow::getId);
        List<StockOverflow> docs = this.list(docWrapper);
        List<Long> docIds = docs.stream().map(StockOverflow::getId).collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize(), 0);
        }

        // 2. 明细过滤
        LambdaQueryWrapper<StockOverflowItem> itemWrapper = new LambdaQueryWrapper<StockOverflowItem>()
                .in(StockOverflowItem::getOverflowId, docIds)
                .like(StringUtils.hasText(query.getProductName()), StockOverflowItem::getProductName, query.getProductName())
                .like(StringUtils.hasText(query.getItemRemark()), StockOverflowItem::getRemark, query.getItemRemark())
                .orderByDesc(StockOverflowItem::getCreateTime);
        Page<StockOverflowItem> itemPage = overflowItemMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), itemWrapper);

        // 3. 批量补齐单据级字段
        List<Long> pageDocIds = itemPage.getRecords().stream()
                .map(StockOverflowItem::getOverflowId).distinct().collect(Collectors.toList());
        Map<Long, StockOverflow> docMap = pageDocIds.isEmpty() ? Map.of() :
                this.listByIds(pageDocIds).stream().collect(Collectors.toMap(StockOverflow::getId, Function.identity()));

        List<StockOverflowItemVO> voList = new ArrayList<>();
        for (StockOverflowItem item : itemPage.getRecords()) {
            StockOverflowItemVO vo = new StockOverflowItemVO();
            BeanUtils.copyProperties(item, vo);
            StockOverflow doc = docMap.get(item.getOverflowId());
            if (doc != null) {
                vo.setOverflowDate(doc.getOverflowDate());
                vo.setOverflowNo(doc.getOverflowNo());
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
        Page<StockOverflowItemVO> voPage = new Page<>(query.getPageNum(), query.getPageSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public StockOverflow getDetail(Long id) {
        StockOverflow o = this.getById(id);
        if (o == null) throw BusinessException.notFound("报溢单不存在");
        o.setItems(getItems(id));
        return o;
    }

    @Override
    public List<StockOverflowItem> getItems(Long overflowId) {
        return overflowItemMapper.selectList(
                new LambdaQueryWrapper<StockOverflowItem>().eq(StockOverflowItem::getOverflowId, overflowId)
                        .orderByAsc(StockOverflowItem::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOverflow createOverflow(StockOverflow overflow, List<StockOverflowItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        overflow.setId(null);
        overflow.setOverflowNo(generateNo());
        overflow.setTenantId(1L);
        if (overflow.getStatus() == null) overflow.setStatus(0);
        overflow.setCreateBy(userId);
        if (overflow.getCreatorName() == null) overflow.setCreatorName(String.valueOf(userId));
        overflow.setCreateTime(LocalDateTime.now());
        fillTotals(overflow, items);
        this.save(overflow);
        saveItems(overflow.getId(), items);
        return overflow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOverflow updateOverflow(Long id, StockOverflow overflow, List<StockOverflowItem> items) {
        StockOverflow exist = this.getById(id);
        if (exist == null) throw BusinessException.notFound("报溢单不存在");
        if (exist.getStatus() != 0) throw BusinessException.badRequest("只有草稿状态的报溢单可以修改");
        overflow.setId(id);
        overflow.setTenantId(exist.getTenantId());
        if (overflow.getOverflowNo() == null) overflow.setOverflowNo(exist.getOverflowNo());
        overflow.setStatus(0);
        overflow.setCreateBy(exist.getCreateBy());
        overflow.setCreateTime(exist.getCreateTime());
        overflow.setUpdateBy(StpUtil.getLoginIdAsLong());
        fillTotals(overflow, items);
        this.updateById(overflow);
        // 重建明细
        overflowItemMapper.delete(new LambdaQueryWrapper<StockOverflowItem>().eq(StockOverflowItem::getOverflowId, id));
        saveItems(id, items);
        return overflow;
    }

    private void fillTotals(StockOverflow overflow, List<StockOverflowItem> items) {
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        if (items != null) {
            for (StockOverflowItem item : items) {
                if (item.getAmount() == null && item.getQuantity() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO));
                }
                totalQty = totalQty.add(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
                totalAmt = totalAmt.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                totalWeight = totalWeight.add(item.getWeight() != null ? item.getWeight() : BigDecimal.ZERO);
                totalVolume = totalVolume.add(item.getVolume() != null ? item.getVolume() : BigDecimal.ZERO);
            }
        }
        overflow.setTotalQuantity(totalQty);
        overflow.setTotalAmount(totalAmt);
        overflow.setTotalWeight(totalWeight);
        overflow.setTotalVolume(totalVolume);
        overflow.setTotalItems(items != null ? items.size() : 0);
    }

    private void saveItems(Long overflowId, List<StockOverflowItem> items) {
        if (items == null) return;
        for (StockOverflowItem item : items) {
            item.setId(null);
            item.setOverflowId(overflowId);
            item.setTenantId(1L);
            item.setCreateTime(LocalDateTime.now());
            overflowItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOverflow submitForApproval(Long id) {
        StockOverflow o = getAndCheck(id, 0, "只有草稿状态的报溢单可以提交审批");
        o.setStatus(1);
        o.setApplicantId(StpUtil.getLoginIdAsLong());
        o.setApplicantName(StpUtil.getLoginId().toString());
        o.setApplyTime(LocalDateTime.now());
        this.updateById(o);
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOverflow approve(Long id, Long approverId, String note) {
        StockOverflow o = getAndCheck(id, 1, "只有待审批状态的报溢单可以审批");
        o.setStatus(2);
        o.setApprovedBy(approverId);
        o.setApprovedTime(LocalDateTime.now());
        o.setApprovedNote(note);
        this.updateById(o);
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOverflow reject(Long id, String reason) {
        StockOverflow o = getAndCheck(id, 1, "只有待审批状态的报溢单可以拒绝");
        o.setStatus(4);
        o.setApprovedNote(reason);
        this.updateById(o);
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOverflow execute(Long id) {
        // 执行入库（记账）：报溢入库生效，按批次增加库存
        StockOverflow o = getAndCheck(id, 2, "只有已审核状态的报溢单可以记账入库");
        o.setStatus(3);
        o.setExecutedBy(StpUtil.getLoginIdAsLong());
        o.setExecutedTime(LocalDateTime.now());
        o.setBookkeeperId(StpUtil.getLoginIdAsLong());
        o.setBookkeeperName(StpUtil.getLoginId().toString());
        o.setBookkeepingTime(LocalDateTime.now());
        this.updateById(o);
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOverflow cancel(Long id, String reason) {
        StockOverflow o = this.getById(id);
        if (o == null) throw BusinessException.notFound("报溢单不存在");
        if (o.getStatus() >= 3) throw BusinessException.badRequest("已记账入库的报溢单不能取消");
        o.setStatus(5);
        o.setCancelReason(reason);
        this.updateById(o);
        return o;
    }

    private StockOverflow getAndCheck(Long id, int expectedStatus, String msg) {
        StockOverflow o = this.getById(id);
        if (o == null) throw BusinessException.notFound("报溢单不存在");
        if (o.getStatus() != expectedStatus) throw BusinessException.badRequest(msg);
        return o;
    }
}
