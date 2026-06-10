package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.entity.Receipt;
import cn.aiedge.erp.payment.entity.ReceiptItem;
import cn.aiedge.erp.payment.enums.ReceiptStatus;
import cn.aiedge.erp.payment.mapper.ReceiptItemMapper;
import cn.aiedge.erp.payment.mapper.ReceiptMapper;
import cn.aiedge.erp.payment.entity.WriteOff;
import cn.aiedge.erp.payment.service.CapitalFlowService;
import cn.aiedge.erp.payment.service.ReceiptService;
import cn.aiedge.erp.payment.service.WriteOffService;
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
public class ReceiptServiceImpl extends ServiceImpl<ReceiptMapper, Receipt> implements ReceiptService {

    private final ReceiptItemMapper receiptItemMapper;
    private final CapitalFlowService capitalFlowService;
    private final WriteOffService writeOffService;

    @Override
    public Receipt getByReceiptNo(String receiptNo) {
        return lambdaQuery()
                .eq(Receipt::getReceiptNo, receiptNo)
                .eq(Receipt::getDeleted, 0)
                .one();
    }

    @Override
    public Page<Receipt> pageList(String keyword, Long customerId, Long orderId, Integer status, String sourceType, int pageNum, int pageSize) {
        LambdaQueryWrapper<Receipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Receipt::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Receipt::getReceiptNo, keyword)
                    .or().like(Receipt::getOrderNo, keyword)
                    .or().like(Receipt::getCustomerName, keyword));
        }
        if (customerId != null) {
            wrapper.eq(Receipt::getCustomerId, customerId);
        }
        if (orderId != null) {
            wrapper.eq(Receipt::getOrderId, orderId);
        }
        if (status != null) {
            wrapper.eq(Receipt::getStatus, status);
        }
        if (sourceType != null && !sourceType.isEmpty()) {
            wrapper.eq(Receipt::getSourceType, sourceType);
        }
        wrapper.orderByDesc(Receipt::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Receipt> listByCustomerId(Long customerId) {
        return baseMapper.selectByCustomerId(customerId);
    }

    @Override
    public List<Receipt> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public String generateReceiptNo() {
        String prefix = "RC";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<Receipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Receipt::getReceiptNo, prefix + dateStr)
                .eq(Receipt::getDeleted, 0)
                .orderByDesc(Receipt::getReceiptNo)
                .last("LIMIT 1");
        Receipt lastReceipt = getOne(wrapper);
        int seq = 1;
        if (lastReceipt != null) {
            String lastNo = lastReceipt.getReceiptNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt createReceipt(Receipt receipt, List<ReceiptItem> items) {
        receipt.setReceiptNo(generateReceiptNo());
        receipt.setStatus(ReceiptStatus.DRAFT.getCode());
        receipt.setReceiptDate(LocalDate.now());
        receipt.setReceiptType(1);
        receipt.setVerifiedAmount(BigDecimal.ZERO);
        receipt.setPendingAmount(receipt.getReceiptAmount());
        save(receipt);
        capitalFlowService.recordReceiptFlow(receipt);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                ReceiptItem item = items.get(i);
                item.setReceiptId(receipt.getId());
                item.setLineNo(i + 1);
                item.setTenantId(receipt.getTenantId());
                item.setVerifiedAmount(BigDecimal.ZERO);
                item.setVerifyStatus(0);
                receiptItemMapper.insert(item);
            }
        }
        calculateTotals(receipt.getId());
        return getById(receipt.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt createFromOrder(Long orderId) {
        Receipt receipt = new Receipt();
        receipt.setOrderId(orderId);
        receipt.setReceiptDate(LocalDate.now());
        receipt.setReceiptType(1);
        return createReceipt(receipt, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt createFromInvoice(Long invoiceId) {
        Receipt receipt = new Receipt();
        receipt.setInvoiceId(invoiceId);
        receipt.setReceiptDate(LocalDate.now());
        receipt.setReceiptType(1);
        return createReceipt(receipt, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt updateReceipt(Long receiptId, Receipt receipt, List<ReceiptItem> items) {
        Receipt existing = getById(receiptId);
        if (existing == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        if (existing.getStatus() != ReceiptStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的收款单可以修改");
        }
        receipt.setId(receiptId);
        receipt.setPendingAmount(receipt.getReceiptAmount());
        updateById(receipt);
        if (items != null) {
            List<ReceiptItem> existingItems = getItems(receiptId);
            for (ReceiptItem oldItem : existingItems) {
                receiptItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                ReceiptItem item = items.get(i);
                item.setReceiptId(receiptId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                item.setVerifiedAmount(BigDecimal.ZERO);
                item.setVerifyStatus(0);
                receiptItemMapper.insert(item);
            }
        }
        calculateTotals(receiptId);
        return getById(receiptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt submitForApproval(Long receiptId) {
        Receipt receipt = getById(receiptId);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        if (receipt.getStatus() != ReceiptStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的收款单可以提交审批");
        }
        receipt.setStatus(ReceiptStatus.PENDING_APPROVAL.getCode());
        updateById(receipt);
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt approve(Long receiptId, Long approverId, String note) {
        Receipt receipt = getById(receiptId);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        if (receipt.getStatus() != ReceiptStatus.PENDING_APPROVAL.getCode()) {
            throw BusinessException.badRequest("只有待审批状态的收款单可以审批");
        }
        receipt.setStatus(ReceiptStatus.APPROVED.getCode());
        receipt.setApprovedBy(approverId);
        receipt.setApprovedTime(LocalDateTime.now());
        receipt.setApprovedNote(note);
        updateById(receipt);
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt reject(Long receiptId, String reason) {
        Receipt receipt = getById(receiptId);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        if (receipt.getStatus() != ReceiptStatus.PENDING_APPROVAL.getCode()) {
            throw BusinessException.badRequest("只有待审批状态的收款单可以拒绝");
        }
        receipt.setStatus(ReceiptStatus.DRAFT.getCode());
        receipt.setRemark(reason);
        updateById(receipt);
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt startVerify(Long receiptId) {
        Receipt receipt = getById(receiptId);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        if (receipt.getStatus() != ReceiptStatus.APPROVED.getCode()) {
            throw BusinessException.badRequest("只有已审批状态的收款单可以开始核销");
        }
        receipt.setStatus(ReceiptStatus.VERIFYING.getCode());
        updateById(receipt);
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceiptItem verifyItem(Long itemId, BigDecimal verifyAmount) {
        ReceiptItem item = receiptItemMapper.selectById(itemId);
        if (item == null) {
            throw BusinessException.notFound("核销明细不存在");
        }
        Receipt receipt = getById(item.getReceiptId());
        if (receipt.getStatus() != ReceiptStatus.VERIFYING.getCode()) {
            throw BusinessException.badRequest("只有核销中状态的收款单可以处理明细");
        }
        item.setVerifyAmount(verifyAmount);
        item.setVerifiedAmount(verifyAmount);
        item.setVerifyStatus(1);
        item.setVerifiedTime(LocalDateTime.now());
        item.setVerifiedBy(receipt.getCreateBy());
        receiptItemMapper.updateById(item);
        calculateTotals(item.getReceiptId());
        return receiptItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt completeVerify(Long receiptId) {
        Receipt receipt = getById(receiptId);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        receipt.setStatus(ReceiptStatus.VERIFIED.getCode());
        receipt.setVerifiedBy(receipt.getCreateBy());
        receipt.setVerifiedTime(LocalDateTime.now());
        updateById(receipt);
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt complete(Long receiptId) {
        Receipt receipt = getById(receiptId);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        if (receipt.getStatus() != ReceiptStatus.VERIFIED.getCode()) {
            throw BusinessException.badRequest("只有已核销状态的收款单可以完成");
        }
        receipt.setStatus(ReceiptStatus.COMPLETED.getCode());
        receipt.setCompletedBy(receipt.getCreateBy());
        receipt.setCompletedTime(LocalDateTime.now());
        updateById(receipt);
        capitalFlowService.recordReceiptFlow(receipt);
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt cancel(Long receiptId, String reason) {
        Receipt receipt = getById(receiptId);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        if (receipt.getStatus() == ReceiptStatus.COMPLETED.getCode()) {
            throw BusinessException.badRequest("已完成的收款单不能取消");
        }
        receipt.setStatus(ReceiptStatus.CANCELLED.getCode());
        receipt.setRemark(reason);
        updateById(receipt);
        capitalFlowService.recordReceiptFlow(receipt);
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long receiptId) {
        BigDecimal verifiedAmount = receiptItemMapper.sumVerifiedAmountByReceiptId(receiptId);
        Receipt receipt = getById(receiptId);
        receipt.setVerifiedAmount(verifiedAmount != null ? verifiedAmount : BigDecimal.ZERO);
        receipt.setPendingAmount(receipt.getReceiptAmount().subtract(receipt.getVerifiedAmount()));
        updateById(receipt);
    }

    @Override
    public List<ReceiptItem> getItems(Long receiptId) {
        return receiptItemMapper.selectByReceiptId(receiptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceiptItem addItem(Long receiptId, ReceiptItem item) {
        Receipt receipt = getById(receiptId);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        if (receipt.getStatus() != ReceiptStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的收款单可以添加明细");
        }
        List<ReceiptItem> existingItems = getItems(receiptId);
        item.setReceiptId(receiptId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(receipt.getTenantId());
        item.setVerifiedAmount(BigDecimal.ZERO);
        item.setVerifyStatus(0);
        receiptItemMapper.insert(item);
        calculateTotals(receiptId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        ReceiptItem item = receiptItemMapper.selectById(itemId);
        if (item == null) {
            throw BusinessException.notFound("核销明细不存在");
        }
        Receipt receipt = getById(item.getReceiptId());
        if (receipt.getStatus() != ReceiptStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的收款单可以删除明细");
        }
        receiptItemMapper.deleteById(itemId);
        calculateTotals(item.getReceiptId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt writeOff(Long receiptId, BigDecimal amount) {
        Receipt receipt = getById(receiptId);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        if (receipt.getStatus() == ReceiptStatus.COMPLETED.getCode() || receipt.getStatus() == ReceiptStatus.CANCELLED.getCode()) {
            throw BusinessException.badRequest("已完成或已取消的收款单不能核销");
        }
        BigDecimal pending = receipt.getPendingAmount() != null ? receipt.getPendingAmount() : receipt.getReceiptAmount();
        if (amount.compareTo(pending) > 0) {
            throw BusinessException.badRequest("核销金额不能大于未核销金额");
        }
        receipt.setVerifiedAmount(receipt.getVerifiedAmount() != null
                ? receipt.getVerifiedAmount().add(amount) : amount);
        receipt.setPendingAmount(pending.subtract(amount));
        if (receipt.getPendingAmount().compareTo(BigDecimal.ZERO) == 0) {
            receipt.setStatus(ReceiptStatus.VERIFIED.getCode());
        }
        updateById(receipt);

        // 创建核销记录
        writeOffService.createReceiptWriteOff(
                receiptId, receipt.getReceiptNo(), null,
                receipt.getCustomerId(), receipt.getCustomerName(),
                receipt.getReceiptAmount(), amount, receipt.getPendingAmount());

        log.info("收款单核销成功: receiptId={}, amount={}", receiptId, amount);
        return receipt;
    }
}