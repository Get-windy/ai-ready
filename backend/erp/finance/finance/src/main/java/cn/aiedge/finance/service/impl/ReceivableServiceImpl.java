package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.Receivable;
import cn.aiedge.finance.enums.ReceivableStatus;
import cn.aiedge.finance.mapper.ReceivableMapper;
import cn.aiedge.finance.service.ReceivableService;
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
public class ReceivableServiceImpl extends ServiceImpl<ReceivableMapper, Receivable> implements ReceivableService {

    @Override
    public Receivable getByReceivableNo(String receivableNo) {
        return lambdaQuery()
                .eq(Receivable::getReceivableNo, receivableNo)
                .eq(Receivable::getDeleted, 0)
                .one();
    }

    @Override
    public Page<Receivable> pageList(String keyword, Long customerId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Receivable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Receivable::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Receivable::getReceivableNo, keyword)
                    .or().like(Receivable::getOrderNo, keyword)
                    .or().like(Receivable::getCustomerName, keyword));
        }
        if (customerId != null) {
            wrapper.eq(Receivable::getCustomerId, customerId);
        }
        if (status != null) {
            wrapper.eq(Receivable::getStatus, status);
        }
        wrapper.orderByAsc(Receivable::getDueDate);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Receivable> listByCustomerId(Long customerId) {
        return baseMapper.selectByCustomerId(customerId);
    }

    @Override
    public List<Receivable> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public List<Receivable> listOverdue() {
        return baseMapper.selectOverdue();
    }

    @Override
    public String generateReceivableNo() {
        String prefix = "AR";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<Receivable> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Receivable::getReceivableNo, prefix + dateStr)
                .eq(Receivable::getDeleted, 0)
                .orderByDesc(Receivable::getReceivableNo)
                .last("LIMIT 1");
        Receivable lastReceivable = getOne(wrapper);
        int seq = 1;
        if (lastReceivable != null) {
            String lastNo = lastReceivable.getReceivableNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receivable createFromOrder(Long orderId, BigDecimal amount, LocalDate dueDate) {
        Receivable receivable = new Receivable();
        receivable.setReceivableNo(generateReceivableNo());
        receivable.setOrderId(orderId);
        receivable.setReceivableDate(LocalDate.now());
        receivable.setDueDate(dueDate);
        receivable.setOriginalAmount(amount);
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setPendingAmount(amount);
        receivable.setOverdueAmount(BigDecimal.ZERO);
        receivable.setOverdueDays(0);
        receivable.setStatus(ReceivableStatus.PENDING.getCode());
        receivable.setTenantId(1L);
        save(receivable);
        return getById(receivable.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receivable createFromInvoice(Long invoiceId) {
        Receivable receivable = new Receivable();
        receivable.setReceivableNo(generateReceivableNo());
        receivable.setInvoiceId(invoiceId);
        receivable.setReceivableDate(LocalDate.now());
        receivable.setStatus(ReceivableStatus.PENDING.getCode());
        receivable.setTenantId(1L);
        save(receivable);
        return getById(receivable.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receivable receive(Long receivableId, BigDecimal amount) {
        Receivable receivable = getById(receivableId);
        if (receivable == null) {
            throw new RuntimeException("应收账款不存在");
        }
        BigDecimal newReceivedAmount = receivable.getReceivedAmount().add(amount);
        receivable.setReceivedAmount(newReceivedAmount);
        receivable.setPendingAmount(receivable.getOriginalAmount().subtract(newReceivedAmount));
        if (receivable.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            receivable.setStatus(ReceivableStatus.RECEIVED.getCode());
            receivable.setPendingAmount(BigDecimal.ZERO);
        } else {
            receivable.setStatus(ReceivableStatus.PARTIAL.getCode());
        }
        updateById(receivable);
        return getById(receivableId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receivable close(Long receivableId) {
        Receivable receivable = getById(receivableId);
        if (receivable == null) {
            throw new RuntimeException("应收账款不存在");
        }
        receivable.setStatus(ReceivableStatus.CLOSED.getCode());
        updateById(receivable);
        return receivable;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receivable cancel(Long receivableId) {
        Receivable receivable = getById(receivableId);
        if (receivable == null) {
            throw new RuntimeException("应收账款不存在");
        }
        receivable.setStatus(ReceivableStatus.CANCELLED.getCode());
        updateById(receivable);
        return receivable;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOverdueStatus() {
        List<Receivable> overdueList = listOverdue();
        for (Receivable receivable : overdueList) {
            int overdueDays = LocalDate.now().compareTo(receivable.getDueDate());
            receivable.setOverdueDays(overdueDays);
            receivable.setOverdueAmount(receivable.getPendingAmount());
            receivable.setStatus(ReceivableStatus.OVERDUE.getCode());
            updateById(receivable);
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingAmount", baseMapper.sumPendingAmount(1L));
        stats.put("overdueAmount", baseMapper.sumOverdueAmount(1L));
        stats.put("receivedAmount", baseMapper.sumReceivedAmount(1L));
        stats.put("pendingCount", lambdaQuery()
                .eq(Receivable::getStatus, ReceivableStatus.PENDING.getCode())
                .eq(Receivable::getDeleted, 0)
                .count());
        stats.put("overdueCount", lambdaQuery()
                .eq(Receivable::getStatus, ReceivableStatus.OVERDUE.getCode())
                .eq(Receivable::getDeleted, 0)
                .count());
        return stats;
    }

    @Override
    public Map<String, BigDecimal> getAgingAnalysis() {
        Map<String, BigDecimal> aging = new HashMap<>();
        LocalDate today = LocalDate.now();
        aging.put("within30Days", lambdaQuery()
                .eq(Receivable::getDeleted, 0)
                .ge(Receivable::getDueDate, today)
                .le(Receivable::getDueDate, today.plusDays(30))
                .sum(Receivable::getPendingAmount));
        aging.put("30to60Days", lambdaQuery()
                .eq(Receivable::getDeleted, 0)
                .ge(Receivable::getDueDate, today.plusDays(30))
                .le(Receivable::getDueDate, today.plusDays(60))
                .sum(Receivable::getPendingAmount));
        aging.put("60to90Days", lambdaQuery()
                .eq(Receivable::getDeleted, 0)
                .ge(Receivable::getDueDate, today.plusDays(60))
                .le(Receivable::getDueDate, today.plusDays(90))
                .sum(Receivable::getPendingAmount));
        aging.put("over90Days", lambdaQuery()
                .eq(Receivable::getDeleted, 0)
                .lt(Receivable::getDueDate, today.plusDays(90))
                .sum(Receivable::getPendingAmount));
        return aging;
    }
}