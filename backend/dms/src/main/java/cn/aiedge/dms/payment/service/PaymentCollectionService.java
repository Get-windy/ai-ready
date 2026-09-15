package cn.aiedge.dms.payment.service;

import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.payment.dto.DmsPaymentVO;
import cn.aiedge.dms.payment.entity.DmsPayment;
import cn.aiedge.dms.payment.entity.DmsPaymentCollection;
import cn.aiedge.dms.payment.mapper.DmsPaymentCollectionMapper;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 未付（挂账）管理服务（《收款管理开发文档》§3.1-2「未付管理」/ §3.4-6）
 *
 * <p>管的是末端收款里**当场没收到钱**的单据：登记原因（挂账）→ 催收留痕 → 客户承诺 →
 * 实际收回（核销，`status` 3 → 1）。每一次动作都写 `dms_payment_collection`，
 * 支撑「催了几次、承诺什么时候付、收回多少」的可追溯。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCollectionService {

    private final DmsPaymentMapper paymentMapper;
    private final DmsPaymentCollectionMapper collectionMapper;
    private final DmsTaskMapper taskMapper;

    public static final int ACTION_URGE = 1;
    public static final int ACTION_WRITE_OFF = 2;
    public static final int ACTION_PROMISE = 3;

    public static final Map<Integer, String> ACTION_NAMES = Map.of(
            ACTION_URGE, "催收", ACTION_WRITE_OFF, "核销", ACTION_PROMISE, "承诺付款");

    // ═══════════════════════════════════════════════
    // 未付台账（挂账单据 + 催收聚合）
    // ═══════════════════════════════════════════════

    /** 挂账分页（status=3）+ 联查任务快照 + 催收/核销聚合 */
    public IPage<DmsPaymentVO> unpaidPage(String taskNo, String customerName, Long riderId,
                                          LocalDate startDate, LocalDate endDate, long current, long size) {
        List<Long> taskIds = null;
        if (StringUtils.hasText(taskNo) || StringUtils.hasText(customerName)) {
            taskIds = taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                            .like(StringUtils.hasText(taskNo), DmsTask::getTaskNo, taskNo)
                            .like(StringUtils.hasText(customerName), DmsTask::getCustomerName, customerName))
                    .stream().map(DmsTask::getId).collect(Collectors.toList());
            if (taskIds.isEmpty()) {
                return new Page<>(current, size);
            }
        }
        LambdaQueryWrapper<DmsPayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DmsPayment::getStatus, 3);
        wrapper.in(taskIds != null, DmsPayment::getTaskId, taskIds == null ? Collections.emptyList() : taskIds);
        wrapper.eq(riderId != null, DmsPayment::getRiderId, riderId);
        wrapper.ge(startDate != null, DmsPayment::getCreateTime, startDate == null ? null : startDate.atStartOfDay());
        wrapper.le(endDate != null, DmsPayment::getCreateTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        wrapper.orderByAsc(DmsPayment::getCreateTime).orderByAsc(DmsPayment::getId);
        IPage<DmsPayment> raw = paymentMapper.selectPage(new Page<>(current, size), wrapper);

        Page<DmsPaymentVO> result = new Page<>(current, size, raw.getTotal());
        List<DmsPaymentVO> rows = toVos(raw.getRecords());
        enrich(rows);
        result.setRecords(rows);
        return result;
    }

    /** 挂账汇总（未付笔数/未付金额/已催收笔数/承诺付款笔数/已核销金额） */
    public Map<String, Object> unpaidStat(Long riderId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DmsPayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DmsPayment::getStatus, 3);
        wrapper.eq(riderId != null, DmsPayment::getRiderId, riderId);
        wrapper.ge(startDate != null, DmsPayment::getCreateTime, startDate == null ? null : startDate.atStartOfDay());
        wrapper.le(endDate != null, DmsPayment::getCreateTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        List<DmsPayment> list = paymentMapper.selectList(wrapper);

        BigDecimal unpaidAmount = list.stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Long, Map<String, Object>> agg = aggregate(list.stream().map(DmsPayment::getId).collect(Collectors.toList()));

        int urged = 0, promised = 0;
        BigDecimal written = BigDecimal.ZERO;
        for (Map<String, Object> a : agg.values()) {
            if (asInt(a.get("urgeCount")) > 0) {
                urged++;
            }
            if (a.get("promiseDate") != null) {
                promised++;
            }
            written = written.add(asDecimal(a.get("writeOffAmount")));
        }
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("unpaidCount", list.size());
        stat.put("unpaidAmount", unpaidAmount.setScale(2, RoundingMode.HALF_UP));
        stat.put("urgedCount", urged);
        stat.put("promisedCount", promised);
        stat.put("writeOffAmount", written.setScale(2, RoundingMode.HALF_UP));
        stat.put("notUrgedCount", list.size() - urged);
        return stat;
    }

    // ═══════════════════════════════════════════════
    // 催收 / 承诺付款
    // ═══════════════════════════════════════════════

    /** 催收登记（挂账单据才有意义） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> urge(Long paymentId, String content, LocalDate promiseDate,
                                    Long operatorId, String operatorName) {
        DmsPayment payment = requireUnpaid(paymentId);
        if (!StringUtils.hasText(content)) {
            throw new DmsBusinessException("催收说明必填（如：电话催收 / 上门催收 / 客户承诺）");
        }
        insert(payment, ACTION_URGE, null, promiseDate, content, operatorId, operatorName);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", paymentId);
        result.put("urgeCount", countByAction(paymentId, ACTION_URGE));
        result.put("lastUrgeTime", LocalDateTime.now());
        result.put("promiseDate", promiseDate);
        log.info("未付催收登记: paymentId={}, promiseDate={}, content={}", paymentId, promiseDate, content);
        return result;
    }

    /** 客户承诺付款日登记 */
    @Transactional(rollbackFor = Exception.class)
    public DmsPaymentCollection promise(Long paymentId, LocalDate promiseDate, String content,
                                        Long operatorId, String operatorName) {
        DmsPayment payment = requireUnpaid(paymentId);
        if (promiseDate == null) {
            throw new DmsBusinessException("承诺付款日必填");
        }
        return insert(payment, ACTION_PROMISE, null, promiseDate, content, operatorId, operatorName);
    }

    // ═══════════════════════════════════════════════
    // 核销（挂账收回 → 已支付）
    // ═══════════════════════════════════════════════

    /**
     * 核销（挂账收回）
     *
     * <p>累计核销金额达到应收 → `status` 3 → 1（已支付），并记录核销时间；
     * 未达则为部分核销，单据仍留在未付台账，直到收清。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> writeOff(Long paymentId, BigDecimal amount, Integer payChannel,
                                        String content, Long operatorId, String operatorName) {
        DmsPayment payment = requireUnpaid(paymentId);
        BigDecimal add = amount == null ? BigDecimal.ZERO : amount;
        if (add.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DmsBusinessException("核销金额必须大于 0");
        }
        BigDecimal due = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        BigDecimal written = sumWriteOff(paymentId);
        BigDecimal total = written.add(add);
        if (total.compareTo(due) > 0) {
            throw new DmsBusinessException("核销金额超出应收（应收 " + due.setScale(2, RoundingMode.HALF_UP)
                    + "，已核销 " + written.setScale(2, RoundingMode.HALF_UP) + "）");
        }
        insert(payment, ACTION_WRITE_OFF, add, null, content, operatorId, operatorName);

        boolean cleared = total.compareTo(due) >= 0;
        if (cleared) {
            payment.setStatus(1);
            payment.setPayTime(LocalDateTime.now());
            if (payChannel != null) {
                payment.setPayChannel(payChannel);
                payment.setPayChannelName(PaymentService.payChannelName(payChannel));
            }
            paymentMapper.updateById(payment);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", paymentId);
        result.put("amount", add.setScale(2, RoundingMode.HALF_UP));
        result.put("writeOffTotal", total.setScale(2, RoundingMode.HALF_UP));
        result.put("dueAmount", due.setScale(2, RoundingMode.HALF_UP));
        result.put("cleared", cleared);
        result.put("status", payment.getStatus());
        log.info("挂账核销: paymentId={}, +{}, 累计={}/{}, 结清={}", paymentId, add, total, due, cleared);
        return result;
    }

    /** 挂账单据的动作流水（催收/承诺/核销） */
    public List<DmsPaymentCollection> collections(Long paymentId) {
        requirePayment(paymentId);
        return collectionMapper.selectList(new LambdaQueryWrapper<DmsPaymentCollection>()
                .eq(DmsPaymentCollection::getPaymentId, paymentId)
                .orderByDesc(DmsPaymentCollection::getCreateTime)
                .orderByDesc(DmsPaymentCollection::getId));
    }

    // ═══════════════════════════════════════════════
    // 聚合（供台账/未付台账补齐催收与核销列）
    // ═══════════════════════════════════════════════

    /** 按收款记录聚合催收次数 / 最近催收时间 / 最近承诺付款日 / 累计核销金额 */
    public Map<Long, Map<String, Object>> aggregate(List<Long> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DmsPaymentCollection> list = collectionMapper.selectList(
                new LambdaQueryWrapper<DmsPaymentCollection>()
                        .in(DmsPaymentCollection::getPaymentId, paymentIds)
                        .orderByAsc(DmsPaymentCollection::getCreateTime)
                        .orderByAsc(DmsPaymentCollection::getId));
        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (DmsPaymentCollection c : list) {
            Map<String, Object> agg = result.computeIfAbsent(c.getPaymentId(), k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("urgeCount", 0);
                m.put("lastUrgeTime", null);
                m.put("promiseDate", null);
                m.put("writeOffAmount", BigDecimal.ZERO);
                return m;
            });
            if (c.getActionType() != null && c.getActionType() == ACTION_URGE) {
                agg.put("urgeCount", asInt(agg.get("urgeCount")) + 1);
                agg.put("lastUrgeTime", c.getCreateTime() != null ? c.getCreateTime() : c.getUpdateTime());
            }
            if (c.getActionType() != null && c.getActionType() == ACTION_PROMISE && c.getPromiseDate() != null) {
                agg.put("promiseDate", c.getPromiseDate());
            }
            if (c.getActionType() != null && c.getActionType() == ACTION_WRITE_OFF) {
                agg.put("writeOffAmount", asDecimal(agg.get("writeOffAmount")).add(asDecimal(c.getAmount())));
            }
        }
        return result;
    }

    /** 给台账行补齐催收/核销派生列 */
    public void enrich(List<DmsPaymentVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, Map<String, Object>> agg = aggregate(rows.stream()
                .map(DmsPaymentVO::getId).filter(Objects::nonNull).collect(Collectors.toList()));
        for (DmsPaymentVO vo : rows) {
            Map<String, Object> a = agg.get(vo.getId());
            vo.setUrgeCount(a == null ? 0 : asInt(a.get("urgeCount")));
            vo.setLastUrgeTime(a == null ? null : (LocalDateTime) a.get("lastUrgeTime"));
            vo.setPromiseDate(a == null ? null : (LocalDate) a.get("promiseDate"));
            vo.setWriteOffAmount(a == null ? BigDecimal.ZERO : asDecimal(a.get("writeOffAmount")));
        }
    }

    // ═══════════════════════════════════════════════
    // 内部工具
    // ═══════════════════════════════════════════════

    private DmsPayment requirePayment(Long paymentId) {
        DmsPayment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new DmsBusinessException("收款记录不存在: " + paymentId);
        }
        return payment;
    }

    private DmsPayment requireUnpaid(Long paymentId) {
        DmsPayment payment = requirePayment(paymentId);
        if (payment.getStatus() == null || payment.getStatus() != 3) {
            throw new DmsBusinessException("仅未付（挂账）记录可催收/核销，当前收款记录状态："
                    + PaymentService.statusName(payment.getStatus()));
        }
        return payment;
    }

    private DmsPaymentCollection insert(DmsPayment payment, int actionType, BigDecimal amount,
                                        LocalDate promiseDate, String content,
                                        Long operatorId, String operatorName) {
        DmsPaymentCollection c = new DmsPaymentCollection();
        c.setTenantId(payment.getTenantId());
        c.setPaymentId(payment.getId());
        c.setTaskId(payment.getTaskId());
        c.setActionType(actionType);
        c.setAmount(amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP));
        c.setPromiseDate(promiseDate);
        c.setContent(content);
        c.setOperatorId(operatorId);
        c.setOperatorName(StringUtils.hasText(operatorName) ? operatorName : "系统");
        collectionMapper.insert(c);
        return c;
    }

    private long countByAction(Long paymentId, int actionType) {
        Long n = collectionMapper.selectCount(new LambdaQueryWrapper<DmsPaymentCollection>()
                .eq(DmsPaymentCollection::getPaymentId, paymentId)
                .eq(DmsPaymentCollection::getActionType, actionType));
        return n == null ? 0 : n;
    }

    private BigDecimal sumWriteOff(Long paymentId) {
        return collectionMapper.selectList(new LambdaQueryWrapper<DmsPaymentCollection>()
                        .eq(DmsPaymentCollection::getPaymentId, paymentId)
                        .eq(DmsPaymentCollection::getActionType, ACTION_WRITE_OFF))
                .stream().map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 挂账行 → VO（联查任务快照） */
    private List<DmsPaymentVO> toVos(List<DmsPayment> records) {
        List<DmsPaymentVO> rows = new ArrayList<>();
        if (records == null || records.isEmpty()) {
            return rows;
        }
        List<Long> taskIds = records.stream().map(DmsPayment::getTaskId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, DmsTask> taskMap = taskIds.isEmpty() ? Collections.emptyMap()
                : taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(DmsTask::getId, t -> t, (a, b) -> a));
        for (DmsPayment p : records) {
            DmsPaymentVO vo = new DmsPaymentVO();
            org.springframework.beans.BeanUtils.copyProperties(p, vo);
            DmsTask t = taskMap.get(p.getTaskId());
            if (t != null) {
                vo.setTaskNo(t.getTaskNo());
                vo.setCustomerName(t.getCustomerName());
                vo.setCustomerPhone(t.getCustomerPhone());
                vo.setRiderName(t.getRiderName());
                vo.setTaskCollectOnDelivery(t.getCollectOnDelivery());
                vo.setTaskDeliveryFee(t.getDeliveryFee());
            }
            rows.add(vo);
        }
        return rows;
    }

    static int asInt(Object v) {
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    static BigDecimal asDecimal(Object v) {
        return v instanceof BigDecimal ? (BigDecimal) v : BigDecimal.ZERO;
    }
}
