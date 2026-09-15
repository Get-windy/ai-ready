package cn.aiedge.dms.payment.service;

import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.payment.entity.DmsPayment;
import cn.aiedge.dms.payment.entity.DmsPaymentFlow;
import cn.aiedge.dms.payment.mapper.DmsPaymentFlowMapper;
import cn.aiedge.dms.payment.mapper.DmsPaymentMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 支付平台流水与对账服务（《收款管理开发文档》§3.1-4「支付流水」/ §3.3 `/reconcile`）
 *
 * <p><b>干什么</b>：把支付平台（微信/支付宝/POS/银行）的日终流水导入系统，与
 * {@link DmsPayment} 的已支付记录**逐笔核对**，三类差异一目了然：</p>
 * <ul>
 *   <li><b>MATCHED</b> 匹配成功（交易号或商户单号一致且金额相符）；</li>
 *   <li><b>FLOW_ONLY / AMOUNT_MISMATCH / DUPLICATE</b> 流水有、系统异常（多收/金额不符/重复）；</li>
 *   <li><b>SYSTEM_ONLY</b> 系统已支付、流水缺失（掉单，需向渠道追款/补账）。</li>
 * </ul>
 *
 * <p><b>口径</b>：只对**线上渠道**（微信/支付宝/POS/银行转账）的已支付记录对账 ——
 * 现金收款本就无平台流水，不参与，避免把现金单误判为掉单。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFlowService {

    private final DmsPaymentFlowMapper flowMapper;
    private final DmsPaymentMapper paymentMapper;
    private final DmsTaskMapper taskMapper;

    /** 参与对账的线上支付方式（现金 3 / 其他 9 无平台流水） */
    private static final List<Integer> ONLINE_CHANNELS = List.of(1, 2, 4, 5);

    /** 差异明细返回上限（避免大范围对账把响应撑爆） */
    private static final int DETAIL_LIMIT = 200;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 含时分秒的解析格式 */
    private static final List<DateTimeFormatter> DT_PARSERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    /** 仅日期的解析格式（补 00:00:00） */
    private static final List<DateTimeFormatter> D_PARSERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"));

    // ═══════════════════════════════════════════════
    // 查询 / 统计
    // ═══════════════════════════════════════════════

    public IPage<DmsPaymentFlow> page(String channelCode, String tradeNo, Integer matchStatus,
                                      LocalDate startDate, LocalDate endDate, long current, long size) {
        return flowMapper.selectPage(new Page<>(current, size), wrapper(channelCode, tradeNo, matchStatus, startDate, endDate)
                .orderByDesc(DmsPaymentFlow::getTradeTime).orderByDesc(DmsPaymentFlow::getId));
    }

    /** 对账口径统计（流水总数 / 已匹配 / 未匹配 / 差异 / 差异金额） */
    public Map<String, Object> stat(String channelCode, LocalDate startDate, LocalDate endDate) {
        List<DmsPaymentFlow> list = flowMapper.selectList(wrapper(channelCode, null, null, startDate, endDate));
        int matched = 0, unmatched = 0, diff = 0, ignored = 0;
        BigDecimal matchedAmount = BigDecimal.ZERO;
        BigDecimal diffAmount = BigDecimal.ZERO;
        for (DmsPaymentFlow f : list) {
            int st = f.getMatchStatus() == null ? 0 : f.getMatchStatus();
            BigDecimal amt = f.getAmount() == null ? BigDecimal.ZERO : f.getAmount();
            switch (st) {
                case 1 -> { matched++; matchedAmount = matchedAmount.add(amt); }
                case 2 -> { diff++; diffAmount = diffAmount.add(amt); }
                case 3 -> ignored++;
                default -> unmatched++;
            }
        }
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("total", list.size());
        stat.put("matched", matched);
        stat.put("unmatched", unmatched);
        stat.put("diff", diff);
        stat.put("ignored", ignored);
        stat.put("matchedAmount", matchedAmount.setScale(2, RoundingMode.HALF_UP));
        stat.put("diffAmount", diffAmount.setScale(2, RoundingMode.HALF_UP));
        return stat;
    }

    private LambdaQueryWrapper<DmsPaymentFlow> wrapper(String channelCode, String tradeNo, Integer matchStatus,
                                                       LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DmsPaymentFlow> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(channelCode), DmsPaymentFlow::getChannelCode, channelCode);
        w.and(StringUtils.hasText(tradeNo), q -> q.like(DmsPaymentFlow::getTradeNo, tradeNo)
                .or().like(DmsPaymentFlow::getOutTradeNo, tradeNo));
        w.eq(matchStatus != null, DmsPaymentFlow::getMatchStatus, matchStatus);
        w.ge(startDate != null, DmsPaymentFlow::getTradeTime, startDate == null ? null : startDate.atStartOfDay());
        w.le(endDate != null, DmsPaymentFlow::getTradeTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        return w;
    }

    // ═══════════════════════════════════════════════
    // 导入（幂等：同租户+渠道+平台交易号 唯一）
    // ═══════════════════════════════════════════════

    /**
     * 批量导入平台流水
     *
     * <p>逐行校验 + 落库；重复行（同渠道同交易号）**跳过**并计入 `skipped`，
     * 失败行返回原因，不整批回滚（避免一行脏数据挡住整日流水）。</p>
     *
     * @param rows           行数据（键支持 channelCode/tradeNo/outTradeNo/amount/tradeTime/payer/remark 及中文列名）
     * @param defaultChannel 缺省渠道编码（页面选择，行内未给时使用）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importFlows(List<Map<String, Object>> rows, String defaultChannel) {
        if (rows == null || rows.isEmpty()) {
            throw new DmsBusinessException("导入内容为空");
        }
        String batchNo = "IMP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int inserted = 0, skipped = 0;
        List<Map<String, Object>> failed = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> row : rows) {
            index++;
            try {
                String tradeNo = str(row, "tradeNo", "trade_no", "平台交易号", "交易号");
                if (!StringUtils.hasText(tradeNo)) {
                    failed.add(fail(index, "平台交易号为空"));
                    continue;
                }
                String channelCode = str(row, "channelCode", "channel_code", "渠道", "渠道编码");
                if (!StringUtils.hasText(channelCode)) {
                    channelCode = defaultChannel;
                }
                if (!StringUtils.hasText(channelCode)) {
                    failed.add(fail(index, "渠道编码为空"));
                    continue;
                }
                // 渠道编码统一大写去空格：否则 wechat / WECHAT 会被当成两个渠道，导致幂等判重失效
                channelCode = channelCode.trim().toUpperCase();
                BigDecimal amount = decimal(row, "amount", "金额", "流水金额");
                if (amount == null) {
                    failed.add(fail(index, "金额为空或非法"));
                    continue;
                }
                Long exists = flowMapper.selectCount(new LambdaQueryWrapper<DmsPaymentFlow>()
                        .eq(DmsPaymentFlow::getChannelCode, channelCode)
                        .eq(DmsPaymentFlow::getTradeNo, tradeNo));
                if (exists != null && exists > 0) {
                    skipped++;
                    continue;
                }
                DmsPaymentFlow flow = new DmsPaymentFlow();
                flow.setChannelCode(channelCode);
                flow.setChannelName(str(row, "channelName", "channel_name", "渠道名称"));
                flow.setTradeNo(tradeNo);
                flow.setOutTradeNo(str(row, "outTradeNo", "out_trade_no", "商户单号", "外部单号"));
                flow.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
                flow.setTradeTime(datetime(row, "tradeTime", "trade_time", "成交时间", "交易时间"));
                flow.setPayer(str(row, "payer", "付款人"));
                flow.setRemark(str(row, "remark", "备注"));
                flow.setBatchNo(batchNo);
                flow.setMatchStatus(0);
                flowMapper.insert(flow);
                inserted++;
            } catch (Exception e) {
                log.warn("流水导入第 {} 行失败: {}", index, e.getMessage());
                failed.add(fail(index, e.getMessage()));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchNo", batchNo);
        result.put("total", rows.size());
        result.put("inserted", inserted);
        result.put("skipped", skipped);
        result.put("failedCount", failed.size());
        result.put("failed", failed);
        log.info("支付流水导入完成: batch={}, 总={}, 入库={}, 跳过={}, 失败={}",
                batchNo, rows.size(), inserted, skipped, failed.size());
        return result;
    }

    private static Map<String, Object> fail(int index, String reason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("row", index);
        row.put("reason", reason);
        return row;
    }

    // ═══════════════════════════════════════════════
    // 对账（逐笔匹配）
    // ═══════════════════════════════════════════════

    /**
     * 与支付平台流水对账（幂等：已匹配的流水不重复处理，差异可反复重试）
     *
     * <p>匹配优先级：① 平台交易号 = 收款记录 tradeNo → ② 商户单号 = 收款记录 externalOrderNo。
     * 金额不符、重复匹配、系统无此笔均记为**差异**；系统已支付但无对应流水记为**掉单**。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reconcile(LocalDate startDate, LocalDate endDate, String channelCode) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();

        List<DmsPaymentFlow> flows = flowMapper.selectList(new LambdaQueryWrapper<DmsPaymentFlow>()
                .eq(StringUtils.hasText(channelCode), DmsPaymentFlow::getChannelCode, channelCode)
                .and(w -> w.between(DmsPaymentFlow::getTradeTime, from, to)
                        .or(o -> o.isNull(DmsPaymentFlow::getTradeTime).between(DmsPaymentFlow::getCreateTime, from, to)))
                .orderByAsc(DmsPaymentFlow::getId));

        List<DmsPayment> payments = paymentMapper.selectList(new LambdaQueryWrapper<DmsPayment>()
                .eq(DmsPayment::getStatus, 1)
                .in(DmsPayment::getPayChannel, ONLINE_CHANNELS)
                .and(w -> w.between(DmsPayment::getPayTime, from, to)
                        .or(o -> o.isNull(DmsPayment::getPayTime).between(DmsPayment::getCreateTime, from, to))));

        Map<String, DmsPayment> byTradeNo = new HashMap<>();
        Map<String, DmsPayment> byExternalNo = new HashMap<>();
        for (DmsPayment p : payments) {
            if (StringUtils.hasText(p.getTradeNo())) {
                byTradeNo.putIfAbsent(p.getTradeNo(), p);
            }
            if (StringUtils.hasText(p.getExternalOrderNo())) {
                byExternalNo.putIfAbsent(p.getExternalOrderNo(), p);
            }
        }
        Set<Long> usedPaymentIds = new HashSet<>();
        for (DmsPaymentFlow f : flows) {
            if (f.getMatchStatus() != null && f.getMatchStatus() == 1 && f.getPaymentId() != null) {
                usedPaymentIds.add(f.getPaymentId());
            }
        }
        Map<Long, String> taskNoMap = loadTaskNos(payments);

        int matched = 0, flowOnly = 0, amountMismatch = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        for (DmsPaymentFlow f : flows) {
            if (f.getMatchStatus() != null && f.getMatchStatus() == 1) {
                matched++; // 已匹配：保留历史结果，不重复处理
                continue;
            }
            if (f.getMatchStatus() != null && f.getMatchStatus() == 3) {
                continue; // 已人工忽略：尊重人工判定，不再自动改判
            }
            DmsPayment p = StringUtils.hasText(f.getTradeNo()) ? byTradeNo.get(f.getTradeNo()) : null;
            int matchType = 1;
            if (p == null && StringUtils.hasText(f.getOutTradeNo())) {
                p = byExternalNo.get(f.getOutTradeNo());
                matchType = 2;
            }
            String now = LocalDateTime.now().format(DT_FMT);
            if (p == null) {
                flowOnly++;
                f.setMatchStatus(2);
                f.setPaymentId(null);
                f.setMatchType(null);
                f.setMatchTime(LocalDateTime.now());
                f.setRemark("系统无此笔（未找到对应收款记录）");
                addDetail(details, "FLOW_ONLY", f, null, "系统无此笔", taskNoMap);
            } else if (usedPaymentIds.contains(p.getId())) {
                flowOnly++;
                f.setMatchStatus(2);
                f.setPaymentId(p.getId());
                f.setMatchTime(LocalDateTime.now());
                f.setRemark("重复流水：该收款记录已被其他流水匹配");
                addDetail(details, "DUPLICATE", f, p, "重复匹配（收款记录已被占用）", taskNoMap);
            } else if (f.getAmount() != null && p.getAmount() != null
                    && f.getAmount().compareTo(p.getAmount()) != 0) {
                amountMismatch++;
                f.setMatchStatus(2);
                f.setPaymentId(p.getId());
                f.setMatchType(matchType);
                f.setMatchTime(LocalDateTime.now());
                f.setRemark("金额不符：流水 " + f.getAmount().stripTrailingZeros().toPlainString()
                        + " ≠ 系统 " + p.getAmount().stripTrailingZeros().toPlainString());
                addDetail(details, "AMOUNT_MISMATCH", f, p, f.getRemark(), taskNoMap);
            } else {
                matched++;
                usedPaymentIds.add(p.getId());
                f.setMatchStatus(1);
                f.setPaymentId(p.getId());
                f.setMatchType(matchType);
                f.setMatchTime(LocalDateTime.now());
                f.setRemark(matchType == 1 ? "自动匹配（平台交易号）" : "自动匹配（商户单号）");
                addDetail(details, "MATCHED", f, p, f.getRemark(), taskNoMap);
            }
            flowMapper.updateById(f);
            log.debug("对账 {}: tradeNo={}, status={}", now, f.getTradeNo(), f.getMatchStatus());
        }

        // 系统已支付、流水缺失（掉单）
        int systemOnly = 0;
        for (DmsPayment p : payments) {
            if (usedPaymentIds.contains(p.getId())) {
                continue;
            }
            if (!StringUtils.hasText(p.getTradeNo()) && !StringUtils.hasText(p.getExternalOrderNo())) {
                continue;
            }
            if (existsInFlows(flows, p)) {
                continue;
            }
            systemOnly++;
            if (details.size() < DETAIL_LIMIT) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("type", "SYSTEM_ONLY");
                d.put("paymentId", p.getId());
                d.put("tradeNo", p.getTradeNo());
                d.put("outTradeNo", p.getExternalOrderNo());
                d.put("systemAmount", p.getAmount());
                d.put("taskNo", taskNoMap.get(p.getTaskId()));
                d.put("remark", "系统已支付但平台流水缺失（掉单）");
                details.add(d);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start.toString());
        result.put("endDate", end.toString());
        result.put("channelCode", channelCode);
        result.put("flowTotal", flows.size());
        result.put("paymentTotal", payments.size());
        result.put("matched", matched);
        result.put("flowOnly", flowOnly);
        result.put("amountMismatch", amountMismatch);
        result.put("systemOnly", systemOnly);
        result.put("diffTotal", flowOnly + amountMismatch + systemOnly);
        result.put("detailTruncated", details.size() >= DETAIL_LIMIT);
        result.put("details", details);
        result.put("reconciledAt", LocalDateTime.now().format(DT_FMT));
        log.info("对账完成 [{}, {}]: 流水={}, 已支付={}, 匹配={}, 差异={}（无此笔{} / 金额不符{} / 掉单{}）",
                start, end, flows.size(), payments.size(), matched, result.get("diffTotal"),
                flowOnly, amountMismatch, systemOnly);
        return result;
    }

    /** 该收款记录是否存在对应流水（不论匹配状态），避免把"金额不符"误报为掉单 */
    private static boolean existsInFlows(List<DmsPaymentFlow> flows, DmsPayment p) {
        for (DmsPaymentFlow f : flows) {
            if (f.getPaymentId() != null && f.getPaymentId().equals(p.getId())) {
                return true;
            }
            if (StringUtils.hasText(p.getTradeNo()) && p.getTradeNo().equals(f.getTradeNo())) {
                return true;
            }
            if (StringUtils.hasText(p.getExternalOrderNo())
                    && p.getExternalOrderNo().equals(f.getOutTradeNo())) {
                return true;
            }
        }
        return false;
    }

    private void addDetail(List<Map<String, Object>> details, String type,
                           DmsPaymentFlow f, DmsPayment p, String remark, Map<Long, String> taskNoMap) {
        if (details.size() >= DETAIL_LIMIT) {
            return;
        }
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("type", type);
        d.put("flowId", f.getId());
        d.put("tradeNo", f.getTradeNo());
        d.put("outTradeNo", f.getOutTradeNo());
        d.put("flowAmount", f.getAmount());
        d.put("paymentId", p == null ? f.getPaymentId() : p.getId());
        d.put("systemAmount", p == null ? null : p.getAmount());
        d.put("taskNo", p == null ? null : taskNoMap.get(p.getTaskId()));
        d.put("remark", remark);
        details.add(d);
    }

    /** 批量取任务编号（对账明细下钻用，避免逐行查库） */
    private Map<Long, String> loadTaskNos(List<DmsPayment> payments) {
        List<Long> taskIds = payments.stream().map(DmsPayment::getTaskId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return taskMapper.selectBatchIds(taskIds).stream()
                .filter(t -> t.getTaskNo() != null)
                .collect(Collectors.toMap(DmsTask::getId, DmsTask::getTaskNo, (a, b) -> a));
    }

    // ═══════════════════════════════════════════════
    // 人工匹配 / 忽略
    // ═══════════════════════════════════════════════

    /** 人工匹配（自动匹配失败或金额不符时，人工指定收款记录） */
    @Transactional(rollbackFor = Exception.class)
    public DmsPaymentFlow manualMatch(Long flowId, Long paymentId) {
        DmsPaymentFlow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            throw new DmsBusinessException("支付流水不存在: " + flowId);
        }
        DmsPayment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new DmsBusinessException("收款记录不存在: " + paymentId);
        }
        if (payment.getStatus() == null || payment.getStatus() != 1) {
            throw new DmsBusinessException("仅已支付的收款记录可参与对账匹配");
        }
        flow.setMatchStatus(1);
        flow.setPaymentId(paymentId);
        flow.setMatchType(2);
        flow.setMatchTime(LocalDateTime.now());
        flow.setRemark("人工匹配");
        flowMapper.updateById(flow);
        log.info("流水分个人工匹配: flowId={}, tradeNo={} → paymentId={}", flowId, flow.getTradeNo(), paymentId);
        return flow;
    }

    /** 忽略差异（渠道测试单/误报，保留忽略原因） */
    @Transactional(rollbackFor = Exception.class)
    public DmsPaymentFlow ignore(Long flowId, String remark) {
        DmsPaymentFlow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            throw new DmsBusinessException("支付流水不存在: " + flowId);
        }
        flow.setMatchStatus(3);
        flow.setMatchTime(LocalDateTime.now());
        flow.setRemark(StringUtils.hasText(remark) ? remark : "人工忽略差异");
        flowMapper.updateById(flow);
        return flow;
    }

    // ── 行解析工具 ──

    private static String str(Map<String, Object> row, String... keys) {
        for (String k : keys) {
            Object v = row.get(k);
            if (v != null && StringUtils.hasText(String.valueOf(v))) {
                return String.valueOf(v).trim();
            }
        }
        return null;
    }

    private static BigDecimal decimal(Map<String, Object> row, String... keys) {
        String raw = str(row, keys);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.replace(",", "").replace("¥", "").replace("￥", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDateTime datetime(Map<String, Object> row, String... keys) {
        String raw = str(row, keys);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        for (DateTimeFormatter fmt : DT_PARSERS) {
            try {
                return LocalDateTime.parse(raw, fmt);
            } catch (Exception ignored) {
                // 试下一个格式
            }
        }
        for (DateTimeFormatter fmt : D_PARSERS) {
            try {
                return LocalDate.parse(raw, fmt).atStartOfDay();
            } catch (Exception ignored) {
                // 试下一个格式
            }
        }
        log.warn("流水成交时间无法解析，已置空: {}", raw);
        return null;
    }
}
