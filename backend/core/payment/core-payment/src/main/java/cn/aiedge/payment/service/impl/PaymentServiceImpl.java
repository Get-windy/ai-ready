package cn.aiedge.payment.service.impl;

import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.payment.channel.ChannelPayResult;
import cn.aiedge.payment.channel.PaymentChannel;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.mapper.PaymentRequestMapper;
import cn.aiedge.payment.mapper.PaymentRecordMapper;
import cn.aiedge.payment.service.PaymentService;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SPACE_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PaymentRequestMapper requestMapper;
    private final PaymentRecordMapper recordMapper;
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
    public PaymentRequest createPayment(String bizType, Long bizId, String bizNo, BigDecimal amount, String channel) {
        PaymentChannel paymentChannel = getChannelMap().get(channel);
        if (paymentChannel == null) {
            throw new IllegalArgumentException("不支持的支付渠道: " + channel);
        }
        if (!paymentChannel.isAvailable()) {
            throw new IllegalArgumentException("支付渠道不可用: " + channel);
        }
        if (amount.compareTo(paymentChannel.getMinAmount()) < 0) {
            throw new IllegalArgumentException("支付金额低于最低限额");
        }
        if (amount.compareTo(paymentChannel.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("支付金额超过最高限额");
        }

        PaymentRequest request = new PaymentRequest();
        request.setBizType(bizType);
        request.setBizId(bizId);
        request.setBizNo(bizNo);
        request.setAmount(amount);
        request.setChannel(channel);
        request.setStatus(0); // 待支付
        request.setExpireTime(LocalDateTime.now().plusHours(24));

        requestMapper.insert(request);

        // 调用渠道下单：拿「渠道订单标识」+「客户端付款地址」
        ChannelPayResult payResult = paymentChannel.createPayment(request);
        // 渠道订单标识 = 提交给渠道的商户单号（支付宝/银联的 out_trade_no 口径），
        // 后续查单/关单/退款都以它为键
        request.setChannelOrderNo(payResult.channelOrderNo());
        request.setStatus(1); // 支付中
        requestMapper.updateById(request);
        // 付款地址**不落库**（见 PaymentRequest#payUrl 注释），只随本次响应回给前端
        request.setPayUrl(payResult.payUrl());

        return request;
    }

    @Override
    public PageResult<PaymentRequest> pagePaymentRequest(Integer pageNum, Integer pageSize, String bizType, String bizNo,
                                                         String channel, Integer status, String startTime, String endTime,
                                                         String payerName) {
        LambdaQueryWrapper<PaymentRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(bizType != null, PaymentRequest::getBizType, bizType);
        wrapper.like(hasText(bizNo), PaymentRequest::getBizNo, bizNo);
        wrapper.eq(channel != null, PaymentRequest::getChannel, channel);
        wrapper.eq(status != null, PaymentRequest::getStatus, status);
        // 付款人（payer_name）模糊查询：原先前端仅能展示/导出该列，无法按付款人筛选
        wrapper.like(hasText(payerName), PaymentRequest::getPayerName, payerName);
        LocalDateTime from = parseTime(startTime);
        LocalDateTime to = parseTime(endTime);
        wrapper.ge(from != null, PaymentRequest::getCreateTime, from);
        wrapper.lt(to != null, PaymentRequest::getCreateTime, endOfRange(to));
        wrapper.orderByDesc(PaymentRequest::getCreateTime).orderByDesc(PaymentRequest::getId);

        Page<PaymentRequest> page = requestMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public PaymentRequest getPaymentRequest(Long id) {
        return requestMapper.selectById(id);
    }

    @Override
    @Transactional
    public void cancelPayment(Long id) {
        PaymentRequest request = requestMapper.selectById(id);
        if (request == null) {
            throw new IllegalArgumentException("支付请求不存在");
        }
        if (request.getStatus() == 2) {
            throw new IllegalArgumentException("支付已完成，无法取消");
        }

        PaymentChannel channel = getChannelMap().get(request.getChannel());
        if (channel != null && request.getChannelOrderNo() != null) {
            channel.closePayment(request.getChannelOrderNo());
        }

        request.setStatus(3);
        requestMapper.updateById(request);
    }

    @Override
    @Transactional
    public PaymentRecord handleVerifiedCallback(Long tenantId, String channel, PaymentCallbackResult result) {
        // 只接受**已验签**的结果（调用方 PaymentController 在调本方法前已完成验签）。
        // 早先这里的入参是原始报文、并转交渠道实现的桩方法，那个桩无条件 setStatus(2) ——
        // 等于"不验签当成功"（审计 P0-2）。
        if (result == null || !result.success()) {
            throw new IllegalArgumentException("回调结果未通过验签或支付未成功，拒绝入账");
        }

        // 先关联我方支付请求：先用渠道单号匹配，其次用商户单号（out_trade_no = 我们的业务单号）。
        // ⚠️ 必须**先查再插**：支付记录要写 request_id 指回支付请求 ——
        //    原先先 insert 再查询，结果 payment_record.request_id 恒为 null，
        //    支付记录与支付请求**脱钩**（对账时按 request_id 关联会一行都查不到；
        //    真机侧表现为"清理测试数据时按 request_id 删不掉，留下残留"）。
        PaymentRequest request = null;
        if (result.channelOrderNo() != null) {
            request = requestMapper.selectOne(new LambdaQueryWrapper<PaymentRequest>()
                    .eq(PaymentRequest::getChannelOrderNo, result.channelOrderNo())
                    .last("LIMIT 1"));
        }
        if (request == null && result.merchantOrderNo() != null) {
            request = requestMapper.selectOne(new LambdaQueryWrapper<PaymentRequest>()
                    .eq(PaymentRequest::getBizNo, result.merchantOrderNo())
                    .last("LIMIT 1"));
        }
        if (request == null) {
            // 找不到对应支付请求：不入账（记录会带着 request_id=null 落库留痕，便于排查是谁在回调）
            log.warn("支付回调找不到对应的支付请求，仅留痕不入账：merchantOrderNo={}, channelOrderNo={}",
                    result.merchantOrderNo(), result.channelOrderNo());
        }

        PaymentRecord record = new PaymentRecord();
        // ⚠️ **必须显式写租户**：本方法由回调驱动，而回调是匿名的（无 Sa-Token 会话），
        //    BaseEntity 的 @TableField(fill = INSERT) 自动填充拿不到租户 ⇒ tenant_id 落 null ⇒
        //    payment_record.tenant_id NOT NULL 违约 ⇒ 回调业务失败（真机实测：
        //    "null value in column tenant_id of relation payment_record violates not-null constraint"）。
        //    这与会话内下单不同：那条路径有租户上下文，自动填充才有效。
        record.setTenantId(tenantId);
        record.setRequestId(request == null ? null : request.getId());
        record.setChannel(channel);   // 渠道码由控制器从路径传入（PaymentCallbackResult 里没有该字段）
        record.setChannelOrderNo(result.channelOrderNo());
        record.setChannelTradeNo(result.channelOrderNo());
        record.setAmount(result.paidAmount());
        record.setStatus(2);
        record.setCallbackTime(LocalDateTime.now());
        record.setCallbackData(result.rawPayload());
        recordMapper.insert(record);

        if (request == null) {
            // 没有对应支付请求：记录已留痕，但状态无从更新 —— 明确抛出让渠道重推（网关会再发）
            throw new IllegalArgumentException("回调找不到对应的支付请求：merchantOrderNo="
                    + result.merchantOrderNo() + ", channelOrderNo=" + result.channelOrderNo());
        }
        if (request.getStatus() != null && request.getStatus() != 2) {
            request.setStatus(2);
            request.setChannelTradeNo(result.channelOrderNo());
            request.setPaidTime(LocalDateTime.now());
            requestMapper.updateById(request);
        }
        log.info("支付回调入账成功：tenantId={}, bizNo={}, amount={}", tenantId,
                request.getBizNo(), result.paidAmount());
        return record;
    }

    @Override
    @Transactional
    public void confirmOfflinePayment(Long id, String channelOrderNo) {
        PaymentRequest request = requestMapper.selectById(id);
        if (request == null) {
            throw new IllegalArgumentException("支付请求不存在");
        }
        if (request.getStatus() != 1) {
            throw new IllegalArgumentException("支付状态不正确");
        }
        // 仅「线下」渠道可人工确认收款（与前端展示条件一致：CASH 现金 / BANK 银行转账）。
        // 在线渠道（ALIPAY / WECHAT / UNIONPAY）必须由渠道回调驱动入账，不得人工置为已支付——
        // 否则绕过前端即可把任意「支付中」的在线支付单确认收款。
        String ch = request.getChannel();
        if (!"CASH".equalsIgnoreCase(ch) && !"BANK".equalsIgnoreCase(ch)) {
            throw new IllegalArgumentException(
                    "渠道 " + ch + " 为在线支付，须由渠道回调确认，不支持人工确认收款");
        }

        request.setStatus(2);
        request.setChannelOrderNo(channelOrderNo);
        request.setPaidTime(LocalDateTime.now());
        requestMapper.updateById(request);

        PaymentRecord record = new PaymentRecord();
        record.setRequestId(id);
        record.setChannel(request.getChannel());
        record.setChannelOrderNo(channelOrderNo);
        record.setAmount(request.getAmount());
        record.setStatus(2);
        record.setCallbackTime(LocalDateTime.now());
        recordMapper.insert(record);
    }

    @Override
    public PageResult<PaymentRecord> pagePaymentRecord(Integer pageNum, Integer pageSize, String channel, Integer status,
                                                       String channelOrderNo, String startTime, String endTime) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channel != null, PaymentRecord::getChannel, channel);
        wrapper.eq(status != null, PaymentRecord::getStatus, status);
        wrapper.like(hasText(channelOrderNo), PaymentRecord::getChannelOrderNo, channelOrderNo);
        LocalDateTime from = parseTime(startTime);
        LocalDateTime to = parseTime(endTime);
        // 支付时间口径 = 回调时间（payment_record 无独立「支付时间」列）
        wrapper.ge(from != null, PaymentRecord::getCallbackTime, from);
        wrapper.lt(to != null, PaymentRecord::getCallbackTime, endOfRange(to));
        wrapper.orderByDesc(PaymentRecord::getCallbackTime).orderByDesc(PaymentRecord::getId);

        Page<PaymentRecord> page = recordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<String, Object> statPaymentRecord(String channel) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channel != null, PaymentRecord::getChannel, channel);
        List<PaymentRecord> records = recordMapper.selectList(wrapper);

        long successCount = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 2).count();
        BigDecimal successAmount = records.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 2)
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", records.size());
        data.put("successCount", successCount);
        data.put("failedCount", records.size() - successCount);
        data.put("successAmount", successAmount);
        return data;
    }

    @Override
    public Map<String, Object> statPaymentRequest(String channel) {
        LambdaQueryWrapper<PaymentRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channel != null, PaymentRequest::getChannel, channel);
        List<PaymentRequest> records = requestMapper.selectList(wrapper);

        long pending = records.stream()
                .filter(r -> r.getStatus() != null && (r.getStatus() == 0 || r.getStatus() == 1)).count();
        long success = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 2).count();
        long failed = records.stream()
                .filter(r -> r.getStatus() != null && (r.getStatus() == 3 || r.getStatus() == 4)).count();
        BigDecimal totalAmount = records.stream()
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingCount", pending);
        data.put("successCount", success);
        data.put("failedCount", failed);
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
     * 表现为 `/api/payment/record/page` 直接 500（2026-09-14 实踩）。返回 null 表示不设上界。</p>
     */
    private static LocalDateTime endOfRange(LocalDateTime to) {
        if (to == null) {
            return null;
        }
        return to.toLocalTime().equals(LocalTime.MIDNIGHT) ? to.plusDays(1) : to;
    }

    @Override
    public List<ChannelInfo> getAvailableChannels(BigDecimal amount) {
        return channels.stream()
                .filter(PaymentChannel::isAvailable)
                .filter(c -> amount.compareTo(c.getMinAmount()) >= 0 && amount.compareTo(c.getMaxAmount()) <= 0)
                .map(c -> new ChannelInfo(c.getChannelCode(), c.getChannelName(), c.getMinAmount(), c.getMaxAmount(), true))
                .collect(Collectors.toList());
    }
}