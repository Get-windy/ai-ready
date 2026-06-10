package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockOverflowServiceImpl extends ServiceImpl<StockOverflowMapper, StockOverflow> implements StockOverflowService {

    private final StockOverflowItemMapper overflowItemMapper;

    @Override
    public Page<StockOverflow> pageList(String keyword, Long warehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockOverflow> wrapper = new LambdaQueryWrapper<StockOverflow>()
                .like(keyword != null, StockOverflow::getOverflowNo, keyword)
                .eq(warehouseId != null, StockOverflow::getWarehouseId, warehouseId)
                .eq(status != null, StockOverflow::getStatus, status)
                .orderByDesc(StockOverflow::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    private String generateNo() {
        return "OF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOverflow createOverflow(StockOverflow overflow, List<StockOverflowItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        overflow.setOverflowNo(generateNo());
        overflow.setTenantId(1L);
        overflow.setStatus(0);
        overflow.setCreateBy(userId);
        overflow.setCreateTime(LocalDateTime.now());
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        if (items != null) {
            for (StockOverflowItem item : items) {
                item.setAmount(item.getQuantity().multiply(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO));
                totalQty = totalQty.add(item.getQuantity());
                totalAmt = totalAmt.add(item.getAmount());
                item.setCreateTime(LocalDateTime.now());
            }
        }
        overflow.setTotalQuantity(totalQty);
        overflow.setTotalAmount(totalAmt);
        overflow.setTotalItems(items != null ? items.size() : 0);
        this.save(overflow);
        if (items != null) {
            for (StockOverflowItem item : items) {
                item.setOverflowId(overflow.getId());
                overflowItemMapper.insert(item);
            }
        }
        return overflow;
    }

    private StockOverflow getAndCheck(Long id, int expectedStatus, String msg) {
        StockOverflow o = this.getById(id);
        if (o == null) throw BusinessException.notFound("报溢单不存在");
        if (o.getStatus() != expectedStatus) throw BusinessException.badRequest(msg);
        return o;
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
        StockOverflow o = getAndCheck(id, 2, "只有已审核状态的报溢单可以执行入库");
        o.setStatus(3);
        o.setExecutedBy(StpUtil.getLoginIdAsLong());
        o.setExecutedTime(LocalDateTime.now());
        this.updateById(o);
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOverflow cancel(Long id, String reason) {
        StockOverflow o = this.getById(id);
        if (o == null) throw BusinessException.notFound("报溢单不存在");
        if (o.getStatus() >= 3) throw BusinessException.badRequest("已执行的报溢单不能取消");
        o.setStatus(5);
        this.updateById(o);
        return o;
    }

    @Override
    public List<StockOverflowItem> getItems(Long overflowId) {
        return overflowItemMapper.selectList(
                new LambdaQueryWrapper<StockOverflowItem>().eq(StockOverflowItem::getOverflowId, overflowId));
    }
}
