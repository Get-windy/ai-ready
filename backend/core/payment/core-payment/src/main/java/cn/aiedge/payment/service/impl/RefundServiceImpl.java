package cn.aiedge.payment.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.payment.channel.PaymentChannel;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.entity.RefundRecord;
import cn.aiedge.payment.mapper.PaymentRequestMapper;
import cn.aiedge.payment.mapper.RefundRequestMapper;
import cn.aiedge.payment.mapper.RefundRecordMapper;
import cn.aiedge.payment.service.RefundService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final DateTimeFormatter SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SPACE_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RefundRequestMapper requestMapper;
    private final RefundRecordMapper recordMapper;
    private final PaymentRequestMapper paymentRequestMapper;
    private final SysUserMapper sysUserMapper;
    private final List<PaymentChannel> channels;

    private Map<String, PaymentChannel> channelMap;

    private Map<String, PaymentChannel> getChannelMap() {
        if (channelMap == null) {
            channelMap = channels.stream()
                    .collect(Collectors.toMap(PaymentChannel::getChannelCode, Function.identity()));
        }
        return channelMap;
    }

    @Override
    @Transactional
    public RefundRequest createRefund(Long paymentId, BigDecimal amount, String reason) {
        PaymentRequest payment = paymentRequestMapper.selectById(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("支付请求不存在");
        }
        if (payment.getStatus() != 2) {
            throw new IllegalArgumentException("支付未完成，无法退款");
        }
        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("退款金额超过支付金额");
        }

        RefundRequest request = new RefundRequest();
        request.setPaymentId(paymentId);
        request.setAmount(amount);
        request.setReason(reason);
        request.setStatus(0); // 待处理

        requestMapper.insert(request);
        return request;
    }

    @Override
    public PageResult<RefundRequest> pageRefundRequest(Integer pageNum, Integer pageSize, Integer status, String refundNo,
                                                       String startTime, String endTime, String channel) {
        LambdaQueryWrapper<RefundRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, RefundRequest::getStatus, status);
        if (hasText(refundNo)) {
            String kw = refundNo.trim();
            // 单号口径：渠道退款号 / 支付请求ID（前端「单号」同时支持两者）
            if (kw.matches("\\d+")) {
                wrapper.and(w -> w.like(RefundRequest::getChannelRefundNo, kw)
                        .or().eq(RefundRequest::getPaymentId, Long.valueOf(kw)));
            } else {
                wrapper.like(RefundRequest::getChannelRefundNo, kw);
            }
        }
        // 渠道过滤：refund_request 无渠道列 → 先取该渠道的支付请求 ID 集合（空集则不返回任何数据）
        if (hasText(channel)) {
            List<Long> paymentIds = paymentRequestMapper.selectList(
                            new LambdaQueryWrapper<PaymentRequest>().eq(PaymentRequest::getChannel, channel))
                    .stream().map(PaymentRequest::getId).collect(Collectors.toList());
            if (paymentIds.isEmpty()) {
                return PageResult.empty(pageNum, pageSize);
            }
            wrapper.in(RefundRequest::getPaymentId, paymentIds);
        }
        LocalDateTime from = parseTime(startTime);
        LocalDateTime to = parseTime(endTime);
        // 退款日期口径 = 申请创建时间（退款完成时间 refundedTime 仅回调成功后写入）
        wrapper.ge(from != null, RefundRequest::getCreateTime, from);
        wrapper.lt(to != null, RefundRequest::getCreateTime, endOfRange(to));
        wrapper.orderByDesc(RefundRequest::getCreateTime).orderByDesc(RefundRequest::getId);

        Page<RefundRequest> page = requestMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<String, Object> statRefundRequest(String channel) {
        LambdaQueryWrapper<RefundRequest> wrapper = new LambdaQueryWrapper<>();
        if (hasText(channel)) {
            List<Long> paymentIds = paymentRequestMapper.selectList(
                            new LambdaQueryWrapper<PaymentRequest>().eq(PaymentRequest::getChannel, channel))
                    .stream().map(PaymentRequest::getId).collect(Collectors.toList());
            if (paymentIds.isEmpty()) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("total", 0);
                empty.put("pendingCount", 0);
                empty.put("approvedCount", 0);
                empty.put("rejectedCount", 0);
                empty.put("totalAmount", BigDecimal.ZERO);
                return empty;
            }
            wrapper.in(RefundRequest::getPaymentId, paymentIds);
        }
        List<RefundRequest> records = requestMapper.selectList(wrapper);

        long pending = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 0).count();
        long approved = records.stream().filter(r -> r.getStatus() != null && (r.getStatus() == 1 || r.getStatus() == 2)).count();
        long rejected = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 3).count();
        BigDecimal totalAmount = records.stream()
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", records.size());
        data.put("pendingCount", pending);
        data.put("approvedCount", approved);
        data.put("rejectedCount", rejected);
        data.put("totalAmount", totalAmount);
        return data;
    }

    // ── 查询参数宽松解析（与交易模块 TimeParsers 同口径；core-payment 不依赖 core-base 的 trade 包） ──

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static LocalDateTime parseTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim().replace('T', ' ');
        if (value.endsWith("Z")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        int dot = value.indexOf('.');
        if (dot > 0) {
            value = value.substring(0, dot);
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{SPACE, SPACE_MINUTE}) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种格式
            }
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 结束时间只传日期（00:00:00）时按「含当日」处理。
     *
     * <p>⚠️ 必须容忍 `to == null`：调用方写的是
     * {@code wrapper.lt(to != null, col, endOfRange(to))}，而 Java 会**先求值实参**再传参，
     * 即条件为 false（不筛结束时间）时 `endOfRange(null)` 仍会被执行 →
     * 原实现在「不传时间区间」的默认查询下必抛
     * {@code NullPointerException: Cannot invoke "LocalDateTime.toLocalTime()" because "to" is null}，
     * 表现为 `/api/refund/request/page` 直接 500（2026-09-14 实踩）。返回 null 表示不设上界。</p>
     */
    private static LocalDateTime endOfRange(LocalDateTime to) {
        if (to == null) {
            return null;
        }
        return to.toLocalTime().equals(LocalTime.MIDNIGHT) ? to.plusDays(1) : to;
    }

    @Override
    public RefundRequest getRefundRequest(Long id) {
        return requestMapper.selectById(id);
    }

    @Override
    @Transactional
    public void approveRefund(Long id, boolean approved, String remark) {
        RefundRequest request = requestMapper.selectById(id);
        if (request == null) {
            throw new IllegalArgumentException("退款请求不存在");
        }
        if (request.getStatus() != 0) {
            throw new IllegalArgumentException("退款请求已处理");
        }

        // 审批留痕：审批人取当前登录用户（无登录上下文时留空，不阻断审批），
        // 姓名按 approver_id 关联 sys_user（real_name → nickname → username）回写，供详情抽屉展示。
        Long approverId = currentUserId();
        request.setApproverId(approverId);
        request.setApproverName(approverId == null ? null : resolveUserName(approverId));
        request.setApproveRemark(remark);
        // 处理时间 = 审批动作发生的时刻。批准与拒绝都要写（迁移注释即「审批（批准/拒绝）时写入」）；
        // 旧实现只在拒绝分支写入，导致「已批准」的单据处理时间恒为空。
        // 注意与 refundedTime 区分：refundedTime 仅在渠道回调成功后写入（实际退款完成时刻）。
        request.setProcessTime(LocalDateTime.now());

        if (!approved) {
            request.setStatus(3); // 已拒绝
            requestMapper.updateById(request);
            return;
        }

        request.setStatus(1); // 处理中
        requestMapper.updateById(request);

        // 调用渠道退款
        PaymentRequest payment = paymentRequestMapper.selectById(request.getPaymentId());
        PaymentChannel channel = payment == null ? null : getChannelMap().get(payment.getChannel());
        if (channel != null) {
            String channelRefundNo = channel.createRefund(request, payment);
            request.setChannelRefundNo(channelRefundNo);
            requestMapper.updateById(request);
        }
    }

    /** 当前登录用户 ID（无登录上下文 → null，不阻断审批） */
    private static Long currentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId != null ? Long.parseLong(loginId.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 审批人姓名：按 sys_user 主键查询，取 real_name → nickname → username 首个非空值
     *
     * <p>sys_user 在租户插件忽略表中（登录/跨租户查询需要），此处按主键查询不会误伤多租户隔离。</p>
     */
    private String resolveUserName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        if (hasText(user.getRealName())) {
            return user.getRealName();
        }
        if (hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    @Override
    @Transactional
    public RefundRecord handleCallback(String channel, String callbackData) {
        PaymentChannel paymentChannel = getChannelMap().get(channel);
        if (paymentChannel == null) {
            throw new IllegalArgumentException("不支持的支付渠道: " + channel);
        }

        RefundRecord record = paymentChannel.queryRefund(callbackData);
        if (record == null) {
            // 尝试从回调数据解析
            record = new RefundRecord();
            record.setChannel(channel);
            record.setCallbackData(callbackData);
            record.setStatus(1);
        }
        record.setChannel(channel);
        record.setCallbackTime(LocalDateTime.now());
        recordMapper.insert(record);

        // 更新退款请求状态
        if (record.getStatus() == 1) {
            LambdaQueryWrapper<RefundRequest> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RefundRequest::getChannelRefundNo, record.getChannelRefundNo());
            RefundRequest request = requestMapper.selectOne(wrapper);
            if (request != null) {
                request.setStatus(2);
                request.setRefundedTime(LocalDateTime.now());
                requestMapper.updateById(request);
            }
        }

        return record;
    }

    @Override
    public PageResult<RefundRecord> pageRefundRecord(Integer pageNum, Integer pageSize, String channel) {
        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channel != null, RefundRecord::getChannel, channel);
        wrapper.orderByDesc(RefundRecord::getCallbackTime);

        Page<RefundRecord> page = recordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }
}