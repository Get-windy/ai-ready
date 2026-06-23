package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.entity.CapitalFlow;
import cn.aiedge.erp.payment.entity.PreReceipt;
import cn.aiedge.erp.payment.mapper.PreReceiptMapper;
import cn.aiedge.erp.payment.service.CapitalFlowService;
import cn.aiedge.erp.payment.service.PreReceiptService;
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
public class PreReceiptServiceImpl extends ServiceImpl<PreReceiptMapper, PreReceipt> implements PreReceiptService {

    private final CapitalFlowService capitalFlowService;
    private final PaymentAccountingService paymentAccountingService;

    @Override
    public PreReceipt getByPreReceiptNo(String preReceiptNo) {
        return lambdaQuery()
                .eq(PreReceipt::getPreReceiptNo, preReceiptNo)
                .eq(PreReceipt::getDeleted, 0)
                .one();
    }

    @Override
    public Page<PreReceipt> pageList(String keyword, Long customerId, String status, int pageNum, int pageSize) {
        LambdaQueryWrapper<PreReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PreReceipt::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(PreReceipt::getPreReceiptNo, keyword)
                    .or().like(PreReceipt::getCustomerName, keyword)
                    .or().like(PreReceipt::getSourceNo, keyword)
                    .or().like(PreReceipt::getTransactionNo, keyword));
        }
        if (customerId != null) {
            wrapper.eq(PreReceipt::getCustomerId, customerId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PreReceipt::getStatus, status);
        }
        wrapper.orderByDesc(PreReceipt::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreReceipt createPreReceipt(PreReceipt receipt) {
        receipt.setPreReceiptNo(generatePreReceiptNo());
        receipt.setTenantId(1L);
        receipt.setUsedAmount(BigDecimal.ZERO);
        receipt.setRemainingAmount(receipt.getAmount());
        receipt.setStatus("received");
        receipt.setReceiptDate(LocalDate.now());
        save(receipt);

        // Record capital flow
        capitalFlowService.recordPreReceiptFlow(receipt);

        // Call accounting service to create receipt voucher
        try {
            paymentAccountingService.postReceiptVoucher(
                    receipt.getId(),
                    receipt.getPreReceiptNo(),
                    String.valueOf(receipt.getCustomerId()),
                    receipt.getCustomerName(),
                    receipt.getAmount(),
                    null
            );
        } catch (Exception e) {
            log.error("调用财务模块创建预收款凭证失败: preReceiptNo={}, error={}", receipt.getPreReceiptNo(), e.getMessage(), e);
        }

        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offsetToReceipt(Long preReceiptId, Long receiptId, BigDecimal amount) {
        PreReceipt preReceipt = getById(preReceiptId);
        if (preReceipt == null) {
            throw BusinessException.notFound("预收款单不存在");
        }

        calculateRemaining(preReceiptId);
        preReceipt = getById(preReceiptId);

        if (amount.compareTo(preReceipt.getRemainingAmount()) > 0) {
            throw BusinessException.badRequest("冲抵金额不能大于剩余金额");
        }

        BigDecimal usedAmount = preReceipt.getUsedAmount() != null ? preReceipt.getUsedAmount() : BigDecimal.ZERO;
        preReceipt.setUsedAmount(usedAmount.add(amount));
        preReceipt.setRemainingAmount(preReceipt.getAmount().subtract(preReceipt.getUsedAmount()));

        if (preReceipt.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            preReceipt.setStatus("offset");
        }

        updateById(preReceipt);

        log.info("预收款冲抵收款单成功: preReceiptId={}, receiptId={}, amount={}", preReceiptId, receiptId, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreReceipt forfeit(Long id, String reason) {
        PreReceipt preReceipt = getById(id);
        if (preReceipt == null) {
            throw BusinessException.notFound("预收款单不存在");
        }
        if (!"received".equals(preReceipt.getStatus()) && !"offset".equals(preReceipt.getStatus())) {
            throw BusinessException.badRequest("当前状态的预收款单不能执行没收操作");
        }

        preReceipt.setStatus("forfeited");
        preReceipt.setRemark(reason);
        updateById(preReceipt);

        // Record forfeiture capital flow
        CapitalFlow flow = new CapitalFlow();
        flow.setFlowType("PRE_RECEIPT_FORFEIT");
        flow.setDirection("OUT");
        flow.setRefId(preReceipt.getId());
        flow.setRefNo(preReceipt.getPreReceiptNo());
        flow.setRefType("PreReceipt");
        flow.setAmount(preReceipt.getRemainingAmount());
        flow.setPartyType("customer");
        flow.setPartyId(preReceipt.getCustomerId());
        flow.setPartyName(preReceipt.getCustomerName());
        flow.setBusinessType("pre_receipt_forfeit");
        flow.setOccurDate(LocalDateTime.now());
        flow.setRemark(reason);
        capitalFlowService.createFlow(flow);

        return preReceipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreReceipt refund(Long id, String reason) {
        PreReceipt preReceipt = getById(id);
        if (preReceipt == null) {
            throw BusinessException.notFound("预收款单不存在");
        }
        if (!"received".equals(preReceipt.getStatus()) && !"offset".equals(preReceipt.getStatus())) {
            throw BusinessException.badRequest("当前状态的预收款单不能执行退款操作");
        }

        preReceipt.setStatus("refunded");
        preReceipt.setRemark(reason);
        updateById(preReceipt);

        // Record refund capital flow
        CapitalFlow flow = new CapitalFlow();
        flow.setFlowType("PRE_RECEIPT_REFUND");
        flow.setDirection("OUT");
        flow.setRefId(preReceipt.getId());
        flow.setRefNo(preReceipt.getPreReceiptNo());
        flow.setRefType("PreReceipt");
        flow.setAmount(preReceipt.getRemainingAmount());
        flow.setPartyType("customer");
        flow.setPartyId(preReceipt.getCustomerId());
        flow.setPartyName(preReceipt.getCustomerName());
        flow.setBusinessType("pre_receipt_refund");
        flow.setOccurDate(LocalDateTime.now());
        flow.setRemark(reason);
        capitalFlowService.createFlow(flow);

        return preReceipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateRemaining(Long id) {
        PreReceipt preReceipt = getById(id);
        if (preReceipt == null) {
            throw BusinessException.notFound("预收款单不存在");
        }
        BigDecimal used = preReceipt.getUsedAmount() != null ? preReceipt.getUsedAmount() : BigDecimal.ZERO;
        BigDecimal remaining = preReceipt.getAmount().subtract(used);
        preReceipt.setRemainingAmount(remaining);
        updateById(preReceipt);
    }

    @Override
    public String generatePreReceiptNo() {
        String prefix = "PR";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PreReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PreReceipt::getPreReceiptNo, prefix + dateStr)
                .eq(PreReceipt::getDeleted, 0)
                .orderByDesc(PreReceipt::getPreReceiptNo)
                .last("LIMIT 1");
        PreReceipt last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getPreReceiptNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }
}
