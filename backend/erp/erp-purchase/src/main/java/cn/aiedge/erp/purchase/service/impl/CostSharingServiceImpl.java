package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.dto.CostSharingCreateRequest;
import cn.aiedge.erp.purchase.dto.CostSharingPageDTO;
import cn.aiedge.erp.purchase.entity.CostSharing;
import cn.aiedge.erp.purchase.entity.CostSharingItem;
import cn.aiedge.erp.purchase.mapper.CostSharingItemMapper;
import cn.aiedge.erp.purchase.mapper.CostSharingMapper;
import cn.aiedge.erp.purchase.service.CostSharingService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 采购费用分摊单服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CostSharingServiceImpl extends ServiceImpl<CostSharingMapper, CostSharing>
        implements CostSharingService {

    private static final Logger logger = LoggerFactory.getLogger(CostSharingServiceImpl.class);

    private final CostSharingItemMapper costSharingItemMapper;
    private final cn.aiedge.erp.purchase.inbound.mapper.PurchaseInboundItemMapper inboundItemMapper;

    @Override
    public Page<CostSharingPageDTO> pageList(int pageNum, int pageSize, String sharingNo,
                                              String supplierName, String startDate, String endDate) {
        // Build query for main entity
        LambdaQueryWrapper<CostSharing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostSharing::getDeleted, 0)
                .like(sharingNo != null && !sharingNo.isEmpty(), CostSharing::getSharingNo, sharingNo)
                .like(supplierName != null && !supplierName.isEmpty(), CostSharing::getSupplierName, supplierName);

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(CostSharing::getSharingDate, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(CostSharing::getSharingDate, endDate + " 23:59:59");
        }

        wrapper.orderByDesc(CostSharing::getCreateTime);

        Page<CostSharing> entityPage = page(new Page<>(pageNum, pageSize), wrapper);

        // Convert to DTO
        Page<CostSharingPageDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CostSharingPageDTO> dtoList = entityPage.getRecords().stream().map(entity -> {
            CostSharingPageDTO dto = new CostSharingPageDTO();
            dto.setId(entity.getId());
            dto.setSharingNo(entity.getSharingNo());
            dto.setSharingDate(entity.getSharingDate());
            dto.setSupplierName(entity.getSupplierName());
            dto.setExpenseType(entity.getExpenseType());
            dto.setAllocationMethod(entity.getAllocationMethod());
            dto.setStatus(entity.getStatus() != null ? entity.getStatus().toString() : null);
            dto.setTotalAmount(entity.getTotalAmount());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCostSharing(CostSharingCreateRequest request) {
        if (request.getExpenseItems() == null || request.getExpenseItems().isEmpty()) {
            throw BusinessException.badRequest("费用项不能为空");
        }
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw BusinessException.badRequest("分摊明细不能为空");
        }

        // Build main entity
        CostSharing sharing = new CostSharing();
        sharing.setSharingNo(generateSharingNo());
        sharing.setSharingDate(LocalDateTime.now());
        sharing.setAllocationMethod(request.getSharingMethod());
        sharing.setStatus(0);
        sharing.setCreateTime(LocalDateTime.now());
        sharing.setUpdateTime(LocalDateTime.now());

        try {
            sharing.setCreateBy(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            sharing.setCreateBy(null);
        }

        // Calculate total amount from expense items
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CostSharingCreateRequest.ExpenseItemDTO expense : request.getExpenseItems()) {
            if (expense.getAmount() != null) {
                totalAmount = totalAmount.add(expense.getAmount());
            }
        }
        sharing.setTotalAmount(totalAmount);

        // Take expenseType from first expense item
        if (!request.getExpenseItems().isEmpty()) {
            sharing.setExpenseType(request.getExpenseItems().get(0).getExpenseType());
        }

        // Take supplierName from first detail if available
        if (!request.getDetails().isEmpty()) {
            // supplierName is not directly in details, we could look it up from inbound order
            // For now, leave it blank - the list will show it when we enrich
        }

        // Save main entity
        save(sharing);
        Long sharingId = sharing.getId();

        // Save allocation details as CostSharingItem records
        for (CostSharingCreateRequest.AllocationDetailDTO detail : request.getDetails()) {
            CostSharingItem item = new CostSharingItem();
            item.setCostSharingId(sharingId);
            item.setInboundOrderId(detail.getInboundId());
            item.setQuantity(detail.getQuantity());
            item.setAmount(detail.getAmount());
            item.setAllocatedCost(detail.getSharedAmount());
            // weight/volume not available from request, leave null
            costSharingItemMapper.insert(item);
        }

        logger.info("创建采购费用分摊单成功: sharingId={}, sharingNo={}", sharingId, sharing.getSharingNo());
        return sharingId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        CostSharing sharing = getById(id);
        if (sharing == null) {
            throw BusinessException.notFound("分摊单不存在");
        }
        if (sharing.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的分摊单才能完成");
        }

        sharing.setStatus(1);
        sharing.setUpdateTime(LocalDateTime.now());
        try {
            sharing.setUpdateBy(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            sharing.setUpdateBy(null);
        }
        updateById(sharing);

        // 回写分摊成本到入库单明细行
        // 注：CostSharingItem.inboundOrderId 存的是入库单头ID (erp_purchase_inbound.id)，
        // 需通过 inbound_id 查询该单所有明细行，按数量比例分摊
        try {
            List<CostSharingItem> items = costSharingItemMapper.selectList(
                new LambdaQueryWrapper<CostSharingItem>()
                    .eq(CostSharingItem::getCostSharingId, id)
            );
            int updatedCount = 0;
            for (CostSharingItem item : items) {
                if (item.getInboundOrderId() != null && item.getAllocatedCost() != null
                        && item.getAllocatedCost().compareTo(BigDecimal.ZERO) > 0) {
                    // 查出入库单所有明细行
                    List<cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem> inboundItems =
                        inboundItemMapper.selectByInboundId(item.getInboundOrderId());
                    if (inboundItems.isEmpty()) {
                        logger.warn("未找到入库单明细: inboundId={}", item.getInboundOrderId());
                        continue;
                    }
                    // 按数量比例分摊费用到每行
                    BigDecimal totalQty = BigDecimal.ZERO;
                    for (cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem pi : inboundItems) {
                        if (pi.getInboundQuantity() != null) {
                            totalQty = totalQty.add(pi.getInboundQuantity());
                        }
                    }
                    if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
                        logger.warn("入库单明细数量为零，跳过分摊: inboundId={}", item.getInboundOrderId());
                        continue;
                    }
                    // 计算每个入库单明细行的单位成本增加额
                    // 说明：updateInboundItemCost 的 SQL 为 SET unit_cost = unit_cost + #{allocatedCost}
                    // 因此传入的应是单位成本增加额 = 总分摊费用 / 入库总数量
                    BigDecimal perUnitCost = item.getAllocatedCost()
                        .divide(totalQty, 4, java.math.RoundingMode.HALF_UP);
                    for (cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem pi : inboundItems) {
                        if (pi.getInboundQuantity() != null && pi.getInboundQuantity().compareTo(BigDecimal.ZERO) > 0) {
                            inboundItemMapper.updateInboundItemCost(pi.getId(), perUnitCost);
                            updatedCount++;
                        }
                    }
                }
            }
            logger.info("费用分摊成本回写完成, 单据ID={}, 更新明细行数={}", id, updatedCount);
        } catch (Exception e) {
            logger.error("费用分摊成本回写失败: {}", e.getMessage(), e);
        }

        logger.info("完成采购费用分摊单: sharingId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        CostSharing sharing = getById(id);
        if (sharing == null) {
            throw BusinessException.notFound("分摊单不存在");
        }
        if (sharing.getStatus() == 2) {
            throw BusinessException.badRequest("分摊单已取消");
        }

        sharing.setStatus(2);
        sharing.setUpdateTime(LocalDateTime.now());
        try {
            sharing.setUpdateBy(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            sharing.setUpdateBy(null);
        }
        updateById(sharing);

        logger.info("取消采购费用分摊单: sharingId={}", id);
    }

    /**
     * 生成分摊单号（格式：FY+yyyyMMdd+0001）
     */
    private String generateSharingNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "FY" + dateStr;
        LambdaQueryWrapper<CostSharing> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CostSharing::getSharingNo, prefix)
               .orderByDesc(CostSharing::getSharingNo)
               .last("LIMIT 1");
        CostSharing last = getOne(wrapper);

        int seq = 1;
        if (last != null && last.getSharingNo() != null) {
            String seqStr = last.getSharingNo().substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }
}
