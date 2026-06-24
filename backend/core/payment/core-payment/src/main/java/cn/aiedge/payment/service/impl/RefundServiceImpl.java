package cn.aiedge.payment.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.payment.channel.PaymentChannel;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.entity.RefundRecord;
import cn.aiedge.payment.mapper.PaymentRequestMapper;
import cn.aiedge.payment.mapper.RefundRequestMapper;
import cn.aiedge.payment.mapper.RefundRecordMapper;
import cn.aiedge.payment.service.RefundService;
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
public class RefundServiceImpl implements RefundService {

    private final RefundRequestMapper requestMapper;
    private final RefundRecordMapper recordMapper;
    private final PaymentRequestMapper paymentRequestMapper;
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
    public PageResult<RefundRequest> pageRefundRequest(Integer pageNum, Integer pageSize, Integer status) {
        LambdaQueryWrapper<RefundRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, RefundRequest::getStatus, status);
        wrapper.orderByDesc(RefundRequest::getCreateTime);

        Page<RefundRequest> page = requestMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
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

        if (!approved) {
            request.setStatus(3); // 已拒绝
            request.setApproveRemark(remark);
            requestMapper.updateById(request);
            return;
        }

        request.setStatus(1); // 处理中
        request.setApproveRemark(remark);
        requestMapper.updateById(request);

        // 调用渠道退款
        PaymentRequest payment = paymentRequestMapper.selectById(request.getPaymentId());
        PaymentChannel channel = getChannelMap().get(payment.getChannel());
        if (channel != null) {
            String channelRefundNo = channel.createRefund(request, payment);
            request.setChannelRefundNo(channelRefundNo);
            requestMapper.updateById(request);
        }
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