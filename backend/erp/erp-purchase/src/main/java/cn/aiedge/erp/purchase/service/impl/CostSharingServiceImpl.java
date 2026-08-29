package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.dto.CostSharingCreateRequest;
import cn.aiedge.erp.purchase.dto.CostSharingDetailDTO;
import cn.aiedge.erp.purchase.dto.CostSharingPageDTO;
import cn.aiedge.erp.purchase.entity.CostSharing;
import cn.aiedge.erp.purchase.entity.CostSharingExpenseItem;
import cn.aiedge.erp.purchase.entity.CostSharingItem;
import cn.aiedge.erp.purchase.mapper.CostSharingExpenseItemMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private final CostSharingExpenseItemMapper costSharingExpenseItemMapper;
    private final cn.aiedge.erp.purchase.inbound.mapper.PurchaseInboundItemMapper inboundItemMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<CostSharingPageDTO> pageList(int pageNum, int pageSize, String sharingNo,
                                              String handlerName, String departmentName,
                                              String createByName, String bookkeeperName,
                                              String summary, String remark, Integer status,
                                              String startDate, String endDate) {
        LambdaQueryWrapper<CostSharing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostSharing::getDeleted, 0)
                .like(sharingNo != null && !sharingNo.isEmpty(), CostSharing::getSharingNo, sharingNo)
                .like(handlerName != null && !handlerName.isEmpty(), CostSharing::getHandlerName, handlerName)
                .like(departmentName != null && !departmentName.isEmpty(), CostSharing::getDepartmentName, departmentName)
                .like(createByName != null && !createByName.isEmpty(), CostSharing::getCreateByName, createByName)
                .like(bookkeeperName != null && !bookkeeperName.isEmpty(), CostSharing::getBookkeeperName, bookkeeperName)
                .like(summary != null && !summary.isEmpty(), CostSharing::getSummary, summary)
                .like(remark != null && !remark.isEmpty(), CostSharing::getRemark, remark)
                .eq(status != null, CostSharing::getStatus, status);

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(CostSharing::getSharingDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(CostSharing::getSharingDate, LocalDate.parse(endDate));
        }

        wrapper.orderByDesc(CostSharing::getCreateTime);

        Page<CostSharing> entityPage = page(new Page<>(pageNum, pageSize), wrapper);

        Page<CostSharingPageDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CostSharingPageDTO> dtoList = entityPage.getRecords().stream().map(entity -> {
            CostSharingPageDTO dto = new CostSharingPageDTO();
            dto.setId(entity.getId());
            dto.setSharingNo(entity.getSharingNo());
            dto.setSharingDate(entity.getSharingDate());
            dto.setStatus(entity.getStatus() != null ? entity.getStatus().toString() : null);
            dto.setAccountTime(entity.getAccountTime());
            dto.setCreateTime(entity.getCreateTime());
            dto.setTotalAmount(entity.getTotalAmount());
            dto.setHandlerName(entity.getHandlerName());
            dto.setDepartmentName(entity.getDepartmentName());
            dto.setCreateByName(entity.getCreateByName());
            dto.setBookkeeperName(entity.getBookkeeperName());
            dto.setSummary(entity.getSummary());
            dto.setRemark(entity.getRemark());
            dto.setAttachment(entity.getAttachment());
            dto.setAllocationMethod(entity.getAllocationMethod());
            dto.setExpenseType(entity.getExpenseType());
            dto.setSupplierName(entity.getSupplierName());
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
            throw BusinessException.badRequest("费用单明细不能为空");
        }
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw BusinessException.badRequest("采购入库单分摊明细不能为空");
        }

        // ── 保存主表 ──
        CostSharing sharing = new CostSharing();
        sharing.setSharingNo(generateSharingNo());
        sharing.setSharingDate(request.getSharingDate() != null ? request.getSharingDate() : LocalDate.now());
        sharing.setAllocationMethod(request.getSharingMethod());
        sharing.setHandlerId(request.getHandlerId());
        sharing.setHandlerName(request.getHandlerName());
        sharing.setDepartmentId(request.getDepartmentId());
        sharing.setDepartmentName(request.getDepartmentName());
        sharing.setSummary(request.getSummary());
        sharing.setRemark(request.getRemark());
        sharing.setCreateByName(request.getCreateByName());
        sharing.setStatus(0);
        sharing.setCreateTime(LocalDateTime.now());
        sharing.setUpdateTime(LocalDateTime.now());

        try {
            sharing.setCreateBy(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            sharing.setCreateBy(null);
        }

        // 合计金额 = 费用单明细金额之和
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CostSharingCreateRequest.ExpenseItemDTO expense : request.getExpenseItems()) {
            if (expense.getExpenseAmount() != null) {
                totalAmount = totalAmount.add(expense.getExpenseAmount());
            }
        }
        sharing.setTotalAmount(totalAmount);

        // 冗余取第一个费用单项作为费用类型
        if (!request.getExpenseItems().isEmpty()) {
            sharing.setExpenseType(request.getExpenseItems().get(0).getExpenseType());
        }

        save(sharing);
        Long sharingId = sharing.getId();

        // ── 保存费用单明细 ──
        for (CostSharingCreateRequest.ExpenseItemDTO expense : request.getExpenseItems()) {
            CostSharingExpenseItem item = new CostSharingExpenseItem();
            item.setCostSharingId(sharingId);
            item.setExpenseNo(expense.getExpenseNo());
            item.setPartnerId(expense.getPartnerId());
            item.setPartnerName(expense.getPartnerName());
            item.setPartnerCode(expense.getPartnerCode());
            item.setSettleUnitId(expense.getSettleUnitId());
            item.setSettleUnit(expense.getSettleUnit());
            item.setExpenseType(expense.getExpenseType());
            item.setExpenseAmount(expense.getExpenseAmount());
            item.setRemark(expense.getRemark());
            costSharingExpenseItemMapper.insert(item);
        }

        // ── 保存采购入库单分摊明细 ──
        for (CostSharingCreateRequest.AllocationDetailDTO detail : request.getDetails()) {
            CostSharingItem item = new CostSharingItem();
            item.setCostSharingId(sharingId);
            item.setInboundOrderId(detail.getInboundId());
            item.setInboundNo(detail.getInboundNo());
            item.setSupplierId(detail.getSupplierId());
            item.setSupplierName(detail.getSupplierName());
            item.setSupplierCode(detail.getSupplierCode());
            item.setSettleUnitId(detail.getSettleUnitId());
            item.setSettleUnit(detail.getSettleUnit());
            item.setProductId(detail.getProductId());
            item.setProductName(detail.getProductName());
            item.setPricingUnit(detail.getPricingUnit());
            item.setQuantity(detail.getQuantity());
            item.setDiscountedUnitPrice(detail.getDiscountedUnitPrice());
            item.setDiscountedAmount(detail.getDiscountedAmount());
            item.setAllocatedCost(detail.getAllocatedCost());
            costSharingItemMapper.insert(item);
        }

        logger.info("创建采购费用分摊单成功: sharingId={}, sharingNo={}", sharingId, sharing.getSharingNo());
        return sharingId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCostSharing(Long id, CostSharingCreateRequest request) {
        CostSharing sharing = getById(id);
        if (sharing == null) {
            throw BusinessException.notFound("分摊单不存在");
        }
        if (sharing.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的分摊单才能修改");
        }
        if (request.getExpenseItems() == null || request.getExpenseItems().isEmpty()) {
            throw BusinessException.badRequest("费用单明细不能为空");
        }
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw BusinessException.badRequest("采购入库单分摊明细不能为空");
        }

        // 更新主表头部字段（不重编号、不改制单信息）
        sharing.setSharingDate(request.getSharingDate() != null ? request.getSharingDate() : sharing.getSharingDate());
        sharing.setAllocationMethod(request.getSharingMethod());
        sharing.setHandlerId(request.getHandlerId());
        sharing.setHandlerName(request.getHandlerName());
        sharing.setDepartmentId(request.getDepartmentId());
        sharing.setDepartmentName(request.getDepartmentName());
        sharing.setSummary(request.getSummary());
        sharing.setRemark(request.getRemark());
        sharing.setUpdateTime(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CostSharingCreateRequest.ExpenseItemDTO expense : request.getExpenseItems()) {
            if (expense.getExpenseAmount() != null) {
                totalAmount = totalAmount.add(expense.getExpenseAmount());
            }
        }
        sharing.setTotalAmount(totalAmount);
        if (!request.getExpenseItems().isEmpty()) {
            sharing.setExpenseType(request.getExpenseItems().get(0).getExpenseType());
        }
        updateById(sharing);

        // 替换明细：先删旧，再插新
        costSharingExpenseItemMapper.delete(
                new LambdaQueryWrapper<CostSharingExpenseItem>().eq(CostSharingExpenseItem::getCostSharingId, id));
        costSharingItemMapper.delete(
                new LambdaQueryWrapper<CostSharingItem>().eq(CostSharingItem::getCostSharingId, id));

        for (CostSharingCreateRequest.ExpenseItemDTO expense : request.getExpenseItems()) {
            CostSharingExpenseItem item = new CostSharingExpenseItem();
            item.setCostSharingId(id);
            item.setExpenseNo(expense.getExpenseNo());
            item.setPartnerId(expense.getPartnerId());
            item.setPartnerName(expense.getPartnerName());
            item.setPartnerCode(expense.getPartnerCode());
            item.setSettleUnitId(expense.getSettleUnitId());
            item.setSettleUnit(expense.getSettleUnit());
            item.setExpenseType(expense.getExpenseType());
            item.setExpenseAmount(expense.getExpenseAmount());
            item.setRemark(expense.getRemark());
            costSharingExpenseItemMapper.insert(item);
        }
        for (CostSharingCreateRequest.AllocationDetailDTO detail : request.getDetails()) {
            CostSharingItem item = new CostSharingItem();
            item.setCostSharingId(id);
            item.setInboundOrderId(detail.getInboundId());
            item.setInboundNo(detail.getInboundNo());
            item.setSupplierId(detail.getSupplierId());
            item.setSupplierName(detail.getSupplierName());
            item.setSupplierCode(detail.getSupplierCode());
            item.setSettleUnitId(detail.getSettleUnitId());
            item.setSettleUnit(detail.getSettleUnit());
            item.setProductId(detail.getProductId());
            item.setProductName(detail.getProductName());
            item.setPricingUnit(detail.getPricingUnit());
            item.setQuantity(detail.getQuantity());
            item.setDiscountedUnitPrice(detail.getDiscountedUnitPrice());
            item.setDiscountedAmount(detail.getDiscountedAmount());
            item.setAllocatedCost(detail.getAllocatedCost());
            costSharingItemMapper.insert(item);
        }

        logger.info("更新采购费用分摊单成功: sharingId={}", id);
    }

    @Override
    public CostSharingDetailDTO getDetail(Long id) {
        CostSharing sharing = getById(id);
        if (sharing == null) {
            throw BusinessException.notFound("分摊单不存在");
        }

        CostSharingDetailDTO dto = new CostSharingDetailDTO();
        dto.setSharing(sharing);
        dto.setExpenseItems(costSharingExpenseItemMapper.selectList(
                new LambdaQueryWrapper<CostSharingExpenseItem>()
                        .eq(CostSharingExpenseItem::getCostSharingId, id)
                        .orderByAsc(CostSharingExpenseItem::getId)));
        dto.setItems(costSharingItemMapper.selectList(
                new LambdaQueryWrapper<CostSharingItem>()
                        .eq(CostSharingItem::getCostSharingId, id)
                        .orderByAsc(CostSharingItem::getId)));
        return dto;
    }

    @Override
    public String nextNo() {
        return generateSharingNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        CostSharing sharing = getById(id);
        if (sharing == null) {
            throw BusinessException.notFound("分摊单不存在");
        }
        if (sharing.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的分摊单才能记账");
        }

        // 先回写分摊成本到入库单明细行
        try {
            List<CostSharingItem> items = costSharingItemMapper.selectList(
                    new LambdaQueryWrapper<CostSharingItem>()
                            .eq(CostSharingItem::getCostSharingId, id)
            );
            int updatedCount = 0;
            for (CostSharingItem item : items) {
                if (item.getInboundOrderId() == null || item.getAllocatedCost() == null
                        || item.getAllocatedCost().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                // 按商品ID匹配该入库单对应明细行；无商品ID则按整单数量分摊
                List<cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem> inboundItems;
                if (item.getProductId() != null) {
                    inboundItems = inboundItemMapper.selectList(
                            new LambdaQueryWrapper<cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem>()
                                    .eq(cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem::getInboundId, item.getInboundOrderId())
                                    .eq(cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem::getProductId, item.getProductId()));
                } else {
                    inboundItems = inboundItemMapper.selectByInboundId(item.getInboundOrderId());
                }
                if (inboundItems.isEmpty()) {
                    logger.warn("未找到入库单明细: inboundId={}, productId={}", item.getInboundOrderId(), item.getProductId());
                    continue;
                }
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
                BigDecimal perUnitCost = item.getAllocatedCost()
                        .divide(totalQty, 4, java.math.RoundingMode.HALF_UP);
                for (cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem pi : inboundItems) {
                    if (pi.getInboundQuantity() != null && pi.getInboundQuantity().compareTo(BigDecimal.ZERO) > 0) {
                        inboundItemMapper.updateInboundItemCost(pi.getId(), perUnitCost);
                        updatedCount++;
                    }
                }
            }
            logger.info("费用分摊成本回写完成, 单据ID={}, 更新明细行数={}", id, updatedCount);
        } catch (Exception e) {
            logger.error("费用分摊成本回写失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "成本回写失败: " + e.getMessage());
        }

        // 记账
        sharing.setStatus(1);
        sharing.setAccountTime(LocalDateTime.now());
        sharing.setUpdateTime(LocalDateTime.now());
        try {
            Long loginId = StpUtil.getLoginIdAsLong();
            sharing.setBookkeeperId(loginId);
        } catch (Exception e) {
            sharing.setBookkeeperId(null);
        }
        updateById(sharing);

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

        if (redisTemplate != null) {
            String key = "sharing:no:seq:" + dateStr;
            Long seq = redisTemplate.opsForValue().increment(key);
            if (seq != null && seq == 1) {
                redisTemplate.expire(key, 2, TimeUnit.DAYS);
            }
            return prefix + String.format("%04d", seq);
        }

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
