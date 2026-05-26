package cn.aiedge.crm.marketing.service;

import cn.aiedge.crm.marketing.entity.MarketingCampaign;
import cn.aiedge.crm.marketing.entity.MarketingChannel;
import cn.aiedge.crm.marketing.entity.MarketingContent;
import cn.aiedge.crm.marketing.entity.MarketingExecution;
import cn.aiedge.crm.marketing.entity.MarketingTarget;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface MarketingCampaignService extends IService<MarketingCampaign> {

    MarketingCampaign getByCampaignCode(String campaignCode);

    Page<MarketingCampaign> pageList(String keyword, Integer campaignType, Integer status, Long ownerId, Long departmentId, int pageNum, int pageSize);

    List<MarketingCampaign> listByOwnerId(Long ownerId);

    List<MarketingCampaign> listByDepartmentId(Long departmentId);

    List<MarketingCampaign> listByStatus(Integer status);

    List<MarketingCampaign> listRunningCampaigns();

    List<MarketingCampaign> listEndedCampaigns();

    String generateCampaignCode();

    MarketingCampaign createCampaign(MarketingCampaign campaign);

    MarketingCampaign updateCampaign(Long campaignId, MarketingCampaign campaign);

    MarketingCampaign submitForApproval(Long campaignId);

    MarketingCampaign approve(Long campaignId, Long approverId, String note);

    MarketingCampaign reject(Long campaignId, Long rejecterId, String reason);

    MarketingCampaign schedule(Long campaignId);

    MarketingCampaign start(Long campaignId);

    MarketingCampaign pause(Long campaignId);

    MarketingCampaign resume(Long campaignId);

    MarketingCampaign complete(Long campaignId);

    MarketingCampaign cancel(Long campaignId, String reason);

    MarketingCampaign updateProgress(Long campaignId);

    MarketingCampaign updateCost(Long campaignId, BigDecimal actualCost);

    MarketingCampaign updateRevenue(Long campaignId, BigDecimal actualRevenue);

    MarketingCampaign calculateROI(Long campaignId);

    List<MarketingTarget> getTargets(Long campaignId);

    MarketingTarget addTarget(Long campaignId, MarketingTarget target);

    MarketingTarget updateTarget(Long targetId, MarketingTarget target);

    void removeTarget(Long targetId);

    List<MarketingTarget> batchAddTargets(Long campaignId, List<Long> customerIds);

    MarketingTarget markReached(Long targetId, Integer channel);

    MarketingTarget markResponded(Long targetId, String content);

    MarketingTarget markConverted(Long targetId, Long leadId, Long opportunityId, Long orderId, BigDecimal orderAmount);

    List<MarketingExecution> getExecutions(Long campaignId);

    MarketingExecution addExecution(Long campaignId, MarketingExecution execution);

    List<MarketingExecution> getExecutionsByTarget(Long targetId);

    BigDecimal calculateTotalCost(Long campaignId);

    Integer calculateResponseRate(Long campaignId);

    Integer calculateConversionRate(Long campaignId);

    Integer calculateROIPercentage(Long campaignId);
}