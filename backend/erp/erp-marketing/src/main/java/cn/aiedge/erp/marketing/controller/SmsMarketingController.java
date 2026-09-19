package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.service.MessageService;
import cn.aiedge.base.util.DesensitizeUtils;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.SmsHistoryRow;
import cn.aiedge.erp.marketing.entity.SmsRecord;
import cn.aiedge.erp.marketing.entity.SmsSetting;
import cn.aiedge.erp.marketing.entity.SmsTemplate;
import cn.aiedge.erp.marketing.mapper.MarketingQueryMapper;
import cn.aiedge.erp.marketing.mapper.SmsRecordMapper;
import cn.aiedge.erp.marketing.mapper.SmsSettingMapper;
import cn.aiedge.erp.marketing.mapper.SmsTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发短信（营销 → 营销活动 → 发短信，菜单 80310）
 * 三 Tab：`发短信`（表单群发）/ `短信历史`（投递台账）/ `短信模板管理`
 *
 * <p>发送统一走平台消息底座 {@link MessageService#sendSms}（落 sys_message，由 MessageSendTask 消费与重试），
 * 本模块只留营销台账（mkt_sms_record）以承载「经手人 / 发送批次」两个营销域事实，
 * 投递状态与失败原因实时 join sys_message 读取（单一真源，不重复存储）。</p>
 */
@Slf4j
@Tag(name = "营销短信")
@RestController
@RequestMapping("/api/erp/marketing/sms")
@RequiredArgsConstructor
public class SmsMarketingController {

    private final SmsSettingMapper settingMapper;
    private final SmsTemplateMapper templateMapper;
    private final SmsRecordMapper recordMapper;
    private final MarketingQueryMapper marketingQueryMapper;
    private final MessageService messageService;
    private final cn.aiedge.erp.marketing.service.SmsSendService smsSendService;
    private final cn.aiedge.erp.marketing.mapper.SmsOptOutMapper smsOptOutMapper;
    private final cn.aiedge.erp.marketing.mapper.SmsConsentMapper smsConsentMapper;

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    // ══════ 短信设置（公司签名 + 配额） ══════

    @Operation(summary = "读取短信设置")
    @GetMapping("/setting")
    public Result<SmsSetting> getSetting() {
        SmsSetting setting = loadOrCreateSetting();
        return Result.ok(withRemain(setting));
    }

    @Operation(summary = "保存短信设置")
    @PutMapping("/setting")
    public Result<SmsSetting> saveSetting(@RequestBody SmsSetting req) {
        SmsSetting current = loadOrCreateSetting();
        req.setId(current.getId());
        req.setTenantId(current.getTenantId());
        req.setUpdateTime(LocalDateTime.now());
        settingMapper.updateById(req);
        return Result.ok(withRemain(settingMapper.selectById(current.getId())));
    }

    private SmsSetting loadOrCreateSetting() {
        Long tid = tenantId();
        SmsSetting setting = settingMapper.selectOne(new LambdaQueryWrapper<SmsSetting>()
                .eq(SmsSetting::getTenantId, tid).orderByAsc(SmsSetting::getId).last("limit 1"));
        if (setting != null) return setting;
        SmsSetting created = new SmsSetting()
                .setTenantId(tid)
                .setQuotaTotal(100)
                .setQuotaUsed(0)
                // 合规默认：8:00–21:00 发送；7 天内同一号码最多 3 条
                .setSendStartHour(8)
                .setSendEndHour(21)
                .setFreqLimitDays(7)
                .setFreqLimitCount(3)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        settingMapper.insert(created);
        log.info("[营销短信] 租户 {} 首次读取短信设置，已建默认行（配额 100 条）id={}", tid, created.getId());
        return created;
    }

    /**
     * 补齐派生值与合规默认值。
     * ⚠️ 合规四项（发送时段/频控）必须在**读取时**补默认：V11.379.1 之前建好的设置行这几列是 NULL，
     * 只在建行时设默认会让存量租户拿到 null → 前端显示空、后端按兜底值走，口径不一致。
     */
    private SmsSetting withRemain(SmsSetting s) {
        if (s == null) return null;
        int total = s.getQuotaTotal() == null ? 0 : s.getQuotaTotal();
        int used = s.getQuotaUsed() == null ? 0 : s.getQuotaUsed();
        s.setQuotaRemain(Math.max(total - used, 0));
        if (s.getSendStartHour() == null) s.setSendStartHour(8);
        if (s.getSendEndHour() == null) s.setSendEndHour(21);
        if (s.getFreqLimitDays() == null) s.setFreqLimitDays(7);
        if (s.getFreqLimitCount() == null) s.setFreqLimitCount(3);
        return s;
    }

    // ══════ 短信模板管理 ══════

    @Operation(summary = "分页查询短信模板")
    @GetMapping("/template/page")
    public Result<IPage<SmsTemplate>> templatePage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String smsType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<SmsTemplate> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            w.and(x -> x.like(SmsTemplate::getTemplateTitle, keyword)
                    .or().like(SmsTemplate::getTemplateContent, keyword));
        }
        if (smsType != null && !smsType.isEmpty()) w.eq(SmsTemplate::getSmsType, smsType);
        w.orderByDesc(SmsTemplate::getUpdateTime).orderByDesc(SmsTemplate::getId);
        return Result.ok(templateMapper.selectPage(new Page<>(pageNum, pageSize), w));
    }

    @Operation(summary = "查询全部启用短信模板（供「选择短信模板」）")
    @GetMapping("/template/list")
    public Result<List<SmsTemplate>> templateList(@RequestParam(required = false) String smsType) {
        LambdaQueryWrapper<SmsTemplate> w = new LambdaQueryWrapper<>();
        if (smsType != null && !smsType.isEmpty()) w.eq(SmsTemplate::getSmsType, smsType);
        w.orderByDesc(SmsTemplate::getUpdateTime);
        return Result.ok(templateMapper.selectList(w));
    }

    @Operation(summary = "新增短信模板")
    @PostMapping("/template")
    public Result<Boolean> createTemplate(@RequestBody SmsTemplate tpl) {
        tpl.setId(null);
        tpl.setTenantId(tenantId());
        if (tpl.getSmsType() == null) tpl.setSmsType("NOTICE");
        if (tpl.getStatus() == null) tpl.setStatus(1);
        tpl.setCreateTime(LocalDateTime.now());
        tpl.setUpdateTime(LocalDateTime.now());
        return Result.ok(templateMapper.insert(tpl) > 0);
    }

    @Operation(summary = "修改短信模板")
    @PutMapping("/template/{id}")
    public Result<Boolean> updateTemplate(@PathVariable Long id, @RequestBody SmsTemplate tpl) {
        tpl.setId(id);
        tpl.setTenantId(null);
        tpl.setUpdateTime(LocalDateTime.now());
        return Result.ok(templateMapper.updateById(tpl) > 0);
    }

    @Operation(summary = "删除短信模板")
    @DeleteMapping("/template/{id}")
    public Result<Boolean> deleteTemplate(@PathVariable Long id) {
        return Result.ok(templateMapper.deleteById(id) > 0);
    }

    // ══════ 短信历史 ══════

    @Operation(summary = "分页查询短信历史")
    @GetMapping("/history/page")
    public Result<IPage<SmsHistoryRow>> historyPage(
            @RequestParam(required = false) String receiver,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String smsType,
            @RequestParam(required = false) String sendStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<SmsHistoryRow> page = recordMapper.selectHistoryPage(
                new Page<>(pageNum, pageSize), tenantId(), receiver, mobile, smsType, sendStatus);
        for (SmsHistoryRow row : page.getRecords()) {
            row.setMobile(DesensitizeUtils.mobile(row.getMobile()));
        }
        return Result.ok(page);
    }

    // ══════ 合规：退订名单 / 同意留痕 ══════

    @Operation(summary = "分页查询短信退订名单")
    @GetMapping("/opt-out/page")
    public Result<IPage<cn.aiedge.erp.marketing.entity.SmsOptOut>> optOutPage(
            @RequestParam(required = false) String mobile,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<cn.aiedge.erp.marketing.entity.SmsOptOut> w = new LambdaQueryWrapper<>();
        if (mobile != null && !mobile.isEmpty()) w.like(cn.aiedge.erp.marketing.entity.SmsOptOut::getMobile, mobile);
        w.orderByDesc(cn.aiedge.erp.marketing.entity.SmsOptOut::getOptOutTime);
        IPage<cn.aiedge.erp.marketing.entity.SmsOptOut> page =
                smsOptOutMapper.selectPage(new Page<>(pageNum, pageSize), w);
        for (cn.aiedge.erp.marketing.entity.SmsOptOut row : page.getRecords()) {
            row.setMobile(DesensitizeUtils.mobile(row.getMobile()));
        }
        return Result.ok(page);
    }

    @Operation(summary = "登记退订（回复R / 人工登记 / 客户主动要求）")
    @PostMapping("/opt-out")
    public Result<Boolean> addOptOut(@RequestBody cn.aiedge.erp.marketing.entity.SmsOptOut req) {
        if (req.getMobile() == null || req.getMobile().isBlank()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        Long dup = smsOptOutMapper.selectCount(
                new LambdaQueryWrapper<cn.aiedge.erp.marketing.entity.SmsOptOut>()
                        .eq(cn.aiedge.erp.marketing.entity.SmsOptOut::getMobile, req.getMobile()));
        if (dup != null && dup > 0) {
            throw new IllegalArgumentException("该号码已在退订名单中");
        }
        req.setId(null);
        req.setTenantId(tenantId());
        if (req.getSource() == null) req.setSource("MANUAL");
        req.setOptOutTime(LocalDateTime.now());
        req.setCreateTime(LocalDateTime.now());
        req.setDeleted(0);
        return Result.ok(smsOptOutMapper.insert(req) > 0);
    }

    @Operation(summary = "移出退订名单（须客户重新明确同意后才可移出）")
    @DeleteMapping("/opt-out/{id}")
    public Result<Boolean> removeOptOut(@PathVariable Long id) {
        return Result.ok(smsOptOutMapper.deleteById(id) > 0);
    }

    @Operation(summary = "分页查询同意留痕（合规举证）")
    @GetMapping("/consent/page")
    public Result<IPage<cn.aiedge.erp.marketing.entity.SmsConsent>> consentPage(
            @RequestParam(required = false) String mobile,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<cn.aiedge.erp.marketing.entity.SmsConsent> w = new LambdaQueryWrapper<>();
        if (mobile != null && !mobile.isEmpty()) w.like(cn.aiedge.erp.marketing.entity.SmsConsent::getMobile, mobile);
        w.orderByDesc(cn.aiedge.erp.marketing.entity.SmsConsent::getAgreedTime);
        IPage<cn.aiedge.erp.marketing.entity.SmsConsent> page =
                smsConsentMapper.selectPage(new Page<>(pageNum, pageSize), w);
        for (cn.aiedge.erp.marketing.entity.SmsConsent row : page.getRecords()) {
            row.setMobile(DesensitizeUtils.mobile(row.getMobile()));
        }
        return Result.ok(page);
    }

    // ══════ 发短信（群发） ══════

    @Operation(summary = "群发短信（对选中客户逐个入队发送）")
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestBody SendRequest req) {
        if (!Boolean.TRUE.equals(req.getAgreed())) {
            throw new IllegalArgumentException("请先勾选同意短信协议");
        }
        // 合规校验与入队统一在 SmsSendService（与营销自动化共用同一实现，避免两处口径不一致）
        Map<String, Object> out = smsSendService.sendBatch(
                tenantId(), req.getPartnerIds(), req.getContent(), req.getSignName(),
                req.getSmsType(), SecurityUtils.getCurrentUsername(), "手工群发");
        return Result.ok(out);
    }

    @Data
    public static class SendRequest {
        private List<Long> partnerIds;
        private String content;
        private String signName;
        private String smsType;
        private Boolean agreed;
    }
}
