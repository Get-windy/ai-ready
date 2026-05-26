package cn.aiedge.erp.product.kit.service.impl;

import cn.aiedge.erp.product.kit.entity.KitDisassembly;
import cn.aiedge.erp.product.kit.entity.KitDisassemblyItem;
import cn.aiedge.erp.product.kit.entity.ProductKit;
import cn.aiedge.erp.product.kit.entity.ProductKitItem;
import cn.aiedge.erp.product.kit.enums.DisassemblyStatus;
import cn.aiedge.erp.product.kit.mapper.KitDisassemblyItemMapper;
import cn.aiedge.erp.product.kit.mapper.KitDisassemblyMapper;
import cn.aiedge.erp.product.kit.service.KitDisassemblyService;
import cn.aiedge.erp.product.kit.service.ProductKitService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KitDisassemblyServiceImpl extends ServiceImpl<KitDisassemblyMapper, KitDisassembly> implements KitDisassemblyService {

    private final KitDisassemblyItemMapper disassemblyItemMapper;
    private final ProductKitService productKitService;

    @Override
    public KitDisassembly getByDisassemblyNo(String disassemblyNo) {
        return lambdaQuery()
                .eq(KitDisassembly::getDisassemblyNo, disassemblyNo)
                .eq(KitDisassembly::getDeleted, 0)
                .one();
    }

    @Override
    public Page<KitDisassembly> pageList(String keyword, Long kitId, Long warehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<KitDisassembly> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KitDisassembly::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(KitDisassembly::getDisassemblyNo, keyword)
                    .or().like(KitDisassembly::getKitName, keyword));
        }
        if (kitId != null) {
            wrapper.eq(KitDisassembly::getKitId, kitId);
        }
        if (warehouseId != null) {
            wrapper.eq(KitDisassembly::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(KitDisassembly::getStatus, status);
        }
        wrapper.orderByDesc(KitDisassembly::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public String generateDisassemblyNo() {
        String prefix = "KD";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<KitDisassembly> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(KitDisassembly::getDisassemblyNo, prefix + dateStr)
                .eq(KitDisassembly::getDeleted, 0)
                .orderByDesc(KitDisassembly::getDisassemblyNo)
                .last("LIMIT 1");
        KitDisassembly lastDisassembly = getOne(wrapper);
        int seq = 1;
        if (lastDisassembly != null) {
            String lastNo = lastDisassembly.getDisassemblyNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassembly createDisassembly(KitDisassembly disassembly) {
        disassembly.setDisassemblyNo(generateDisassemblyNo());
        disassembly.setStatus(DisassemblyStatus.DRAFT.getCode());
        disassembly.setDisassemblyDate(LocalDate.now());
        disassembly.setTotalCost(BigDecimal.ZERO);
        save(disassembly);
        return getById(disassembly.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassembly createFromKit(Long kitId, BigDecimal quantity, Long warehouseId, String batchNo) {
        ProductKit kit = productKitService.getById(kitId);
        if (kit == null) {
            throw new RuntimeException("套装不存在");
        }
        KitDisassembly disassembly = new KitDisassembly();
        disassembly.setKitId(kitId);
        disassembly.setKitCode(kit.getKitCode());
        disassembly.setKitName(kit.getKitName());
        disassembly.setProductId(kit.getProductId());
        disassembly.setProductCode(kit.getProductCode());
        disassembly.setProductName(kit.getProductName());
        disassembly.setBatchNo(batchNo);
        disassembly.setDisassemblyQuantity(quantity);
        disassembly.setWarehouseId(warehouseId);
        disassembly.setDisassemblyDate(LocalDate.now());
        disassembly.setTotalCost(BigDecimal.ZERO);
        save(disassembly);
        List<ProductKitItem> kitItems = productKitService.getKitItems(kitId);
        for (int i = 0; i < kitItems.size(); i++) {
            ProductKitItem kitItem = kitItems.get(i);
            KitDisassemblyItem disassemblyItem = new KitDisassemblyItem();
            disassemblyItem.setDisassemblyId(disassembly.getId());
            disassemblyItem.setLineNo(i + 1);
            disassemblyItem.setTenantId(disassembly.getTenantId());
            disassemblyItem.setComponentProductId(kitItem.getComponentProductId());
            disassemblyItem.setComponentProductCode(kitItem.getComponentProductCode());
            disassemblyItem.setComponentProductName(kitItem.getComponentProductName());
            disassemblyItem.setComponentProductSpec(kitItem.getComponentProductSpec());
            disassemblyItem.setComponentProductUnit(kitItem.getComponentProductUnit());
            disassemblyItem.setExpectedQuantity(kitItem.getQuantity().multiply(quantity));
            disassemblyItem.setActualQuantity(BigDecimal.ZERO);
            disassemblyItem.setUnitCost(kitItem.getUnitCost());
            disassemblyItem.setLineCost(BigDecimal.ZERO);
            disassemblyItemMapper.insert(disassemblyItem);
        }
        calculateTotals(disassembly.getId());
        return getById(disassembly.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassembly updateDisassembly(Long disassemblyId, KitDisassembly disassembly) {
        KitDisassembly existing = getById(disassemblyId);
        if (existing == null) {
            throw new RuntimeException("拆分单不存在");
        }
        if (existing.getStatus() != DisassemblyStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的拆分单可以修改");
        }
        disassembly.setId(disassemblyId);
        updateById(disassembly);
        return getById(disassemblyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassembly submitForApproval(Long disassemblyId) {
        KitDisassembly disassembly = getById(disassemblyId);
        if (disassembly == null) {
            throw new RuntimeException("拆分单不存在");
        }
        if (disassembly.getStatus() != DisassemblyStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的拆分单可以提交审批");
        }
        disassembly.setStatus(DisassemblyStatus.PENDING_APPROVAL.getCode());
        updateById(disassembly);
        return disassembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassembly approve(Long disassemblyId, Long approverId, String note) {
        KitDisassembly disassembly = getById(disassemblyId);
        if (disassembly == null) {
            throw new RuntimeException("拆分单不存在");
        }
        if (disassembly.getStatus() != DisassemblyStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的拆分单可以审批");
        }
        disassembly.setStatus(DisassemblyStatus.APPROVED.getCode());
        disassembly.setApprovedBy(approverId);
        disassembly.setApprovedTime(LocalDateTime.now());
        disassembly.setApprovedNote(note);
        updateById(disassembly);
        return disassembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassembly reject(Long disassemblyId, String reason) {
        KitDisassembly disassembly = getById(disassemblyId);
        if (disassembly == null) {
            throw new RuntimeException("拆分单不存在");
        }
        if (disassembly.getStatus() != DisassemblyStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的拆分单可以拒绝");
        }
        disassembly.setStatus(DisassemblyStatus.DRAFT.getCode());
        disassembly.setRemark(reason);
        updateById(disassembly);
        return disassembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassembly execute(Long disassemblyId, Long executorId) {
        KitDisassembly disassembly = getById(disassemblyId);
        if (disassembly == null) {
            throw new RuntimeException("拆分单不存在");
        }
        if (disassembly.getStatus() != DisassemblyStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的拆分单可以执行");
        }
        disassembly.setStatus(DisassemblyStatus.EXECUTING.getCode());
        disassembly.setExecutedBy(executorId);
        disassembly.setExecutedTime(LocalDateTime.now());
        updateById(disassembly);
        return disassembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassemblyItem executeItem(Long itemId, BigDecimal actualQuantity, String batchNo) {
        KitDisassemblyItem item = disassemblyItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("拆分明细不存在");
        }
        KitDisassembly disassembly = getById(item.getDisassemblyId());
        if (disassembly.getStatus() != DisassemblyStatus.EXECUTING.getCode()) {
            throw new RuntimeException("只有执行中状态的拆分单可以处理明细");
        }
        item.setActualQuantity(actualQuantity);
        item.setBatchNo(batchNo);
        BigDecimal lineCost = actualQuantity.multiply(item.getUnitCost()).setScale(2, RoundingMode.HALF_UP);
        item.setLineCost(lineCost);
        disassemblyItemMapper.updateById(item);
        calculateTotals(item.getDisassemblyId());
        return disassemblyItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassembly complete(Long disassemblyId) {
        KitDisassembly disassembly = getById(disassemblyId);
        if (disassembly == null) {
            throw new RuntimeException("拆分单不存在");
        }
        if (disassembly.getStatus() != DisassemblyStatus.EXECUTING.getCode()) {
            throw new RuntimeException("只有执行中状态的拆分单可以完成");
        }
        disassembly.setStatus(DisassemblyStatus.COMPLETED.getCode());
        disassembly.setCompletedBy(disassembly.getCreateBy());
        disassembly.setCompletedTime(LocalDateTime.now());
        updateById(disassembly);
        updateStock(disassemblyId);
        return disassembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitDisassembly cancel(Long disassemblyId, String reason) {
        KitDisassembly disassembly = getById(disassemblyId);
        if (disassembly == null) {
            throw new RuntimeException("拆分单不存在");
        }
        if (disassembly.getStatus() == DisassemblyStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的拆分单不能取消");
        }
        disassembly.setStatus(DisassemblyStatus.CANCELLED.getCode());
        disassembly.setRemark(reason);
        updateById(disassembly);
        return disassembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long disassemblyId) {
        BigDecimal totalCost = disassemblyItemMapper.sumCostByDisassemblyId(disassemblyId);
        KitDisassembly disassembly = getById(disassemblyId);
        disassembly.setTotalCost(totalCost != null ? totalCost : BigDecimal.ZERO);
        updateById(disassembly);
    }

    @Override
    public List<KitDisassemblyItem> getItems(Long disassemblyId) {
        return disassemblyItemMapper.selectByDisassemblyId(disassemblyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long disassemblyId) {
        List<KitDisassemblyItem> items = getItems(disassemblyId);
        KitDisassembly disassembly = getById(disassemblyId);
        log.info("扣减套装产品库存: 产品ID={}, 仓库ID={}, 数量={}", 
                disassembly.getProductId(), disassembly.getWarehouseId(), disassembly.getDisassemblyQuantity());
        for (KitDisassemblyItem item : items) {
            if (item.getActualQuantity().compareTo(BigDecimal.ZERO) > 0) {
                log.info("增加组件产品库存: 产品ID={}, 仓库ID={}, 数量={}", 
                        item.getComponentProductId(), disassembly.getWarehouseId(), item.getActualQuantity());
            }
        }
    }
}