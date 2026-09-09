package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.dto.PreReceiptDTO;
import cn.aiedge.erp.payment.dto.PreReceiptQuery;
import cn.aiedge.erp.payment.dto.PreReceiptSaveDTO;
import cn.aiedge.erp.payment.entity.CapitalFlow;
import cn.aiedge.erp.payment.entity.PreReceipt;
import cn.aiedge.erp.payment.entity.PreReceiptItem;
import cn.aiedge.erp.payment.mapper.PreReceiptItemMapper;
import cn.aiedge.erp.payment.mapper.PreReceiptMapper;
import cn.aiedge.erp.payment.service.CapitalFlowService;
import cn.aiedge.erp.payment.service.PreReceiptService;
import cn.aiedge.erp.payment.service.integration.PaymentAccountingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
public class PreReceiptServiceImpl extends ServiceImpl<PreReceiptMapper, PreReceipt> implements PreReceiptService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CapitalFlowService capitalFlowService;
    private final PaymentAccountingService paymentAccountingService;
    private final PreReceiptItemMapper preReceiptItemMapper;

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
    public Page<PreReceipt> pageQuery(PreReceiptQuery query, int pageNum, int pageSize) {
        LambdaQueryWrapper<PreReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PreReceipt::getDeleted, 0);
        if (query == null) {
            query = new PreReceiptQuery();
        }
        wrapper.likeRight(PreReceipt::getPreReceiptNo, query.getPreReceiptNo() != null ? query.getPreReceiptNo() : "")
                .likeRight(PreReceipt::getCustomerName, query.getCustomerName() != null ? query.getCustomerName() : "")
                .likeRight(PreReceipt::getSourceNo, query.getSourceNo() != null ? query.getSourceNo() : "")
                .likeRight(PreReceipt::getHandlerName, query.getHandlerName() != null ? query.getHandlerName() : "")
                .likeRight(PreReceipt::getDeptName, query.getDeptName() != null ? query.getDeptName() : "")
                .likeRight(PreReceipt::getCreatorName, query.getCreatorName() != null ? query.getCreatorName() : "")
                .likeRight(PreReceipt::getBookkeeperName, query.getBookkeeperName() != null ? query.getBookkeeperName() : "")
                .likeRight(PreReceipt::getAuditorName, query.getAuditorName() != null ? query.getAuditorName() : "")
                .likeRight(PreReceipt::getRemark, query.getRemark() != null ? query.getRemark() : "");
        if (query.getBankAccount() != null && !query.getBankAccount().isEmpty()) {
            wrapper.likeRight(PreReceipt::getBankAccount, query.getBankAccount());
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(PreReceipt::getCustomerId, query.getCustomerId());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(PreReceipt::getStatus, query.getStatus());
        }
        if (query.getDateStart() != null) {
            wrapper.ge(PreReceipt::getReceiptDate, query.getDateStart());
        }
        if (query.getDateEnd() != null) {
            wrapper.le(PreReceipt::getReceiptDate, query.getDateEnd());
        }
        wrapper.orderByDesc(PreReceipt::getReceiptDate).orderByDesc(PreReceipt::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreReceipt createPreReceipt(PreReceipt receipt) {
        receipt.setPreReceiptNo(generatePreReceiptNo());
        receipt.setTenantId(1L);
        receipt.setUsedAmount(ZERO);
        receipt.setGiftAmount(receipt.getGiftAmount() != null ? receipt.getGiftAmount() : ZERO);
        receipt.setTotalAmount(nvl(receipt.getAmount()).add(nvl(receipt.getGiftAmount())));
        receipt.setRemainingAmount(receipt.getTotalAmount());
        receipt.setStatus("received");
        receipt.setReceiptDate(receipt.getReceiptDate() != null ? receipt.getReceiptDate() : LocalDate.now());
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
                    nvl(receipt.getAmount()),
                    null
            );
        } catch (Exception e) {
            log.error("调用财务模块创建预收款凭证失败: preReceiptNo={}, error={}", receipt.getPreReceiptNo(), e.getMessage(), e);
        }

        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreReceipt saveDraft(PreReceiptSaveDTO dto) {
        PreReceipt receipt;
        if (dto.getId() != null) {
            receipt = getById(dto.getId());
            if (receipt == null) {
                throw BusinessException.notFound("预收款单不存在");
            }
            if (STATUS_CONFIRMED.equals(receipt.getStatus())) {
                throw BusinessException.badRequest("已记账的预收款单不能修改");
            }
        } else {
            receipt = new PreReceipt();
        }
        BeanUtils.copyProperties(dto, receipt);
        if (receipt.getId() == null) {
            receipt.setPreReceiptNo(dto.getPreReceiptNo() != null && !dto.getPreReceiptNo().isEmpty()
                    ? dto.getPreReceiptNo() : generatePreReceiptNo());
        }
        receipt.setTenantId(1L);
        receipt.setUsedAmount(ZERO);
        receipt.setGiftAmount(nvl(dto.getGiftAmount()));
        receipt.setTotalAmount(nvl(dto.getAmount()).add(nvl(dto.getGiftAmount())));
        receipt.setRemainingAmount(receipt.getTotalAmount());
        receipt.setStatus(STATUS_DRAFT);
        receipt.setReceiptDate(dto.getReceiptDate() != null ? dto.getReceiptDate() : LocalDate.now());
        receipt.setPrintCount(receipt.getPrintCount() != null ? receipt.getPrintCount() : 0);
        if (dto.getId() != null) {
            updateById(receipt);
        } else {
            save(receipt);
        }
        // 替换收款账户明细
        replaceItems(receipt.getId(), dto.getItems());
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreReceipt confirm(Long id, Long operatorId, String operatorName) {
        PreReceipt receipt = getById(id);
        if (receipt == null) {
            throw BusinessException.notFound("预收款单不存在");
        }
        if (STATUS_CONFIRMED.equals(receipt.getStatus())) {
            return receipt; // 幂等
        }
        if (nvl(receipt.getAmount()).compareTo(ZERO) <= 0 && nvl(receipt.getGiftAmount()).compareTo(ZERO) <= 0) {
            throw BusinessException.badRequest("本次预收金额不能为空");
        }
        // 记账前预收余额快照（不含本单草稿）
        receipt.setPrevAmount(getCustomerAdvanceBalance(receipt.getCustomerId()));
        receipt.setStatus(STATUS_CONFIRMED);
        receipt.setBookkeeperId(operatorId);
        receipt.setBookkeeperName(operatorName != null ? operatorName : receipt.getCreatorName());
        receipt.setBookkeepingTime(LocalDateTime.now());
        updateById(receipt);

        // 记录资金流水（资金实收=本次预收）
        capitalFlowService.recordPreReceiptFlow(receipt);

        // 记账生成凭证（KJPZ-，2203 预收账款，资金实收）
        try {
            paymentAccountingService.postReceiptVoucher(
                    receipt.getId(),
                    receipt.getPreReceiptNo(),
                    String.valueOf(receipt.getCustomerId()),
                    receipt.getCustomerName(),
                    nvl(receipt.getAmount()),
                    null
            );
        } catch (Exception e) {
            log.error("调用财务模块创建预收款凭证失败: preReceiptNo={}, error={}", receipt.getPreReceiptNo(), e.getMessage(), e);
        }
        return receipt;
    }

    @Override
    public PreReceiptDTO getDetail(Long id) {
        PreReceipt receipt = getById(id);
        if (receipt == null) {
            throw BusinessException.notFound("预收款单不存在");
        }
        PreReceiptDTO dto = new PreReceiptDTO();
        BeanUtils.copyProperties(receipt, dto);
        dto.setItems(preReceiptItemMapper.selectList(new LambdaQueryWrapper<PreReceiptItem>()
                .eq(PreReceiptItem::getPreReceiptId, id)
                .eq(PreReceiptItem::getDeleted, 0)
                .orderByAsc(PreReceiptItem::getLineNo)));
        return dto;
    }

    @Override
    public BigDecimal getCustomerAdvanceBalance(Long customerId) {
        if (customerId == null) {
            return ZERO;
        }
        return baseMapper.sumAdvanceBalanceByCustomer(customerId);
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

        BigDecimal usedAmount = preReceipt.getUsedAmount() != null ? preReceipt.getUsedAmount() : ZERO;
        preReceipt.setUsedAmount(usedAmount.add(amount));
        preReceipt.setRemainingAmount(preReceipt.getTotalAmount().subtract(preReceipt.getUsedAmount()));

        if (preReceipt.getRemainingAmount().compareTo(ZERO) == 0) {
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
        if (!STATUS_CONFIRMED.equals(preReceipt.getStatus()) && !"received".equals(preReceipt.getStatus())
                && !"offset".equals(preReceipt.getStatus())) {
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
        if (!STATUS_CONFIRMED.equals(preReceipt.getStatus()) && !"received".equals(preReceipt.getStatus())
                && !"offset".equals(preReceipt.getStatus())) {
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
        BigDecimal used = preReceipt.getUsedAmount() != null ? preReceipt.getUsedAmount() : ZERO;
        BigDecimal total = preReceipt.getTotalAmount() != null ? preReceipt.getTotalAmount() : nvl(preReceipt.getAmount());
        BigDecimal remaining = total.subtract(used);
        preReceipt.setRemainingAmount(remaining);
        updateById(preReceipt);
    }

    @Override
    public String generatePreReceiptNo() {
        String prefix = "YSKD";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PreReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PreReceipt::getPreReceiptNo, prefix + "-" + dateStr)
                .eq(PreReceipt::getDeleted, 0)
                .orderByDesc(PreReceipt::getPreReceiptNo)
                .last("LIMIT 1");
        PreReceipt last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getPreReceiptNo();
            String tail = lastNo.substring(lastNo.lastIndexOf('-') + 1);
            // 兼容 YSKD-20260907-001 三位序号
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + "-" + dateStr + "-" + String.format("%03d", seq);
    }

    private void replaceItems(Long preReceiptId, List<PreReceiptItem> items) {
        preReceiptItemMapper.delete(new LambdaQueryWrapper<PreReceiptItem>().eq(PreReceiptItem::getPreReceiptId, preReceiptId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int line = 1;
        for (PreReceiptItem item : items) {
            item.setId(null);
            item.setPreReceiptId(preReceiptId);
            item.setLineNo(line++);
            item.setTenantId(1L);
            preReceiptItemMapper.insert(item);
        }
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }
}
