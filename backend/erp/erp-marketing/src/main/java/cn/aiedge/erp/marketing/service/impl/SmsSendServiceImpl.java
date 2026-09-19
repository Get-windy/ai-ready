package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.base.service.MessageService;
import cn.aiedge.erp.marketing.entity.SmsConsent;
import cn.aiedge.erp.marketing.entity.SmsRecord;
import cn.aiedge.erp.marketing.entity.SmsSetting;
import cn.aiedge.erp.marketing.mapper.MarketingQueryMapper;
import cn.aiedge.erp.marketing.mapper.SmsConsentMapper;
import cn.aiedge.erp.marketing.mapper.SmsOptOutMapper;
import cn.aiedge.erp.marketing.mapper.SmsRecordMapper;
import cn.aiedge.erp.marketing.mapper.SmsSettingMapper;
import cn.aiedge.erp.marketing.entity.SmsOptOut;
import cn.aiedge.erp.marketing.service.SmsSendService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 营销短信群发实现：手工群发（控制器）与营销自动化共用。
 * 合规口径见 {@link SmsSendService}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsSendServiceImpl implements SmsSendService {

    /** 协议版本号：协议文本变更时递增（举证"当时同意的是哪一版"） */
    private static final String AGREEMENT_VERSION = "v1.0";

    private final SmsSettingMapper settingMapper;
    private final SmsRecordMapper recordMapper;
    private final SmsOptOutMapper smsOptOutMapper;
    private final SmsConsentMapper smsConsentMapper;
    private final MarketingQueryMapper marketingQueryMapper;
    private final MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendBatch(Long tenantId, List<Long> partnerIds, String content, String signName,
                                         String smsType, String handlerName, String source) {
        if (partnerIds == null || partnerIds.isEmpty()) {
            throw new IllegalArgumentException("请先选择客户");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("请填写短信内容");
        }
        if (signName == null || signName.isBlank()) {
            throw new IllegalArgumentException("公司签名不能为空");
        }
        Long tid = tenantId == null ? 1L : tenantId;

        SmsSetting setting = loadOrCreateSetting(tid);
        List<Map<String, Object>> contacts = marketingQueryMapper.selectPartnerContacts(tid, partnerIds);
        List<Map<String, Object>> reachable = new ArrayList<>();
        for (Map<String, Object> c : contacts) {
            Object mobile = c.get("mobile");
            if (mobile != null && !String.valueOf(mobile).isBlank()) reachable.add(c);
        }
        if (reachable.isEmpty()) {
            throw new IllegalArgumentException("所选客户均无可用手机号，无法发送");
        }

        // 合规前置 ①：全局发送时段（先判，否则会被逐收件人过滤的报错掩盖真实原因）
        assertSendWindow(setting);
        // 合规前置 ②：退订名单 + 频控
        int totalBefore = reachable.size();
        reachable.removeIf(c -> {
            String m = String.valueOf(c.get("mobile"));
            return isOptedOut(m) || isOverFrequency(m, setting);
        });
        int skippedByCompliance = totalBefore - reachable.size();
        if (reachable.isEmpty()) {
            throw new IllegalArgumentException("所选客户均已退订或超出频控限制，无可发送对象");
        }
        int remain = setting.getQuotaRemain() != null ? setting.getQuotaRemain()
                : (nz(setting.getQuotaTotal()) - nz(setting.getQuotaUsed()));
        if (remain < reachable.size()) {
            throw new IllegalArgumentException("剩余短信 " + remain + " 条，不足本次发送 " + reachable.size() + " 条");
        }

        String type = smsType == null ? "NOTICE" : smsType;
        String suffix = "MARKETING".equals(type) ? "拒收请回复R" : "";
        String fullContent = suffix.isEmpty() ? content : content + suffix;
        String batchNo = "SMS" + System.currentTimeMillis();
        String complianceNote = "时段 " + nzHour(setting.getSendStartHour(), 8) + ":00-"
                + nzHour(setting.getSendEndHour(), 21) + ":00；频控 "
                + nz(setting.getFreqLimitDays(), 7) + "天/" + nz(setting.getFreqLimitCount(), 3)
                + "条；退订名单已过滤" + (source == null || source.isBlank() ? "" : "；来源 " + source);

        int sent = 0;
        for (Map<String, Object> c : reachable) {
            String mobile = String.valueOf(c.get("mobile"));
            String receiverName = c.get("partner_name") == null ? "" : String.valueOf(c.get("partner_name"));
            Long receiverId = c.get("partner_id") == null ? null : Long.valueOf(String.valueOf(c.get("partner_id")));
            Long messageId = messageService.sendSms(null, mobile, "营销短信", fullContent, "MARKETING_SMS", null);
            recordMapper.insert(new SmsRecord()
                    .setTenantId(tid)
                    .setBatchNo(batchNo)
                    .setMessageId(messageId)
                    .setReceiverId(receiverId)
                    .setReceiverName(receiverName)
                    .setMobile(mobile)
                    .setContent(content)
                    .setSmsType(type)
                    .setSignName(signName)
                    .setHandlerName(handlerName)
                    .setComplianceNote(complianceNote)
                    .setCreateTime(LocalDateTime.now())
                    .setDeleted(0));
            // 同意留痕（合规举证）：谁 · 何时 · 为哪个号码 · 同意哪版协议
            smsConsentMapper.insert(new SmsConsent()
                    .setTenantId(tid)
                    .setPartnerId(receiverId)
                    .setMobile(mobile)
                    .setAgreementVersion(AGREEMENT_VERSION)
                    .setAgreedTime(LocalDateTime.now())
                    .setAgreedBy(handlerName)
                    .setCreateTime(LocalDateTime.now())
                    .setDeleted(0));
            sent++;
        }

        settingMapper.update(null, new LambdaUpdateWrapper<SmsSetting>()
                .eq(SmsSetting::getId, setting.getId())
                .set(SmsSetting::getQuotaUsed, nz(setting.getQuotaUsed()) + sent)
                .set(SmsSetting::getQuotaTotal,
                        setting.getQuotaTotal() == null ? Math.max(sent, 0) : setting.getQuotaTotal())
                .set(SmsSetting::getUpdateTime, LocalDateTime.now()));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("batchNo", batchNo);
        out.put("sentCount", sent);
        out.put("skippedCount", contacts.size() - reachable.size());
        out.put("skippedByCompliance", skippedByCompliance);
        out.put("failedCount", 0);
        out.put("remainQuota", remain - sent);
        log.info("[营销短信] 批次 {} 入队 {} 条（跳过无手机号 {} 条 / 合规过滤 {} 条）来源={}",
                batchNo, sent, contacts.size() - totalBefore, skippedByCompliance, source);
        return out;
    }

    // ══════════════ 合规 ══════════════

    private SmsSetting loadOrCreateSetting(Long tenantId) {
        SmsSetting setting = settingMapper.selectOne(new LambdaQueryWrapper<SmsSetting>()
                .eq(SmsSetting::getTenantId, tenantId).orderByAsc(SmsSetting::getId).last("limit 1"));
        if (setting != null) return withDefaults(setting);
        SmsSetting created = new SmsSetting()
                .setTenantId(tenantId)
                .setQuotaTotal(100)
                .setQuotaUsed(0)
                .setSendStartHour(8)
                .setSendEndHour(21)
                .setFreqLimitDays(7)
                .setFreqLimitCount(3)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        settingMapper.insert(created);
        return withDefaults(created);
    }

    /** 读取时补齐合规默认（存量行这几列可能为 NULL） */
    private SmsSetting withDefaults(SmsSetting s) {
        int total = nz(s.getQuotaTotal());
        int used = nz(s.getQuotaUsed());
        s.setQuotaRemain(Math.max(total - used, 0));
        if (s.getSendStartHour() == null) s.setSendStartHour(8);
        if (s.getSendEndHour() == null) s.setSendEndHour(21);
        if (s.getFreqLimitDays() == null) s.setFreqLimitDays(7);
        if (s.getFreqLimitCount() == null) s.setFreqLimitCount(3);
        return s;
    }

    private boolean isOptedOut(String mobile) {
        if (mobile == null || mobile.isBlank()) return false;
        Long cnt = smsOptOutMapper.selectCount(new LambdaQueryWrapper<SmsOptOut>()
                .eq(SmsOptOut::getMobile, mobile));
        return cnt != null && cnt > 0;
    }

    private boolean isOverFrequency(String mobile, SmsSetting setting) {
        int days = nz(setting.getFreqLimitDays(), 7);
        int limit = nz(setting.getFreqLimitCount(), 3);
        if (days <= 0 || limit <= 0) return false;
        Long cnt = recordMapper.selectCount(new LambdaQueryWrapper<SmsRecord>()
                .eq(SmsRecord::getMobile, mobile)
                .ge(SmsRecord::getCreateTime, LocalDateTime.now().minusDays(days)));
        return cnt != null && cnt >= limit;
    }

    private void assertSendWindow(SmsSetting setting) {
        int start = nzHour(setting.getSendStartHour(), 8);
        int end = nzHour(setting.getSendEndHour(), 21);
        int hour = LocalDateTime.now().getHour();
        if (hour < start || hour >= end) {
            throw new IllegalArgumentException(String.format(
                    "当前时间 %02d:00 不在允许发送时段（%02d:00–%02d:00）内，请到时段后再发（合规要求）",
                    hour, start, end));
        }
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }

    private static int nz(Integer v, int dft) { return v == null ? dft : v; }

    private static int nzHour(Integer v, int dft) { return v == null ? dft : v; }
}
