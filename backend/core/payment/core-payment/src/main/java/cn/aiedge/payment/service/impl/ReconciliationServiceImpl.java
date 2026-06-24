package cn.aiedge.payment.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.entity.PaymentReconciliation;
import cn.aiedge.payment.mapper.PaymentRecordMapper;
import cn.aiedge.payment.mapper.PaymentReconciliationMapper;
import cn.aiedge.payment.service.ReconciliationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private final PaymentReconciliationMapper reconciliationMapper;
    private final PaymentRecordMapper recordMapper;

    @Override
    @Transactional
    public List<PaymentReconciliation> executeDailyReconciliation(LocalDate date, String channel) {
        List<PaymentReconciliation> results = new ArrayList<>();

        // 查询当天支付记录
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(PaymentRecord::getCallbackTime, date.atStartOfDay());
        wrapper.lt(PaymentRecord::getCallbackTime, date.plusDays(1).atStartOfDay());
        wrapper.eq(PaymentRecord::getStatus, 2); // 成功记录
        if (channel != null) {
            wrapper.eq(PaymentRecord::getChannel, channel);
        }

        List<PaymentRecord> records = recordMapper.selectList(wrapper);

        // 按渠道分组统计
        Map<String, List<PaymentRecord>> channelGroups = records.stream()
                .collect(Collectors.groupingBy(PaymentRecord::getChannel));

        for (Map.Entry<String, List<PaymentRecord>> entry : channelGroups.entrySet()) {
            String ch = entry.getKey();
            List<PaymentRecord> chRecords = entry.getValue();

            PaymentReconciliation recon = new PaymentReconciliation();
            recon.setReconcileDate(date);
            recon.setChannel(ch);
            recon.setTotalCount(chRecords.size());
            recon.setTotalAmount(chRecords.stream()
                    .map(PaymentRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            recon.setSuccessCount(chRecords.size());
            recon.setSuccessAmount(recon.getTotalAmount());
            recon.setDiffCount(0);
            recon.setDiffAmount(BigDecimal.ZERO);
            recon.setStatus(1); // 已对账
            recon.setReconciledTime(LocalDate.now());

            reconciliationMapper.insert(recon);
            results.add(recon);
        }

        return results;
    }

    @Override
    public PageResult<PaymentReconciliation> pageReconciliation(Integer pageNum, Integer pageSize,
            LocalDate startDate, LocalDate endDate, String channel, Integer status) {
        LambdaQueryWrapper<PaymentReconciliation> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(startDate != null, PaymentReconciliation::getReconcileDate, startDate);
        wrapper.le(endDate != null, PaymentReconciliation::getReconcileDate, endDate);
        wrapper.eq(channel != null, PaymentReconciliation::getChannel, channel);
        wrapper.eq(status != null, PaymentReconciliation::getStatus, status);
        wrapper.orderByDesc(PaymentReconciliation::getReconcileDate);

        Page<PaymentReconciliation> page = reconciliationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public PaymentReconciliation getReconciliation(Long id) {
        return reconciliationMapper.selectById(id);
    }

    @Override
    @Transactional
    public void handleDifference(Long id, String remark) {
        PaymentReconciliation recon = reconciliationMapper.selectById(id);
        if (recon == null) {
            throw new IllegalArgumentException("对账记录不存在");
        }
        recon.setRemark(remark);
        recon.setStatus(2); // 有差异
        reconciliationMapper.updateById(recon);
    }

    @Override
    public List<LocalDate> getPendingDates(String channel) {
        // 查询最近未对账的日期
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(30);

        LambdaQueryWrapper<PaymentReconciliation> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(PaymentReconciliation::getReconcileDate, startDate);
        wrapper.le(PaymentReconciliation::getReconcileDate, today);
        wrapper.eq(channel != null, PaymentReconciliation::getChannel, channel);
        wrapper.eq(PaymentReconciliation::getStatus, 0); // 待对账

        List<PaymentReconciliation> recons = reconciliationMapper.selectList(wrapper);
        return recons.stream()
                .map(PaymentReconciliation::getReconcileDate)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}