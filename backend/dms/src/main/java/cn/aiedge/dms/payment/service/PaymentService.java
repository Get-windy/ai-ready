package cn.aiedge.dms.payment.service;

import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.config.service.ConfigService;
import cn.aiedge.dms.event.entity.DmsEventOutbox;
import cn.aiedge.dms.event.mapper.DmsEventOutboxMapper;
import cn.aiedge.dms.payment.dto.DmsPaymentVO;
import cn.aiedge.dms.payment.dto.PaymentQueryDTO;
import cn.aiedge.dms.payment.entity.DmsPayment;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 末端收款服务（配送 → 结算收款 → 收款管理，菜单 80920）
 *
 * <p>管的是配送员送货上门时向客户收取的钱，**两类科目不同**：</p>
 * <ul>
 *   <li><b>代收货款（paymentType=1）</b>：代商家/平台收的商品款 → 负债，需上交企业；</li>
 *   <li><b>配送费（paymentType=2）</b>：向客户收的服务费 → 企业收入。</li>
 * </ul>
 *
 * <p>金标准要点（《收款管理开发文档》§3）：收款码**配置化**（未配置不做扫码收款，杜绝假二维码）、
 * 支付回调**幂等**、资金上交/稽核（应上交 vs 已上交，含**交款时限超时预警**）、
 * **现金限额**（单笔/单日）守资金安全、**推送财务**幂等（生成收款单/核销应收）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final DmsPaymentMapper paymentMapper;
    private final DmsTaskMapper taskMapper;
    private final ConfigService configService;
    private final DmsEventOutboxMapper eventOutboxMapper;
    private final PaymentCollectionService collectionService;

    /** 支付方式字典（与实体 payChannel 对齐） */
    private static final Map<Integer, String> PAY_CHANNEL_NAMES = Map.of(
            1, "微信", 2, "支付宝", 3, "现金", 4, "POS", 5, "银行转账", 9, "其他");

    /** 收款类型字典（与实体 paymentType 对齐） */
    private static final Map<Integer, String> PAYMENT_TYPE_NAMES = Map.of(1, "代收货款", 2, "配送费");

    /** 现金支付方式（限额校验对象） */
    private static final int CHANNEL_CASH = 3;

    /** 导出上限（防止一次拉爆内存） */
    private static final int EXPORT_LIMIT = 5000;

    public static String payChannelName(Integer payChannel) {
        return payChannel == null ? null : PAY_CHANNEL_NAMES.getOrDefault(payChannel, "其他");
    }

    public static String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已退款";
            case 3 -> "未付(挂账)";
            default -> String.valueOf(status);
        };
    }

    // ═══════════════════════════════════════════════
    // 收款码（配置化，杜绝假二维码）
    // ═══════════════════════════════════════════════

    /**
     * 生成收款二维码
     *
     * <p>收款码服务地址读配置 `dms.payment.qrcode.base-url`：
     * <b>配置为空则不生成二维码</b>（`qrcodeUrl = null`），前端提示改用现金/POS 等线下方式——
     * 不再返回示例域名，避免出现"扫了不能付"的假二维码。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsPayment generateQrcode(Long taskId, BigDecimal amount) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        String baseUrl = readQrcodeBaseUrl();
        String qrcodeUrl = null;
        if (StringUtils.hasText(baseUrl)) {
            String sep = baseUrl.contains("?") ? "&" : "?";
            qrcodeUrl = baseUrl + sep + "amount=" + (amount == null ? "0" : amount.toPlainString())
                    + "&ref=" + (task.getTaskNo() == null ? taskId : task.getTaskNo());
        }

        DmsPayment payment = new DmsPayment();
        payment.setTaskId(taskId);
        payment.setTenantId(task.getTenantId());
        payment.setRiderId(task.getRiderId());
        payment.setAmount(amount);
        // 收款类型按任务口径落定（代收货款 > 0 → 代收货款），避免科目为空导致记账失真
        payment.setPaymentType(resolvePaymentType(taskId, null));
        payment.setQrcodeUrl(qrcodeUrl);
        payment.setStatus(0);
        payment.setAuditStatus(0);
        payment.setHandoverStatus(0);
        payment.setHandoverAmount(BigDecimal.ZERO);
        payment.setFinancePushStatus(0);
        paymentMapper.insert(payment);

        log.info("收款码{}: taskId={}, paymentId={}, amount={}",
                qrcodeUrl == null ? "未开通（无收款码服务配置）" : "已生成", taskId, payment.getId(), amount);
        return payment;
    }

    /** 收款码服务地址（配置化；为空表示未开通扫码收款） */
    private String readQrcodeBaseUrl() {
        try {
            String v = configService.getString(null, "dms.payment.qrcode.base-url");
            return StringUtils.hasText(v) ? v.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ═══════════════════════════════════════════════
    // 收款确认（线下）/ 支付回调（线上，幂等）
    // ═══════════════════════════════════════════════

    /**
     * 线下收款确认（现金 / POS / 银行转账）
     *
     * @param taskId          任务ID
     * @param payChannel      支付方式（1 微信 2 支付宝 3 现金 4 POS 5 银行转账 9 其他）
     * @param amount          实收金额
     * @param externalOrderNo 外部单号（POS 流水等）
     * @param paymentType     收款类型（1 代收货款 2 配送费；为空时按任务代收货款推断）
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsPayment confirmPayment(Long taskId, Integer payChannel, BigDecimal amount,
                                     String externalOrderNo, Integer paymentType) {
        DmsPayment payment = findPending(taskId);
        // 资金安全：现金单笔/单日限额校验（超限拒绝，引导改走扫码）
        checkCashLimit(taskId, payment == null ? null : payment.getId(), payChannel, amount);
        if (payment == null) {
            DmsTask task = taskMapper.selectById(taskId);
            if (task == null) {
                throw new DmsBusinessException("任务不存在: " + taskId);
            }
            payment = newPayment(task);
        }
        payment.setAmount(amount == null ? payment.getAmount() : amount);
        payment.setPayChannel(payChannel);
        payment.setPayChannelName(payChannelName(payChannel));
        payment.setPaymentType(resolvePaymentType(taskId, paymentType));
        payment.setExternalOrderNo(externalOrderNo);
        payment.setPayTime(LocalDateTime.now());
        payment.setStatus(1);
        if (payment.getId() != null) {
            paymentMapper.updateById(payment);
        } else {
            paymentMapper.insert(payment);
        }
        log.info("线下收款确认: taskId={}, channel={}, amount={}", taskId, payChannel, payment.getAmount());
        return payment;
    }

    /** 批量线下收款确认（逐条独立结果，一条失败不影响其余） */
    public Map<String, Object> confirmBatch(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new DmsBusinessException("批量确认内容为空");
        }
        int success = 0;
        List<Map<String, Object>> failed = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            try {
                Long taskId = longOf(row, "taskId");
                if (taskId == null) {
                    throw new DmsBusinessException("taskId 为空");
                }
                BigDecimal amount = decimalOf(row, "amount");
                if (amount == null) {
                    throw new DmsBusinessException("金额为空或非法");
                }
                confirmPayment(taskId, intOf(row, "payChannel"), amount,
                        strOf(row, "externalOrderNo"), intOf(row, "paymentType"));
                success++;
            } catch (Exception e) {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("taskId", row.get("taskId"));
                fail.put("reason", e.getMessage());
                failed.add(fail);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("success", success);
        result.put("failedCount", failed.size());
        result.put("failed", failed);
        log.info("批量收款确认: 总={}, 成功={}, 失败={}", rows.size(), success, failed.size());
        return result;
    }

    /** 收款类型：显式传入优先；否则按任务代收货款是否 > 0 推断 */
    private Integer resolvePaymentType(Long taskId, Integer explicit) {
        if (explicit != null) {
            return explicit;
        }
        DmsTask task = taskMapper.selectById(taskId);
        if (task != null && task.getCollectOnDelivery() != null
                && task.getCollectOnDelivery().compareTo(BigDecimal.ZERO) > 0) {
            return 1;
        }
        return 2;
    }

    /**
     * 支付回调（线上扫码支付结果通知）
     *
     * <p><b>幂等</b>：以 `tradeNo`（平台交易号）或「任务 + 待支付记录」为幂等键，
     * 重复回调直接返回既有记录、不重复置账。</p>
     *
     * @return 是否为重复回调（true = 幂等命中）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> callback(Long taskId, String tradeNo, Integer payChannel,
                                        BigDecimal amount, String externalOrderNo) {
        if (taskId == null) {
            throw new DmsBusinessException("回调缺少 taskId");
        }
        DmsPayment exist = null;
        if (StringUtils.hasText(tradeNo)) {
            exist = paymentMapper.selectOne(new LambdaQueryWrapper<DmsPayment>()
                    .eq(DmsPayment::getTradeNo, tradeNo)
                    .last("LIMIT 1"));
        }
        if (exist != null && exist.getStatus() != null && exist.getStatus() == 1) {
            log.info("支付回调幂等命中: tradeNo={}, paymentId={}", tradeNo, exist.getId());
            return result(exist, true);
        }
        DmsPayment payment = exist != null ? exist : findPending(taskId);
        DmsTask task = null;
        if (payment == null) {
            task = taskMapper.selectById(taskId);
            if (task == null) {
                throw new DmsBusinessException("任务不存在: " + taskId);
            }
            payment = newPayment(task);
            payment.setPaymentType(resolvePaymentType(taskId, null));
        }
        payment.setTradeNo(tradeNo);
        payment.setExternalOrderNo(externalOrderNo);
        // 复用的待支付记录（收款码生成）可能还没有收款类型 → 按任务口径补齐，否则分科目记账失真
        if (payment.getPaymentType() == null) {
            payment.setPaymentType(resolvePaymentType(taskId, null));
        }
        payment.setPayChannel(payChannel);
        payment.setPayChannelName(payChannelName(payChannel));
        if (amount != null) {
            payment.setAmount(amount);
        } else if (payment.getAmount() == null) {
            // 平台未回传金额时按任务应收兜底（代收货款 + 配送费）
            payment.setAmount(taskDue(task));
        }
        payment.setPayTime(LocalDateTime.now());
        payment.setCallbackTime(LocalDateTime.now());
        payment.setStatus(1);
        if (payment.getId() != null) {
            paymentMapper.updateById(payment);
        } else {
            paymentMapper.insert(payment);
        }
        log.info("支付回调已入账: taskId={}, tradeNo={}, amount={}", taskId, tradeNo, payment.getAmount());
        return result(payment, false);
    }

    private Map<String, Object> result(DmsPayment payment, boolean idempotent) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", payment.getId());
        result.put("taskId", payment.getTaskId());
        result.put("tradeNo", payment.getTradeNo());
        result.put("status", payment.getStatus());
        result.put("amount", payment.getAmount());
        result.put("idempotent", idempotent);
        return result;
    }

    /** 标记未付（挂账） */
    @Transactional(rollbackFor = Exception.class)
    public void markUnpaid(Long taskId, String remark) {
        DmsPayment payment = findPending(taskId);
        DmsTask task = null;
        if (payment == null) {
            task = taskMapper.selectById(taskId);
            if (task == null) {
                throw new DmsBusinessException("任务不存在: " + taskId);
            }
            payment = newPayment(task);
            payment.setPaymentType(resolvePaymentType(taskId, null));
        }
        // 挂账必须记清「欠多少」：应收 = 代收货款 + 配送费（新建记录时按任务口径补齐）
        if (payment.getAmount() == null) {
            if (task == null) {
                task = taskMapper.selectById(taskId);
            }
            payment.setAmount(taskDue(task));
        }
        payment.setStatus(3); // 3-未付（挂账）
        payment.setUnpaidRemark(remark);
        if (payment.getId() != null) {
            paymentMapper.updateById(payment);
        } else {
            paymentMapper.insert(payment);
        }
        log.info("未付标记成功: taskId={}, remark={}", taskId, remark);
    }

    public DmsPayment getByTaskId(Long taskId) {
        return paymentMapper.selectOne(
                new LambdaQueryWrapper<DmsPayment>()
                        .eq(DmsPayment::getTaskId, taskId)
                        .orderByDesc(DmsPayment::getCreateTime)
                        .last("LIMIT 1"));
    }

    /** 待支付记录（同任务最近一条 status=0） */
    private DmsPayment findPending(Long taskId) {
        return paymentMapper.selectOne(new LambdaQueryWrapper<DmsPayment>()
                .eq(DmsPayment::getTaskId, taskId)
                .eq(DmsPayment::getStatus, 0)
                .orderByDesc(DmsPayment::getCreateTime)
                .last("LIMIT 1"));
    }

    /** 任务应收金额（代收货款 + 配送费），用于挂账/回调缺金额时的兜底 */
    private static BigDecimal taskDue(DmsTask task) {
        if (task == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal cod = task.getCollectOnDelivery() == null ? BigDecimal.ZERO : task.getCollectOnDelivery();
        BigDecimal fee = task.getDeliveryFee() == null ? BigDecimal.ZERO : task.getDeliveryFee();
        return cod.add(fee).setScale(2, RoundingMode.HALF_UP);
    }

    private DmsPayment newPayment(DmsTask task) {
        DmsPayment payment = new DmsPayment();
        payment.setTaskId(task.getId());
        payment.setTenantId(task.getTenantId());
        payment.setRiderId(task.getRiderId());
        payment.setAuditStatus(0);
        payment.setHandoverStatus(0);
        payment.setHandoverAmount(BigDecimal.ZERO);
        payment.setFinancePushStatus(0);
        return payment;
    }

    // ═══════════════════════════════════════════════
    // 资金安全：现金限额（§3.4-2）
    // ═══════════════════════════════════════════════

    /**
     * 现金收款限额校验
     *
     * <p>单笔现金超过 `dms.payment.cash.limit.per.order` 或该配送员当日现金累计超过
     * `dms.payment.cash.limit.daily` 时**拒绝**（提示改走扫码/POS），避免配送员身上压大量现金。
     * 配置为 0 表示不限。</p>
     */
    private void checkCashLimit(Long taskId, Long excludePaymentId, Integer payChannel, BigDecimal amount) {
        if (payChannel == null || payChannel != CHANNEL_CASH || amount == null || amount.signum() <= 0) {
            return;
        }
        BigDecimal perOrder = readConfigDecimal("dms.payment.cash.limit.per.order", BigDecimal.ZERO);
        if (perOrder.signum() > 0 && amount.compareTo(perOrder) > 0) {
            throw new DmsBusinessException("单笔现金收款超过限额 "
                    + perOrder.stripTrailingZeros().toPlainString() + " 元，请改用扫码 / POS 收款");
        }
        BigDecimal daily = readConfigDecimal("dms.payment.cash.limit.daily", BigDecimal.ZERO);
        if (daily.signum() <= 0) {
            return;
        }
        DmsTask task = taskMapper.selectById(taskId);
        Long riderId = task == null ? null : task.getRiderId();
        if (riderId == null) {
            return;
        }
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        List<DmsPayment> todayCash = paymentMapper.selectList(new LambdaQueryWrapper<DmsPayment>()
                .eq(DmsPayment::getRiderId, riderId)
                .eq(DmsPayment::getPayChannel, CHANNEL_CASH)
                .eq(DmsPayment::getStatus, 1)
                .ge(DmsPayment::getPayTime, dayStart)
                .ne(excludePaymentId != null, DmsPayment::getId, excludePaymentId));
        BigDecimal already = todayCash.stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = already.add(amount);
        if (total.compareTo(daily) > 0) {
            throw new DmsBusinessException("该配送员今日现金收款将达 "
                    + total.setScale(2, RoundingMode.HALF_UP).toPlainString() + " 元，超过单日限额 "
                    + daily.stripTrailingZeros().toPlainString() + " 元，请改用扫码 / POS 收款");
        }
    }

    // ═══════════════════════════════════════════════
    // 台账（分页 + 联查任务/客户/配送员 + 催收/超时派生列）
    // ═══════════════════════════════════════════════

    public IPage<DmsPaymentVO> page(PaymentQueryDTO query) {
        LambdaQueryWrapper<DmsPayment> wrapper = buildWrapper(query);
        wrapper.orderByDesc(DmsPayment::getCreateTime).orderByDesc(DmsPayment::getId);
        IPage<DmsPayment> raw = paymentMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        Page<DmsPaymentVO> result = new Page<>(query.getCurrent(), query.getSize(), raw.getTotal());
        List<DmsPaymentVO> rows = toVos(raw.getRecords());
        collectionService.enrich(rows);
        markOverdue(rows);
        result.setRecords(rows);
        return result;
    }

    /** 导出数据（前端据此生成真实 xlsx；条数上限 EXPORT_LIMIT） */
    public List<DmsPaymentVO> exportList(PaymentQueryDTO query) {
        LambdaQueryWrapper<DmsPayment> wrapper = buildWrapper(query);
        wrapper.orderByDesc(DmsPayment::getCreateTime).orderByDesc(DmsPayment::getId);
        wrapper.last("LIMIT " + EXPORT_LIMIT);
        List<DmsPaymentVO> rows = toVos(paymentMapper.selectList(wrapper));
        collectionService.enrich(rows);
        markOverdue(rows);
        return rows;
    }

    private LambdaQueryWrapper<DmsPayment> buildWrapper(PaymentQueryDTO query) {
        LambdaQueryWrapper<DmsPayment> wrapper = new LambdaQueryWrapper<>();
        // 超时筛选会在 SQL 层强制「已支付 + 未交清 + 支付时间早于时限」
        BigDecimal deadlineHours = readConfigDecimal("dms.payment.handover.deadline.hours", BigDecimal.valueOf(24));
        if (Boolean.TRUE.equals(query.getOverdueOnly()) && deadlineHours.signum() > 0) {
            wrapper.eq(DmsPayment::getStatus, 1);
            wrapper.and(w -> w.isNull(DmsPayment::getHandoverStatus).or().ne(DmsPayment::getHandoverStatus, 2));
            wrapper.lt(DmsPayment::getPayTime,
                    LocalDateTime.now().minusHours(deadlineHours.longValue()));
        } else {
            wrapper.eq(query.getStatus() != null, DmsPayment::getStatus, query.getStatus());
            wrapper.eq(query.getHandoverStatus() != null, DmsPayment::getHandoverStatus, query.getHandoverStatus());
        }

        String kw = query.getKeyword();
        boolean hasTaskFilter = StringUtils.hasText(query.getTaskNo()) || StringUtils.hasText(query.getCustomerName());
        boolean hasKeyword = StringUtils.hasText(kw);
        if (hasTaskFilter || hasKeyword) {
            List<Long> taskIds = taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                            .like(StringUtils.hasText(query.getTaskNo()), DmsTask::getTaskNo, query.getTaskNo())
                            .like(StringUtils.hasText(query.getCustomerName()), DmsTask::getCustomerName, query.getCustomerName())
                            .and(hasKeyword, w -> w.like(DmsTask::getTaskNo, kw)
                                    .or().like(DmsTask::getCustomerName, kw)
                                    .or().like(DmsTask::getRiderName, kw)))
                    .stream().map(DmsTask::getId).collect(Collectors.toList());
            if (hasTaskFilter && taskIds.isEmpty()) {
                // 只按任务/客户过滤且无命中 → 空结果
                wrapper.eq(DmsPayment::getId, -1L);
            } else if (hasKeyword) {
                List<Long> ids = taskIds;
                wrapper.and(w -> {
                    if (!ids.isEmpty()) {
                        w.in(DmsPayment::getTaskId, ids).or();
                    }
                    w.like(DmsPayment::getTradeNo, kw).or().like(DmsPayment::getExternalOrderNo, kw);
                });
            } else {
                wrapper.in(DmsPayment::getTaskId, taskIds);
            }
        }

        wrapper.eq(query.getPaymentType() != null, DmsPayment::getPaymentType, query.getPaymentType());
        wrapper.eq(query.getPayChannel() != null, DmsPayment::getPayChannel, query.getPayChannel());
        wrapper.eq(query.getRiderId() != null, DmsPayment::getRiderId, query.getRiderId());
        LocalDate start = parseDate(query.getStartDate());
        LocalDate end = parseDate(query.getEndDate());
        wrapper.ge(start != null, DmsPayment::getCreateTime, start == null ? null : start.atStartOfDay());
        wrapper.le(end != null, DmsPayment::getCreateTime, end == null ? null : end.plusDays(1).atStartOfDay());
        return wrapper;
    }

    private List<DmsPaymentVO> toVos(List<DmsPayment> records) {
        List<DmsPaymentVO> rows = new ArrayList<>();
        if (records == null || records.isEmpty()) {
            return rows;
        }
        List<Long> ids = records.stream().map(DmsPayment::getTaskId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, DmsTask> taskMap = ids.isEmpty() ? Collections.emptyMap()
                : taskMapper.selectBatchIds(ids).stream()
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

    /** 计算交款超时标记（已支付未交清 且 超过 `dms.payment.handover.deadline.hours`） */
    private void markOverdue(List<DmsPaymentVO> rows) {
        BigDecimal deadlineHours = readConfigDecimal("dms.payment.handover.deadline.hours", BigDecimal.valueOf(24));
        long deadline = deadlineHours.longValue();
        LocalDateTime now = LocalDateTime.now();
        for (DmsPaymentVO vo : rows) {
            boolean overdue = false;
            long overdueHours = 0;
            if (deadline > 0
                    && vo.getStatus() != null && vo.getStatus() == 1
                    && (vo.getHandoverStatus() == null || vo.getHandoverStatus() != 2)
                    && vo.getPayTime() != null) {
                long elapsed = Duration.between(vo.getPayTime(), now).toHours();
                if (elapsed > deadline) {
                    overdue = true;
                    overdueHours = elapsed - deadline;
                }
            }
            vo.setOverdue(overdue);
            vo.setOverdueHours(overdueHours);
        }
    }

    // ═══════════════════════════════════════════════
    // 资金上交 / 稽核
    // ═══════════════════════════════════════════════

    /**
     * 交款登记（配送员把代收/现金上交企业）
     *
     * <p>累计上交金额达到应收金额 → 交款状态置「已交」，否则「部分交」。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsPayment handover(Long id, BigDecimal amount, Long operatorId, String operatorName, String remark) {
        DmsPayment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new DmsBusinessException("收款记录不存在: " + id);
        }
        if (payment.getStatus() == null || payment.getStatus() != 1) {
            throw new DmsBusinessException("仅已支付的收款记录可登记交款（当前状态可能为待支付/未付）");
        }
        BigDecimal add = amount == null ? BigDecimal.ZERO : amount;
        if (add.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DmsBusinessException("交款金额必须大于 0");
        }
        BigDecimal already = payment.getHandoverAmount() == null ? BigDecimal.ZERO : payment.getHandoverAmount();
        BigDecimal total = already.add(add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal due = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        payment.setHandoverAmount(total);
        payment.setHandoverStatus(total.compareTo(due) >= 0 ? 2 : 1);
        payment.setHandoverTime(LocalDateTime.now());
        payment.setHandoverBy(operatorId);
        payment.setHandoverByName(operatorName);
        payment.setHandoverRemark(remark);
        paymentMapper.updateById(payment);
        log.info("交款登记: paymentId={}, +{}, 累计={}, due={}, status={}",
                id, add, total, due, payment.getHandoverStatus());
        return payment;
    }

    /**
     * 交款稽核汇总（按配送员）：应上交 vs 已上交 vs 未上交，并标出**超时限**笔数
     */
    public Map<String, Object> handoverSummary(Long riderId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DmsPayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DmsPayment::getStatus, 1); // 仅已支付（有资金需上交）
        wrapper.eq(riderId != null, DmsPayment::getRiderId, riderId);
        wrapper.ge(startDate != null, DmsPayment::getCreateTime, startDate == null ? null : startDate.atStartOfDay());
        wrapper.le(endDate != null, DmsPayment::getCreateTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        List<DmsPayment> list = paymentMapper.selectList(wrapper);

        Map<Long, List<DmsPayment>> grouped = list.stream()
                .filter(p -> p.getRiderId() != null)
                .collect(Collectors.groupingBy(DmsPayment::getRiderId, LinkedHashMap::new, Collectors.toList()));

        BigDecimal deadlineHours = readConfigDecimal("dms.payment.handover.deadline.hours", BigDecimal.valueOf(24));
        long deadline = deadlineHours.longValue();
        LocalDateTime now = LocalDateTime.now();

        BigDecimal dueTotal = BigDecimal.ZERO;
        BigDecimal handoverTotal = BigDecimal.ZERO;
        int overdueTotal = 0;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, List<DmsPayment>> e : grouped.entrySet()) {
            BigDecimal due = e.getValue().stream()
                    .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal done = e.getValue().stream()
                    .map(p -> p.getHandoverAmount() == null ? BigDecimal.ZERO : p.getHandoverAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long pending = e.getValue().stream().filter(p -> p.getHandoverStatus() == null || p.getHandoverStatus() != 2).count();
            int overdue = (int) e.getValue().stream().filter(p -> isOverdue(p, deadline, now)).count();
            dueTotal = dueTotal.add(due);
            handoverTotal = handoverTotal.add(done);
            overdueTotal += overdue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("riderId", e.getKey());
            row.put("riderName", "配送员#" + e.getKey());
            row.put("recordCount", e.getValue().size());
            row.put("dueAmount", due.setScale(2, RoundingMode.HALF_UP));
            row.put("handoverAmount", done.setScale(2, RoundingMode.HALF_UP));
            row.put("pendingCount", pending);
            row.put("overdueCount", overdue);
            row.put("diffAmount", due.subtract(done).setScale(2, RoundingMode.HALF_UP));
            rows.add(row);
        }
        // 用任务表补配送员姓名（dms_payment 未存姓名快照）
        List<Long> riderIds = new ArrayList<>(grouped.keySet());
        if (!riderIds.isEmpty()) {
            List<DmsTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                    .in(DmsTask::getRiderId, riderIds)
                    .select(DmsTask::getRiderId, DmsTask::getRiderName));
            Map<Long, String> nameMap = tasks.stream()
                    .filter(t -> t.getRiderId() != null && StringUtils.hasText(t.getRiderName()))
                    .collect(Collectors.toMap(DmsTask::getRiderId, DmsTask::getRiderName, (a, b) -> a));
            rows.forEach(r -> r.put("riderName", nameMap.getOrDefault((Long) r.get("riderId"), "配送员#" + r.get("riderId"))));
        }
        // 超时限未交明细（资金安全追责用）
        List<Map<String, Object>> overdueList = list.stream()
                .filter(p -> isOverdue(p, deadline, now))
                .sorted(Comparator.comparing(DmsPayment::getPayTime))
                .limit(100)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("paymentId", p.getId());
                    m.put("taskId", p.getTaskId());
                    m.put("riderId", p.getRiderId());
                    m.put("amount", p.getAmount());
                    m.put("payTime", p.getPayTime());
                    m.put("overdueHours", p.getPayTime() == null ? 0
                            : Duration.between(p.getPayTime(), now).toHours() - deadline);
                    return m;
                }).collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("startDate", startDate == null ? null : startDate.toString());
        summary.put("endDate", endDate == null ? null : endDate.toString());
        summary.put("deadlineHours", deadline);
        summary.put("riders", rows);
        summary.put("dueTotal", dueTotal.setScale(2, RoundingMode.HALF_UP));
        summary.put("handoverTotal", handoverTotal.setScale(2, RoundingMode.HALF_UP));
        summary.put("unhandoverTotal", dueTotal.subtract(handoverTotal).setScale(2, RoundingMode.HALF_UP));
        summary.put("overdueCount", overdueTotal);
        summary.put("overdueList", overdueList);
        return summary;
    }

    private static boolean isOverdue(DmsPayment p, long deadlineHours, LocalDateTime now) {
        return deadlineHours > 0
                && p.getStatus() != null && p.getStatus() == 1
                && (p.getHandoverStatus() == null || p.getHandoverStatus() != 2)
                && p.getPayTime() != null
                && Duration.between(p.getPayTime(), now).toHours() > deadlineHours;
    }

    /** 收款统计（随查询条件）：笔数/金额/待收/未付/已交/代收货款/配送费/超时未交 */
    public Map<String, Object> stat(PaymentQueryDTO query) {
        List<DmsPayment> list = paymentMapper.selectList(buildWrapper(query));
        BigDecimal deadlineHours = readConfigDecimal("dms.payment.handover.deadline.hours", BigDecimal.valueOf(24));
        long deadline = deadlineHours.longValue();
        LocalDateTime now = LocalDateTime.now();

        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal unpaidAmount = BigDecimal.ZERO;
        BigDecimal codAmount = BigDecimal.ZERO;
        BigDecimal feeAmount = BigDecimal.ZERO;
        BigDecimal handoverDone = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;
        int paidCount = 0;
        int unpaidCount = 0;
        int overdueCount = 0;
        for (DmsPayment p : list) {
            BigDecimal amt = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
            if (p.getStatus() != null && p.getStatus() == 1) {
                paidCount++;
                paidAmount = paidAmount.add(amt);
                if (p.getPaymentType() != null && p.getPaymentType() == 1) {
                    codAmount = codAmount.add(amt);
                } else {
                    feeAmount = feeAmount.add(amt);
                }
                handoverDone = handoverDone.add(p.getHandoverAmount() == null ? BigDecimal.ZERO : p.getHandoverAmount());
                if (isOverdue(p, deadline, now)) {
                    overdueCount++;
                    overdueAmount = overdueAmount.add(amt);
                }
            } else if (p.getStatus() != null && p.getStatus() == 3) {
                unpaidCount++;
                unpaidAmount = unpaidAmount.add(amt);
            }
        }
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("totalCount", list.size());
        stat.put("paidCount", paidCount);
        stat.put("unpaidCount", unpaidCount);
        stat.put("paidAmount", paidAmount.setScale(2, RoundingMode.HALF_UP));
        stat.put("unpaidAmount", unpaidAmount.setScale(2, RoundingMode.HALF_UP));
        stat.put("codAmount", codAmount.setScale(2, RoundingMode.HALF_UP));
        stat.put("deliveryFeeAmount", feeAmount.setScale(2, RoundingMode.HALF_UP));
        stat.put("handoverAmount", handoverDone.setScale(2, RoundingMode.HALF_UP));
        stat.put("unhandoverAmount", paidAmount.subtract(handoverDone).setScale(2, RoundingMode.HALF_UP));
        stat.put("overdueCount", overdueCount);
        stat.put("overdueAmount", overdueAmount.setScale(2, RoundingMode.HALF_UP));
        stat.put("deadlineHours", deadline);
        return stat;
    }

    // ═══════════════════════════════════════════════
    // 财务打通（推 ERP 生成收款单 / 核销应收，幂等）
    // ═══════════════════════════════════════════════

    /**
     * 推送财务（幂等）
     *
     * <p>把已支付的收款记录外发到 ERP 财务：<b>代收货款</b>冲减代收负债、
     * <b>配送费</b>确认其他业务收入。同一记录只推一次（`finance_push_status`），
     * 重复调用返回既有 traceId；实际出账由事件发件箱消费方完成。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pushFinance(Long paymentId) {
        return doPushFinance(requirePayment(paymentId));
    }

    /** 批量推送财务（逐单反馈：一条失败不影响其余） */
    public Map<String, Object> pushFinanceBatch(List<Long> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            throw new DmsBusinessException("请选择要推送的收款记录");
        }
        List<Map<String, Object>> success = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        int idempotent = 0;
        for (Long id : paymentIds) {
            try {
                Map<String, Object> r = doPushFinance(requirePayment(id));
                if (Boolean.TRUE.equals(r.get("idempotent"))) {
                    idempotent++;
                }
                success.add(r);
            } catch (Exception e) {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("paymentId", id);
                fail.put("reason", e.getMessage());
                failed.add(fail);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", paymentIds.size());
        result.put("success", success.size());
        result.put("idempotentCount", idempotent);
        result.put("failedCount", failed.size());
        result.put("failed", failed);
        result.put("details", success);
        return result;
    }

    private Map<String, Object> doPushFinance(DmsPayment payment) {
        if (payment.getStatus() == null || payment.getStatus() != 1) {
            throw new DmsBusinessException("仅已支付的收款记录可推送财务，当前状态：" + statusName(payment.getStatus()));
        }
        if (payment.getFinancePushStatus() != null && payment.getFinancePushStatus() == 1) {
            Map<String, Object> again = new LinkedHashMap<>();
            again.put("paymentId", payment.getId());
            again.put("traceId", payment.getFinanceTraceId());
            again.put("idempotent", true);
            return again;
        }
        DmsTask task = payment.getTaskId() == null ? null : taskMapper.selectById(payment.getTaskId());
        String traceId = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("taskId", payment.getTaskId());
        payload.put("taskNo", task == null ? null : task.getTaskNo());
        payload.put("customerName", task == null ? null : task.getCustomerName());
        payload.put("paymentType", payment.getPaymentType());
        payload.put("paymentTypeText", PAYMENT_TYPE_NAMES.getOrDefault(payment.getPaymentType(), "未知"));
        payload.put("amount", payment.getAmount());
        payload.put("payChannel", payment.getPayChannel());
        payload.put("payChannelName", payment.getPayChannelName());
        payload.put("tradeNo", payment.getTradeNo());
        payload.put("payTime", String.valueOf(payment.getPayTime()));

        DmsEventOutbox outbox = new DmsEventOutbox();
        outbox.setTraceId(traceId);
        outbox.setEventType("PAYMENT_PUSH_FINANCE");
        outbox.setSource("DMS");
        outbox.setTarget("ERP");
        outbox.setPayload(payload.toString());
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        eventOutboxMapper.insert(outbox);

        payment.setFinancePushStatus(1);
        payment.setFinanceTraceId(traceId);
        paymentMapper.updateById(payment);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", payment.getId());
        result.put("taskNo", payload.get("taskNo"));
        result.put("paymentType", payment.getPaymentType());
        result.put("amount", payment.getAmount());
        result.put("traceId", traceId);
        result.put("eventId", outbox.getId());
        result.put("idempotent", false);
        log.info("收款记录已推送财务: paymentId={}, taskNo={}, 科目={}, amount={}, traceId={}",
                payment.getId(), payload.get("taskNo"), payload.get("paymentTypeText"),
                payment.getAmount(), traceId);
        return result;
    }

    private DmsPayment requirePayment(Long paymentId) {
        DmsPayment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new DmsBusinessException("收款记录不存在: " + paymentId);
        }
        return payment;
    }

    /** 支付方式字典（前端下拉用） */
    public Map<String, Object> dict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("payChannels", PAY_CHANNEL_NAMES);
        dict.put("paymentTypes", PAYMENT_TYPE_NAMES);
        dict.put("qrcodeEnabled", StringUtils.hasText(readQrcodeBaseUrl()));
        dict.put("cashLimitPerOrder", readConfigDecimal("dms.payment.cash.limit.per.order", BigDecimal.ZERO));
        dict.put("cashLimitDaily", readConfigDecimal("dms.payment.cash.limit.daily", BigDecimal.ZERO));
        dict.put("handoverDeadlineHours", readConfigDecimal("dms.payment.handover.deadline.hours", BigDecimal.valueOf(24)));
        return dict;
    }

    // ═══════════════════════════════════════════════
    // 配置读取
    // ═══════════════════════════════════════════════

    /** 读取数值型配置（租户覆盖 → 全局缺省 → 内置缺省） */
    private BigDecimal readConfigDecimal(String key, BigDecimal fallback) {
        try {
            String raw = configService.getString(null, key);
            if (StringUtils.hasText(raw)) {
                return new BigDecimal(raw.trim());
            }
        } catch (Exception e) {
            log.debug("收款配置缺失，回落缺省: key={}, fallback={}", key, fallback);
        }
        return fallback;
    }

    private static LocalDate parseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    private static Long longOf(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer intOf(Map<String, Object> row, String key) {
        Long v = longOf(row, key);
        return v == null ? null : v.intValue();
    }

    private static BigDecimal decimalOf(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(v).replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String strOf(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
