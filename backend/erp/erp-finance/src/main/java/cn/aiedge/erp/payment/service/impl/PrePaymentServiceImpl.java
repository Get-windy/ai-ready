package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.dto.PrePaymentDTO;
import cn.aiedge.erp.payment.dto.PrePaymentQuery;
import cn.aiedge.erp.payment.dto.PrePaymentSaveDTO;
import cn.aiedge.erp.payment.entity.CapitalFlow;
import cn.aiedge.erp.payment.entity.PrePayment;
import cn.aiedge.erp.payment.entity.PrePaymentItem;
import cn.aiedge.erp.payment.mapper.PrePaymentItemMapper;
import cn.aiedge.erp.payment.mapper.PrePaymentMapper;
import cn.aiedge.erp.payment.service.CapitalFlowService;
import cn.aiedge.erp.payment.service.PrePaymentService;
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
public class PrePaymentServiceImpl extends ServiceImpl<PrePaymentMapper, PrePayment> implements PrePaymentService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_OFFSET = "offset";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CapitalFlowService capitalFlowService;
    private final PaymentAccountingService paymentAccountingService;
    private final PrePaymentItemMapper prePaymentItemMapper;

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
    public Page<PrePayment> pageQuery(PrePaymentQuery query, int pageNum, int pageSize) {
        LambdaQueryWrapper<PrePayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrePayment::getDeleted, 0);
        if (query == null) {
            query = new PrePaymentQuery();
        }
        wrapper.likeRight(PrePayment::getPrePaymentNo, query.getPrePaymentNo() != null ? query.getPrePaymentNo() : "")
                .likeRight(PrePayment::getSupplierName, query.getSupplierName() != null ? query.getSupplierName() : "")
                .likeRight(PrePayment::getSourceNo, query.getSourceNo() != null ? query.getSourceNo() : "")
                .likeRight(PrePayment::getHandlerName, query.getHandlerName() != null ? query.getHandlerName() : "")
                .likeRight(PrePayment::getDeptName, query.getDeptName() != null ? query.getDeptName() : "")
                .likeRight(PrePayment::getCreatorName, query.getCreatorName() != null ? query.getCreatorName() : "")
                .likeRight(PrePayment::getBookkeeperName, query.getBookkeeperName() != null ? query.getBookkeeperName() : "")
                .likeRight(PrePayment::getAuditorName, query.getAuditorName() != null ? query.getAuditorName() : "")
                .likeRight(PrePayment::getRemark, query.getRemark() != null ? query.getRemark() : "");
        if (query.getBankAccount() != null && !query.getBankAccount().isEmpty()) {
            wrapper.likeRight(PrePayment::getBankAccount, query.getBankAccount());
        }
        if (query.getSupplierId() != null) {
            wrapper.eq(PrePayment::getSupplierId, query.getSupplierId());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(PrePayment::getStatus, query.getStatus());
        }
        if (query.getDateStart() != null) {
            wrapper.ge(PrePayment::getPaymentDate, query.getDateStart());
        }
        if (query.getDateEnd() != null) {
            wrapper.le(PrePayment::getPaymentDate, query.getDateEnd());
        }
        wrapper.orderByDesc(PrePayment::getPaymentDate).orderByDesc(PrePayment::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrePayment createPrePayment(PrePayment payment) {
        payment.setPrePaymentNo(generatePrePaymentNo());
        payment.setUsedAmount(ZERO);
        payment.setRemainingAmount(nvl(payment.getAmount()));
        payment.setStatus(STATUS_CONFIRMED);
        payment.setPaymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDate.now());
        payment.setPrevAmount(getSupplierAdvanceBalance(payment.getSupplierId()));
        save(payment);

        // Record capital flow
        capitalFlowService.recordPrePaymentFlow(payment);

        // Call accounting service to create prepayment voucher (KJPZ-, 1123 预付账款)
        try {
            paymentAccountingService.postPrePaymentVoucher(
                    payment.getId(),
                    payment.getPrePaymentNo(),
                    String.valueOf(payment.getSupplierId()),
                    payment.getSupplierName(),
                    nvl(payment.getAmount())
            );
        } catch (Exception e) {
            log.error("调用财务模块创建预付款凭证失败: prePaymentNo={}, error={}", payment.getPrePaymentNo(), e.getMessage(), e);
        }

        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrePayment saveDraft(PrePaymentSaveDTO dto) {
        PrePayment payment;
        if (dto.getId() != null) {
            payment = getById(dto.getId());
            if (payment == null) {
                throw BusinessException.notFound("预付款单不存在");
            }
            if (STATUS_CONFIRMED.equals(payment.getStatus())) {
                throw BusinessException.badRequest("已记账的预付款单不能修改");
            }
        } else {
            payment = new PrePayment();
        }
        BeanUtils.copyProperties(dto, payment);
        if (payment.getId() == null) {
            payment.setPrePaymentNo(dto.getPrePaymentNo() != null && !dto.getPrePaymentNo().isEmpty()
                    ? dto.getPrePaymentNo() : generatePrePaymentNo());
        }
        payment.setUsedAmount(ZERO);
        payment.setRemainingAmount(nvl(dto.getAmount()));
        payment.setStatus(STATUS_DRAFT);
        payment.setPaymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now());
        payment.setPrintCount(payment.getPrintCount() != null ? payment.getPrintCount() : 0);
        if (dto.getId() != null) {
            updateById(payment);
        } else {
            save(payment);
        }
        // 替换付款账户明细
        replaceItems(payment.getId(), dto.getItems());
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrePayment confirm(Long id, Long operatorId, String operatorName) {
        PrePayment payment = getById(id);
        if (payment == null) {
            throw BusinessException.notFound("预付款单不存在");
        }
        if (STATUS_CONFIRMED.equals(payment.getStatus())) {
            return payment; // 幂等
        }
        if (nvl(payment.getAmount()).compareTo(ZERO) <= 0) {
            throw BusinessException.badRequest("本次预付金额不能为空");
        }
        // 记账前预付余额快照（不含本单草稿）
        payment.setPrevAmount(getSupplierAdvanceBalance(payment.getSupplierId()));
        payment.setStatus(STATUS_CONFIRMED);
        payment.setBookkeeperId(operatorId);
        payment.setBookkeeperName(operatorName != null ? operatorName : payment.getCreatorName());
        payment.setBookkeepingTime(LocalDateTime.now());
        updateById(payment);

        // 记录资金流水（资金付出=本次预付）
        capitalFlowService.recordPrePaymentFlow(payment);

        // 记账生成凭证（KJPZ-，1123 预付账款）
        try {
            paymentAccountingService.postPrePaymentVoucher(
                    payment.getId(),
                    payment.getPrePaymentNo(),
                    String.valueOf(payment.getSupplierId()),
                    payment.getSupplierName(),
                    nvl(payment.getAmount())
            );
        } catch (Exception e) {
            log.error("调用财务模块创建预付款凭证失败: prePaymentNo={}, error={}", payment.getPrePaymentNo(), e.getMessage(), e);
        }
        return payment;
    }

    @Override
    public PrePaymentDTO getDetail(Long id) {
        PrePayment payment = getById(id);
        if (payment == null) {
            throw BusinessException.notFound("预付款单不存在");
        }
        PrePaymentDTO dto = new PrePaymentDTO();
        BeanUtils.copyProperties(payment, dto);
        dto.setItems(prePaymentItemMapper.selectList(new LambdaQueryWrapper<PrePaymentItem>()
                .eq(PrePaymentItem::getPrePaymentId, id)
                .eq(PrePaymentItem::getDeleted, 0)
                .orderByAsc(PrePaymentItem::getLineNo)));
        return dto;
    }

    @Override
    public BigDecimal getSupplierAdvanceBalance(Long supplierId) {
        if (supplierId == null) {
            return ZERO;
        }
        return baseMapper.sumAdvanceBalanceBySupplier(supplierId);
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

        BigDecimal usedAmount = prePayment.getUsedAmount() != null ? prePayment.getUsedAmount() : ZERO;
        prePayment.setUsedAmount(usedAmount.add(amount));
        prePayment.setRemainingAmount(prePayment.getAmount().subtract(prePayment.getUsedAmount()));

        if (prePayment.getRemainingAmount().compareTo(ZERO) == 0) {
            prePayment.setStatus(STATUS_OFFSET);
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
        if (!STATUS_CONFIRMED.equals(prePayment.getStatus()) && !STATUS_OFFSET.equals(prePayment.getStatus())) {
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
        if (!STATUS_CONFIRMED.equals(prePayment.getStatus()) && !STATUS_OFFSET.equals(prePayment.getStatus())) {
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
        BigDecimal used = prePayment.getUsedAmount() != null ? prePayment.getUsedAmount() : ZERO;
        BigDecimal remaining = prePayment.getAmount().subtract(used);
        prePayment.setRemainingAmount(remaining);
        updateById(prePayment);
    }

    @Override
    public String generatePrePaymentNo() {
        String prefix = "YFKD";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PrePayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PrePayment::getPrePaymentNo, prefix + "-" + dateStr)
                .eq(PrePayment::getDeleted, 0)
                .orderByDesc(PrePayment::getPrePaymentNo)
                .last("LIMIT 1");
        PrePayment last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getPrePaymentNo();
            String tail = lastNo.substring(lastNo.lastIndexOf('-') + 1);
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + "-" + dateStr + "-" + String.format("%03d", seq);
    }

    private void replaceItems(Long prePaymentId, List<PrePaymentItem> items) {
        prePaymentItemMapper.delete(new LambdaQueryWrapper<PrePaymentItem>().eq(PrePaymentItem::getPrePaymentId, prePaymentId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int line = 1;
        for (PrePaymentItem item : items) {
            item.setId(null);
            item.setPrePaymentId(prePaymentId);
            item.setLineNo(line++);
            prePaymentItemMapper.insert(item);
        }
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }
}
