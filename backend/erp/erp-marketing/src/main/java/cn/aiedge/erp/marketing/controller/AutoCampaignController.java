package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.util.DesensitizeUtils;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.AutoCampaignCandidate;
import cn.aiedge.erp.marketing.entity.AutoCampaign;
import cn.aiedge.erp.marketing.entity.AutoCampaignLog;
import cn.aiedge.erp.marketing.mapper.AutoCampaignLogMapper;
import cn.aiedge.erp.marketing.mapper.AutoCampaignMapper;
import cn.aiedge.erp.marketing.service.AutoCampaignService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 营销自动化（菜单 80303，本系统建模页）
 * 双视图：自动化规则 / 执行记录。
 */
@Slf4j
@Tag(name = "营销自动化")
@RestController
@RequestMapping("/api/erp/marketing/auto-campaign")
@RequiredArgsConstructor
public class AutoCampaignController {

    private final AutoCampaignMapper campaignMapper;
    private final AutoCampaignLogMapper logMapper;
    private final AutoCampaignService autoCampaignService;

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    // ══════ Tab1 自动化规则 ══════

    @Operation(summary = "分页查询自动化规则")
    @GetMapping("/page")
    public Result<IPage<AutoCampaign>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String triggerType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<AutoCampaign> w = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) w.like(AutoCampaign::getName, name);
        if (triggerType != null && !triggerType.isEmpty()) w.eq(AutoCampaign::getTriggerType, triggerType);
        if (status != null) w.eq(AutoCampaign::getStatus, status);
        w.orderByDesc(AutoCampaign::getCreateTime).orderByDesc(AutoCampaign::getId);
        return Result.ok(campaignMapper.selectPage(new Page<>(pageNum, pageSize), w));
    }

    @Operation(summary = "规则详情")
    @GetMapping("/{id}")
    public Result<AutoCampaign> getById(@PathVariable Long id) {
        AutoCampaign c = campaignMapper.selectById(id);
        if (c == null) return Result.fail("规则不存在");
        return Result.ok(c);
    }

    @Operation(summary = "新增自动化规则")
    @PostMapping
    public Result<Long> create(@RequestBody AutoCampaign req) {
        validate(req);
        req.setId(null);
        req.setTenantId(tenantId());
        if (req.getStatus() == null) req.setStatus(1);
        if (req.getOncePerMember() == null) req.setOncePerMember(1);
        req.setCreateBy(SecurityUtils.getCurrentUserId());
        req.setCreateTime(LocalDateTime.now());
        req.setUpdateTime(LocalDateTime.now());
        campaignMapper.insert(req);
        return Result.ok(req.getId());
    }

    @Operation(summary = "修改自动化规则")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody AutoCampaign req) {
        validate(req);
        req.setId(id);
        req.setTenantId(null);
        req.setUpdateBy(SecurityUtils.getCurrentUserId());
        req.setUpdateTime(LocalDateTime.now());
        return Result.ok(campaignMapper.updateById(req) > 0);
    }

    @Operation(summary = "删除自动化规则")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(campaignMapper.deleteById(id) > 0);
    }

    @Operation(summary = "启用/停用规则")
    @PostMapping("/{id}/status")
    public Result<Boolean> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        AutoCampaign c = new AutoCampaign().setId(id).setStatus(status).setUpdateTime(LocalDateTime.now());
        return Result.ok(campaignMapper.updateById(c) > 0);
    }

    @Operation(summary = "候选会员预览（不执行动作）")
    @GetMapping("/{id}/candidates")
    public Result<List<AutoCampaignCandidate>> candidates(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "200") Integer limit) {
        AutoCampaign c = campaignMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("规则不存在");
        List<AutoCampaignCandidate> rows = autoCampaignService.candidates(c, limit);
        for (AutoCampaignCandidate row : rows) {
            row.setMobile(DesensitizeUtils.mobile(row.getMobile()));
        }
        return Result.ok(rows);
    }

    @Operation(summary = "立即执行一次规则")
    @PostMapping("/{id}/run")
    public Result<Map<String, Object>> run(@PathVariable Long id,
                                           @RequestParam(defaultValue = "500") Integer limit) {
        return Result.ok(autoCampaignService.run(id, limit));
    }

    @Operation(summary = "执行全部启用规则（定时任务入口）")
    @PostMapping("/run-all")
    public Result<Map<String, Object>> runAll(@RequestParam(defaultValue = "500") Integer limitPerCampaign) {
        return Result.ok(autoCampaignService.runAll(limitPerCampaign));
    }

    @Operation(summary = "规则执行概况（规则数/启用数/近 7 日触达）")
    @GetMapping("/stat")
    public Result<Map<String, Object>> stat() {
        Long total = campaignMapper.selectCount(new LambdaQueryWrapper<>());
        Long enabled = campaignMapper.selectCount(new LambdaQueryWrapper<AutoCampaign>()
                .eq(AutoCampaign::getStatus, 1));
        Long recent = logMapper.selectCount(new LambdaQueryWrapper<AutoCampaignLog>()
                .ge(AutoCampaignLog::getCreateTime, LocalDateTime.now().minusDays(7))
                .eq(AutoCampaignLog::getResult, AutoCampaignLog.SUCCESS));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("enabled", enabled);
        out.put("recentSuccess", recent);
        return Result.ok(out);
    }

    // ══════ Tab2 执行记录 ══════

    @Operation(summary = "分页查询执行记录")
    @GetMapping("/log/page")
    public Result<IPage<AutoCampaignLog>> logPage(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String memberName,
            @RequestParam(required = false) String triggerType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<AutoCampaignLog> w = new LambdaQueryWrapper<>();
        if (campaignId != null) w.eq(AutoCampaignLog::getCampaignId, campaignId);
        if (result != null && !result.isEmpty()) w.eq(AutoCampaignLog::getResult, result);
        if (memberName != null && !memberName.isEmpty()) w.like(AutoCampaignLog::getMemberName, memberName);
        if (triggerType != null && !triggerType.isEmpty()) w.eq(AutoCampaignLog::getTriggerType, triggerType);
        w.orderByDesc(AutoCampaignLog::getCreateTime);
        IPage<AutoCampaignLog> page = logMapper.selectPage(new Page<>(pageNum, pageSize), w);
        for (AutoCampaignLog row : page.getRecords()) {
            row.setMobile(DesensitizeUtils.mobile(row.getMobile()));
        }
        return Result.ok(page);
    }

    // ══════ 内部 ══════

    private void validate(AutoCampaign req) {
        if (req.getName() == null || req.getName().isBlank()) throw new IllegalArgumentException("规则名称不能为空");
        if (req.getTriggerType() == null || req.getTriggerType().isBlank()) {
            throw new IllegalArgumentException("请选择触发点");
        }
        if (req.getActionType() == null || req.getActionType().isBlank()) {
            throw new IllegalArgumentException("请选择动作");
        }
        if (AutoCampaign.ACTION_COUPON.equals(req.getActionType()) && req.getCouponTemplateId() == null) {
            throw new IllegalArgumentException("动作为「发优惠券」时必须选择券模板");
        }
        if (AutoCampaign.ACTION_SMS.equals(req.getActionType())
                && (req.getSmsContent() == null || req.getSmsContent().isBlank())) {
            throw new IllegalArgumentException("动作为「发短信」时必须填写短信内容");
        }
        if (AutoCampaign.ACTION_POINTS.equals(req.getActionType())
                && (req.getPointsValue() == null || req.getPointsValue().signum() <= 0)) {
            throw new IllegalArgumentException("动作为「赠积分」时必须填写正数积分");
        }
    }
}
