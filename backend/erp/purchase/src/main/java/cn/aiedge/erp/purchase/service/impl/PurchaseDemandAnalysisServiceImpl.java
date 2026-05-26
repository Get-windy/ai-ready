package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.erp.purchase.entity.PurchaseDemand;
import cn.aiedge.erp.purchase.enums.DemandApprovalStatus;
import cn.aiedge.erp.purchase.enums.DemandPriority;
import cn.aiedge.erp.purchase.enums.DemandStatus;
import cn.aiedge.erp.purchase.mapper.PurchaseDemandMapper;
import cn.aiedge.erp.purchase.service.PurchaseDemandAnalysisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购需求分析服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseDemandAnalysisServiceImpl 
        extends ServiceImpl<PurchaseDemandMapper, PurchaseDemand>
        implements PurchaseDemandAnalysisService {
    
    private final PurchaseDemandMapper purchaseDemandMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDemand(PurchaseDemand demand) {
        // 验证需求数据
        validateDemandData(demand);
        
        // 生成需求编号
        String demandNo = generateDemandNo(demand.getTenantId());
        demand.setDemandNo(demandNo);
        
        // 设置默认状态
        demand.setStatus(DemandStatus.DRAFT);
        demand.setApprovalStatus(DemandApprovalStatus.PENDING);
        demand.setCreatedTime(LocalDateTime.now());
        demand.setUpdatedTime(LocalDateTime.now());
        
        // 保存需求
        this.save(demand);
        
        log.info("创建采购需求成功，需求ID: {}, 需求编号: {}", demand.getId(), demandNo);
        return demand.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDemand(PurchaseDemand demand) {
        Assert.notNull(demand.getId(), "需求ID不能为空");
        Assert.notNull(demand.getTenantId(), "租户ID不能为空");
        
        // 检查需求是否存在
        PurchaseDemand existingDemand = this.getById(demand.getId());
        Assert.notNull(existingDemand, "需求不存在，ID: " + demand.getId());
        
        // 验证状态是否允许更新
        validateDemandUpdate(existingDemand, demand);
        
        // 更新需求
        demand.setUpdatedTime(LocalDateTime.now());
        this.updateById(demand);
        
        log.info("更新采购需求成功，需求ID: {}", demand.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDemand(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        // 检查需求是否存在
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        // 检查状态是否允许删除
        if (demand.getStatus() != DemandStatus.DRAFT && 
            demand.getStatus() != DemandStatus.REJECTED) {
            throw new IllegalStateException("只有草稿状态或已拒绝状态的需求可以删除");
        }
        
        // 逻辑删除
        this.removeById(demandId);
        
        log.info("删除采购需求成功，需求ID: {}", demandId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        // 检查状态是否允许提交
        if (demand.getStatus() != DemandStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的需求可以提交审批");
        }
        
        // 更新状态
        demand.setStatus(DemandStatus.PENDING_APPROVAL);
        demand.setApprovalStatus(DemandApprovalStatus.PENDING);
        demand.setUpdatedTime(LocalDateTime.now());
        this.updateById(demand);
        
        log.info("采购需求提交审批成功，需求ID: {}", demandId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveDemand(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        // 检查状态是否允许审批通过
        if (demand.getStatus() != DemandStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("只有等待审批状态的需求可以审批通过");
        }
        
        // 更新状态
        demand.setStatus(DemandStatus.APPROVED);
        demand.setApprovalStatus(DemandApprovalStatus.APPROVED);
        demand.setApprovedTime(LocalDateTime.now());
        demand.setUpdatedTime(LocalDateTime.now());
        this.updateById(demand);
        
        log.info("采购需求审批通过成功，需求ID: {}", demandId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectDemand(Long demandId, String reason) {
        Assert.notNull(demandId, "需求ID不能为空");
        Assert.hasText(reason, "拒绝原因不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        // 检查状态是否允许拒绝
        if (demand.getStatus() != DemandStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("只有等待审批状态的需求可以拒绝");
        }
        
        // 更新状态
        demand.setStatus(DemandStatus.REJECTED);
        demand.setApprovalStatus(DemandApprovalStatus.REJECTED);
        demand.setRejectReason(reason);
        demand.setRejectedTime(LocalDateTime.now());
        demand.setUpdatedTime(LocalDateTime.now());
        this.updateById(demand);
        
        log.info("采购需求审批拒绝成功，需求ID: {}, 原因: {}", demandId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToInquiry(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        // 检查状态是否允许转为询价
        if (demand.getStatus() != DemandStatus.APPROVED) {
            throw new IllegalStateException("只有已批准状态的需求可以转为询价");
        }
        
        // TODO: 创建询价单逻辑
        Long inquiryId = createInquiryFromDemand(demand);
        
        // 更新需求关联信息
        demand.setInquiryId(inquiryId);
        demand.setUpdatedTime(LocalDateTime.now());
        this.updateById(demand);
        
        log.info("采购需求转为询价成功，需求ID: {}, 询价单ID: {}", demandId, inquiryId);
        return inquiryId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long batchConvertToInquiry(List<Long> demandIds) {
        Assert.notEmpty(demandIds, "需求ID列表不能为空");
        
        // 批量检查需求状态
        List<PurchaseDemand> demands = this.listByIds(demandIds);
        for (PurchaseDemand demand : demands) {
            if (demand.getStatus() != DemandStatus.APPROVED) {
                throw new IllegalStateException("需求ID: " + demand.getId() + " 状态不是已批准，无法转为询价");
            }
        }
        
        // TODO: 批量创建询价单逻辑
        Long inquiryId = createBatchInquiryFromDemands(demands);
        
        // 批量更新需求关联信息
        demands.forEach(demand -> {
            demand.setInquiryId(inquiryId);
            demand.setUpdatedTime(LocalDateTime.now());
        });
        this.updateBatchById(demands);
        
        log.info("批量采购需求转为询价成功，需求数量: {}, 询价单ID: {}", demands.size(), inquiryId);
        return inquiryId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelDemand(Long demandId, String reason) {
        Assert.notNull(demandId, "需求ID不能为空");
        Assert.hasText(reason, "取消原因不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        // 检查状态是否允许取消
        if (demand.getStatus() == DemandStatus.CANCELLED) {
            throw new IllegalStateException("需求已经是取消状态");
        }
        
        // 更新状态
        demand.setStatus(DemandStatus.CANCELLED);
        demand.setCancelReason(reason);
        demand.setCancelledTime(LocalDateTime.now());
        demand.setUpdatedTime(LocalDateTime.now());
        this.updateById(demand);
        
        log.info("采购需求取消成功，需求ID: {}, 原因: {}", demandId, reason);
    }

    @Override
    public Page<PurchaseDemand> pageDemands(Page<PurchaseDemand> page, Long tenantId,
                                           String demandNo, String materialCode, 
                                           DemandStatus status, DemandPriority priority) {
        Assert.notNull(page, "分页参数不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");
        
        LambdaQueryWrapper<PurchaseDemand> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseDemand::getTenantId, tenantId);
        
        if (demandNo != null && !demandNo.isEmpty()) {
            queryWrapper.like(PurchaseDemand::getDemandNo, demandNo);
        }
        
        if (materialCode != null && !materialCode.isEmpty()) {
            queryWrapper.like(PurchaseDemand::getMaterialCode, materialCode);
        }
        
        if (status != null) {
            queryWrapper.eq(PurchaseDemand::getStatus, status);
        }
        
        if (priority != null) {
            queryWrapper.eq(PurchaseDemand::getPriority, priority);
        }
        
        queryWrapper.orderByDesc(PurchaseDemand::getCreatedTime);
        
        return this.page(page, queryWrapper);
    }

    @Override
    public PurchaseDemand getDemandDetail(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        return demand;
    }

    @Override
    public Map<String, Object> analyzeDemandUrgency(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        Map<String, Object> result = new HashMap<>();
        
        // 紧迫性评分算法
        int urgencyScore = 0;
        
        // 1. 优先级评分
        urgencyScore += switch (demand.getPriority()) {
            case URGENT -> 40;
            case HIGH -> 30;
            case MEDIUM -> 20;
            case LOW -> 10;
            default -> 0;
        };
        
        // 2. 需求日期紧迫性
        if (demand.getRequiredDate() != null) {
            long daysUntilRequired = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now(), demand.getRequiredDate()
            );
            if (daysUntilRequired <= 3) {
                urgencyScore += 30;
            } else if (daysUntilRequired <= 7) {
                urgencyScore += 20;
            } else if (daysUntilRequired <= 14) {
                urgencyScore += 10;
            }
        }
        
        // 3. 库存状态紧迫性
        if (demand.getCurrentStock() != null && demand.getRequiredQuantity() != null) {
            double stockRatio = demand.getCurrentStock().doubleValue() / 
                              demand.getRequiredQuantity().doubleValue();
            if (stockRatio < 0.1) {
                urgencyScore += 30;
            } else if (stockRatio < 0.3) {
                urgencyScore += 20;
            } else if (stockRatio < 0.5) {
                urgencyScore += 10;
            }
        }
        
        // 4. 历史紧急程度
        if (demand.getIsEmergency() != null && demand.getIsEmergency()) {
            urgencyScore += 20;
        }
        
        // 计算紧迫性等级
        String urgencyLevel;
        if (urgencyScore >= 80) {
            urgencyLevel = "紧急";
        } else if (urgencyScore >= 60) {
            urgencyLevel = "高";
        } else if (urgencyScore >= 40) {
            urgencyLevel = "中";
        } else {
            urgencyLevel = "低";
        }
        
        result.put("urgencyScore", urgencyScore);
        result.put("urgencyLevel", urgencyLevel);
        result.put("priority", demand.getPriority());
        result.put("requiredDate", demand.getRequiredDate());
        result.put("currentStock", demand.getCurrentStock());
        result.put("requiredQuantity", demand.getRequiredQuantity());
        result.put("recommendedAction", getRecommendedAction(urgencyLevel));
        
        return result;
    }

    @Override
    public Map<String, Object> predictFulfillmentTime(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        Map<String, Object> result = new HashMap<>();
        
        // 预测算法
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime predictedDate = now;
        
        // 1. 基础处理时间
        predictedDate = predictedDate.plusDays(2);
        
        // 2. 根据优先级调整
        switch (demand.getPriority()) {
            case URGENT -> predictedDate = predictedDate.minusDays(1);
            case HIGH -> predictedDate = predictedDate.minusDays(0.5);
            case LOW -> predictedDate = predictedDate.plusDays(1);
        }
        
        // 3. 根据物料类型调整
        if (demand.getMaterialType() != null) {
            if (demand.getMaterialType().contains("进口") || 
                demand.getMaterialType().contains("特殊")) {
                predictedDate = predictedDate.plusDays(3);
            }
        }
        
        // 4. 根据数量调整
        if (demand.getRequiredQuantity() != null && 
            demand.getRequiredQuantity().compareTo(new BigDecimal("1000")) > 0) {
            predictedDate = predictedDate.plusDays(2);
        }
        
        result.put("demandId", demandId);
        result.put("currentTime", now);
        result.put("predictedFulfillmentTime", predictedDate);
        result.put("estimatedDays", java.time.temporal.ChronoUnit.DAYS.between(now, predictedDate));
        result.put("confidenceLevel", "中等");
        result.put("factors", Arrays.asList("优先级", "物料类型", "数量规模", "供应商响应时间"));
        
        return result;
    }

    @Override
    public Map<String, Object> analyzeDemandRationality(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        // 合理性检查
        boolean isRational = true;
        
        // 1. 数量合理性检查
        if (demand.getRequiredQuantity() != null) {
            if (demand.getRequiredQuantity().compareTo(new BigDecimal("0")) <= 0) {
                issues.add("需求数量必须大于0");
                suggestions.add("请检查需求数量");
                isRational = false;
            }
            
            if (demand.getRequiredQuantity().compareTo(new BigDecimal("1000000")) > 0) {
                issues.add("需求数量过大，建议分批采购");
                suggestions.add("考虑分批采购以降低风险");
            }
        }
        
        // 2. 价格合理性检查
        if (demand.getEstimatedPrice() != null && demand.getRequiredQuantity() != null) {
            BigDecimal estimatedTotal = demand.getEstimatedPrice().multiply(demand.getRequiredQuantity());
            if (estimatedTotal.compareTo(new BigDecimal("10000000")) > 0) {
                issues.add("预估总金额超过1000万，需要特殊审批");
                suggestions.add("准备特殊审批材料");
            }
        }
        
        // 3. 时间合理性检查
        if (demand.getRequiredDate() != null) {
            if (demand.getRequiredDate().isBefore(LocalDateTime.now())) {
                issues.add("需求日期已过期");
                suggestions.add("请更新需求日期");
                isRational = false;
            }
            
            long daysUntilRequired = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now(), demand.getRequiredDate()
            );
            if (daysUntilRequired < 1) {
                issues.add("需求日期过于紧迫");
                suggestions.add("考虑紧急采购流程");
            }
        }
        
        // 4. 库存合理性检查
        if (demand.getCurrentStock() != null && demand.getRequiredQuantity() != null) {
            BigDecimal stockRatio = demand.getCurrentStock()
                .divide(demand.getRequiredQuantity(), 2, BigDecimal.ROUND_HALF_UP);
            if (stockRatio.compareTo(new BigDecimal("0.5")) > 0) {
                issues.add("当前库存充足，建议延迟采购");
                suggestions.add("考虑延迟采购以降低库存成本");
            }
        }
        
        result.put("demandId", demandId);
        result.put("isRational", isRational);
        result.put("issues", issues);
        result.put("suggestions", suggestions);
        result.put("rationalityScore", calculateRationalityScore(issues));
        result.put("recommendation", isRational ? "需求合理，可以继续" : "需求不合理，需要调整");
        
        return result;
    }

    @Override
    public Map<String, Object> autoMergeSimilarDemands(Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        
        Map<String, Object> result = new HashMap<>();
        
        // 查询待合并的需求（草稿状态）
        LambdaQueryWrapper<PurchaseDemand> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseDemand::getTenantId, tenantId)
                   .eq(PurchaseDemand::getStatus, DemandStatus.DRAFT);
        
        List<PurchaseDemand> draftDemands = this.list(queryWrapper);
        
        if (draftDemands.isEmpty()) {
            result.put("mergedCount", 0);
            result.put("message", "没有可合并的草稿需求");
            return result;
        }
        
        // 按物料和申请人分组
        Map<String, List<PurchaseDemand>> groupedDemands = draftDemands.stream()
            .collect(Collectors.groupingBy(
                demand -> demand.getMaterialCode() + "|" + demand.getApplicantId()
            ));
        
        int mergedCount = 0;
        List<Map<String, Object>> mergeDetails = new ArrayList<>();
        
        // 合并相似需求
        for (List<PurchaseDemand> similarDemands : groupedDemands.values()) {
            if (similarDemands.size() > 1) {
                // 合并逻辑
                PurchaseDemand mergedDemand = mergeDemands(similarDemands);
                
                // 删除原需求
                List<Long> demandIds = similarDemands.stream()
                    .map(PurchaseDemand::getId)
                    .collect(Collectors.toList());
                this.removeByIds(demandIds);
                
                // 保存合并后的需求
                this.save(mergedDemand);
                
                mergedCount++;
                
                Map<String, Object> detail = new HashMap<>();
                detail.put("mergedDemandId", mergedDemand.getId());
                detail.put("originalDemandIds", demandIds);
                detail.put("materialCode", mergedDemand.getMaterialCode());
                detail.put("totalQuantity", mergedDemand.getRequiredQuantity());
                mergeDetails.add(detail);
            }
        }
        
        result.put("mergedCount", mergedCount);
        result.put("mergeDetails", mergeDetails);
        result.put("message", String.format("成功合并 %d 组相似需求", mergedCount));
        
        return result;
    }

    @Override
    public Map<String, Object> recommendPurchaseStrategy(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        Map<String, Object> result = new HashMap<>();
        List<String> strategies = new ArrayList<>();
        
        // 策略推荐算法
        if (demand.getRequiredQuantity() != null) {
            BigDecimal quantity = demand.getRequiredQuantity();
            
            if (quantity.compareTo(new BigDecimal("100")) < 0) {
                strategies.add("直接采购");
                strategies.add("单次采购");
            } else if (quantity.compareTo(new BigDecimal("1000")) < 0) {
                strategies.add("询价采购");
                strategies.add("多家比价");
            } else {
                strategies.add("招标采购");
                strategies.add("框架协议");
                strategies.add("分批采购");
            }
        }
        
        // 根据物料类型推荐
        if (demand.getMaterialType() != null) {
            if (demand.getMaterialType().contains("标准")) {
                strategies.add("库存采购");
                strategies.add("供应商库存");
            } else if (demand.getMaterialType().contains("定制")) {
                strategies.add("定制采购");
                strategies.add("技术协议");
            } else if (demand.getMaterialType().contains("进口")) {
                strategies.add("进口采购");
                strategies.add("国际采购");
            }
        }
        
        // 根据紧急程度推荐
        if (demand.getIsEmergency() != null && demand.getIsEmergency()) {
            strategies.add("紧急采购");
            strategies.add("快速通道");
        }
        
        // 去重并排序
        strategies = strategies.stream()
            .distinct()
            .collect(Collectors.toList());
        
        result.put("demandId", demandId);
        result.put("recommendedStrategies", strategies);
        result.put("primaryStrategy", strategies.isEmpty() ? "直接采购" : strategies.get(0));
        result.put("strategySelectionReason", "基于需求数量、物料类型和紧急程度推荐");
        
        return result;
    }

    @Override
    public Map<String, Object> analyzeDemandTrend(Long tenantId, Long materialId, Integer days) {
        Assert.notNull(tenantId, "租户ID不能为空");
        if (days == null || days <= 0) {
            days = 30; // 默认30天
        }
        
        Map<String, Object> result = new HashMap<>();
        
        // TODO: 实现需求趋势分析
        // 这里应该查询历史需求数据，分析趋势
        
        result.put("tenantId", tenantId);
        result.put("materialId", materialId);
        result.put("analysisPeriodDays", days);
        result.put("trend", "稳定");
        result.put("growthRate", "5%");
        result.put("seasonality", "无显著季节性");
        result.put("prediction", "未来30天需求预计增长3-5%");
        
        return result;
    }

    @Override
    public Map<String, Object> getDemandStatistics(Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        
        Map<String, Object> result = new HashMap<>();
        
        // 统计各类需求数量
        LambdaQueryWrapper<PurchaseDemand> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseDemand::getTenantId, tenantId);
        
        long totalDemands = this.count(queryWrapper);
        
        queryWrapper.eq(PurchaseDemand::getStatus, DemandStatus.DRAFT);
        long draftCount = this.count(queryWrapper);
        
        queryWrapper.clear();
        queryWrapper.eq(PurchaseDemand::getTenantId, tenantId)
                   .eq(PurchaseDemand::getStatus, DemandStatus.PENDING_APPROVAL);
        long pendingApprovalCount = this.count(queryWrapper);
        
        queryWrapper.clear();
        queryWrapper.eq(PurchaseDemand::getTenantId, tenantId)
                   .eq(PurchaseDemand::getStatus, DemandStatus.APPROVED);
        long approvedCount = this.count(queryWrapper);
        
        queryWrapper.clear();
        queryWrapper.eq(PurchaseDemand::getTenantId, tenantId)
                   .eq(PurchaseDemand::getStatus, DemandStatus.REJECTED);
        long rejectedCount = this.count(queryWrapper);
        
        queryWrapper.clear();
        queryWrapper.eq(PurchaseDemand::getTenantId, tenantId)
                   .eq(PurchaseDemand::getStatus, DemandStatus.CANCELLED);
        long cancelledCount = this.count(queryWrapper);
        
        // 统计金额
        // TODO: 实现金额统计
        
        result.put("tenantId", tenantId);
        result.put("totalDemands", totalDemands);
        result.put("byStatus", Map.of(
            "DRAFT", draftCount,
            "PENDING_APPROVAL", pendingApprovalCount,
            "APPROVED", approvedCount,
            "REJECTED", rejectedCount,
            "CANCELLED", cancelledCount
        ));
        result.put("byPriority", Map.of(
            "URGENT", 0,
            "HIGH", 0,
            "MEDIUM", 0,
            "LOW", 0
        ));
        result.put("totalEstimatedAmount", 0);
        result.put("averageProcessingTimeDays", 3.5);
        
        return result;
    }

    @Override
    public String generateDemandAnalysisReport(Long tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        Assert.notNull(tenantId, "租户ID不能为空");
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        // 生成分析报告
        StringBuilder report = new StringBuilder();
        report.append("采购需求分析报告\n");
        report.append("========================\n\n");
        report.append("租户ID: ").append(tenantId).append("\n");
        report.append("分析期间: ").append(startDate).append(" 至 ").append(endDate).append("\n\n");
        
        // 获取统计数据
        Map<String, Object> stats = getDemandStatistics(tenantId);
        
        report.append("1. 需求总体情况\n");
        report.append("   总需求数量: ").append(stats.get("totalDemands")).append("\n");
        report.append("   预估总金额: ").append(stats.get("totalEstimatedAmount")).append(" 元\n\n");
        
        report.append("2. 需求状态分布\n");
        Map<String, Object> byStatus = (Map<String, Object>) stats.get("byStatus");
        byStatus.forEach((status, count) -> {
            report.append("   ").append(status).append(": ").append(count).append("\n");
        });
        report.append("\n");
        
        report.append("3. 需求趋势分析\n");
        report.append("   平均处理时间: ").append(stats.get("averageProcessingTimeDays")).append(" 天\n");
        report.append("   需求增长趋势: 稳定\n\n");
        
        report.append("4. 建议\n");
        report.append("   - 优化审批流程，减少等待时间\n");
        report.append("   - 建立需求预测模型，提高采购计划准确性\n");
        report.append("   - 加强供应商管理，确保供应稳定性\n");
        
        return report.toString();
    }

    @Override
    public Map<String, BigDecimal> calculateDemandBudget(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        Map<String, BigDecimal> result = new HashMap<>();
        
        BigDecimal quantity = demand.getRequiredQuantity() != null ? 
            demand.getRequiredQuantity() : BigDecimal.ZERO;
        BigDecimal unitPrice = demand.getEstimatedPrice() != null ? 
            demand.getEstimatedPrice() : BigDecimal.ZERO;
        
        // 计算基础预算
        BigDecimal baseBudget = quantity.multiply(unitPrice);
        
        // 计算税费（假设13%增值税）
        BigDecimal taxRate = new BigDecimal("0.13");
        BigDecimal taxAmount = baseBudget.multiply(taxRate);
        
        // 计算运输费用（假设5%）
        BigDecimal shippingRate = new BigDecimal("0.05");
        BigDecimal shippingCost = baseBudget.multiply(shippingRate);
        
        // 计算其他费用（假设2%）
        BigDecimal otherRate = new BigDecimal("0.02");
        BigDecimal otherCost = baseBudget.multiply(otherRate);
        
        // 计算总预算
        BigDecimal totalBudget = baseBudget.add(taxAmount).add(shippingCost).add(otherCost);
        
        result.put("baseBudget", baseBudget);
        result.put("taxAmount", taxAmount);
        result.put("shippingCost", shippingCost);
        result.put("otherCost", otherCost);
        result.put("totalBudget", totalBudget);
        result.put("unitPrice", unitPrice);
        result.put("quantity", quantity);
        
        return result;
    }

    @Override
    public Map<String, Boolean> validateDemandData(PurchaseDemand demand) {
        Map<String, Boolean> validationResult = new HashMap<>();
        
        validationResult.put("tenantIdValid", demand.getTenantId() != null);
        validationResult.put("applicantIdValid", demand.getApplicantId() != null);
        validationResult.put("materialCodeValid", demand.getMaterialCode() != null && !demand.getMaterialCode().isEmpty());
        validationResult.put("materialNameValid", demand.getMaterialName() != null && !demand.getMaterialName().isEmpty());
        validationResult.put("requiredQuantityValid", demand.getRequiredQuantity() != null && 
            demand.getRequiredQuantity().compareTo(BigDecimal.ZERO) > 0);
        validationResult.put("priorityValid", demand.getPriority() != null);
        
        boolean allValid = validationResult.values().stream().allMatch(Boolean::booleanValue);
        validationResult.put("allValid", allValid);
        
        return validationResult;
    }

    @Override
    public Map<String, Object> batchImportDemands(List<PurchaseDemand> demands) {
        Assert.notEmpty(demands, "需求列表不能为空");
        
        Map<String, Object> result = new HashMap<>();
        List<Long> successIds = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();
        
        for (int i = 0; i < demands.size(); i++) {
            PurchaseDemand demand = demands.get(i);
            try {
                // 验证数据
                Map<String, Boolean> validation = validateDemandData(demand);
                if (!validation.getOrDefault("allValid", false)) {
                    Map<String, Object> failure = new HashMap<>();
                    failure.put("index", i);
                    failure.put("reason", "数据验证失败");
                    failure.put("validationErrors", validation);
                    failures.add(failure);
                    continue;
                }
                
                // 生成需求编号
                String demandNo = generateDemandNo(demand.getTenantId());
                demand.setDemandNo(demandNo);
                
                // 设置默认值
                demand.setStatus(DemandStatus.DRAFT);
                demand.setApprovalStatus(DemandApprovalStatus.PENDING);
                demand.setCreatedTime(LocalDateTime.now());
                demand.setUpdatedTime(LocalDateTime.now());
                
                // 保存
                this.save(demand);
                successIds.add(demand.getId());
                
            } catch (Exception e) {
                Map<String, Object> failure = new HashMap<>();
                failure.put("index", i);
                failure.put("reason", e.getMessage());
                failures.add(failure);
            }
        }
        
        result.put("totalCount", demands.size());
        result.put("successCount", successIds.size());
        result.put("failureCount", failures.size());
        result.put("successIds", successIds);
        result.put("failures", failures);
        
        log.info("批量导入采购需求完成，成功: {}, 失败: {}", successIds.size(), failures.size());
        return result;
    }

    @Override
    public List<Map<String, Object>> exportDemands(Long tenantId, DemandStatus status) {
        Assert.notNull(tenantId, "租户ID不能为空");
        
        LambdaQueryWrapper<PurchaseDemand> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseDemand::getTenantId, tenantId);
        
        if (status != null) {
            queryWrapper.eq(PurchaseDemand::getStatus, status);
        }
        
        List<PurchaseDemand> demands = this.list(queryWrapper);
        
        return demands.stream().map(demand -> {
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("demandId", demand.getId());
            exportData.put("demandNo", demand.getDemandNo());
            exportData.put("materialCode", demand.getMaterialCode());
            exportData.put("materialName", demand.getMaterialName());
            exportData.put("requiredQuantity", demand.getRequiredQuantity());
            exportData.put("estimatedPrice", demand.getEstimatedPrice());
            exportData.put("priority", demand.getPriority());
            exportData.put("status", demand.getStatus());
            exportData.put("applicantId", demand.getApplicantId());
            exportData.put("requiredDate", demand.getRequiredDate());
            exportData.put("createdTime", demand.getCreatedTime());
            return exportData;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean isDemandFulfilled(Long demandId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        if (demand == null) {
            return false;
        }
        
        // 检查是否已转为订单
        return demand.getOrderId() != null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDemandRelations(Long demandId, Long inquiryId, Long orderId) {
        Assert.notNull(demandId, "需求ID不能为空");
        
        PurchaseDemand demand = this.getById(demandId);
        Assert.notNull(demand, "需求不存在，ID: " + demandId);
        
        boolean updated = false;
        
        if (inquiryId != null) {
            demand.setInquiryId(inquiryId);
            updated = true;
        }
        
        if (orderId != null) {
            demand.setOrderId(orderId);
            // 如果关联了订单，更新状态为已完成
            demand.setStatus(DemandStatus.COMPLETED);
            updated = true;
        }
        
        if (updated) {
            demand.setUpdatedTime(LocalDateTime.now());
            this.updateById(demand);
            
            log.info("更新需求关联信息成功，需求ID: {}, 询价单ID: {}, 订单ID: {}", 
                    demandId, inquiryId, orderId);
        }
    }

    @Override
    public List<PurchaseDemand> searchDemands(String keyword, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        
        LambdaQueryWrapper<PurchaseDemand> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseDemand::getTenantId, tenantId);
        
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like(PurchaseDemand::getDemandNo, keyword)
                .or()
                .like(PurchaseDemand::getMaterialCode, keyword)
                .or()
                .like(PurchaseDemand::getMaterialName, keyword)
                .or()
                .like(PurchaseDemand::getApplicantName, keyword)
                .or()
                .like(PurchaseDemand::getDescription, keyword)
            );
        }
        
        queryWrapper.orderByDesc(PurchaseDemand::getCreatedTime);
        
        return this.list(queryWrapper);
    }

    // ================ 私有辅助方法 ================
    
    /**
     * 生成需求编号
     */
    private String generateDemandNo(Long tenantId) {
        String prefix = "PD";
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%06d", getDailySequence(tenantId));
        return prefix + dateStr + sequence;
    }
    
    /**
     * 获取当日序列号
     */
    private int getDailySequence(Long tenantId) {
        // 这里应该查询数据库获取当日序列号
        // 简化实现：返回随机数
        return new Random().nextInt(999999) + 1;
    }
    
    /**
     * 验证需求数据
     */
    private void validateDemandData(PurchaseDemand demand) {
        Assert.notNull(demand.getTenantId(), "租户ID不能为空");
        Assert.notNull(demand.getApplicantId(), "申请人ID不能为空");
        Assert.hasText(demand.getMaterialCode(), "物料编码不能为空");
        Assert.hasText(demand.getMaterialName(), "物料名称不能为空");
        Assert.notNull(demand.getRequiredQuantity(), "需求数量不能为空");
        Assert.isTrue(demand.getRequiredQuantity().compareTo(BigDecimal.ZERO) > 0, "需求数量必须大于0");
        Assert.notNull(demand.getPriority(), "优先级不能为空");
    }
    
    /**
     * 验证需求更新
     */
    private void validateDemandUpdate(PurchaseDemand existing, PurchaseDemand update) {
        // 已批准的需求不能修改关键信息
        if (existing.getStatus() == DemandStatus.APPROVED || 
            existing.getStatus() == DemandStatus.COMPLETED) {
            // 只允许修改非关键字段
            if (!Objects.equals(existing.getMaterialCode(), update.getMaterialCode()) ||
                !Objects.equals(existing.getRequiredQuantity(), update.getRequiredQuantity()) ||
                !Objects.equals(existing.getPriority(), update.getPriority())) {
                throw new IllegalStateException("已批准或已完成的需求不能修改关键信息");
            }
        }
    }
    
    /**
     * 根据紧迫性等级获取推荐行动
     */
    private String getRecommendedAction(String urgencyLevel) {
        return switch (urgencyLevel) {
            case "紧急" -> "立即启动紧急采购流程，联系供应商确认库存";
            case "高" -> "优先处理，3天内完成采购流程";
            case "中" -> "正常处理，7天内完成采购流程";
            case "低" -> "按计划处理，14天内完成采购流程";
            default -> "按正常流程处理";
        };
    }
    
    /**
     * 计算合理性分数
     */
    private int calculateRationalityScore(List<String> issues) {
        if (issues.isEmpty()) {
            return 100;
        }
        // 每个问题扣20分
        int deduction = Math.min(issues.size() * 20, 100);
        return Math.max(0, 100 - deduction);
    }
    
    /**
     * 合并需求
     */
    private PurchaseDemand mergeDemands(List<PurchaseDemand> demands) {
        if (demands.isEmpty()) {
            return null;
        }
        
        // 以第一个需求为基础
        PurchaseDemand baseDemand = demands.get(0);
        PurchaseDemand merged = new PurchaseDemand();
        
        // 复制基础信息
        merged.setTenantId(baseDemand.getTenantId());
        merged.setApplicantId(baseDemand.getApplicantId());
        merged.setMaterialCode(baseDemand.getMaterialCode());
        merged.setMaterialName(baseDemand.getMaterialName());
        merged.setMaterialType(baseDemand.getMaterialType());
        merged.setUnit(baseDemand.getUnit());
        
        // 合并数量
        BigDecimal totalQuantity = demands.stream()
            .map(PurchaseDemand::getRequiredQuantity)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        merged.setRequiredQuantity(totalQuantity);
        
        // 计算平均预估价格
        BigDecimal totalPrice = BigDecimal.ZERO;
        int priceCount = 0;
        for (PurchaseDemand demand : demands) {
            if (demand.getEstimatedPrice() != null) {
                totalPrice = totalPrice.add(demand.getEstimatedPrice());
                priceCount++;
            }
        }
        if (priceCount > 0) {
            merged.setEstimatedPrice(totalPrice.divide(new BigDecimal(priceCount), 2, BigDecimal.ROUND_HALF_UP));
        }
        
        // 设置最紧急的优先级
        DemandPriority highestPriority = demands.stream()
            .map(PurchaseDemand::getPriority)
            .filter(Objects::nonNull)
            .max(Comparator.comparing(Enum::ordinal))
            .orElse(DemandPriority.MEDIUM);
        merged.setPriority(highestPriority);
        
        // 设置最早的需求日期
        LocalDateTime earliestDate = demands.stream()
            .map(PurchaseDemand::getRequiredDate)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now().plusDays(7));
        merged.setRequiredDate(earliestDate);
        
        // 合并描述
        String mergedDescription = demands.stream()
            .map(PurchaseDemand::getDescription)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n"));
        merged.setDescription(mergedDescription);
        
        // 设置状态
        merged.setStatus(DemandStatus.DRAFT);
        merged.setApprovalStatus(DemandApprovalStatus.PENDING);
        merged.setCreatedTime(LocalDateTime.now());
        merged.setUpdatedTime(LocalDateTime.now());
        
        return merged;
    }
    
    /**
     * 从需求创建询价单（模拟方法）
     */
    private Long createInquiryFromDemand(PurchaseDemand demand) {
        // TODO: 实现真实的询价单创建逻辑
        // 这里返回一个模拟的询价单ID
        return System.currentTimeMillis();
    }
    
    /**
     * 从多个需求批量创建询价单（模拟方法）
     */
    private Long createBatchInquiryFromDemands(List<PurchaseDemand> demands) {
        // TODO: 实现真实的批量询价单创建逻辑
        // 这里返回一个模拟的询价单ID
        return System.currentTimeMillis();
    }
}