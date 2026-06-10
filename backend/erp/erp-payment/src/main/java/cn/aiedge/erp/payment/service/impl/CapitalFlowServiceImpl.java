package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.erp.payment.entity.CapitalFlow;
import cn.aiedge.erp.payment.entity.Offset;
import cn.aiedge.erp.payment.entity.Payment;
import cn.aiedge.erp.payment.entity.PrePayment;
import cn.aiedge.erp.payment.entity.PreReceipt;
import cn.aiedge.erp.payment.entity.Receipt;
import cn.aiedge.erp.payment.mapper.CapitalFlowMapper;
import cn.aiedge.erp.payment.service.CapitalFlowService;
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
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CapitalFlowServiceImpl extends ServiceImpl<CapitalFlowMapper, CapitalFlow> implements CapitalFlowService {

    @Override
    public Page<CapitalFlow> pageList(String flowType, String direction, String partyType,
                                       Long partyId, LocalDate startDate, LocalDate endDate,
                                       int pageNum, int pageSize) {
        LambdaQueryWrapper<CapitalFlow> wrapper = new LambdaQueryWrapper<>();
        if (flowType != null && !flowType.isEmpty()) {
            wrapper.eq(CapitalFlow::getFlowType, flowType);
        }
        if (direction != null && !direction.isEmpty()) {
            wrapper.eq(CapitalFlow::getDirection, direction);
        }
        if (partyType != null && !partyType.isEmpty()) {
            wrapper.eq(CapitalFlow::getPartyType, partyType);
        }
        if (partyId != null) {
            wrapper.eq(CapitalFlow::getPartyId, partyId);
        }
        if (startDate != null) {
            wrapper.ge(CapitalFlow::getOccurDate, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(CapitalFlow::getOccurDate, endDate.atTime(23, 59, 59));
        }
        wrapper.orderByDesc(CapitalFlow::getOccurDate);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CapitalFlow createFlow(CapitalFlow flow) {
        flow.setFlowNo(generateFlowNo());
        if (flow.getTenantId() == null) {
            flow.setTenantId(1L);
        }
        if (flow.getOccurDate() == null) {
            flow.setOccurDate(LocalDateTime.now());
        }
        save(flow);
        return flow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordReceiptFlow(Receipt receipt) {
        CapitalFlow flow = new CapitalFlow();
        flow.setFlowType("RECEIPT");
        flow.setDirection("IN");
        flow.setRefId(receipt.getId());
        flow.setRefNo(receipt.getReceiptNo());
        flow.setRefType("Receipt");
        flow.setAmount(receipt.getReceiptAmount());
        flow.setPartyType("customer");
        flow.setPartyId(receipt.getCustomerId());
        flow.setPartyName(receipt.getCustomerName());
        flow.setBusinessType("receipt");
        flow.setOccurDate(receipt.getCreateTime() != null ? receipt.getCreateTime() : LocalDateTime.now());
        flow.setPaymentMethod(parsePaymentMethodCode(receipt.getPaymentMethod()));
        flow.setBankAccount(receipt.getBankAccount());
        flow.setBankName(receipt.getBankName());
        flow.setTransactionNo(receipt.getTransactionNo());
        flow.setRemark("收款 - " + receipt.getReceiptNo());
        createFlow(flow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPaymentFlow(Payment payment) {
        CapitalFlow flow = new CapitalFlow();
        flow.setFlowType("PAYMENT");
        flow.setDirection("OUT");
        flow.setRefId(payment.getId());
        flow.setRefNo(payment.getPaymentNo());
        flow.setRefType("Payment");
        flow.setAmount(payment.getPaymentAmount());
        flow.setPartyType("supplier");
        flow.setPartyId(payment.getSupplierId());
        flow.setPartyName(payment.getSupplierName());
        flow.setBusinessType("payment");
        flow.setOccurDate(payment.getCreateTime() != null ? payment.getCreateTime() : LocalDateTime.now());
        flow.setPaymentMethod(parsePaymentMethodCode(payment.getPaymentMethod()));
        flow.setBankAccount(payment.getBankAccount());
        flow.setBankName(payment.getBankName());
        flow.setTransactionNo(payment.getTransactionNo());
        flow.setRemark("付款 - " + payment.getPaymentNo());
        createFlow(flow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPreReceiptFlow(PreReceipt preReceipt) {
        CapitalFlow flow = new CapitalFlow();
        flow.setFlowType("PRE_RECEIPT");
        flow.setDirection("IN");
        flow.setRefId(preReceipt.getId());
        flow.setRefNo(preReceipt.getPreReceiptNo());
        flow.setRefType("PreReceipt");
        flow.setAmount(preReceipt.getAmount());
        flow.setPartyType("customer");
        flow.setPartyId(preReceipt.getCustomerId());
        flow.setPartyName(preReceipt.getCustomerName());
        flow.setBusinessType("pre_receipt");
        flow.setOccurDate(LocalDateTime.now());
        flow.setPaymentMethod(preReceipt.getPaymentMethod());
        flow.setBankAccount(preReceipt.getBankAccount());
        flow.setBankName(preReceipt.getBankName());
        flow.setTransactionNo(preReceipt.getTransactionNo());
        flow.setRemark("预收款 - " + preReceipt.getPreReceiptNo());
        createFlow(flow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPrePaymentFlow(PrePayment prePayment) {
        CapitalFlow flow = new CapitalFlow();
        flow.setFlowType("PRE_PAYMENT");
        flow.setDirection("OUT");
        flow.setRefId(prePayment.getId());
        flow.setRefNo(prePayment.getPrePaymentNo());
        flow.setRefType("PrePayment");
        flow.setAmount(prePayment.getAmount());
        flow.setPartyType("supplier");
        flow.setPartyId(prePayment.getSupplierId());
        flow.setPartyName(prePayment.getSupplierName());
        flow.setBusinessType("pre_payment");
        flow.setOccurDate(LocalDateTime.now());
        flow.setPaymentMethod(prePayment.getPaymentMethod());
        flow.setBankAccount(prePayment.getBankAccount());
        flow.setBankName(prePayment.getBankName());
        flow.setTransactionNo(prePayment.getTransactionNo());
        flow.setRemark("预付款 - " + prePayment.getPrePaymentNo());
        createFlow(flow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordOffsetFlow(Offset offset) {
        // Create IN flow for receivable side
        CapitalFlow receivableFlow = new CapitalFlow();
        receivableFlow.setFlowType("OFFSET");
        receivableFlow.setDirection("IN");
        receivableFlow.setRefId(offset.getId());
        receivableFlow.setRefNo(offset.getOffsetNo());
        receivableFlow.setRefType("Offset");
        receivableFlow.setAmount(offset.getOffsetAmount());
        receivableFlow.setPartyType(offset.getPartyType());
        receivableFlow.setPartyId(offset.getPartyId());
        receivableFlow.setPartyName(offset.getPartyName());
        receivableFlow.setBusinessType("offset_receivable");
        receivableFlow.setOccurDate(LocalDateTime.now());
        receivableFlow.setRemark("对冲-应收冲减 - " + offset.getOffsetNo());
        createFlow(receivableFlow);

        // Create OUT flow for payable side
        CapitalFlow payableFlow = new CapitalFlow();
        payableFlow.setFlowType("OFFSET");
        payableFlow.setDirection("OUT");
        payableFlow.setRefId(offset.getId());
        payableFlow.setRefNo(offset.getOffsetNo());
        payableFlow.setRefType("Offset");
        payableFlow.setAmount(offset.getOffsetAmount());
        payableFlow.setPartyType(offset.getPartyType());
        payableFlow.setPartyId(offset.getPartyId());
        payableFlow.setPartyName(offset.getPartyName());
        payableFlow.setBusinessType("offset_payable");
        payableFlow.setOccurDate(LocalDateTime.now());
        payableFlow.setRemark("对冲-应付冲减 - " + offset.getOffsetNo());
        createFlow(payableFlow);
    }

    @Override
    public List<CapitalFlow> exportList(String flowType, String direction, String partyType,
                                         Long partyId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<CapitalFlow> wrapper = new LambdaQueryWrapper<>();
        if (flowType != null && !flowType.isEmpty()) {
            wrapper.eq(CapitalFlow::getFlowType, flowType);
        }
        if (direction != null && !direction.isEmpty()) {
            wrapper.eq(CapitalFlow::getDirection, direction);
        }
        if (partyType != null && !partyType.isEmpty()) {
            wrapper.eq(CapitalFlow::getPartyType, partyType);
        }
        if (partyId != null) {
            wrapper.eq(CapitalFlow::getPartyId, partyId);
        }
        if (startDate != null) {
            wrapper.ge(CapitalFlow::getOccurDate, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(CapitalFlow::getOccurDate, endDate.atTime(23, 59, 59));
        }
        wrapper.orderByDesc(CapitalFlow::getOccurDate);
        return baseMapper.selectList(wrapper);
    }

    /**
     * Generate a unique flow number with prefix "CF" + yyyyMMdd + 4-digit sequence.
     */
    private String generateFlowNo() {
        String prefix = "CF";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<CapitalFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CapitalFlow::getFlowNo, prefix + dateStr)
                .orderByDesc(CapitalFlow::getFlowNo)
                .last("LIMIT 1");
        CapitalFlow last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getFlowNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    /**
     * Parse payment method string from Receipt/Payment entity to integer code.
     * Returns null if the input is already an integer or null.
     */
    private Integer parsePaymentMethodCode(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(paymentMethod);
        } catch (NumberFormatException e) {
            // If it's a descriptive string, default to 2 (bank transfer)
            return 2;
        }
    }
}
