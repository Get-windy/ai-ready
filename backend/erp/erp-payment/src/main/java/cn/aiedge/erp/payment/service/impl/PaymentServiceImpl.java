package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.erp.payment.entity.Payment;
import cn.aiedge.erp.payment.entity.PaymentItem;
import cn.aiedge.erp.payment.enums.ReceiptStatus;
import cn.aiedge.erp.payment.mapper.PaymentItemMapper;
import cn.aiedge.erp.payment.mapper.PaymentMapper;
import cn.aiedge.erp.payment.service.PaymentService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private final PaymentItemMapper paymentItemMapper;

    @Override
    public Payment getByPaymentNo(String paymentNo) {
        return lambdaQuery()
                .eq(Payment::getPaymentNo, paymentNo)
                .eq(Payment::getDeleted, 0)
                .one();
    }

    @Override
    public Page<Payment> pageList(String keyword, Long supplierId, Long orderId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Payment::getPaymentNo, keyword)
                    .or().like(Payment::getOrderNo, keyword)
                    .or().like(Payment::getSupplierName, keyword));
        }
        if (supplierId != null) {
            wrapper.eq(Payment::getSupplierId, supplierId);
        }
        if (orderId != null) {
            wrapper.eq(Payment::getOrderId, orderId);
        }
        if (status != null) {
            wrapper.eq(Payment::getStatus, status);
        }
        wrapper.orderByDesc(Payment::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Payment> listBySupplierId(Long supplierId) {
        return baseMapper.selectBySupplierId(supplierId);
    }

    @Override
    public List<Payment> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public String generatePaymentNo() {
        String prefix = "PY";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Payment::getPaymentNo, prefix + dateStr)
                .eq(Payment::getDeleted, 0)
                .orderByDesc(Payment::getPaymentNo)
                .last("LIMIT 1");
        Payment lastPayment = getOne(wrapper);
        int seq = 1;
        if (lastPayment != null) {
            String lastNo = lastPayment.getPaymentNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment createPayment(Payment payment, List<PaymentItem> items) {
        payment.setPaymentNo(generatePaymentNo());
        payment.setStatus(ReceiptStatus.DRAFT.getCode());
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentType(1);
        payment.setVerifiedAmount(BigDecimal.ZERO);
        payment.setPendingAmount(payment.getPaymentAmount());
        save(payment);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                PaymentItem item = items.get(i);
                item.setPaymentId(payment.getId());
                item.setLineNo(i + 1);
                item.setTenantId(payment.getTenantId());
                item.setVerifiedAmount(BigDecimal.ZERO);
                item.setVerifyStatus(0);
                paymentItemMapper.insert(item);
            }
        }
        calculateTotals(payment.getId());
        return getById(payment.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment createFromOrder(Long orderId) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentType(1);
        return createPayment(payment, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment createFromInvoice(Long invoiceId) {
        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentType(1);
        return createPayment(payment, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment updatePayment(Long paymentId, Payment payment, List<PaymentItem> items) {
        Payment existing = getById(paymentId);
        if (existing == null) {
            throw new RuntimeException("付款单不存在");
        }
        if (existing.getStatus() != ReceiptStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的付款单可以修改");
        }
        payment.setId(paymentId);
        payment.setPendingAmount(payment.getPaymentAmount());
        updateById(payment);
        if (items != null) {
            List<PaymentItem> existingItems = getItems(paymentId);
            for (PaymentItem oldItem : existingItems) {
                paymentItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                PaymentItem item = items.get(i);
                item.setPaymentId(paymentId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                item.setVerifiedAmount(BigDecimal.ZERO);
                item.setVerifyStatus(0);
                paymentItemMapper.insert(item);
            }
        }
        calculateTotals(paymentId);
        return getById(paymentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment submitForApproval(Long paymentId) {
        Payment payment = getById(paymentId);
        if (payment == null) {
            throw new RuntimeException("付款单不存在");
        }
        if (payment.getStatus() != ReceiptStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的付款单可以提交审批");
        }
        payment.setStatus(ReceiptStatus.PENDING_APPROVAL.getCode());
        updateById(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment approve(Long paymentId, Long approverId, String note) {
        Payment payment = getById(paymentId);
        if (payment == null) {
            throw new RuntimeException("付款单不存在");
        }
        if (payment.getStatus() != ReceiptStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的付款单可以审批");
        }
        payment.setStatus(ReceiptStatus.APPROVED.getCode());
        payment.setApprovedBy(approverId);
        payment.setApprovedTime(LocalDateTime.now());
        payment.setApprovedNote(note);
        updateById(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment reject(Long paymentId, String reason) {
        Payment payment = getById(paymentId);
        if (payment == null) {
            throw new RuntimeException("付款单不存在");
        }
        if (payment.getStatus() != ReceiptStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的付款单可以拒绝");
        }
        payment.setStatus(ReceiptStatus.DRAFT.getCode());
        payment.setRemark(reason);
        updateById(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment startVerify(Long paymentId) {
        Payment payment = getById(paymentId);
        if (payment == null) {
            throw new RuntimeException("付款单不存在");
        }
        if (payment.getStatus() != ReceiptStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的付款单可以开始核销");
        }
        payment.setStatus(ReceiptStatus.VERIFYING.getCode());
        updateById(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentItem verifyItem(Long itemId, BigDecimal verifyAmount) {
        PaymentItem item = paymentItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("核销明细不存在");
        }
        Payment payment = getById(item.getPaymentId());
        if (payment.getStatus() != ReceiptStatus.VERIFYING.getCode()) {
            throw new RuntimeException("只有核销中状态的付款单可以处理明细");
        }
        item.setVerifyAmount(verifyAmount);
        item.setVerifiedAmount(verifyAmount);
        item.setVerifyStatus(1);
        item.setVerifiedTime(LocalDateTime.now());
        item.setVerifiedBy(payment.getCreateBy());
        paymentItemMapper.updateById(item);
        calculateTotals(item.getPaymentId());
        return paymentItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment completeVerify(Long paymentId) {
        Payment payment = getById(paymentId);
        if (payment == null) {
            throw new RuntimeException("付款单不存在");
        }
        payment.setStatus(ReceiptStatus.VERIFIED.getCode());
        payment.setVerifiedBy(payment.getCreateBy());
        payment.setVerifiedTime(LocalDateTime.now());
        updateById(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment complete(Long paymentId) {
        Payment payment = getById(paymentId);
        if (payment == null) {
            throw new RuntimeException("付款单不存在");
        }
        if (payment.getStatus() != ReceiptStatus.VERIFIED.getCode()) {
            throw new RuntimeException("只有已核销状态的付款单可以完成");
        }
        payment.setStatus(ReceiptStatus.COMPLETED.getCode());
        payment.setCompletedBy(payment.getCreateBy());
        payment.setCompletedTime(LocalDateTime.now());
        updateById(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment cancel(Long paymentId, String reason) {
        Payment payment = getById(paymentId);
        if (payment == null) {
            throw new RuntimeException("付款单不存在");
        }
        if (payment.getStatus() == ReceiptStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的付款单不能取消");
        }
        payment.setStatus(ReceiptStatus.CANCELLED.getCode());
        payment.setRemark(reason);
        updateById(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long paymentId) {
        BigDecimal verifiedAmount = paymentItemMapper.sumVerifiedAmountByPaymentId(paymentId);
        Payment payment = getById(paymentId);
        payment.setVerifiedAmount(verifiedAmount != null ? verifiedAmount : BigDecimal.ZERO);
        payment.setPendingAmount(payment.getPaymentAmount().subtract(payment.getVerifiedAmount()));
        updateById(payment);
    }

    @Override
    public List<PaymentItem> getItems(Long paymentId) {
        return paymentItemMapper.selectByPaymentId(paymentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentItem addItem(Long paymentId, PaymentItem item) {
        Payment payment = getById(paymentId);
        if (payment == null) {
            throw new RuntimeException("付款单不存在");
        }
        if (payment.getStatus() != ReceiptStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的付款单可以添加明细");
        }
        List<PaymentItem> existingItems = getItems(paymentId);
        item.setPaymentId(paymentId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(payment.getTenantId());
        item.setVerifiedAmount(BigDecimal.ZERO);
        item.setVerifyStatus(0);
        paymentItemMapper.insert(item);
        calculateTotals(paymentId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        PaymentItem item = paymentItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("核销明细不存在");
        }
        Payment payment = getById(item.getPaymentId());
        if (payment.getStatus() != ReceiptStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的付款单可以删除明细");
        }
        paymentItemMapper.deleteById(itemId);
        calculateTotals(item.getPaymentId());
    }
}