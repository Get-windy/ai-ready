package cn.aiedge.payment.service.impl;

import cn.aiedge.common.result.PageResult;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

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

        // 调用渠道创建订单
        String channelOrderNo = paymentChannel.createPayment(request);
        request.setChannelOrderNo(channelOrderNo);
        request.setStatus(1); // 支付中
        requestMapper.updateById(request);

        return request;
    }

    @Override
    public PageResult<PaymentRequest> pagePaymentRequest(Integer pageNum, Integer pageSize, String bizType, String channel, Integer status) {
        LambdaQueryWrapper<PaymentRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(bizType != null, PaymentRequest::getBizType, bizType);
        wrapper.eq(channel != null, PaymentRequest::getChannel, channel);
        wrapper.eq(status != null, PaymentRequest::getStatus, status);
        wrapper.orderByDesc(PaymentRequest::getCreateTime);

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
    public PaymentRecord handleCallback(String channel, String callbackData) {
        PaymentChannel paymentChannel = getChannelMap().get(channel);
        if (paymentChannel == null) {
            throw new IllegalArgumentException("不支持的支付渠道: " + channel);
        }

        PaymentRecord record = paymentChannel.handleCallback(callbackData);
        record.setChannel(channel);
        record.setCallbackTime(LocalDateTime.now());
        recordMapper.insert(record);

        // 更新支付请求状态
        if (record.getStatus() == 2) {
            LambdaQueryWrapper<PaymentRequest> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentRequest::getChannelOrderNo, record.getChannelOrderNo());
            PaymentRequest request = requestMapper.selectOne(wrapper);
            if (request != null) {
                request.setStatus(2);
                request.setChannelTradeNo(record.getChannelTradeNo());
                request.setPaidTime(LocalDateTime.now());
                requestMapper.updateById(request);
            }
        }

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
    public PageResult<PaymentRecord> pagePaymentRecord(Integer pageNum, Integer pageSize, String channel) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channel != null, PaymentRecord::getChannel, channel);
        wrapper.orderByDesc(PaymentRecord::getCallbackTime);

        Page<PaymentRecord> page = recordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
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