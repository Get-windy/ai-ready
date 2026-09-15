package cn.aiedge.dms.settlement.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.mapper.DmsChannelMapper;
import cn.aiedge.dms.config.service.ConfigService;
import cn.aiedge.dms.event.entity.DmsEventOutbox;
import cn.aiedge.dms.event.mapper.DmsEventOutboxMapper;
import cn.aiedge.dms.payment.entity.DmsPayment;
import cn.aiedge.dms.payment.mapper.DmsPaymentMapper;
import cn.aiedge.dms.settlement.entity.DmsSettlement;
import cn.aiedge.dms.settlement.entity.DmsSettlementItem;
import cn.aiedge.dms.settlement.entity.DmsSettlementRule;
import cn.aiedge.dms.settlement.mapper.DmsSettlementItemMapper;
import cn.aiedge.dms.settlement.mapper.DmsSettlementMapper;
import cn.aiedge.dms.sign.entity.DmsSign;
import cn.aiedge.dms.sign.mapper.DmsSignMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.mapper.VoucherItemMapper;
import cn.aiedge.erp.finance.model.entity.VoucherItem;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.PayableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 配送结算服务（金标准）
 *
 * <p>三件事：算得准（一单多少钱）、算得清（周期内谁跑了多少）、付得出（推 ERP 出账）。</p>
 *
 * <p>核心口径：</p>
 * <ul>
 *   <li><b>计费规则</b>：优先命中《计费规则》表（按 结算对象 × 渠道/线路 × 生效期 × 优先级），
 *       未命中回落《配送参数》全局缺省费率（`dms.settlement.*`）—— 规则表为空也能算费；</li>
 *   <li><b>结算单锁定</b>：按「周期 + 结算对象（配送员/渠道）」聚合**已签收/已完成**（status 5/6）任务，
 *       明细记录每单费用构成与签收口径；确认后金额锁定，推送 ERP 幂等；</li>
 *   <li><b>部分签收</b>：按**实际签收数量/应签收数量**折算里程费·重量费·夜间·加急（起步价全收），拒收不计费；</li>
 *   <li><b>业财一体</b>：推送 ERP 真实生成记账凭证与应付（外部运力），以结算单为幂等键，可对账。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final DmsTaskMapper taskMapper;
    private final DmsEventOutboxMapper eventOutboxMapper;
    private final DmsSettlementMapper settlementMapper;
    private final DmsSettlementItemMapper settlementItemMapper;
    private final DmsSignMapper signMapper;
    private final DmsChannelMapper channelMapper;
    private final DmsPaymentMapper paymentMapper;
    private final ConfigService configService;
    private final SettlementRuleService ruleService;
    // ── 业财集成（记账凭证 / 应付） ──
    private final BusinessAccountingService businessAccountingService;
    private final VoucherItemMapper voucherItemMapper;
    private final PayableService payableService;

    /** 缺省计费规则（配置缺失时回落，保证可算费） */
    private static final BigDecimal DEFAULT_BASE_FEE = new BigDecimal("5.00");
    private static final BigDecimal DEFAULT_FREE_DISTANCE_KM = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_PER_KM_RATE = new BigDecimal("2.00");
    private static final BigDecimal DEFAULT_TIME_SURCHARGE_RATE = new BigDecimal("1.50");
    private static final BigDecimal DEFAULT_URGENT_SURCHARGE = new BigDecimal("10.00");
    /** 算费兜底距离（未填配送里程时，与既有行为一致） */
    private static final BigDecimal FALLBACK_DISTANCE = BigDecimal.TEN;
    /** 记账来源类型（凭证/应付的幂等键前缀） */
    private static final String SOURCE_TYPE = "DMS_SETTLEMENT";

    private static final DateTimeFormatter NO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ═══════════════════════════════════════════════
    // 计费规则（全局缺省；规则表命中见 resolveRule）
    // ═══════════════════════════════════════════════

    /** 当前全局缺省计费规则（页面展示；规则表命中时以规则为准） */
    public Map<String, BigDecimal> currentRule() {
        Map<String, BigDecimal> rule = new LinkedHashMap<>();
        rule.put("baseFee", readDecimal("dms.settlement.base.fee", DEFAULT_BASE_FEE));
        rule.put("freeDistanceKm", readDecimal("dms.settlement.free.distance.km", DEFAULT_FREE_DISTANCE_KM));
        rule.put("perKmRate", readDecimal("dms.settlement.per.km.rate", DEFAULT_PER_KM_RATE));
        rule.put("timeSurchargeRate", readDecimal("dms.settlement.time.surcharge.rate", DEFAULT_TIME_SURCHARGE_RATE));
        rule.put("urgentSurcharge", readDecimal("dms.settlement.urgent.surcharge", DEFAULT_URGENT_SURCHARGE));
        return rule;
    }

    /** 读取数值型配置（租户覆盖 → 全局缺省 → 内置缺省） */
    private BigDecimal readDecimal(String key, BigDecimal fallback) {
        try {
            String raw = configService.getString(null, key);
            if (StringUtils.hasText(raw)) {
                return new BigDecimal(raw.trim());
            }
        } catch (Exception e) {
            log.debug("计费配置缺失，回落缺省: key={}, fallback={}", key, fallback);
        }
        return fallback;
    }

    /** 读取文本型配置 */
    private String readString(String key, String fallback) {
        try {
            String raw = configService.getString(null, key);
            if (StringUtils.hasText(raw)) {
                return raw.trim();
            }
        } catch (Exception e) {
            log.debug("配置缺失，回落缺省: key={}, fallback={}", key, fallback);
        }
        return fallback;
    }

    /** 生效的计费规则（命中规则表则整体用规则，否则用全局缺省费率） */
    private static final class FeeRule {
        private Long ruleId;
        private String ruleName;
        private Integer billingType = 2;
        private BigDecimal baseFee = BigDecimal.ZERO;
        private BigDecimal freeDistanceKm = BigDecimal.ZERO;
        private BigDecimal perKmRate = BigDecimal.ZERO;
        private BigDecimal perKgRate = BigDecimal.ZERO;
        private BigDecimal timeSurchargeRate = BigDecimal.ZERO;
        private BigDecimal urgentSurcharge = BigDecimal.ZERO;
    }

    private FeeRule resolveRule(DmsTask task, Integer targetType) {
        Integer type = targetType != null ? targetType
                : (task.getRiderId() != null ? 1 : (task.getChannelId() != null ? 2 : 1));
        DmsSettlementRule matched = ruleService.match(type, task.getChannelId(), task.getRouteId());
        FeeRule rule = new FeeRule();
        if (matched != null) {
            rule.ruleId = matched.getId();
            rule.ruleName = matched.getRuleName();
            rule.billingType = matched.getBillingType() == null ? 2 : matched.getBillingType();
            rule.baseFee = nvl(matched.getBaseFee());
            rule.freeDistanceKm = nvl(matched.getFreeDistanceKm());
            rule.perKmRate = nvl(matched.getPerKmRate());
            rule.perKgRate = nvl(matched.getPerKgRate());
            rule.timeSurchargeRate = nvl(matched.getTimeSurchargeRate());
            rule.urgentSurcharge = nvl(matched.getUrgentSurcharge());
            return rule;
        }
        Map<String, BigDecimal> cfg = currentRule();
        rule.billingType = 2;
        rule.baseFee = cfg.get("baseFee");
        rule.freeDistanceKm = cfg.get("freeDistanceKm");
        rule.perKmRate = cfg.get("perKmRate");
        rule.timeSurchargeRate = cfg.get("timeSurchargeRate");
        rule.urgentSurcharge = cfg.get("urgentSurcharge");
        return rule;
    }

    private static String ruleSnapshot(FeeRule rule, Map<String, BigDecimal> defaults) {
        if (rule.ruleId != null) {
            return "rule#" + rule.ruleId + "(" + rule.ruleName + ")"
                    + ";billingType=" + rule.billingType
                    + ";baseFee=" + rule.baseFee
                    + ";freeKm=" + rule.freeDistanceKm
                    + ";perKm=" + rule.perKmRate
                    + ";perKg=" + rule.perKgRate
                    + ";timeRate=" + rule.timeSurchargeRate
                    + ";urgent=" + rule.urgentSurcharge;
        }
        return defaults.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ═══════════════════════════════════════════════
    // 单任务算费
    // ═══════════════════════════════════════════════

    /**
     * 计算单任务配送费
     *
     * <p>费用 = 起步价 + 里程费（按计价方式）+ 重量费 + 夜间附加（按起步价倍数）+ 加急附加费；
     * 部分签收按实签收/应签收折算（起步价全收），拒收不计费。</p>
     */
    public Map<String, Object> calculateDeliveryFee(Long taskId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.notFound("任务不存在: " + taskId);
        }
        FeeRule rule = resolveRule(task, null);
        Map<String, Object> fee = computeFee(task, rule, latestSign(taskId));
        fee.put("taskId", taskId);
        fee.put("taskNo", task.getTaskNo());
        fee.put("rule", currentRule());
        fee.put("ruleId", rule.ruleId);
        fee.put("ruleName", rule.ruleName);
        fee.put("billingType", rule.billingType);
        return fee;
    }

    /** 最近一次签收记录（部分签收计费依据） */
    private DmsSign latestSign(Long taskId) {
        return signMapper.selectOne(new LambdaQueryWrapper<DmsSign>()
                .eq(DmsSign::getTaskId, taskId)
                .orderByDesc(DmsSign::getId)
                .last("LIMIT 1"));
    }

    /**
     * 计费系数：正常签收 1；部分签收 = min(1, 实签收/应签收)；拒收 0；审核驳回视为无效按 1。
     */
    private static BigDecimal ratioOf(DmsSign sign) {
        if (sign == null) {
            return BigDecimal.ONE;
        }
        if (Integer.valueOf(2).equals(sign.getAuditStatus())) {
            return BigDecimal.ONE;
        }
        if (Integer.valueOf(3).equals(sign.getSignType())) {
            return BigDecimal.ZERO;
        }
        if (Integer.valueOf(2).equals(sign.getSignType())
                && sign.getActualQuantity() != null && sign.getPlannedQuantity() != null
                && sign.getPlannedQuantity().signum() > 0) {
            BigDecimal ratio = sign.getActualQuantity()
                    .divide(sign.getPlannedQuantity(), 4, RoundingMode.HALF_UP);
            return ratio.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : ratio.max(BigDecimal.ZERO);
        }
        return BigDecimal.ONE;
    }

    /** 按规则计算单任务费用（单任务算费与生成结算单共用同一口径） */
    private Map<String, Object> computeFee(DmsTask task, FeeRule rule, DmsSign sign) {
        BigDecimal ratio = ratioOf(sign);
        BigDecimal distance = task.getEstimatedDistance() != null && task.getEstimatedDistance().signum() > 0
                ? task.getEstimatedDistance()
                : FALLBACK_DISTANCE;
        BigDecimal weight = nvl(task.getTotalWeight());
        int billingType = rule.billingType == null ? 2 : rule.billingType;

        BigDecimal baseFee = rule.baseFee;
        BigDecimal mileageFee = BigDecimal.ZERO;
        BigDecimal weightFee = BigDecimal.ZERO;
        if (billingType == 2 || billingType == 4) {
            BigDecimal chargeableKm = distance.subtract(rule.freeDistanceKm).max(BigDecimal.ZERO);
            mileageFee = rule.perKmRate.multiply(chargeableKm).setScale(2, RoundingMode.HALF_UP);
        }
        if (billingType == 3 || billingType == 4) {
            weightFee = rule.perKgRate.multiply(weight).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal timeSurcharge = BigDecimal.ZERO;
        LocalDateTime dispatchAt = task.getDispatchTime();
        if (dispatchAt != null) {
            int hour = dispatchAt.getHour();
            if (hour >= 22 || hour < 6) {
                timeSurcharge = rule.baseFee.multiply(rule.timeSurchargeRate)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        BigDecimal urgentSurcharge = (task.getPriority() != null && task.getPriority() >= 2)
                ? rule.urgentSurcharge
                : BigDecimal.ZERO;

        // 部分签收折算：起步价全收，其余按系数折算；拒收（系数 0）整单不计费
        if (ratio.signum() == 0) {
            baseFee = BigDecimal.ZERO;
            mileageFee = BigDecimal.ZERO;
            weightFee = BigDecimal.ZERO;
            timeSurcharge = BigDecimal.ZERO;
            urgentSurcharge = BigDecimal.ZERO;
        } else if (ratio.compareTo(BigDecimal.ONE) < 0) {
            mileageFee = mileageFee.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            weightFee = weightFee.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            timeSurcharge = timeSurcharge.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            urgentSurcharge = urgentSurcharge.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = baseFee.add(mileageFee).add(weightFee).add(timeSurcharge).add(urgentSurcharge)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseFee", baseFee);
        result.put("distanceKm", distance);
        result.put("weight", weight);
        result.put("freeDistanceKm", rule.freeDistanceKm);
        result.put("mileageFee", mileageFee);
        result.put("weightFee", weightFee);
        result.put("timeSurcharge", timeSurcharge);
        result.put("urgentSurcharge", urgentSurcharge);
        result.put("totalFee", total);
        result.put("billingRatio", ratio);
        result.put("signType", sign == null ? null : sign.getSignType());
        result.put("plannedQuantity", sign == null ? null : sign.getPlannedQuantity());
        result.put("actualQuantity", sign == null ? null : sign.getActualQuantity());
        return result;
    }

    // ═══════════════════════════════════════════════
    // 结算单：生成 / 查询 / 确认 / 推送
    // ═══════════════════════════════════════════════

    /**
     * 按周期生成结算单（草稿）
     *
     * <p>结算对象：1-配送员（targetId = dms_rider.id）/ 2-渠道（targetId = dms_channel.id，外部运力）。
     * 重复生成同一「周期 + 对象」：已有**草稿**会被重建（幂等）；已确认/已推送的拒绝覆盖。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsSettlement generate(LocalDate periodStart, LocalDate periodEnd,
                                  Integer targetType, Long targetId, String remark) {
        if (periodStart == null || periodEnd == null || periodStart.isAfter(periodEnd)) {
            throw BusinessException.badRequest("结算周期不合法");
        }
        if (targetId == null) {
            throw BusinessException.badRequest("请选择结算对象（配送员或渠道）");
        }
        int type = targetType == null ? 1 : targetType;
        List<DmsTask> tasks = listSettleableTasks(periodStart, periodEnd);
        if (type == 2) {
            tasks = tasks.stream().filter(t -> targetId.equals(t.getChannelId())).collect(Collectors.toList());
        } else {
            tasks = tasks.stream().filter(t -> targetId.equals(t.getRiderId())).collect(Collectors.toList());
        }
        if (tasks.isEmpty()) {
            throw BusinessException.badRequest("该周期内该结算对象没有可结算的已签收/已完成任务");
        }
        // 防重复计费：排除已被「已确认/已推送」结算单结算过的任务（草稿不占用，允许重建）
        tasks = excludeSettled(tasks);
        if (tasks.isEmpty()) {
            throw BusinessException.badRequest("该周期内该结算对象的任务均已结算，不能重复计费");
        }

        DmsSettlement exist = settlementMapper.selectOne(new LambdaQueryWrapper<DmsSettlement>()
                .eq(DmsSettlement::getTargetType, type)
                .eq(DmsSettlement::getTargetId, targetId)
                .eq(DmsSettlement::getPeriodStart, periodStart)
                .eq(DmsSettlement::getPeriodEnd, periodEnd)
                .orderByDesc(DmsSettlement::getId)
                .last("LIMIT 1"));
        if (exist != null && exist.getStatus() != null && exist.getStatus() >= 1) {
            throw BusinessException.badRequest("该周期结算单已" + (exist.getStatus() == 1 ? "确认" : "推送")
                    + "，不能重复生成：" + exist.getSettlementNo());
        }
        if (exist != null) {
            // 草稿重建（幂等）：连同明细一并删除
            settlementItemMapper.delete(new LambdaQueryWrapper<DmsSettlementItem>()
                    .eq(DmsSettlementItem::getSettlementId, exist.getId()));
            settlementMapper.deleteById(exist.getId());
        }

        Map<String, BigDecimal> defaults = currentRule();
        DmsSettlement settlement = new DmsSettlement();
        settlement.setSettlementNo(nextNo());
        settlement.setTargetType(type);
        settlement.setTargetId(targetId);
        settlement.setTargetName(resolveTargetName(type, targetId, tasks.get(0)));
        settlement.setPeriodStart(periodStart);
        settlement.setPeriodEnd(periodEnd);
        settlement.setStatus(0);
        settlement.setPushCount(0);

        BigDecimal total = BigDecimal.ZERO;
        List<DmsSettlementItem> items = new ArrayList<>();
        FeeRule snapshotRule = null;
        for (DmsTask task : tasks) {
            FeeRule rule = resolveRule(task, type);
            if (snapshotRule == null) {
                snapshotRule = rule;
            }
            Map<String, Object> fee = computeFee(task, rule, latestSign(task.getId()));
            DmsSettlementItem item = new DmsSettlementItem();
            item.setTaskId(task.getId());
            item.setTaskNo(task.getTaskNo());
            item.setSignTime(task.getCompletedTime() != null ? task.getCompletedTime() : task.getUpdateTime());
            item.setDistanceKm((BigDecimal) fee.get("distanceKm"));
            item.setBaseFee((BigDecimal) fee.get("baseFee"));
            item.setMileageFee((BigDecimal) fee.get("mileageFee"));
            item.setWeightFee((BigDecimal) fee.get("weightFee"));
            item.setTimeSurcharge((BigDecimal) fee.get("timeSurcharge"));
            item.setUrgentSurcharge((BigDecimal) fee.get("urgentSurcharge"));
            item.setTotalFee((BigDecimal) fee.get("totalFee"));
            item.setBillingRatio((BigDecimal) fee.get("billingRatio"));
            item.setSignType((Integer) fee.get("signType"));
            item.setPlannedQuantity((BigDecimal) fee.get("plannedQuantity"));
            item.setActualQuantity((BigDecimal) fee.get("actualQuantity"));
            items.add(item);
            total = total.add(item.getTotalFee());
        }
        settlement.setRuleId(snapshotRule == null ? null : snapshotRule.ruleId);
        settlement.setRuleSnapshot(ruleSnapshot(snapshotRule, defaults));
        settlement.setTaskCount(items.size());
        settlement.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        settlement.setRemark(remark);
        settlementMapper.insert(settlement);
        for (DmsSettlementItem item : items) {
            item.setSettlementId(settlement.getId());
            settlementItemMapper.insert(item);
        }
        log.info("结算单已生成: no={}, type={}, target={}, tasks={}, amount={}",
                settlement.getSettlementNo(), type, settlement.getTargetName(), items.size(), settlement.getTotalAmount());
        return settlement;
    }

    /** 排除已被「已确认/已推送」结算单结算过的任务（同一任务不得重复计费） */
    private List<DmsTask> excludeSettled(List<DmsTask> tasks) {
        List<Long> ids = tasks.stream().map(DmsTask::getId).collect(Collectors.toList());
        List<Long> settled = settlementItemMapper.selectSettledTaskIds(ids);
        if (settled == null || settled.isEmpty()) {
            return tasks;
        }
        return tasks.stream().filter(t -> !settled.contains(t.getId())).collect(Collectors.toList());
    }

    /** 结算对象名称：渠道名 / 配送员名（任务快照兜底） */
    private String resolveTargetName(int type, Long targetId, DmsTask sample) {
        if (type == 2) {
            DmsChannel channel = channelMapper.selectById(targetId);
            if (channel != null && StringUtils.hasText(channel.getChannelName())) {
                return channel.getChannelName();
            }
        }
        return StringUtils.hasText(sample.getRiderName()) ? sample.getRiderName() : ("对象#" + targetId);
    }

    /** 周期内可结算任务（已签收 5 / 已完成 6） */
    private List<DmsTask> listSettleableTasks(LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        return taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                .in(DmsTask::getStatus, 5, 6)
                .and(w -> w.between(DmsTask::getCompletedTime, from, to)
                        .or(o -> o.isNull(DmsTask::getCompletedTime)
                                .between(DmsTask::getUpdateTime, from, to))));
    }

    /** 结算单分页（多条件） */
    public IPage<DmsSettlement> page(String settlementNo, Integer targetType, Long targetId,
                                     Integer status, LocalDate startDate, LocalDate endDate,
                                     long current, long size) {
        LambdaQueryWrapper<DmsSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(settlementNo), DmsSettlement::getSettlementNo, settlementNo);
        wrapper.eq(targetType != null, DmsSettlement::getTargetType, targetType);
        wrapper.eq(targetId != null, DmsSettlement::getTargetId, targetId);
        wrapper.eq(status != null, DmsSettlement::getStatus, status);
        wrapper.ge(startDate != null, DmsSettlement::getPeriodStart, startDate);
        wrapper.le(endDate != null, DmsSettlement::getPeriodEnd, endDate);
        wrapper.orderByDesc(DmsSettlement::getCreateTime).orderByDesc(DmsSettlement::getId);
        return settlementMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public DmsSettlement getById(Long id) {
        DmsSettlement settlement = settlementMapper.selectById(id);
        if (settlement == null) {
            throw BusinessException.notFound("结算单不存在: " + id);
        }
        return settlement;
    }

    /** 结算单明细（费用构成） */
    public List<DmsSettlementItem> items(Long settlementId) {
        getById(settlementId);
        return settlementItemMapper.selectList(new LambdaQueryWrapper<DmsSettlementItem>()
                .eq(DmsSettlementItem::getSettlementId, settlementId)
                .orderByAsc(DmsSettlementItem::getId));
    }

    /** 确认（锁定金额）：仅草稿可确认 */
    @Transactional(rollbackFor = Exception.class)
    public DmsSettlement confirm(Long id, String remark) {
        DmsSettlement settlement = getById(id);
        if (settlement.getStatus() == null || settlement.getStatus() != 0) {
            throw BusinessException.badRequest("仅草稿状态可确认，当前状态：" + statusText(settlement.getStatus()));
        }
        settlement.setStatus(1);
        if (StringUtils.hasText(remark)) {
            settlement.setRemark(remark);
        }
        settlementMapper.updateById(settlement);
        log.info("结算单已确认: no={}, amount={}", settlement.getSettlementNo(), settlement.getTotalAmount());
        return settlement;
    }

    /**
     * 推送 ERP（幂等）
     *
     * <p>仅已确认（1）可推送；已推送（2）直接返回既有 traceId，不重复外发。
     * 推送时**真实记账**：生成记账凭证（借 配送成本科目 / 贷 应付科目），
     * 结算对象为渠道（外部运力）时另生成应付单；以结算单为幂等键（sourceType=DMS_SETTLEMENT + sourceId）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pushErp(Long id) {
        DmsSettlement settlement = getById(id);
        if (settlement.getStatus() != null && settlement.getStatus() == 2) {
            Map<String, Object> again = new HashMap<>();
            again.put("settlementNo", settlement.getSettlementNo());
            again.put("traceId", settlement.getPushTraceId());
            again.put("voucherNo", settlement.getErpVoucherNo());
            again.put("payableNo", settlement.getErpPayableNo());
            again.put("idempotent", true);
            return again;
        }
        if (settlement.getStatus() == null || settlement.getStatus() != 1) {
            throw BusinessException.badRequest("仅已确认的结算单可推送 ERP，当前状态：" + statusText(settlement.getStatus()));
        }

        // ① 真实记账（先查幂等：防上次事务回滚但凭证已独立提交）
        Map<String, Object> account = accountToErp(settlement);

        // ② 领域事件外发（outbox 仅作事件载体）
        String traceId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("settlementNo", settlement.getSettlementNo());
        payload.put("targetType", settlement.getTargetType());
        payload.put("targetId", settlement.getTargetId());
        payload.put("targetName", settlement.getTargetName());
        payload.put("periodStart", String.valueOf(settlement.getPeriodStart()));
        payload.put("periodEnd", String.valueOf(settlement.getPeriodEnd()));
        payload.put("taskCount", settlement.getTaskCount());
        payload.put("totalAmount", settlement.getTotalAmount());
        payload.put("voucherNo", account.get("voucherNo"));
        payload.put("payableNo", account.get("payableNo"));

        DmsEventOutbox outbox = new DmsEventOutbox();
        outbox.setTraceId(traceId);
        outbox.setEventType("SETTLEMENT_PUSH_ERP");
        outbox.setSource("DMS");
        outbox.setTarget("ERP");
        outbox.setPayload(payload.toString());
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        eventOutboxMapper.insert(outbox);

        // ③ 回执落库
        settlement.setStatus(2);
        settlement.setPushTime(LocalDateTime.now());
        settlement.setPushCount((settlement.getPushCount() == null ? 0 : settlement.getPushCount()) + 1);
        settlement.setPushTraceId(traceId);
        settlement.setErpVoucherNo((String) account.get("voucherNo"));
        settlement.setErpPayableId((Long) account.get("payableId"));
        settlement.setErpPayableNo((String) account.get("payableNo"));
        settlementMapper.updateById(settlement);

        Map<String, Object> result = new HashMap<>();
        result.put("settlementNo", settlement.getSettlementNo());
        result.put("traceId", traceId);
        result.put("eventId", outbox.getId());
        result.put("voucherNo", account.get("voucherNo"));
        result.put("payableNo", account.get("payableNo"));
        result.put("accounted", account.get("accounted"));
        result.put("accountMessage", account.get("message"));
        result.put("idempotent", false);
        log.info("结算单已推送 ERP: no={}, traceId={}, voucherNo={}, payableNo={}",
                settlement.getSettlementNo(), traceId, account.get("voucherNo"), account.get("payableNo"));
        return result;
    }

    /**
     * 结算单 → 财务记账（凭证 + 外部运力应付）
     *
     * <p>科目可配：借 `dms.settlement.account.debit.subject`（缺省 6602 管理费用）、
     * 贷 渠道 `...credit.subject`（缺省 2202 应付账款）/ 自有配送员 `...staff.subject`（缺省 2241 其他应付款）。</p>
     */
    private Map<String, Object> accountToErp(DmsSettlement settlement) {
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal amount = settlement.getTotalAmount();
        if (amount == null || amount.signum() <= 0) {
            result.put("accounted", false);
            result.put("message", "结算金额为 0，无需记账");
            return result;
        }
        // 幂等：同一结算单已有凭证明细则复用，不重复记账
        List<VoucherItem> existed = voucherItemMapper.findBySourceTypeAndSourceId(SOURCE_TYPE, settlement.getId());
        if (existed != null && !existed.isEmpty()) {
            String voucherNo = settlement.getErpVoucherNo();
            result.put("accounted", true);
            result.put("voucherNo", voucherNo);
            result.put("payableId", settlement.getErpPayableId());
            result.put("payableNo", settlement.getErpPayableNo());
            result.put("message", "凭据已存在，复用（幂等）");
            return result;
        }

        boolean external = Integer.valueOf(2).equals(settlement.getTargetType());
        String debitSubject = readString("dms.settlement.account.debit.subject", "6602");
        String creditSubject = external
                ? readString("dms.settlement.account.credit.subject", "2202")
                : readString("dms.settlement.account.staff.subject", "2241");
        String summary = "配送费结算-" + settlement.getTargetName() + "-" + settlement.getSettlementNo();

        BusinessAccountingRequest req = new BusinessAccountingRequest();
        req.setSourceType(SOURCE_TYPE);
        req.setSourceId(settlement.getId());
        req.setSourceNo(settlement.getSettlementNo());
        req.setAmount(amount);
        req.setSummary(summary);
        req.setVoucherDate(LocalDate.now());
        req.setSupplierName(settlement.getTargetName());

        List<BusinessAccountingRequest.AccountingRequestItem> reqItems = new ArrayList<>();
        BusinessAccountingRequest.AccountingRequestItem debit = new BusinessAccountingRequest.AccountingRequestItem();
        debit.setSummary(summary);
        debit.setSubjectCode(debitSubject);
        debit.setDebitAmount(amount);
        debit.setAuxDept(null);
        debit.setAuxStaff(Integer.valueOf(1).equals(settlement.getTargetType()) ? settlement.getTargetName() : null);
        reqItems.add(debit);
        BusinessAccountingRequest.AccountingRequestItem credit = new BusinessAccountingRequest.AccountingRequestItem();
        credit.setSummary(summary);
        credit.setSubjectCode(creditSubject);
        credit.setCreditAmount(amount);
        credit.setAuxUnit(settlement.getTargetName());
        credit.setAuxStaff(Integer.valueOf(1).equals(settlement.getTargetType()) ? settlement.getTargetName() : null);
        reqItems.add(credit);
        req.setItems(reqItems);

        try {
            VoucherDTO voucher = businessAccountingService.createVoucherFromBusiness(req);
            result.put("voucherNo", voucher == null ? null : voucher.getVoucherNo());
            if (external) {
                PayableDTO payable = businessAccountingService.createPayableFromBusiness(req);
                if (payable != null) {
                    result.put("payableId", payable.getId());
                    result.put("payableNo", payable.getSourceNo());
                }
            }
            result.put("accounted", true);
            result.put("message", "已生成记账凭证" + (external ? "与应付单" : ""));
            log.info("结算单记账完成: no={}, voucherNo={}, amount={}",
                    settlement.getSettlementNo(), result.get("voucherNo"), amount);
        } catch (Exception e) {
            // 记账失败不阻断推送（业务侧仍有 outbox 事件与人工补记），但如实回执
            result.put("accounted", false);
            result.put("message", "记账失败：" + e.getMessage());
            log.warn("结算单记账失败: no={}, error={}", settlement.getSettlementNo(), e.getMessage());
        }
        return result;
    }

    /** 删除（仅草稿可删） */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DmsSettlement settlement = getById(id);
        if (settlement.getStatus() == null || settlement.getStatus() != 0) {
            throw BusinessException.badRequest("仅草稿结算单可删除，已确认/已推送的需保留审计留痕");
        }
        settlementItemMapper.delete(new LambdaQueryWrapper<DmsSettlementItem>()
                .eq(DmsSettlementItem::getSettlementId, id));
        settlementMapper.deleteById(id);
        log.info("结算单已删除: no={}", settlement.getSettlementNo());
    }

    /** 结算单号 JSD-YYYYMMDD-序号 */
    private String nextNo() {
        String prefix = "JSD-" + LocalDate.now().format(NO_DATE_FMT) + "-";
        DmsSettlement last = settlementMapper.selectOne(new LambdaQueryWrapper<DmsSettlement>()
                .likeRight(DmsSettlement::getSettlementNo, prefix)
                .orderByDesc(DmsSettlement::getSettlementNo)
                .last("LIMIT 1"));
        int seq = 1;
        if (last != null && last.getSettlementNo() != null) {
            try {
                seq = Integer.parseInt(last.getSettlementNo().substring(last.getSettlementNo().lastIndexOf('-') + 1)) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    private static String statusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已确认";
            case 2 -> "已推送";
            default -> String.valueOf(status);
        };
    }

    // ═══════════════════════════════════════════════
    // 周期报表（口径：已签收 5 / 已完成 6）
    // ═══════════════════════════════════════════════

    /**
     * 周期结算报表（按配送员/按日聚合）
     *
     * <p>⚠️ 历史实现按 `status = 4`（配送中）统计，口径错误；现统一为**已签收/已完成**（5/6）。</p>
     */
    public Map<String, Object> generateSettlementReport(Long tenantId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        List<DmsTask> tasks = listSettleableTasks(start, end);
        if (tenantId != null) {
            tasks = tasks.stream().filter(t -> tenantId.equals(t.getTenantId())).collect(Collectors.toList());
        }

        BigDecimal totalFee = BigDecimal.ZERO;
        Map<String, BigDecimal> byRider = new LinkedHashMap<>();
        Map<String, Integer> byRiderCount = new LinkedHashMap<>();
        Map<String, BigDecimal> byDay = new LinkedHashMap<>();
        for (DmsTask task : tasks) {
            FeeRule rule = resolveRule(task, null);
            BigDecimal fee = (BigDecimal) computeFee(task, rule, latestSign(task.getId())).get("totalFee");
            totalFee = totalFee.add(fee);
            String rider = StringUtils.hasText(task.getRiderName()) ? task.getRiderName() : "未指派";
            byRider.merge(rider, fee, BigDecimal::add);
            byRiderCount.merge(rider, 1, Integer::sum);
            LocalDateTime at = task.getCompletedTime() != null ? task.getCompletedTime() : task.getUpdateTime();
            if (at != null) {
                byDay.merge(at.toLocalDate().toString(), fee, BigDecimal::add);
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tenantId", tenantId);
        report.put("startDate", start.toString());
        report.put("endDate", end.toString());
        report.put("totalTasks", tasks.size());
        report.put("totalFee", totalFee.setScale(2, RoundingMode.HALF_UP));
        report.put("byRider", byRider.entrySet().stream()
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("riderName", e.getKey());
                    row.put("taskCount", byRiderCount.getOrDefault(e.getKey(), 0));
                    row.put("amount", e.getValue().setScale(2, RoundingMode.HALF_UP));
                    return row;
                }).collect(Collectors.toList()));
        report.put("byDay", byDay.entrySet().stream()
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("date", e.getKey());
                    row.put("amount", e.getValue().setScale(2, RoundingMode.HALF_UP));
                    return row;
                }).collect(Collectors.toList()));
        report.put("rule", currentRule());
        report.put("generatedAt", LocalDateTime.now());
        return report;
    }

    // ═══════════════════════════════════════════════
    // 对账（结算单 vs 财务：凭证金额 / 应付核销 / 配送费收款）
    // ═══════════════════════════════════════════════

    /**
     * 结算对账
     *
     * <p>逐张结算单核对三件事：</p>
     * <ol>
     *   <li><b>记账一致</b>：凭证明细借方合计 = 结算金额；</li>
     *   <li><b>已结金额</b>：渠道（外部运力）取应付已付金额；配送员取周期内该对象的**配送费收款**（dms_payment）；</li>
     *   <li><b>差异</b>：已结金额 − 结算金额（0 = 已对平；&lt;0 = 未结清）。</li>
     * </ol>
     */
    public Map<String, Object> reconcile(LocalDate startDate, LocalDate endDate, Integer targetType,
                                         String settlementNo, boolean onlyDiff) {
        LambdaQueryWrapper<DmsSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(startDate != null, DmsSettlement::getPeriodStart, startDate);
        wrapper.le(endDate != null, DmsSettlement::getPeriodEnd, endDate);
        wrapper.eq(targetType != null, DmsSettlement::getTargetType, targetType);
        wrapper.like(StringUtils.hasText(settlementNo), DmsSettlement::getSettlementNo, settlementNo);
        wrapper.orderByDesc(DmsSettlement::getId);
        List<DmsSettlement> settlements = settlementMapper.selectList(wrapper);

        LocalDateTime from = (startDate != null ? startDate : LocalDate.now().withDayOfMonth(1)).atStartOfDay();
        LocalDateTime to = (endDate != null ? endDate : LocalDate.now()).plusDays(1).atStartOfDay();

        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal settledTotal = BigDecimal.ZERO;
        BigDecimal diffTotal = BigDecimal.ZERO;
        int diffCount = 0;
        int unaccountedCount = 0;
        for (DmsSettlement s : settlements) {
            Map<String, Object> row = new LinkedHashMap<>();
            BigDecimal amount = nvl(s.getTotalAmount());
            // ① 凭证借方合计（记账一致性）
            List<VoucherItem> voucherItems = voucherItemMapper.findBySourceTypeAndSourceId(SOURCE_TYPE, s.getId());
            BigDecimal voucherAmount = voucherItems == null ? BigDecimal.ZERO : voucherItems.stream()
                    .map(i -> nvl(i.getDebitAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean accounted = voucherItems != null && !voucherItems.isEmpty();
            // ② 已结金额
            BigDecimal settledAmount = BigDecimal.ZERO;
            BigDecimal paidAmount = BigDecimal.ZERO;
            String payableStatus = null;
            if (Integer.valueOf(2).equals(s.getTargetType()) && s.getErpPayableId() != null) {
                try {
                    PayableDTO payable = payableService.getById(s.getErpPayableId());
                    if (payable != null) {
                        paidAmount = nvl(payable.getPaidAmount());
                        payableStatus = payable.getStatus();
                    }
                } catch (Exception e) {
                    log.debug("对账读取应付失败: settlementNo={}, payableId={}", s.getSettlementNo(), s.getErpPayableId());
                }
                settledAmount = paidAmount;
            } else if (s.getTargetId() != null) {
                List<DmsPayment> pays = paymentMapper.selectList(new LambdaQueryWrapper<DmsPayment>()
                        .eq(DmsPayment::getRiderId, s.getTargetId())
                        .eq(DmsPayment::getPaymentType, 2)
                        .eq(DmsPayment::getStatus, 1)
                        .between(DmsPayment::getPayTime, from, to));
                settledAmount = pays.stream().map(p -> nvl(p.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            settledAmount = settledAmount.setScale(2, RoundingMode.HALF_UP);
            BigDecimal diff = settledAmount.subtract(amount).setScale(2, RoundingMode.HALF_UP);
            boolean matched = accounted && diff.signum() == 0;

            totalAmount = totalAmount.add(amount);
            settledTotal = settledTotal.add(settledAmount);
            diffTotal = diffTotal.add(diff);
            if (diff.signum() != 0) {
                diffCount++;
            }
            if (!accounted) {
                unaccountedCount++;
            }
            if (onlyDiff && matched) {
                continue;
            }

            row.put("id", s.getId());
            row.put("settlementNo", s.getSettlementNo());
            row.put("targetType", s.getTargetType());
            row.put("targetName", s.getTargetName());
            row.put("periodStart", String.valueOf(s.getPeriodStart()));
            row.put("periodEnd", String.valueOf(s.getPeriodEnd()));
            row.put("taskCount", s.getTaskCount());
            row.put("totalAmount", amount);
            row.put("status", s.getStatus());
            row.put("erpVoucherNo", s.getErpVoucherNo());
            row.put("voucherAmount", voucherAmount.setScale(2, RoundingMode.HALF_UP));
            row.put("accounted", accounted);
            row.put("erpPayableNo", s.getErpPayableNo());
            row.put("payableStatus", payableStatus);
            row.put("settledAmount", settledAmount);
            row.put("diff", diff);
            row.put("matched", matched);
            rows.add(row);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("settlementCount", settlements.size());
        summary.put("totalAmount", totalAmount.setScale(2, RoundingMode.HALF_UP));
        summary.put("settledAmount", settledTotal.setScale(2, RoundingMode.HALF_UP));
        summary.put("diffAmount", diffTotal.setScale(2, RoundingMode.HALF_UP));
        summary.put("diffCount", diffCount);
        summary.put("unaccountedCount", unaccountedCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", String.valueOf(startDate != null ? startDate : LocalDate.now().withDayOfMonth(1)));
        result.put("endDate", String.valueOf(endDate != null ? endDate : LocalDate.now()));
        result.put("summary", summary);
        result.put("rows", rows);
        return result;
    }

    /** 兼容旧入口：直接推送任意 Map（写入事件发件箱） */
    @Transactional(rollbackFor = Exception.class)
    public void pushToErp(Map<String, Object> settlementData) {
        DmsEventOutbox outbox = new DmsEventOutbox();
        outbox.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        outbox.setEventType("SETTLEMENT_PUSH_ERP");
        outbox.setSource("DMS");
        outbox.setTarget("ERP");
        outbox.setPayload(String.valueOf(settlementData));
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        eventOutboxMapper.insert(outbox);
        log.info("结算数据已写入事件发件箱（兼容入口）, traceId={}, eventId={}", outbox.getTraceId(), outbox.getId());
    }

    /** 供测试/调试：当前规则键列表 */
    public List<String> ruleKeys() {
        return Arrays.asList("baseFee", "freeDistanceKm", "perKmRate", "timeSurchargeRate", "urgentSurcharge");
    }
}
