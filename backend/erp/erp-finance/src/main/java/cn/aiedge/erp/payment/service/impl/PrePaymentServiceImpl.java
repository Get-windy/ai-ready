package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.entity.CapitalFlow;
import cn.aiedge.erp.payment.entity.PrePayment;
import cn.aiedge.erp.payment.mapper.PrePaymentMapper;
import cn.aiedge.erp.payment.service.CapitalFlowService;
import cn.aiedge.erp.payment.service.PrePaymentService;
import cn.aiedge.erp.payment.service.integration.PaymentAccountingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrePaymentServiceImpl extends ServiceImpl<PrePaymentMapper, PrePayment> implements PrePaymentService {

    private final CapitalFlowService capitalFlowService;
    private final PaymentAccountingService paymentAccountingService;

    @Override
    public PrePayment getByPrePaymentNo(String prePaymentNo) {
        return lambdaQuery()
                .eq(PrePayment::getPrePaymentNo, prePaymentNo)
                .eq(PrePayment::getDeleted, 0)
                .one();
    }

    @Override
    public Page<PrePayment> pageList(String keyword, Long supplierId, String status, int pageNum, int pageSize) {
        LambdaQueryWrapper<PrePayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrePayment::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(PrePayment::getPrePaymentNo, keyword)
                    .or().like(PrePayment::getSupplierName, keyword)
                    .or().like(PrePayment::getSourceNo, keyword)
                    .or().like(PrePayment::getTransactionNo, keyword));
        }
        if (supplierId != null) {
            wrapper.eq(PrePayment::getSupplierId, supplierId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PrePayment::getStatus, status);
        }
        wrapper.orderByDesc(PrePayment::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrePayment createPrePayment(PrePayment payment) {
        payment.setPrePaymentNo(generatePrePaymentNo());
        payment.setTenantId(1L);
        payment.setUsedAmount(BigDecimal.ZERO);
        payment.setRemainingAmount(payment.getAmount());
        payment.setStatus("paid");
        payment.setPaymentDate(LocalDate.now());
        save(payment);

        // Record capital flow
        capitalFlowService.recordPrePaymentFlow(payment);

        // Call accounting service to create payment voucher
        try {
            paymentAccountingService.postPaymentVoucher(
                    payment.getId(),
                    payment.getPrePaymentNo(),
                    String.valueOf(payment.getSupplierId()),
                    payment.getSupplierName(),
                    payment.getAmount(),
                    null
            );
        } catch (Exception e) {
            log.error("调用财务模块创建预付款凭证失败: prePaymentNo={}, error={}", payment.getPrePaymentNo(), e.getMessage(), e);
        }

        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offsetToPayment(Long prePaymentId, Long paymentId, BigDecimal amount) {
        PrePayment prePayment = getById(prePaymentId);
        if (prePayment == null) {
            throw BusinessException.notFound("预付款单不存在");
        }

        calculateRemaining(prePaymentId);
        prePayment = getById(prePaymentId);

        if (amount.compareTo(prePayment.getRemainingAmount()) > 0) {
            throw BusinessException.badRequest("冲抵金额不能大于剩余金额");
        }

        BigDecimal usedAmount = prePayment.getUsedAmount() != null ? prePayment.getUsedAmount() : BigDecimal.ZERO;
        prePayment.setUsedAmount(usedAmount.add(amount));
        prePayment.setRemainingAmount(prePayment.getAmount().subtract(prePayment.getUsedAmount()));

        if (prePayment.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            prePayment.setStatus("offset");
        }

        updateById(prePayment);

        log.info("预付款冲抵付款单成功: prePaymentId={}, paymentId={}, amount={}", prePaymentId, paymentId, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrePayment recover(Long id, String reason) {
        PrePayment prePayment = getById(id);
        if (prePayment == null) {
            throw BusinessException.notFound("预付款单不存在");
        }
        if (!"paid".equals(prePayment.getStatus()) && !"offset".equals(prePayment.getStatus())) {
            throw BusinessException.badRequest("当前状态的预付款单不能执行收回操作");
        }

        prePayment.setStatus("recovered");
        prePayment.setRemark(reason);
        updateById(prePayment);

        // Record recovery capital flow (money coming back in)
        CapitalFlow flow = new CapitalFlow();
        flow.setFlowType("PRE_PAYMENT_RECOVER");
        flow.setDirection("IN");
        flow.setRefId(prePayment.getId());
        flow.setRefNo(prePayment.getPrePaymentNo());
        flow.setRefType("PrePayment");
        flow.setAmount(prePayment.getRemainingAmount());
        flow.setPartyType("supplier");
        flow.setPartyId(prePayment.getSupplierId());
        flow.setPartyName(prePayment.getSupplierName());
        flow.setBusinessType("pre_payment_recover");
        flow.setOccurDate(LocalDateTime.now());
        flow.setRemark(reason);
        capitalFlowService.createFlow(flow);

        return prePayment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrePayment refund(Long id, String reason) {
        PrePayment prePayment = getById(id);
        if (prePayment == null) {
            throw BusinessException.notFound("预付款单不存在");
        }
        if (!"paid".equals(prePayment.getStatus()) && !"offset".equals(prePayment.getStatus())) {
            throw BusinessException.badRequest("当前状态的预付款单不能执行退款操作");
        }

        prePayment.setStatus("refunded");
        prePayment.setRemark(reason);
        updateById(prePayment);

        // Record refund capital flow (supplier refunds, money comes back)
        CapitalFlow flow = new CapitalFlow();
        flow.setFlowType("PRE_PAYMENT_REFUND");
        flow.setDirection("IN");
        flow.setRefId(prePayment.getId());
        flow.setRefNo(prePayment.getPrePaymentNo());
        flow.setRefType("PrePayment");
        flow.setAmount(prePayment.getRemainingAmount());
        flow.setPartyType("supplier");
        flow.setPartyId(prePayment.getSupplierId());
        flow.setPartyName(prePayment.getSupplierName());
        flow.setBusinessType("pre_payment_refund");
        flow.setOccurDate(LocalDateTime.now());
        flow.setRemark(reason);
        capitalFlowService.createFlow(flow);

        return prePayment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateRemaining(Long id) {
        PrePayment prePayment = getById(id);
        if (prePayment == null) {
            throw BusinessException.notFound("预付款单不存在");
        }
        BigDecimal used = prePayment.getUsedAmount() != null ? prePayment.getUsedAmount() : BigDecimal.ZERO;
        BigDecimal remaining = prePayment.getAmount().subtract(used);
        prePayment.setRemainingAmount(remaining);
        updateById(prePayment);
    }

    @Override
    public String generatePrePaymentNo() {
        String prefix = "PP";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PrePayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PrePayment::getPrePaymentNo, prefix + dateStr)
                .eq(PrePayment::getDeleted, 0)
                .orderByDesc(PrePayment::getPrePaymentNo)
                .last("LIMIT 1");
        PrePayment last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getPrePaymentNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }
}
