package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.base.service.SysConfigService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.erp.marketing.dto.AutoCampaignCandidate;
import cn.aiedge.erp.marketing.entity.AutoCampaign;
import cn.aiedge.erp.marketing.entity.AutoCampaignLog;
import cn.aiedge.erp.marketing.entity.PointsBatch;
import cn.aiedge.erp.marketing.mapper.AutoCampaignLogMapper;
import cn.aiedge.erp.marketing.mapper.AutoCampaignMapper;
import cn.aiedge.erp.marketing.mapper.AutoCampaignQueryMapper;
import cn.aiedge.erp.marketing.service.AutoCampaignService;
import cn.aiedge.erp.marketing.service.CouponTemplateService;
import cn.aiedge.erp.marketing.service.PointsLedgerService;
import cn.aiedge.erp.marketing.service.SmsSendService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoCampaignServiceImpl implements AutoCampaignService {

    private final AutoCampaignMapper campaignMapper;
    private final AutoCampaignLogMapper logMapper;
    private final AutoCampaignQueryMapper queryMapper;
    private final CouponTemplateService couponTemplateService;
    private final SmsSendService smsSendService;
    private final PointsLedgerService pointsLedgerService;
    /** 平台系统参数服务（全域频控参数从 sys_project_config 读，不重复造配置读取） */
    private final SysConfigService sysConfigService;

    private static final int DEFAULT_DAYS = 30;
    /** 全域频控默认：1 天内最多被任意规则触达 2 次（可用全局系统配置覆盖） */
    private static final int GLOBAL_FREQ_DAYS = 1;
    private static final int GLOBAL_FREQ_CAP = 2;
    /** 全域频控参数键（系统参数表 sys_project_config，可在「系统参数」页维护） */
    private static final String CFG_GLOBAL_FREQ_DAYS = "marketing.autoCampaign.globalFreqDays";
    private static final String CFG_GLOBAL_FREQ_CAP = "marketing.autoCampaign.globalFreqCap";

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    @Override
    public List<AutoCampaignCandidate> candidates(AutoCampaign c, int limit) {
        Long tid = c.getTenantId() == null ? tenantId() : c.getTenantId();
        int days = c.getTriggerDays() == null || c.getTriggerDays() <= 0 ? DEFAULT_DAYS : c.getTriggerDays();
        int lim = Math.min(Math.max(limit, 1), 2000);
        String type = c.getTriggerType();
        if (AutoCampaign.TRIGGER_NEW_CUSTOMER.equals(type)) {
            return queryMapper.selectNewCustomer(tid, days, lim);
        }
        if (AutoCampaign.TRIGGER_BIRTHDAY.equals(type)) {
            return queryMapper.selectBirthday(tid, days, lim);
        }
        if (AutoCampaign.TRIGGER_SLEEPING.equals(type)) {
            return queryMapper.selectSleeping(tid, days, lim);
        }
        if (AutoCampaign.TRIGGER_REPURCHASE.equals(type)) {
            return queryMapper.selectRepurchase(tid, days, lim);
        }
        if (AutoCampaign.TRIGGER_CARD_EXPIRING.equals(type)) {
            return queryMapper.selectCardExpiring(tid, days, lim);
        }
        if (AutoCampaign.TRIGGER_POINTS_EXPIRING.equals(type)) {
            // 积分即将过期：复用积分台账（批次级），按会员卡号聚合成候选
            List<PointsBatch> batches = pointsLedgerService.expiringSoon(days);
            Map<String, AutoCampaignCandidate> agg = new LinkedHashMap<>();
            for (PointsBatch b : batches) {
                if (b.getMemberCardNo() == null) continue;
                AutoCampaignCandidate cand = agg.computeIfAbsent(b.getMemberCardNo(), k -> {
                    AutoCampaignCandidate x = new AutoCampaignCandidate();
                    x.setMemberCardNo(b.getMemberCardNo());
                    x.setPartnerId(b.getPartnerId());
                    return x;
                });
                BigDecimal prev = BigDecimal.ZERO;
                if (cand.getTriggerNote() != null) {
                    prev = new BigDecimal(cand.getTriggerNote().replaceAll("[^0-9.]", ""));
                }
                cand.setTriggerNote("积分 " + prev.add(nvl(b.getRemainingPoints())).stripTrailingZeros().toPlainString()
                        + " 分将于 " + (b.getExpireTime() == null ? "-" : b.getExpireTime().toLocalDate()) + " 过期");
                cand.setKeyDate(b.getExpireTime() == null ? null : b.getExpireTime().toLocalDate().toString());
            }
            // 补齐会员姓名/手机号（候选展示需要）
            for (AutoCampaignCandidate cand : agg.values()) {
                Map<String, Object> info = loadMemberInfo(cand.getMemberCardNo());
                if (info != null) {
                    cand.setPartyCode(str(info.get("party_code")));
                    cand.setPartyName(str(info.get("party_name")));
                    cand.setMemberName(str(info.get("member_name")));
                    cand.setMobile(str(info.get("mobile")));
                    if (cand.getPartnerId() == null && info.get("id") != null) {
                        cand.setPartnerId(Long.valueOf(String.valueOf(info.get("id"))));
                    }
                }
            }
            List<AutoCampaignCandidate> out = new ArrayList<>(agg.values());
            return out.size() > lim ? out.subList(0, lim) : out;
        }
        throw new IllegalArgumentException("不支持的触发点：" + type);
    }

    @Override
    public Map<String, Object> run(Long campaignId, int limit) {
        AutoCampaign c = campaignMapper.selectById(campaignId);
        if (c == null) throw new IllegalArgumentException("自动化规则不存在");
        if (!Integer.valueOf(1).equals(c.getStatus())) {
            throw new IllegalArgumentException("规则「" + c.getName() + "」已停用，请先启用");
        }
        return execute(c, limit);
    }

    @Override
    public Map<String, Object> runAll(int limitPerCampaign) {
        List<AutoCampaign> list = campaignMapper.selectList(new LambdaQueryWrapper<AutoCampaign>()
                .eq(AutoCampaign::getStatus, 1));
        int total = 0;
        int failed = 0;
        for (AutoCampaign c : list) {
            try {
                Map<String, Object> r = execute(c, limitPerCampaign);
                total += ((Number) r.get("success")).intValue();
            } catch (Exception e) {
                failed++;
                log.error("[营销自动化] 规则「{}」执行失败", c.getName(), e);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("campaignCount", list.size());
        out.put("success", total);
        out.put("failedCampaigns", failed);
        return out;
    }

    @Override
    public int candidatesCount(Long tenantId) {
        List<AutoCampaign> list = campaignMapper.selectList(new LambdaQueryWrapper<AutoCampaign>()
                .eq(AutoCampaign::getStatus, 1));
        int total = 0;
        for (AutoCampaign c : list) {
            AutoCampaign scoped = new AutoCampaign();
            scoped.setId(c.getId());
            scoped.setName(c.getName());
            scoped.setTenantId(tenantId);
            scoped.setTriggerType(c.getTriggerType());
            scoped.setTriggerDays(c.getTriggerDays());
            try {
                total += candidates(scoped, 500).size();
            } catch (Exception e) {
                log.warn("[营销自动化] 规则「{}」候选统计失败：{}", c.getName(), e.getMessage());
            }
        }
        return total;
    }

    // ══════════════ 内部 ══════════════

    /**
     * 执行一条规则。
     * ⚠️ 逐会员处理：单个会员触达失败**不影响**其余会员（各自独立事务由动作内部保证），
     * 失败原因写入执行台账的 resultMsg。
     */
    private Map<String, Object> execute(AutoCampaign c, int limit) {
        int lim = Math.min(Math.max(limit, 1), 2000);
        List<AutoCampaignCandidate> cands = candidates(c, lim);
        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (AutoCampaignCandidate cand : cands) {
            String skipReason = skipReason(c, cand);
            if (skipReason != null) {
                writeLog(c, cand, AutoCampaignLog.SKIPPED, skipReason, null);
                skipped++;
                continue;
            }
            try {
                String msg = doAction(c, cand);
                writeLog(c, cand, AutoCampaignLog.SUCCESS, msg, null);
                success++;
            } catch (Exception e) {
                writeLog(c, cand, AutoCampaignLog.FAILED, e.getMessage(), null);
                failed++;
                log.warn("[营销自动化] 规则「{}」对会员 {} 触达失败：{}", c.getName(), cand.getPartnerId(), e.getMessage());
            }
        }

        c.setLastRunTime(LocalDateTime.now());
        c.setLastRunCount(cands.size());
        c.setLastRunSuccess(success);
        campaignMapper.updateById(c);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("campaignId", c.getId());
        out.put("campaignName", c.getName());
        out.put("candidateCount", cands.size());
        out.put("success", success);
        out.put("skipped", skipped);
        out.put("failed", failed);
        log.info("[营销自动化] 规则「{}」执行完成：候选 {} / 成功 {} / 跳过 {} / 失败 {}",
                c.getName(), cands.size(), success, skipped, failed);
        return out;
    }

    /** 频控与「只触发一次」判定；返回 null 表示可执行 */
    private String skipReason(AutoCampaign c, AutoCampaignCandidate cand) {
        Long tid = c.getTenantId() == null ? tenantId() : c.getTenantId();
        if (Integer.valueOf(1).equals(c.getOncePerMember())) {
            Long done = logMapper.selectCount(new LambdaQueryWrapper<AutoCampaignLog>()
                    .eq(AutoCampaignLog::getCampaignId, c.getId())
                    .eq(AutoCampaignLog::getPartnerId, cand.getPartnerId())
                    .eq(AutoCampaignLog::getResult, AutoCampaignLog.SUCCESS));
            if (done != null && done > 0) return "该会员已触发过（规则设为每人仅一次）";
        }
        int freqDays = c.getFreqDays() == null ? 0 : c.getFreqDays();
        int freqCount = c.getFreqCount() == null ? 0 : c.getFreqCount();
        if (freqDays > 0 && freqCount > 0) {
            Long cnt = logMapper.selectCount(new LambdaQueryWrapper<AutoCampaignLog>()
                    .eq(AutoCampaignLog::getCampaignId, c.getId())
                    .eq(AutoCampaignLog::getPartnerId, cand.getPartnerId())
                    .eq(AutoCampaignLog::getResult, AutoCampaignLog.SUCCESS)
                    .ge(AutoCampaignLog::getCreateTime, LocalDateTime.now().minusDays(freqDays)));
            if (cnt != null && cnt >= freqCount) return "频控拦截（" + freqDays + " 天内已达 " + freqCount + " 次）";
        }
        if (cand.getPartnerId() == null) return "缺少客户标识，无法触达";

        // ── 全域频控（跨规则）：同一会员在 N 天内被**任意**规则成功触达达到上限即跳过 ──
        // 单规则频控只管得住"同一条规则不重复骚扰"，管不住"多条规则同一天一起发"——
        // 后者才是真实骚扰来源（业界称 frequency cap / 全域触达上限）。
        int globalDays = sysConfigService.getIntValue(CFG_GLOBAL_FREQ_DAYS, GLOBAL_FREQ_DAYS);
        int globalCap = sysConfigService.getIntValue(CFG_GLOBAL_FREQ_CAP, GLOBAL_FREQ_CAP);
        if (globalDays > 0 && globalCap > 0) {
            Long globalCnt = logMapper.selectCount(new LambdaQueryWrapper<AutoCampaignLog>()
                    .eq(AutoCampaignLog::getPartnerId, cand.getPartnerId())
                    .eq(AutoCampaignLog::getResult, AutoCampaignLog.SUCCESS)
                    .ge(AutoCampaignLog::getCreateTime, LocalDateTime.now().minusDays(globalDays)));
            if (globalCnt != null && globalCnt >= globalCap) {
                return "全域频控拦截（" + globalDays + " 天内已被触达 " + globalCnt + " 次，上限 " + globalCap + "）";
            }
        }
        return null;
    }



    /** 执行动作：复用券发放 / 短信群发 / 积分台账（不重复实现提交与合规逻辑） */
    private String doAction(AutoCampaign c, AutoCampaignCandidate cand) {
        Long tid = c.getTenantId() == null ? tenantId() : c.getTenantId();
        String type = c.getActionType();
        if (AutoCampaign.ACTION_COUPON.equals(type)) {
            if (c.getCouponTemplateId() == null) throw new IllegalArgumentException("未配置优惠券模板");
            int n = couponTemplateService.issue(c.getCouponTemplateId(), List.of(cand.getPartnerId()), 1,
                    "自动化:" + c.getName());
            return "已发放优惠券 " + n + " 张";
        }
        if (AutoCampaign.ACTION_SMS.equals(type)) {
            if (cand.getMobile() == null || cand.getMobile().isBlank()) {
                throw new IllegalArgumentException("该会员无手机号，无法发送短信");
            }
            String content = c.getSmsContent();
            if (content == null || content.isBlank()) throw new IllegalArgumentException("未配置短信内容");
            // 模板占位符渲染（{会员名称} 等；未匹配的占位符原样保留以便排障）
            content = content.replace("{会员名称}", nvl(cand.getMemberName(), cand.getPartyName()))
                    .replace("{客户名称}", nvl(cand.getPartyName(), ""))
                    .replace("{会员卡号}", nvl(cand.getMemberCardNo(), ""));
            Map<String, Object> r = smsSendService.sendBatch(tid, List.of(cand.getPartnerId()),
                    content, "自动化", "NOTICE", "系统自动化", "自动化:" + c.getName());
            return "短信已入队（批次 " + r.get("batchNo") + "）";
        }
        if (AutoCampaign.ACTION_POINTS.equals(type)) {
            if (c.getPointsValue() == null || c.getPointsValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("未配置赠送积分");
            }
            if (cand.getMemberCardNo() == null || cand.getMemberCardNo().isBlank()) {
                throw new IllegalArgumentException("该会员无会员卡号，无法赠送积分");
            }
            pointsLedgerService.earn(cand.getMemberCardNo(), cand.getPartnerId(), c.getPointsValue(),
                    "GIFT", "自动化:" + c.getName());
            return "已赠送积分 " + c.getPointsValue().stripTrailingZeros().toPlainString();
        }
        throw new IllegalArgumentException("不支持的动作类型：" + type);
    }

    private void writeLog(AutoCampaign c, AutoCampaignCandidate cand, String result, String msg, String batchNo) {
        logMapper.insert(new AutoCampaignLog()
                .setTenantId(c.getTenantId() == null ? tenantId() : c.getTenantId())
                .setCampaignId(c.getId())
                .setCampaignName(c.getName())
                .setTriggerType(c.getTriggerType())
                .setActionType(c.getActionType())
                .setPartnerId(cand.getPartnerId())
                .setMemberName(nvl(cand.getMemberName(), cand.getPartyName()))
                .setMobile(cand.getMobile())
                .setResult(result)
                .setResultMsg(msg)
                .setBatchNo(batchNo)
                .setTriggerNote(cand.getTriggerNote())
                .setCreateTime(LocalDateTime.now()));
    }

    /** 按会员卡号补会员基础信息（积分到期触发的候选来自批次表，缺姓名/手机号） */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    protected Map<String, Object> loadMemberInfo(String memberCardNo) {
        return queryMapper.selectMemberByCard(memberCardNo, tenantId());
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }

    private static String nvl(String s, String dft) { return s == null || s.isBlank() ? dft : s; }
}
