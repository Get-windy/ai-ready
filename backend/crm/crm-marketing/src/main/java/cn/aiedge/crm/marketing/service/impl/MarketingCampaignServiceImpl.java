package cn.aiedge.crm.marketing.service.impl;

import cn.aiedge.crm.marketing.entity.MarketingCampaign;
import cn.aiedge.crm.marketing.entity.MarketingExecution;
import cn.aiedge.crm.marketing.entity.MarketingTarget;
import cn.aiedge.crm.marketing.enums.CampaignStatus;
import cn.aiedge.crm.marketing.mapper.MarketingCampaignMapper;
import cn.aiedge.crm.marketing.mapper.MarketingExecutionMapper;
import cn.aiedge.crm.marketing.mapper.MarketingTargetMapper;
import cn.aiedge.crm.marketing.service.MarketingCampaignService;
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
public class MarketingCampaignServiceImpl extends ServiceImpl<MarketingCampaignMapper, MarketingCampaign> implements MarketingCampaignService {

    private final MarketingTargetMapper targetMapper;
    private final MarketingExecutionMapper executionMapper;

    @Override
    public MarketingCampaign getByCampaignCode(String campaignCode) {
        return lambdaQuery()
                .eq(MarketingCampaign::getCampaignCode, campaignCode)
                .eq(MarketingCampaign::getDeleted, 0)
                .one();
    }

    @Override
    public Page<MarketingCampaign> pageList(String keyword, Integer campaignType, Integer status, Long ownerId, Long departmentId, int pageNum, int pageSize) {
        LambdaQueryWrapper<MarketingCampaign> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingCampaign::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(MarketingCampaign::getCampaignCode, keyword)
                    .or().like(MarketingCampaign::getCampaignName, keyword));
        }
        if (campaignType != null) {
            wrapper.eq(MarketingCampaign::getCampaignType, campaignType);
        }
        if (status != null) {
            wrapper.eq(MarketingCampaign::getStatus, status);
        }
        if (ownerId != null) {
            wrapper.eq(MarketingCampaign::getOwnerId, ownerId);
        }
        if (departmentId != null) {
            wrapper.eq(MarketingCampaign::getDepartmentId, departmentId);
        }
        wrapper.orderByDesc(MarketingCampaign::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<MarketingCampaign> listByOwnerId(Long ownerId) {
        return baseMapper.selectByOwnerId(ownerId);
    }

    @Override
    public List<MarketingCampaign> listByDepartmentId(Long departmentId) {
        return baseMapper.selectByDepartmentId(departmentId);
    }

    @Override
    public List<MarketingCampaign> listByStatus(Integer status) {
        return baseMapper.selectByStatus(status, 1L);
    }

    @Override
    public List<MarketingCampaign> listRunningCampaigns() {
        return baseMapper.selectRunningCampaigns(LocalDate.now());
    }

    @Override
    public List<MarketingCampaign> listEndedCampaigns() {
        return baseMapper.selectEndedCampaigns(LocalDate.now());
    }

    @Override
    public String generateCampaignCode() {
        String prefix = "MC";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<MarketingCampaign> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(MarketingCampaign::getCampaignCode, prefix + dateStr)
                .eq(MarketingCampaign::getDeleted, 0)
                .orderByDesc(MarketingCampaign::getCampaignCode)
                .last("LIMIT 1");
        MarketingCampaign lastCampaign = getOne(wrapper);
        int seq = 1;
        if (lastCampaign != null) {
            String lastNo = lastCampaign.getCampaignCode();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign createCampaign(MarketingCampaign campaign) {
        campaign.setCampaignCode(generateCampaignCode());
        campaign.setStatus(CampaignStatus.DRAFT.getCode());
        campaign.setActualCost(BigDecimal.ZERO);
        campaign.setActualRevenue(BigDecimal.ZERO);
        campaign.setActualLeads(0);
        campaign.setActualOpportunities(0);
        campaign.setActualOrders(0);
        campaign.setReachedCustomerCount(0);
        campaign.setRespondedCustomerCount(0);
        campaign.setConvertedCustomerCount(0);
        campaign.setRoi(0);
        campaign.setConversionRate(0);
        campaign.setResponseRate(0);
        save(campaign);
        return getById(campaign.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign updateCampaign(Long campaignId, MarketingCampaign campaign) {
        MarketingCampaign existing = getById(campaignId);
        if (existing == null) {
            throw new RuntimeException("营销活动不存在");
        }
        if (existing.getStatus() != CampaignStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的营销活动可以修改");
        }
        campaign.setId(campaignId);
        updateById(campaign);
        return getById(campaignId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign submitForApproval(Long campaignId) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        if (campaign.getStatus() != CampaignStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的营销活动可以提交审批");
        }
        campaign.setStatus(CampaignStatus.PENDING_APPROVAL.getCode());
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign approve(Long campaignId, Long approverId, String note) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        if (campaign.getStatus() != CampaignStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的营销活动可以审批");
        }
        campaign.setStatus(CampaignStatus.APPROVED.getCode());
        campaign.setApprovedBy(approverId);
        campaign.setApprovedTime(LocalDateTime.now());
        campaign.setApprovedNote(note);
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign reject(Long campaignId, Long rejecterId, String reason) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        if (campaign.getStatus() != CampaignStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的营销活动可以拒绝");
        }
        campaign.setStatus(CampaignStatus.DRAFT.getCode());
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign schedule(Long campaignId) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        if (campaign.getStatus() != CampaignStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的营销活动可以排期");
        }
        campaign.setStatus(CampaignStatus.SCHEDULED.getCode());
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign start(Long campaignId) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        if (campaign.getStatus() != CampaignStatus.SCHEDULED.getCode() && campaign.getStatus() != CampaignStatus.PAUSED.getCode()) {
            throw new RuntimeException("只有已排期或已暂停状态的营销活动可以启动");
        }
        campaign.setStatus(CampaignStatus.RUNNING.getCode());
        campaign.setStartedBy(campaign.getCreateBy());
        campaign.setStartedTime(LocalDateTime.now());
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign pause(Long campaignId) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        if (campaign.getStatus() != CampaignStatus.RUNNING.getCode()) {
            throw new RuntimeException("只有进行中的营销活动可以暂停");
        }
        campaign.setStatus(CampaignStatus.PAUSED.getCode());
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign resume(Long campaignId) {
        return start(campaignId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign complete(Long campaignId) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        campaign.setStatus(CampaignStatus.COMPLETED.getCode());
        campaign.setCompletedBy(campaign.getCreateBy());
        campaign.setCompletedTime(LocalDateTime.now());
        calculateROI(campaignId);
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign cancel(Long campaignId, String reason) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        if (campaign.getStatus() == CampaignStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的营销活动不能取消");
        }
        campaign.setStatus(CampaignStatus.CANCELLED.getCode());
        campaign.setRemark(reason);
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign updateProgress(Long campaignId) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        Integer reachedCount = targetMapper.countReachedByCampaignId(campaignId);
        Integer respondedCount = targetMapper.countRespondedByCampaignId(campaignId);
        Integer convertedCount = targetMapper.countConvertedByCampaignId(campaignId);
        campaign.setReachedCustomerCount(reachedCount);
        campaign.setRespondedCustomerCount(respondedCount);
        campaign.setConvertedCustomerCount(convertedCount);
        BigDecimal totalCost = calculateTotalCost(campaignId);
        campaign.setActualCost(totalCost);
        campaign.setResponseRate(calculateResponseRate(campaignId));
        campaign.setConversionRate(calculateConversionRate(campaignId));
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign updateCost(Long campaignId, BigDecimal actualCost) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        campaign.setActualCost(actualCost);
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign updateRevenue(Long campaignId, BigDecimal actualRevenue) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        campaign.setActualRevenue(actualRevenue);
        updateById(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaign calculateROI(Long campaignId) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        BigDecimal cost = campaign.getActualCost();
        BigDecimal revenue = campaign.getActualRevenue();
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0 && revenue != null) {
            BigDecimal roi = revenue.subtract(cost).divide(cost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            campaign.setRoi(roi.intValue());
        }
        updateById(campaign);
        return campaign;
    }

    @Override
    public List<MarketingTarget> getTargets(Long campaignId) {
        return targetMapper.selectByCampaignId(campaignId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingTarget addTarget(Long campaignId, MarketingTarget target) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        target.setCampaignId(campaignId);
        target.setTenantId(campaign.getTenantId());
        target.setTargetStatus(0);
        target.setReachStatus(0);
        target.setResponseStatus(0);
        target.setConversionStatus(0);
        targetMapper.insert(target);
        Integer count = targetMapper.countByCampaignId(campaignId);
        campaign.setTargetCustomerCount(count);
        updateById(campaign);
        return target;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingTarget updateTarget(Long targetId, MarketingTarget target) {
        MarketingTarget existing = targetMapper.selectById(targetId);
        if (existing == null) {
            throw new RuntimeException("目标客户不存在");
        }
        target.setId(targetId);
        targetMapper.updateById(target);
        return targetMapper.selectById(targetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTarget(Long targetId) {
        MarketingTarget target = targetMapper.selectById(targetId);
        if (target != null) {
            targetMapper.deleteById(targetId);
            Integer count = targetMapper.countByCampaignId(target.getCampaignId());
            MarketingCampaign campaign = getById(target.getCampaignId());
            campaign.setTargetCustomerCount(count);
            updateById(campaign);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MarketingTarget> batchAddTargets(Long campaignId, List<Long> customerIds) {
        List<MarketingTarget> targets = new java.util.ArrayList<>();
        for (Long customerId : customerIds) {
            MarketingTarget target = new MarketingTarget();
            target.setCampaignId(campaignId);
            target.setCustomerId(customerId);
            targets.add(addTarget(campaignId, target));
        }
        return targets;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingTarget markReached(Long targetId, Integer channel) {
        MarketingTarget target = targetMapper.selectById(targetId);
        if (target == null) {
            throw new RuntimeException("目标客户不存在");
        }
        target.setTargetStatus(1);
        target.setReachStatus(1);
        target.setReachTime(LocalDateTime.now());
        target.setReachChannel(channel);
        targetMapper.updateById(target);
        updateProgress(target.getCampaignId());
        return target;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingTarget markResponded(Long targetId, String content) {
        MarketingTarget target = targetMapper.selectById(targetId);
        if (target == null) {
            throw new RuntimeException("目标客户不存在");
        }
        target.setTargetStatus(2);
        target.setResponseStatus(1);
        target.setResponseTime(LocalDateTime.now());
        target.setResponseContent(content);
        targetMapper.updateById(target);
        updateProgress(target.getCampaignId());
        return target;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingTarget markConverted(Long targetId, Long leadId, Long opportunityId, Long orderId, BigDecimal orderAmount) {
        MarketingTarget target = targetMapper.selectById(targetId);
        if (target == null) {
            throw new RuntimeException("目标客户不存在");
        }
        target.setTargetStatus(3);
        target.setConversionStatus(1);
        target.setConversionTime(LocalDateTime.now());
        target.setLeadId(leadId);
        target.setOpportunityId(opportunityId);
        target.setOrderId(orderId);
        target.setOrderAmount(orderAmount);
        targetMapper.updateById(target);
        MarketingCampaign campaign = getById(target.getCampaignId());
        if (leadId != null) {
            campaign.setActualLeads(campaign.getActualLeads() + 1);
        }
        if (opportunityId != null) {
            campaign.setActualOpportunities(campaign.getActualOpportunities() + 1);
        }
        if (orderId != null) {
            campaign.setActualOrders(campaign.getActualOrders() + 1);
            BigDecimal revenue = campaign.getActualRevenue().add(orderAmount != null ? orderAmount : BigDecimal.ZERO);
            campaign.setActualRevenue(revenue);
        }
        updateById(campaign);
        updateProgress(target.getCampaignId());
        return target;
    }

    @Override
    public List<MarketingExecution> getExecutions(Long campaignId) {
        return executionMapper.selectByCampaignId(campaignId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingExecution addExecution(Long campaignId, MarketingExecution execution) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        execution.setCampaignId(campaignId);
        execution.setTenantId(campaign.getTenantId());
        execution.setExecutionTime(LocalDateTime.now());
        executionMapper.insert(execution);
        return execution;
    }

    @Override
    public List<MarketingExecution> getExecutionsByTarget(Long targetId) {
        return executionMapper.selectByTargetId(targetId);
    }

    @Override
    public BigDecimal calculateTotalCost(Long campaignId) {
        return executionMapper.sumCostByCampaignId(campaignId);
    }

    @Override
    public Integer calculateResponseRate(Long campaignId) {
        Integer total = targetMapper.countByCampaignId(campaignId);
        Integer responded = targetMapper.countRespondedByCampaignId(campaignId);
        if (total == null || total == 0) {
            return 0;
        }
        return (responded * 100) / total;
    }

    @Override
    public Integer calculateConversionRate(Long campaignId) {
        Integer total = targetMapper.countByCampaignId(campaignId);
        Integer converted = targetMapper.countConvertedByCampaignId(campaignId);
        if (total == null || total == 0) {
            return 0;
        }
        return (converted * 100) / total;
    }

    @Override
    public Integer calculateROIPercentage(Long campaignId) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            return 0;
        }
        return campaign.getRoi() != null ? campaign.getRoi() : 0;
    }
}