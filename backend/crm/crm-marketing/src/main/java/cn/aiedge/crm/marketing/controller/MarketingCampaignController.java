package cn.aiedge.crm.marketing.controller;

import cn.aiedge.crm.marketing.dto.CampaignCreateDTO;
import cn.aiedge.crm.marketing.dto.CampaignVO;
import cn.aiedge.crm.marketing.entity.MarketingCampaign;
import cn.aiedge.crm.marketing.entity.MarketingExecution;
import cn.aiedge.crm.marketing.entity.MarketingTarget;
import cn.aiedge.crm.marketing.enums.CampaignStatus;
import cn.aiedge.crm.marketing.service.MarketingCampaignService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/crm/marketing")
@RequiredArgsConstructor
@Tag(name = "CRM营销活动管理", description = "营销活动创建、审批、执行跟踪、效果分析等操作")
public class MarketingCampaignController {

    private final MarketingCampaignService campaignService;

    @GetMapping("/page")
    @Operation(summary = "分页查询营销活动")
    public Page<CampaignVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "活动类型") @RequestParam(required = false) Integer campaignType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "负责人ID") @RequestParam(required = false) Long ownerId,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<MarketingCampaign> page = campaignService.pageList(keyword, campaignType, status, ownerId, departmentId, pageNum, pageSize);
        Page<CampaignVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取营销活动详情")
    public CampaignVO getById(@PathVariable Long id) {
        MarketingCampaign campaign = campaignService.getById(id);
        if (campaign == null) {
            throw new RuntimeException("营销活动不存在");
        }
        CampaignVO vo = convertToVO(campaign);
        vo.setTotalCost(campaignService.calculateTotalCost(id));
        vo.setResponseRate(campaignService.calculateResponseRate(id));
        vo.setConversionRate(campaignService.calculateConversionRate(id));
        vo.setRoiPercentage(campaignService.calculateROIPercentage(id));
        return vo;
    }

    @GetMapping("/running")
    @Operation(summary = "获取进行中的营销活动")
    public List<CampaignVO> listRunningCampaigns() {
        return campaignService.listRunningCampaigns().stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/ended")
    @Operation(summary = "获取已结束的营销活动")
    public List<CampaignVO> listEndedCampaigns() {
        return campaignService.listEndedCampaigns().stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "创建营销活动")
    public CampaignVO create(@RequestBody CampaignCreateDTO dto) {
        MarketingCampaign campaign = new MarketingCampaign();
        BeanUtils.copyProperties(dto, campaign);
        campaign.setTenantId(1L);
        campaign.setCreateBy(StpUtil.getLoginIdAsLong());
        MarketingCampaign created = campaignService.createCampaign(campaign);
        return convertToVO(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新营销活动")
    public CampaignVO update(@PathVariable Long id, @RequestBody CampaignCreateDTO dto) {
        MarketingCampaign campaign = new MarketingCampaign();
        BeanUtils.copyProperties(dto, campaign);
        MarketingCampaign updated = campaignService.updateCampaign(id, campaign);
        return convertToVO(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public CampaignVO submitForApproval(@PathVariable Long id) {
        MarketingCampaign campaign = campaignService.submitForApproval(id);
        return convertToVO(campaign);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public CampaignVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        MarketingCampaign campaign = campaignService.approve(id, approverId, note);
        return convertToVO(campaign);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public CampaignVO reject(@PathVariable Long id, @RequestParam String reason) {
        Long rejecterId = StpUtil.getLoginIdAsLong();
        MarketingCampaign campaign = campaignService.reject(id, rejecterId, reason);
        return convertToVO(campaign);
    }

    @PostMapping("/{id}/schedule")
    @Operation(summary = "排期")
    public CampaignVO schedule(@PathVariable Long id) {
        MarketingCampaign campaign = campaignService.schedule(id);
        return convertToVO(campaign);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "启动活动")
    public CampaignVO start(@PathVariable Long id) {
        MarketingCampaign campaign = campaignService.start(id);
        return convertToVO(campaign);
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停活动")
    public CampaignVO pause(@PathVariable Long id) {
        MarketingCampaign campaign = campaignService.pause(id);
        return convertToVO(campaign);
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复活动")
    public CampaignVO resume(@PathVariable Long id) {
        MarketingCampaign campaign = campaignService.resume(id);
        return convertToVO(campaign);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成活动")
    public CampaignVO complete(@PathVariable Long id) {
        MarketingCampaign campaign = campaignService.complete(id);
        return convertToVO(campaign);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消活动")
    public CampaignVO cancel(@PathVariable Long id, @RequestParam String reason) {
        MarketingCampaign campaign = campaignService.cancel(id, reason);
        return convertToVO(campaign);
    }

    @PostMapping("/{id}/update-progress")
    @Operation(summary = "更新进度")
    public CampaignVO updateProgress(@PathVariable Long id) {
        MarketingCampaign campaign = campaignService.updateProgress(id);
        return convertToVO(campaign);
    }

    @GetMapping("/{id}/targets")
    @Operation(summary = "获取目标客户列表")
    public List<MarketingTarget> getTargets(@PathVariable Long id) {
        return campaignService.getTargets(id);
    }

    @PostMapping("/{id}/targets")
    @Operation(summary = "添加目标客户")
    public MarketingTarget addTarget(@PathVariable Long id, @RequestBody MarketingTarget target) {
        return campaignService.addTarget(id, target);
    }

    @PostMapping("/{id}/targets/batch")
    @Operation(summary = "批量添加目标客户")
    public List<MarketingTarget> batchAddTargets(@PathVariable Long id, @RequestBody List<Long> customerIds) {
        return campaignService.batchAddTargets(id, customerIds);
    }

    @PutMapping("/{id}/targets/{targetId}")
    @Operation(summary = "更新目标客户")
    public MarketingTarget updateTarget(@PathVariable Long targetId, @RequestBody MarketingTarget target) {
        return campaignService.updateTarget(targetId, target);
    }

    @DeleteMapping("/{id}/targets/{targetId}")
    @Operation(summary = "删除目标客户")
    public void removeTarget(@PathVariable Long targetId) {
        campaignService.removeTarget(targetId);
    }

    @PostMapping("/{id}/targets/{targetId}/reach")
    @Operation(summary = "标记已触达")
    public MarketingTarget markReached(@PathVariable Long targetId, @RequestParam Integer channel) {
        return campaignService.markReached(targetId, channel);
    }

    @PostMapping("/{id}/targets/{targetId}/respond")
    @Operation(summary = "标记已响应")
    public MarketingTarget markResponded(@PathVariable Long targetId, @RequestParam String content) {
        return campaignService.markResponded(targetId, content);
    }

    @PostMapping("/{id}/targets/{targetId}/convert")
    @Operation(summary = "标记已转化")
    public MarketingTarget markConverted(
            @PathVariable Long targetId,
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) Long opportunityId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) BigDecimal orderAmount) {
        return campaignService.markConverted(targetId, leadId, opportunityId, orderId, orderAmount);
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "获取执行记录")
    public List<MarketingExecution> getExecutions(@PathVariable Long id) {
        return campaignService.getExecutions(id);
    }

    @PostMapping("/{id}/executions")
    @Operation(summary = "添加执行记录")
    public MarketingExecution addExecution(@PathVariable Long id, @RequestBody MarketingExecution execution) {
        return campaignService.addExecution(id, execution);
    }

    @GetMapping("/statistics")
    @Operation(summary = "营销活动统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (CampaignStatus status : CampaignStatus.values()) {
            stats.put(status.getDesc(), campaignService.lambdaQuery()
                    .eq(MarketingCampaign::getStatus, status.getCode())
                    .eq(MarketingCampaign::getDeleted, 0)
                    .count());
        }
        stats.put("totalBudget", campaignService.baseMapper.sumBudget(1L));
        stats.put("totalActualCost", campaignService.baseMapper.sumActualCost(1L));
        stats.put("totalActualRevenue", campaignService.baseMapper.sumActualRevenue(1L));
        return stats;
    }

    private CampaignVO convertToVO(MarketingCampaign campaign) {
        CampaignVO vo = new CampaignVO();
        BeanUtils.copyProperties(campaign, vo);
        for (CampaignStatus status : CampaignStatus.values()) {
            if (status.getCode().equals(campaign.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }
}