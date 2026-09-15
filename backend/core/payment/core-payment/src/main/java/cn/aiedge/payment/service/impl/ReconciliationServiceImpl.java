package cn.aiedge.payment.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.payment.dto.ReconciliationDetailVO;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.entity.PaymentReconciliation;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.mapper.PaymentRecordMapper;
import cn.aiedge.payment.mapper.PaymentReconciliationMapper;
import cn.aiedge.payment.mapper.PaymentRequestMapper;
import cn.aiedge.payment.service.ReconciliationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private final PaymentReconciliationMapper reconciliationMapper;
    private final PaymentRecordMapper recordMapper;
    private final PaymentRequestMapper requestMapper;

    @Override
    @Transactional
    public List<PaymentReconciliation> executeDailyReconciliation(LocalDate date, String channel) {
        // ── 我方口径：当日该渠道的全部支付请求（payment_request）= 「渠道路由总流水」 ──
        LambdaQueryWrapper<PaymentRequest> reqWrapper = new LambdaQueryWrapper<>();
        reqWrapper.ge(PaymentRequest::getCreateTime, date.atStartOfDay());
        reqWrapper.lt(PaymentRequest::getCreateTime, date.plusDays(1).atStartOfDay());
        reqWrapper.eq(StringUtils.hasText(channel), PaymentRequest::getChannel, channel);
        List<PaymentRequest> requests = requestMapper.selectList(reqWrapper);

        List<PaymentReconciliation> results = new ArrayList<>();
        if (requests.isEmpty()) {
            return results;
        }

        Map<String, List<PaymentRequest>> channelGroups = requests.stream()
                .collect(Collectors.groupingBy(PaymentRequest::getChannel));

        for (Map.Entry<String, List<PaymentRequest>> entry : channelGroups.entrySet()) {
            results.add(saveForChannel(date, entry.getKey(), entry.getValue()));
        }

        return results;
    }

    /**
     * 按「对账日 + 渠道」重算并**落库（upsert）**：该日期渠道已有记录则原地更新，否则插入。
     *
     * <p>upsert 的意义：① 同一「日期+渠道」重复执行日对账不再追加重复行；② 「重新对账」
     * （`handleDifference` 的 REPROCESS 分支）复用它，把 status=3（处理中）的记录收敛为最终态
     * 1（已对账）/ 2（有差异）并刷新汇总。</p>
     *
     * <p>取既有记录时加了 {@code LIMIT 1}：历史版本是「只插入不更新」，同一日期渠道可能已存在
     * 多条重复行，`selectOne` 不加限制会抛异常。</p>
     */
    private PaymentReconciliation saveForChannel(LocalDate date, String channel, List<PaymentRequest> chRequests) {
        PaymentReconciliation existing = reconciliationMapper.selectOne(
                new LambdaQueryWrapper<PaymentReconciliation>()
                        .eq(PaymentReconciliation::getReconcileDate, date)
                        .eq(PaymentReconciliation::getChannel, channel)
                        .orderByAsc(PaymentReconciliation::getId)
                        .last("LIMIT 1"));

        // 我方成功 = status 2（已支付）；差异比对只针对我方认为已成功的交易
        List<PaymentRequest> paidRequests = chRequests.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 2)
                .collect(Collectors.toList());

        BigDecimal totalAmount = chRequests.stream()
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal successAmount = paidRequests.stream()
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ReconciliationDetailVO.DiffRecord> diffs = buildDiffRecords(date, channel, paidRequests);
        BigDecimal diffAmount = diffs.stream()
                .map(d -> d.getAmount() == null ? BigDecimal.ZERO : d.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PaymentReconciliation recon = existing != null ? existing : new PaymentReconciliation();
        recon.setReconcileDate(date);
        recon.setChannel(channel);
        recon.setTotalCount(chRequests.size());
        recon.setTotalAmount(totalAmount);
        recon.setSuccessCount(paidRequests.size());
        recon.setSuccessAmount(successAmount);
        recon.setDiffCount(diffs.size());
        recon.setDiffAmount(diffAmount);
        // 重算即回到客观口径：有差异 → 2（前端「处理差异」仅对 2 显示）；无差异 → 1（已对账）
        recon.setStatus(diffs.isEmpty() ? 1 : 2);
        recon.setReconciledTime(LocalDate.now());

        if (existing == null) {
            reconciliationMapper.insert(recon);
        } else {
            reconciliationMapper.updateById(recon);
        }
        return recon;
    }

    /**
     * 计算「我方（payment_request 已支付）↔ 渠道侧（payment_record 成功回执）」的差异明细。
     *
     * <p><b>口径（本实现定义，对标系统未给出明细字段口径）</b>，三类差异全部由真实数据比对得出，
     * 不做任何伪造：</p>
     * <ol>
     *   <li><b>渠道缺单</b>：我方已支付（`payment_request.status=2`），但按 `payment_record.request_id`
     *       找不到该笔的成功回执（无回执，或回执 `status != 2`）。</li>
     *   <li><b>金额不一致</b>：双方均成功，但 `payment_record.amount != payment_request.amount`。</li>
     *   <li><b>平台缺单</b>：渠道侧当日（`callback_time` 落在对账日）有成功回执（`status=2`），但其
     *       `request_id` 为空，或指向的支付请求不在「我方已支付」集合内。</li>
     * </ol>
     *
     * <p>差异金额 `amount` = 渠道金额 − 我方金额（有符号，与
     * {@link ReconciliationDetailVO.DiffRecord#getAmount()} 注释一致）；对账汇总列的
     * `diffAmount` 取各明细 `|amount|` 之和（差异规模，避免正负相抵掩盖差异）。</p>
     */
    private List<ReconciliationDetailVO.DiffRecord> buildDiffRecords(LocalDate date, String channel,
            List<PaymentRequest> paidRequests) {
        List<ReconciliationDetailVO.DiffRecord> diffs = new ArrayList<>();
        Set<Long> paidRequestIds = paidRequests.stream()
                .map(PaymentRequest::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 渠道侧：这批已支付请求的全部回执（不限 callback_time，避免跨日回执被误判为「渠道缺单」）
        Map<Long, List<PaymentRecord>> recordsByRequest = new HashMap<>();
        if (!paidRequestIds.isEmpty()) {
            List<PaymentRecord> matched = recordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                    .in(PaymentRecord::getRequestId, paidRequestIds));
            recordsByRequest = matched.stream()
                    .filter(r -> r.getRequestId() != null)
                    .collect(Collectors.groupingBy(PaymentRecord::getRequestId));
        }

        // 1) / 2) 逐笔已支付请求比对
        for (PaymentRequest req : paidRequests) {
            List<PaymentRecord> recs = recordsByRequest.getOrDefault(req.getId(), List.of());
            List<PaymentRecord> successRecs = recs.stream()
                    .filter(r -> r.getStatus() != null && r.getStatus() == 2)
                    .collect(Collectors.toList());

            if (successRecs.isEmpty()) {
                ReconciliationDetailVO.DiffRecord d = new ReconciliationDetailVO.DiffRecord();
                d.setBizNo(req.getBizNo());
                d.setType("渠道缺单");
                d.setOurAmount(req.getAmount());
                d.setChannelAmount(BigDecimal.ZERO);
                d.setAmount(BigDecimal.ZERO.subtract(nvl(req.getAmount())));
                d.setRemark(recs.isEmpty()
                        ? "我方已支付，渠道侧无回执记录"
                        : "我方已支付，渠道侧回执非成功状态（status=" + recs.get(0).getStatus() + "）");
                diffs.add(d);
                continue;
            }

            for (PaymentRecord rec : successRecs) {
                BigDecimal our = nvl(req.getAmount());
                BigDecimal theirs = nvl(rec.getAmount());
                if (our.compareTo(theirs) != 0) {
                    ReconciliationDetailVO.DiffRecord d = new ReconciliationDetailVO.DiffRecord();
                    d.setBizNo(StringUtils.hasText(req.getBizNo())
                            ? req.getBizNo() : rec.getChannelTradeNo());
                    d.setType("金额不一致");
                    d.setOurAmount(our);
                    d.setChannelAmount(theirs);
                    d.setAmount(theirs.subtract(our));
                    d.setRemark("双方均成功，但金额不一致");
                    diffs.add(d);
                }
            }
        }

        // 3) 渠道侧当日成功回执，但我方无对应已支付请求 → 平台缺单
        List<PaymentRecord> dayChannelRecords = recordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getChannel, channel)
                .eq(PaymentRecord::getStatus, 2)
                .ge(PaymentRecord::getCallbackTime, date.atStartOfDay())
                .lt(PaymentRecord::getCallbackTime, date.plusDays(1).atStartOfDay()));
        for (PaymentRecord rec : dayChannelRecords) {
            if (rec.getRequestId() != null && paidRequestIds.contains(rec.getRequestId())) {
                continue;
            }
            ReconciliationDetailVO.DiffRecord d = new ReconciliationDetailVO.DiffRecord();
            d.setBizNo(StringUtils.hasText(rec.getChannelTradeNo())
                    ? rec.getChannelTradeNo() : rec.getChannelOrderNo());
            d.setType("平台缺单");
            d.setOurAmount(BigDecimal.ZERO);
            d.setChannelAmount(nvl(rec.getAmount()));
            d.setAmount(nvl(rec.getAmount()));
            d.setRemark(rec.getRequestId() == null
                    ? "渠道侧成功回执未关联支付请求（request_id 为空）"
                    : "渠道侧成功回执对应的支付请求不在我方已支付集合内");
            diffs.add(d);
        }

        return diffs;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public PageResult<PaymentReconciliation> pageReconciliation(Integer pageNum, Integer pageSize,
            LocalDate startDate, LocalDate endDate, String channel, Integer status) {
        LambdaQueryWrapper<PaymentReconciliation> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(startDate != null, PaymentReconciliation::getReconcileDate, startDate);
        wrapper.le(endDate != null, PaymentReconciliation::getReconcileDate, endDate);
        wrapper.eq(channel != null, PaymentReconciliation::getChannel, channel);
        wrapper.eq(status != null, PaymentReconciliation::getStatus, status);
        wrapper.orderByDesc(PaymentReconciliation::getReconcileDate);

        Page<PaymentReconciliation> page = reconciliationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<String, Object> statReconciliation(LocalDate startDate, LocalDate endDate, String channel) {
        LambdaQueryWrapper<PaymentReconciliation> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(startDate != null, PaymentReconciliation::getReconcileDate, startDate);
        wrapper.le(endDate != null, PaymentReconciliation::getReconcileDate, endDate);
        wrapper.eq(channel != null && !channel.isBlank(), PaymentReconciliation::getChannel, channel);
        List<PaymentReconciliation> records = reconciliationMapper.selectList(wrapper);

        long matched = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 1).count();
        long diff = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 2).count();
        long processing = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 3).count();
        BigDecimal diffAmount = records.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 2)
                .map(r -> r.getDiffAmount() == null ? BigDecimal.ZERO : r.getDiffAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", records.size());
        data.put("matchedCount", matched);
        data.put("diffCount", diff);
        data.put("processingCount", processing);
        data.put("diffAmount", diffAmount);
        return data;
    }

    @Override
    public PaymentReconciliation getReconciliation(Long id) {
        return reconciliationMapper.selectById(id);
    }

    @Override
    public ReconciliationDetailVO getReconciliationDetail(Long id) {
        PaymentReconciliation recon = reconciliationMapper.selectById(id);
        if (recon == null) {
            return null;
        }
        ReconciliationDetailVO vo = new ReconciliationDetailVO();
        BeanUtils.copyProperties(recon, vo);
        // 差异明细：按对账记录的「日期 + 渠道」实时比对「我方 payment_request（已支付）↔ 渠道侧
        // payment_record（成功回执）」现算，无需新增子表，且不会因落库快照过期而失真。
        // 口径见 buildDiffRecords 的 javadoc。
        List<PaymentRequest> paidRequests = requestMapper.selectList(new LambdaQueryWrapper<PaymentRequest>()
                .eq(PaymentRequest::getChannel, recon.getChannel())
                .eq(PaymentRequest::getStatus, 2)
                .ge(PaymentRequest::getCreateTime, recon.getReconcileDate().atStartOfDay())
                .lt(PaymentRequest::getCreateTime, recon.getReconcileDate().plusDays(1).atStartOfDay()));
        vo.setDiffRecords(buildDiffRecords(recon.getReconcileDate(), recon.getChannel(), paidRequests));
        return vo;
    }

    @Override
    @Transactional
    public void handleDifference(Long id, String method, String remark) {
        PaymentReconciliation recon = reconciliationMapper.selectById(id);
        if (recon == null) {
            throw new IllegalArgumentException("对账记录不存在");
        }
        recon.setRemark(remark);
        recon.setHandleMethod(method);

        if ("REPROCESS".equalsIgnoreCase(method)) {
            // 重新对账：先落「处理中」（3，对标状态流中的 3=处理中），再按同一「对账日 + 渠道」
            // 重算并收敛为 1（已对账）/ 2（有差异）。
            // ⚠️ 当前重算为**同步**执行，故 3 是执行中态，外部通常观察不到 → 统计卡片「处理中」恒 0；
            //    若后续改为异步重算，该状态即可自然外显，无需再改数据模型。
            recon.setStatus(3);
            reconciliationMapper.updateById(recon);

            LambdaQueryWrapper<PaymentRequest> reqWrapper = new LambdaQueryWrapper<>();
            reqWrapper.ge(PaymentRequest::getCreateTime, recon.getReconcileDate().atStartOfDay());
            reqWrapper.lt(PaymentRequest::getCreateTime, recon.getReconcileDate().plusDays(1).atStartOfDay());
            reqWrapper.eq(PaymentRequest::getChannel, recon.getChannel());
            List<PaymentRequest> requests = requestMapper.selectList(reqWrapper);
            if (!requests.isEmpty()) {
                PaymentReconciliation refreshed = saveForChannel(
                        recon.getReconcileDate(), recon.getChannel(), requests);
                // 重算会重置状态与汇总；处理方式/备注属于人工处置痕迹，需保留
                refreshed.setHandleMethod(method);
                refreshed.setRemark(remark);
                reconciliationMapper.updateById(refreshed);
            }
            return;
        }

        // MANUAL 手工调账 / IGNORE 忽略差异：差异已处置 → 回到「已对账」
        recon.setStatus(1);
        reconciliationMapper.updateById(recon);
    }

    @Override
    public List<LocalDate> getPendingDates(String channel) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(30);

        // 「待对账日期」的真实语义：区间内**有支付流水**（payment_request）但**尚无对账记录**的日期。
        // 旧实现只查已有记录中 status=0 的日期，而写入路径只产生 1/2，导致该接口恒返回空列表。
        LambdaQueryWrapper<PaymentRequest> reqWrapper = new LambdaQueryWrapper<>();
        reqWrapper.ge(PaymentRequest::getCreateTime, startDate.atStartOfDay());
        reqWrapper.lt(PaymentRequest::getCreateTime, today.plusDays(1).atStartOfDay());
        reqWrapper.eq(StringUtils.hasText(channel), PaymentRequest::getChannel, channel);
        List<PaymentRequest> requests = requestMapper.selectList(reqWrapper);
        if (requests.isEmpty()) {
            return new ArrayList<>();
        }
        Set<LocalDate> activeDates = requests.stream()
                .map(PaymentRequest::getCreateTime)
                .filter(java.util.Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        LambdaQueryWrapper<PaymentReconciliation> reconWrapper = new LambdaQueryWrapper<>();
        reconWrapper.ge(PaymentReconciliation::getReconcileDate, startDate);
        reconWrapper.le(PaymentReconciliation::getReconcileDate, today);
        reconWrapper.eq(StringUtils.hasText(channel), PaymentReconciliation::getChannel, channel);
        Set<LocalDate> reconciledDates = reconciliationMapper.selectList(reconWrapper).stream()
                .map(PaymentReconciliation::getReconcileDate)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return activeDates.stream()
                .filter(d -> !reconciledDates.contains(d))
                .sorted()
                .collect(Collectors.toList());
    }
}