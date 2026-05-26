package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.Payable;
import cn.aiedge.finance.enums.PayableStatus;
import cn.aiedge.finance.mapper.PayableMapper;
import cn.aiedge.finance.service.PayableService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayableServiceImpl extends ServiceImpl<PayableMapper, Payable> implements PayableService {

    @Override
    public Payable getByPayableNo(String payableNo) {
        return lambdaQuery()
                .eq(Payable::getPayableNo, payableNo)
                .eq(Payable::getDeleted, 0)
                .one();
    }

    @Override
    public Page<Payable> pageList(String keyword, Long supplierId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Payable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payable::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Payable::getPayableNo, keyword)
                    .or().like(Payable::getOrderNo, keyword)
                    .or().like(Payable::getSupplierName, keyword));
        }
        if (supplierId != null) {
            wrapper.eq(Payable::getSupplierId, supplierId);
        }
        if (status != null) {
            wrapper.eq(Payable::getStatus, status);
        }
        wrapper.orderByAsc(Payable::getDueDate);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Payable> listBySupplierId(Long supplierId) {
        return baseMapper.selectBySupplierId(supplierId);
    }

    @Override
    public List<Payable> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public List<Payable> listOverdue() {
        return baseMapper.selectOverdue();
    }

    @Override
    public String generatePayableNo() {
        String prefix = "AP";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<Payable> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Payable::getPayableNo, prefix + dateStr)
                .eq(Payable::getDeleted, 0)
                .orderByDesc(Payable::getPayableNo)
                .last("LIMIT 1");
        Payable lastPayable = getOne(wrapper);
        int seq = 1;
        if (lastPayable != null) {
            String lastNo = lastPayable.getPayableNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payable createFromOrder(Long orderId, BigDecimal amount, LocalDate dueDate) {
        Payable payable = new Payable();
        payable.setPayableNo(generatePayableNo());
        payable.setOrderId(orderId);
        payable.setPayableDate(LocalDate.now());
        payable.setDueDate(dueDate);
        payable.setOriginalAmount(amount);
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setPendingAmount(amount);
        payable.setOverdueAmount(BigDecimal.ZERO);
        payable.setOverdueDays(0);
        payable.setStatus(PayableStatus.PENDING.getCode());
        payable.setTenantId(1L);
        save(payable);
        return getById(payable.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payable createFromInvoice(Long invoiceId) {
        Payable payable = new Payable();
        payable.setPayableNo(generatePayableNo());
        payable.setInvoiceId(invoiceId);
        payable.setPayableDate(LocalDate.now());
        payable.setStatus(PayableStatus.PENDING.getCode());
        payable.setTenantId(1L);
        save(payable);
        return getById(payable.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payable pay(Long payableId, BigDecimal amount) {
        Payable payable = getById(payableId);
        if (payable == null) {
            throw new RuntimeException("应付账款不存在");
        }
        BigDecimal newPaidAmount = payable.getPaidAmount().add(amount);
        payable.setPaidAmount(newPaidAmount);
        payable.setPendingAmount(payable.getOriginalAmount().subtract(newPaidAmount));
        if (payable.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            payable.setStatus(PayableStatus.PAID.getCode());
            payable.setPendingAmount(BigDecimal.ZERO);
        } else {
            payable.setStatus(PayableStatus.PARTIAL.getCode());
        }
        updateById(payable);
        return getById(payableId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payable close(Long payableId) {
        Payable payable = getById(payableId);
        if (payable == null) {
            throw new RuntimeException("应付账款不存在");
        }
        payable.setStatus(PayableStatus.CLOSED.getCode());
        updateById(payable);
        return payable;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payable cancel(Long payableId) {
        Payable payable = getById(payableId);
        if (payable == null) {
            throw new RuntimeException("应付账款不存在");
        }
        payable.setStatus(PayableStatus.CANCELLED.getCode());
        updateById(payable);
        return payable;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOverdueStatus() {
        List<Payable> overdueList = listOverdue();
        for (Payable payable : overdueList) {
            int overdueDays = LocalDate.now().compareTo(payable.getDueDate());
            payable.setOverdueDays(overdueDays);
            payable.setOverdueAmount(payable.getPendingAmount());
            payable.setStatus(PayableStatus.OVERDUE.getCode());
            updateById(payable);
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingAmount", baseMapper.sumPendingAmount(1L));
        stats.put("overdueAmount", baseMapper.sumOverdueAmount(1L));
        stats.put("paidAmount", baseMapper.sumPaidAmount(1L));
        stats.put("pendingCount", lambdaQuery()
                .eq(Payable::getStatus, PayableStatus.PENDING.getCode())
                .eq(Payable::getDeleted, 0)
                .count());
        stats.put("overdueCount", lambdaQuery()
                .eq(Payable::getStatus, PayableStatus.OVERDUE.getCode())
                .eq(Payable::getDeleted, 0)
                .count());
        return stats;
    }

    @Override
    public Map<String, BigDecimal> getAgingAnalysis() {
        Map<String, BigDecimal> aging = new HashMap<>();
        LocalDate today = LocalDate.now();
        aging.put("within30Days", lambdaQuery()
                .eq(Payable::getDeleted, 0)
                .ge(Payable::getDueDate, today)
                .le(Payable::getDueDate, today.plusDays(30))
                .sum(Payable::getPendingAmount));
        aging.put("30to60Days", lambdaQuery()
                .eq(Payable::getDeleted, 0)
                .ge(Payable::getDueDate, today.plusDays(30))
                .le(Payable::getDueDate, today.plusDays(60))
                .sum(Payable::getPendingAmount));
        aging.put("60to90Days", lambdaQuery()
                .eq(Payable::getDeleted, 0)
                .ge(Payable::getDueDate, today.plusDays(60))
                .le(Payable::getDueDate, today.plusDays(90))
                .sum(Payable::getPendingAmount));
        aging.put("over90Days", lambdaQuery()
                .eq(Payable::getDeleted, 0)
                .lt(Payable::getDueDate, today.plusDays(90))
                .sum(Payable::getPendingAmount));
        return aging;
    }
}