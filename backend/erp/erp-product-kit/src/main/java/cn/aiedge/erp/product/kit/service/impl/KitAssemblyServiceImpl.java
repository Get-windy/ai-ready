package cn.aiedge.erp.product.kit.service.impl;

import cn.aiedge.erp.product.kit.entity.KitAssembly;
import cn.aiedge.erp.product.kit.entity.KitAssemblyItem;
import cn.aiedge.erp.product.kit.entity.ProductKit;
import cn.aiedge.erp.product.kit.entity.ProductKitItem;
import cn.aiedge.erp.product.kit.enums.AssemblyStatus;
import cn.aiedge.erp.product.kit.mapper.KitAssemblyItemMapper;
import cn.aiedge.erp.product.kit.mapper.KitAssemblyMapper;
import cn.aiedge.erp.product.kit.service.KitAssemblyService;
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
public class KitAssemblyServiceImpl extends ServiceImpl<KitAssemblyMapper, KitAssembly> implements KitAssemblyService {

    private final KitAssemblyItemMapper assemblyItemMapper;
    private final ProductKitService productKitService;

    @Override
    public KitAssembly getByAssemblyNo(String assemblyNo) {
        return lambdaQuery()
                .eq(KitAssembly::getAssemblyNo, assemblyNo)
                .eq(KitAssembly::getDeleted, 0)
                .one();
    }

    @Override
    public Page<KitAssembly> pageList(String keyword, Long kitId, Long warehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<KitAssembly> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KitAssembly::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(KitAssembly::getAssemblyNo, keyword)
                    .or().like(KitAssembly::getKitName, keyword));
        }
        if (kitId != null) {
            wrapper.eq(KitAssembly::getKitId, kitId);
        }
        if (warehouseId != null) {
            wrapper.eq(KitAssembly::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(KitAssembly::getStatus, status);
        }
        wrapper.orderByDesc(KitAssembly::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public String generateAssemblyNo() {
        String prefix = "KA";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<KitAssembly> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(KitAssembly::getAssemblyNo, prefix + dateStr)
                .eq(KitAssembly::getDeleted, 0)
                .orderByDesc(KitAssembly::getAssemblyNo)
                .last("LIMIT 1");
        KitAssembly lastAssembly = getOne(wrapper);
        int seq = 1;
        if (lastAssembly != null) {
            String lastNo = lastAssembly.getAssemblyNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssembly createAssembly(KitAssembly assembly) {
        assembly.setAssemblyNo(generateAssemblyNo());
        assembly.setStatus(AssemblyStatus.DRAFT.getCode());
        assembly.setAssemblyDate(LocalDate.now());
        assembly.setTotalCost(BigDecimal.ZERO);
        save(assembly);
        return getById(assembly.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssembly createFromKit(Long kitId, BigDecimal quantity, Long warehouseId) {
        ProductKit kit = productKitService.getById(kitId);
        if (kit == null) {
            throw new RuntimeException("套装不存在");
        }
        KitAssembly assembly = new KitAssembly();
        assembly.setKitId(kitId);
        assembly.setKitCode(kit.getKitCode());
        assembly.setKitName(kit.getKitName());
        assembly.setProductId(kit.getProductId());
        assembly.setProductCode(kit.getProductCode());
        assembly.setProductName(kit.getProductName());
        assembly.setAssemblyQuantity(quantity);
        assembly.setWarehouseId(warehouseId);
        assembly.setAssemblyDate(LocalDate.now());
        assembly.setTotalCost(BigDecimal.ZERO);
        save(assembly);
        List<ProductKitItem> kitItems = productKitService.getKitItems(kitId);
        for (int i = 0; i < kitItems.size(); i++) {
            ProductKitItem kitItem = kitItems.get(i);
            KitAssemblyItem assemblyItem = new KitAssemblyItem();
            assemblyItem.setAssemblyId(assembly.getId());
            assemblyItem.setLineNo(i + 1);
            assemblyItem.setTenantId(assembly.getTenantId());
            assemblyItem.setComponentProductId(kitItem.getComponentProductId());
            assemblyItem.setComponentProductCode(kitItem.getComponentProductCode());
            assemblyItem.setComponentProductName(kitItem.getComponentProductName());
            assemblyItem.setComponentProductSpec(kitItem.getComponentProductSpec());
            assemblyItem.setComponentProductUnit(kitItem.getComponentProductUnit());
            assemblyItem.setRequiredQuantity(kitItem.getQuantity().multiply(quantity));
            assemblyItem.setActualQuantity(BigDecimal.ZERO);
            assemblyItem.setUnitCost(kitItem.getUnitCost());
            assemblyItem.setLineCost(BigDecimal.ZERO);
            assemblyItemMapper.insert(assemblyItem);
        }
        calculateTotals(assembly.getId());
        return getById(assembly.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssembly updateAssembly(Long assemblyId, KitAssembly assembly) {
        KitAssembly existing = getById(assemblyId);
        if (existing == null) {
            throw new RuntimeException("组装单不存在");
        }
        if (existing.getStatus() != AssemblyStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的组装单可以修改");
        }
        assembly.setId(assemblyId);
        updateById(assembly);
        return getById(assemblyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssembly submitForApproval(Long assemblyId) {
        KitAssembly assembly = getById(assemblyId);
        if (assembly == null) {
            throw new RuntimeException("组装单不存在");
        }
        if (assembly.getStatus() != AssemblyStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的组装单可以提交审批");
        }
        assembly.setStatus(AssemblyStatus.PENDING_APPROVAL.getCode());
        updateById(assembly);
        return assembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssembly approve(Long assemblyId, Long approverId, String note) {
        KitAssembly assembly = getById(assemblyId);
        if (assembly == null) {
            throw new RuntimeException("组装单不存在");
        }
        if (assembly.getStatus() != AssemblyStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的组装单可以审批");
        }
        assembly.setStatus(AssemblyStatus.APPROVED.getCode());
        assembly.setApprovedBy(approverId);
        assembly.setApprovedTime(LocalDateTime.now());
        assembly.setApprovedNote(note);
        updateById(assembly);
        return assembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssembly reject(Long assemblyId, String reason) {
        KitAssembly assembly = getById(assemblyId);
        if (assembly == null) {
            throw new RuntimeException("组装单不存在");
        }
        if (assembly.getStatus() != AssemblyStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的组装单可以拒绝");
        }
        assembly.setStatus(AssemblyStatus.DRAFT.getCode());
        assembly.setRemark(reason);
        updateById(assembly);
        return assembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssembly execute(Long assemblyId, Long executorId) {
        KitAssembly assembly = getById(assemblyId);
        if (assembly == null) {
            throw new RuntimeException("组装单不存在");
        }
        if (assembly.getStatus() != AssemblyStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的组装单可以执行");
        }
        assembly.setStatus(AssemblyStatus.EXECUTING.getCode());
        assembly.setExecutedBy(executorId);
        assembly.setExecutedTime(LocalDateTime.now());
        updateById(assembly);
        return assembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssemblyItem executeItem(Long itemId, BigDecimal actualQuantity, String batchNo) {
        KitAssemblyItem item = assemblyItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("组装明细不存在");
        }
        KitAssembly assembly = getById(item.getAssemblyId());
        if (assembly.getStatus() != AssemblyStatus.EXECUTING.getCode()) {
            throw new RuntimeException("只有执行中状态的组装单可以处理明细");
        }
        item.setActualQuantity(actualQuantity);
        item.setBatchNo(batchNo);
        BigDecimal lineCost = actualQuantity.multiply(item.getUnitCost()).setScale(2, RoundingMode.HALF_UP);
        item.setLineCost(lineCost);
        assemblyItemMapper.updateById(item);
        calculateTotals(item.getAssemblyId());
        return assemblyItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssembly complete(Long assemblyId) {
        KitAssembly assembly = getById(assemblyId);
        if (assembly == null) {
            throw new RuntimeException("组装单不存在");
        }
        if (assembly.getStatus() != AssemblyStatus.EXECUTING.getCode()) {
            throw new RuntimeException("只有执行中状态的组装单可以完成");
        }
        assembly.setStatus(AssemblyStatus.COMPLETED.getCode());
        assembly.setCompletedBy(assembly.getCreateBy());
        assembly.setCompletedTime(LocalDateTime.now());
        updateById(assembly);
        updateStock(assemblyId);
        return assembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitAssembly cancel(Long assemblyId, String reason) {
        KitAssembly assembly = getById(assemblyId);
        if (assembly == null) {
            throw new RuntimeException("组装单不存在");
        }
        if (assembly.getStatus() == AssemblyStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的组装单不能取消");
        }
        assembly.setStatus(AssemblyStatus.CANCELLED.getCode());
        assembly.setRemark(reason);
        updateById(assembly);
        return assembly;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long assemblyId) {
        BigDecimal totalCost = assemblyItemMapper.sumCostByAssemblyId(assemblyId);
        KitAssembly assembly = getById(assemblyId);
        assembly.setTotalCost(totalCost != null ? totalCost : BigDecimal.ZERO);
        updateById(assembly);
    }

    @Override
    public List<KitAssemblyItem> getItems(Long assemblyId) {
        return assemblyItemMapper.selectByAssemblyId(assemblyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long assemblyId) {
        List<KitAssemblyItem> items = getItems(assemblyId);
        KitAssembly assembly = getById(assemblyId);
        for (KitAssemblyItem item : items) {
            if (item.getActualQuantity().compareTo(BigDecimal.ZERO) > 0) {
                log.info("扣减组件库存: 产品ID={}, 仓库ID={}, 数量={}", 
                        item.getComponentProductId(), assembly.getWarehouseId(), item.getActualQuantity());
            }
        }
        log.info("增加套装产品库存: 产品ID={}, 仓库ID={}, 数量={}", 
                assembly.getProductId(), assembly.getWarehouseId(), assembly.getAssemblyQuantity());
    }
}