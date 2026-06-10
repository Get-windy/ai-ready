package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockDamageServiceImpl extends ServiceImpl<StockDamageMapper, StockDamage> implements StockDamageService {

    private final StockDamageItemMapper damageItemMapper;

    @Override
    public Page<StockDamage> pageList(String keyword, Long warehouseId, Integer status, Integer damageCause, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockDamage> wrapper = new LambdaQueryWrapper<StockDamage>()
                .like(keyword != null, StockDamage::getDamageNo, keyword)
                .eq(warehouseId != null, StockDamage::getWarehouseId, warehouseId)
                .eq(status != null, StockDamage::getStatus, status)
                .eq(damageCause != null, StockDamage::getDamageCause, damageCause)
                .orderByDesc(StockDamage::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    private String generateNo() {
        return "DM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockDamage createDamage(StockDamage damage, List<StockDamageItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        damage.setDamageNo(generateNo());
        damage.setTenantId(1L);
        damage.setStatus(0);
        damage.setCreateBy(userId);
        damage.setCreateTime(LocalDateTime.now());
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        if (items != null) {
            for (StockDamageItem item : items) {
                item.setAmount(item.getQuantity().multiply(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO));
                totalQty = totalQty.add(item.getQuantity());
                totalAmt = totalAmt.add(item.getAmount());
                item.setCreateTime(LocalDateTime.now());
            }
        }
        damage.setTotalQuantity(totalQty);
        damage.setTotalAmount(totalAmt);
        damage.setTotalItems(items != null ? items.size() : 0);
        this.save(damage);
        if (items != null) {
            for (StockDamageItem item : items) {
                item.setDamageId(damage.getId());
                damageItemMapper.insert(item);
            }
        }
        return damage;
    }

    private StockDamage getAndCheck(Long id, int expectedStatus, String msg) {
        StockDamage d = this.getById(id);
        if (d == null) throw BusinessException.notFound("报损单不存在");
        if (d.getStatus() != expectedStatus) throw BusinessException.badRequest(msg);
        return d;
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
        StockDamage d = getAndCheck(id, 2, "只有已审核状态的报损单可以执行出库");
        d.setStatus(3);
        d.setExecutedBy(StpUtil.getLoginIdAsLong());
        d.setExecutedTime(LocalDateTime.now());
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockDamage cancel(Long id, String reason) {
        StockDamage d = this.getById(id);
        if (d == null) throw BusinessException.notFound("报损单不存在");
        if (d.getStatus() >= 3) throw BusinessException.badRequest("已执行的报损单不能取消");
        d.setStatus(5);
        this.updateById(d);
        return d;
    }

    @Override
    public List<StockDamageItem> getItems(Long damageId) {
        return damageItemMapper.selectList(
                new LambdaQueryWrapper<StockDamageItem>().eq(StockDamageItem::getDamageId, damageId));
    }
}
